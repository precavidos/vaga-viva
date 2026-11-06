# Briefing para o Devin: repositório-modelo Java/Spring

Este texto é o pedido de trabalho para o Devin. Ele é autossuficiente: não é preciso ler o repositório do Vaga Viva para executá-lo, mas as fontes estão citadas no fim.

## Objetivo

Criar o **repositório-modelo** do arquétipo `java-spring` e a sua **solução de referência**. O Vaga Viva cria o repositório de cada aluno como cópia do modelo e escolhe de 5 a 6 missões do catálogo abaixo. Em cada missão, os testes falham até o aluno implementá-la.

## Entregáveis

| Repositório | Visibilidade | Conteúdo |
| --- | --- | --- |
| `precavidos/vaga-viva-template-java-spring` | **público**, com *Template repository* marcado | código inicial, testes de **todas** as 11 missões, extensão JUnit do plano, workflow mínimo |
| `precavidos/vaga-viva-template-java-spring-solution` | **privado** | o modelo mais a implementação de todas as missões, **um commit por missão** |

O aluno nunca pode ver a solução. Nada do repositório público pode conter, em código, comentário, histórico ou branch, a implementação de uma missão.

## Stack e convenções

- Java 21, Spring Boot 4.1.x, Maven com wrapper (`./mvnw`), PostgreSQL 17.
- Testes com JUnit 5 e Testcontainers (PostgreSQL); `./mvnw verify` precisa rodar só com Java 21 e Docker.
- Pacote raiz `io.github.precavidos.clinic`. Código, nomes e commits em **inglês**.
- Tudo o que o aluno lê fica em **português do Brasil**: README, `@DisplayName` dos testes, mensagens das asserções, mensagens de erro da API.
- Só dados fictícios: nada de nomes, CPFs, telefones ou e-mails reais nas fixtures (use domínios como `example.com`).

## Domínio fictício: agendamento de clínica

Sistema de uma clínica para cadastrar pacientes e agendar consultas com profissionais de saúde. Entidades: **paciente** (`Patient`), **profissional** (`Professional`) e **consulta** (`Appointment`, com início, duração e profissional). A clínica tem um horário de funcionamento fixo, definido em configuração.

## Catálogo de missões

O id é o mesmo usado nas tags dos testes e no `plan.json`. Missões centrais entram em todo plano.

| Id | Título | O que o aluno faz | Dificuldade | Central |
| --- | --- | --- | --- | --- |
| `patients-api` | Cadastro de pacientes | API REST para cadastrar, buscar, alterar e remover pacientes | 1 | sim |
| `persistence` | Pacientes no banco de dados | guardar os pacientes no PostgreSQL com JPA e criar o esquema com uma migração | 2 | sim |
| `scheduling-rules` | Regras de agendamento | agendar consultas respeitando o horário de funcionamento, sem sobreposição e sem datas no passado | 3 | sim |
| `input-validation` | Validação e mensagens de erro | recusar dados inválidos com mensagens claras, no formato padrão da API (Problem Details) | 2 | não |
| `bug-hunt` | Caça ao bug | encontrar e corrigir um defeito escondido no cálculo da duração das consultas | 2 | não |
| `pagination` | Listagem paginada de consultas | listar as consultas de um profissional com paginação e ordenação por data | 2 | não |
| `ci-pipeline` | Pipeline de integração contínua | rodar build e testes a cada push com GitHub Actions | 2 | não |
| `availability-query` | Consulta de horários livres | calcular, com uma consulta SQL, os horários livres de um profissional em um dia | 3 | não |
| `containerization` | Aplicação em contêiner | empacotar a aplicação numa imagem Docker e subir tudo com Docker Compose | 3 | não |
| `reminder-strategies` | Lembretes por e-mail e SMS | enviar lembretes por canais diferentes, aplicando um padrão de projeto | 4 | não |
| `api-security` | Proteção da API | exigir autenticação e separar o que recepcionistas e profissionais podem fazer | 4 | não |

## Regras que os testes precisam seguir

### 1. Tags e plano

- Todo teste de missão leva `@Tag("<id da missão>")`. Testes sem tag de missão (por exemplo, "a aplicação sobe") rodam sempre e **passam no modelo**.
- Uma extensão JUnit (`ExecutionCondition`, registrada por autodetecção em `junit-platform.properties`) lê `.vaga-viva/plan.json` na raiz do projeto e **desativa** os testes cuja tag de missão não está em `missions`. Sem o arquivo, todos rodam. O motivo da desativação aparece no relatório ("missão fora do seu plano").
- `./mvnw test -Dgroups=<id>` roda só uma missão.
- O modelo **não** traz `.vaga-viva/plan.json`: quem grava é o Vaga Viva, ao criar o repositório do aluno. Formato:

```json
{
  "archetype": "java-spring",
  "domain": "clinic-scheduling",
  "missions": ["patients-api", "persistence", "pagination", "scheduling-rules", "availability-query", "containerization"]
}
```

### 2. Falhar pelo motivo certo

No modelo, cada teste de missão:

- **compila** (o modelo inteiro compila com `./mvnw test-compile`);
- **sobe o contexto** do Spring sem erro;
- **falha numa asserção** que diz o que falta, em português (por exemplo, status 404 em vez de 201, ou "esperava recusar consulta no passado"). Nunca por erro de compilação, `NoSuchBeanDefinitionException`, contexto que não sobe ou timeout.

