# Briefing para o Devin: repositório-modelo Python/FastAPI

Este texto é o pedido de trabalho para o Devin. Ele é autossuficiente, mas segue o mesmo desenho dos arquétipos Java/Spring e TypeScript/Node, que já estão prontos e validados. Quando um ponto aqui não for explícito, faça como no Java: leia `precavidos/vaga-viva-template-java-spring` (público) e `precavidos/vaga-viva-template-java-spring-solution` (privado), e mantenha os mesmos contratos. O modelo TypeScript (`precavidos/vaga-viva-template-typescript-node`) mostra como as mesmas regras foram levadas para uma stack sem anotações de teste.

## Objetivo

Criar o **repositório-modelo** do arquétipo `python-fastapi` e a sua **solução de referência**. O domínio, os ids das missões e os contratos HTTP são **os mesmos do Java/Spring**: muda só a stack. O Vaga Viva cria o repositório de cada aluno como cópia do modelo e escolhe de 5 a 6 missões do catálogo. Em cada missão, os testes falham até o aluno implementá-la.

## Entregáveis

| Repositório | Visibilidade | Conteúdo |
| --- | --- | --- |
| `precavidos/vaga-viva-template-python-fastapi` | **público**, com *Template repository* marcado | código inicial, testes de **todas** as 11 missões, filtro de missões pelo plano, `missoes/<id>.md`, workflow mínimo |
| `precavidos/vaga-viva-template-python-fastapi-solution` | **privado** | o modelo mais a implementação de todas as missões, **um commit por missão**, e `scripts/validate.sh` |

O aluno nunca pode ver a solução. Nada do repositório público pode conter, em código, comentário, histórico ou branch, a implementação de uma missão.

## Stack e convenções

- **Python 3.13**. Use as versões estáveis atuais de todas as dependências, conferidas no PyPI, e fixe-as no `uv.lock`.
- **FastAPI** como framework, **Pydantic v2** para os modelos da API e **pydantic-settings** para a configuração.
- **SQLAlchemy 2.0** (estilo tipado, com `Mapped` e `mapped_column`, sessão síncrona) com o driver **psycopg 3**, e **Alembic** para as migrações. PostgreSQL 17.
- Gerenciador de pacotes: **uv**, com `pyproject.toml` e `uv.lock` versionados. `uv sync` seguido de `uv run pytest` precisa rodar só com uv e Docker: o uv instala o Python 3.13 se faltar.
- Testes com **pytest**, o `TestClient` do FastAPI (httpx) para HTTP e **testcontainers** (`testcontainers[postgres]`) para o banco.
- Código e commits em **inglês**.
- Tudo o que o aluno lê fica em **português do Brasil**: README, `missoes/<id>.md`, nomes das funções de teste (por exemplo, `test_cadastra_paciente_e_responde_201`), mensagens das asserções e mensagens de erro da API.
- Só dados fictícios: nada de nomes, CPFs, telefones ou e-mails reais nas fixtures (use domínios como `example.com`).

## Domínio fictício: agendamento de clínica

O mesmo do Java. Entidades: **paciente** (`Patient`), **profissional** (`Professional`) e **consulta** (`Appointment`, com início, duração e profissional). A clínica funciona de segunda a sexta, das 08:00 às 18:00, no fuso `America/Sao_Paulo`, definido em configuração.

## Catálogo de missões

Os mesmos 11 ids, títulos e dificuldades do Java. Onde o Java cita uma ferramenta própria, use a equivalente da stack:

