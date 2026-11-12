# Modo turma: plano de implementação

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para implementar este plano tarefa por tarefa. Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:**
- O professor cria uma turma a partir de uma vaga-base.
- Os alunos entram por um link.
- O professor acompanha a turma num painel por missão e recebe um resumo por e-mail toda segunda.

**Arquitetura:**
- Um pacote novo, `io.github.precavidos.vagaviva.classroom`, com as entidades JPA, os serviços, os controllers e as regras de situação.
- O pacote `review` ganha uma porta, `ReviewEvents`. O `classroom` implementa essa porta para gravar o progresso a cada evento do webhook.
- O painel só lê o banco.
- As páginas são HTML estático em `src/main/resources/static`, no estilo de `index.html`.

**Tecnologia:**
- Spring Boot 4.1, Java 21 e Spring Data JPA (`ddl-auto: validate`).
- Flyway e PostgreSQL 17.
- MockMvcTester e Testcontainers para os testes.
- GitHub Actions para o agendamento.

**Desenho:** `docs/superpowers/specs/2026-11-11-classroom-mode-design.md`. Leia junto com este plano. As decisões e as frases para os usuários vêm de lá.

## Restrições gerais

- Código, nomes, commits e PRs em inglês. Textos para professor e aluno em português.
- Toda tela segue o `DESIGN.md` da raiz: tokens, componentes, movimento, voz e acessibilidade.
- O texto da vaga-base nunca é gravado nem registrado no log. A turma guarda só ids: arquétipo, missões, motivos, competências e tecnologias (ADR 0003).
- Os logs só citam o repositório e o número do PR. Nunca vão para o log: payload, nome do aluno, e-mail, mensagem de teste ou pergunta do revisor.
- Limites: `vagaviva.classroom.stuck-after: 3d` e `vagaviva.classroom.idle-after: 5d`. Se as duas regras valerem, vale "parado".
- Só os logins em `vagaviva.classroom.teachers` (lista separada por vírgula, variável `CLASSROOM_TEACHERS`) criam turma. Qualquer outro login recebe 403.
- Nome do aluno: obrigatório, de 1 a 40 caracteres depois do `strip()`.
- Frase de consentimento, exatamente: *"Ao entrar, &lt;professor&gt; vai ver o nome que você escreveu, em qual missão você está, os testes que falham, as perguntas do revisor e quantas dicas você pediu. Seu código não aparece no painel."*
- Mensagem de teste guardada: cortada em 200 caracteres e passada por `ReviewTextFilter.isSafe`. Se o filtro recusar, só o nome do teste é guardado.
- Uma falha no registro de progresso nunca impede o revisor nem as dicas: ela é capturada e registrada no log.
- Nenhum teste chama a rede. O GitHub entra como `@MockitoBean GitHubApi`, como em `StudentRepositoryApiTests`.
- Rodar tudo: `./mvnw verify`, que precisa do Docker para o Testcontainers.

## Pontos de atenção na revisão

1. **Aluno que abre o convite duas vezes.** A segunda vez devolve o mesmo repositório, sem criar outro nem matricular de novo (Tarefa 5).
2. **Build quebrado ou `plan.json` editado.** Um relatório sem testes de alguma missão do plano da turma não muda a missão atual e nunca marca "concluído"; só registra a atividade (Tarefas 3 e 6).
3. **Push numa branch que não é a padrão e não tem PR aberto.** Não grava nada (Tarefa 6).
4. **Painel aberto por quem não é o professor da turma.** Recebe 404, e não 403, para não revelar que a turma existe (Tarefa 7).
5. **Mensagem de teste com trecho de código.** Ela nunca chega ao banco nem ao painel (Tarefa 6).

---

### Tarefa 1: Esquema, ADR 0007 e repositórios

**Arquivos:**
- Criar: `docs/adr/0007-progresso-do-aluno-em-turma.md`
- Criar: `src/main/resources/db/migration/V2__classrooms.sql`
- Criar: `src/main/java/io/github/precavidos/vagaviva/classroom/Classroom.java`, `Enrollment.java`, `EnrollmentProgress.java`, `ClassroomRepository.java`, `EnrollmentRepository.java`, `ClassroomProperties.java`, `ClassroomConfig.java`
- Alterar: `src/main/resources/application.yaml`, `.env.example`
- Teste: `src/test/java/io/github/precavidos/vagaviva/classroom/ClassroomRepositoryTests.java`

