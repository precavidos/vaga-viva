# Como publicar a API do Vaga Viva

A API roda no Render a partir do `Dockerfile` da raiz, com o PostgreSQL no Neon. A cada push no `main`, o CI roda os testes e, só se passarem, chama o *deploy hook* do Render. Só uma pessoa do time precisa fazer isto, uma vez.

Os planos gratuitos bastam até a semana 4. No gratuito do Render a API dorme após um tempo sem uso e leva alguns segundos para acordar. Antes da submissão, troque para um plano pago que não dorme (cronograma, semana 4). Confira os limites atuais de cada plano no site do Render e do Neon.

## 1. Banco no Neon

O repositório já está ligado ao projeto Neon (`neon.ts` e o arquivo `.neon`, que fica fora do Git), na branch `production`, em `AWS us-east-1`. O Render não tem região no Brasil; crie o serviço em Virginia, a mesma região do banco, para cada consulta não atravessar o continente.

Para trazer as variáveis do Neon para a sua máquina (vão para o `.env`):

```bash
neon env pull
```

Ele grava duas strings de conexão no formato do Neon (`postgresql://usuario:senha@host/banco?...`), e a API aceita as duas como vêm:

| Variável | Conexão | Quem usa |
| --- | --- | --- |
| `DATABASE_URL` | com pooler (host com `-pooler`) | a aplicação |
| `DATABASE_URL_UNPOOLED` | direta | o Flyway, porque migrações não funcionam bem atrás do pooler |

A conversão para o formato JDBC fica em `PostgresUrlEnvironmentPostProcessor`. Ela descarta a opção `channel_binding` (com ela o driver JDBC tem a conexão recusada pelo Neon); a conexão continua com TLS por causa do `sslmode=require`. O formato JDBC antigo (`jdbc:postgresql://...` com `DATABASE_USERNAME` e `DATABASE_PASSWORD`) continua funcionando, e é o que o banco local do `compose.yaml` usa.

O Spring não lê o `.env` sozinho. Para rodar a API na sua máquina contra o Neon:

```bash
set -a; . ./.env; set +a; ./mvnw spring-boot:run
```

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

Em *Environment*, cadastre `DATABASE_URL` e `DATABASE_URL_UNPOOLED` com os mesmos valores do seu `.env` (passo 1), sem mudar nada. O Render define `PORT` sozinho, e a API já lê essa variável.

Para o modo turma, cadastre `CLASSROOM_TEACHERS` com os logins do GitHub dos professores do piloto, separados por vírgula (maiúsculas e minúsculas não importam, e espaços em volta das vírgulas são ignorados). Vazia, ninguém consegue criar turma, e os professores recebem 403.

Exclusão manual de um aluno ou de uma turma (procedimento de privacidade do piloto): as chaves estrangeiras não têm cascata, então apague nesta ordem. Primeiro `enrollment_progress` do aluno (ou de todos os alunos da turma), depois `enrollment`, e por último `classroom`.

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

## E-mail (Resend)

O aluno pode encaminhar uma vaga por e-mail e receber o plano de volta. Quem recebe e responde é o Resend; sem as variáveis abaixo, o endpoint responde 503 e o resto da API segue normal.

1. No Resend, verifique o domínio de envio e ative o recebimento (*Receiving*) nesse domínio. Se preferir não mexer no DNS, use o endereço de recebimento que o próprio Resend dá à conta.
2. Em *Webhooks*, crie um webhook para o evento `email.received` apontando para `https://<app>/api/inbound-email`. Copie o segredo de assinatura (`whsec_…`).
3. No Render, em *Environment*, cadastre:
   - `RESEND_API_KEY`: a chave de API do Resend;
   - `RESEND_WEBHOOK_SECRET`: o `whsec_…` do webhook;
   - `RESEND_FROM`: o remetente das respostas, num domínio verificado (por exemplo, `Vaga Viva <vaga@seu-dominio.com>`);
   - `VAGAVIVA_PUBLIC_URL`: a URL pública da API (por exemplo, `https://vaga-viva.onrender.com`), usada para montar o link `#vaga=` das respostas.
4. Para testar, encaminhe uma vaga fictícia (com mais de 200 caracteres) para o endereço de recebimento. A resposta com o plano deve chegar em alguns segundos, e o link dela abre a página já com a vaga preenchida.

Tudo o que chega por e-mail (remetente, assunto, corpo e link) fica só em memória e não vai para o log (ADR 0003).

## Depois

Para o login com GitHub, cadastre também `GITHUB_CLIENT_ID` e `GITHUB_CLIENT_SECRET` no Render (e `GITHUB_APP_SLUG`, se o app não se chamar `vaga-viva`). A Callback URL e a Setup URL do GitHub App estão em `docs/github-app.md`. A API já confia nos cabeçalhos `X-Forwarded-*` do Render, então os redirecionamentos do login saem com `https`.
