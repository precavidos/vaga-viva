# Escolha do arquétipo pela vaga e aviso de compatibilidade

O arquétipo de uma vaga sai das linguagens e frameworks que ela cita, por regras fixas (ADR 0002). A resposta diz ao aluno o quanto o projeto cobre a stack da vaga: tudo (`COVERED`), em parte (`PARTIAL`), nada (`NOT_COVERED`) ou se a vaga não citou linguagem (`NOT_STATED`), junto com o que ficou de fora. Quando nenhum arquétipo cobre a vaga, o plano vem mesmo assim, do arquétipo padrão, e quem decide se continua é o aluno.

O catálogo `archetypes/catalog.json` lista as linguagens e os frameworks que o Vaga Viva sabe reconhecer, inclusive os que nenhum arquétipo pratica, e os arquétipos disponíveis. Um framework conta como citação da sua linguagem ("Kotlin com Spring Boot" é candidato ao Java/Spring). Entre os candidatos, vence o arquétipo que pratica mais itens citados; no empate, o primeiro do catálogo. Os termos seguem a regra da tecnologia nomeada: "Java" não é "JavaScript", e um termo ambíguo fica de fora ("Go" só como "Golang").

## Considered Options

- **Sempre o Java/Spring, sem aviso:** é o que acontecia antes. Uma vaga de Python recebia um projeto Java sem saber por quê.
- **Recusar vagas sem arquétipo:** é honesto, mas o aluno sai sem nada, mesmo quando queria praticar.
- **Escolher e avisar (escolhido):** o aluno vê o que o projeto pratica e o que fica de fora, e a tela decide como mostrar.

## Consequences

- O MVP planeja três arquétipos: `java-spring`, `python-fastapi` e `typescript-node`. Cada um entra no catálogo só quando tiver repositório-modelo validado (ADR 0004).
- Com mais de um arquétipo, o plano e o vocabulário de competências passam a ser escolhidos pelo arquétipo da compatibilidade; hoje existe só o do Java/Spring.
- Reconhecer uma linguagem não promete um projeto para ela: a lista de linguagens cresce sem custo, e os arquétipos, com o custo de um modelo cada.
