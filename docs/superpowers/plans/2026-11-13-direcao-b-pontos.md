# Direção B e linguagem de pontos: plano de implementação

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para implementar este plano tarefa por tarefa. Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** trocar a interface do Vaga Viva para a direção B ("ferramenta de dev"), com uma linguagem de pontos, seis componentes de movimento e a grade de testes no cartão do aluno, primeiro na referência (`DESIGN.md` e protótipos), depois nas páginas reais e na landing.

**Arquitetura:**
- Back-end: `JunitReports` passa a contar os testes de cada missão; o progresso guarda essa contagem numa coluna `jsonb`; o painel devolve `tests` por missão.
- Front-end: um par novo de arquivos, `vv-motion.css` e `vv-motion.js`, com os seis componentes ativados por atributos `data-`. Ele vive nos protótipos e é copiado para `src/main/resources/static/`. A landing recebe uma cópia embutida do que usa.
- Ordem: back-end (Tarefas 1 a 3) em paralelo à referência (Tarefas 4 a 8); páginas reais (9 e 10) só depois da aprovação do David; landing (11 e 12); revisão final (13).

**Tecnologia:** Spring Boot 4.1, Java 21, Flyway, PostgreSQL 17, MockMvcTester e Testcontainers; HTML, CSS e JS puros sem build; Node 22 para os verificadores da skill design-taste-frontend.

**Desenho:** `docs/superpowers/specs/2026-11-13-direcao-b-pontos-design.md`. Leia junto com este plano. As decisões de aparência, a cor de cada ponto e onde cada componente aparece vêm de lá. Mockups de conversa (não são código para copiar): `.superpowers/brainstorm/1361-1794598134/content/`.

**Ramo:** `feat/direcao-b`, criado a partir de `feat/classroom-mode`.

## Restrições gerais

- Código, nomes, commits e PRs em inglês. Textos para professor e aluno em português, na voz do `DESIGN.md` seção 6.
- Sem framework, sem build, sem pacote de npm na interface. Nada de React, three.js ou código copiado do React Bits ou da transitions.dev.
- Raios: 4 px (chips, selos, pontos quadrados), 6 px (botões, campos), 8 px (cartões, painéis), 12 px (bloco principal). Pílula só no campo de cópia.
- Títulos: Bricolage Grotesque 600; `h1` no máximo 48 px, espaçamento -0.025em.
- Botão principal roxo (`--purple`, texto branco), um por tela. Sem sombra dura e sem `--ease-spring` em lugar nenhum.
- Amarelo só em marca-texto e varredura; verde e coral só em estado de teste e situação.
- Pontos redondos para testes e carregamentos; quadrados de 4 px de raio nas grades.
- Só `transform` e `opacity` são animados (mais `background-size` no marca-texto). No máximo um `<canvas>` por página.
- Laço infinito só durante espera real; nas demos da landing, as animações param fora da tela (`IntersectionObserver`).
- `prefers-reduced-motion: reduce`: todo componente mostra o estado final, parado, e a contagem mostra o número final.
- Contraste 4.5:1 para texto e 3:1 para bordas de controles, nos dois modos de cor. Foco visível em tudo.
- Os logs continuam citando só repositório e número do PR.
- Rodar o back-end: `./mvnw verify` (precisa do Docker).

## Pontos de atenção na revisão

1. **Relatório parcial:** um push que roda só algumas missões não pode zerar a contagem das outras. Esperado: só as missões do relatório são substituídas. Teste na Tarefa 2.
2. **Coluna nova vazia ou com JSON quebrado:** alunos de antes da migração ou uma linha corrompida. Esperado: o painel e a ficha abrem, a missão aparece com `total` nulo e a interface mostra "—". Teste na Tarefa 3.
3. **Missões futuras sempre falham:** a suíte inteira roda a cada push. Esperado: missões depois da atual aparecem vazias, nunca coral. Verificação na Tarefa 8 e na Tarefa 10.
4. **Movimento reduzido:** quem ativou a preferência não pode ficar preso esperando animação nem ver conteúdo escondido. Esperado: estado final imediato em todos os componentes. Verificação na Tarefa 5, na 6 e na 13.
5. **Mesma missão em vários arquivos XML do zip:** o Surefire pode dividir uma missão em mais de um arquivo. Esperado: as contagens se somam. Teste na Tarefa 1.

---