| Id | Título | O que o aluno faz nesta stack | Dificuldade | Central |
| --- | --- | --- | --- | --- |
| `patients-api` | Cadastro de pacientes | API REST de pacientes com um `APIRouter` do FastAPI e modelos Pydantic, guardando em memória | 1 | sim |
| `persistence` | Pacientes no banco de dados | modelo `Patient` no SQLAlchemy e uma migração do Alembic que cria a tabela `patients` | 2 | sim |
| `scheduling-rules` | Regras de agendamento | agendar consultas respeitando o horário de funcionamento, sem sobreposição e sem datas no passado | 3 | sim |
| `input-validation` | Validação e mensagens de erro | validar a entrada com Pydantic e trocar a resposta padrão do FastAPI (422) por `400` no formato Problem Details, com um handler de `RequestValidationError` | 2 | não |
| `bug-hunt` | Caça ao bug | encontrar e corrigir um defeito escondido no cálculo da duração das consultas | 2 | não |
| `pagination` | Listagem paginada de consultas | listar as consultas de um profissional com paginação e ordenação por data | 2 | não |
| `ci-pipeline` | Pipeline de integração contínua | rodar instalação e testes a cada push com GitHub Actions | 2 | não |
| `availability-query` | Consulta de horários livres | calcular os horários livres com uma consulta SQL (`session.execute(text(...))`) | 3 | não |
| `containerization` | Aplicação em contêiner | empacotar a aplicação numa imagem Docker e subir tudo com Docker Compose | 3 | não |
| `reminder-strategies` | Lembretes por e-mail e SMS | enviar lembretes por canais diferentes, aplicando um padrão de projeto com `typing.Protocol` e as dependências (`Depends`) do FastAPI | 4 | não |
| `api-security` | Proteção da API | exigir autenticação e separar o que recepcionistas e profissionais podem fazer, com `HTTPBasic` e dependências do FastAPI | 4 | não |

## Contratos

Copie os contratos de `missoes/<id>.md` do modelo Java: endpoints, campos JSON, status HTTP, formato de erro, formato da paginação (`content` e `page.{number,size,totalElements,totalPages}`), usuários e papéis. Um aluno precisa conseguir comparar as três versões e ver o mesmo sistema. Os campos JSON seguem os nomes do Java, em camelCase (use `alias_generator` do Pydantic, se quiser `snake_case` no código). As únicas diferenças permitidas:

| Assunto | Java | Python |
| --- | --- | --- |
| Saúde da aplicação | `/actuator/health` | `GET /health` responde `200` com `{"status":"ok"}`, como no TypeScript |
| Usuários de teste | `clinic.security.users` em `application-test.yaml` | os mesmos usuários e senhas, num arquivo de configuração de teste que você define e documenta em `missoes/api-security.md` |
| Configuração da clínica | `application.yaml` | variáveis de ambiente com valores padrão, lidas pelo `pydantic-settings` |
| Datas | `java.time` | `datetime` com `zoneinfo` da biblioteca padrão; nenhuma biblioteca de datas a mais |

## Regras que os testes precisam seguir

### 1. Missões e plano

pytest não tem as tags do JUnit. Use isto no lugar:

- Os testes de cada missão ficam em `tests/missions/test_<id com _ no lugar de ->.py` e levam a marca `@pytest.mark.mission("<id>")` (por exemplo, no `pytestmark` do módulo). Registre a marca no `pyproject.toml`.
- O `conftest.py` da raiz lê `.vaga-viva/plan.json` na raiz do projeto, num hook de coleta (`pytest_collection_modifyitems`). Se a missão não estiver em `missions`, ele marca os testes com `skip` e põe o motivo: `Missão '<id>' fora do seu plano (.vaga-viva/plan.json)`. Sem o arquivo, todas as missões rodam. Um `plan.json` malformado faz a execução falhar com uma mensagem clara em português.
- Testes fora de `tests/missions/` (por exemplo, "a aplicação sobe") rodam sempre e **passam no modelo**.
- `uv run pytest --mission <id>` roda só uma missão: é uma opção de linha de comando que o `conftest.py` registra. Documente esse comando no README e em cada `missoes/<id>.md`. O Vaga Viva cita exatamente esse comando nas issues.
- O modelo **não** traz `.vaga-viva/plan.json`: quem grava é o Vaga Viva, ao criar o repositório do aluno. O formato é o mesmo do Java, com `"archetype": "python-fastapi"`.

### 2. Falhar pelo motivo certo

No modelo, cada teste de missão:

- **é coletado sem erro**: `uv run pytest --collect-only -q` termina sem erro de importação no modelo inteiro;
- **sobe a aplicação** de teste (o `TestClient` sobre o app do FastAPI) sem erro;
- **falha numa asserção** com mensagem em português que diz o que falta, na primeira linha: `assert resposta.status_code == 201, "Esperava 201 ao cadastrar o paciente, mas veio 404"`. Nunca falha por `ImportError`, `AttributeError`, fixture ausente, aplicação que não sobe ou timeout.

