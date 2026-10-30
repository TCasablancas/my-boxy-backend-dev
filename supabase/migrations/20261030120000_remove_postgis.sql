-- Remove o PostGIS: as colunas de coordenada nunca receberam dado (o app não envia
-- latitude/longitude) e a consulta de raio do BFF não era usada. A cidade do usuário
-- é resolvida no aparelho. Leva junto spatial_ref_sys (gravável pelo anon) e
-- st_estimatedextent, apontadas pelos advisors.
-- Aplicar DEPOIS do deploy do BFF sem o campo location.
begin;

drop index if exists public.idx_user_addresses_location;
drop index if exists public.idx_orders_shipping_location;

alter table public.user_addresses drop column if exists location;
alter table public.orders drop column if exists shipping_location;

-- Sem cascade: se algo ainda depender do PostGIS, a migração falha em vez de apagar junto.
drop extension if exists postgis;

commit;