## Fase A: back-end da grade de testes

### Task 1: Contar os testes de cada missão no relatório

**Files:**
- Modify: `src/main/java/io/github/precavidos/vagaviva/review/TestReport.java`
- Modify: `src/main/java/io/github/precavidos/vagaviva/review/JunitReports.java`
- Test: `src/test/java/io/github/precavidos/vagaviva/review/JunitReportsTests.java`

**Interfaces:**
- Produces:
  - `public record MissionTally(int passed, int total)` em `io.github.precavidos.vagaviva.review`.
  - `TestReport(Set<String> missionsRun, List<FailedTest> failed, Map<String, MissionTally> tallies)`, com `tallies` copiado por `Map.copyOf` no construtor compacto.
  - Construtor de conveniência `TestReport(Set<String> missionsRun, List<FailedTest> failed)` que usa `Map.of()`, para não mexer nos testes que já existem.
  - `report.tallies()`: chave é o id da missão; só missões com pelo menos um teste não pulado.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void countsPassedAndTotalTestsPerMission() {
	String xml = """
			<testsuite name="x">
			  <testcase name="a" classname="Missão persistence: p"/>
			  <testcase name="b" classname="Missão persistence: p"><failure message="m"/></testcase>
			  <testcase name="c" classname="Missão persistence: p"><error message="e"/></testcase>
			  <testcase name="d" classname="Missão persistence: p"><skipped/></testcase>
			  <testcase name="e" classname="Outra coisa"/>
			</testsuite>
			""";
	TestReport report = JunitReports.read(zip(Map.of("TEST-a.xml", xml)));
	assertThat(report.tallies()).containsExactly(entry("persistence", new MissionTally(1, 3)));
}

@Test
void tallySumsTheSameMissionAcrossFiles() {
	String one = """
			<testsuite name="x"><testcase name="a" classname="Missão persistence: p"/></testsuite>
			""";
	String two = """
			<testsuite name="y"><testcase name="b" classname="Missão persistence: p"><failure message="m"/></testcase></testsuite>
			""";
	TestReport report = JunitReports.read(zip(Map.of("TEST-a.xml", one, "TEST-b.xml", two)));
	assertThat(report.tallies().get("persistence")).isEqualTo(new MissionTally(1, 2));
}
```

- [ ] **Step 2: Run them to verify they fail**

Run: `./mvnw test -Dtest=JunitReportsTests`
Expected: compilation FAIL ("cannot find symbol MissionTally" / "tallies()").

- [ ] **Step 3: Implement `MissionTally`, the new `TestReport` component and the counting in `JunitReports.reportInXml` and `read`**

`reportInXml` conta, por missão, cada `<testcase>` não pulado em `total` e os sem `failure`/`error` em `passed`. `read` soma as contagens de todos os arquivos (`Map.merge` com uma soma de `MissionTally`), na mesma ordem de `LinkedHashMap` usada para `missionsRun`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./mvnw test -Dtest='JunitReportsTests,MissionProgressTests,WebhookRoutingTests,ProgressRecorderTests'`
Expected: PASS (os testes antigos compilam pelo construtor de conveniência).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/precavidos/vagaviva/review src/test/java/io/github/precavidos/vagaviva/review/JunitReportsTests.java
git commit -m "Count passed and total tests per mission in the test report"
```

### Task 2: Guardar a contagem no progresso do aluno

**Files:**
- Create: `src/main/resources/db/migration/V3__test_tallies.sql`
- Modify: `src/main/java/io/github/precavidos/vagaviva/classroom/EnrollmentProgress.java`
- Modify: `src/main/java/io/github/precavidos/vagaviva/classroom/ProgressRecorder.java`
- Test: `src/test/java/io/github/precavidos/vagaviva/classroom/ProgressRecorderTests.java`

**Interfaces:**
- Consumes: `TestReport.tallies()` e `MissionTally` (Task 1).
- Produces:
  - Coluna `enrollment_progress.test_tallies jsonb` (nula para quem já existe).
  - `EnrollmentProgress.getTestTallies()` / `setTestTallies(String)`, mapeada como as outras colunas `jsonb` (`@JdbcTypeCode(SqlTypes.JSON)`).
  - JSON guardado: `{"<missionId>": {"passed": n, "total": n}}`.

- [ ] **Step 1: Write the failing tests**

```java
@Test
void recordsTheTallyOfEveryMissionThatRan() {
	recorder.testsRan(REPO, 3, new TestReport(BOTH, List.of(failed("persistence", "savesIt", "x")),
			Map.of("patients-api", new MissionTally(4, 4), "persistence", new MissionTally(1, 3))));

	assertThat(tallies(reload())).containsOnly(entry("patients-api", new MissionTally(4, 4)),
			entry("persistence", new MissionTally(1, 3)));
}

