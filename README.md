# my-boxy-backend-dev

## Railway

Configure these Railway service variables before deploying:

```text
SUPABASE_DB_USERNAME=<database username>
SUPABASE_DB_PASSWORD=<database password>
SUPABASE_JWT_ISSUER_URI=<Supabase JWT issuer URL>
PGCRYPTO_SECRET_KEY=<existing pgcrypto encryption key>
CORS_ALLOWED_ORIGINS=<comma-separated frontend origins>
```

`PGCRYPTO_SECRET_KEY` must be same key used to encrypt existing CPF values. Do not replace it with a newly generated key: previously stored CPF values would no longer decrypt.