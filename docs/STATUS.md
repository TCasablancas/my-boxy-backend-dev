# Status do projeto

Última atualização: 2026-10-14 (levantamento inicial feito a partir do código).
Agentes: atualizem este arquivo ao terminar uma tarefa.

## Feito
- **Catálogo público:** produtos (`GET /api/products`, `/{id}`, `/{id}/related`, filtros via `ProductSpecifications`), lojas (`/api/stores`), categorias (`/api/categories`), menu (`/api/menu`).
- **Auth via BFF (Supabase Auth):** `POST /api/auth/login`, `POST /api/auth/signup` (cria usuário no Auth + `user_profiles` + endereço principal numa transação; desfaz no Auth com service role se o perfil falhar), `POST /api/auth/set-password`.
  - Checagem prévia de disponibilidade: `/api/auth/signup/check-alias` e `/api/auth/signup/check-cpf`.
  - Erros mapeados: 400 (campos em snake_case), 409 (e-mail/alias/CPF em uso, com log da origem), 429 (rate limit do Supabase), 502/503 (falha do provedor).
  - `POST /api/auth/register` está **deprecated** (substituído por `/signup`).
- **Perfil do usuário (autenticado):** `GET/PUT /api/user_profiles/me`, `PUT /me/alias`, `PUT /me/document` (CPF criptografado com pgcrypto + hash).
- **Endereços (autenticado):** `GET/POST /api/v1/me/addresses`, `DELETE /{addressId}`, `PUT /{addressId}/set-primary`.
- **Frete:** `POST /api/shipping/calculate` (Melhor Envio).
- **Checkout de visitante:** `POST /api/checkout/express-guest` (Stripe + Melhor Envio).
- **Segurança:** JWT do Supabase validado pelo `issuer-uri`, CORS por variável, chaves do Supabase validadas no boot.
- **Testes:** 10 classes (signup, disponibilidade, cliente Supabase Auth, propriedades, segurança, mapeamento de entidades, contexto, contrato OpenAPI). `./mvnw -B verify` em 2026-10-14: 32 testes, 0 falhas.
- **Contrato OpenAPI:** `/v3/api-docs` funciona (springdoc 2.7.0 com Boot 4.1.1), lista os 22 paths e respeita os nomes snake_case; coberto por `OpenApiDocsTests`.
- **Deploy:** Railway via `Dockerfile` (branch `main`).

## Em andamento
- Robustez do signup: diagnóstico de 409 e tratamento de 429 (commits mais recentes).
- Configuração para trabalho com agentes de IA (AGENTS.md, CI, contrato OpenAPI).

## Próximo
- **Contrato OpenAPI:** falta declarar o esquema de auth Bearer (JWT), título/versão da API e `application/json` como tipo de resposta (hoje `*/*`), para o gerador do Flutter. springdoc 2.x é oficialmente para Boot 3: planejar upgrade para 3.x.
- **CI:** workflow de testes em todo PR (`.github/workflows/ci.yml`) — ativar como check obrigatório na branch `main` no GitHub.
- **Processo de migrations:** hoje não existe. Definir (ex.: Supabase CLI com `supabase/migrations` versionado) e registrar em ADR.
- Área do vendedor (`/api/seller/**` está protegida no `SecurityConfig`, mas ainda não há controller) e checkout autenticado (`/api/checkout/**`).

## Pontos de atenção (encontrados no levantamento, ainda não corrigidos)
- `application.yaml` tem valor padrão para `PGCRYPTO_SECRET_KEY` e `SUPABASE_JWT_SECRET`. Se a variável faltar no Railway, o app sobe com uma chave errada em vez de falhar. Ideal: remover o default e validar no boot.
- `SecurityConfig` libera `GET /api/users/check-alias`, mas o endpoint real é `GET /api/user_profiles/users/check-alias` — hoje ele exige login. Confirmar se o app usa esse endpoint deslogado.
- `show-sql: true` e `format_sql: true` também em produção: log muito verboso e pode expor dados de queries.
- CORS não permite `PATCH` (hoje nenhum endpoint usa, mas vale saber).
- `render.yaml` é legado do Render; avaliar remoção (contém o host do banco).
- A pasta `target/` (saída do build, incluindo um jar de 66 MB) está versionada no git: todo build gera diff. Ideal: `git rm -r --cached target` e adicionar `target/` ao `.gitignore`.
- `target/surefire-reports` tem relatório de `TmpLocationTypeProbeTests`, classe que não existe mais (resto de build antigo; some com `./mvnw clean`).