Para isso, prefira testes de caixa-preta: chamadas HTTP (MockMvc ou cliente REST de teste) e consultas SQL. Quando a missão exige uma classe de domínio, o código inicial traz a interface ou a assinatura, com corpo que lança `UnsupportedOperationException("Missão <id>")`.

### 3. Independência entre missões

O plano escolhe um subconjunto qualquer das missões opcionais, sempre junto com as 3 centrais, e as ordena por dificuldade. Então:

- Os testes de uma missão só podem depender do **código inicial** e das **missões centrais de dificuldade menor** que a dela. Exemplo: `pagination` (2) pode usar a API de `patients-api` (1), mas não as regras de `scheduling-rules` (3).
- Dados de que um teste precisa e que viriam de outra missão (profissionais, consultas) entram por **fixtures SQL** dos próprios testes, sobre tabelas que o código inicial já cria. Isso vale para todas as tabelas, exceto a de pacientes, que é o assunto de `persistence`. Como uma missão de dificuldade 2 pode vir antes de `persistence`, a tabela de consultas do código inicial guarda `patient_id` **sem chave estrangeira**. Se a solução de `persistence` quiser a chave, ela entra na migração dessa missão.
- Testes de uma missão não podem quebrar quando o aluno faz outra:
  - todos os testes HTTP enviam credenciais de usuários de teste por um helper comum, para continuarem passando depois de `api-security`. Os usuários e papéis (recepcionista, profissional) ficam fixos em `src/test/resources`, e a missão pede que o aluno os use;
  - nenhum teste fora de `input-validation` manda dados inválidos nem verifica o formato do corpo de erro.

### 4. O que o código inicial já traz

- Aplicação Spring Boot que sobe, com `/actuator/health`.
- JPA, Flyway e driver do PostgreSQL configurados, com uma migração inicial para profissionais e consultas (a de pacientes fica para `persistence`). Uma classe-base de teste com Testcontainers e `@ServiceConnection`.
- O cálculo de duração de consulta **com um defeito sutil**, para `bug-hunt`. Por exemplo: erro em consultas que atravessam a meia-noite ou arredondamento de minutos. O defeito não pode ter comentário que o denuncie. Como `bug-hunt` é opcional, o defeito continua lá em muitos planos: os testes das outras missões (sobretudo `scheduling-rules` e `availability-query`) não podem usar o caso em que ele aparece.
- As interfaces mínimas descritas na regra 2.
- Um README genérico em português: o que é o projeto, como rodar (`./mvnw verify`, Docker), como rodar uma missão e o significado de `.vaga-viva/plan.json`. Sem dicas de solução. O Vaga Viva substitui o README depois.

### 5. Workflow mínimo e a missão `ci-pipeline`

- O modelo traz `.github/workflows/vaga-viva.yml`, que é do Vaga Viva e não faz parte de nenhuma missão. Ele roda `./mvnw -B verify` em `push` e `pull_request` e publica os relatórios do Surefire (`target/surefire-reports`) como artefato, mesmo quando os testes falham. O revisor de PR do Vaga Viva vai ler esse artefato.
- Na missão `ci-pipeline`, o aluno cria o **próprio** `.github/workflows/ci.yml`. Os testes da missão leem esse YAML (SnakeYAML) e verificam:
  - que ele dispara em `push` e `pull_request`;
  - que usa Java 21 com cache do Maven;
  - que roda `./mvnw verify`.
- Os testes não devem cobrar exatamente o conteúdo do `vaga-viva.yml`.

### 6. Missões com verificação fora do Java

- `containerization`: o teste constrói a imagem do `Dockerfile` do aluno com Testcontainers e confere que a aplicação responde em `/actuator/health`. Também confere que o `compose.yaml` declara a aplicação e o PostgreSQL. A imagem não pode depender de rede além do Maven Central.
- `availability-query`: o teste confere o resultado, não o texto do SQL. O README da missão (na issue, feito pelo Vaga Viva) pede o uso de SQL. Você não precisa forçar isso no teste.

## Solução de referência

- **Um commit por missão**, na ordem do catálogo, com a mensagem `Solve <id>`, por cima do modelo. Assim dá para validar cada missão isoladamente.
- Com a solução completa, `./mvnw verify` passa **sem nenhum teste desativado**, já que não há `plan.json`.

## Critérios de aceite (o Vaga Viva vai conferir)

1. No modelo: `./mvnw test-compile` passa, os testes sem tag passam, e cada missão (`-Dgroups=<id>`) tem pelo menos 3 testes. Todos falham por asserção, com mensagem em português.
2. Na solução completa: `./mvnw verify` passa, com zero falhas e zero testes ignorados.
3. Independência: para cada missão opcional M, o modelo mais os commits das 3 centrais e de M faz passar os testes de M.
4. Com um `plan.json` de 6 missões, os testes das outras 5 aparecem como desativados, com o motivo, e não como falhas.
5. Nenhum arquivo do repositório público contém a solução de uma missão, e o histórico dele não tem os commits da solução.
6. Um script `scripts/validate.sh` na solução executa os critérios 1 a 4 e imprime uma tabela missão × resultado.

## Fora do escopo

- Textos das issues e o README personalizado (o Vaga Viva gera).
- Front-end.
- Outros arquétipos.

## Fontes no repositório do Vaga Viva

- Catálogo: `src/main/resources/missions/java-spring.json`
- Contrato do plano: `docs/adr/0005-plano-no-repositorio-do-aluno.md`
- Domínio único por arquétipo: `docs/adr/0004-um-dominio-ficticio-por-arquetipo.md`
- Glossário: `CONTEXT.md`
