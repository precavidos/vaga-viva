# Briefing para o Devin: repositório-modelo TypeScript/Node

Este texto é o pedido de trabalho para o Devin. Ele é autossuficiente, mas segue o mesmo desenho do arquétipo Java/Spring, que já está pronto e validado. Quando um ponto aqui não for explícito, faça como no Java: leia `precavidos/vaga-viva-template-java-spring` (público) e `precavidos/vaga-viva-template-java-spring-solution` (privado), e mantenha os mesmos contratos.

## Objetivo

Criar o **repositório-modelo** do arquétipo `typescript-node` e a sua **solução de referência**. O domínio, os ids das missões e os contratos HTTP são **os mesmos do Java/Spring**: muda só a stack. O Vaga Viva cria o repositório de cada aluno como cópia do modelo e escolhe de 5 a 6 missões do catálogo. Em cada missão, os testes falham até o aluno implementá-la.

## Entregáveis

| Repositório | Visibilidade | Conteúdo |
| --- | --- | --- |
| `precavidos/vaga-viva-template-typescript-node` | **público**, com *Template repository* marcado | código inicial, testes de **todas** as 11 missões, filtro de missões pelo plano, `missoes/<id>.md`, workflow mínimo |
| `precavidos/vaga-viva-template-typescript-node-solution` | **privado** | o modelo mais a implementação de todas as missões, **um commit por missão**, e `scripts/validate.sh` |

O aluno nunca pode ver a solução. Nada do repositório público pode conter, em código, comentário, histórico ou branch, a implementação de uma missão.

## Stack e convenções

- Node.js 22 LTS e TypeScript em modo `strict`. Use as versões estáveis atuais de NestJS, Prisma e das demais dependências, conferidas no npm, e fixe-as no `package-lock.json`.
- **NestJS** como framework e **Prisma** para o banco, com PostgreSQL 17.
- Gerenciador de pacotes: **npm**. `npm ci` seguido de `npm test` precisa rodar só com Node 22 e Docker.
- Testes com **Jest** (o padrão do NestJS), **Supertest** para HTTP e **Testcontainers** (`@testcontainers/postgresql`) para o banco.
- Código, nomes e commits em **inglês**.
- Tudo o que o aluno lê fica em **português do Brasil**: README, `missoes/<id>.md`, nomes dos `describe`/`it`, mensagens das asserções e mensagens de erro da API.
- Só dados fictícios: nada de nomes, CPFs, telefones ou e-mails reais nas fixtures (use domínios como `example.com`).

## Domínio fictício: agendamento de clínica

O mesmo do Java. Entidades: **paciente** (`Patient`), **profissional** (`Professional`) e **consulta** (`Appointment`, com início, duração e profissional). A clínica funciona de segunda a sexta, das 08:00 às 18:00, no fuso `America/Sao_Paulo`, definido em configuração.

## Catálogo de missões

Os mesmos 11 ids, títulos e dificuldades do Java. Onde o Java cita uma ferramenta própria, use a equivalente da stack:

| Id | Título | O que o aluno faz nesta stack | Dificuldade | Central |
| --- | --- | --- | --- | --- |
| `patients-api` | Cadastro de pacientes | API REST de pacientes com NestJS (controller e service), guardando em memória | 1 | sim |
| `persistence` | Pacientes no banco de dados | modelo `Patient` no Prisma e uma migração que cria a tabela `patients` | 2 | sim |
| `scheduling-rules` | Regras de agendamento | agendar consultas respeitando o horário de funcionamento, sem sobreposição e sem datas no passado | 3 | sim |
| `input-validation` | Validação e mensagens de erro | `ValidationPipe` com DTOs anotados, respondendo no formato Problem Details | 2 | não |
| `bug-hunt` | Caça ao bug | encontrar e corrigir um defeito escondido no cálculo da duração das consultas | 2 | não |
| `pagination` | Listagem paginada de consultas | listar as consultas de um profissional com paginação e ordenação por data | 2 | não |
| `ci-pipeline` | Pipeline de integração contínua | rodar instalação e testes a cada push com GitHub Actions | 2 | não |
| `availability-query` | Consulta de horários livres | calcular os horários livres com uma consulta SQL (`prisma.$queryRaw`) | 3 | não |
| `containerization` | Aplicação em contêiner | empacotar a aplicação numa imagem Docker e subir tudo com Docker Compose | 3 | não |
| `reminder-strategies` | Lembretes por e-mail e SMS | enviar lembretes por canais diferentes, aplicando um padrão de projeto com providers do NestJS | 4 | não |
| `api-security` | Proteção da API | exigir autenticação e separar o que recepcionistas e profissionais podem fazer, com guards do NestJS | 4 | não |

