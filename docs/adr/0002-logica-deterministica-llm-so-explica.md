# Lógica determinística; o LLM só explica

A escolha e a ordem das missões vêm das competências da vaga e do arquétipo, por regras determinísticas. O LLM só redige textos: o README, o contexto das missões e as perguntas do revisor. Assim o plano é reproduzível e testável, o custo por aluno fica previsível, e uma falha do LLM não impede a entrega.

## Considered Options

- **LLM monta o plano inteiro:** mais flexível, mas não é reproduzível, é difícil de testar e pode sugerir missões sem testes no repositório-modelo.
- **Regras escolhem, LLM redige (escolhido):** o plano só usa missões que existem e foram validadas contra a solução de referência.

## Consequences

- Se o LLM cair, o repositório-modelo e os testes continuam sendo entregues, só que com textos padrão.
- O revisor nunca decide se uma missão foi concluída: quem decide são os testes. O LLM só formula as perguntas.
- Uma missão nova exige testes no repositório-modelo antes de poder entrar em qualquer plano.
