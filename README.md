# my-boxy-backend-dev

## Railway

Configure these Railway service variables before deploying:

```text
SUPABASE_DB_USERNAME=<database username>
SUPABASE_DB_PASSWORD=<database password>
SUPABASE_JWT_ISSUER_URI=<Supabase JWT issuer URL>
PGCRYPTO_SECRET_KEY=<existing pgcrypto encryption key>
CORS_ALLOWED_ORIGINS=<comma-separated frontend origins>
SUPABASE_ANON_KEY=<Supabase anon/public key, used by login and signup>
SUPABASE_SERVICE_ROLE_KEY=<Supabase service role key, server-side only>
SUPABASE_EMAIL_REDIRECT_URL=<optional deep link opened by the confirmation e-mail>
```

`SUPABASE_ANON_KEY` and `SUPABASE_SERVICE_ROLE_KEY` are validated at startup: the
app does not boot without them. The service role key is used only to delete the
Supabase Auth user when the profile cannot be persisted during signup. Never ship
it to the app.

Signup `409` responses are logged with their origin (`perfil existente`,
`Auth: email_exists` or `Auth: identities vazio`). When Supabase rejects an e-mail
that has no `user_profiles` row, the log line says `conta órfã em auth.users id=<uuid>`.

`JPA_DDL_AUTO` defaults to `none`: the schema is owned by Supabase.

## User signup

`POST /api/auth/signup` (public) creates the user in Supabase Auth, then persists
`user_profiles` (CPF encrypted with pgcrypto + SHA-256 hash) and the primary
`user_addresses` row in one transaction. Returns `201` with the profile in snake_case.
Errors: `400` validation (field names in snake_case), `409` e-mail/alias/CPF already
in use, `429` rate limited by Supabase, `502/503` auth provider failure.

`PGCRYPTO_SECRET_KEY` must be same key used to encrypt existing CPF values. Do not replace it with a newly generated key: previously stored CPF values would no longer decrypt.