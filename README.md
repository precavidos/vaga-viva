# Vaga Viva

Aprender fazendo com vagas reais, com um revisor que não dá a resposta.

O aluno cola (ou encaminha por e-mail) uma vaga real e recebe no GitHub um projeto de estudo sob medida: README, missões em forma de issues e testes que falham até ele implementar. Um revisor de PR socrático faz perguntas e libera dicas graduais, sem nunca entregar código. Professores podem usar o modo turma para acompanhar todos os alunos num painel.

Projeto da trilha **Acesso à educação** do Hack Brasil 2026.

## Status

Em construção. Este repositório foi criado do zero durante o Hack Brasil 2026, depois da sessão de abertura de 04/11.

## Stack planejada

- Spring Boot 4.1, Java 21 e PostgreSQL 17 (Flyway para migrações)
- GitHub App e GitHub Actions
- LLM para redigir o plano do projeto e as perguntas do revisor
- Resend para e-mail

## Como rodar

Requisitos: Java 21 e Docker.

```bash
docker compose up -d   # PostgreSQL local
./mvnw spring-boot:run # API em http://localhost:8080/actuator/health
./mvnw verify          # testes (sobem um PostgreSQL via Testcontainers)
```

## Princípios

- As habilidades da vaga são mapeadas para um vocabulário fechado (ESCO mais uma lista curta própria), não extraídas em texto livre.
- A escolha e a ordem das missões são determinísticas; o LLM só redige textos.
- Se o LLM cair, o repositório-modelo e os testes continuam sendo entregues, com textos padrão.
- O texto da vaga é lido em memória; do usuário ficam apenas o login do GitHub e o e-mail, com consentimento.

## Licença

[MIT](LICENSE)

## Dados de terceiros

O vocabulário de competências usa a classificação [ESCO](https://esco.ec.europa.eu) (European Skills, Competences, Qualifications and Occupations), © União Europeia, reutilizada sob a licença [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Os rótulos ESCO vêm da API oficial e podem ser conferidos com `python scripts/esco/refresh_labels.py <arquivo>`.
