# Plano gravado no repositório do aluno e testes marcados por missão

O repositório-modelo traz os testes de todas as missões do catálogo, mas cada plano usa só 5 ou 6. Em vez de apagar os testes que sobram, o Vaga Viva grava no repositório do aluno o arquivo `.vaga-viva/plan.json` com as missões do plano, e o modelo desliga os testes das missões que não estão nele. Assim existe um só repositório-modelo por arquétipo (ADR 0004) e nenhum arquivo do modelo precisa ser alterado depois da criação.

## Contrato com o repositório-modelo

- Cada teste de missão leva `@Tag("<id da missão>")`, com o mesmo id do catálogo (por exemplo, `@Tag("patients-api")`).
- Uma extensão JUnit do modelo lê `.vaga-viva/plan.json` e desativa os testes cuja tag não está em `missions`. Sem o arquivo, todos os testes rodam (é o caso da solução de referência).
- Para rodar só uma missão: `./mvnw test -Dgroups=<id da missão>`.
- Formato do arquivo, sem nenhum dado pessoal:

```json
{
  "archetype": "java-spring",
  "domain": "clinic-scheduling",
  "missions": ["patients-api", "persistence", "pagination", "scheduling-rules", "availability-query", "containerization"]
}
```

## Considered Options

- **Apagar os testes das missões fora do plano:** o repositório fica mais limpo, mas são várias chamadas à API e uma convenção de caminhos frágil.
- **Arquivo de plano e tags (escolhido):** uma única gravação, e o aluno pode ver o plano no próprio repositório.

## Consequences

- O app precisa da permissão Contents com escrita.
- Os testes das missões fora do plano continuam no repositório do aluno, desativados; ele pode tentar fazê-las por conta própria.