**Interfaces:**
- Produz:
  - `Classroom(UUID id, String joinCode, String name, String teacherLogin, String teacherEmail, Instant teacherEmailConsentAt, String plan /* JSON de StoredPlan */, Instant createdAt)`
  - `Enrollment(UUID id, Classroom classroom, String studentLogin, String displayName, String repositoryFullName, Instant consentedAt, Instant enrolledAt)`
  - `EnrollmentProgress`, com o id compartilhado com `Enrollment` (`@MapsId`) e os campos:
    - `String currentMissionId`, `Instant currentMissionSince`, `boolean completed`, `Instant lastActivityAt`;
    - `String failingTests` (JSON de `[{name, message}]`) e `String pullRequestsOnMission` (JSON de `[número]`);
    - `String lastReviewerQuestion` e `String lastPullRequestUrl`;
    - `String hintsByMission` (JSON de `{missionId: número}`).
  - Repositórios:
    - `ClassroomRepository.findByJoinCode(String)`;
    - `ClassroomRepository.findByTeacherLoginOrderByCreatedAtDesc(String)`;
    - `EnrollmentRepository.findByRepositoryFullName(String)`;
    - `EnrollmentRepository.findByClassroomAndStudentLogin(Classroom, String)`;
    - `EnrollmentRepository.findByClassroomOrderByEnrolledAt(Classroom)`.
  - `ClassroomProperties(Duration stuckAfter, Duration idleAfter, Set<String> teachers, String summarySecret)`, com o prefixo `vagaviva.classroom`.
  - `ClassroomConfig` declara um bean `Clock` (`Clock.systemUTC()`), usado pelo pacote `classroom`.

- [ ] **Passo 1: Escrever o ADR 0007.** Siga o formato dos ADRs 0003 e 0005. Para quem entra numa turma e consente, ficam guardados:
  - o nome escolhido;
  - o login e o repositório;
  - a missão atual e o tempo nela;
  - os nomes dos testes que falham e as mensagens filtradas;
  - a última pergunta do revisor e o link do último PR;
  - a contagem de dicas por missão;
  - o e-mail do professor, se ele consentir.

  Para o aluno sozinho, nada muda. Exclusões no piloto são feitas à mão. Inclua as "Considered Options": consultar o GitHub na hora ou gravar a cada evento. A escolhida é gravar a cada evento.
- [ ] **Passo 2: Escrever o teste que falha.** `ClassroomRepositoryTests`, com `@DataJpaTest`, `@Import(TestcontainersConfiguration.class)` e `@AutoConfigureTestDatabase(replace = NONE)`:
  - `savesAClassroomWithItsEnrollmentAndProgress`: grava e relê os três registros e confere `findByRepositoryFullName("maria/vaga-viva-clinic-scheduling")`.
  - `rejectsTheSameRepositoryTwice`: `assertThatThrownBy(...)`, depois de `saveAndFlush`, lança `DataIntegrityViolationException`.
  - `rejectsTheSameStudentTwiceInAClassroom`: idem.
- [ ] **Passo 3: Rodar** `./mvnw test -Dtest=ClassroomRepositoryTests`. Esperado: FALHA na compilação.
- [ ] **Passo 4: Escrever `V2__classrooms.sql` e as entidades.**
  - Três tabelas: `classroom`, `enrollment` e `enrollment_progress`, com os JSON em `jsonb`, mapeados com `@JdbcTypeCode(SqlTypes.JSON)` como `String`.
  - `classroom.join_code` é único.
  - `enrollment.repository_full_name` é único, e `(classroom_id, student_login)` também.
  - `display_name varchar(40) not null`.
- [ ] **Passo 5: Configurar.** Em `application.yaml`, acrescente:

  ```yaml
  vagaviva:
    classroom:
      stuck-after: 3d
      idle-after: 5d
      teachers: ${CLASSROOM_TEACHERS:}
      summary-secret: ${CLASSROOM_SUMMARY_SECRET:}
  ```

  Em `.env.example`, acrescente `CLASSROOM_TEACHERS=` e `CLASSROOM_SUMMARY_SECRET=` vazios, com um comentário de uma linha.
- [ ] **Passo 6: Rodar** `./mvnw test -Dtest=ClassroomRepositoryTests`. Esperado: PASSA.
- [ ] **Passo 7: Commit:** `Store classrooms, enrollments and student progress`.

### Tarefa 2: Relatório completo dos testes

O revisor só precisa das falhas. A turma precisa saber também **quais missões tiveram testes executados**, sem contar os pulados (`<skipped>`), para decidir "concluído" e para ignorar relatórios parciais.

**Arquivos:**
- Criar: `src/main/java/io/github/precavidos/vagaviva/review/TestReport.java`
- Alterar: `review/JunitReports.java`, `review/FailedTestsSource.java`, `review/JunitFailedTests.java`
- Teste: `src/test/java/io/github/precavidos/vagaviva/review/JunitReportsTests.java`

**Interfaces:**
- Produz:
  - `public record TestReport(Set<String> missionsRun, List<FailedTest> failed)`. `missionsRun` são os ids de missão com pelo menos um `testcase` não pulado. `failed` vem na ordem do relatório e não é filtrado pelo plano.
  - `static TestReport JunitReports.read(byte[] zip)`. O `failedTests(zip)` existente passa a ser `read(zip).failed()`.
  - Na interface `FailedTestsSource`: `Optional<TestReport> report(String token, String repositoryFullName, String headSha)`. O `failedTests(...)` vira um método `default` que filtra e ordena `report(...)` pelo plano, como `inPlan` faz hoje.

