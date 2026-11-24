-- Carrinho por usuário: um registro por produto, com a quantidade.
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

create table public.cart_items (
  user_id    uuid not null references public.user_profiles(id) on delete cascade,
  product_id uuid not null references public.products(id) on delete cascade,
  quantity   int  not null check (quantity between 1 and 99),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  primary key (user_id, product_id)
);

create index cart_items_product on public.cart_items (product_id);

-- Mesmo padrão dos favoritos: RLS ligada e nenhuma policy, então anon e
-- authenticated não leem nem gravam via PostgREST; o BFF (postgres) acessa direto.
alter table public.cart_items enable row level security;

commit;