@Test
void aPartialReportReplacesOnlyTheMissionsItRan() {
	recorder.testsRan(REPO, 3, new TestReport(BOTH, List.of(failed("persistence", "savesIt", "x")),
			Map.of("patients-api", new MissionTally(4, 4), "persistence", new MissionTally(1, 3))));
	recorder.testsRan(REPO, 4, new TestReport(Set.of("persistence"), List.of(),
			Map.of("persistence", new MissionTally(3, 3))));

	assertThat(tallies(reload())).containsOnly(entry("patients-api", new MissionTally(4, 4)),
			entry("persistence", new MissionTally(3, 3)));
}
```

`tallies(EnrollmentProgress)` é um helper do teste que lê o JSON com `JsonMapper` para `Map<String, MissionTally>`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./mvnw test -Dtest=ProgressRecorderTests`
Expected: FAIL ("cannot find symbol getTestTallies").

- [ ] **Step 3: Write `V3__test_tallies.sql`** com `alter table enrollment_progress add column test_tallies jsonb;`

- [ ] **Step 4: Add the field to `EnrollmentProgress` and record the tally in `ProgressRecorder.testsRan`**

Antes do `switch`, em todos os casos (inclusive `Unknown`), lê o mapa guardado com o `read(...)` que já existe (JSON quebrado vira mapa vazio), faz `putAll(report.tallies())` e grava de volta.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./mvnw test -Dtest='ProgressRecorderTests,ProgressRecorderConcurrencyTests,ClassroomRepositoryTests'`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V3__test_tallies.sql src/main/java/io/github/precavidos/vagaviva/classroom src/test/java/io/github/precavidos/vagaviva/classroom/ProgressRecorderTests.java
git commit -m "Store each student's test tally per mission"
```

### Task 3: Devolver a grade no painel e na ficha

**Files:**
- Modify: `src/main/java/io/github/precavidos/vagaviva/classroom/DashboardService.java`
- Test: `src/test/java/io/github/precavidos/vagaviva/classroom/DashboardApiTests.java`

**Interfaces:**
- Consumes: `EnrollmentProgress.getTestTallies()` (Task 2).
- Produces (JSON da API, usado nas Tarefas 8 e 10):
  - `record MissionTests(int order, String missionId, Integer passed, Integer total)` em `DashboardService`.
  - `Card` ganha o último componente `List<MissionTests> tests`; `StudentCard` também, logo depois de `failingTests`.
  - `tests` tem uma entrada por missão do plano, na ordem do plano. `passed` e `total` são `null` quando não há contagem para a missão.
  - A API não decide cor: a interface pinta as missões depois de `missionOrder` como vazias (desenho, "Grade de testes").

- [ ] **Step 1: Write the failing tests**

```java
@Test
void cardsCarryTheTestTallyOfEveryMissionInPlanOrder() {
	Classroom classroom = newClassroom("David");
	List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
	Enrollment ana = enroll(classroom, "ana", "Ana Lima", ids.get(1), NOW.minus(Duration.ofDays(1)), NOW, "[]", "[]", false);
	setTallies(ana, "{\"" + ids.get(0) + "\":{\"passed\":4,\"total\":4},\"" + ids.get(1) + "\":{\"passed\":1,\"total\":3}}");

	var json = assertThat(get("/api/classrooms/" + classroom.getId() + "/dashboard", "david")).bodyJson();
	json.extractingPath("$.columns[1].students[0].tests.length()").isEqualTo(ids.size());
	json.extractingPath("$.columns[1].students[0].tests[0].missionId").isEqualTo(ids.get(0));
	json.extractingPath("$.columns[1].students[0].tests[0].passed").isEqualTo(4);
	json.extractingPath("$.columns[1].students[0].tests[1].total").isEqualTo(3);
	json.extractingPath("$.columns[1].students[0].tests[2].total").isNull();

	var card = assertThat(get("/api/classrooms/" + classroom.getId() + "/students/ana", "david")).bodyJson();
	card.extractingPath("$.tests[1].passed").isEqualTo(1);
}

@Test
void missingOrMalformedTalliesLeaveEveryMissionWithoutATotal() {
	Classroom classroom = newClassroom("David");
	List<String> ids = StoredPlan.fromJson(classroom.getPlan()).missionIds();
	Enrollment ana = enroll(classroom, "ana", "Ana Lima", ids.get(0), NOW, NOW, "[]", "[]", false);
	setTallies(ana, "[\"not a map\"]");

	var response = get("/api/classrooms/" + classroom.getId() + "/students/ana", "david");
	assertThat(response).hasStatusOk();
	assertThat(response).bodyJson().extractingPath("$.tests[0].total").isNull();
}
```

