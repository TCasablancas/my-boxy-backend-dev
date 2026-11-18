# ADR-0003: Avaliações de produto e loja (lista, resumo por nota, curtidas e fotos)

- **Status:** Aceita (aprovada em 2026-11-18; migration ainda não aplicada)
- **Data:** 2026-11-18
- **Autor(es):** Thyago Casablancas (com Claude Code)

## Contexto
A página do produto precisa de uma seção de avaliações com:
1. Resumo das 5 notas em barras (quantas avaliações em cada nota).
2. Lista infinita de comentários, com filtro por uma ou mais notas.
3. Card de comentário com nome, foto e alias do autor (já em `user_profiles`, e o DS tem `MBUserHeaderContainer`), comentário, nota, número de curtidas e número de fotos enviadas.

Depois a mesma coisa vai valer para a página da loja, então as peças precisam ser genéricas desde já.
Hoje só existe `products.reviews` (um inteiro solto) e `stores.rating`, sem tabela de avaliações.

## Decisão

### 1. Banco (migration `supabase/migrations/20261118120000_reviews.sql`)
- **`reviews`**: uma tabela para os dois alvos. `store_id` é sempre preenchido; `product_id` preenchido = avaliação do produto, nulo = avaliação da loja.
  - O trigger copia `store_id` do produto (o cliente nunca escolhe a loja).
  - `rating` 1–5, `comment` até 2000 caracteres, `status` `PUBLISHED | HIDDEN | REMOVED` (moderação), `order_item_id` opcional (compra verificada), `edited_at` automático.
  - Uma avaliação por pessoa por produto, e uma "da loja" por pessoa por loja (índices únicos parciais).
  - `likes_count` e `photos_count` são contadores mantidos por trigger, para a lista não fazer `COUNT` por card.
- **`review_photos`**: até 5 fotos por avaliação (`position` 0–4 + `CHECK photos_count <= 5`). Guarda só o caminho no bucket, nunca URL completa.
- **`review_likes`**: PK `(review_id, user_id)` (curtir duas vezes é impossível); trigger impede curtir a própria avaliação.
- **Índices parciais** (`status = 'PUBLISHED'`) para a lista por data e para a lista/resumo por nota, de produto e de loja.
- **RLS ligada sem policies** nas três tabelas, igual ao catálogo: nada via PostgREST/anon key; o BFF lê direto.

Testado num Postgres 16 local com tabelas de apoio: store_id corrigido pelo trigger, contadores sobem e descem, 6ª foto, autocurtida, avaliação duplicada, nota 6 e caminho `https://` são recusados.

### 2. Contrato da API (novo, snake_case, prefixo `/api/v1`)
Leitura pública (visitante vê avaliações), com `Cache-Control: public, max-age=60`. Em GET público o BFF ignora o token (regra do `SecurityConfig`, para token vencido não derrubar o catálogo), então a lista não diz se quem está vendo já curtiu. Isso vem na fase 2, numa rota autenticada (ex.: `GET /api/v1/me/review-likes?review_ids=...`), sem mexer no resolver de token.

`GET /api/v1/products/{productId}/reviews/summary`
```json
{
  "average_rating": 4.6,
  "total_reviews": 128,
  "distribution": [
    { "rating": 5, "count": 96, "percentage": 75.0 },
    { "rating": 4, "count": 20, "percentage": 15.6 },
    { "rating": 3, "count": 8,  "percentage": 6.3 },
    { "rating": 2, "count": 3,  "percentage": 2.3 },
    { "rating": 1, "count": 1,  "percentage": 0.8 }
  ]
}
```
Sempre 5 itens, de 5 a 1, mesmo com `count` 0 (o app desenha as barras sem tratar buraco). Produto sem avaliações: `total_reviews: 0`, `average_rating: null`. Produto inexistente: 404.

`GET /api/v1/products/{productId}/reviews?ratings=5,4&limit=20&cursor=<opaco>`
```json
{
  "items": [
    {
      "id": "uuid",
      "rating": 5,
      "comment": "Chegou rápido e é lindo.",
      "created_at": "2026-11-10T14:03:00Z",
      "edited": false,
      "verified_purchase": true,
      "likes_count": 12,
      "photos_count": 3,
      "author": { "display_name": "Ana S.", "alias": "ana.ceramica", "avatar_url": "https://..." }
    }
  ],
  "next_cursor": "eyJ0IjoiMjAyNi0xMS0xMFQxNDowMzowMFoiLCJpIjoiLi4uIn0",
  "has_more": true
}
```
- `ratings` opcional, lista de 1–5 separada por vírgula; inválido → 400 com `fieldErrors`.
- `limit` padrão 20, máximo 50.
- Paginação por cursor (`created_at` + `id`), não por página/offset: comentário novo entrando no topo não duplica nem pula itens no scroll infinito. Fim da lista: `next_cursor: null`, `has_more: false`.
- Ordem: mais recentes primeiro. "Mais úteis" (por curtidas) pode virar `sort=helpful` depois, sem quebrar o contrato.
- Autor com perfil `ANONYMIZED`: `display_name: "Usuário MyBoxy"`, `alias` e `avatar_url` nulos.

