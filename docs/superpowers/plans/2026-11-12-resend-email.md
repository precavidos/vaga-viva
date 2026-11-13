# Vaga por e-mail e e-mail semanal do aluno — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer o aluno receber o plano ao encaminhar uma vaga por e-mail (PR 1) e, quando ele consentir, receber toda segunda a missão atual e o último retorno do revisor (PR 2).

**Architecture:** Um pacote novo, `email`, concentra o cliente do Resend (`ResendApi`), a verificação Svix, a entrada (`InboundEmailController` → `InboundEmailService`) e o envio semanal (`WeeklyEmailService`). O pacote `review` expõe uma leitura do repositório do aluno: plano, relatório de testes e última revisão. O `classroom` expõe a situação de um aluno de turma. O texto da vaga volta ao aluno comprimido no fragmento `#vaga=`, e a página o lê no navegador.

**Tech Stack:** Java 21, Spring Boot 4.1.2 (`RestClient`, Spring Data JPA, Flyway, Spring Security), Jackson 3 (`tools.jackson.databind`), jsoup 1.24.1, JUnit 5 + AssertJ + Mockito, `MockRestServiceServer`, Testcontainers PostgreSQL, `OutputCaptureExtension`.

**Spec:** [`docs/superpowers/specs/2026-11-12-resend-email-design.md`](../specs/2026-11-12-resend-email-design.md) (issue [precavidos/vaga-viva#28](https://github.com/precavidos/vaga-viva/issues/28)).

## Global Constraints

- O trabalho fica na branch `feat/resend-email`.
  - O PR 1 reúne as Tarefas 1 a 7, até 15/11. O PR 2 reúne as Tarefas 8 a 12, até 17/11, numa branch `feat/weekly-email` criada a partir do `main` atualizado depois do merge do PR 1.
  - Os commits seguem o histórico: em inglês, no imperativo (`Add ...`), terminando com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- O código fica em inglês, e os textos para o aluno em português.
- Os testes rodam com `./mvnw -B verify`, como no CI, e nenhum faz chamada de rede: o Resend e o GitHub são falsos (Mockito ou `MockRestServiceServer`).
- ADR 0003, na entrada:
  - O remetente, o assunto, o corpo e o link só existem em memória.
  - Nenhum `log.*` e nenhuma mensagem ou `cause` de exceção recebe esses dados.
  - A `ResendApiException` carrega só o status HTTP.
- ADR 0003, no envio semanal: o log registra o login do GitHub e o resultado, nunca o e-mail.
- Com a configuração desligada (chave ou segredo em branco), os endpoints de máquina respondem **503**, como o `GitHubWebhookController` faz hoje. *A spec dizia 404 e foi corrigida junto com este plano.*
- A assinatura Svix:
  - assina `"{svix-id}.{svix-timestamp}.{corpo bruto}"` com HMAC-SHA256;
  - a chave é o base64 decodificado do segredo, sem o prefixo `whsec_`;
  - o cabeçalho é uma lista separada por espaço de `v1,<base64>`, e basta uma conferir;
  - a tolerância é de **5 minutos** para os dois lados.
- O link: `{public-url}/#vaga=` + base64url sem padding de deflate-raw (`new Deflater(Deflater.BEST_COMPRESSION, true)`) do UTF-8 do texto. O link inteiro tem no máximo **8 000** caracteres.
- Os limites da entrada:
  - **5** respostas por remetente por hora e **80** por dia (UTC), configuráveis;
  - `svix-id` repetidos são lembrados por **24 h**, até 1 000 ids;
  - uma vaga com menos de **200** caracteres recebe a resposta de ajuda.
- Envios ao Resend levam o cabeçalho `Idempotency-Key`:
  - na entrada, `inbound/{svix-id}`;
  - no semanal, `weekly/{student_login}/{data ISO da segunda-feira}`.
- O semanal pula quem tem `last_sent_at` posterior a `now() - 6 dias`. Faz uma pausa de **600 ms** entre envios. O texto do revisor é cortado em **600** caracteres, sem quebrar palavra.
- Os textos exatos, para copiar literalmente:
  - Rodapé da resposta com o plano: `Você recebeu este e-mail porque encaminhou uma vaga para o Vaga Viva. Não guardamos o texto da vaga nem o seu endereço.`
  - Vaga longa: `Essa vaga é longa demais para caber num link. Abra {public-url} e cole o texto.`
  - Erro na página: `Não conseguimos abrir a vaga desse link. Cole o texto aqui.`
  - Checkbox: `Quero receber toda segunda a minha missão da semana e o último retorno do revisor. Guardamos o endereço só para isso.`
  - Falha ao salvar o e-mail: `Não conseguimos salvar o seu e-mail. Tente de novo mais tarde.`
  - Sem testes: `Ainda não vimos os testes rodarem no seu repositório.`
  - Sem PR: `Você ainda não abriu um PR nesta missão.`
  - Assuntos: `Seu plano no Vaga Viva` (resposta sem assunto original), `Sua missão da semana: {título}`, `Você concluiu o projeto!`
- Todas as fixtures de e-mail são inventadas, sem nenhuma vaga real de terceiros.

## Review Focus

1. **Encaminhamento em que a nota do aluno vem depois da vaga** ("segue abaixo" sem linha de encaminhamento, só a vaga colada): sem linha de encaminhamento, o corpo inteiro vira a vaga e o plano sai. Teste na Tarefa 3.
2. **Remetente com maiúsculas e display name** (`Maria <MARIA@x.com>` e `maria@x.com`): os dois contam para o mesmo limite por remetente. Teste na Tarefa 5.
3. **Vaga com emoji e caracteres fora do BMP** ("🚀 Vaga Júnior"): o link comprime e descomprime sem corromper o texto. Teste na Tarefa 4.
4. **Revisão do revisor com HTML e Markdown** (`<script>`, `**negrito**`) no e-mail semanal: o HTML do e-mail escapa o texto e não executa nada. Teste na Tarefa 11.
5. **Aluno que trocou de repositório e consentiu de novo**: só o repositório novo recebe o e-mail, e o `last_sent_at` antigo não bloqueia a semana. Teste na Tarefa 8.

---

## Mapa de arquivos

Os arquivos novos ficam em `src/main/java/io/github/precavidos/vagaviva/email/`, package-private salvo indicação:

| Arquivo | Responsabilidade | Tarefa |
|---|---|---|
| `ResendProperties.java` (public record) | Configuração `vagaviva.resend.*` | 1 |
| `EmailConfig.java` | `@EnableConfigurationProperties` e o bean `ResendApi` | 1, 2 |
| `SvixSignature.java` | Verificação da assinatura | 1 |
| `ResendApi.java` (public), `ResendClient.java`, `ResendApiException.java`, `ReceivedEmail.java`, `OutgoingEmail.java` | Cliente do Resend | 2 |
| `ForwardedPosting.java` | Corpo do e-mail → texto da vaga | 3 |
| `PostingLink.java` | Texto → link `#vaga=` | 4 |
| `InboundLimits.java`, `PlanReplyTemplate.java` | Limites em memória e textos da resposta | 5 |
| `InboundEmailController.java`, `InboundEmailService.java` | `POST /api/inbound-email` | 6 |
| `WeeklyEmailSubscription.java`, `WeeklyEmailSubscriptionRepository.java`, `WeeklyEmailController.java` | Consentimento e descadastro | 8, 9 |
| `WeeklyEmailTemplate.java`, `WeeklyEmailService.java` | Montagem e envio semanal | 11, 12 |

Arquivos existentes que mudam:
- `login/SecurityConfig.java` (Tarefas 6, 8, 9, 12)
- `review/ReviewContextBuilder.java`, `review/ReviewTexts.java` e `review/InstallationTokens.java` (Tarefa 10)
- `github/GitHubApi.java` e `github/GitHubClient.java` (Tarefa 10)
- `static/index.html` (Tarefas 7 e 12)
- `application.yaml`, `.env.example` e `docs/deploy.md`
- `docs/adr/0003-privacidade-da-vaga.md` (Tarefa 12)

Os testes ficam em `src/test/java/io/github/precavidos/vagaviva/email/`, e as fixtures em `src/test/resources/email/`.

---

# PR 1 — entrada por e-mail

### Task 1: Configuração do Resend e assinatura Svix

**Files:**
- Create: `email/ResendProperties.java`, `email/EmailConfig.java`, `email/SvixSignature.java`
- Modify: `src/main/resources/application.yaml`, `.env.example`
- Test: `email/SvixSignatureTests.java`, `email/ResendPropertiesTests.java`

**Interfaces:**
- Produces:
  - `@ConfigurationProperties("vagaviva.resend") public record ResendProperties(@DefaultValue("") String apiKey, @DefaultValue("") String webhookSecret, @DefaultValue("") String from, @DefaultValue("") String publicUrl, @DefaultValue("5") int inboundPerSenderPerHour, @DefaultValue("80") int inboundPerDay)`
    - `boolean sendingEnabled()`: `apiKey`, `from` e `publicUrl` não estão em branco.
    - `boolean inboundEnabled()`: `sendingEnabled()` e `webhookSecret` não está em branco.
    - O construtor compacto tira a barra final de `publicUrl`.
    - O `toString` mostra `****` para a chave e o segredo, como o `GitHubAppProperties`.
  - `final class SvixSignature { static boolean isValid(String secret, String id, String timestamp, byte[] body, String header, Instant now) }`

- [ ] **Step 1: Escrever os testes que falham**

`SvixSignatureTests`, com o vetor da documentação do Svix:

```java
static final String SECRET = "whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw";
static final String ID = "msg_p5jXN8AQM9LWM0D4loKWxJek";
static final String TS = "1614265330";
static final byte[] BODY = "{\"test\": 2432232314}".getBytes(UTF_8);
static final String SIG = "v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE=";
static final Instant AT = Instant.ofEpochSecond(1614265330);
```

Casos:
- `acceptsTheSvixDocumentationVector`: `isValid(SECRET, ID, TS, BODY, SIG, AT)` é `true`.
- `acceptsWhenOneOfSeveralSignaturesMatches`: com o header `"v1,AAAA v1,g0hM9SsE+OTPJTGt/tmIKtSyZlE3uFJELVlNIOLJ1OE="`, o resultado é `true`.
- `rejectsAChangedBody`: o corpo `{"test": 2432232315}` dá `false`.
- `rejectsOutsideTheTolerance`: `AT.plusSeconds(301)` e `AT.minusSeconds(301)` dão `false`, e `AT.plusSeconds(299)` dá `true`.
- `rejectsMalformedInput`:
  - header `null`, `""`, `"v2,..."` e `"v1,not-base64!"`;
  - timestamp `"abc"`;
  - secret `""`.

  Todos dão `false` e nenhum lança exceção.

`ResendPropertiesTests`:
- `stripsTheTrailingSlashOfThePublicUrl`: `"https://vagaviva.dev/"` vira `"https://vagaviva.dev"`.
- `neverShowsTheKeyOrTheSecret`: o `toString()` não contém os valores e contém `****`.
- `inboundNeedsKeySecretFromAndUrl`: com qualquer um dos quatro em branco, `inboundEnabled()` é `false`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest='SvixSignatureTests,ResendPropertiesTests'`
Expected: falha de compilação (as classes não existem).

- [ ] **Step 3: Implementar**

- `SvixSignature`:
  - tira o prefixo `whsec_` se existir;
  - decodifica com `Base64.getDecoder()`;
  - faz o HMAC com `javax.crypto.Mac`;
  - compara cada `v1,` decodificado com `MessageDigest.isEqual`;
  - qualquer `IllegalArgumentException` ou `NumberFormatException` dá `false`.
- `EmailConfig`: `@Configuration(proxyBeanMethods = false) @EnableConfigurationProperties(ResendProperties.class)`.
- `application.yaml`, em `vagaviva:`:

```yaml
  resend:
    api-key: ${RESEND_API_KEY:}
    webhook-secret: ${RESEND_WEBHOOK_SECRET:}
    from: ${RESEND_FROM:}
    public-url: ${VAGAVIVA_PUBLIC_URL:}
```

- `.env.example`: acrescentar, com o comentário `# Resend: forwarded postings and the weekly e-mail (docs/deploy.md). Leave empty to turn e-mail off.`, as linhas `RESEND_API_KEY=`, `RESEND_WEBHOOK_SECRET=`, `RESEND_FROM=` e `VAGAVIVA_PUBLIC_URL=`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest='SvixSignatureTests,ResendPropertiesTests'`
Expected: PASS.

- [ ] **Step 5: Commit**: `Add the Resend settings and the Svix signature check`

### Task 2: Cliente do Resend

**Files:**
- Create: `email/ResendApi.java`, `email/ResendClient.java`, `email/ResendApiException.java`, `email/ReceivedEmail.java`, `email/OutgoingEmail.java`
- Modify: `email/EmailConfig.java`
- Test: `email/ResendClientTests.java`

**Interfaces:**
- Consumes: `ResendProperties` (Tarefa 1).
- Produces:
  - `public record ReceivedEmail(String from, String subject, String messageId, String text, String html, Map<String, String> headers)`
    - os campos ausentes no JSON viram `""`, e `headers` vira `Map.of()`;
    - as chaves de `headers` ficam em minúsculas.
  - `public record OutgoingEmail(String to, String subject, String text, String html, Map<String, String> headers, String idempotencyKey)`
  - `public interface ResendApi { ReceivedEmail receivedEmail(String emailId); void send(OutgoingEmail email); }`
  - `public class ResendApiException extends RuntimeException { int status(); }`
    - a mensagem é só `"Resend answered " + status`;
    - erro de I/O usa status `0`;
    - não tem `cause`.
  - Bean `ResendApi` em `EmailConfig`: `new ResendClient(RestClient.Builder, ResendProperties)`.

- [ ] **Step 1: Escrever os testes que falham**

`ResendClientTests` usa `MockRestServiceServer.bindTo(builder)`, como o `GitHubClientTests`, com base `https://api.resend.com`:
- `readsAReceivedEmail`:
  - espera `GET /emails/receiving/abc` com `Authorization: Bearer re_test`;
  - responde `{"from":"Maria <maria@example.com>","subject":"Fwd: Vaga","message_id":"<m1@x>","text":"corpo","html":"<p>corpo</p>","headers":{"Auto-Submitted":"no"}}`;
  - o resultado tem `headers().get("auto-submitted")` igual a `"no"`, `messageId()` igual a `"<m1@x>"` e `from()` igual a `"Maria <maria@example.com>"`.
- `toleratesMissingFields`: com `{}`, o resultado tem `text()` igual a `""` e `headers()` vazio.
- `sendsAnEmail`:
  - espera `POST /emails` com `Idempotency-Key: inbound/1`;
  - o corpo é o JSON com `from` (de `ResendProperties.from`), `to` igual a `["maria@example.com"]`, `subject`, `text`, `html` e `headers`.
- `errorsCarryOnlyTheStatus`: um `422` com o corpo `{"message":"maria@example.com is invalid"}` lança `ResendApiException` com `status()` igual a `422`. A `getMessage()` não contém `maria`, e `getCause()` é `null`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=ResendClientTests`
Expected: falha de compilação.

- [ ] **Step 3: Implementar `ResendClient implements ResendApi`**

- Cria o `RestClient` com `baseUrl("https://api.resend.com")`, a chave no `defaultHeaders(h -> h.setBearerAuth(...))` e o `defaultStatusHandler(HttpStatusCode::isError, ...)` lançando `ResendApiException`.
- Lê como `JsonNode`, como o `GitHubClient`.
- Só inclui `headers` no envio quando não estiver vazio.
- O `toString` nunca mostra a chave.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=ResendClientTests`
Expected: PASS.

- [ ] **Step 5: Commit**: `Add a Resend client for received and outgoing e-mails`

### Task 3: Texto da vaga no e-mail encaminhado

**Files:**
- Create: `email/ForwardedPosting.java`
- Test: `email/ForwardedPostingTests.java`
- Fixtures em `src/test/resources/email/`, todas fictícias:
  - `gmail-pt.txt`, `gmail-en.txt`, `outlook-pt.txt`, `apple-mail.txt`, `html-only.html`, `no-forward-line.txt`, `with-signature.txt`
  - cada uma traz a nota do aluno ("olha essa vaga"), a linha de encaminhamento, os cabeçalhos do encaminhamento e uma vaga de "Desenvolvedora Java Júnior" com pelo menos 300 caracteres

**Interfaces:**
- Consumes: `ReceivedEmail` (Tarefa 2), `JobPostingExtractor.MAX_LENGTH`.
- Produces: `final class ForwardedPosting { static String text(ReceivedEmail email) }`. Nunca devolve `null`; devolve `strip()`.

- [ ] **Step 1: Escrever os testes que falham**

Para cada fixture com linha de encaminhamento:
- o resultado começa pela primeira linha da vaga (`"Desenvolvedora Java Júnior"`);
- o resultado não contém `"olha essa vaga"`, `"Forwarded message"`, `"Mensagem encaminhada"`, `"Begin forwarded message"`, `"De:"`, `"From:"`, `"Assunto:"` nem `"Subject:"`.

Os demais casos:
- `usesTheHtmlWhenTextIsBlank`: com `text = ""` e o `html-only.html`, o resultado contém a vaga com as quebras de linha dos `<p>` e `<br>`.
- `keepsTheWholeBodyWithoutAForwardLine`: `no-forward-line.txt` (a vaga colada, com "segue abaixo" no fim) é devolvido inteiro.
- `dropsTheSignature`: em `with-signature.txt`, nada depois da linha `-- ` aparece.
- `capsAtTheExtractorLimit`: um texto com `MAX_LENGTH + 500` caracteres sai com `MAX_LENGTH`.
- `blankEmailGivesBlankText`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=ForwardedPostingTests`
Expected: falha de compilação.

- [ ] **Step 3: Implementar `ForwardedPosting.text`**

1. Normalizar `\r\n` para `\n`.
2. Achar a primeira linha que casa com `^-{5,}\s*(Forwarded message|Mensagem encaminhada)\s*-{5,}$` ou com `^(Begin forwarded message|Início da mensagem encaminhada):$` (sem diferenciar maiúsculas). Para o Outlook, achar a primeira linha `^(De|From):\s` seguida, em até 6 linhas, de uma linha `^(Assunto|Subject):\s`.
3. Cortar tudo até essa linha e pular as linhas seguintes do bloco de cabeçalhos: as que casam com `^(De|From|Date|Data|Enviado|Sent|Para|To|Cc|Assunto|Subject):` e as linhas em branco do começo.
4. Cortar na primeira linha que é exatamente `-- `.
5. Para o HTML, usar o jsoup: trocar `<br>` e `</p>` por `\n` antes de `Jsoup.parse(...).wholeText()`, ou equivalente que preserve as quebras.
6. Cortar em `MAX_LENGTH`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=ForwardedPostingTests`
Expected: PASS.

- [ ] **Step 5: Commit**: `Read the job posting out of a forwarded e-mail`

### Task 4: Link com a vaga no fragmento

**Files:**
- Create: `email/PostingLink.java`
- Test: `email/PostingLinkTests.java`

**Interfaces:**
- Produces:
  - `final class PostingLink { static final int MAX_LENGTH = 8_000; static Optional<String> of(String publicUrl, String postingText); static String decode(String fragmentValue) }`
  - `decode` só existe para os testes e para servir de referência ao JS: base64url → inflate (`Inflater(true)`) → UTF-8.

- [ ] **Step 1: Escrever os testes que falham**

- `roundTripsAccentsAndEmoji`: para `"🚀 Vaga Júnior — São Paulo\nRequisitos: Java, Spring"`, a parte depois de `#vaga=` passa por `decode` e devolve o texto igual.
- `startsWithThePublicUrlAndFragment`: `of("https://vagaviva.dev", "x".repeat(300))` começa com `"https://vagaviva.dev/#vaga="`.
- `usesBase64UrlWithoutPadding`: o valor casa com `^[A-Za-z0-9_-]+$`.
- `refusesLinksLongerThanTheLimit`: um texto aleatório (sem padrão, para não comprimir) de 20 000 caracteres dá `Optional.empty()`. Um texto que gera um link de até 8 000 caracteres dá um valor presente com `length() <= 8_000`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=PostingLinkTests`
Expected: falha de compilação.

- [ ] **Step 3: Implementar**

Usar `Deflater(BEST_COMPRESSION, true)`, um `DeflaterOutputStream` sobre `ByteArrayOutputStream` e `Base64.getUrlEncoder().withoutPadding()`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=PostingLinkTests`
Expected: PASS.

- [ ] **Step 5: Commit**: `Carry the posting in a compressed link fragment`

### Task 5: Limites da entrada e textos da resposta

**Files:**
- Create: `email/InboundLimits.java`, `email/PlanReplyTemplate.java`
- Test: `email/InboundLimitsTests.java`, `email/PlanReplyTemplateTests.java`

**Interfaces:**
- Consumes: `ProjectPlan`, `ArchetypeFit`, `SeniorityLevel`, `PlannedMission`.
- Produces:
  - `final class InboundLimits`:
    - `InboundLimits(int perSenderPerHour, int perDay, Clock clock)`
    - `boolean seen(String eventId)`: true se o id foi registrado nas últimas 24 h.
    - `void remember(String eventId)`
    - `boolean tryAcquire(String sender)`: false quando estoura o limite do remetente ou o do dia. Só consome a cota quando devolve true.
    - `static String address(String sender)`: extrai o endereço de `Nome <a@b>`, em minúsculas e sem espaços nas pontas.
  - `record Reply(String subject, String text, String html)` aninhado em `PlanReplyTemplate`.
  - `final class PlanReplyTemplate`:
    - `static Reply plan(ProjectPlan plan, String archetypeName, Optional<String> link, String publicUrl, String originalSubject)`
    - `static Reply help(String publicUrl, String originalSubject)`
    - `static String fitWarning(ArchetypeFit fit, String stack)`

- [ ] **Step 1: Escrever os testes que falham**

`InboundLimitsTests`, com um `Clock` mutável:
- `limitsEachSenderPerHour`: 5 chamadas de `tryAcquire("Maria <MARIA@example.com>")` dão true e a sexta dá false. A chamada com `"maria@example.com"` também dá false, porque é o mesmo remetente. Depois de avançar 61 min, volta a dar true.
- `limitsTheDay`: com `perDay = 3`, três remetentes diferentes dão true e o quarto dá false. Depois de passar para o dia seguinte (UTC), dá true.
- `remembersEventsFor24Hours`: depois de `remember("e1")`, `seen("e1")` é true. Depois de avançar 24 h e 1 s, é false.
- `keepsNoPlainAddress`: por reflexão, nenhum campo `String` ou coleção de `String` contém `"maria@example.com"` depois de `tryAcquire`. O teste explica no comentário que a chave é HMAC-SHA256 com um sal aleatório gerado na construção.

`PlanReplyTemplateTests`, com um `ProjectPlan` montado pelos planejadores reais a partir de uma vaga fictícia, como em `ProjectPlanApiTests`:
- `planReplyListsTheMissionsAndTheLink`:
  - o `text` contém o nome do arquétipo, `"Nível júnior"`, o nome do domínio, cada `mission.title()` e o link;
  - o `text` termina com o rodapé exato da seção Global Constraints;
  - o `subject` é `"Re: Fwd: Vaga Java"` para o assunto original `"Fwd: Vaga Java"`.
- `subjectFallsBackWhenBlank`: com o assunto `""`, o `subject` é `"Seu plano no Vaga Viva"`.
- `longPostingAsksToPaste`: com `link = Optional.empty()`, o texto contém `"Essa vaga é longa demais para caber num link. Abra https://vagaviva.dev e cole o texto."`.
- `htmlEscapesValues`: um título de missão com `<b>` aparece como `&lt;b&gt;` no `html`.
- `fitWarningMatchesThePage`: `PARTIAL` e `NONE` dão exatamente os textos do `fitWarning` de `index.html`; `COVERED` e `NOT_STATED` dão `""`.
- `helpExplainsTheBodyAndLinksThePage`: o texto contém `"no corpo do e-mail"` e `publicUrl`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest='InboundLimitsTests,PlanReplyTemplateTests'`
Expected: falha de compilação.

- [ ] **Step 3: Implementar**

- `InboundLimits`:
  - os mapas `ConcurrentHashMap` guardam, por chave HMAC, os `Instant` da última hora;
  - um contador diário é reiniciado quando muda o `LocalDate` UTC;
  - os eventos ficam num `LinkedHashMap` com `removeEldestEntry` acima de 1 000 e com expiração de 24 h na leitura;
  - os métodos públicos são `synchronized`, porque o volume é pequeno.
- `PlanReplyTemplate`:
  - os níveis usam os mesmos rótulos de `LEVELS` da página (`INTERN: estágio`, `JUNIOR: júnior`, `MID: pleno`, `SENIOR: sênior`);
  - o HTML é escapado com `org.springframework.web.util.HtmlUtils.htmlEscape`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest='InboundLimitsTests,PlanReplyTemplateTests'`
Expected: PASS.

- [ ] **Step 5: Commit**: `Add the inbound limits and the plan reply texts`

### Task 6: Endpoint `POST /api/inbound-email`

**Files:**
- Create: `email/InboundEmailController.java`, `email/InboundEmailService.java`
- Modify: `email/EmailConfig.java` (bean `InboundLimits` com o `Clock` existente), `login/SecurityConfig.java`
- Test: `email/InboundEmailApiTests.java`

**Interfaces:**
- Consumes:
  - `SvixSignature`, `ResendApi`, `ForwardedPosting`, `PostingLink`, `InboundLimits`, `PlanReplyTemplate`;
  - `JobPostingExtractor.extract(String)`, `ProjectPlanners.plan(ExtractedJobPosting)`, `ArchetypeCatalog.archetype(id).name()`.
- Produces:
  - `InboundEmailController`, com `static final String PATH = "/api/inbound-email"`.
  - `enum Outcome { PLANNED, NO_POSTING, RATE_LIMITED, IGNORED, DUPLICATE }` e `Outcome InboundEmailService.handle(String eventId, String emailId)`, que lança `ResendApiException` nas falhas do Resend.

- [ ] **Step 1: Escrever os testes que falham**

`InboundEmailApiTests`:
- configuração: `@SpringBootTest(properties = {"vagaviva.resend.api-key=re_test", "vagaviva.resend.webhook-secret=whsec_MfKQ9r8GKYqrTwjUPD8ILPZIo2LaLaSw", "vagaviva.resend.from=Vaga Viva <vaga@vagaviva.dev>", "vagaviva.resend.public-url=https://vagaviva.dev"})`, `@AutoConfigureMockMvc`, `@Import(TestcontainersConfiguration.class)`, `@ExtendWith(OutputCaptureExtension.class)`, `@MockitoBean ResendApi resend`. O `Clock` fica o real, e os eventos são assinados com `Instant.now()`.
- Um helper `signed(String eventId, String json)` monta os três cabeçalhos `svix-*`, com a assinatura calculada no teste com `Mac`.
- O evento usado é `{"type":"email.received","data":{"email_id":"e-1","from":"maria@example.com","subject":"Fwd: Vaga Java"}}`.

Casos:
- `repliesWithThePlan`:
  - o `receivedEmail("e-1")` devolve o conteúdo de `gmail-pt.txt`, com `messageId` igual a `"<m1@x>"`;
  - a resposta é `200`;
  - o `send` recebe um `OutgoingEmail` com `to` igual a `"maria@example.com"`, `subject` igual a `"Re: Fwd: Vaga Java"`, `text` contendo `"https://vagaviva.dev/#vaga="`, os headers `In-Reply-To` e `References` iguais a `"<m1@x>"`, `Auto-Submitted` igual a `"auto-replied"` e `idempotencyKey` igual a `"inbound/evt-1"`.
- `longPostingRepliesWithoutLink`: um corpo aleatório de 20 000 caracteres gera um `send` cujo `text` não contém `"#vaga="`.
- `shortPostingGetsHelp`: um corpo de 50 caracteres gera um `send` com o texto de ajuda.
- `ignoresOtherEventTypes`: `{"type":"email.sent"}` dá `200`, sem `receivedEmail` e sem `send`.
- `rejectsABadSignature`: `401`, sem chamadas ao Resend.
- `processesARepeatedEventOnce`: o mesmo `svix-id` enviado duas vezes dá `200` nas duas e um `send` só.
- Ignoram, com `200` e sem `send`:
  - `ignoresOurOwnAddress`: `from` igual a `vaga@vagaviva.dev`;
  - `ignoresMailerDaemon`: `MAILER-DAEMON@x.com`;
  - `ignoresAutoSubmitted`: header `auto-submitted: auto-replied`. *Já o valor `no` é respondido.*
- `limitsTheSender`: 6 eventos diferentes do mesmo remetente dão 5 `send`s.
- `retriesWhenResendFails`:
  - quando o `receivedEmail` lança `ResendApiException(500)`, a resposta é `503`;
  - em seguida, o mesmo `svix-id` com o Resend funcionando dá `200` e um `send`, porque o id não foi lembrado.
- `retriesWhenSendingFails`: quando o `send` lança, a resposta é `503`.
- `isOffWithoutConfiguration`: numa classe aninhada `@Nested` ou num teste separado com `webhook-secret` vazio, a resposta é `503`.
- `logsNeitherSenderNorBody(CapturedOutput output)`: depois de `repliesWithThePlan` e de `retriesWhenSendingFails`, o output não contém `"maria@example.com"`, `"Desenvolvedora Java"` nem `"#vaga="`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=InboundEmailApiTests`
Expected: falha de compilação.

- [ ] **Step 3: Implementar o controller**

O método é `ResponseEntity<Void> receive(@RequestHeader(name="svix-id", required=false) String id, ...timestamp, ...signature, @RequestBody(required=false) byte[] body)`.

1. Se `!properties.inboundEnabled()`, responder `503`.
2. Conferir `SvixSignature.isValid(..., clock.instant())`; se falhar, responder `401` e logar `"Rejected an inbound e-mail with an invalid signature"`.
3. Ler `type` e `data.email_id` com `JsonMapper`. Se `type` for diferente de `email.received`, responder `200`.
4. Chamar `service.handle(id, emailId)` e logar `"Inbound e-mail {}: {}"` com o id e o `Outcome` em minúsculas.
5. Se o service lançar `ResendApiException`, responder `503` e logar `"Inbound e-mail {}: failed ({})"` com o status.

Diferente do webhook do GitHub, o processamento é síncrono, para que o `503` provoque uma nova tentativa do Resend.

- [ ] **Step 4: Implementar `InboundEmailService.handle`**

1. Se `limits.seen(eventId)`, devolver `DUPLICATE`.
2. Buscar o e-mail.
3. Aplicar as regras de loop sobre `InboundLimits.address(email.from())`: o próprio `from`, a parte local `mailer-daemon` ou `postmaster`, ou um header `auto-submitted` presente e diferente de `no`. Se alguma valer, chamar `remember` e devolver `IGNORED`.
4. Se `!limits.tryAcquire(from)`, chamar `remember` e devolver `RATE_LIMITED`.
5. Extrair o texto. Se ele tiver menos de 200 caracteres, enviar `help` e devolver `NO_POSTING`. Se não, planejar e enviar `plan` com `PostingLink.of(...)` e devolver `PLANNED`.
6. Chamar `remember(eventId)` só depois de o `send` retornar.

- [ ] **Step 5: Liberar a rota no `SecurityConfig`**

Acrescentar `api.matcher("/api/inbound-email")` ao `ignoringRequestMatchers` do CSRF, com o comentário `// Resend signs its webhooks instead (Svix).`. A rota já está em `anyRequest().permitAll()`.

- [ ] **Step 6: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=InboundEmailApiTests`
Expected: PASS.

- [ ] **Step 7: Commit**: `Answer a forwarded posting with its plan by e-mail`

### Task 7: A página abre o link `#vaga=` e o guia de deploy

**Files:**
- Modify: `src/main/resources/static/index.html`, `docs/deploy.md`

**Interfaces:**
- Consumes: o formato do link da Tarefa 4.

- [ ] **Step 1: Implementar na página**

Acrescentar, antes do bloco "Back from GitHub":

```js
  async function postingFromLink() {
    if (!location.hash.startsWith('#vaga=')) return null;
    const value = location.hash.slice('#vaga='.length);
    history.replaceState(null, '', '/');
    const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
    const bytes = Uint8Array.from(atob(base64 + '='.repeat((4 - base64.length % 4) % 4)), (c) => c.charCodeAt(0));
    const stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
    return await new Response(stream).text();
  }
```

No fim do script:
- Se não houver `pending && comingBack` e não houver `returnTo`, chamar `postingFromLink()`.
- Se der certo e o texto não estiver vazio, preencher `$('posting').value` e chamar `plan(text)`.
- Se lançar, chamar `setStatus('Não conseguimos abrir a vaga desse link. Cole o texto aqui.', true)`.

A vaga pendente da volta do GitHub continua com prioridade.

- [ ] **Step 2: Escrever o guia**

Acrescentar a `docs/deploy.md` uma seção "E-mail (Resend)" com:
1. verificar o domínio de envio e ativar o recebimento no domínio, ou usar o endereço de recebimento do Resend;
2. criar o webhook `email.received` para `https://<app>/api/inbound-email`;
3. pôr no Render `RESEND_API_KEY`, `RESEND_WEBHOOK_SECRET` (o `whsec_…` do webhook), `RESEND_FROM` e `VAGAVIVA_PUBLIC_URL`;
4. testar encaminhando uma vaga fictícia.

- [ ] **Step 3: Verificação manual**, para o checklist do PR

Primeiro, gerar um link pelo Java: um teste temporário, ou o `jshell` com `PostingLink.of` sobre o `gmail-pt.txt`. Depois, com `./mvnw spring-boot:run` e o banco do `compose.yaml`:
- abrir `http://localhost:8080/#vaga=<valor>`: o campo é preenchido, o plano aparece e a barra de endereço mostra só `/`;
- abrir `http://localhost:8080/#vaga=corrompido`: aparece a mensagem de erro.

- [ ] **Step 4: Rodar a suíte inteira**

Run: `./mvnw -B verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**: `Open a posting link on the page and document the Resend setup`

- [ ] **Step 6: Abrir o PR 1** contra o `main`, com `Part of #28` e o checklist da verificação manual.

---

# PR 2 — consentimento e e-mail semanal

Começar numa branch `feat/weekly-email` a partir do `main` atualizado, depois do merge do PR 1.

### Task 8: Tabela e consentimento

**Files:**
- Create: `src/main/resources/db/migration/V3__weekly_email.sql`, `email/WeeklyEmailSubscription.java`, `email/WeeklyEmailSubscriptionRepository.java`, `email/WeeklyEmailController.java`
- Modify: `login/SecurityConfig.java`
- Test: `email/WeeklyEmailSubscriptionRepositoryTests.java`, `email/WeeklyEmailConsentApiTests.java`

**Interfaces:**
- Produces:
  - A migration com o SQL exato da spec, seção "Tabela".
  - `@Entity @Table(name = "weekly_email_subscription") class WeeklyEmailSubscription`:
    - campos `studentLogin` (`@Id`), `email`, `repositoryFullName`, `consentedAt`, `unsubscribeToken` e `lastSentAt`, com getters package-private no estilo do `Enrollment`;
    - `void consent(String email, String repositoryFullName, String token, Instant now)`, que zera o `lastSentAt`;
    - `void sentAt(Instant now)`.
  - `interface WeeklyEmailSubscriptionRepository extends JpaRepository<WeeklyEmailSubscription, String>`:
    - `Optional<WeeklyEmailSubscription> findByUnsubscribeToken(String token)`
    - `List<WeeklyEmailSubscription> findDue(Instant sentBefore)`, com `@Query`: `lastSentAt is null or lastSentAt < :sentBefore`, ordenado por `studentLogin`.
  - `POST /api/weekly-email` com o body `record ConsentRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Pattern(regexp = "[A-Za-z0-9-]+/[A-Za-z0-9._-]+") String repositoryFullName)`, que responde `204`, `400` ou `403`.
  - O token tem 32 bytes de `SecureRandom` em `Base64.getUrlEncoder().withoutPadding()`.
  - O login da sessão é guardado em minúsculas.

- [ ] **Step 1: Escrever os testes que falham**

`WeeklyEmailSubscriptionRepositoryTests` (`@DataJpaTest` + Testcontainers, como o `ClassroomRepositoryTests`):
- `reconsentingSwitchesTheRepositoryAndClearsLastSent`:
  - salvar a linha com `maria/vaga-viva-a`, chamar `sentAt(NOW)` e depois `consent(..., "maria/vaga-viva-b", "t2", NOW.plusSeconds(60))`;
  - recarregada, a linha tem o repositório `b`, o token `t2` e `lastSentAt` nulo;
  - `findDue(NOW)` a contém.
- `findDueSkipsRecentSends`:
  - uma linha com `lastSentAt = NOW.minus(3, DAYS)` e outra com `NOW.minus(7, DAYS)`;
  - `findDue(NOW.minus(6, DAYS))` devolve só a segunda.
- `tokensAreUnique`: salvar duas linhas com o mesmo token lança `DataIntegrityViolationException`.

`WeeklyEmailConsentApiTests` (`@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers, com `oauth2Login()` e `csrf()` como o `StudentRepositoryApiTests`):
- `savesTheConsentOfTheRepositoryOwner`: com o login `Maria` e o corpo `{"email":"maria@example.com","repositoryFullName":"maria/vaga-viva-x"}`, a resposta é `204` e a linha existe com `consentedAt` preenchido.
- `refusesSomeoneElsesRepository`: `joao/vaga-viva-x` dá `403` e não cria linha.
- `refusesAnInvalidEmail`: `"maria"` dá `400`.
- `needsASession`: sem `oauth2Login()`, a resposta é `401`.
- `needsCsrf`: sem `csrf()`, a resposta é `403`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest='WeeklyEmailSubscriptionRepositoryTests,WeeklyEmailConsentApiTests'`
Expected: falha de compilação.

- [ ] **Step 3: Implementar a migration, a entidade, o repositório e o endpoint**

- O endpoint pega o login com `OAuth2User.getAttribute("login")`, como o `StudentRepositoryController.login`.
- Compara o dono do repositório sem diferenciar maiúsculas.
- Faz upsert com `findById(login).orElseGet(new)` e `consent(...)`.
- No `SecurityConfig`, acrescentar `api.matcher("/api/weekly-email")` à lista `.authenticated()`. O CSRF continua valendo.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest='WeeklyEmailSubscriptionRepositoryTests,WeeklyEmailConsentApiTests'`
Expected: PASS.

- [ ] **Step 5: Commit**: `Store the weekly e-mail consent of a repository owner`

### Task 9: Descadastro

**Files:**
- Create: `src/main/resources/static/descadastrar.html`, `email/WeeklyEmailPages.java`
- Modify: `email/WeeklyEmailController.java`, `login/SecurityConfig.java`
- Test: `email/WeeklyEmailUnsubscribeApiTests.java`

**Interfaces:**
- Produces:
  - `POST /api/weekly-email/unsubscribe`:
    - aceita JSON `{"token":"..."}` ou `?t=<token>` com o corpo `List-Unsubscribe=One-Click` (`application/x-www-form-urlencoded`);
    - sempre responde `204`;
    - apaga a linha do token, se existir.
  - `descadastrar.html`:
    - lê `t` de `location.search` e mostra um botão "Parar de receber" que faz o POST JSON;
    - depois mostra "Pronto. Você não vai mais receber o e-mail semanal.";
    - usa `vv.css` e o cabeçalho das outras páginas.

- [ ] **Step 1: Escrever os testes que falham**

- `deletesTheRowWithTheJsonToken`: `204`, e a linha some.
- `oneClickUnsubscribeWorks`: com `?t=tok` e o corpo `List-Unsubscribe=One-Click`, a resposta é `204` e a linha some.
- `unknownTokenStillAnswers204`.
- `needsNoSessionNorCsrf`: os dois casos acima, sem `oauth2Login()` e sem `csrf()`.
- `pageDeletesNothingByItself`: `GET /descadastrar?t=tok` dá `200` com HTML, e a linha continua lá.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=WeeklyEmailUnsubscribeApiTests`
Expected: FAIL.

- [ ] **Step 3: Implementar**

- No `SecurityConfig`, acrescentar ao CSRF `api.matcher("/api/weekly-email/unsubscribe")`, com o comentário `// The unsubscribe token is the credential.`.
- `/descadastrar` precisa servir `descadastrar.html`. Seguir o padrão do `ClassroomPages`, que é um `@Controller`: criar `email/WeeklyEmailPages.java` com `@GetMapping("/descadastrar")`, devolvendo `"forward:/descadastrar.html"`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=WeeklyEmailUnsubscribeApiTests`
Expected: PASS.

- [ ] **Step 5: Commit**: `Let students stop the weekly e-mail with one click`

### Task 10: Leitura do repositório do aluno para o semanal

**Files:**
- Create:
  - `review/StudentPlans.java` (public)
  - `review/RepositorySnapshot.java` (public record)
  - `review/RepositorySnapshots.java` (public)
  - `classroom/ClassroomStandings.java` (public)
- Modify:
  - `review/ReviewContextBuilder.java`, que passa a usar `StudentPlans`
  - `review/ReviewTexts.java`
  - `review/InstallationTokens.java`
  - `review/ReviewConfig.java`
  - `github/GitHubApi.java`, `github/GitHubClient.java`
- Test: `github/GitHubClientTests.java` (casos novos), `review/RepositorySnapshotsTests.java`, `classroom/ClassroomStandingsTests.java`

**Interfaces:**
- Produces:
  - `GitHubApi.repositoryInstallation(String appJwt, String repositoryFullName)`:
    - devolve `OptionalLong`, a partir de `GET /repos/{repo}/installation`;
    - 404 dá vazio.
  - `GitHubApi.latestPullRequest(String token, String repositoryFullName)`:
    - devolve `Optional<GitHubPullRequest>`, a partir de `GET /repos/{repo}/pulls?state=all&sort=updated&direction=desc&per_page=1`;
    - vazio quando não há PR.
  - `InstallationTokens.forRepository(String repositoryFullName)`: devolve `Optional<String>`, com `repositoryInstallation` mais `forInstallation`.
  - `public final class StudentPlans`:
    - `public Optional<StudentPlan> read(String token, String repositoryFullName)`, com o corpo movido de `ReviewContextBuilder.readPlan`, sem mudar o comportamento;
    - é um bean em `ReviewConfig` (`new StudentPlans(github, catalogs)`).
  - `ReviewTexts` vira `public final class`, com `public static Optional<String> reviewText(String body)`:
    - vazio se não começa com `MARKER`;
    - sem isso, devolve o texto sem `MARKER` e sem `FOOTER`, com `strip()`.
  - `public record RepositorySnapshot(StudentPlan plan, Optional<TestReport> report, Optional<String> lastReview)`
  - `public class RepositorySnapshots`:
    - `public Optional<RepositorySnapshot> take(String repositoryFullName)`, vazio quando o app não está instalado, o repositório não existe (`GitHubApiException` com status 404) ou não há plano legível;
    - `report` vem de `FailedTestsSource.report(token, repo, sha)`, onde `sha` é o `headSha` do PR mais recente ou `latestCommitSha` se não houver PR;
    - `lastReview` é o último comentário do PR mais recente com `bot()` verdadeiro, `authorLogin()` igual a `app.botLogin()` e `reviewText` presente.
    - é um bean em `ReviewConfig`.
  - `public class ClassroomStandings`:
    - `public Optional<Standing> forRepository(String repositoryFullName)`, com `public record Standing(String currentMissionId, boolean completed)`;
    - lê `EnrollmentRepository.findByRepositoryFullName` e o `EnrollmentProgress` dele;
    - é anotado com `@Component`.

- [ ] **Step 1: Escrever os testes que falham**

- `GitHubClientTests`:
  - `findsTheInstallationOfARepository`: com `{"id": 42}`, o resultado é `OptionalLong.of(42)`;
  - `noInstallationIsEmpty`: um 404 dá vazio;
  - `readsTheLatestPullRequest`: a URI exata acima, com `[{"number":7,"head":{"sha":"abc"}}]`, dá `number` 7 e `headSha` `"abc"`;
  - `noPullRequestIsEmpty`: `[]` dá vazio.
- `RepositorySnapshotsTests`, com `GitHubApi` e `FailedTestsSource` mockados e `InstallationTokens` real sobre o mock:
  - `takesPlanReportAndLastReview`: com dois comentários do bot, o `lastReview` é o texto do mais recente, sem o marcador e sem o rodapé;
  - `ignoresHintCommentsAndStudentComments`: um comentário `<!-- vaga-viva:hint ...` e um comentário do aluno com o marcador de revisão não contam;
  - `usesTheDefaultBranchWithoutPullRequests`: o `report` é pedido com o sha do `latestCommitSha`;
  - `emptyWhenTheAppIsNotInstalled`;
  - `emptyWhenThereIsNoPlan`.
- `ClassroomStandingsTests` (`@DataJpaTest`): uma matrícula com progresso na missão `patients-api` devolve `Standing("patients-api", false)`; um repositório desconhecido dá vazio.
- Os testes existentes do revisor (`SocraticReviewerTests`, `WebhookRoutingTests`) continuam passando sem mudança.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest='GitHubClientTests,RepositorySnapshotsTests,ClassroomStandingsTests'`
Expected: falha de compilação.

- [ ] **Step 3: Implementar** as interfaces acima.

- [ ] **Step 4: Rodar a suíte inteira**, por causa da refatoração do `ReviewContextBuilder`

Run: `./mvnw -B verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**: `Read a student's plan, tests and last review for the weekly e-mail`

### Task 11: Texto e montagem do e-mail semanal

**Files:**
- Create: `email/WeeklyEmailTemplate.java`, `email/WeeklyEmailService.java`
- Test: `email/WeeklyEmailTemplateTests.java`, `email/WeeklyEmailServiceTests.java`

**Interfaces:**
- Consumes:
  - `RepositorySnapshots.take`, `ClassroomStandings.forRepository` (Tarefa 10);
  - `MissionProgress.from(List<String>, TestReport)`, que é `OnMission`, `Completed` ou `Unknown`;
  - `WeeklyEmailSubscriptionRepository.findDue` (Tarefa 8);
  - `ResendApi.send`, `ResendProperties.publicUrl()`.
- Produces:
  - `record Summary(int sent, int skipped, int failed)`
  - `Summary WeeklyEmailService.sendDue()`
  - `WeeklyEmailService(WeeklyEmailSubscriptionRepository, RepositorySnapshots, ClassroomStandings, ResendApi, ResendProperties, Clock, Pause)`, com `@FunctionalInterface interface Pause { void sleep(Duration d); }` aninhada, como a `Pause` do `StudentRepositoryProvisioner`.
  - `WeeklyEmailTemplate.mission(...)`: `static PlanReplyTemplate.Reply mission(String repositoryFullName, int order, Mission mission, boolean testsSeen, Optional<String> lastReview, String unsubscribeUrl)`.
  - `WeeklyEmailTemplate.completed(...)`: `static PlanReplyTemplate.Reply completed(String repositoryFullName, String unsubscribeUrl)`.

- [ ] **Step 1: Escrever os testes que falham**

`WeeklyEmailTemplateTests`:
- `missionEmailShowsMissionReviewAndUnsubscribe`:
  - o `subject` é `"Sua missão da semana: " + mission.title()`;
  - o texto contém `"https://github.com/maria/vaga-viva-x/issues"`, a revisão e o `unsubscribeUrl`.
- `htmlEscapesTheReview`: a revisão `"<script>x</script> **ok**"` aparece escapada no `html`.
- `cutsLongReviewsAtAWord`: uma revisão de 700 caracteres sai com no máximo 600 mais `"…"` e termina em fim de palavra.
- `withoutTestsOrReview`: com `testsSeen = false` e `lastReview` vazio, o texto contém as duas frases exatas da seção Global Constraints.
- `completedSubject`: o assunto é `"Você concluiu o projeto!"`.

`WeeklyEmailServiceTests`, com mocks, um `Clock.fixed(Instant.parse("2026-11-16T11:00:00Z"))` e `Pause` sem espera:
- `sendsToADueSubscriber`:
  - o snapshot mostra a missão 2 falhando;
  - o resultado é `Summary(1, 0, 0)`;
  - o `send` recebe o e-mail do aluno, os headers `List-Unsubscribe` igual a `"<https://vagaviva.dev/api/weekly-email/unsubscribe?t=tok>"` e `List-Unsubscribe-Post` igual a `"List-Unsubscribe=One-Click"`, e `idempotencyKey` igual a `"weekly/maria/2026-11-16"`;
  - a linha ganha `lastSentAt = now`.
- `nobodyDueSendsNothing`: o `findDue` vazio dá `Summary(0, 0, 0)` e nenhum `send`.
- `askedForSixDaysAgo`: o `findDue` é chamado com `now.minus(6, DAYS)`.
- `skipsAMissingRepository`: o snapshot vazio dá `skipped = 1`, sem `send` e sem `lastSentAt`.
- `congratulatesAndStopsWhenCompleted`: com `MissionProgress.Completed`, o e-mail sai com o assunto de conclusão e a linha é apagada.
- `classroomStudentsUseTheirStanding`: com `ClassroomStandings` devolvendo `Standing("patients-api", false)`, o e-mail traz a missão `patients-api` mesmo com o `report` vazio.
- `unknownProgressFallsBackToTheFirstMission`: com o `report` vazio e sem turma, o e-mail traz a primeira missão e `testsSeen = false`.
- `oneFailureDoesNotStopTheBatch`: dois inscritos, o primeiro `send` lança `ResendApiException(500)`; o resultado é `Summary(1, 0, 1)` e só o segundo ganha `lastSentAt`.
- `logsNoEmailAddress(CapturedOutput)`: o output não contém `"maria@example.com"`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest='WeeklyEmailTemplateTests,WeeklyEmailServiceTests'`
Expected: falha de compilação.

- [ ] **Step 3: Implementar**

- O `sendDue` não é `@Transactional` como um todo: cada aluno salva a própria linha logo depois do `send`.
- A `Pause` de 600 ms é chamada entre envios, não antes do primeiro.
- A ordem da missão é `plan.missions().indexOf(mission) + 1`.
- O `idempotencyKey` usa a segunda-feira da semana: `LocalDate.ofInstant(now, UTC).with(TemporalAdjusters.previousOrSame(MONDAY))`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest='WeeklyEmailTemplateTests,WeeklyEmailServiceTests'`
Expected: PASS.

- [ ] **Step 5: Commit**: `Write and send the weekly mission e-mail`

### Task 12: Disparo semanal, consentimento na página e ADR

**Files:**
- Create: `.github/workflows/weekly-email.yml`
- Modify:
  - `email/WeeklyEmailController.java`, `email/EmailConfig.java` (registrar `WeeklyEmailProperties`)
- Create: `email/WeeklyEmailProperties.java`, com `@ConfigurationProperties("vagaviva.weekly-email") record WeeklyEmailProperties(@DefaultValue("") String secret)` e um `toString` que mascara o segredo
  - `application.yaml`, `.env.example`, `login/SecurityConfig.java`
  - `src/main/resources/static/index.html`
  - `docs/adr/0003-privacidade-da-vaga.md`, `docs/deploy.md`
- Test: `email/WeeklyEmailTriggerApiTests.java`

**Interfaces:**
- Consumes: `WeeklyEmailService.sendDue()` (Tarefa 11).
- Produces:
  - `POST /api/weekly-emails` com o header `X-Vaga-Viva-Secret`;
  - responde `200` com o JSON `{"sent":n,"skipped":n,"failed":n}`, `401` para segredo errado ou ausente, e `503` quando o segredo ou o envio do Resend está desligado.

- [ ] **Step 1: Escrever os testes que falham**

`WeeklyEmailTriggerApiTests` (com `@MockitoBean WeeklyEmailService`):
- `runsWithTheSecret`: responde `200` com `{"sent":1,"skipped":0,"failed":0}`, sem sessão e sem CSRF;
- `refusesAWrongSecret`: `401`, e `sendDue` não é chamado;
- `refusesAMissingSecret`: `401`;
- `isOffWithoutConfiguration`: com `vagaviva.weekly-email.secret` vazio, `503`.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./mvnw -B -q test -Dtest=WeeklyEmailTriggerApiTests`
Expected: FAIL.

- [ ] **Step 3: Implementar o endpoint e a configuração**

- O segredo é comparado com `MessageDigest.isEqual` sobre os bytes UTF-8.
- `application.yaml` ganha `vagaviva.weekly-email.secret: ${WEEKLY_EMAIL_SECRET:}`.
- `.env.example` ganha `WEEKLY_EMAIL_SECRET=`.
- No `SecurityConfig`, acrescentar ao CSRF `api.matcher("/api/weekly-emails")`, com o comentário `// The scheduled workflow sends a shared secret instead.`.

- [ ] **Step 4: Rodar e ver passar**

Run: `./mvnw -B -q test -Dtest=WeeklyEmailTriggerApiTests`
Expected: PASS.

- [ ] **Step 5: Criar o workflow**

```yaml
name: Weekly e-mail
on:
  schedule:
    - cron: '0 11 * * 1'
  workflow_dispatch:
jobs:
  send:
    runs-on: ubuntu-latest
    steps:
      - name: Send the weekly e-mails
        run: |
          curl --fail-with-body --silent --show-error --max-time 300 \
            -X POST -H "X-Vaga-Viva-Secret: ${{ secrets.WEEKLY_EMAIL_SECRET }}" \
            "${{ vars.VAGAVIVA_PUBLIC_URL }}/api/weekly-emails"
```

- [ ] **Step 6: Pôr o consentimento na página (`index.html`)**

- Dentro de `#plan`, antes do `#create-button`, entram:
  - um `<input type="email" id="weekly-email" autocomplete="email">` com o rótulo "Seu e-mail";
  - um `<label class="check"><input type="checkbox" id="weekly-consent">` com o texto exato do checkbox, no mesmo padrão de `professor.html:96`.
- `remember`, `recall` e `forget` passam a guardar `{ text, email, consent }` em JSON. `recall` continua aceitando o valor antigo, que era só o texto.
- No clique em criar: se `consent` estiver marcado e o e-mail estiver vazio ou inválido (`checkValidity()`), mostrar no status "Informe um e-mail válido para receber o resumo semanal." e não criar.
- Depois de criar com sucesso e com `consent` marcado, fazer o `fetch('/api/weekly-email', { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrfToken() }, body: JSON.stringify({ email, repositoryFullName: body.fullName }) })`. Se a resposta não for ok, mostrar o texto exato de falha ao salvar o e-mail, sem esconder o repositório criado.

- [ ] **Step 7: Atualizar o ADR 0003 e o guia**

- No ADR 0003, trocar "Do aluno ficam apenas o login do GitHub e o e-mail, com consentimento." por "Do aluno ficam apenas o login do GitHub e o e-mail, com consentimento, e, para quem pede o e-mail semanal, o repositório e um token de descadastro; o descadastro apaga tudo."
- Em `docs/deploy.md`, acrescentar o segredo `WEEKLY_EMAIL_SECRET` no Render e nos *secrets* do repositório no GitHub, e a variável `VAGAVIVA_PUBLIC_URL` nas *variables* do repositório.

- [ ] **Step 8: Rodar a suíte e verificar manualmente**

Run: `./mvnw -B verify`
Expected: BUILD SUCCESS.

Verificação manual, para o checklist do PR:
1. Com o banco local, marcar o checkbox, criar um repositório de teste e conferir a linha em `weekly_email_subscription`.
2. Chamar `curl -X POST -H "X-Vaga-Viva-Secret: <segredo>" localhost:8080/api/weekly-emails` com uma `RESEND_API_KEY` de teste e ver o e-mail chegar.
3. Criar outro repositório sem o checkbox e conferir que nenhum e-mail sai para ele.
4. Clicar no link de descadastro.

- [ ] **Step 9: Commit**: `Send the weekly e-mail every Monday and ask for consent on the page`

- [ ] **Step 10: Abrir o PR 2** contra o `main`, com `Closes #28` e o checklist manual.
