# Copilot — my-boxy-backend-dev (versão resumida de AGENTS.md)

BFF Java do MyBoxy (marketplace artesanal) entre o app Flutter `my_boxy_flutter` e o Supabase (Postgres, Auth, Storage).
Deploy no Railway via `Dockerfile` (branch `main`). Regras completas: `AGENTS.md`. Estado: `docs/STATUS.md`.

## Stack e comandos
- Java 21, Spring Boot 4.1.1, Maven (`./mvnw`), Spring Security + JWT do Supabase, Data JPA, Lombok, springdoc-openapi.
- Testes: `./mvnw -B test` (H2 + perfil `test`, sem secrets). CI: `./mvnw -B verify`.
- Pacotes em `com.myboxydev.dev`: `controllers`, `service`, `repository`, `model`/`domain`, `dto`, `exception`, `config`.

## Gatilhos
- `/criar-classe`: criar DTO, Service, Controller etc. seguindo as convenções abaixo.
- `/alterar-classe`: refatorar ou corrigir mantendo compatibilidade com o app.
- `/investigar`: diagnóstico com evidência (Causa raiz -> Impacto -> 2 soluções com prós/contras).

## Convenções
- DTOs como `record`; nunca expor entidade JPA. `@Valid` em toda entrada.
- Injeção por construtor (`@RequiredArgsConstructor`). Erros via exceções de `exception/` + `GlobalExceptionHandler`.
- Usuário logado só via `jwt.getSubject()`; nunca confiar em id do body/URL.
- Rota nova declarada explicitamente no `SecurityConfig` (pública ou autenticada).
- `@Transactional(readOnly = true)` em leituras. Endpoint novo/alterado ganha teste.
- Comentários e mensagens em português; explicar decisões em linguagem simples.

## NÃO FAZER
- Tocar em outros repositórios.
- Editar migrations/SQL já aplicados ou mudar `ddl-auto` de `none`.
- Rodar SQL direto em produção.
- Expor `SUPABASE_SERVICE_ROLE_KEY` (resposta, log, app, teste).
- Commitar secrets: valores reais só nas variáveis do Railway; `.env` fora do git.
- Alterar endpoints existentes sem manter compatibilidade com o app.
- Trocar `PGCRYPTO_SECRET_KEY` ou logar CPF.