`setTallies` é um helper do teste que carrega o `EnrollmentProgress`, chama `setTestTallies` e faz `saveAndFlush`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./mvnw test -Dtest=DashboardApiTests`
Expected: FAIL (`$.columns[1].students[0].tests` não existe).

- [ ] **Step 3: Build `List<MissionTests>` from the plan's missions and the stored tallies**

Um método `tests(List<Mission> missions, EnrollmentProgress progress)`; JSON ausente, quebrado ou de outro formato lê como mapa vazio, como `readList` já faz.

- [ ] **Step 4: Run the whole back-end suite**

Run: `./mvnw verify`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/io/github/precavidos/vagaviva/classroom/DashboardService.java src/test/java/io/github/precavidos/vagaviva/classroom/DashboardApiTests.java
git commit -m "Return each student's test tally on the dashboard and the card"
```

---

## Fase B: referência (DESIGN.md e protótipos)

As tarefas desta fase não têm testes automatizados. A verificação é no navegador: `preview_start` com o nome `prototipos` (porta 4173, já em `.claude/launch.json`), nos dois modos de cor e em 1440 e 390 px.

### Task 4: Reescrever o DESIGN.md para a direção B

**Files:**
- Modify: `DESIGN.md`

**Interfaces:**
- Produces: os nomes de classes e atributos que as Tarefas 5 a 12 usam:
  - `data-dot-field`, `data-dot-matrix="scan|wave|flip"`, `.status-line` (matriz + `.status-label` + `.status-timer`), `data-decrypt`, `.step-mark` com estados `is-todo|is-current|is-done`, `data-dot-swap`, `data-count-up`;
  - `.test-grid` (linhas `.test-row` com `.test-mission`, `.test-dots` e `.test-count`; pontos `.td` com `.is-pass|.is-fail|.is-empty`).

- [ ] **Step 1: Rewrite sections 1, 2, 3, 4 and 9**

Sobe a tabela "Fundamentos da direção B" do desenho para a seção 2; a seção 3 troca "Estouro" (mola), "Sombra dura", "Virada do teste" e "Leitura" pelos componentes do desenho; a seção 4 descreve os componentes acima, a grade de testes e os botões novos; a seção 9 ganha os itens "selos e bolinhas dentro do título", "rabiscos e carimbos", "parede de logotipos de tecnologia" e "efeitos sem significado (cursor, aurora, vidro, 3D)". As seções 5 a 8 mudam só onde citam componentes que saíram.

- [ ] **Step 2: Verify no section still prescribes what the design removed**

Run: `grep -nE "ease-spring|sombra dura|ziguezague|999 px|Bricolage 800|36 px" DESIGN.md`
Expected: nenhuma linha, ou só linhas da seção 9 dizendo para evitar.

- [ ] **Step 3: Commit**

```bash
git add DESIGN.md
git commit -m "Rewrite the design system for the dev-tool direction and the dot language"
```

### Task 5: Componentes de espera e progresso (matriz, linha de status, marca de etapa, contagem)

**Files:**
- Create: `docs/design/prototipos/vv-motion.css`
- Create: `docs/design/prototipos/vv-motion.js`
- Create: `docs/design/prototipos/movimento.html`
- Modify: `docs/design/prototipos/index.html` (link para `movimento.html`)

**Interfaces:**
- Consumes: nomes da Task 4.
- Produces: `window.VVMotion` com:
  - `init(root?: Element): void`, que liga todo `data-` dentro de `root` (padrão `document`) e é seguro de chamar de novo;
  - `statusLine(el: Element, label: string): { done(): void, stop(): void }`, que inicia o cronômetro "0,0 s" com vírgula decimal;
  - `countUp(el: Element, to: number): void`, 600 ms, `ease-out` cúbico.
