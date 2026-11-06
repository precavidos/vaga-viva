# Textos do aluno escritos por LLM e custo por aluno — design

**Issue:** [precavidos/vaga-viva#17](https://github.com/precavidos/vaga-viva/issues/17)
**Decisão de provedor:** [comentário na issue](https://github.com/precavidos/vaga-viva/issues/17#issuecomment-6694995461) (2026-11-06)
**Branch:** `feat/llm-texts`, a partir do `main`

## Objetivo

O repositório do aluno passa a ter textos escritos para a vaga dele: uma seção "Seu projeto" no README e, em cada issue de missão, um parágrafo dizendo por que aquela missão importa para aquela vaga. O LLM só redige; o plano chega pronto e não muda (ADR 0002). Se o LLM falhar, demorar ou não estiver configurado, entram textos padrão e o provisionamento termina normalmente.

Além disso, medimos o custo real com 3 vagas fictícias e registramos o custo por aluno em `docs/custo-por-aluno.md`, que é um dos critérios do checkpoint de go/no-go de 12/11 (prazo: 11/11).

## Decisões

| Tema | Decisão |
| --- | --- |
| Provedor e modelo | Anthropic, `claude-haiku-5-5`, com thinking desligado. Cerca de US$ 5 de crédito pré-pago numa conta do time. Proposto na issue e aguardando o acordo do David |
| Chamadas por aluno | **Uma**, com saída estruturada em JSON: o parágrafo do README e um texto por missão |
| Cliente | SDK oficial `com.anthropic:anthropic-java`, atrás de uma interface nossa |
| README | O README do repositório-modelo não muda. Só a **cópia do aluno** recebe a seção "Seu projeto" |
| Entrada do LLM | Só o que foi extraído: nada do texto da vaga e nada dos termos encontrados nela (ADR 0003) |

## Componentes

### `plan.ProjectPlan` (alterado)

Ganha duas listas, `postingSkills` e `postingTechnologies`, com os **ids** das competências e das tecnologias reconhecidas na vaga, na ordem do vocabulário. `ProjectPlanner.plan(ExtractedJobPosting)` preenche as duas a partir de `Recognition`. Os termos encontrados no texto (`matchedTerms`, `evidence`) não entram. O ADR 0003 já permite guardar as competências reconhecidas, e as tecnologias são do mesmo tipo de dado.

A resposta de `POST /api/project-plans` não muda.

### `writing.MissionTextWriter` (novo pacote `writing`)

```java
public interface MissionTextWriter {
    WrittenTexts write(ProjectPlan plan);
}

public record WrittenTexts(Optional<String> projectIntro,
                           Map<String, String> missionContexts,   // id da missão -> texto
                           Optional<TokenUsage> usage) { }

public record TokenUsage(long inputTokens, long outputTokens) { }
```

- `write` **nunca lança exceção**. Qualquer falha devolve `WrittenTexts` vazio, e o chamador usa os textos padrão.
- `missionContexts` só traz as missões que o LLM respondeu com texto não vazio e cujo id faz parte do plano. Ids desconhecidos são ignorados.

Implementações:

1. **`ClaudeMissionTextWriter`**: monta o prompt, chama a API e lê a resposta estruturada.
2. **`DefaultMissionTextWriter`**: devolve sempre um resultado vazio. Entra quando `vagaviva.llm.api-key` está vazio, então sem chave nem se cria o cliente da Anthropic.

Os textos padrão (fallback) ficam em `provisioning`, ao lado do `MissionIssueText`, e não dentro do escritor. Assim o fallback vale para qualquer falha, inclusive uma resposta parcial.

### `ClaudeMissionTextWriter`

- **Modelo:** `vagaviva.llm.model`, com padrão `claude-haiku-5-5`.
- **Thinking:** desligado (`ThinkingConfigDisabled`, aceito no Haiku 5.5 com esforço `high` ou menor). O esforço fica explícito em `low`.
- **Limites:** `max_tokens` de 4.000. Timeout de `vagaviva.llm.timeout` por tentativa, padrão 30 s, sem nova tentativa do SDK (`vagaviva.llm.max-retries`, padrão 0). No pior caso, cerca de 30 s, uma vez por aluno; uma falha só significa textos padrão.
- **Limites dos textos:** `projectIntro` com até 1.500 caracteres e cada `context` com até 800. Texto vazio, mais longo que isso, com bloco de código (```` ``` ````) com uma linha começando por `#`, com uma menção `@usuario` ou com uma referência `#<número>` é descartado, e entra o padrão daquela parte.
- **Saída:** estruturada via `output_config.format`, com JSON Schema:
  `{ "projectIntro": string, "missions": [{ "id": string, "context": string }] }`.
  O plano de implementação confere primeiro se o Haiku 5.5 aceita `output_config.format`. Se não aceitar, o schema vai no prompt e a resposta é validada com Jackson. O contrato do `MissionTextWriter` não muda em nenhum dos casos.
- **Prompt de sistema, fixo e em português:** o aluno vai construir o projeto do domínio fictício; o texto deve explicar, não ensinar a resolver nem dar código; tom direto, em segunda pessoa; `projectIntro` com 1 ou 2 parágrafos curtos citando o nível e as competências da vaga; cada `context` com 2 a 4 frases ligando a missão às competências da vaga; não inventar requisitos nem empresas; não mencionar dados pessoais.
- **Mensagem do usuário, montada a partir do plano** (rótulos vêm do `SkillVocabulary`):
  - arquétipo;
  - domínio fictício (nome e descrição);
  - nível;
  - rótulos das competências da vaga;
  - nomes das tecnologias da vaga;
  - missões na ordem: id, título, resumo e os rótulos das competências da vaga que cada uma exercita.
- **Erros tratados como resultado vazio:** `stop_reason` diferente de `end_turn` (inclusive `refusal` e `max_tokens`), JSON que não valida, exceção de rede ou da API, e timeout. Nenhum deles é relançado.
- **Log:** um `INFO` por chamada, só com o modelo, os tokens de entrada e saída e a duração. Falhas geram um `WARN` só com o tipo da exceção ou o `stop_reason`, nunca com o prompt nem a resposta (ADR 0003).

### `provisioning.StudentRepositoryProvisioner` (alterado)

Nova ordem:

1. `texts = writer.write(plan)`, antes de qualquer chamada ao GitHub.
2. Criar o repositório a partir do modelo (como hoje).
3. Gravar `.vaga-viva/plan.json` (como hoje).
4. Abrir as issues na ordem do plano. O corpo é o texto de contexto da missão seguido do corpo atual do `MissionIssueText`.
5. Atualizar o README do aluno, por último, porque a seção precisa dos links das issues.

Textos padrão:

- **Contexto da missão (padrão):** `Esta missão exercita <rótulos das competências da vaga que ela exercita>, pedidas na vaga.` Se `matchedSkills` da missão estiver vazio: `Esta missão faz parte da base de todo projeto do Vaga Viva.`
- **Introdução do projeto (padrão):** um parágrafo montado a partir do nível, do domínio fictício e das competências da vaga, sem LLM.

O corpo da issue fica assim:

```markdown
### Por que esta missão importa para a vaga

<contexto do LLM ou padrão>

<corpo atual do MissionIssueText>
```

### README do aluno

O `GitHubApi` ganha duas operações:

```java
Optional<GitHubFile> readFile(String token, String repositoryFullName, String path);     // GET contents
void updateFile(String token, String repositoryFullName, String path, String commitMessage,
                String content, String sha);                                              // PUT contents com sha
```

`GitHubFile(String content, String sha)` traz o conteúdo já decodificado de Base64 como UTF-8. `readFile` devolve vazio quando o GitHub responde 404 e rejeita respostas que não são de um arquivo comum (diretório ou arquivo sem conteúdo) com `GitHubApiException` 422.

Uma função pura, `StudentReadme.withProjectSection(String readme, String section)`, decide onde a seção entra:

- antes da primeira linha que começa com `## `, ignorando linhas dentro de blocos de código cercados (``` ou ~~~, com até 3 espaços de recuo, como no CommonMark);
- no fim do arquivo, se não houver nenhum `## `;
- se o README não existir, o arquivo é criado (`createFile`) só com o título `# Vaga Viva: <domínio>` e a seção.

A seção:

```markdown
## Seu projeto

<introdução do LLM ou padrão>

| Ordem | Missão | Issue |
| --- | --- | --- |
| 1 | <título> | [#<número>](<url da issue>) |
...
```

A tabela vem do plano e das issues criadas, nunca do LLM. A leitura e a gravação passam pelo `whenReady`, que já existe para tratar o 404/409 logo depois de criar o repositório a partir do modelo. Mensagem do commit: `Add your project section to the README`. Uma falha neste passo é registrada no log (só o status ou o tipo da exceção) e **não** faz o provisionamento falhar, porque as issues já carregam o contexto.

## Configuração

`application.yaml`:

```yaml
vagaviva:
  llm:
    api-key: ${ANTHROPIC_API_KEY:}
    model: claude-haiku-5-5
    timeout: 30s
    max-retries: 0
```

- `LlmProperties` (`@ConfigurationProperties("vagaviva.llm")`) e uma `@Configuration` que cria o `ClaudeMissionTextWriter` quando a chave não está vazia, e o `DefaultMissionTextWriter` caso contrário.
- `.env.example` ganha `ANTHROPIC_API_KEY=`, com um comentário dizendo que sem ela os textos padrão são usados.
- `docs/deploy.md` ganha `ANTHROPIC_API_KEY` na lista de variáveis do Render. **Cadastrar a chave no Render é um passo manual do time.**
- Dependência nova no `pom.xml`: `com.anthropic:anthropic-java` `3.6.0`, a última no Maven Central em 2026-11-06.

## Medição de custo

- **3 vagas fictícias** em `src/test/resources/llm-cost/`: `estagio.txt`, `junior.txt` e `pleno.txt`. São escritas por nós, sem nomes, e-mails, telefones ou empresas reais, e têm competências diferentes para que os planos variem.
- **Medição:** `LlmCostMeasurement`, um teste JUnit que só roda com `-Dvagaviva.measure-llm-cost=true` e `ANTHROPIC_API_KEY` definida (`@EnabledIfSystemProperty` mais `@EnabledIfEnvironmentVariable`). Para cada vaga, roda extração, plano e o `ClaudeMissionTextWriter` real, e imprime os tokens de entrada e saída e o custo calculado com os preços do Haiku 5.5. Nunca roda no `./mvnw verify` nem no CI. Comando:

  ```bash
  ANTHROPIC_API_KEY=... ./mvnw test -Dtest=LlmCostMeasurement -Dvagaviva.measure-llm-cost=true
  ```

- **`docs/custo-por-aluno.md`**, em português, com:
  - a data, o modelo e os preços por 1M tokens;
  - uma tabela com os tokens e o custo de cada uma das 3 vagas;
  - o custo médio por aluno;
  - quantos alunos cabem em US$ 5;
  - a estimativa por PR revisado (semana 2), feita a partir dos tokens medidos, com as premissas explícitas;
  - a tabela comparativa de provedores publicada na issue.

## Testes (sem rede)

- **`StudentRepositoryProvisionerTests`**, com um `MissionTextWriter` falso:
  - sucesso: os textos do LLM aparecem no corpo das issues e na seção do README;
  - resultado vazio, que representa falha, timeout ou ausência de chave: textos padrão em todas as issues e no README, e o provisionamento termina;
  - resposta parcial: o texto padrão entra só nas missões que faltaram;
  - a tabela do README traz as missões na ordem do plano, com os números das issues.
- **`StudentReadmeTests`:** a seção entra antes do primeiro `##`; vai para o fim quando não há `##`; uma linha como `###` não conta como `##`.
- **`ClaudeMissionTextWriterTests`**, com o SDK apontado (`baseUrl`) para um `com.sun.net.httpserver.HttpServer` local:
  - a requisição usa o modelo configurado, desliga o thinking, pede saída estruturada e não contém o texto da vaga nem os termos encontrados nela;
  - uma resposta válida vira `WrittenTexts`, com o `usage` preenchido;
  - HTTP 500, `stop_reason: refusal`, JSON inválido e uma resposta lenta (timeout curto no teste) viram resultado vazio, sem exceção;
  - ids de missão fora do plano são ignorados.
- **`ProjectPlannerTests`:** o plano traz as competências e as tecnologias reconhecidas, sem os termos do texto.
- **`GitHubClientTests`:** `readFile` decodifica Base64 e devolve vazio em 404; `updateFile` envia o `sha`.

## Fora do escopo

- As perguntas do revisor de PR (semana 2). Entra só a estimativa de custo no documento.
- Mudar o README do repositório-modelo ou o do Vaga Viva.
- Expor os novos campos do plano na resposta de `POST /api/project-plans`.
- Cache de prompt: as instruções de sistema ficam abaixo do tamanho mínimo que pode ser cacheado, e cada aluno faz uma chamada só.

## Critérios de pronto (da issue)

- [ ] Provedor e modelo combinados com o time (comentário publicado; falta a resposta do David)
- [ ] README e textos das missões gerados a partir do plano, com textos padrão como fallback
- [ ] Testes de sucesso e de fallback, sem chamadas de rede
- [ ] `docs/custo-por-aluno.md` com tokens e custo de 3 vagas, o custo por aluno e a comparação com os créditos
- [ ] Chave cadastrada no Render (manual) e `.env.example` atualizado