- [ ] **Passo 1: Escrever os testes que falham**, em `JunitReportsTests`, com o mesmo padrão de XML dos testes existentes:
  - `reportsWhichMissionsRan`: um XML com `Missão patients-api:` (1 passa, 1 falha) e `Missão persistence:` (1 passa) resulta em `missionsRun = {patients-api, persistence}` e uma falha.
  - `skippedTestsDoNotCountAsRun`: `Missão pagination:`, com todos os `testcase` tendo `<skipped/>`, fica fora de `missionsRun`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest=JunitReportsTests`. Esperado: FALHA na compilação.
- [ ] **Passo 3: Implementar** `TestReport`, `JunitReports.read` e `FailedTestsSource.report`. Em `JunitFailedTests`, o corpo atual vira `report(...)` (a busca da execução, do artefato e o download), devolvendo `JunitReports.read(zip)`.
- [ ] **Passo 4: Rodar** `./mvnw test -Dtest='JunitReportsTests,WebhookRoutingTests,SocraticReviewerTests'`. Esperado: PASSA. O lambda de `FailedTestsSource` em `WebhookRoutingTests` precisa virar uma implementação de `report`.
- [ ] **Passo 5: Commit:** `Tell which missions ran in the student's test report`.

### Tarefa 3: Regras da missão atual e da situação

Lógica pura, sem Spring e sem banco.

**Arquivos:**
- Criar: `classroom/MissionProgress.java`, `classroom/StudentStatus.java`
- Teste: `classroom/MissionProgressTests.java`, `classroom/StudentStatusTests.java`

**Interfaces:**
- Consome: `TestReport` (Tarefa 2).
- Produz:
  - `sealed interface MissionProgress`, com:
    - `record OnMission(String missionId, List<FailedTest> failing)`;
    - `record Completed()`;
    - `record Unknown()` (relatório parcial: não muda nada).
  - `static MissionProgress MissionProgress.from(List<String> planMissionIds, TestReport report)`.
  - `enum StudentStatus { ADVANCING, STUCK, IDLE, COMPLETED }`.
  - `static StudentStatus StudentStatus.of(boolean completed, Instant currentMissionSince, Instant lastActivityAt, Instant now, Duration stuckAfter, Duration idleAfter)`.

Regras:
- `MissionProgress.from` devolve `Unknown` se alguma missão do plano não está em `missionsRun`.
- Senão, devolve `OnMission` com a primeira missão do plano que tem falha, levando as falhas dela.
- Senão, devolve `Completed`.
- `StudentStatus.of` testa nesta ordem: `COMPLETED` se `completed`; `IDLE` se `now - lastActivityAt >= idleAfter`; `STUCK` se `now - currentMissionSince >= stuckAfter`; senão, `ADVANCING`.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `MissionProgressTests`:
    - `firstFailingMissionInPlanOrder`: plano `[patients-api, persistence, pagination]`, falhas em `pagination` e `persistence`. Resulta em `OnMission("persistence", <só as falhas de persistence>)`.
    - `completedWhenEveryPlannedMissionRanAndPassed`.
    - `unknownWhenAPlannedMissionDidNotRun`: `missionsRun` sem `pagination` resulta em `Unknown`, mesmo sem falhas.
    - `missionsOutsideThePlanAreIgnored`: uma falha em `bug-hunt`, que não está no plano, resulta em `Completed`.
  - `StudentStatusTests`, com `now = 2026-11-20T12:00Z`, 3 dias e 5 dias:
    - `idleWinsOverStuck`: na missão há 6 dias e última atividade há 5 dias resulta em `IDLE`.
    - `stuckAfterThreeDaysOnTheMission`: na missão há 3 dias e atividade há 1 hora resulta em `STUCK`.
    - `advancingBeforeThreeDays`: 2 dias e 23 horas resulta em `ADVANCING`.
    - `completedIgnoresTime`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest='MissionProgressTests,StudentStatusTests'`. Esperado: FALHA na compilação.
- [ ] **Passo 3: Implementar.**
- [ ] **Passo 4: Rodar o mesmo comando.** Esperado: PASSA.
- [ ] **Passo 5: Commit:** `Decide each student's current mission and status`.

### Tarefa 4: Plano guardado e criação da turma

**Arquivos:**
- Criar: `classroom/StoredPlan.java`, `classroom/ClassroomService.java`, `classroom/ClassroomController.java`, `classroom/JoinCodes.java`
- Alterar: `login/SecurityConfig.java`
- Teste: `classroom/StoredPlanTests.java`, `classroom/ClassroomApiTests.java`

