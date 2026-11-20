# ADR-0004: Galeria de mídias do produto (imagens e vídeos)

- **Status:** Proposta (migration ainda não aplicada)
- **Data:** 2026-11-20
- **Autor(es):** Thyago Casablancas (com Claude Code)

## Contexto
A página do produto mostra um carrossel com várias imagens e também vídeos. Hoje `products` só tem `main_image` (uma URL), então a API entrega uma imagem só.

## Decisão
- **Tabela `product_media`** (migration `supabase/migrations/20261120120000_product_media.sql`): `product_id`, `media_type` (`IMAGE | VIDEO`), `url`, `thumbnail_url` opcional (capa do vídeo) e `position` (ordem no carrossel, a partir de 0, única por produto). RLS ligada sem policies, igual ao catálogo.
- A migration copia a `main_image` de cada produto para a galeria como posição 0.
- `products.main_image` **continua** como capa (cards da listagem e apps antigos). Não removemos nada.
- **API:** `GET /api/products/{id}` ganha o campo novo `media` (lista em ordem):
  ```json
  "media": [
    { "type": "IMAGE", "url": "https://.../1.webp", "thumbnailUrl": null },
    { "type": "VIDEO", "url": "https://.../2.mp4", "thumbnailUrl": "https://.../2.webp" }
  ]
  ```
  camelCase, como o resto desse endpoint. Produto sem linhas em `product_media` devolve a `main_image` como única imagem, então o app nunca recebe lista vazia se houver capa.

## Consequências
- Mudança só aditiva: nenhum campo removido ou renomeado, app atual continua funcionando.
- Sem a migration aplicada, `GET /api/products/{id}` responde 500 (tabela inexistente). Aplicar antes do deploy.
- Fase 2: upload/edição de mídias pela área do vendedor (bucket no Storage) e regra de manter `main_image` igual à mídia de posição 0.
