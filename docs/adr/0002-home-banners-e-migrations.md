# ADR-0002: Banners da home em tabela própria e migrations versionadas

- **Status:** Proposta
- **Data:** 2026-10-15
- **Autor(es):** Thyago Casablancas (com Claude Code)

## Contexto
O carrossel principal da home tinha cards fixos no app. O time quer trocar banners sem publicar versão nova do app, e a home é vista também por visitantes (sem login). Até aqui não havia processo de migration: o `schema.sql` é um dump e mudanças eram aplicadas à mão sem registro.

## Decisão
1. Criar `public.home_banners` com: imagem (https), texto alternativo, no máximo um destino (rota interna **ou** URL externa https), `is_active`, `sort_order` e janela `starts_at`/`ends_at`. As regras ficam em CHECKs no banco e são repetidas no BFF.
2. RLS: leitura para `anon` e `authenticated` só do que está no ar; nenhuma policy de escrita (só a service role grava).
3. Bucket público `home-banners` no Storage (até 500 KB; WebP, JPEG ou PNG). Recomendação para quem sobe imagem: 1200×900 px (4:3), WebP até 300 KB, conteúdo importante nos 80% centrais.
4. O BFF expõe `GET /api/home/banners` (público, até 8 itens, `Cache-Control: public, max-age=300` e ETag).
5. Migrations passam a ser arquivos em `supabase/migrations/<timestamp>_<nome>.sql`, aplicados por uma pessoa (nunca pelo BFF; `ddl-auto` continua `none`). Seeds de dev ficam em `supabase/seed/` e nunca vão para o projeto compartilhado com produção.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Banners fixos no app | Zero backend | Troca exige release |
| Ler a tabela direto do app (PostgREST) | Sem endpoint | Foge do padrão "app só fala com o BFF"; regra de link só no app |
| Tabela + endpoint no BFF (escolhida) | Regras no banco e no BFF; cache HTTP | Mais um endpoint para manter |

## Consequências
- O que melhora: banners mudam sem release; links validados em duas camadas.
- O que piora ou vira risco: o painel para cadastrar banners não existe; por enquanto é SQL ou o Table Editor com a service role.
- Impacto no app Flutter / contrato OpenAPI: novo `GET /api/home/banners` (snake_case).
- Impacto em segurança e secrets: nenhum secret novo; leitura pública só do que está ativo e na janela.
- O que precisa ser feito depois: aplicar a migration (primeiro no Supabase local, depois em produção com aprovação); decidir o processo para produção (CLI `supabase db push` ou SQL Editor).

## Explicação simples
Antes, para trocar um banner da home era preciso publicar o app de novo. Agora os banners ficam numa tabela no banco, e o BFF entrega para o app só os que estão ligados e dentro das datas de exibição.

O banco recusa banner com link perigoso (só aceita `https://`) e com dois destinos ao mesmo tempo. O BFF confere de novo antes de responder. O app abre link externo no navegador do sistema, nunca dentro do app.

Mudanças de banco agora viram arquivos na pasta `supabase/migrations`, revisados no PR e aplicados por uma pessoa. Assim dá para saber o que mudou, quando e por quê.
