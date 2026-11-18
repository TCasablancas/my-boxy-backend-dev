-- Avaliações de produto e de loja (ADR-0003).
-- Uma tabela só para os dois alvos: avaliação de produto tem product_id; avaliação
-- da loja (sem produto) tem product_id nulo. store_id está sempre preenchido, então
-- a listagem da loja pode juntar as duas coisas num único índice.
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

-- ---------------------------------------------------------------------------
-- reviews
-- ---------------------------------------------------------------------------
create table public.reviews (
  id             uuid primary key default gen_random_uuid(),
  author_id      uuid not null references public.user_profiles(id) on delete cascade,
  store_id       uuid not null references public.stores(id) on delete cascade,
  product_id     uuid references public.products(id) on delete cascade,
  -- Item do pedido que deu direito à avaliação ("compra verificada"). Opcional na v1.
  order_item_id  uuid references public.order_items(id) on delete set null,
  rating         smallint not null,
  comment        text,
  status         varchar(20) not null default 'PUBLISHED',
  -- Contadores mantidos por trigger, para o card da lista não precisar de COUNT por linha.
  likes_count    integer  not null default 0,
  photos_count   smallint not null default 0,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  edited_at      timestamptz,

  constraint reviews_rating_range   check (rating between 1 and 5),
  constraint reviews_comment_length check (comment is null or char_length(btrim(comment)) between 1 and 2000),
  constraint reviews_status_check   check (status in ('PUBLISHED', 'HIDDEN', 'REMOVED')),
  constraint reviews_likes_positive check (likes_count >= 0),
  constraint reviews_photos_range   check (photos_count between 0 and 5)
);

comment on table public.reviews is
  'Avaliações. product_id preenchido = avaliação de produto; nulo = avaliação da loja.';

-- Uma avaliação por pessoa por produto, e uma avaliação "da loja" por pessoa por loja.
create unique index reviews_one_per_product on public.reviews (author_id, product_id)
  where product_id is not null;
create unique index reviews_one_per_store on public.reviews (author_id, store_id)
  where product_id is null;

-- Lista sem filtro (mais recentes primeiro, paginação por cursor created_at+id).
create index reviews_product_feed on public.reviews (product_id, created_at desc, id desc)
  where status = 'PUBLISHED' and product_id is not null;
create index reviews_store_feed on public.reviews (store_id, created_at desc, id desc)
  where status = 'PUBLISHED';
-- Resumo por nota (barras) e lista filtrada por nota.
create index reviews_product_rating on public.reviews (product_id, rating, created_at desc, id desc)
  where status = 'PUBLISHED' and product_id is not null;
create index reviews_store_rating on public.reviews (store_id, rating, created_at desc, id desc)
  where status = 'PUBLISHED';
create index reviews_author on public.reviews (author_id);

-- store_id sempre vem do produto (o cliente não escolhe), e updated_at/edited_at
-- são mantidos pelo banco.
create function public.reviews_before_write() returns trigger
  language plpgsql
  set search_path = ''
as $$
begin
  if new.product_id is not null then
    select p.store_id into new.store_id from public.products p where p.id = new.product_id;
    if new.store_id is null then
      raise exception 'Produto % não existe', new.product_id using errcode = '23503';
    end if;
  end if;

  if tg_op = 'UPDATE' then
    new.updated_at := now();
    if new.rating is distinct from old.rating or new.comment is distinct from old.comment then
      new.edited_at := now();
    end if;
  end if;
  return new;
end;
$$;

create trigger trg_reviews_before_write
  before insert or update on public.reviews
  for each row execute function public.reviews_before_write();

-- ---------------------------------------------------------------------------
-- review_photos (fotos do produto enviadas junto com a avaliação, até 5)
-- ---------------------------------------------------------------------------
create table public.review_photos (
  id           uuid primary key default gen_random_uuid(),
  review_id    uuid not null references public.reviews(id) on delete cascade,
  -- Caminho do objeto no bucket (ex.: '<review_id>/<uuid>.webp'), nunca URL completa.
  storage_path text not null,
  position     smallint not null,
  created_at   timestamptz not null default now(),

  constraint review_photos_position_range check (position between 0 and 4),
  constraint review_photos_path_relative  check (storage_path !~* '^[a-z]+://' and storage_path !~ '\.\.'),
  constraint review_photos_unique_position unique (review_id, position)
);

-- ---------------------------------------------------------------------------
-- review_likes (curtidas de outras pessoas)
-- ---------------------------------------------------------------------------
create table public.review_likes (
  review_id  uuid not null references public.reviews(id) on delete cascade,
  user_id    uuid not null references public.user_profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (review_id, user_id)
);

create index review_likes_user on public.review_likes (user_id);

-- Contadores. O CHECK photos_count <= 5 faz a 6ª foto falhar sozinha.
create function public.review_counters_sync() returns trigger
  language plpgsql
  set search_path = ''
as $$
declare
  delta integer := case when tg_op = 'INSERT' then 1 else -1 end;
  target uuid  := case when tg_op = 'INSERT' then new.review_id else old.review_id end;
begin
  if tg_table_name = 'review_likes' then
    update public.reviews set likes_count = greatest(likes_count + delta, 0) where id = target;
  else
    update public.reviews set photos_count = greatest(photos_count + delta, 0) where id = target;
  end if;
  return null;
end;
$$;

create trigger trg_review_likes_count
  after insert or delete on public.review_likes
  for each row execute function public.review_counters_sync();

create trigger trg_review_photos_count
  after insert or delete on public.review_photos
  for each row execute function public.review_counters_sync();

-- Quem escreveu não curte a própria avaliação.
create function public.review_likes_block_self() returns trigger
  language plpgsql
  set search_path = ''
as $$
begin
  if exists (select 1 from public.reviews r where r.id = new.review_id and r.author_id = new.user_id) then
    raise exception 'Não é possível curtir a própria avaliação' using errcode = '23514';
  end if;
  return new;
end;
$$;

create trigger trg_review_likes_block_self
  before insert on public.review_likes
  for each row execute function public.review_likes_block_self();

-- ---------------------------------------------------------------------------
-- Acesso: mesmo padrão do catálogo. RLS ligada e nenhuma policy, então anon e
-- authenticated não leem nem gravam via PostgREST; o BFF (postgres) acessa direto.
-- ---------------------------------------------------------------------------
alter table public.reviews       enable row level security;
alter table public.review_photos enable row level security;
alter table public.review_likes  enable row level security;

revoke execute on function public.reviews_before_write()    from public, anon, authenticated;
revoke execute on function public.review_counters_sync()    from public, anon, authenticated;
revoke execute on function public.review_likes_block_self() from public, anon, authenticated;

commit;
