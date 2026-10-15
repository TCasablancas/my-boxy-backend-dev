# Status do projeto

Última atualização: 2026-10-15 (correção do 409/429 no cadastro).
Agentes: atualizem este arquivo ao terminar uma tarefa.

## Feito
- **Catálogo público:** produtos (`GET /api/products`, `/{id}`, `/{id}/related`, filtros via `ProductSpecifications`), lojas (`/api/stores`), categorias (`/api/categories`), menu (`/api/menu`).
- **Auth via BFF (Supabase Auth):** `POST /api/auth/login`, `POST /api/auth/signup` (cria usuário no Auth + `user_profiles` com CPF já cifrado no INSERT + endereço principal numa transação; desfaz no Auth com service role, com 1 nova tentativa, se o perfil falhar; repetir o cadastro de conta órfã com a mesma senha conclui o perfil; se o Auth não responder, remove o usuário criado nesse meio-tempo antes do 503), `POST /api/auth/set-password`.
  - Checagem prévia de disponibilidade: `/api/auth/signup/check-alias` e `/api/auth/signup/check-cpf`.
  - Erros mapeados: 400 (campos em snake_case), 409 só para conflito real (e-mail/alias/CPF em uso, com o campo em `fieldErrors`), 429 (rate limit do Supabase, sempre com `Retry-After`: o do Supabase ou 600 s para e-mail / 60 s para requisições), 500 (falha ao gravar o perfil que não é conflito), 502/503 (falha do provedor).
  - `POST /api/auth/register` está **deprecated** (substituído por `/signup`).
- **Perfil do usuário (autenticado):** `GET/PUT /api/user_profiles/me`, `PUT /me/alias`, `PUT /me/document` (CPF criptografado com pgcrypto + hash).
- **Endereços (autenticado):** `GET/POST /api/v1/me/addresses`, `DELETE /{addressId}`, `PUT /{addressId}/set-primary`.
- **Frete:** `POST /api/shipping/calculate` (Melhor Envio).
- **Checkout de visitante:** `POST /api/checkout/express-guest` (Stripe + Melhor Envio).
- **Segurança:** JWT do Supabase validado pelo `issuer-uri`, CORS por variável, chaves do Supabase validadas no boot.
- **Testes:** 10 classes (signup, disponibilidade, cliente Supabase Auth, propriedades, segurança, mapeamento de entidades, contexto, contrato OpenAPI). `./mvnw -B test` em 2026-10-15: 46 testes, 0 falhas. `EntityMappingTests` confere que as colunas NOT NULL de `user_profiles` no Supabase também são obrigatórias na entidade (o H2 dos testes nasce da entidade).
- **Contrato OpenAPI:** `/v3/api-docs` funciona (springdoc 2.7.0 com Boot 4.1.1), lista os 22 paths e respeita os nomes snake_case; coberto por `OpenApiDocsTests`.
- **Deploy:** Railway via `Dockerfile` (branch `main`).

## Em andamento
- Signup 409/429 (branch `fix/signup-409-429`): causa do 409 era o INSERT de `user_profiles` sem `cpf_encrypted`/`has_cpf_registered` (NOT NULL no banco), mapeado como conflito. O 429 é a cota de e-mails do SMTP padrão do Supabase (1–2/h no projeto todo), gasta por esses cadastros que falhavam. Pendente: SMTP próprio + limite de envio no painel do Supabase.
- Configuração para trabalho com agentes de IA (AGENTS.md, CI, contrato OpenAPI).

## Próximo
- **Contrato OpenAPI:** falta declarar o esquema de auth Bearer (JWT), título/versão da API e `application/json` como tipo de resposta (hoje `*/*`), para o gerador do Flutter. springdoc 2.x é oficialmente para Boot 3: planejar upgrade para 3.x.
- **CI:** workflow de testes em todo PR (`.github/workflows/ci.yml`) — ativar como check obrigatório na branch `main` no GitHub.
- **Processo de migrations:** hoje não existe. Definir (ex.: Supabase CLI com `supabase/migrations` versionado) e registrar em ADR.
- Área do vendedor (`/api/seller/**` está protegida no `SecurityConfig`, mas ainda não há controller) e checkout autenticado (`/api/checkout/**`).

## Pontos de atenção (encontrados no levantamento, ainda não corrigidos)
- `AuthService.login` cria um perfil padrão sem CPF quando o usuário do Auth não tem perfil; com `cpf_encrypted`/`cpf_hash` NOT NULL esse insert falha. O mesmo vale para o `register` (deprecated).
- Dev e prod usam o mesmo projeto Supabase: testes de cadastro gastam a cota de e-mail de usuários reais.
- Funções do pgcrypto ficam no schema `extensions`; as queries do BFF agora usam `extensions.pgp_sym_*` explicitamente.
- `application.yaml` tem valor padrão para `PGCRYPTO_SECRET_KEY` e `SUPABASE_JWT_SECRET`. Se a variável faltar no Railway, o app sobe com uma chave errada em vez de falhar. Ideal: remover o default e validar no boot.
- `SecurityConfig` libera `GET /api/users/check-alias`, mas o endpoint real é `GET /api/user_profiles/users/check-alias` — hoje ele exige login. Confirmar se o app usa esse endpoint deslogado.
- `show-sql: true` e `format_sql: true` também em produção: log muito verboso e pode expor dados de queries.
- CORS não permite `PATCH` (hoje nenhum endpoint usa, mas vale saber).
- `render.yaml` é legado do Render; avaliar remoção (contém o host do banco).
- A pasta `target/` (saída do build, incluindo um jar de 66 MB) está versionada no git: todo build gera diff. Ideal: `git rm -r --cached target` e adicionar `target/` ao `.gitignore`.
- `target/surefire-reports` tem relatório de `TmpLocationTypeProbeTests`, classe que não existe mais (resto de build antigo; some com `./mvnw clean`).
