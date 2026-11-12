# Modo turma: turma por link, painel e resumo semanal — design

**Cronograma:** semana 3 (19 a 25/11), com as funcionalidades congeladas em 25/11.
**Depende de:** o webhook e o revisor (PRs 30 a 32, já no `main`), as dicas (issue 27) para a contagem de dicas e o Resend (issue 28) para o envio do e-mail.

## Por que

O professor quer ver a turma inteira de uma vez e, ao abrir um aluno, entender o que está segurando ele. Hoje o Vaga Viva atende só o aluno sozinho: não guarda nada e não sabe quem é de qual turma. Também é na semana 3 que o piloto gera os números do vídeo. Os 6 testadores entram numa turma, e o painel é a cena 1:50–2:25 do roteiro.

No piloto, há **um professor (o David) e uma turma de 6 alunos**. O desenho não precisa servir a vários professores nem a turmas grandes, mas não deve impedir isso depois.

## Objetivos

1. O professor entra com o GitHub, cola uma vaga-base e cria uma turma. Ele recebe um link de convite.
2. O aluno abre o link, entra com o GitHub, aceita ser acompanhado e recebe o projeto da turma no próprio repositório. É o mesmo plano para todos.
3. O painel da turma mostra cada aluno na missão em que está, com a situação (travado, parado, avançando ou concluído) e o tempo na missão.
4. A ficha do aluno mostra o que o segura: os testes que falham na missão atual, a última pergunta do revisor, as dicas pedidas na missão e o link do PR mais recente.
5. Toda segunda de manhã, o professor recebe por e-mail um resumo com os contadores, quem está travado ou parado e em qual missão, e um link para o painel.

## Fora do escopo

- Vários professores numa turma, transferir uma turma, arquivar ou apagar pela tela. Um pedido de exclusão (LGPD) no piloto é atendido à mão, no banco.
- O aluno sair da turma pela tela.
- Ranking, notas ou comparação entre alunos.
- E-mail semanal para o **aluno** (é da issue 28).
- Ajustar os limites de "travado" e "parado" pela tela: eles ficam na configuração.
- Turmas com alunos de arquétipos diferentes: a turma tem um só plano.

## O que seria um fracasso, mesmo funcionando

- **O aluno não saber que está sendo acompanhado.** Antes de o repositório ser criado, a página de convite diz em uma frase o que o professor vai ver, e o aluno aceita. Sem o aceite, nada é criado nem gravado.
- **O professor ver mais do que precisa.** Nada de código do aluno no painel, nem do diff nem de trechos. Nada de comentários do aluno: só os textos do próprio Vaga Viva.
- **Os rótulos parecerem notas.** "Travado" e "parado" são avisos para o professor ir conversar. Eles aparecem sempre com o motivo, por exemplo "4 dias na missão 2, 3 PRs nesse período", e são escritos como aviso, nunca como avaliação.
- **Guardar a vaga.** O texto da vaga-base é lido em memória, como hoje. A turma guarda só o plano, que já não carrega nenhuma palavra da vaga (ADR 0003).
- **Mexer no fluxo do aluno sozinho.** Quem cria o repositório pela página atual, sem convite, continua sem nada gravado, e o webhook não registra nada para esses repositórios.

## Decisões do David

| Tema | Decisão |
| --- | --- |
| Entrada do aluno | Por um link do professor |
| Login do professor | GitHub, o mesmo login do aluno |
| Visão geral | Uma coluna por missão do plano, com cada aluno na missão em que está e, no topo, os contadores de travados, parados e avançando (esboço "painel-v2") |
| Ficha do aluno | Testes que falham, a última pergunta do revisor, as dicas pedidas na missão e o link do PR mais recente |
| Rótulos | **Travado** (mexe, mas não avança) e **parado** (sumiu) |
| Resumo semanal | Segunda de manhã: contadores, travados e parados com a missão, e o link do painel |
| Limites acima | Os quatro da seção anterior |
| Nome do aluno | O aluno digita como quer ser chamado ao entrar pela turma. O painel e o e-mail mostram esse nome, e o login do GitHub aparece na ficha |

## Decisões do Claude (o David pode revisar)

