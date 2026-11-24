-- Perguntas (comentários) públicas sobre o produto, feitas por usuários logados.
-- answer/answered_at ficam para a resposta do vendedor (fora deste escopo por enquanto).
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

create table public.product_questions (
  id          uuid primary key default gen_random_uuid(),
  product_id  uuid not null references public.products(id) on delete cascade,
  author_id   uuid not null references public.user_profiles(id) on delete cascade,
  question    text not null check (char_length(btrim(question)) between 1 and 500),
  answer      text,
  answered_at timestamptz,
  created_at  timestamptz not null default now()
);

-- Lista infinita: paginação por cursor (created_at, id), mais recentes primeiro.
create index product_questions_product_page
  on public.product_questions (product_id, created_at desc, id desc);

-- Mesmo padrão do catálogo, avaliações e favoritos: RLS ligada e nenhuma policy,
-- então anon e authenticated não leem nem gravam via PostgREST; o BFF acessa direto.
alter table public.product_questions enable row level security;

commit;
