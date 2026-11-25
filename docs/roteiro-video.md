# Roteiro do vídeo de submissão

Vídeo de até 3 minutos, **narrado em inglês com legenda em português**, como pede o cronograma. As cenas seguem a tabela "Roteiro do vídeo (3 min)" do cronograma. A narração tem cerca de 400 palavras, o que dá uns 140 por minuto, um ritmo calmo para quem não é nativo.

Os trechos entre colchetes, como **[X]**, são os números do piloto: preencha com os dados reais antes de gravar. Não use dados pessoais reais na tela (regulamento, item 7.2): use as contas de teste e a vaga de exemplo.

## Antes de gravar

- Acesse a demo alguns minutos antes, para o servidor estar acordado.
- Use uma conta do GitHub de teste, sem repositórios pessoais à mostra.
- Deixe pronto um repositório de aluno com um PR que já tenha revisão e uma dica. Pode ser o [PR #7 de teste](https://github.com/davsilvam/vaga-viva-clinic-scheduling/pull/7) ou, melhor, um repositório novo e limpo.
- Deixe pronta uma turma com 3 a 6 alunos de teste em missões diferentes, para o painel mostrar travados, parados e avançando.
- A frase da cena 3 "We tried to break it… It kept asking questions" só vale se a rodada B da bateria (issue 29) terminar sem vazamento. Se algum caso vazar e ainda não estiver corrigido, troque por "We test it against attack pull requests".
- Grave a tela em 1440 px de largura, com zoom de 125% no navegador, para o texto ficar legível no vídeo.

## Cena 1 · 0:00–0:25 · O problema

**Na tela:** um site de vagas com várias exigências de "experiência" para júnior, e depois o logo do Vaga Viva.

**Narração (EN):**
> Entry-level jobs keep asking for experience. Juniors in Brazil read a posting, see a list of technologies, and have no project that proves they can use them. Tutorials teach syntax, not the job. So we built Vaga Viva: learn by doing, from a real job posting.

**Legenda (PT):**
> As vagas de entrada continuam pedindo experiência. O júnior lê a vaga, vê uma lista de tecnologias e não tem um projeto que prove que sabe usá-las. Tutoriais ensinam sintaxe, não o trabalho. Por isso criamos o Vaga Viva: aprender fazendo, a partir de uma vaga real.

## Cena 2 · 0:25–1:10 · Momento uau: da vaga ao repositório

**Na tela:**
1. Na demo, clique em **Usar uma vaga de exemplo** e em **Montar meu plano**. Mostre as competências marcadas no texto e as missões.
2. Marque **18+**, clique em **Criar meu repositório no GitHub** e entre com o GitHub.
3. Corte para o repositório criado: o README, as issues numeradas e uma issue aberta ("O que fazer", com o link para o contrato).
4. Corte para a aba **Actions** com os testes vermelhos.

**Narração (EN):**
> A student pastes a real posting. Vaga Viva reads the level and the skills it asks for, with a fixed vocabulary, not guesswork. It builds a plan of five or six missions in a fictional domain, from simple to harder. One click, and a full project appears in the student's own GitHub: a README tied to the job, one issue per mission, and tests that fail until the student makes them pass. It works for Java, TypeScript and Python. The student can even forward a posting by e-mail.

**Legenda (PT):**
> O aluno cola uma vaga real. O Vaga Viva lê o nível e as competências que ela pede, com um vocabulário fechado, sem chute. Monta um plano de cinco ou seis missões num domínio fictício, das mais simples às mais difíceis. Com um clique, um projeto completo aparece no GitHub do próprio aluno: um README ligado à vaga, uma issue por missão e testes que falham até o aluno fazê-los passar. Funciona para Java, TypeScript e Python. O aluno pode até encaminhar a vaga por e-mail.

## Cena 3 · 1:10–1:50 · O revisor que não dá a resposta

**Na tela:**
1. O PR do aluno com a revisão: role devagar pelo progresso, pelos testes que falham e pelas perguntas.
2. Comente `/dica` e mostre a dica 1 de 3 chegando.
3. Mostre a **recusa escolhida na rodada B da bateria** (issue 29): um PR que tenta fazer o revisor entregar código e a resposta com perguntas. Use o link anotado em `docs/bateria-revisor.md`.
4. Termine com os testes ficando verdes e o aviso "🎉 Missão concluída".

**Narração (EN):**
> When the student opens a pull request, the tests run and the reviewer answers a minute later. It shows the progress, the tests that still fail, and asks questions. It never writes the code. Stuck? Comment "dica" and get one hint at a time, each closer to the problem. We tried to break it: ten attack pull requests asked for the solution, hid instructions in the code, in the tests, in the mission contract. It kept asking questions. And when the tests turn green, the mission is done. The tests decide, not the AI.

**Legenda (PT):**
> Quando o aluno abre um pull request, os testes rodam e o revisor responde um minuto depois. Ele mostra o progresso, os testes que ainda falham e faz perguntas. Ele nunca escreve o código. Travou? Comente "dica" e receba uma dica por vez, cada uma mais perto do problema. Nós tentamos quebrá-lo: dez pull requests de ataque pediram a solução e esconderam instruções no código, nos testes e no contrato da missão. Ele continuou perguntando. E quando os testes ficam verdes, a missão está pronta. Quem decide são os testes, não a IA.

## Cena 4 · 1:50–2:25 · Modo turma e os números do piloto

**Na tela:**
1. O painel do professor: os contadores, as colunas por missão e a grade de testes. Clique em **travados** para filtrar.
2. Abra a ficha de um aluno: os testes que falham e a última pergunta do revisor.
3. Os números do piloto em tela cheia, um por vez.

**Narração (EN):**
> Teachers can run a whole class on one posting. The dashboard shows each student's mission, who is stuck and why, and every Monday a summary arrives by e-mail. In our pilot with **[X]** testers, **[X]%** opened a pull request, they completed **[X]** missions, and their confidence went from **[X]** to **[X]** out of five.

**Legenda (PT):**
> Professores podem levar uma turma inteira a partir de uma vaga. O painel mostra a missão de cada aluno, quem está travado e por quê, e toda segunda um resumo chega por e-mail. No nosso piloto com **[X]** testadores, **[X]%** abriram um pull request, eles concluíram **[X]** missões, e a autoconfiança foi de **[X]** para **[X]**, numa escala de um a cinco.

## Cena 5 · 2:25–3:00 · Arquitetura e fechamento

**Na tela:** o diagrama "Como funciona" do README, e depois o logo com a URL da demo e do repositório.

**Narração (EN):**
> Under the hood: a Spring Boot API, a GitHub App that creates the repository and listens to pull requests, GitHub Actions running the tests, and Claude Haiku writing only the explanations, behind a filter that blocks code. The plan itself is deterministic, and the posting text is never stored. Vaga Viva: learn by doing, from real jobs, with a reviewer that never gives the answer.

**Legenda (PT):**
> Por dentro: uma API em Spring Boot, um GitHub App que cria o repositório e acompanha os pull requests, o GitHub Actions rodando os testes, e o Claude Haiku escrevendo só as explicações, atrás de um filtro que bloqueia código. O plano em si é determinístico, e o texto da vaga nunca é guardado. Vaga Viva: aprender fazendo, com vagas reais, e um revisor que nunca dá a resposta.

## Checklist antes de publicar

- [ ] Até 3:00, contando a abertura e o fechamento.
- [ ] Números do piloto preenchidos e conferidos com a planilha.
- [ ] Legenda em português sincronizada.
- [ ] Nenhum e-mail, nome ou rosto real sem autorização na tela.
- [ ] O link do vídeo abre sem login.