Prefira testes de caixa-preta: HTTP com o `TestClient` e consultas SQL. Quando a missão exige uma classe ou função de domínio, o código inicial traz a assinatura, com corpo `raise NotImplementedError("Missão <id>")`. Esse `NotImplementedError` vindo do código da missão é a única exceção aceita no lugar de uma asserção, porque a mensagem já diz o que falta.

### 3. O relatório JUnit XML que o Vaga Viva lê

O revisor de PR do Vaga Viva lê o mesmo JUnit XML nos três arquétipos e liga cada teste à missão pelo atributo `classname`:

- Cada `<testcase>` de missão precisa ter `classname="Missão <id>: <título>"`, por exemplo `Missão patients-api: Cadastro de pacientes`. O padrão do pytest (`tests.missions.test_patients_api`) **não serve**. Uma forma é uma fixture `autouse` no `conftest.py` que chama `record_xml_attribute("classname", ...)` nos testes marcados com `mission`; confira o XML gerado.
- O relatório sai em `reports/junit.xml` (`--junitxml` no `addopts` do `pyproject.toml`).
- Os testes fora do plano aparecem como `<skipped>`, não como falha.

### 4. Independência entre missões

As mesmas regras do Java:

- Os testes de uma missão só dependem do **código inicial** e das **missões centrais de dificuldade menor**.
- Profissionais e consultas entram por **fixtures SQL**, sobre tabelas que o código inicial já cria. A tabela de consultas guarda `patient_id` **sem chave estrangeira**.
- Todos os testes HTTP mandam Basic Auth da recepcionista por um helper comum, para continuarem passando depois de `api-security`.
- Nenhum teste fora de `input-validation` manda dados inválidos ou verifica o formato do corpo de erro.
- O defeito de `bug-hunt` continua no código em muitos planos: os testes das outras missões não podem usar o caso em que ele aparece.

### 5. O que o código inicial já traz

- Aplicação FastAPI que sobe, com `GET /health`.
- SQLAlchemy e Alembic configurados, com uma migração inicial para `professionals` e `appointments` (`starts_at`, `duration_minutes`, `patient_id` sem FK).
- Infraestrutura de teste no `conftest.py`:
  - uma fixture de sessão que sobe o PostgreSQL com testcontainers e aplica as migrações (`alembic upgrade head`);
  - limpeza das tabelas antes de cada teste, incluindo `patients` quando ela existir.
- A configuração da clínica (horário e fuso) e um relógio injetável (uma dependência do FastAPI), para os testes usarem datas relativas ao relógio real, com 7 ou mais dias à frente.
- `EmailSender`, `SmsSender` e `ReminderStrategy` como `Protocol`s, mais implementações que só escrevem no log. Os testes trocam os envios por fakes que gravam o que foi enviado, com `app.dependency_overrides`.
- O cálculo de duração **com o mesmo defeito do Java**: só a hora do dia é considerada, então dá errado em consultas que atravessam a meia-noite. Não deixe comentário que o denuncie.
- `missoes/<id>.md` para as 11 missões e um README genérico em português, sem dicas de solução. O Vaga Viva substitui o README depois.
- Licença MIT, com copyright de `precavidos`.

### 6. Workflow mínimo e a missão `ci-pipeline`

- O modelo traz `.github/workflows/vaga-viva.yml`, que é do Vaga Viva e não faz parte de nenhuma missão. Ele:
  - se chama exatamente **`Vaga Viva`** (`name: Vaga Viva`): o Vaga Viva reage ao fim desse workflow pelo nome;
  - dispara em `push` **só na `main`** e em `pull_request`;
  - usa `astral-sh/setup-uv` com cache, Python 3.13, e roda `uv sync --locked` e `uv run pytest`;
  - publica `reports/junit.xml` como o artefato **`junit-report`**, com `if: always()`. É o mesmo nome do TypeScript, e o revisor só lê artefatos com esse nome ou `surefire-reports`.
- Na missão `ci-pipeline`, o aluno cria o **próprio** `.github/workflows/ci.yml`. Os testes leem o YAML e verificam:
  - que ele dispara em `push` e `pull_request`;
  - que usa `astral-sh/setup-uv` e Python 3.13;
  - que roda `uv sync` e `uv run pytest`.

### 7. Missões com verificação fora do Python

