# Direção B: marca amadurecida e linguagem de pontos — design

**Data:** 13/11/2026
**Depende de:** o modo turma (`docs/superpowers/specs/2026-11-11-classroom-mode-design.md`), já no ramo `feat/classroom-mode`.
**Mockups de referência:** `.superpowers/brainstorm/1361-1794598134/content/` (`direcao-visual.html`, opção B; `pontos.html`; `reactbits-inspiracoes.html`). Eles são rascunhos de conversa, não código para copiar.

## Por que

A interface atual parece infantil e tem padrões com cara de gerada por IA. O objetivo é que o Vaga Viva pareça uma ferramenta séria de desenvolvimento e impressione na apresentação do Hack Brasil, sem perder a identidade que já existe.

O que deixa a interface infantil hoje:
- selos coloridos, bolinhas e balão de "?" dentro do título da landing;
- rabiscos, carimbos flutuantes e ziguezagues;
- cantos de 28 a 36 px, botões em pílula, ícones em círculos coloridos e animações com mola;
- as quatro cores de marca juntas em toda tela e Bricolage 800 em todo título.

O que tem cara de IA: rótulo com bolinha acima de cada título, listas de três itens com ícone, frases de efeito como título e a parede "Feito com" com 14 selos de tecnologia.

O que já é forte e fica: a demonstração da vaga virando plano e repositório, o painel com o motivo de cada situação, o tom "avisa, não avalia" e a privacidade explícita.

## Objetivos

1. **Direção B, "ferramenta de dev".** Mesma paleta (papel, tinta, roxo, amarelo, verde, coral), mas com cantos de 4 a 8 px, títulos em peso 600, roxo como cor do botão principal e painéis escuros para tudo que representa código, GitHub ou o sistema trabalhando.
2. **Uma linguagem de pontos como sistema visual.** O ponto já é do produto: cada teste é uma bolinha que passa de coral para verde. Carregamentos, progresso, grade de testes e transições passam a ser feitos de pontos.
3. **Seis componentes de movimento**, inspirados no React Bits e no Matrix dot loader da transitions.dev, reescritos em HTML, CSS e JS puros (item "Componentes de movimento").
4. **Grade de testes no cartão do aluno:** uma linha por missão, um ponto por teste, verde passando, coral falhando, vazio ainda não rodado. Isso exige o back-end contar os testes de cada missão.
5. **Ordem de trabalho: referência primeiro.** O `DESIGN.md` e os protótipos em `docs/design/prototipos/` são atualizados e aprovados antes das páginas reais (`src/main/resources/static/`) e da landing (`site/index.html`).

## Fora do escopo

- Trocar a paleta, as fontes ou o nome da marca.
- React, three.js, qualquer etapa de build ou dependência de npm na interface.
- O crachá 3D (Lanyard) e efeitos sem significado: cursores, auroras, vidro, 3D, cartões inclinados, brilho neon.
- Grade de dias no estilo do gráfico de contribuições do GitHub.
- O e-mail semanal (`email.html`) muda só nos tokens de cor e raio, não ganha pontos nem animação.
- Novas telas ou fluxos. As telas e o que cada uma faz continuam como no `DESIGN.md` seção 5.

## O que seria um fracasso, mesmo funcionando

- A interface trocar o "infantil" por "template de SaaS genérico": fundo escuro com degradê roxo, vidro, brilhos.
- Animação que não vem de dado real ou de espera real (o `DESIGN.md` seção 9 continua valendo).
- Movimento em tela de trabalho que distrai o professor de ler o painel.
- A grade de testes virar ranking ou nota: ordenar alunos pela quantidade de pontos verdes, somar pontos da turma como placar.
- A marca rachar entre a landing e as telas: a demo ao vivo precisa parecer o mesmo produto.

## Restrições

- Sem framework e sem build, como manda o `DESIGN.md` seção 8. Os componentes vivem num arquivo próprio, usado pelos protótipos, pelas páginas reais e pela landing.
- A landing (`site/index.html`) é publicada sozinha, com o CSS dentro do arquivo. Ela recebe uma cópia dos tokens e dos componentes que usa, marcada com um comentário de origem.
- Acessibilidade do `DESIGN.md` seção 7: contraste, foco, teclado, `prefers-reduced-motion` e `prefers-color-scheme`. Com movimento reduzido, todos os componentes mostram o estado final, parado.
- Desempenho: no máximo um `<canvas>` animado por página (o fundo de pontos da landing). O resto é CSS com `transform` e `opacity`.
- Um laço infinito só existe durante espera real (`DESIGN.md` 3.2). As demos da landing são a exceção: são ilustrativas por natureza e param quando saem da tela.
- A regra da skill design-taste-frontend que proíbe roxo **não** se aplica: o roxo é a marca do Vaga Viva. Da skill usamos a lista de padrões genéricos, a revisão de texto e a checagem de layout no navegador.
- Calendário: a apresentação é no fim de novembro. A repaginação e a grade de testes podem chegar depois do congelamento de funcionalidades do modo turma (25/11); tudo precisa estar pronto e verificado antes da apresentação. As páginas reais só mudam depois que o David aprovar os protótipos.

