# Um domínio fictício por arquétipo

Cada arquétipo tem um único domínio fictício fixo; no Java/Spring, é o agendamento de clínica. O plano do projeto varia quais missões entram e em que ordem, não o negócio. Os testes JUnit do repositório-modelo usam entidades concretas (pacientes, consultas), então cada domínio a mais exigiria outro repositório-modelo, com seus testes e sua solução de referência, todos validados de novo.

## Considered Options

- **Vários domínios por arquétipo** (clínica, biblioteca, academia): mais variedade entre alunos, mas multiplica o trabalho e o custo de gerar e validar os modelos.
- **Domínio só no texto:** testes genéricos (recurso, reserva), com README e issues trocando os nomes. É barato, mas o código que o aluno lê não bate com a história.
- **Um domínio fixo (escolhido):** um modelo, uma suíte de testes e uma solução de referência por arquétipo, como prevê a resposta ao risco de custo.

## Consequences

- Dois alunos da mesma turma, ou com vagas diferentes, constroem o mesmo sistema; o que muda é o conjunto de missões.
- Toda missão do catálogo precisa existir, com testes, no repositório-modelo do arquétipo antes de poder entrar num plano (ADR 0002).
- Um segundo domínio pode ser acrescentado depois, como um novo catálogo com seu próprio repositório-modelo.