## Contratos

Copie os contratos de `missoes/<id>.md` do modelo Java: endpoints, campos JSON, status HTTP, formato de erro, formato da paginação (`content` e `page.{number,size,totalElements,totalPages}`), usuários e papéis. Um aluno precisa conseguir comparar as duas versões e ver o mesmo sistema. As únicas diferenças permitidas:

| Assunto | Java | TypeScript |
| --- | --- | --- |
| Saúde da aplicação | `/actuator/health` | `GET /health` responde `200` com `{"status":"ok"}` |
| Usuários de teste | `clinic.security.users` em `application-test.yaml` | os mesmos usuários e senhas, num arquivo de configuração de teste que você define e documenta em `missoes/api-security.md` |
| Configuração da clínica | `application.yaml` | variáveis de ambiente com valores padrão, lidas pelo `@nestjs/config` |
| Datas | `java.time` | uma biblioteca de datas com fuso (Luxon ou date-fns-tz), já instalada no código inicial |

## Regras que os testes precisam seguir

### 1. Missões e plano

Jest não tem tags. Use isto no lugar:

- Os testes de cada missão ficam em `test/missions/<id>.spec.ts` e são declarados por um helper comum, por exemplo `describeMission('<id>', 'título', () => { ... })`.
- O helper lê `.vaga-viva/plan.json` na raiz do projeto. Se a missão não estiver em `missions`, ele usa `describe.skip` e põe o motivo no nome: `Missão '<id>' fora do seu plano (.vaga-viva/plan.json)`. Sem o arquivo, todas as missões rodam. Um `plan.json` malformado faz a execução falhar com uma mensagem clara em português.
- Testes fora de `test/missions/` (por exemplo, "a aplicação sobe") rodam sempre e **passam no modelo**.
- `npm test -- missions/<id>` roda só uma missão. Documente esse comando no README e em cada `missoes/<id>.md`.
- O modelo **não** traz `.vaga-viva/plan.json`: quem grava é o Vaga Viva, ao criar o repositório do aluno. O formato é o mesmo do Java, com `"archetype": "typescript-node"`.

### 2. Falhar pelo motivo certo

No modelo, cada teste de missão:

- **passa na checagem de tipos**: `npx tsc --noEmit` passa no modelo inteiro, testes incluídos;
- **sobe o módulo de teste** do NestJS sem erro;
- **falha numa asserção** com mensagem em português que diz o que falta (por exemplo, status 404 em vez de 201). Para ter a mensagem, use `jest-expect-message` (`expect(valor, 'mensagem')`) ou equivalente. Nunca falha por erro de tipo, provider ausente, módulo que não sobe ou timeout.

Prefira testes de caixa-preta: HTTP com Supertest e consultas SQL. Quando a missão exige uma classe de domínio, o código inicial traz a interface ou a assinatura, com corpo que lança `new Error('Missão <id>')`.

### 3. Independência entre missões

As mesmas regras do Java:

- Os testes de uma missão só dependem do **código inicial** e das **missões centrais de dificuldade menor**.
- Profissionais e consultas entram por **fixtures SQL**, sobre tabelas que o código inicial já cria. A tabela de consultas guarda `patient_id` **sem chave estrangeira**.
- Todos os testes HTTP mandam Basic Auth da recepcionista por um helper comum, para continuarem passando depois de `api-security`.
- Nenhum teste fora de `input-validation` manda dados inválidos ou verifica o formato do corpo de erro.
- O defeito de `bug-hunt` continua no código em muitos planos: os testes das outras missões não podem usar o caso em que ele aparece.

### 4. O que o código inicial já traz

- Aplicação NestJS que sobe, com `GET /health`.
- Prisma configurado, com `schema.prisma` e uma migração inicial para `professionals` e `appointments` (`starts_at`, `duration_minutes`, `patient_id` sem FK). O cliente do Prisma é gerado antes dos testes (por exemplo, num script `pretest`).
- Infraestrutura de teste:
  - um `globalSetup` do Jest que sobe o PostgreSQL com Testcontainers e aplica as migrações (`prisma migrate deploy`);
  - limpeza das tabelas antes de cada teste, incluindo `patients` quando ela existir.
- A configuração da clínica (horário e fuso) e um relógio injetável, para os testes usarem datas relativas ao relógio real, com 7 ou mais dias à frente.
- `EmailSender`, `SmsSender` e `ReminderStrategy` como interfaces com tokens de injeção, mais implementações que só escrevem no log. Os testes trocam os envios por fakes que gravam o que foi enviado.
- O cálculo de duração **com o mesmo defeito do Java**: só a hora do dia é considerada, então dá errado em consultas que atravessam a meia-noite. Não deixe comentário que o denuncie.
- `missoes/<id>.md` para as 11 missões e um README genérico em português, sem dicas de solução. O Vaga Viva substitui o README depois.
- Licença MIT, com copyright de `precavidos`.