| Tema | Decisão |
| --- | --- |
| Limites | **Travado:** 3 dias ou mais na mesma missão, com alguma atividade nesse período. **Parado:** 5 dias ou mais sem atividade nenhuma. Ficam em `vagaviva.classroom.stuck-after` e `vagaviva.classroom.idle-after`. Se as duas regras valerem, vale "parado" |
| Quem cria turma | No piloto, só os logins em `vagaviva.classroom.teachers` (o do David). Outro login recebe 403. Isso evita que qualquer conta junte o progresso de alunos e gaste o crédito do LLM |
| Atividade | Um `workflow_run` concluído no repositório do aluno, na branch padrão ou na cabeça de um PR aberto, ou um `/dica`. Push em outras branches não conta |
| Missão atual | Calculada contra o plano **gravado na turma**, nunca contra o `.vaga-viva/plan.json` do repositório, que o aluno pode editar. É a primeira missão desse plano, na ordem, que não tem todos os testes passando no último relatório lido, seja da branch padrão ou de um PR. Uma missão sem nenhum teste no relatório conta como não passando |
| Concluído | Só quando o relatório traz testes de **todas** as missões do plano e todos passam. Um build quebrado ou um relatório parcial nunca marca "concluído"; nesse caso, a atividade é registrada e a missão atual não muda. O aluno concluído aparece numa última coluna, "Concluído" |
| Testes na ficha | O nome do teste e a mensagem, cortada em 200 caracteres e passada pelo `ReviewTextFilter`. Se o filtro recusar a mensagem, só o nome é guardado. Assim, nenhum trecho de código que vier na mensagem de uma exceção chega ao painel |
| Dicas | Contam para a missão que o `HintResponder` da issue 27 escolher. O registro fica dentro do próprio responder, e não no dispatcher. Se a issue 27 não estiver pronta em 19/11, o progresso entra sem a contagem de dicas, e a ficha mostra "dicas ainda não disponíveis" |
| Tempo na missão | Contado desde a primeira vez em que aquela missão virou a missão atual |
| Fonte do progresso | O webhook grava o progresso a cada evento. O painel só lê o banco e não consulta o GitHub na hora |
| Plano da turma | Gerado uma vez, na criação, e gravado na turma. Os alunos recebem esse plano, sem nova leitura de vaga |
| Textos do LLM | Escritos para cada aluno no provisionamento, como hoje, a partir do plano da turma. Isso mantém um só caminho de código, e o custo por aluno já foi medido |
| E-mail do professor | Pedido no formulário da turma, com consentimento, porque o e-mail do GitHub pode ser privado |
| Disparo do resumo | Um workflow agendado do GitHub Actions chama um endpoint protegido por segredo, e não um `@Scheduled`: no plano gratuito o Render dorme, e um agendador dentro da API não dispararia |
| Envio | Atrás de uma interface `ClassroomMailer`. Até a issue 28 entrar, a implementação só registra no log que um resumo seria enviado, sem o conteúdo |
| Novo ADR | **ADR 0007**, que amplia o ADR 0003: para quem entra numa turma e consente, guardamos também o nome que o aluno escolheu, o progresso por missão, os nomes dos testes que falham, a última pergunta do revisor e a contagem de dicas. Para o aluno sozinho, nada muda |

## Fluxos

### Professor cria a turma

1. Na página do professor (nova), entra com o GitHub.
2. Cola a vaga-base. A página mostra o plano, como a página do aluno já faz (`POST /api/project-plans`).
3. Dá um nome à turma, informa o e-mail e marca o consentimento para receber o resumo.
4. `POST /api/classrooms` lê a vaga de novo no servidor, como faz `StudentRepositoryController`, para que o plano não possa ser adulterado no navegador. Grava a turma com o plano e devolve o link de convite (`/turma/<código>`).
5. A página mostra o link para copiar e um botão para abrir o painel.

### Aluno entra pelo link

1. `/turma/<código>` mostra o nome da turma, quem é o professor, o arquétipo e as missões.
2. Pede o nome pelo qual o aluno quer ser chamado na turma (obrigatório, até 40 caracteres, pode ser só o primeiro nome) e mostra a frase de consentimento: *"Ao entrar, &lt;professor&gt; vai ver o nome que você escreveu, em qual missão você está, os testes que falham, as perguntas do revisor e quantas dicas você pediu. Seu código não aparece no painel."* O aluno só segue se marcar a caixa.
3. O aluno entra com o GitHub e instala o app, se ainda não tiver instalado (mesmo tratamento do 409 de hoje).
4. `POST /api/classrooms/<código>/enrollments` cria o repositório com o plano da turma (`StudentRepositoryProvisioner.provision`) e grava a matrícula com a data do consentimento.
5. Se o aluno já estiver matriculado, o endpoint devolve o repositório existente e não cria outro.

### Webhook registra o progresso

O `WebhookDispatcher` já recebe `workflow_run` e `issue_comment`. Antes de revisar ou responder, ele consulta se o repositório é de uma matrícula. Se não for, segue como hoje, sem gravar nada. Se for:

- **`workflow_run` concluído, na branch padrão ou num PR aberto:**
  - lê o relatório JUnit com o que já existe em `JunitReports` e `FailedTestsSource`;
  - calcula a missão atual e atualiza o progresso: missão atual, desde quando, última atividade, testes que falham na missão atual e, se houver PR, o link dele;
  - se o relatório não foi lido (`testReportRead = false`), só registra a atividade.
- **Revisor comentou:** guarda a pergunta postada como a última pergunta, depois do filtro de código (`ReviewTextFilter`).
- **`/dica`:** registra a atividade. O `HintResponder` soma 1 nas dicas da missão que ele escolheu.