- `movimento.html` mostra cada componente com rótulo, e é a página que o David usa para aprovar.

- [ ] **Step 1: Write `movimento.html`** com uma seção por componente, e chame `VVMotion.init()`.
- [ ] **Step 2: Implement the 4×4 matrix in CSS** (`[data-dot-matrix]` gera 16 `<i>` via JS; `scan` por linha em amarelo, `wave` diagonal em roxo, `flip` coral → verde um por vez; ciclo de 1,6 s).
- [ ] **Step 3: Implement `.status-line`, `.step-mark` (SVG anel/arco/visto) and `countUp`.**
- [ ] **Step 4: Verify in the browser**

Abra `http://localhost:4173/movimento.html`. Confira: os três padrões da matriz animam; a linha de status conta o tempo; as marcas de etapa passam por anel, arco e visto; a contagem sobe uma vez. Com `prefers-reduced-motion` emulado (`javascript_tool`: `matchMedia('(prefers-reduced-motion: reduce)').matches` precisa refletir a emulação), tudo aparece parado no estado final. `read_console_messages` sem erros.

- [ ] **Step 5: Commit**

```bash
git add docs/design/prototipos/vv-motion.css docs/design/prototipos/vv-motion.js docs/design/prototipos/movimento.html docs/design/prototipos/index.html
git commit -m "Add the dot matrix, status line, step mark and count-up prototypes"
```

### Task 6: Componentes de destaque (fundo de pontos, termo vira competência, troca em pontos)

**Files:**
- Modify: `docs/design/prototipos/vv-motion.css`, `docs/design/prototipos/vv-motion.js`, `docs/design/prototipos/movimento.html`

**Interfaces:**
- Consumes: `VVMotion.init` (Task 5).
- Produces:
  - `VVMotion.decrypt(el: Element, text: string): Promise<void>`: resolve da esquerda para a direita em até 600 ms; o `aria-label` do elemento recebe o texto final antes de começar.
  - `VVMotion.dotSwap(el: Element, swap: () => void): Promise<void>`: cobre com malha de pontos roxos, chama `swap`, descobre; cerca de 500 ms no total.
  - `[data-dot-field]`: um `<canvas>` atrás do conteúdo do elemento; pontos ficam verdes e se afastam perto do cursor; parado com toque (`pointer: coarse`), movimento reduzido ou fora da tela.

- [ ] **Step 1: Implement the three components and add them to `movimento.html`.**
- [ ] **Step 2: Verify in the browser** em `movimento.html`: o fundo reage ao mouse e pausa ao rolar para fora (conferir com `javascript_tool` que o laço de `requestAnimationFrame` para); o texto decifra para o nome certo e o leitor de tela recebe o texto final (`read_page` mostra o `aria-label`); a troca em pontos troca o conteúdo uma vez por clique. Movimento reduzido: estado final direto.
- [ ] **Step 3: Commit**

```bash
git add docs/design/prototipos/vv-motion.css docs/design/prototipos/vv-motion.js docs/design/prototipos/movimento.html
git commit -m "Add the dot field, decrypt and dot swap prototypes"
```

### Task 7: Tokens e telas do aluno, do convite e do professor na direção B

**Files:**
- Modify: `docs/design/prototipos/vv.css`, `docs/design/prototipos/vv.js`
- Modify: `docs/design/prototipos/aluno.html`, `docs/design/prototipos/turma.html`, `docs/design/prototipos/professor.html`, `docs/design/prototipos/email.html` (só tokens de cor e raio)

**Interfaces:**
- Consumes: `vv-motion.*` (Tasks 5 e 6).
- Produces: `vv.css` com os tokens e componentes da Task 4; `vv.js` com `ic()` gerando o quadrado com contorno e `avatar()` neutro.

- [ ] **Step 1: Update `vv.css` and `vv.js`** com raios, títulos, botões, fichas de ícone, avatar e painéis escuros do desenho; remover sombra dura, mola e ziguezague.
- [ ] **Step 2: Apply the components to the screens**
  - `aluno.html`: botão "Montar meu plano" carregando vira linha de status `scan`; a extração usa `decrypt` nas competências; a geração do plano usa `wave`; a passagem do plano para o repositório usa `dotSwap`; as etapas da criação usam `.step-mark`, com a última verde só quando a resposta chega.
  - `turma.html`: mesmo passo 3 do aluno.
  - `professor.html`: carregamento do plano com linha de status `wave`.
