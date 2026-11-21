# ADR-0005: Usuários de teste pelo BFF e hash do CPF com pepper

- **Status:** Aceita
- **Data:** 2026-11-21
- **Autor(es):** ThiagoACSilva (decisão), Claude (proposta)

## Contexto
Criar usuário de teste no painel do Supabase é difícil: `user_profiles.id` é FK para `auth.users.id` (não pode ter id gerado à parte) e o CPF precisa ir cifrado (`cpf_encrypted`) e com hash (`cpf_hash`), mas a chave do pgcrypto só existe no BFF. Na mesma análise, o `cpf_hash` era SHA-256 puro: com ~1 bilhão de CPFs possíveis, um vazamento da tabela revela todos os CPFs por força bruta.

## Decisão
1. Endpoint só de dev `POST /api/dev/test-users`, desligado por padrão (`DEV_TOOLS_ENABLED`), protegido por `X-Dev-Tools-Token` e bloqueado no Railway `production`. Cria o usuário pela API admin do Auth já confirmado e reaproveita o fluxo do signup.
2. `cpf_hash` = HMAC-SHA256 com `CPF_HASH_PEPPER` (segredo separado da chave do pgcrypto). Hashes existentes recalculados por um SQL rodado uma vez no SQL Editor.

## Alternativas consideradas
| Alternativa | Prós | Contras |
|---|---|---|
| Trigger no banco com a chave no Vault | Cria tudo pelo painel | Chave mora junto dos dados; CPF em claro no INSERT/logs |
| Função SQL recebendo a chave como argumento | Nada guardado no banco | Ainda exige criar o usuário no Auth antes; chave passa pelo SQL Editor sempre |
| Endpoint de dev no BFF (escolhida) | Chave nunca sai do BFF; sem cota de e-mail; reaproveita o signup | Precisa rodar o BFF local com a flag |
| Projeto Supabase separado para dev | Separa dados de teste dos reais | Mais setup; fica como próximo passo |

## Consequências
- Melhora: usuário de teste em uma chamada; `cpf_hash` não revela CPF sem o pepper.
- Risco: trocar o `CPF_HASH_PEPPER` exige recalcular todos os hashes. O endpoint de dev nunca pode ser ligado em produção.
- App Flutter: nenhum impacto (rota nova só de dev; `/api/auth/register`, não usado pelo app, foi removido).
- Depois: criar `CPF_HASH_PEPPER` no Railway e rodar o SQL de recálculo junto com o deploy.

## Explicação simples
É como ter um "debug menu" no app: uma tela escondida, só no build de dev, que cria um usuário pronto para testar. Aqui é uma rota do BFF que só existe quando você liga a flag localmente e pede uma senha no header.

O hash com pepper funciona como um hash com uma senha secreta misturada. Sem essa senha, quem roubar a tabela não consegue testar CPF por CPF para descobrir de quem é cada linha.