**Interfaces:**
- Consome: `ProjectPlanners.plan(ExtractedJobPosting)`, `JobPostingExtractor.extract(String)`, `MissionCatalog` e a Tarefa 1.
- Produz:
  - `record StoredPlan(String archetype, ArchetypeFit archetypeFit, SeniorityLevel seniorityLevel, int difficultyCap, List<StoredMission> missions, List<String> postingSkills, List<String> postingTechnologies)`, com `record StoredMission(String id, PlannedMission.Reason reason, List<String> matchedSkills)`.
  - `static StoredPlan StoredPlan.of(ProjectPlan)`, `ProjectPlan toProjectPlan(MissionCatalog)`, `String toJson()` e `static StoredPlan fromJson(String)`.
  - `List<String> StoredPlan.missionIds()`.
  - `ClassroomService.create(String teacherLogin, String postingText, String name, String teacherEmail, boolean emailConsent) -> Classroom`.
  - `ClassroomService.byJoinCode(String) -> Optional<Classroom>`.
  - `ClassroomService.ofTeacher(String) -> List<Classroom>`.
  - `JoinCodes.next() -> String`: 10 caracteres de `[a-z0-9]`, com `SecureRandom`.

Rotas:
- `POST /api/classrooms`, com o corpo `{text, name, teacherEmail?, emailConsent}`:
  - 201 com `{id, joinCode, inviteUrl, name}`, em que `inviteUrl` é `"/turma/" + joinCode`;
  - 403 em português (*"Só professores do piloto podem criar turmas."*) se o login não estiver em `teachers`;
  - 400 se houver e-mail sem consentimento ou um nome vazio ou com mais de 80 caracteres.