Loja (fase seguinte, mesmo DTO): `GET /api/v1/stores/{storeId}/reviews` e `/reviews/summary`, que juntam avaliações dos produtos da loja e as avaliações diretas da loja.

Rotas de escrita (fase 2, autenticadas): curtidas do usuário logado, `POST /api/v1/products/{productId}/reviews`, `PUT/DELETE /api/v1/reviews/{id}`, `PUT/DELETE /api/v1/reviews/{id}/like`, upload de fotos (bucket `review-photos`) e `GET /api/v1/reviews/{id}/photos`.

### 3. Estrutura no BFF
`ReviewTarget` (`PRODUCT`, `STORE`) + um `ReviewService` único com `getSummary(target, id)` e `list(target, id, ratings, cursor, limit, viewerId)`. Os controllers de produto e loja só montam o alvo e delegam. Queries nativas no `ReviewRepository` (agregação por nota e lista com keyset).

### 4. Saída no Flutter (`my_boxy_flutter`, depois do contrato publicado)
Feature global `lib/features/reviews/`, parametrizada por `ReviewTarget`:
- `data/repositories/review_repository.dart` usando o client gerado do OpenAPI.
- `presentation/view_models/review_list_view_model.dart` (`ChangeNotifier`): resumo, itens, `Set<int> selectedRatings`, `loadMore()` com trava contra chamadas duplicadas, `refresh()`, estados loading/erro/vazio; trocar o filtro zera o cursor.
- Widgets: barras de distribuição (tocar numa barra liga/desliga a nota no filtro) e chips de nota.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Tabelas separadas `product_reviews` e `store_reviews` | Esquema óbvio | Duplica triggers, curtidas, fotos e código; loja precisa de UNION para juntar |
| Coluna polimórfica `target_type` + `target_id` | Genérico | Sem chave estrangeira real: avaliação de produto apagado fica órfã |
| Uma tabela com `store_id` obrigatório + `product_id` opcional (escolhida) | Chaves estrangeiras reais; loja e produto usam os mesmos índices e o mesmo código | Regra "store_id vem do produto" depende do trigger |
| Tabela de resumo (contagem por nota) mantida por trigger | Resumo instantâneo em qualquer escala | Mais um trigger para manter consistente; desnecessário no volume da v1 |
| Paginação por offset (`page=3`) | Simples | Itens repetidos/pulados quando chega avaliação nova durante o scroll |

## Consequências
- O que melhora: avaliações reais no lugar de números fixos; o mesmo código serve produto e loja.
- O que piora ou vira risco: o resumo é calculado na hora (`GROUP BY` no índice). Se um produto passar de dezenas de milhares de avaliações, criar a tabela de resumo.
- `products.reviews` e `stores.rating` continuam existindo e **não** são atualizados por esta migration. Decidir se passam a ser derivados de `reviews` (trigger) ou se o app passa a ler do `/summary`.
- Impacto no app Flutter / contrato OpenAPI: 2 rotas novas, nenhuma alterada.
- Impacto em segurança e secrets: nenhum secret novo. Rotas GET públicas declaradas no `SecurityConfig`. O BFF nunca devolve `author_id`, e-mail ou nome completo.
- O que precisa ser feito depois: aplicar a migration (local, depois produção); fase 2 (escrita, curtidas, fotos e bucket); moderação (`HIDDEN`/`REMOVED`).

## Explicação simples
Toda avaliação mora numa tabela só. Se ela tem produto, é avaliação do produto; se não tem, é da loja. Como a loja de cada avaliação é sempre preenchida, a página da loja consegue mostrar tudo com uma consulta, do mesmo jeito que a do produto.

Curtidas e fotos ficam em tabelas próprias, e o banco mantém um contador em cada avaliação, como um `ValueNotifier` que se atualiza sozinho. Assim o card mostra "12 curtidas, 3 fotos" sem contar nada na hora.

O scroll infinito usa um "marcador" (cursor) em vez de número de página: o app devolve o marcador do último item que recebeu e o BFF continua dali. Se alguém publicar uma avaliação nova enquanto você rola, a lista não repete nem pula itens.
