# Vocabulário fechado de competências

As competências de uma vaga são mapeadas para um vocabulário fechado (a ESCO mais uma lista curta própria, como Extensão IA), e não extraídas em texto livre por um LLM. Com um conjunto fixo, as missões ficam estáveis e mensuráveis: a mesma vaga gera sempre as mesmas competências, e dá para medir a precisão da extração contra rótulos humanos.

## Considered Options

- **Texto livre extraído pelo LLM:** mais cobertura, mas cada execução pode nomear a mesma habilidade de um jeito diferente, o que impede ligar competências a missões e medir acertos.
- **Vocabulário fechado (escolhido):** cobre menos, mas tudo o que é reconhecido tem nome canônico e pode ser associado a missões.

## Consequences

- Tecnologias só são reconhecidas pelo nome ou por termos listados: “Java” não casa com “JavaScript”, e termos ambíguos ficam de fora. O erro aceito é deixar de reconhecer uma tecnologia, nunca inventar uma.
- Ampliar a cobertura significa editar o vocabulário, não ajustar um prompt.
- A licença CC BY da ESCO exige atribuição. Ainda falta confirmar com a organização se os dados podem vir do Trilha ou precisam ser baixados de novo.
