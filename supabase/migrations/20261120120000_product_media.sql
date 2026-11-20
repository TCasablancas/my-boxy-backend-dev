-- Galeria de mídias do produto (ADR-0004).
-- Lista ordenada de imagens e vídeos para o carrossel da página do produto.
-- products.main_image continua existindo (capa usada nos cards e por apps antigos).
-- Aplicar à mão (primeiro no Supabase local, depois em produção com aprovação).
begin;

create table public.product_media (
  id            uuid primary key default gen_random_uuid(),
  product_id    uuid not null references public.products(id) on delete cascade,
  media_type    varchar(10) not null,
  url           text not null,
  -- Capa do vídeo (opcional; em imagem normalmente fica nulo).
  thumbnail_url text,
  -- Ordem no carrossel, começando em 0.
  position      smallint not null,
  created_at    timestamptz not null default now(),

  constraint product_media_type_check     check (media_type in ('IMAGE', 'VIDEO')),
  constraint product_media_position_check check (position >= 0),
  constraint product_media_url_not_blank  check (char_length(btrim(url)) > 0),
  -- Também serve de índice para buscar as mídias do produto já em ordem.
  constraint product_media_unique_position unique (product_id, position)
);

comment on table public.product_media is
  'Mídias do produto (imagens e vídeos) na ordem do carrossel.';

-- Leva a imagem atual de cada produto para a galeria, como primeira mídia.
insert into public.product_media (product_id, media_type, url, position)
select p.id, 'IMAGE', p.main_image, 0
from public.products p
where p.main_image is not null and btrim(p.main_image) <> '';

-- Acesso: mesmo padrão do catálogo. RLS ligada e nenhuma policy; o BFF lê direto.
alter table public.product_media enable row level security;

commit;
