-- Banners do carrossel principal da home (ADR-0002).
-- Aplicação manual por uma pessoa (ver ADR-0002 e docs/STATUS.md); nunca pelo BFF.
begin;

create table public.home_banners (
  id             uuid primary key default gen_random_uuid(),
  image_url      text not null
                 check (image_url ~ '^https://[^[:space:]]+$' and length(image_url) <= 2048),
  alt_text       text not null
                 check (length(btrim(alt_text)) between 1 and 140),
  -- Rota interna do app (ex.: /search). Só caminho, sem esquema nem query.
  internal_route text
                 check (internal_route ~ '^/[a-z0-9/_-]{0,199}$'),
  -- Link externo: só https. Barra javascript:, data:, http: etc.
  external_url   text
                 check (external_url ~ '^https://[^[:space:]]+$' and length(external_url) <= 2048),
  is_active      boolean not null default false,
  sort_order     integer not null default 0,
  starts_at      timestamptz,
  ends_at        timestamptz,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  -- No máximo um destino; nenhum = banner não clicável
  constraint home_banners_one_link check (num_nonnulls(internal_route, external_url) <= 1),
  constraint home_banners_window check (starts_at is null or ends_at is null or ends_at > starts_at)
);

comment on table public.home_banners is 'Cards do carrossel principal da home. Escrita só pela service role.';

create index idx_home_banners_live on public.home_banners (sort_order, created_at desc) where is_active;

create function public.home_banners_touch_updated_at() returns trigger
  language plpgsql
  set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end
$$;

create trigger trg_home_banners_touch_updated_at
  before update on public.home_banners
  for each row execute function public.home_banners_touch_updated_at();

alter table public.home_banners enable row level security;

-- Leitura pública (a home também é vista por visitantes), só do que está no ar
create policy "leitura publica de banners no ar" on public.home_banners
  for select to anon, authenticated
  using (
    is_active
    and (starts_at is null or starts_at <= now())
    and (ends_at is null or ends_at > now())
  );

-- Sem policy de escrita: só a service role (que ignora RLS) grava
revoke insert, update, delete, truncate on public.home_banners from anon, authenticated;

-- Imagens dos banners: leitura pública, até 500 KB, só imagem
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('home-banners', 'home-banners', true, 512000, array['image/webp', 'image/jpeg', 'image/png'])
on conflict (id) do nothing;

commit;