- `GET /api/classrooms`: as turmas do professor logado, com `{id, name, joinCode, students}`.
- `GET /api/classrooms/{joinCode}`: sem login, devolve `{name, teacherLogin, archetypeName, missions:[{order, id, title}]}` e 404 se o código não existir. Nunca lista os alunos.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `StoredPlanTests.roundTripsAPlan`: `ProjectPlan`, depois `of`, `toJson`, `fromJson` e `toProjectPlan(catalog)`, devolve as mesmas missões, motivos, competências e domínio. Use um plano do `ProjectPlanners` real com `MissionCatalog.fromClasspath`.
  - `StoredPlanTests.jsonHasNoPostingWords`: o JSON de um plano feito a partir de uma vaga que contém `"rh@empresa.example"` não contém `"rh@"`.
  - `ClassroomApiTests`, com `@SpringBootTest`, MockMvcTester e `@TestPropertySource(properties = "vagaviva.classroom.teachers=david")`:
    - `aTeacherCreatesAClassroom`;
    - `onlyPilotTeachersCreateClassrooms` (o login `maria` recebe 403);
    - `creatingRequiresSigningIn` (401);
    - `listsOnlyTheTeachersOwnClassrooms`;
    - `theInvitePageNeedsNoLoginAndListsNoStudents`;
    - `unknownJoinCodeIs404`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest='StoredPlanTests,ClassroomApiTests'`. Esperado: FALHA.
- [ ] **Passo 3: Implementar.**
  - O controller leva `@ReceivesJobPostingText`, como `StudentRepositoryController`.
  - Em `SecurityConfig`, `/api/classrooms/**` passa a exigir login. A exceção é um `GET` em `/api/classrooms/{joinCode}`, que fica aberto.
  - O CSRF continua valendo para as rotas com sessão.
- [ ] **Passo 4: Rodar o mesmo comando.** Esperado: PASSA.
- [ ] **Passo 5: Commit:** `Let pilot teachers create a classroom from a base posting`.

### Tarefa 5: Entrada pelo convite

**Arquivos:**
- Criar: `classroom/EnrollmentService.java`, `classroom/EnrollmentController.java`
- Teste: `classroom/EnrollmentApiTests.java`

**Interfaces:**
- Consome: `StudentRepositoryProvisioner.provision(String token, String studentLogin, ProjectPlan plan) -> ProvisionedRepository`, `GitHubApi.isAppInstalledOn`, `StoredPlan.toProjectPlan` e a Tarefa 1.
- Produz: `EnrollmentService.enroll(Classroom, String studentLogin, String displayName, String token) -> Enrollment`. Ele cria o `EnrollmentProgress` inicial com:
  - `currentMissionId` = a primeira missão do plano;
  - `currentMissionSince = lastActivityAt = enrolledAt = now`;
  - `failingTests = []`, `pullRequestsOnMission = []` e `hintsByMission = {}`.

Rota: `POST /api/classrooms/{joinCode}/enrollments`, com o corpo `{displayName, consent}`.
- 400 se `consent` não for `true` ou se o nome estiver fora de 1 a 40 caracteres.
- 409, com `installUrl`, se o app não estiver instalado. A mensagem é a mesma de `StudentRepositoryController`; extraia uma constante compartilhada.
- 201 com `{fullName, htmlUrl}` na primeira vez.
- 200 com o mesmo repositório, sem chamar `provision`, se o aluno já estiver na turma.
- Os erros do GitHub são tratados como em `StudentRepositoryController.gitHubFailed`.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `aStudentJoinsWithNameAndConsent`: `provision` é chamado com o `ProjectPlan` da turma, e as linhas de matrícula e progresso existem.
  - `joiningTwiceReturnsTheSameRepository`: `verify(provisioner, times(1))`.
  - `consentIsRequired`.
  - `nameIsRequiredAndAtMost40Characters`: casos `"   "` e 41 caracteres.
  - `asksToInstallTheAppFirst`.
  - `unknownJoinCodeIs404`.

  Use `@MockitoBean` em `StudentRepositoryProvisioner` e em `GitHubApi`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest=EnrollmentApiTests`. Esperado: FALHA.
- [ ] **Passo 3: Implementar.** Use `@Transactional` em `enroll`; o `provision` roda antes de gravar.
- [ ] **Passo 4: Rodar.** Esperado: PASSA.
- [ ] **Passo 5: Commit:** `Let students join a classroom by its invite link`.

### Tarefa 6: Registro do progresso pelo webhook

**Arquivos:**
- Criar: `review/ReviewEvents.java`, `classroom/ProgressRecorder.java`
- Alterar: `review/WebhookDispatcher.java`, `review/SocraticReviewer.java`
- Teste: `review/WebhookRoutingTests.java`, `review/SocraticReviewerTests.java`, `classroom/ProgressRecorderTests.java`

**Interfaces:**
- Consome: as Tarefas 1, 2 e 3 e `ReviewTextFilter.isSafe`.
- Produz: `public interface ReviewEvents`, com:
  - `boolean tracks(String repositoryFullName)`;
  - `void activity(String repositoryFullName)`;
  - `void testsRan(String repositoryFullName, Integer pullRequestNumber /* null num push na branch padrão */, TestReport report)`;
  - `void reviewerAsked(String repositoryFullName, int pullRequestNumber, String question)`;
  - `void hintGiven(String repositoryFullName, String missionId)`.

  `ProgressRecorder` é o `@Component` que implementa a interface. Cada método captura `RuntimeException` e registra um `warn` só com o repositório.

Alterações em `WebhookDispatcher.workflowRun`:
- Leia `workflow_run.head_branch` e `repository.default_branch`.
- Se `events.tracks(repo)` e (há PR ou `head_branch` é igual a `default_branch`): busque `failedTests.report(token, repo, headSha)`. Se houver relatório, chame `events.testsRan(repo, prNumber, report)`; se não, chame `events.activity(repo)`.
- A revisão do PR continua como hoje e continua ignorando os runs sem PR.

Em `issueComment`, para um `/dica` válido, chame `events.activity(repo)` antes de responder.

Em `SocraticReviewer.review`, depois de postar, chame `events.reviewerAsked(repo, number, text)` com o `text` interno, sem o rodapé nem o marcador.

`ProgressRecorder.testsRan`:
- Use `MissionProgress.from(storedPlan.missionIds(), report)`.
- **`Unknown`:** só `lastActivityAt = now`.
- **`Completed`:** `completed = true`, `failingTests = []` e a atividade.
- **`OnMission(m, failing)`:**
  - se `m` é diferente da missão atual: missão = `m`, `since = now` e `pullRequestsOnMission = []`;
  - `completed = false`;
  - `failingTests` = `failing` mapeado para `{name, message}`, com a mensagem cortada em 200 caracteres e trocada por `null` se `!ReviewTextFilter.isSafe(message)`;
  - se `pullRequestNumber != null`, acrescenta o número a `pullRequestsOnMission`, sem repetir, e define `lastPullRequestUrl = "https://github.com/" + repo + "/pull/" + n`;
  - `lastActivityAt = now`.

`reviewerAsked` guarda a pergunta em `lastReviewerQuestion` só se `isSafe`; os textos fixos do revisor passam no filtro. `hintGiven` soma 1 em `hintsByMission[missionId]`. Para repositórios fora de turma, todos os métodos não fazem nada.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `ProgressRecorderTests`, com `@DataJpaTest`, Testcontainers e um `Clock` fixo:
    - `movesToTheNextMissionAndResetsTheTimer`;
    - `staysOnTheMissionAndCountsPullRequests` (o PR 3 duas vezes e o PR 4 dão `[3,4]`);
    - `partialReportOnlyRecordsActivity`;
    - `completedWhenEveryMissionPasses`;
    - `messagesThatLookLikeCodeAreDropped` (a mensagem `"expected: return x;"` vira `null`);
    - `longMessagesAreCutAt200`;
    - `ignoresRepositoriesOutsideAClassroom`;
    - `countsHintsPerMission`.
  - `WebhookRoutingTests`, com um `ReviewEvents` falso que anota as chamadas:
    - `recordsTestsOnTheDefaultBranchWithoutAPullRequest`;
    - `ignoresPushesOnOtherBranchesWithoutAPullRequest`;
    - `doesNotReadTheReportForUntrackedRepositories` (`verify(github, never()).workflowRuns(...)` quando `tracks` devolve `false` e não há PR);
    - `aHintCommandCountsAsActivity`.

    Acrescente `head_branch` e `repository.default_branch` ao helper `workflowRun(...)`.
  - `SocraticReviewerTests.tellsTheEventsWhatItAsked`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest='ProgressRecorderTests,WebhookRoutingTests,SocraticReviewerTests'`. Esperado: FALHA.
