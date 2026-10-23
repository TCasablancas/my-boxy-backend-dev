-- Controle de acesso: fecha a superfície anon do PostgREST.
-- O BFF fala com o Postgres direto (JPA) e o app nunca usa a anon key para dados:
-- catálogo continua sem policies (público só via BFF) e dados privados só para authenticated.
begin;

-- "BFF possui acesso total" era FOR ALL para PUBLIC (inclui anon) com USING true:
-- qualquer um com a anon key lia/escrevia todos os pedidos. Service role já ignora RLS.
drop policy if exists "BFF possui acesso total a pedidos" on public.orders;
drop policy if exists "BFF possui acesso total aos itens do pedido" on public.order_items;

-- Leituras do comprador/lojista: só authenticated.
alter policy "Comprador pode visualizar seus próprios pedidos" on public.orders to authenticated;
alter policy "Lojista pode visualizar pedidos recebidos por sua loja" on public.orders to authenticated;
alter policy "Comprador visualiza itens dos seus pedidos" on public.order_items to authenticated;
alter policy "Lojista visualiza itens dos pedidos da sua loja" on public.order_items to authenticated;

alter policy "select own addresses" on public.user_addresses to authenticated;
alter policy "insert own addresses" on public.user_addresses to authenticated;
alter policy "update own addresses" on public.user_addresses to authenticated;
alter policy "delete own addresses" on public.user_addresses to authenticated;

-- Utilitário de DDL não deve ser chamável pela API.
revoke execute on function public.rls_auto_enable() from public, anon, authenticated;

commit;
