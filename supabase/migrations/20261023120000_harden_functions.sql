-- Endurece funções apontadas pelos advisors de segurança.
begin;

-- st_estimatedextent (PostGIS) é SECURITY DEFINER e ficava exposta em /rest/v1/rpc
-- para anon e authenticated. Nem o app nem o BFF (conecta como postgres) usam via PostgREST.
-- Atenção: sem efeito no Supabase hospedado. Os grants foram dados por supabase_admin
-- (dono do PostGIS) e o postgres só revoga o que ele mesmo concedeu. Correção real:
-- PostGIS fora do schema public (advisor extension_in_public).
revoke execute on function public.st_estimatedextent(text, text) from public, anon, authenticated;
revoke execute on function public.st_estimatedextent(text, text, text) from public, anon, authenticated;
revoke execute on function public.st_estimatedextent(text, text, text, boolean) from public, anon, authenticated;

-- Trigger de updated_at com search_path fixo. O corpo só usa NOW() (pg_catalog,
-- sempre resolvido), então search_path vazio não muda o comportamento.
alter function public.update_orders_updated_at_column() set search_path = '';

commit;