Uma falha ao gravar o progresso não pode impedir o revisor nem as dicas. Ela é registrada no log apenas com o repositório e o número do PR.

### Resumo semanal

`.github/workflows/classroom-summary.yml` roda na segunda às 11:00 UTC (8:00 em Brasília) e chama `POST /api/classrooms/weekly-summaries`, com o segredo em um cabeçalho. Para cada turma cujo professor consentiu, a API monta o resumo com os mesmos cálculos do painel e envia. O texto vai em português, como texto puro e um HTML simples:

> **Turma piloto — semana de 18/11**
> 2 travados · 1 parado · 3 avançando
> - João Pedro: travado há 4 dias na Missão 2 (Persistência), 3 PRs nesse período
> - Caio: parado há 6 dias na Missão 1 (API de pacientes)
>
> Abrir o painel: &lt;link&gt;

## Dados (Flyway, `V2__classrooms.sql`)

Este é o esboço; o nome das colunas fica a critério de quem implementar.

- **`classroom`:** id, código do convite (aleatório, não sequencial), nome, login do GitHub do professor, e-mail do professor e data do consentimento (os dois podem ser nulos), plano (JSON com o mesmo conteúdo de `ProjectPlan`: só ids), data de criação.
- **`enrollment`:** id, turma, login do GitHub do aluno, nome escolhido pelo aluno, nome completo do repositório (único), data do consentimento, data de entrada. Único por turma e aluno.
- **`enrollment_progress`:** uma linha por matrícula, com:
  - a missão atual, desde quando e se está concluído;
  - a última atividade e o início da atividade na missão atual;
  - os testes que falham na missão atual (JSON com nome e mensagem);
  - a última pergunta do revisor e o link do PR mais recente;
  - as dicas por missão (JSON, de missão para quantidade);
  - os PRs abertos desde que a missão atual começou.

O e-mail do aluno não entra aqui (é da issue 28).

## API

Todas as rotas exigem login, exceto `GET /api/classrooms/<código>`.

| Rota | Quem | O que faz |
| --- | --- | --- |
| `POST /api/classrooms` | professor | cria a turma a partir do texto da vaga-base, do nome e do e-mail |
| `GET /api/classrooms` | professor | lista as turmas do professor logado |
| `GET /api/classrooms/<código>` | qualquer pessoa | nome, professor, arquétipo e missões, para a página de convite. Não lista os alunos |
| `POST /api/classrooms/<código>/enrollments` | aluno | nome, consentimento, criação do repositório e matrícula |
| `GET /api/classrooms/<id>/dashboard` | só o professor da turma | colunas, contadores e cartões |
| `GET /api/classrooms/<id>/students/<login>` | só o professor da turma | a ficha do aluno |
| `POST /api/classrooms/weekly-summaries` | workflow agendado | protegido por segredo, sem sessão e sem CSRF |

A situação de cada aluno é calculada na leitura, a partir do progresso e do relógio, com um `Clock` injetado para os testes.

## Telas

Elas seguem o estilo de `src/main/resources/static/index.html`. Os esboços aprovados estão em `.superpowers/brainstorm/`, que fica fora do Git:

- **Professor:** criação da turma e lista de turmas.
- **Convite:** `/turma/<código>`.
- **Painel:** os contadores no topo e uma coluna por missão do plano, mais a coluna "Concluído". Cada cartão tem uma borda colorida (vermelho para travado, cinza para parado, verde para avançando), o nome do aluno e o tempo na missão.
- **Ficha:** abre ao clicar no cartão, com os três blocos (testes que falham, última pergunta, dicas) e o link para o PR.
- **Acessibilidade:** as telas funcionam em 1440 px e 375 px, e a cor nunca é o único sinal (o texto da situação sempre aparece). A semana 3 tem a auditoria com axe.

## Ordem de entrega

O piloto precisa dos alunos entrando pelo link desde o primeiro dia, para os números existirem. Por isso:

1. **Até 19/11:**
   - ADR 0007;
   - a migração;
   - criação da turma e entrada pelo link;
   - o registro do progresso no webhook.

   O painel pode chegar depois, porque o progresso já estará gravado.
2. **Até 22/11:** o painel e a ficha.
3. **Até 24/11:** o resumo semanal. Sem a issue 28, ele fica só no log; o envio de verdade entra quando o Resend estiver pronto.

## O que fica para quem implementar

- O nome exato das classes, colunas e rotas das páginas estáticas.
- Como a página do professor reaproveita o código da página do aluno.
- A forma do código do convite. Ele precisa ser impossível de adivinhar; por exemplo, 10 caracteres aleatórios.
- A organização dos testes, que seguem o padrão do projeto: Testcontainers para o banco e nenhuma chamada de rede para o GitHub ou o LLM.