### 5. Workflow mínimo e a missão `ci-pipeline`

- O modelo traz `.github/workflows/vaga-viva.yml`, que é do Vaga Viva e não faz parte de nenhuma missão. Ele:
  - dispara em `push` **só na `main`** e em `pull_request`;
  - usa Node 22 com cache do npm e roda `npm ci` e `npm test`;
  - publica um relatório **JUnit XML** (por exemplo, com `jest-junit`, em `reports/junit.xml`) como artefato, com `if: always()`. É o mesmo formato do Surefire no Java, e o revisor de PR do Vaga Viva vai ler os dois do mesmo jeito.
- Na missão `ci-pipeline`, o aluno cria o **próprio** `.github/workflows/ci.yml`. Os testes leem o YAML e verificam:
  - que ele dispara em `push` e `pull_request`;
  - que usa `setup-node` com versão 22 e `cache: npm`;
  - que roda `npm ci` e `npm test`.

### 6. Missões com verificação fora do TypeScript

- `containerization`: o teste confere que o `Dockerfile` existe. Depois constrói a imagem com Testcontainers, sobe a aplicação junto com o PostgreSQL numa rede do Testcontainers e espera `200` em `/health`, com timeout de 10 minutos. Também lê o `compose.yaml` e procura um serviço com `build` e outro com uma imagem `postgres`.
- `availability-query`: o teste confere o resultado, não o texto do SQL.

## Lições do modelo Java

Erros que já aconteceram e não podem se repetir:

- **Caminhos no Windows.** Mensagens que citam arquivos usam o texto fixo com `/`, como `.vaga-viva/plan.json`, nunca o resultado de `path.join`. Muitos alunos usam Windows.
- **CI duplicado.** `push` sem filtro de branch mais `pull_request` roda tudo duas vezes. Use `push` só na `main`.
- **Limite de requisições do registro de pacotes.** Se o npm devolver 429, espere e reaproveite o cache, em vez de insistir. O `Dockerfile` do modelo e o da solução baixam só do registro público do npm.

## Solução de referência

- **Um commit por missão**, na ordem do catálogo, com a mensagem `Solve <id>`, por cima do `main` do modelo já mergeado. Mantenha cada missão nos próprios arquivos, para o cherry-pick isolado não dar conflito.
- Com a solução completa, `npm test` passa **sem nenhum teste ignorado**, já que não há `plan.json`.
- O merge do PR da solução vai ser **merge commit**, sem squash, para os commits por missão não se perderem.

## Critérios de aceite (o Vaga Viva vai conferir, também no Windows)

1. No modelo: `npx tsc --noEmit` passa, os testes fora de `test/missions/` passam, e cada missão tem pelo menos 3 testes. Todos falham por asserção, com mensagem em português.
2. Na solução completa: `npm test` passa, com zero falhas e zero testes ignorados.
3. Independência: para cada missão opcional M, o modelo mais os commits das 3 centrais e de M faz passar os testes de M.
4. Com um `plan.json` de 6 missões, os testes das outras 5 aparecem como ignorados, com o motivo no nome, e não como falhas.
5. Nenhum arquivo do repositório público contém a solução de uma missão, e o histórico dele não tem os commits da solução.
6. Um script `scripts/validate.sh` na solução executa os critérios 1 a 4 e imprime uma tabela missão × resultado, como o do Java. Ele precisa rodar no Git Bash do Windows.

## Etapas

Trabalhe em duas etapas, como no Java:

1. **Modelo**, no repositório público: abra um PR e **pare** para revisão.
2. **Solução**, no repositório privado: só depois de o PR do modelo ser aprovado e mergeado.

Antes de escrever código, responda com seu plano e suas dúvidas.

## Fora do escopo

- Textos das issues e README personalizado (o Vaga Viva gera).
- Front-end.
- O catálogo de missões e o vocabulário do arquétipo dentro do repositório do Vaga Viva (`typescript-node.json`): o time faz.

## Fontes

- Modelo e solução do Java: `precavidos/vaga-viva-template-java-spring` e `precavidos/vaga-viva-template-java-spring-solution`
- Briefing do Java: `docs/devin/repositorio-modelo-java-spring.md` no `precavidos/vaga-viva`
- Catálogo de missões: `src/main/resources/missions/java-spring.json`
- Contrato do plano: `docs/adr/0005-plano-no-repositorio-do-aluno.md`
- Escolha do arquétipo: `docs/adr/0006-escolha-do-arquetipo-e-compatibilidade.md`