## Abordagens consideradas

1. **Referência primeiro, depois páginas reais (escolhida).** Atualiza `DESIGN.md` e protótipos, depois leva para as páginas reais e a landing, com uma camada única de componentes. Mantém o `DESIGN.md` como fonte da verdade, ao custo de passar duas vezes pelas telas.
2. **Direto nas páginas reais.** Mais rápido, mas os protótipos ficariam desatualizados e decisões tomadas no caminho não passariam pela referência.
3. **React com os componentes originais do React Bits.** Descartada: exigiria uma etapa de build num projeto de HTML puro, e os seis efeitos funcionam sem React.

O David escolheu a 1.

## O que foi decidido

### Fundamentos da direção B

| Item | Hoje | Direção B |
| --- | --- | --- |
| Raios | 12 / 16 / 28 / 36 / 999 px | 4 (chips, pontos quadrados), 6 (botões, campos), 8 (cartões, painéis), 12 (bloco principal da tela). Selos de situação com 4 px; pílula só no campo de cópia do convite. |
| Títulos | Bricolage 800, `h1` até 56 px | Bricolage 600 em `h1` e `h2`, 600 em `h3`. `h1` até 48 px, espaçamento -0.025em. |
| Botão principal | tinta, pílula, sombra dura roxa no hover | roxo `--purple`, texto branco, 6 px de raio. Hover escurece 6% e sobe 1 px; sem sombra dura. Continua um só por tela. |
| Botão secundário | vazado, borda 2 px tinta | vazado, borda 1 px `--line`, texto tinta. Hover: borda tinta. |
| Sombra | repouso + sombra dura colorida | só a sombra de repouso, no bloco principal e na gaveta. A sombra dura sai do sistema. |
| Cor por tela | as quatro em blocos chapados | roxo para ação e foco; amarelo só no marca-texto e na varredura; verde e coral só em estado de teste e situação. Nada de blocos chapados grandes em amarelo, verde ou coral. |
| Painéis escuros | só repositório e terminal | tudo que é código, GitHub, demo do produto ou sistema trabalhando (carregamentos, criação do repositório, revisor). |
| Fichas de ícone | ícone em círculo de cor chapada | ícone em quadrado de 28 px com borda 1 px `--line` e raio 6 px, sem cor de fundo. A cor fica no ícone só quando é situação. |
| Avatar | círculo com cor aleatória | círculo neutro (`--paper-2`), iniciais em mono 600. |
| Motivos gráficos | bolinhas, marca-texto, recorte inclinado, ziguezague, pílula | ficam bolinhas (agora o centro do sistema) e marca-texto. O recorte perde a inclinação e fica com borda tracejada. Ziguezague, rabiscos e carimbos saem. |
| Ease | `--ease-spring` para estouros | sai a mola. Tudo em `--ease-out`. O "estouro" vira escala 0.92 → 1 em `--t-base`. |

Ponto quadrado ou redondo: **redondo** para testes e carregamentos (é a bolinha de teste); **quadrado de 4 px de raio** só nas grades densas (cartão do aluno e linhas da turma), para ler como grade.

### Componentes de movimento

Todos ficam num arquivo único, `vv-motion.js` mais `vv-motion.css`, ao lado de `vv.js` e `vv.css`. Cada componente é ativado por atributo `data-` no HTML, sem build.

| # | Componente | Inspiração | Onde aparece | Regra |
| --- | --- | --- | --- | --- |
| 1 | **Fundo de pontos** (`data-dot-field`) | Dot Grid / Dot Field | só no topo da landing | Grade de pontos num `<canvas>`. Perto do cursor, os pontos se afastam alguns pixels e ficam verdes, como testes passando, e voltam devagar. Em toque ou movimento reduzido, fica parado. Pausa fora da tela. |
| 2 | **Matriz de pontos** (`data-dot-matrix="scan|wave|flip"`) e **linha de status** | Matrix dot loader + Lattice Loader | toda espera real | Matriz 4×4 de pontos. Cada padrão é uma etapa: `scan` (varredura em amarelo) para ler a vaga, `wave` (onda diagonal em roxo) para montar o plano e para o revisor, `flip` (coral vira verde, um por vez) para criar o repositório. A linha de status junta matriz, rótulo e cronômetro em mono ("Montando o plano · 2,3 s"). Substitui as três bolinhas do botão carregando e o esqueleto com varredura. |
| 3 | **Termo vira competência** (`data-decrypt`) | Decrypted Text | passo 3 da extração (`DESIGN.md` 3.1) e demo da landing | O nome da competência se resolve caractere por caractere a partir de símbolos, da esquerda para a direita, em até 600 ms. Só com nomes que a API devolveu. Leitores de tela recebem o texto final direto. |
| 4 | **Marca de etapa** (`.step-mark`) | Status Mark | criação do repositório, entrada na turma | Anel parado (futuro), arco girando (atual), visto verde desenhado (feito). Substitui a "virada do teste" das etapas. A regra de `DESIGN.md` 3.2 continua: a última etapa só fica verde quando o servidor responde. |
| 5 | **Troca em pontos** (`data-dot-swap`) | Pixel Transition | da etapa Plano para a etapa Repositório (aluno e convite) | Uma malha de pontos roxos cobre o bloco em ordem aleatória, o conteúdo troca por baixo e a malha some. Cerca de 500 ms no total. Uma única vez por fluxo. |
| 6 | **Contagem** (`data-count-up`) | Count Up | resumo do painel | Já existe no `DESIGN.md`. Muda só a aparência: Bricolage 600, ponto da situação ao lado do rótulo, sem ficha de ícone. Uma vez por carregamento. |