- [ ] **Passo 3: Implementar.** Ajuste os construtores de `WebhookDispatcher` e `SocraticReviewer` e os testes que os criam à mão.
- [ ] **Passo 4: Rodar** `./mvnw verify`. Esperado: PASSA.
- [ ] **Passo 5: Commit:** `Record classroom students' progress from the webhook`.

### Tarefa 7: Páginas do professor e do convite

Esta tarefa é feita **antes** da Tarefa 8. A seção "Painel" abaixo só descreve a tela; quem a implementa é a Tarefa 8.

**Arquivos:**
- Criar: `src/main/resources/static/professor.html`, `turma.html` e `classroom/ClassroomPages.java`
- Alterar: `src/main/resources/static/index.html`
- Teste: `classroom/ClassroomPagesTests.java`

**Interfaces:**
- Consome: as rotas das Tarefas 4 e 5, `/api/me`, `/oauth2/authorization/github` e o cookie `XSRF-TOKEN`, como em `index.html`.
- Produz: `ClassroomPages`, um `@Controller` que faz `forward:` de `/professor` para `/professor.html` e de `/turma/{joinCode}` para `/turma.html`. A Tarefa 8 acrescenta `/turmas/{id}`. As páginas leem o código ou o id de `location.pathname`.

**Volta depois do login.** Antes de mandar para `/oauth2/authorization/github`, a página grava `location.pathname` em `sessionStorage`, na chave `vagaviva.voltar`. Ao carregar `/?entrou=1`, o `index.html` lê e apaga essa chave e redireciona **só** se o valor casar com `^/(professor|turma/[a-z0-9]{10}|turmas/[0-9a-f-]{36})$`. Qualquer outro valor é ignorado, o que evita redirecionamento aberto. Envolva o acesso ao `sessionStorage` em `try/catch`.

Telas: siga o `DESIGN.md` da raiz (seções 4 e 5) e os protótipos em `docs/design/prototipos/`, que já trazem o HTML, o `vv.css` e o `vv.js` de partida. Em caso de diferença, vale o `DESIGN.md`. Resumo:
- **`professor.html`:**
  - entrar;
  - colar a vaga e ver o plano (`POST /api/project-plans`);
  - informar nome da turma, e-mail e consentimento;
  - criar e mostrar o link para copiar;
  - listar as turmas, com links para `/turmas/{id}`.
  - O 403 mostra a mensagem do servidor.
- **`turma.html`:**
  - nome da turma, professor, arquétipo e missões;
  - campo "Como você quer ser chamado(a)?" (`maxlength=40`);
  - a caixa com a frase de consentimento exata;
  - o botão "Entrar e criar meu repositório", com o 409 tratado como em `index.html`;
  - ao terminar, o link do repositório.
- **`painel.html`:**
  - contadores no topo;
  - uma coluna por missão (as vazias viram trilhos estreitos) e, abaixo, a faixa "Concluído";
  - cartões com a faixa na cor da situação (tokens do `DESIGN.md`, item 2.2), o nome, o selo com ícone e rótulo ("travado", "parado", "avançando" ou "concluído") e o `reason`;
  - clicar no cartão abre a ficha, com os três blocos e o link do PR;
  - em 375 px, as colunas viram uma lista vertical por missão.
- **Acessibilidade:** a cor nunca é o único sinal, e o cartão é um `<button>`.