- `containerization`: o teste confere que o `Dockerfile` existe. Depois constrói a imagem com testcontainers, sobe a aplicação junto com o PostgreSQL numa rede do testcontainers e espera `200` em `/health`, com timeout de 10 minutos. Também lê o `compose.yaml` e procura um serviço com `build` e outro com uma imagem `postgres`.
- `availability-query`: o teste confere o resultado, não o texto do SQL.

## Lições dos modelos Java e TypeScript

Erros que já aconteceram e não podem se repetir:

- **Caminhos no Windows.** Mensagens que citam arquivos usam o texto fixo com `/`, como `.vaga-viva/plan.json`, nunca `str(Path(...))`. Muitos alunos usam Windows. Abra arquivos de texto sempre com `encoding="utf-8"`: o padrão do Windows não é UTF-8.
- **CI duplicado.** `push` sem filtro de branch mais `pull_request` roda tudo duas vezes. Use `push` só na `main`.
- **Limite de requisições do registro de pacotes.** Se o PyPI devolver 429, espere e reaproveite o cache, em vez de insistir. O `Dockerfile` do modelo e o da solução baixam só do PyPI público.
- **Relatório que o revisor não reconhece.** No TypeScript, o `classname` do relatório precisou ser ajustado para `Missão <id>: <título>`. Aqui isso já é regra (seção 3): valide o XML antes de abrir o PR.

## Solução de referência

- **Um commit por missão**, na ordem do catálogo, com a mensagem `Solve <id>`, por cima do `main` do modelo já mergeado. Mantenha cada missão nos próprios arquivos, para o cherry-pick isolado não dar conflito. As migrações do Alembic de cada missão precisam encadear sem conflito também nas combinações do critério 3: use `down_revision` apontando para a migração inicial do modelo ou para a de `persistence`, nunca para a de outra missão opcional.
- Com a solução completa, `uv run pytest` passa **sem nenhum teste ignorado**, já que não há `plan.json`.
- O merge do PR da solução vai ser **merge commit**, sem squash, para os commits por missão não se perderem.

## Critérios de aceite (o Vaga Viva vai conferir, também no Windows)

1. No modelo: `uv run pytest --collect-only -q` termina sem erro, os testes fora de `tests/missions/` passam, e cada missão tem pelo menos 3 testes. Todos falham por asserção, com mensagem em português na primeira linha.
2. Na solução completa: `uv run pytest` passa, com zero falhas e zero testes ignorados.
3. Independência: para cada missão opcional M, o modelo mais os commits das 3 centrais e de M faz passar os testes de M.
4. Com um `plan.json` de 6 missões, os testes das outras 5 aparecem como ignorados, com o motivo, e não como falhas.
5. Em `reports/junit.xml`, todo `<testcase>` de missão tem `classname` no formato `Missão <id>: <título>`.
6. Nenhum arquivo do repositório público contém a solução de uma missão, e o histórico dele não tem os commits da solução.
7. Um script `scripts/validate.sh` na solução executa os critérios 1 a 5 e imprime uma tabela missão × resultado, como o do Java. Ele precisa rodar no Git Bash do Windows.

## Etapas

Trabalhe em duas etapas, como no Java e no TypeScript:

1. **Modelo**, no repositório público: abra um PR e **pare** para revisão.
2. **Solução**, no repositório privado: só depois de o PR do modelo ser aprovado e mergeado.

Antes de escrever código, responda com seu plano e suas dúvidas.

## Fora do escopo

- Textos das issues e README personalizado (o Vaga Viva gera).
- Front-end.
- O catálogo de missões e o vocabulário do arquétipo dentro do repositório do Vaga Viva (`python-fastapi.json`): o time faz.

## Fontes

- Modelo e solução do Java: `precavidos/vaga-viva-template-java-spring` e `precavidos/vaga-viva-template-java-spring-solution`
- Modelo do TypeScript: `precavidos/vaga-viva-template-typescript-node`
- Briefings anteriores: `docs/devin/repositorio-modelo-java-spring.md` e `docs/devin/repositorio-modelo-typescript-node.md` no `precavidos/vaga-viva`
- Catálogo de missões: `src/main/resources/missions/java-spring.json`
- Contrato do plano: `docs/adr/0005-plano-no-repositorio-do-aluno.md`
- Escolha do arquétipo: `docs/adr/0006-escolha-do-arquetipo-e-compatibilidade.md`
