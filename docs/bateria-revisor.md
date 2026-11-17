# Bateria de PRs contra o revisor e as dicas

Issue 29. Resultado da bateria que tenta fazer o revisor de PR e o `/dica` entregarem código ou a solução de uma missão.

## Como rodar de novo

A bateria roda contra um app local, nunca contra o serviço do Render nem o GitHub App de produção.

1. **Branch:** `test/bateria-revisor` (criada de `feat/three-level-hints`). Antes de subir o app, `./mvnw clean`: um `target/` de outra branch carrega classes e migrações que não são desta.
2. **Banco:** um Postgres só da bateria, na porta 55432:
   `docker run -d --name vaga-viva-bateria-db -e POSTGRES_DB=vagaviva -e POSTGRES_USER=vagaviva -e POSTGRES_PASSWORD=vagaviva -p 55432:5432 postgres:17-alpine`
3. **Túnel:** `cloudflared tunnel --url http://localhost:8081`. A URL `https://<nome>.trycloudflare.com` muda a cada vez que o túnel sobe; atualize o webhook do App de teste com ela.
4. **GitHub App de teste:** `vaga-viva-bateria`, da organização `precavidos`, instalado só em `precavidos/vaga-viva-bateria`. Permissões: Contents e Actions só leitura, Pull requests e Issues leitura e escrita. Eventos: Pull request, Workflow run, Issue comment. Webhook em `<URL do túnel>/api/github/webhook`.
5. **Segredos:** `.env.bateria` na raiz (ignorado pelo Git) com `GITHUB_APP_ID`, `GITHUB_APP_SLUG`, `GITHUB_WEBHOOK_SECRET`, `GITHUB_APP_PRIVATE_KEY` e `ANTHROPIC_API_KEY`. Com a chave da Anthropic em branco, o revisor e as dicas usam só os textos fixos.
6. **App:** `set -a; source .env.bateria; set +a; PORT=8081 DATABASE_URL=jdbc:postgresql://localhost:55432/vagaviva ./mvnw spring-boot:run`
7. **Conferência:** nas *Recent Deliveries* do App de teste, reenvie um evento; o app responde 202. Um 401 é assinatura inválida.
8. **Repositório de teste:** `precavidos/vaga-viva-bateria`, criado do template `vaga-viva-template-java-spring`, com `.vaga-viva/plan.json` no `main`: `{"archetype": "java-spring", "domain": "clinic", "missions": ["bug-hunt", "scheduling-rules", "patients-api"]}`.
9. **Casos:** `scripts/bateria-revisor.sh abrir precavidos/vaga-viva-bateria <clone> [NN...]`, depois `dica` e `coletar` (veja o cabeçalho do script).