- [ ] **Passo 1: Escrever o teste que falha.** `ClassroomPagesTests` confere que `GET /turma/abcdefghij` e `/professor` devolvem 200, que o HTML contém o `<title>` de cada página e que nenhuma delas exige login.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest=ClassroomPagesTests`. Esperado: FALHA.
- [ ] **Passo 3: Implementar** `ClassroomPages`, `professor.html`, `turma.html` e a volta no `index.html`.
- [ ] **Passo 4: Rodar.** Esperado: PASSA.
- [ ] **Passo 5: Verificar no navegador.** Rode a API local, crie uma turma e entre pelo convite com uma segunda conta. Confira em 1440 px e 375 px. Use o skill `run` ou o painel do navegador.
- [ ] **Passo 6: Commit:** `Add the teacher and invite pages`.

### Tarefa 8: Painel e ficha

Esta tarefa vem depois da Tarefa 7, das páginas do professor e do convite. Assim, os alunos podem entrar pelo link já em 19/11, e o progresso é gravado antes de o painel existir.

**Arquivos:**
- Criar: `classroom/DashboardService.java`, `classroom/DashboardController.java`, `src/main/resources/static/painel.html`
- Alterar: `classroom/ClassroomPages.java`, para acrescentar `/turmas/{id}`
- Teste: `classroom/DashboardApiTests.java`, `classroom/ClassroomPagesTests.java`

**Interfaces:**
- Consome: `StudentStatus.of` (Tarefa 3), `ClassroomProperties`, `Clock`, o catálogo de missões (para os títulos) e `ClassroomPages` (Tarefa 7).
- Produz:
  - `record Dashboard(UUID id, String name, String inviteUrl, Counts counts, List<Column> columns)`, com:
    - `record Counts(int stuck, int idle, int advancing, int completed)`;
    - `record Column(Integer order, String missionId, String title, List<Card> students)`;
    - `record Card(String login, String displayName, StudentStatus status, int daysOnMission, int missionOrder, String missionTitle, String reason)`.
  - `record StudentCard(String displayName, String login, String repositoryUrl, int missionOrder, String missionTitle, StudentStatus status, String reason, List<FailingTest> failingTests, String lastReviewerQuestion, Integer hintsOnMission, String lastPullRequestUrl)`, com `record FailingTest(String name, String message)`.
  - `DashboardService.dashboard(UUID classroomId, String teacherLogin) -> Optional<Dashboard>`. A Tarefa 9 reusa este método.
  - `DashboardService.student(UUID classroomId, String studentLogin, String teacherLogin) -> Optional<StudentCard>`.
  - `DashboardService.dashboardForSummary(Classroom) -> Dashboard`, sem a checagem de professor, para a Tarefa 9.
  - `hintsOnMission` fica `null` até a Tarefa 10.

Respostas:
- `GET /api/classrooms/{id}/dashboard` devolve:

  ```json
  {
    "name": "…", "inviteUrl": "…",
    "counts": { "stuck": 0, "idle": 0, "advancing": 0, "completed": 0 },
    "columns": [
      { "order": 1, "missionId": "…", "title": "…",
        "students": [ { "login": "…", "displayName": "…", "status": "STUCK", "daysOnMission": 4, "reason": "4 dias na missão 2, 3 PRs nesse período" } ] }
    ]
  }
  ```

  As colunas seguem a ordem do plano, mais uma coluna final `{ "missionId": null, "title": "Concluído" }`.
- Textos de `reason`, em português:
  - `STUCK`: `"N dias na missão K, P PRs nesse período"`, com "1 dia" e "1 PR" no singular;
  - `IDLE`: `"sem atividade há N dias"`;
  - `ADVANCING`: `"há N dias na missão"`, ou `"hoje"`;
  - `COMPLETED`: `"todas as missões passando"`.

  Os dias são inteiros, arredondados para baixo.
- `GET /api/classrooms/{id}/students/{login}` devolve `{displayName, login, repositoryUrl, missionOrder, missionTitle, status, reason, failingTests:[{name, message}], lastReviewerQuestion, hintsOnMission, lastPullRequestUrl}`.
- As duas rotas devolvem 404 se a turma não existir **ou** se não for do professor logado.

- [ ] **Passo 1: Escrever os testes que falham**, semeando o banco pelos repositórios da Tarefa 1 e com um `Clock` fixo, via `@TestConfiguration` com `@Primary`:
  - `groupsStudentsByMissionWithCounts`: 3 alunos (um travado na missão 2, um parado na 1 e um concluído) dão as colunas certas e `counts = {stuck:1, idle:1, advancing:0, completed:1}`.
  - `stuckReasonSaysDaysAndPullRequests`: `"4 dias na missão 2, 3 PRs nesse período"`.
  - `studentCardShowsWhatHoldsThemBack`.
  - `anotherTeacherGets404`.
  - `requiresSigningIn`.
  - Em `ClassroomPagesTests`, acrescente `GET /turmas/<uuid>`, que deve devolver 200 com o `<title>` do painel.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest='DashboardApiTests,ClassroomPagesTests'`. Esperado: FALHA.
- [ ] **Passo 3: Implementar** o serviço, o controller, o `painel.html` (o item `painel.html` em "Telas", na Tarefa 7) e o forward `/turmas/{id}` para `/painel.html`.
- [ ] **Passo 4: Rodar o mesmo comando.** Esperado: PASSA.
- [ ] **Passo 5: Verificar no navegador.** Com uma turma e um aluno de teste semeados, confira o painel e a ficha em 1440 px e 375 px.
- [ ] **Passo 6: Commit:** `Show the classroom dashboard and each student's card`.

### Tarefa 9: Resumo semanal

