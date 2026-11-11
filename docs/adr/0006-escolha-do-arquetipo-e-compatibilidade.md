# Escolha do arquétipo pela vaga e aviso de compatibilidade

O arquétipo de uma vaga sai das linguagens e frameworks que ela cita, por regras fixas (ADR 0002). A resposta diz ao aluno o quanto o projeto cobre a stack da vaga: tudo (`COVERED`), em parte (`PARTIAL`), nada (`NOT_COVERED`) ou se a vaga não citou linguagem (`NOT_STATED`), junto com o que ficou de fora. Quando nenhum arquétipo cobre a vaga, o plano vem mesmo assim, do arquétipo padrão, e quem decide se continua é o aluno.

O catálogo `archetypes/catalog.json` lista as linguagens e os frameworks que o Vaga Viva sabe reconhecer, inclusive os que nenhum arquétipo pratica, e os arquétipos disponíveis. Um framework conta como citação da sua linguagem ("Kotlin com Spring Boot" é candidato ao Java/Spring). Entre os candidatos, vence o arquétipo que pratica mais itens citados; no empate, o primeiro do catálogo. Os termos seguem a regra da tecnologia nomeada: "Java" não é "JavaScript", e um termo ambíguo fica de fora ("Go" só como "Golang").

## Considered Options

- **Sempre o Java/Spring, sem aviso:** é o que acontecia antes. Uma vaga de Python recebia um projeto Java sem saber por quê.
- **Recusar vagas sem arquétipo:** é honesto, mas o aluno sai sem nada, mesmo quando queria praticar.
- **Escolher e avisar (escolhido):** o aluno vê o que o projeto pratica e o que fica de fora, e a tela decide como mostrar.

## Consequences

- O MVP planeja três arquétipos: `java-spring`, `python-fastapi` e `typescript-node`. Cada um entra no catálogo só quando tiver repositório-modelo validado (ADR 0004).
- O arquétipo escolhido decide tudo o que vem depois: o vocabulário de competências, o catálogo de missões, o repositório-modelo e o comando que roda os testes de uma missão. Hoje são dois, `java-spring` e `typescript-node`; o Python entra quando o modelo dele estiver validado.
- Um arquétipo pode praticar mais de uma linguagem: o `typescript-node` cobre TypeScript e JavaScript, já que uma vaga de Node.js costuma citar uma das duas.
- Reconhecer uma linguagem não promete um projeto para ela: a lista de linguagens cresce sem custo, e os arquétipos, com o custo de um modelo cada.
