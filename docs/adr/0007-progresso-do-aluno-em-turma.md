# Progresso do aluno em turma

No modo turma, o professor acompanha os alunos que entraram por um convite. Para isso, o Vaga Viva grava o progresso de cada aluno que entra e consente. O aluno que usa o Vaga Viva sozinho não tem nada novo guardado: vale o ADR 0003.

## Dados guardados

Só para quem entra numa turma e consente:

- o nome que o aluno escolheu para aparecer no painel;
- o login do GitHub e o repositório do aluno;
- a missão atual, há quanto tempo ele está nela e os números dos PRs abertos nessa missão;
- se o aluno concluiu todas as missões e a hora da última atividade;
- os nomes dos testes que falham e as mensagens filtradas;
- a última pergunta do revisor e o link do último PR;
- a contagem de dicas pedidas por missão: o campo existe, mas só passa a ser preenchido quando a função de dicas existir (Tarefa 10);
- na turma: o login do GitHub do professor e, se ele consentir, o e-mail e a hora do consentimento.

O texto da vaga-base continua sem ser gravado: a turma guarda só ids de arquétipo, missões, motivos, competências e tecnologias. O código do aluno também não é gravado.

## Considered Options

- **Consultar o GitHub na hora de abrir o painel:** não guarda nada além da matrícula, mas cada abertura vira várias chamadas à API por aluno, com limite de taxa, e o painel fica lento e frágil.
- **Gravar a cada evento (escolhido):** o webhook e as dicas atualizam uma linha de progresso por aluno; o painel só lê o banco.

## Consequences

- O banco passa a guardar dados pessoais de alunos (nome escolhido e login), por isso o consentimento aparece antes da matrícula e os logs só citam o repositório e o número do PR.
- Exclusões no piloto são feitas à mão, a pedido do aluno ou do professor.
- O progresso pode ficar um pouco atrás do GitHub se um evento se perder; o próximo evento do aluno corrige.