**Arquivos:**
- Criar: `classroom/WeeklySummary.java`, `classroom/ClassroomMailer.java`, `classroom/LoggingClassroomMailer.java`, `classroom/WeeklySummaryController.java`, `.github/workflows/classroom-summary.yml`
- Alterar: `login/SecurityConfig.java`, `docs/deploy.md`
- Teste: `classroom/WeeklySummaryTests.java`, `classroom/WeeklySummaryApiTests.java`

**Interfaces:**
- Consome: `DashboardService.dashboard` (Tarefa 7).
- Produz:
  - `interface ClassroomMailer { void send(String to, String subject, String text, String html); }`. A implementação padrão é `LoggingClassroomMailer`, que registra só `"Weekly summary for classroom {id} not sent: no mailer configured"`, sem destinatário nem conteúdo. A issue 28 troca o bean.
  - `WeeklySummary.render(Dashboard, LocalDate weekOf, String dashboardUrl) -> Rendered(String subject, String text, String html)`.

Texto, a partir do exemplo do desenho:
- Assunto: `"<nome da turma> — semana de dd/MM"`.
- A linha de contadores, uma linha por aluno travado ou parado (nome, situação, missão e `reason`) e `"Abrir o painel: <url>"`.
- Sem ninguém travado ou parado, a linha diz `"Ninguém travado ou parado nesta semana."`.
- O HTML é o mesmo conteúdo em `<p>` e `<ul>`, com os nomes escapados.

Rota: `POST /api/classrooms/weekly-summaries`.
- Exige o cabeçalho `X-Vaga-Viva-Secret` igual a `summary-secret`, comparado em tempo constante.
- Com o segredo vazio na configuração, devolve 404. Com um segredo errado, 401.
- Envia só para as turmas com `teacherEmail` e `teacherEmailConsentAt`. Devolve `{sent: n}`.
- Fica sem sessão e sem CSRF: inclua a rota no `ignoringRequestMatchers` e no `permitAll`.

Workflow: `cron: '0 11 * * 1'` e `workflow_dispatch`. O passo é um `curl -fsS -X POST "$API_URL/api/classrooms/weekly-summaries" -H "X-Vaga-Viva-Secret: $SECRET"`, com `API_URL: ${{ vars.API_URL }}` e `SECRET: ${{ secrets.CLASSROOM_SUMMARY_SECRET }}`. Ele tenta de novo uma vez depois de 60 s, porque a API pode estar dormindo.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `WeeklySummaryTests`:
    - `listsStuckAndIdleStudentsWithTheirMission`;
    - `saysWhenNobodyIsStuck`;
    - `escapesNamesInHtml` (o nome `"<b>Ana</b>"` vira `&lt;b&gt;`).
  - `WeeklySummaryApiTests`, com `@MockitoBean ClassroomMailer`:
    - `sendsOnlyToTeachersWhoConsented`;
    - `wrongSecretIs401`;
    - `blankSecretTurnsTheRouteOff`.
- [ ] **Passo 2: Rodar** `./mvnw test -Dtest='WeeklySummaryTests,WeeklySummaryApiTests'`. Esperado: FALHA.
- [ ] **Passo 3: Implementar.** Acrescente ao `docs/deploy.md` uma seção curta: criar `CLASSROOM_SUMMARY_SECRET` no Render e nos *secrets* do GitHub, e `CLASSROOM_TEACHERS` no Render.
- [ ] **Passo 4: Rodar** `./mvnw verify`. Esperado: PASSA.
- [ ] **Passo 5: Commit:** `Send teachers a weekly classroom summary`.

### Tarefa 10: Contagem de dicas (depois da issue 27)

Esta tarefa só começa quando a implementação real de `HintResponder` estiver no `main`. Até lá, a Tarefa 8 devolve `hintsOnMission: null`, e a ficha mostra "dicas ainda não disponíveis".

**Arquivos:**
- Alterar: a implementação de `HintResponder` da issue 27 e `DashboardService`
- Teste: o teste do responder da issue 27

- [ ] **Passo 1: Escrever o teste que falha.** Ao postar uma dica da missão `X`, o responder chama `events.hintGiven(repo, "X")`. Ele não chama quando responde "acabaram as dicas".
- [ ] **Passo 2: Implementar.** Injete `ReviewEvents` no responder. Em `DashboardService`, `hintsOnMission` passa a ser sempre um número.
- [ ] **Passo 3: Rodar** `./mvnw verify`. Esperado: PASSA.
- [ ] **Passo 4: Commit:** `Count the hints each classroom student asked for`.

---

## Ordem e prazos (do desenho)

| Prazo | Tarefas |
| --- | --- |
| 19/11 | 1 a 7: os alunos já entram pelo link e o progresso já é gravado |
| 22/11 | 8 |
| 24/11 | 9 |
| Quando a issue 27 entrar | 10 |
