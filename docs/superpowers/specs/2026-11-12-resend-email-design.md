# Resend: vaga por e-mail encaminhado e e-mail semanal do aluno — design

**Issue:** [precavidos/vaga-viva#28](https://github.com/precavidos/vaga-viva/issues/28)
**Cronograma:** semana 2 do Hack Brasil, prazo 17/11.
**Entrega:** uma spec, dois PRs. O PR 1 traz o cliente do Resend e a entrada por e-mail. O PR 2 traz o consentimento e o e-mail semanal. O PR 1 não depende de banco. O PR 2 reaproveita o cliente do PR 1.

## Por que

Para muitos alunos, a vaga chega por e-mail, seja de um recrutador, seja de um alerta de vagas. Encaminhar o e-mail é mais fácil do que copiar o texto e colar numa página. Depois que o projeto começa, um lembrete semanal com a missão atual e o último retorno do revisor ajuda o aluno a não abandonar o repositório.

## Objetivos

1. O aluno encaminha uma vaga para o endereço do Vaga Viva e recebe de volta, no mesmo fio, o plano e um link. O link abre a página com a vaga já preenchida e o plano gerado.
2. O aluno que deu o e-mail com consentimento recebe toda segunda a missão atual e o último retorno do revisor.
3. Quem não consentiu não recebe nada. Quem se descadastra tem os dados apagados.

## Fora do escopo

- O resumo semanal do professor. Ele ainda não existe no código: a spec do modo turma o prevê até 24/11. Quando vier, ele usa o mesmo `ResendApi`.
- Ler vagas em anexos (PDF, imagem). O aluno recebe uma ajuda pedindo a vaga no corpo do e-mail.
- Guardar progresso de alunos que não estão em turma. O e-mail semanal consulta o GitHub na hora do envio.
- Mudar o e-mail ou o repositório de um consentimento pela tela. O aluno se descadastra e consente de novo.

## O que seria um fracasso, mesmo funcionando

- **O corpo do e-mail aparecer em algum lugar.** O remetente, o assunto e o corpo só existem em memória durante a requisição. Nada vai para o banco, para o log ou para mensagens de exceção (ADR 0003).
- **O texto da vaga passar pelo servidor numa URL.** O link leva o texto no fragmento (`#`), que o navegador não envia. A página apaga o fragmento assim que o lê.
- **O Vaga Viva virar disparador de e-mails.** O remetente de um e-mail pode ser falsificado. Há limite por remetente e limite diário, e nunca respondemos a respostas automáticas.
- **Um aluno receber o e-mail semanal sem ter consentido, ou duas vezes na mesma semana.**

## Decisões

| Tema | Decisão |
| --- | --- |
| Divisão | Uma spec, dois PRs: entrada primeiro, depois o semanal. |
| Dados do e-mail semanal | A tabela guarda login, e-mail, repositório e data do consentimento. A missão e o retorno do revisor vêm do GitHub na hora do envio. |
| Texto da vaga no link | Comprimido com deflate-raw e codificado em base64url, no fragmento `#vaga=`. Acima de 8 000 caracteres no link, não há link pré-preenchido. |
| Disparo semanal | Um workflow agendado do GitHub Actions chama um endpoint protegido por segredo. Não usamos `@Scheduled`, porque a instância do Render dorme. |
| Descadastro | Token aleatório guardado na linha. `GET` mostra uma confirmação e `POST` apaga a linha. |

## O Resend

- O webhook `email.received` traz apenas metadados: `email_id`, `from` (só o endereço), `to`, `subject` e `message_id`. O corpo vem de `GET https://api.resend.com/emails/receiving/{email_id}`, que devolve `text`, `html` e `headers`.
- O webhook é assinado no padrão Svix:
  - os cabeçalhos são `svix-id`, `svix-timestamp` e `svix-signature`;
  - a assinatura é um HMAC-SHA256 de `"{svix-id}.{svix-timestamp}.{corpo bruto}"`, com o segredo `whsec_…` decodificado de base64 (sem o prefixo);
  - `svix-signature` é uma lista separada por espaços de `v1,<base64>`, e basta uma conferir.
- Para enviar, usamos `POST https://api.resend.com/emails` com `from`, `to`, `subject`, `text`, `html` e `headers`.

## PR 1: entrada por e-mail

### Componentes

Ficam todos num pacote novo, `io.github.precavidos.vagaviva.email`, package-private, salvo onde estiver indicado.

| Unidade | Responsabilidade |
| --- | --- |
| `ResendProperties` | `vagaviva.resend.api-key`, `webhook-secret`, `from` (por exemplo `Vaga Viva <vaga@…>`), `public-url` (o endereço público do site, sem barra no final, usado nos links dos e-mails), `inbound-per-sender-per-hour` (5) e `inbound-per-day` (80). O `toString` mascara a chave e o segredo. |
| `SvixSignature` | `static boolean isValid(String secret, String id, String timestamp, byte[] body, String header, Instant now)`. A tolerância é de 5 minutos para os dois lados, e a comparação é feita em tempo constante. |
| `ResendApi` (public interface) | `ReceivedEmail receivedEmail(String emailId)` e `void send(OutgoingEmail email)`. |
| `ResendClient` | Implementação com o `RestClient`, sem redirecionamentos. Os erros viram `ResendApiException`, que leva só o status HTTP. |
| `ReceivedEmail`, `OutgoingEmail` | Records. `OutgoingEmail(to, subject, text, html, Map<String,String> headers)`. |
| `ForwardedPosting` | `static String text(ReceivedEmail email)`. Ver "Extração da vaga". |
| `PostingLink` | `static Optional<String> of(String publicUrl, String postingText)`. Ver "O link". |
| `InboundEmailController` | `POST /api/inbound-email`. Recebe o corpo como `byte[]` e confere a assinatura antes de ler o JSON. |
| `InboundEmailService` | Busca o corpo, extrai a vaga, gera o plano e envia a resposta. Usa `JobPostingExtractor`, `ProjectPlanners` e `ArchetypeCatalog`, sem mudar nada neles. |
| `InboundLimits` | Repetições, limite por remetente e limite diário, tudo em memória. Ver "Fluxo". |
| `PlanReplyTemplate` | Texto puro e HTML simples da resposta com o plano e da resposta de ajuda. O HTML escapa todos os valores. |

Se a chave ou o segredo estiver em branco, o endpoint responde 503, como o webhook do GitHub.

O `SecurityConfig` passa a aceitar `/api/inbound-email` sem sessão e sem CSRF, porque quem garante a origem é a assinatura.

### Fluxo

1. Conferir `SvixSignature` com o corpo bruto. Se ela falha, está fora da tolerância ou falta algum cabeçalho, a resposta é **401**.
2. Ler o JSON. Se `type` não for `email.received`, a resposta é **200** e nada mais acontece.
3. Se o `svix-id` já foi processado nas últimas 24 horas, a resposta é **200**. O id entra no conjunto só depois do envio, ou quando o resultado é definitivo (`no-posting`, `ignored`, `rate-limited`).
4. Buscar o e-mail com `ResendApi.receivedEmail`. Se falhar, a resposta é **503**, para o Resend tentar de novo.
5. **Evitar loops:** ignorar, com **200**, quando:
   - o remetente é o nosso próprio `from`;
   - a parte local do remetente é `mailer-daemon` ou `postmaster` (sem diferenciar maiúsculas);
   - o cabeçalho `Auto-Submitted` existe com valor diferente de `no`.
6. **Aplicar os limites.** Por remetente, no máximo 5 respostas por hora, com a chave sendo o HMAC-SHA256 do endereço em minúsculas, usando um sal aleatório gerado na inicialização. No total, no máximo 80 respostas por dia (UTC). Se algum estoura, a resposta é **200** e nada é enviado.
7. Extrair a vaga com `ForwardedPosting`. Se o texto tem menos de 200 caracteres, enviar a resposta de ajuda.
8. Gerar o plano e enviar a resposta com o plano. Os detalhes da resposta:
   - o assunto é `Re: <assunto original>`, ou `Seu plano no Vaga Viva` se não houver assunto;
   - os cabeçalhos são `In-Reply-To` e `References` com o `message_id` original, e `Auto-Submitted: auto-replied`.
9. Se o envio falhar, a resposta é **503**. Se der certo, **200**.

Qualquer outro erro gera **500**, com um log sem dados do e-mail.

### Extração da vaga

- Usar `text` quando ele não estiver em branco. Se não, converter o `html` com o jsoup em texto com quebras de linha.
- Cortar a partir da linha de encaminhamento conhecida, e o bloco de cabeçalhos que vem logo depois dela:
  - Gmail: `---------- Forwarded message ---------` ou `---------- Mensagem encaminhada ---------`;
  - Outlook: um bloco `De:`/`From:`, `Enviado:`/`Sent:`, `Para:`/`To:` e `Assunto:`/`Subject:`;
  - Apple Mail: `Begin forwarded message:` ou `Início da mensagem encaminhada:`.
- O que vem antes da linha de encaminhamento é a nota do aluno ("olha essa vaga") e é descartado. Sem linha de encaminhamento, o corpo inteiro é a vaga.
- Remover a assinatura que começa com `-- ` sozinho numa linha.
- Cortar em `JobPostingExtractor.MAX_LENGTH`.

### O link

- O formato é `{public-url}/#vaga=<base64url sem padding de deflate-raw(UTF-8 do texto)>`.
- No Java, usar `Deflater(Deflater.BEST_COMPRESSION, true)` e `Base64.getUrlEncoder().withoutPadding()`.
- Se o link inteiro passar de 8 000 caracteres, `PostingLink.of` devolve vazio. A resposta então diz: "Essa vaga é longa demais para caber num link. Abra {public-url} e cole o texto."

### A página (`index.html`)

Ao carregar, se `location.hash` começa com `#vaga=`, o script:

1. lê o valor e chama `history.replaceState(null, '', '/')`;
2. decodifica o base64url e descomprime com `new DecompressionStream('deflate-raw')`;
3. preenche `#posting` e chama `plan(text)`.

Se o passo 2 falhar, mostra no status: "Não conseguimos abrir a vaga desse link. Cole o texto aqui." Se a página voltou do GitHub com uma vaga pendente, essa vaga continua tendo prioridade.

### A resposta com o plano

A resposta está em português, em texto puro e HTML simples, com:
- arquétipo, nível, domínio e número de missões;
- o aviso de compatibilidade, quando existir, com o mesmo texto do `fitWarning` da página;
- a lista de missões com título e resumo;
- o link, ou a instrução de colar a vaga;
- uma linha final: "Você recebeu este e-mail porque encaminhou uma vaga para o Vaga Viva. Não guardamos o texto da vaga nem o seu endereço."

A resposta de ajuda explica que a vaga precisa estar no corpo do e-mail, não em anexo, e traz o link da página.

### Logs

- O log registra só `svix-id` e o resultado, que é um destes: `planned`, `no-posting`, `rate-limited`, `ignored`, `duplicate`, `failed`.
- Nenhum `log.*` recebe o remetente, o assunto, o corpo ou o link.
- Nenhuma exceção nova carrega esses dados na mensagem ou como `cause`.

### Testes (sem rede)

- `SvixSignatureTests`:
  - o vetor de teste da documentação do Svix;
  - assinatura alterada;
  - fora da tolerância nos dois sentidos;
  - várias assinaturas no cabeçalho;
  - cabeçalho malformado.
- `ForwardedPostingTests`: encaminhamentos fictícios do Gmail (pt e en), do Outlook e do Apple Mail; um e-mail só com HTML; um e-mail sem linha de encaminhamento; um com assinatura. Fixtures em `src/test/resources/email/`, todas inventadas.
- `PostingLinkTests`:
  - comprimir e descomprimir devolve o texto com acentos;
  - o link sai em base64url sem `+`, `/` ou `=`;
  - o teto de 8 000 caracteres é respeitado.
- `InboundEmailControllerTests`, com `ResendApi` falso, um caso para cada situação:
  - plano;
  - vaga longa;
  - vaga curta;
  - evento de outro tipo;
  - assinatura inválida;
  - `svix-id` repetido;
  - remetente próprio;
  - `mailer-daemon`;
  - `Auto-Submitted`;
  - limite por remetente;
  - limite diário;
  - falha ao buscar (503);
  - falha ao enviar (503).
- Um teste de log com `OutputCaptureExtension`: o remetente e um trecho do corpo nunca aparecem.
- A página tem verificação manual no checklist do PR: abrir um link gerado pelo Java e um link corrompido.

## PR 2: consentimento e e-mail semanal

### Consentimento na página

- Perto de "Criar repositório", entra um campo `type="email"` e o checkbox "Quero receber toda segunda a minha missão da semana e o último retorno do revisor. Guardamos o endereço só para isso." Os dois são opcionais. O campo só é obrigatório se o checkbox estiver marcado.
- O e-mail e o checkbox entram no `remember()`/`recall()` junto com a vaga pendente, no `sessionStorage`, para sobreviver ao login e à instalação do app.
- Depois que o repositório é criado, se o checkbox estiver marcado, a página chama `POST /api/weekly-email` com `{ "email": "...", "repositoryFullName": "dono/repo" }`. Se essa chamada falhar, o repositório continua criado e a página mostra "Não conseguimos salvar o seu e-mail. Tente de novo mais tarde."

### `POST /api/weekly-email`

- Exige sessão e CSRF, como `/api/student-repositories`.
- Valida `email` com `@Email` e `@Size(max = 254)`, e `repositoryFullName` com `@Pattern` de `dono/nome`.
- O dono do repositório precisa ser o login da sessão (sem diferenciar maiúsculas). Se não for, a resposta é 403.
- Faz upsert por `student_login`: e-mail, repositório, `consented_at = now()`, `unsubscribe_token` novo e `last_sent_at = null`. A resposta é **204**.

### Tabela (`V3__weekly_email.sql`)

```sql
create table weekly_email_subscription (
    student_login varchar(100) primary key,
    email varchar(254) not null,
    repository_full_name varchar(200) not null,
    consented_at timestamptz not null,
    unsubscribe_token varchar(64) not null unique,
    last_sent_at timestamptz
);
```

O `unsubscribe_token` tem 32 bytes de `SecureRandom` em base64url.

### Descadastro

- `GET /descadastrar?t=<token>` é uma página estática, `descadastrar.html`, com um botão "Parar de receber". Ela não apaga nada sozinha.
- `POST /api/weekly-email/unsubscribe` com `{ "token": "..." }` apaga a linha e responde 204, mesmo que o token não exista, para não revelar quais tokens são válidos. Não exige sessão nem CSRF, porque o token é a credencial.
- `POST /api/weekly-email/unsubscribe?t=<token>` com o corpo `List-Unsubscribe=One-Click` atende o descadastro de um clique (RFC 8058) e tem o mesmo efeito.
- Todo e-mail semanal leva os cabeçalhos `List-Unsubscribe: <{public-url}/api/weekly-email/unsubscribe?t=…>` e `List-Unsubscribe-Post: List-Unsubscribe=One-Click`, além do link `{public-url}/descadastrar?t=…` no rodapé.

### Disparo

- `.github/workflows/weekly-email.yml` roda com `cron: '0 11 * * 1'` (segunda, 8:00 em Brasília), aceita `workflow_dispatch` e chama `POST {VAGAVIVA_PUBLIC_URL}/api/weekly-emails` com o cabeçalho `X-Vaga-Viva-Secret: ${{ secrets.WEEKLY_EMAIL_SECRET }}`.
- O endpoint responde 503 se o segredo ou a `RESEND_API_KEY` estiver em branco, e 401 se o cabeçalho não conferir (comparação em tempo constante). Não exige sessão nem CSRF.
- A resposta é `{ "sent": n, "skipped": n, "failed": n }`.

### Montagem de cada e-mail (`WeeklyEmailService`)

Para cada linha com `last_sent_at` nulo ou anterior a `now() - 6 dias`:

1. Pegar a instalação do app no repositório com o novo `GitHubApi.repositoryInstallation(appJwt, repositoryFullName)` e gerar o token com `InstallationTokens`. Se o repositório sumiu ou o app não está instalado (404), contar como `skipped`.
2. Ler `.vaga-viva/plan.json` para ter a lista de missões. Se não houver plano legível, `skipped`.
3. Pegar o PR mais recente, aberto ou fechado, com o novo `GitHubApi.pullRequests(token, repositoryFullName)`, ordenado por `updated`, limitado a 1.
4. Descobrir a missão atual:
   - se o repositório está numa turma (`enrollment.repository_full_name`), usar `enrollment_progress.current_mission_id` e `completed`. O pacote `classroom` expõe para isso um método público de leitura por nome de repositório;
   - se não, usar `MissionProgress.from(planIds, report)`, com o relatório do `FailedTestsSource` no head do PR mais recente, ou no último commit do branch padrão se não houver PR;
   - se o resultado for `Unknown`, o e-mail diz "Ainda não vimos os testes rodarem no seu repositório" e mostra a primeira missão do plano.
5. Achar o último retorno do revisor: o comentário mais recente desse PR que é uma revisão do Vaga Viva. `ReviewTexts` ganha um método público `static Optional<String> reviewText(String body)`, que devolve o texto sem marcador e sem rodapé. O texto é cortado em 600 caracteres, sem quebrar palavra. Sem PR ou sem revisão, o e-mail diz "Você ainda não abriu um PR nesta missão."
6. Montar o e-mail com o `WeeklyEmailTemplate`:
   - o assunto é `Sua missão da semana: <título>`, ou `Você concluiu o projeto!`;
   - o texto traz o nome do repositório com link, a missão atual (número e título do plano) com o link `https://github.com/{repo}/issues`, o retorno do revisor e o rodapé de descadastro.
7. Enviar com `ResendApi.send` e gravar `last_sent_at = now()`.
   - Se o aluno concluiu tudo, o e-mail de parabéns é enviado e a linha é apagada, encerrando os envios.
   - Uma falha de envio conta como `failed` e não para o lote.

Entre um envio e o próximo, uma pausa de 600 ms respeita o limite de 2 requisições por segundo do Resend. No piloto, o número de alunos é pequeno.

Os logs registram só o login do GitHub e o resultado, nunca o e-mail.

### Testes (sem rede)

- `WeeklyEmailSubscriptionRepositoryTests` (Testcontainers): o upsert troca o repositório e o token, e o descadastro apaga a linha.
- `WeeklyEmailServiceTests`, com `GitHubApi`, `FailedTestsSource` e `ResendApi` falsos, e um relógio fixo:
  - um aluno com consentimento recebe;
  - um aluno sem linha não recebe;
  - quem já recebeu há 3 dias não recebe;
  - um repositório que responde 404 é pulado;
  - um aluno que concluiu tudo recebe os parabéns e a linha é apagada;
  - um aluno de turma usa `enrollment_progress`;
  - um relatório ausente cai no texto de "ainda não vimos os testes";
  - uma falha de envio de um aluno não impede o próximo.
- `WeeklyEmailControllerTests`:
  - o endpoint recusa chamadas sem segredo e com segredo errado;
  - o consentimento recusa repositório de outro dono e e-mail inválido;
  - o descadastro funciona pelo corpo JSON e pelo one-click.
- `WeeklyEmailTemplateTests`: o HTML escapa o texto do revisor.

## Configuração e documentação

- `.env.example`: `RESEND_API_KEY=`, `RESEND_WEBHOOK_SECRET=`, `RESEND_FROM=`, `VAGAVIVA_PUBLIC_URL=` e `WEEKLY_EMAIL_SECRET=` (este último só no PR 2), com um comentário apontando para `docs/deploy.md`.
- `application.yaml`: o bloco `vagaviva.resend` (com `public-url`) e, no PR 2, `vagaviva.weekly-email.secret`, lidos dessas variáveis.
- `SecurityConfig`: `permitAll` e fora do CSRF para `/api/inbound-email`, `/api/weekly-emails` e `/api/weekly-email/unsubscribe`; `permitAll` para `/descadastrar`. `/api/weekly-email` continua exigindo sessão e CSRF.
- `docs/deploy.md`: uma seção sobre o Resend com três passos:
  - verificar o domínio de envio e o de recebimento;
  - criar o webhook `email.received` apontando para `/api/inbound-email`;
  - copiar o segredo para o Render e o `WEEKLY_EMAIL_SECRET` para os segredos do repositório no GitHub.
- ADR 0003: na frase sobre os dados do aluno, incluir "e, para quem pede o e-mail semanal, o repositório e um token de descadastro; o descadastro apaga tudo".

## Ordem de entrega

1. **PR 1, até 15/11:** `ResendProperties`, `SvixSignature`, `ResendApi`/`ResendClient`, `ForwardedPosting`, `PostingLink`, o endpoint de entrada, a página com `#vaga=`, o `.env.example` e o guia de deploy.
2. **PR 2, até 17/11:** a migration V3, o consentimento na página, o descadastro, o `WeeklyEmailService`, o workflow e o ADR 0003.
