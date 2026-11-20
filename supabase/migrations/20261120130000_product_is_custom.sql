-- Produto customizável (personalizado sob encomenda).
-- Usado pelo app para mostrar selos/itens na tela e, depois, para promoções e alertas.
-- Produtos existentes ficam como não customizáveis (false).
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

alter table public.products
  add column if not exists is_custom boolean not null default false;

comment on column public.products.is_custom is
  'true quando o produto é customizável (personalizado sob encomenda).';

commit;
