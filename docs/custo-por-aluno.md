# Custo por aluno

Quanto o LLM custa para cada aluno do Vaga Viva, medido em produção com o Claude Haiku 5.5. Os preços usados são os registrados na issue 17 (US$ 0,10 por milhão de tokens de entrada e US$ 0,50 por milhão de saída). Confira a página de preços da Anthropic antes de citar os números.

## Onde o LLM entra

| Momento | Chamadas | O que escreve |
| --- | --- | --- |
| Criação do repositório | 1 | a seção do README sobre a vaga e o contexto de cada missão |
| Revisão de um PR | 1 por push | o que já funciona e as perguntas |
| `/dica` | 1 por dica, no máximo 3 por missão e PR | o texto da dica |

O plano (competências, nível, missões), a resposta à vaga encaminhada por e-mail, o e-mail semanal e o resumo do professor **não** usam o LLM (ADR 0002).

## Medição (24 e 25/11/2026, painel do Claude Console)

| Dia (UTC) | O que aconteceu | Entrada | Saída | Custo |
| --- | --- | --- | --- | --- |
| 24/11 | 1 criação de repositório (6 missões) | 4.671 | 2.501 | cerca de US$ 0,0017 |
| 25/11 | 5 revisões e 5 dicas no PR de teste davsilvam/vaga-viva-clinic-scheduling#7 | 30,4 mil | 1.738 | cerca de US$ 0,0039 |

O custo calculado com esses preços bate com a barra de custo diário do painel nos dois dias.

Por chamada de revisão ou dica: **cerca de 3 mil tokens de entrada e 170 de saída, ou US$ 0,0004**. A entrada é maior que a saída porque cada chamada leva o diff, o contrato da missão e os testes que falham (o diff é limitado a 30 mil caracteres).

## Estimativa por aluno

Supondo que, em cada missão, o aluno faça 3 pushes e peça 2 dicas:

| Item | Conta | Custo |
| --- | --- | --- |
| Criação do repositório | 1 × US$ 0,0017 | US$ 0,0017 |
| Plano de 6 missões | 6 × (3 revisões + 2 dicas) × US$ 0,0004 | US$ 0,012 |
| **Total por aluno** | | **cerca de US$ 0,014** |

Com os US$ 5 de crédito do Hack Brasil, isso dá para cerca de **350 alunos** completando o plano inteiro. Um aluno que pede todas as 3 dicas e faz 6 pushes por missão custa cerca de US$ 0,023.

## Limites desta medição

- A amostra é pequena: uma criação e dez chamadas, num único repositório Java.
- Diffs maiores (o aluno muda muitos arquivos num PR) aumentam a entrada, até o limite de 30 mil caracteres por diff.
- Os números do piloto vão substituir a suposição de "3 pushes e 2 dicas por missão".