- [ ] **Step 3: Verify in the browser**, cada tela em 1440 e 390 px e nos dois modos: nenhum `border-radius` acima de 12 px fora do campo de cópia (`javascript_tool` percorrendo os elementos visíveis), um só botão roxo por tela, nenhuma rolagem horizontal da página, console sem erros.
- [ ] **Step 4: Commit**

```bash
git add docs/design/prototipos
git commit -m "Move the student, invite and teacher prototypes to the dev-tool direction"
```

### Task 8: Painel com a grade de testes

**Files:**
- Modify: `docs/design/prototipos/painel.html` (dados fictícios no formato da Task 3: `tests` com `order`, `missionId`, `passed`, `total`)
- Modify: `docs/design/prototipos/vv.css` (`.test-grid`)
- Modify: `docs/design/prototipos/vv.js`

**Interfaces:**
- Consumes: formato `tests` da Task 3; `VVMotion.countUp` (Task 5).
- Produces: `VV.testGrid(tests: MissionTests[], missionOrder: number, completed: boolean): string` (HTML), usada no cartão, na ficha e na demo da landing.

Regra de cor, do desenho: missão com `order <= missionOrder` → `passed` pontos `.is-pass` e `total - passed` pontos `.is-fail`, contagem "passed/total"; missão depois da atual ou sem `total` → `.is-empty` e "—" (sem `total`, sem pontos); aluno concluído → tudo `.is-pass`.

- [ ] **Step 1: Implement `testGrid` and place it in the student card and at the top of the drawer.**
- [ ] **Step 2: Add the mock cases:** um aluno travado na missão 3, um concluído, um parado sem contagem nenhuma, e um cuja missão futura tem testes falhando no JSON.
- [ ] **Step 3: Verify in the browser:** o aluno com missão futura falhando mostra essa linha vazia e "—", nunca coral; o aluno sem contagem mostra só "—"; os cartões ficam na ordem de hoje, sem ordenação por pontos; a contagem do resumo sobe uma vez.
- [ ] **Step 4: Commit**

```bash
git add docs/design/prototipos
git commit -m "Show the test grid on the dashboard prototype"
```

**Ponto de parada:** o David abre os protótipos (`index.html` e `movimento.html`) e aprova antes da Fase C. Ajustes pedidos voltam para as Tarefas 4 a 8.

---

## Fase C: páginas reais

Verificação: suba a aplicação (`docker compose up -d` e `./mvnw spring-boot:run`) e abra `http://localhost:8080` no navegador. Para o painel, use uma turma do ambiente local.

### Task 9: Levar tokens, componentes e telas do aluno, convite e professor para o Spring

**Files:**
- Create: `src/main/resources/static/vv-motion.css`, `src/main/resources/static/vv-motion.js` (cópias dos protótipos)
- Modify: `src/main/resources/static/vv.css`, `src/main/resources/static/vv.js`
- Modify: `src/main/resources/static/index.html`, `src/main/resources/static/turma.html`, `src/main/resources/static/professor.html`
- Test: `src/test/java/io/github/precavidos/vagaviva/classroom/ClassroomPagesTests.java`

**Interfaces:**
- Consumes: protótipos aprovados (Tasks 5 a 7).

- [ ] **Step 1: Write the failing test** `staticPagesLoadTheMotionLayer`: `GET /`, `/professor` e `/turma/<código>` devolvem HTML com `vv-motion.js`; `GET /vv-motion.js` responde 200.
- [ ] **Step 2: Run it to verify it fails:** `./mvnw test -Dtest=ClassroomPagesTests` → FAIL.
- [ ] **Step 3: Port the files and the three screens**, mantendo as chamadas reais de API que as páginas já fazem. A linha de status mede a espera real da requisição.
- [ ] **Step 4: Run the tests:** `./mvnw test -Dtest=ClassroomPagesTests` → PASS.
- [ ] **Step 5: Verify in the browser** o fluxo real do aluno com a vaga de exemplo, em 1440 e 390 px e nos dois modos; console sem erros.
- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static src/test/java/io/github/precavidos/vagaviva/classroom/ClassroomPagesTests.java
git commit -m "Move the student, invite and teacher pages to the dev-tool direction"
```

### Task 10: Painel real com a grade de testes

**Files:**
- Modify: `src/main/resources/static/painel.html`

**Interfaces:**
- Consumes: `tests` da API (Task 3), `VV.testGrid` (Task 8).

- [ ] **Step 1: Render `testGrid` in each card and at the top of the drawer, and restyle the dashboard to match the approved prototype.**
- [ ] **Step 2: Verify in the browser** com uma turma local em que um aluno tem `test_tallies` nulo e outro tem contagem: o primeiro mostra "—" em todas as missões, o segundo mostra pontos só até a missão atual. Os contadores sobem uma vez; console sem erros.
- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/painel.html
git commit -m "Show each student's test grid on the dashboard"
```

