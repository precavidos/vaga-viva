# Como publicar a API do Vaga Viva

A API roda no Render a partir do `Dockerfile` da raiz, com o PostgreSQL no Neon. A cada push no `main`, o CI roda os testes e, só se passarem, chama o *deploy hook* do Render. Só uma pessoa do time precisa fazer isto, uma vez.

Os planos gratuitos bastam até a semana 4. No gratuito do Render a API dorme após um tempo sem uso e leva alguns segundos para acordar. Antes da submissão, troque para um plano pago que não dorme (cronograma, semana 4). Confira os limites atuais de cada plano no site do Render e do Neon.

## 1. Banco no Neon

1. Crie um projeto em <https://neon.tech> com estas opções:

   | Campo | Valor |
   | --- | --- |
   | Project name | `vaga-viva` |
   | Postgres version | `17` (a mesma do `compose.yaml` e dos testes) |
   | Cloud provider / Region | `AWS` · `US East (N. Virginia)`, a mesma região do Render |
   | Database name | `vagaviva` |
   | Neon Auth | desligado |

   O Render não tem região no Brasil; Virginia é a mais próxima entre as que ele oferece, e banco e API na mesma região evitam latência em cada consulta.
2. Em *Connect*, escolha a branch `production` (ou `main`), o banco `vagaviva` e **desligue *Connection pooling***. A API já tem seu próprio pool (Hikari), e o Flyway não funciona bem atrás do pooler do Neon (o host com `-pooler` no nome). Copie a string de conexão direta. Ela vem assim:

   ```
   postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require
   ```

3. O Spring espera o formato JDBC, com usuário e senha separados. Monte as três variáveis assim:

   | Variável | Valor |
   | --- | --- |
   | `DATABASE_URL` | `jdbc:postgresql://HOST/BANCO?sslmode=require` |
   | `DATABASE_USERNAME` | `USUARIO` |
   | `DATABASE_PASSWORD` | `SENHA` |

As tabelas são criadas pelo Flyway na primeira subida da API.

## 2. Serviço no Render

Em <https://dashboard.render.com>, *New → Web Service*, conecte o repositório `precavidos/vaga-viva` e preencha:

| Campo | Valor |
| --- | --- |
| Language | `Docker` |
| Branch | `main` |
| Region | a mesma região do Neon |
| Instance Type | `Free` por enquanto |
| Health Check Path | `/actuator/health/readiness` |
| Auto-Deploy | **Off** (quem dispara o deploy é o CI, depois dos testes) |

Em *Environment*, cadastre as três variáveis do banco (passo 1). O Render define `PORT` sozinho, e a API já lê essa variável.

Cadastre também `ANTHROPIC_API_KEY` com a chave da conta do time na Anthropic. Sem ela, a API funciona e os alunos recebem os textos padrão.

## 3. Ligar o CI ao Render

1. No serviço do Render, em *Settings → Deploy Hook*, copie a URL do hook.
2. No GitHub, em *Settings → Secrets and variables → Actions* do repositório:
   - em **Secrets**, crie `RENDER_DEPLOY_HOOK_URL` com a URL do hook;
   - em **Variables**, crie `API_URL` com a URL pública do serviço (por exemplo, `https://vaga-viva.onrender.com`). Ela aparece no ambiente `production` do GitHub.

A URL do hook permite publicar o serviço: trate-a como senha. Nunca a coloque no repositório nem no chat.

Enquanto o secret não existir, o job `deploy` só avisa e termina sem erro.

## 4. Conferir

Depois do próximo push no `main`:

1. No GitHub, em *Actions*, os jobs `build` e `deploy` ficam verdes.
2. No Render, o deploy aparece em *Events* e termina como *Live*.
3. `https://<sua-url>/actuator/health` responde `{"status":"UP", ...}`.

Para testar a imagem na sua máquina, com o banco do `compose.yaml` no ar:

```bash
docker build -t vaga-viva:local .
docker run --rm --network vaga-viva_default -p 8080:8080 -e DATABASE_URL=jdbc:postgresql://postgres:5432/vagaviva vaga-viva:local
```

## Depois

Quando o login com GitHub entrar no código, cadastre também `GITHUB_CLIENT_ID` e `GITHUB_CLIENT_SECRET` no Render e acrescente `https://<sua-url>/login/oauth2/code/github` como Callback URL do GitHub App (`docs/github-app.md`).
