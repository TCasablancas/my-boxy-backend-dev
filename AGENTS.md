# AGENTS.md — my-boxy-backend-dev

Fonte única de regras para agentes de IA (Claude, Gemini, Copilot, Codex etc.) neste repositório.

## Papel do repositório
BFF (Backend for Frontend) do **MyBoxy**, marketplace de produtos artesanais e manufaturados.
Fica entre o app Flutter (repo `my_boxy_flutter`) e o Supabase (Postgres, Auth, Storage).
O app fala só com este BFF; o BFF fala com Supabase, Stripe (pagamento) e Melhor Envio (frete).
Quem mantém é um dev mobile com pouco background em backend: explique decisões em linguagem simples.

## Stack
- Java 21, Spring Boot 4.1.1, Maven (use sempre o wrapper `./mvnw`).
- Spring Web MVC, Spring Security + OAuth2 Resource Server (JWT do Supabase), Data JPA/Hibernate (+ Spatial/JTS), Validation, Lombok.
- Postgres do Supabase via JDBC (`spring.datasource`). Testes usam H2 em modo PostgreSQL.
- springdoc-openapi: `/v3/api-docs` é o contrato que o Flutter usa para gerar o client.
- Deploy: Railway, conectado ao GitHub (branch `main`), build pelo `Dockerfile`. `render.yaml` é legado (Render) — não usar.

## Estrutura (`src/main/java/com/myboxydev/dev/`)
- `config/` — `SecurityConfig` (rotas públicas/protegidas, CORS), `SupabaseProperties`, `AppSecurityProperties`.
- `controllers/` — endpoints REST (`/api/...`). Só recebem, validam e delegam.
- `service/` — regras de negócio e clientes externos (`SupabaseAuthClient`, `StripePaymentService`, `MelhorEnvioService`).
- `repository/` — Spring Data JPA (inclui queries nativas com pgcrypto).
- `model/`, `domain/entity/`, `domain/enums/` — entidades mapeadas nas tabelas do Supabase.
- `dto/` (+ `dto/response/`) — records de entrada/saída da API. `mapper/` — conversão entidade ↔ DTO.
- `exception/` — exceções de negócio + `GlobalExceptionHandler` (resposta padrão `ApiErrorResponseDTO`).
- `specification/` — filtros dinâmicos (JPA Specifications). `util/` — utilitários (ex.: `CpfUtils`).
- `src/main/resources/application.yaml` — config; valores sensíveis vêm de variáveis de ambiente.
- `src/test/` — testes JUnit 5; `src/test/resources/application-test.yaml` (perfil `test`, H2, chaves falsas).
- `docs/STATUS.md` — estado do projeto. `docs/adr/` — decisões de arquitetura.

## Comandos
```bash
./mvnw -B test                 # roda todos os testes (não precisa de secrets)
./mvnw -B test -Dtest=NomeDaClasseTests   # roda uma classe de teste
./mvnw -B verify               # o mesmo que o CI roda
./mvnw -B package -DskipTests  # gera o jar (o Dockerfile faz isso)
./mvnw spring-boot:run         # sobe local; lê variáveis do arquivo .env (não versionado)
```
Contrato OpenAPI local: `http://localhost:8080/v3/api-docs` (Swagger UI em `/swagger-ui`).

## Convenções de código
- DTOs são `record` Java. Nunca devolva entidade JPA direto na API.
- Toda entrada tem `@Valid` no controller e anotações de validação no DTO (`@NotBlank`, `@Size`, `@Email`...).
- Injeção por construtor (`@RequiredArgsConstructor` do Lombok). Nada de `@Autowired` em campo no código de produção.
- Erros: lance exceções de `exception/` (ex.: `ResourceNotFoundException`, `BusinessRuleException`) e trate no `GlobalExceptionHandler`. Não monte JSON de erro no controller.
- Usuário logado: `@AuthenticationPrincipal Jwt jwt` + `UUID.fromString(jwt.getSubject())`. Nunca confie em user id vindo do body ou da URL.
- Rota nova: decida e declare explicitamente no `SecurityConfig` se é pública ou autenticada.
- JSON: mantenha o formato que cada endpoint já usa. Fluxos de cadastro/perfil novos usam snake_case via `@JsonProperty`.
- Leitura no banco: `@Transactional(readOnly = true)`. Escrita que mexe em várias tabelas: `@Transactional`.
- Chamadas externas usam o `RestTemplate` com timeout do `SecurityConfig`; trate 4xx/5xx do provedor e logue sem dados sensíveis.
- Comentários e mensagens de erro em português. Código (nomes de classes/métodos) em inglês.
- Testes: `@SpringBootTest` + `@ActiveProfiles("test")`; MockMvc para controller/segurança. Todo endpoint novo ou alterado ganha teste.

## Fluxo de trabalho para agentes
1. Leia este arquivo e `docs/STATUS.md` antes de começar.
2. Mudança que afeta o contrato com o app (path, campos, status HTTP): descreva o impacto no Flutter e peça aprovação.
3. Decisão de arquitetura (nova lib, novo serviço, mudança de auth, schema): crie ADR a partir de `docs/adr/0000-template.md`.
4. Antes de concluir: `./mvnw -B test` passando e `docs/STATUS.md` atualizado.
5. Não faça commit, push ou PR sem pedido explícito.

## Banco de dados e schema
- O schema pertence ao Supabase. `spring.jpa.hibernate.ddl-auto` fica `none` em produção.
- Ainda não existe processo versionado de migrations. Qualquer mudança de schema precisa de ADR + aprovação humana, e o SQL é aplicado por uma pessoa, não pelo agente.
- CPF é criptografado com pgcrypto (`PGCRYPTO_SECRET_KEY`) + hash SHA-256. Nunca logar CPF em texto.

## Variáveis de ambiente (valores só no Railway / `.env` local)
`SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, `SUPABASE_JWT_ISSUER_URI`, `SUPABASE_ANON_KEY`,
`SUPABASE_SERVICE_ROLE_KEY`, `SUPABASE_EMAIL_REDIRECT_URL`, `PGCRYPTO_SECRET_KEY`, `CORS_ALLOWED_ORIGINS`,
`STRIPE_SECRET_KEY`, `MELHOR_ENVIO_API_URL`, `MELHOR_ENVIO_TOKEN`, `MELHOR_ENVIO_ORIGIN_POSTAL_CODE`, `JPA_DDL_AUTO`.
Variável nova: adicionar em `.env.example` (com valor fictício) e no README.

## NÃO FAZER
- Não tocar em outros repositórios (incluindo `my_boxy_flutter`). Se precisar de mudança lá, descreva e peça.
- Não editar migrations/SQL já aplicados. Mudança de schema = novo script + ADR + aprovação.
- Não rodar SQL direto em produção (nem via JDBC, psql, MCP ou dashboard) e não mudar `ddl-auto` para `update`/`create`.
- Não expor a `SUPABASE_SERVICE_ROLE_KEY`: ela só existe no servidor, nunca vai em resposta, log, app ou teste com valor real.
- Não commitar secrets. Valores reais ficam só nas variáveis do Railway; `.env` não é versionado.
- Não alterar endpoints existentes sem manter compatibilidade com o app (não renomear/remover path, campo ou status). Use campo novo opcional ou nova versão de rota.
- Não trocar `PGCRYPTO_SECRET_KEY`: CPFs já gravados deixariam de ser decifrados.
- Não desabilitar testes nem segurança (`permitAll` amplo, CSRF/CORS abertos) para "fazer funcionar".