---

## Fase D: landing

Verificação: `preview_start` com o nome `landing` (porta 4174).

### Task 11: Novo topo da landing

**Files:**
- Modify: `site/index.html`

**Interfaces:**
- Consumes: cópia embutida de `vv-motion.css/js` (só fundo de pontos, `decrypt`, matriz) com o comentário `/* copied from docs/design/prototipos/vv-motion.* */`.

- [ ] **Step 1: Replace the hero:** título só em texto (sem selos, bolinhas, balão, rabiscos ou carimbos), um parágrafo, botão roxo e um secundário, `data-dot-field` atrás, e à direita a demo da vaga virando plano: recorte com marcas, competências com `decrypt`, primeiras missões. A demo roda uma vez ao entrar na tela.
- [ ] **Step 2: Verify in the browser:** o topo cabe na primeira tela em 1440×900 e em 390 px; o fundo pausa fora da tela; com movimento reduzido a demo abre no estado final; um só `<canvas>` na página (`document.querySelectorAll('canvas').length === 1`).
- [ ] **Step 3: Commit**

```bash
git add site/index.html
git commit -m "Rebuild the landing hero around the dot field and the posting demo"
```

### Task 12: Resto da landing

**Files:**
- Modify: `site/index.html`

- [ ] **Step 1: Apply direction B to the other sections:** demos do plano e do repositório com matriz `flip` e `.step-mark`; demo do painel com a grade de testes e dados fictícios; tirar rótulos com bolinha (no máximo um, onde traz informação), listas de três itens com ícone e a parede "Feito com" (fica uma linha de texto com a stack); texto revisto na voz do `DESIGN.md`.
- [ ] **Step 2: Run the copy check of the skill**

Run: `node .agents/skills/design-taste-frontend/scripts/copy-lint.mjs site/index.html --kind software`
Expected: nenhum `FAIL`. Os `WARN` vão para o relatório da Task 13 com a decisão tomada.

- [ ] **Step 3: Commit**

```bash
git add site/index.html
git commit -m "Move the rest of the landing to the dev-tool direction"
```

---

## Fase E: revisão final

### Task 13: Revisão de layout, movimento e acessibilidade

**Files:**
- Create: `docs/design/revisao-direcao-b.md` (achados e decisões)
- Modify: o que os achados pedirem, com um commit por correção

- [ ] **Step 1: Run the layout check of the skill on the landing**

Run: `node .agents/skills/design-taste-frontend/assets/layout/check.mjs --url site/index.html --profile .taste-skill/chrome-layout-check --sizes 1440x900,1024x768,768x1024,390x844 --no-stress`
Antes, adicione `.taste-skill/` ao `.gitignore` (é onde o verificador guarda o perfil do Chrome).
Expected: os achados listados. Cada um vira correção ou decisão registrada. As regras da skill que o desenho recusa (sem roxo, uma seção por tela, fotos) entram no relatório como "não se aplica".

- [ ] **Step 2: Check every page by hand** (protótipos, páginas reais e landing): 1440, 1024, 768 e 390 px; claro e escuro; movimento reduzido; teclado até o botão principal; sem rolagem horizontal da página; console sem erros.
- [ ] **Step 3: Write `docs/design/revisao-direcao-b.md`** com o que foi visto, o que foi corrigido e o que ficou de fora e por quê.
- [ ] **Step 4: Run the whole suite:** `./mvnw verify` → BUILD SUCCESS.
- [ ] **Step 5: Commit**

```bash
git add docs/design/revisao-direcao-b.md
git commit -m "Record the final review of the dev-tool direction"
```