### Grade de testes

**No cartão do aluno (painel):** abaixo do nome e do selo, uma linha por missão do plano. Cada linha tem o número da missão em mono, um ponto quadrado por teste e, à direita, "passando/total" ("3/4"). A frase de motivo fica embaixo, como hoje.

Cor dos pontos (decisão de design, não do builder):
- Missões **antes** da atual e a **atual**: verde para teste passando, coral para teste falhando.
- Missões **depois** da atual: todos os pontos em `--line`, vazios, mesmo que os testes delas falhem no relatório. Como a suíte inteira roda a cada push, os testes das missões futuras sempre falham; pintá-los de coral deixaria todo cartão alarmante sem informar nada. A contagem à direita mostra "—".
- Aluno **concluído**: tudo verde.
- `total` é a quantidade de testes da missão que rodaram no último relatório (sem `skipped`). Não é preciso guardar a contagem esperada no catálogo de missões.

**Nas linhas da turma (topo do painel, opcional):** a mesma grade, uma linha por aluno, em ordem alfabética do nome. Nunca do pior para o melhor, nunca com soma ou total da turma.

**Na ficha (gaveta):** a lista de testes que falham continua igual, com a grade da missão atual no topo.

**Na landing:** a grade aparece na demo do painel, com dados fictícios.

### Dados para a grade (back-end)

Hoje `JunitReports` lê cada `<testcase>` com o prefixo `Missão <id>:` e devolve só as missões que rodaram e os testes que falharam. A mudança:

1. `TestReport` passa a trazer, por missão, quantos testes rodaram e quantos passaram (testes `skipped` não contam, como hoje).
2. O progresso do aluno guarda essa contagem por missão, numa coluna `jsonb` nova da tabela de progresso, por migração Flyway `V3`. Cada relatório novo substitui a contagem das missões que rodaram.
3. O `Card` e o `StudentCard` do painel ganham a lista `tests` com `missionId`, `passed` e `total`, na ordem do plano.
4. Missão sem contagem (aluno que ainda não fez push depois da mudança, ou missão que não rodou) vem sem total. A interface mostra a linha só com o número da missão e "—", sem pontos.
5. O relatório parcial (`MissionProgress.Unknown`) continua sem mudar a missão atual, mas atualiza a contagem das missões que rodaram.

## Ordem de entrega

1. **Referência:** `DESIGN.md` reescrito para a direção B (seções 1, 2, 3, 4 e 9; as seções 5 a 8 mudam só onde citam componentes) e `docs/design/prototipos/` atualizados, incluindo `vv.css`, `vv.js` e os novos `vv-motion.*`. O David aprova vendo os protótipos.
2. **Back-end da grade de testes**, com testes, em paralelo ao passo 1 (não depende dele).
3. **Páginas reais** em `src/main/resources/static/`: tokens, componentes e telas, ligando a grade aos dados reais.
4. **Landing**: novo topo com o fundo de pontos e, ao lado do título, a demo da vaga virando plano (recorte com marcas, competências decifradas, primeiras missões), que é o momento principal do produto; a demo do painel fica numa seção abaixo, título sem enfeites, as demos da vaga, do plano, do repositório e do painel refeitas com os componentes. Corte de rótulos com bolinha, listas de três itens com ícone e da parede de selos.
5. **Revisão final** com a skill design-taste-frontend: lista de padrões genéricos, revisão de texto e checagem no navegador em 1440, 1024, 768 e 390 px, nos dois modos de cor.

## O que fica para quem constrói

- Os valores exatos de cor nos estados de hover e as variantes de modo escuro dos novos tokens, desde que passem no contraste.
- A forma interna de `vv-motion.js`, desde que cada componente funcione só com atributos no HTML e respeite movimento reduzido.
- O texto novo da landing, dentro da voz do `DESIGN.md` seção 6 e sem os padrões da seção 9.
- Se as linhas da turma no topo do painel entram ou não, conforme o espaço com sete colunas.
- O nome exato da coluna nova e do campo no JSON da API.
