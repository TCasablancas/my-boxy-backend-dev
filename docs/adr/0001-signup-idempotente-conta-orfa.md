# ADR-0001: Cadastro retoma conta órfã quando a senha confere

- **Status:** Proposta
- **Data:** 2026-10-15
- **Autor(es):** Thyago Casablancas (com Claude Code)

## Contexto
O cadastro (`POST /api/auth/signup`) é feito em dois passos que não cabem numa transação única: criar o usuário no Supabase Auth e depois gravar `user_profiles` + endereço no Postgres. Se o segundo passo falha e a compensação (apagar o usuário do Auth) também falha, ou se o Auth cria o usuário mas não responde a tempo, sobra uma conta "órfã" em `auth.users` sem perfil. Hoje o Supabase de dev/prod tem uma dessas (criada em 2026-10-08).

Antes desta decisão, qualquer nova tentativa com aquele e-mail recebia 409 para sempre: o Auth diz que o e-mail existe, mas o app não tem perfil nem consegue usar a conta.

## Decisão
1. Quando o Auth responde que o e-mail já existe e **não** existe perfil para aquele id, o BFF confere a senha com um login por senha (`/token?grant_type=password`, anon key). Se o id devolvido é o da conta órfã, o cadastro continua com esse id e grava o perfil. Se a senha não confere (ou o e-mail ainda não foi confirmado), responde 409 como antes.
2. Quando o `/signup` do Auth falha por rede/timeout, o BFF procura um usuário com aquele e-mail criado nos últimos 5 minutos e sem perfil, apaga e responde 503.
3. A compensação (`deleteUser`) tenta duas vezes e, se falhar, loga `CONTA ÓRFÃ` com o id.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Manter 409 e limpar órfãos à mão | Nenhum código novo | Usuário fica travado até alguém rodar SQL; não escala |
| Apagar a conta órfã e recriar | Simples | Qualquer pessoa que saiba o e-mail apagaria a conta de outra; envia novo e-mail de confirmação (gasta a cota) |
| Criar o usuário via `/admin/users` (service role) | Sem e-mail de confirmação no caminho crítico | Muda o fluxo de confirmação de e-mail do produto; decisão maior, fica para outro ADR |
| **Retomar se a senha confere (escolhida)** | Só o dono da conta retoma; não envia e-mail | Uma chamada extra ao Auth nesse caso raro; não cobre órfão com e-mail não confirmado (nesse caso o próprio Supabase reenvia a confirmação no `/signup`) |

## Consequências
- O que melhora: repetir o cadastro depois de uma falha não trava mais o e-mail em 409.
- O que piora ou vira risco: o login de conferência conta no limite de requisições do Auth (`over_request_rate_limit`); se estourar, o BFF devolve 429 com `Retry-After`.
- Impacto no app Flutter / contrato OpenAPI: nenhum path ou campo novo. 409 agora traz o campo em `fieldErrors`; 429 sempre traz `Retry-After`.
- Impacto em segurança e secrets: a senha só é repassada ao Supabase (como já acontece no `/signup`) e nunca é logada. A sessão do login de conferência é descartada.
- O que precisa ser feito depois: SMTP próprio no Supabase; decidir o destino da conta órfã existente.

## Explicação simples
Pense no cadastro como um "salvar" em dois lugares: primeiro no serviço de login (Supabase Auth), depois no nosso banco. Se o segundo salvar falha e não conseguimos desfazer o primeiro, a pessoa fica com meio cadastro: o login conhece o e-mail, mas o app não tem o perfil dela.

Antes, quando ela tentava de novo, o BFF dizia "e-mail já cadastrado" e pronto, sem saída. Agora o BFF pergunta ao Supabase "essa senha abre essa conta?". Se abre, é a mesma pessoa terminando o que começou, e o BFF só completa o perfil. Se não abre, pode ser outra pessoa usando o e-mail de alguém, e a resposta continua sendo "e-mail já cadastrado".
