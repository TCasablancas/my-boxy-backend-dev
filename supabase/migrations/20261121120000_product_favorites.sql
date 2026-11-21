-- Favoritos de produto, por usuário.
-- Substitui products.is_favorite (coluna global, igual para todo mundo). A coluna
-- antiga fica por enquanto para não quebrar o contrato; remover numa migration futura.
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

create table public.product_favorites (
  user_id    uuid not null references public.user_profiles(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, product_id)
);

create index product_favorites_product on public.product_favorites (product_id);

-- Mesmo padrão do catálogo e das avaliações: RLS ligada e nenhuma policy, então
-- anon e authenticated não leem nem gravam via PostgREST; o BFF (postgres) acessa direto.
alter table public.product_favorites enable row level security;

commit;
