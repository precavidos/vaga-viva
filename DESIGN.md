# Design do Vaga Viva

Este documento é a fonte única das decisões visuais e de interação do produto: página do aluno, área do professor, convite da turma, painel, e-mail e landing page (`site/index.html`). A direção visual é a **direção B, "ferramenta de dev"**, com uma **linguagem de pontos** como sistema visual. A decisão e o porquê estão em `docs/historic/specs/2026-11-13-direcao-b-pontos-design.md`.

Os protótipos navegáveis ficam em `docs/design/prototipos/`. Eles usam dados fictícios e implementam tudo o que está escrito aqui. O CSS de referência é `docs/design/prototipos/vv.css`, e os componentes de movimento ficam em `vv-motion.css` e `vv-motion.js`, ao lado dele.

## 1. Princípios

1. **Ferramenta de dev, não brinquedo nem template.** Cantos pequenos, títulos em peso 600, painéis escuros para tudo que é código, GitHub ou o sistema trabalhando. O que sai do infantil não pode virar template de SaaS genérico: nada de fundo escuro com degradê roxo, vidro ou brilho.
2. **Papel, tinta e cor com função.** O fundo é papel quente e o texto é tinta quase preta. Roxo é ação e foco; amarelo é só o marca-texto e a varredura; verde e coral são só estado de teste e situação (o coral também marca erro e o que a vaga cita fora do projeto). Não há degradês, sombras coloridas nem blocos chapados grandes em amarelo, verde ou coral.
3. **O ponto é o sistema.** Cada teste é um ponto que passa de coral para verde. Carregamentos, progresso, grade de testes e transições são feitos do mesmo ponto, e o marca-texto mostra o que foi lido na vaga.
4. **Uma ação principal por tela.** Cada tela tem um único botão roxo. O resto é botão vazado ou link.
5. **O sistema avisa, não avalia.** "Travado" e "parado" são avisos para o professor conversar com o aluno. Eles sempre vêm com o motivo e nunca com tom de nota, ranking ou alarme. A grade de testes mostra onde o aluno está, não quanto ele vale.
6. **Movimento explica.** Uma animação existe para mostrar de onde algo veio, o que mudou ou que o sistema está trabalhando. Ela vem de dado real ou de espera real. Nada se mexe só para enfeitar uma tela de trabalho.
7. **O aluno sabe o que é visto.** Onde há acompanhamento, a tela diz o que o professor vê e o que não vê, antes de qualquer ação.

## 2. Fundamentos

A direção B em uma tabela. Os itens abaixo detalham cada linha.

| Item | Regra |
| --- | --- |
| Raios | 4 px (chips, selos, pontos quadrados), 6 px (botões, campos), 8 px (cartões, painéis), 12 px (bloco principal da tela). Pílula só no campo de cópia do convite. |
| Títulos | Bricolage 600 em `h1`, `h2` e `h3`. `h1` até 48 px, espaçamento -0.025em. |
| Botão principal | roxo `--purple`, texto branco, raio 6 px. Hover escurece 6% e sobe 1 px. Um só por tela. |
| Botão secundário | vazado, borda 1 px `--line`, texto tinta. Hover: borda tinta. |
| Sombra | só a sombra de repouso, no bloco principal e na gaveta. |
| Cor por tela | roxo para ação e foco; amarelo só no marca-texto e na varredura; verde e coral só em estado de teste e situação, e o coral também em erro e no que fica fora do projeto. Nada de blocos chapados grandes em amarelo, verde ou coral. |
| Painéis escuros | tudo que é código, GitHub, demo do produto ou sistema trabalhando (carregamentos, criação do repositório, revisor). |
| Fichas de ícone | ícone em quadrado de 28 px, borda 1 px `--line`, raio 6 px, sem cor de fundo. O ícone só leva cor quando é situação. |
| Avatar | círculo neutro (`--paper-2`), iniciais em mono 600. |
| Motivos gráficos | pontos (o centro do sistema) e marca-texto. O recorte da vaga tem borda tracejada e não inclina. |
| Ease | tudo em `--ease-out`. O surgimento de selos e pontos é escala 0.92 → 1 em `--t-base`. |

### 2.1 Cor

Os valores são tokens CSS em `:root`. O modo escuro troca os tokens neutros; as quatro cores de marca não mudam.

| Token | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| `--paper` | `#f4f1ea` | `#161513` | fundo da página |
| `--paper-2` | `#ebe7dc` | `#201e1b` | fundo de áreas rebaixadas, trilhos, avatar |
| `--card` | `#ffffff` | `#1f1d1a` | superfícies |
| `--ink` | `#141414` | `#f4f1ea` | texto e bordas fortes |
| `--ink-2` | `#4a4740` | `#b4afa3` | texto secundário |
| `--line` | `#dcd7ca` | `#34312c` | bordas finas |
| `--purple` | `#5b3df5` | igual | botão principal, foco, revisor, concluído |
| `--purple-soft` | `#e6e0ff` | `#2c2266` | fundos do revisor e de seleção |
| `--focus` | `#5b3df5` (igual a `--purple`) | `#8a75ff` | contorno de foco e borda do campo em foco; no escuro, clareado para ter pelo menos 3:1 sobre `--card` e `--paper` |
| `--yellow` | `#ffd23f` | igual | marca-texto e varredura |
| `--green` | `#33c47a` | igual | teste passando, avançando |
| `--coral` | `#ff6b4a` | igual | teste falhando, travado, erro |
| `--green-ink` | `#0e6b3c` | `#5ee39c` | texto verde sobre papel |
| `--coral-ink` | `#b3341a` | `#ff9a82` | texto coral sobre papel |
| `--idle` | `#8b8678` | `#8b8678` | parado |
| `--dot-empty` | `#8f8a7c` | `#767166` | ponto de teste vazio (missão futura) |
| `--night`, `--night-2`, `--night-3` | `#121212`, `#1e1e1e`, `#2a2a2a` | `#0b0b0a`, `#1a1917`, `#26241f` | painéis escuros (código, GitHub, demos, sistema trabalhando) |

Regras:
- Sobre amarelo, verde, coral ou o cinza de parado (`--idle`), o texto é sempre `#141414`, nos dois modos. Sobre roxo, o texto é sempre branco.
- Texto colorido sobre papel usa as variantes `-ink`. Nunca use `--green` ou `--coral` como cor de texto pequeno.
- Cor nunca é o único sinal. Toda situação tem também um ícone e uma palavra, e toda linha da grade de testes tem a contagem em texto.
- `--dot-empty` tem pelo menos 3:1 sobre `--card` e `--paper` nos dois modos. O ponto vazio é desenhado só com contorno nessa cor; `--line` é claro demais para um ponto que precisa ser visto.

### 2.2 Situações do aluno

| Situação | Cor | Ícone | Rótulo | Frase de motivo |
| --- | --- | --- | --- | --- |
| `STUCK` | `--coral` | seta em volta | travado | "4 dias na missão 2, 3 PRs nesse período" |
| `IDLE` | `--idle` | lua | parado | "sem atividade há 6 dias" |
| `ADVANCING` | `--green` | seta para cima | avançando | "há 2 dias na missão" |
| `COMPLETED` | `--purple` | visto | concluído | "todas as missões passando" |

O rótulo é o nome da situação e não flexiona por gênero, porque o sistema não guarda esse dado; por isso ele aparece sempre como selo, em maiúsculas mono, e nunca dentro de uma frase sobre a pessoa.

No cartão do aluno, a situação aparece como uma faixa de 6 px à esquerda, um selo de 4 px de raio com ícone e rótulo, a grade de testes (item 4.16) e a frase de motivo. O painel não usa vermelho piscando, contagem regressiva nem ordenação "do pior para o melhor".

### 2.3 Tipografia

| Papel | Fonte | Peso | Tamanho |
| --- | --- | --- | --- |
| Título de página (`h1`) | Bricolage Grotesque | 600 | `clamp(34px, 5vw, 48px)`, entrelinha 1.04, espaçamento -0.025em |
| Título de seção (`h2`) | Bricolage Grotesque | 600 | 28 px |
| Título de cartão (`h3`) | Bricolage Grotesque | 600 | 19 px |
| Corpo | DM Sans | 400 e 500 | 16 px, entrelinha 1.55 |
| Rótulo de campo, ênfase | DM Sans | 600 | 14 a 16 px |
| Botão | DM Sans | 700 | 16 px (14 px no `.sm`) |
| Texto de apoio | DM Sans | 400 | 14 px |
| Rótulo, número, código, nome de teste, cronômetro | JetBrains Mono | 600 | 12 a 13 px, maiúsculas com espaçamento 0.08em nos rótulos |

- Os números dos contadores usam Bricolage 600 com `font-variant-numeric: tabular-nums`.
- Títulos em frases curtas, com ponto final quando forem afirmações ("Sua turma, de relance."). Título não leva selo, bolinha nem ícone dentro.

### 2.4 Espaço, forma e elevação

- **Escala de espaço:** 4, 8, 12, 16, 24, 32, 48, 72 px.
- **Largura:** `.wrap` de 1160 px como padrão, 760 px de conteúdo (`.wrap.narrow`) para fluxos de formulário e 1440 px (`.wrap.wide`) para o painel, onde cabem as sete colunas.
- **Raios:** 4 px (chips, selos, pontos quadrados), 6 px (botões, campos, fichas de ícone, itens de lista), 8 px (cartões, colunas, painéis), 12 px (bloco principal da tela). A gaveta não tem raio quando é painel lateral; no celular, vira folha de baixo com 12 px nos cantos de cima. A pílula só existe no campo de cópia do convite.
- **Bordas:** 1 px `--line` em superfícies e no botão vazado; o foco é roxo (seção 7).
- **Elevação tem um nível só:** a sombra de repouso, `0 30px 60px -30px rgba(20,20,20,.25)`, no bloco principal da tela e na gaveta. Nada mais tem sombra.

### 2.5 Iconografia

O produto tem uma família própria de ícones, desenhada para ele. A fonte é o mapa `ICON` de `vv.js`.

- **Desenho:** grade de 24 px, traço de 2,2 px na cor do texto, pontas e junções redondas, sem preenchimento. Formas geométricas simples, com no máximo três traços por ícone.
- **Ficha de ícone (`.ic`):** quadrado de 28 px, borda 1 px `--line`, raio 6 px, sem cor de fundo. O ícone vai em tinta. Só quando representa uma situação ele leva a cor dela (`--coral-ink`, `--idle`, `--green-ink` ou `--purple`).
- **Vocabulário:** cada conceito tem um único ícone, sempre o mesmo.

| Conceito | Ícone | Conceito | Ícone |
| --- | --- | --- | --- |
| vaga | folha com canto dobrado | recorte da vaga | tesoura |
| nível | degraus | tecnologias | camadas |
| competência | porca sextavada | missão | bandeira |
| testes | frasco | pergunta do revisor | balão com interrogação |
| dica | lâmpada | repositório | livro |
| turma, alunos | duas pessoas | painel | três colunas |
| e-mail | envelope | tempo | relógio |
| travado | seta em volta | parado | lua |
| avançando | seta para cima | concluído | visto |

- **Onde não usar:** ícone não substitui texto em botão de ação nem em rótulo de campo, e não entra só para decorar um título. Sem ícones de brilho, estrela ou varinha para "inteligência": o produto mostra o que leu, e não que é mágico.
- **Marcas de terceiros:** só o logotipo do GitHub, nos botões e painéis que levam ao GitHub.

### 2.6 Motivos gráficos

- **Pontos:** o centro do sistema. **Redondos**, de 10 a 18 px, para testes e carregamentos: coral para falhando, verde para passando. **Quadrados com 4 px de raio** só nas grades densas (cartão do aluno e linhas da turma), para ler como grade.
- **Marca-texto:** faixa que se desenha da esquerda para a direita atrás de um trecho. Amarela para tecnologias, lilás para o nível e coral claro para o que fica fora do projeto.
- **Recorte:** a vaga aparece como um recorte de jornal: papel claro nos dois modos, borda tracejada de corte e uma tesoura no rótulo. Não inclina.
- **Selo mono:** retângulo em `--ink` de 4 px de raio (preto no modo claro, claro no escuro, com o texto em `--paper`) com conteúdo mono, para número de missão, código de convite e nome de repositório.

## 3. Movimento

| Token | Valor | Uso |
| --- | --- | --- |
| `--t-fast` | 150 ms | hover, foco, pressionar |
| `--t-base` | 280 ms | entrada de itens, troca de estado, surgimento |
| `--t-slow` | 520 ms | gaveta, marca-texto |
| `--ease-out` | `cubic-bezier(.2, .8, .2, 1)` | todo movimento |

Padrões:

| Nome | O que faz | Onde |
| --- | --- | --- |
| **Subir** (`rise`) | opacidade 0 → 1 e 10 px para cima, `--t-base` | qualquer bloco que aparece |
| **Cascata** (`stagger`) | filhos sobem com 60 ms de atraso entre si, até 8 itens | missões do plano, cartões do painel |
| **Surgir** (`pop`) | escala 0.92 → 1 e opacidade 0 → 1, `--t-base` | selos, pontos, visto de sucesso |
| **Marca-texto** (`marker`) | a faixa cresce de 0 a 100% em `--t-slow` | termo reconhecido, título do sucesso |
| **Extração** | a sequência do item 3.1 | leitura da vaga |
| **Gaveta** | desliza 100% → 0 em `--t-slow`; o fundo escurece para 45% | ficha do aluno |
| **Hover** | botões sobem 1 px em `--t-fast`; o primário escurece 6%, o vazado ganha borda tinta; cartões clicáveis não sobem, só ganham borda tinta | botões e cartões clicáveis |
| **Pressionar** | volta ao lugar | `:active` |

Os seis componentes de movimento estão no item 3.3.

### 3.1 A extração da vaga

É o momento principal do produto: o aluno vê o que foi lido na vaga dele. A animação mostra dados reais e nada além deles. Cada palavra marcada é um termo que `POST /api/job-postings/extraction` devolveu (`matchedTerms` e `evidence`). O que o sistema não reconheceu fica sem marca.

Enquanto a resposta não chega, o botão mostra a linha de status em `scan` ("Lendo a vaga · 1,2 s"). Depois, a sequência dura cerca de 4 segundos:

1. **Varredura (0,9 s).** O campo vira o recorte da vaga, à esquerda, e uma linha de leitura amarela desce por ele uma vez.
2. **Palavras (0,4 s cada, sobrepostas).** Na ordem do texto, cada termo reconhecido ganha o marca-texto. Na primeira vez que aparece, a palavra se solta do recorte, faz um arco com leve giro e pousa como chip no seu grupo, à direita: Nível, Tecnologias ou Fora deste projeto. A próxima palavra se acende enquanto a anterior ainda está no ar. O título conta: "3 termos reconhecidos".
3. **Competências (uma a cada 0,13 s).** Para cada competência, o chip do termo que a evidencia pulsa, o chip da competência surge no grupo e o nome dela se decifra (`data-decrypt`, item 3.3), dizendo de onde veio: "Programação em Java, por 'Java'".
4. **Plano.** O título vira "O que a vaga pede", e as missões entram em cascata abaixo.

Regras:
- **"Pular a animação"** fica visível durante toda a sequência e leva direto ao estado final.
- Com `prefers-reduced-motion`, a tela já abre no estado final, com todas as marcas e chips.
- O recorte fica fixo ao lado do plano em telas largas, para o aluno conferir cada marca. Em telas estreitas, ele fica acima, com altura limitada e rolagem própria.
- Uma região `role="status"` anuncia só o resultado ("Leitura concluída: nível júnior, 5 tecnologias e 5 competências"), e não cada palavra.
- O que a vaga cita e o projeto não pratica entra no grupo "Fora deste projeto", com contorno coral e uma frase de explicação. Isso substitui o aviso amarelo de compatibilidade.

### 3.2 Regras gerais

- Só `transform` e `opacity` são animados, além de `background-size` no marca-texto e do traço do visto desenhado.
- No máximo um `<canvas>` animado por página: o fundo de pontos da landing. O resto é CSS.
- Nada entra em laço infinito, exceto a matriz de pontos e o arco da marca de etapa enquanto houver espera real. As demos da landing são a exceção: são ilustrativas por natureza e param quando saem da tela.
- Com `prefers-reduced-motion: reduce`, todas as animações e transições caem para 0.01 ms e todo componente mostra o estado final, parado: a contagem mostra o número final, o texto decifrado aparece pronto, a troca em pontos troca direto, a matriz fica parada ao lado do rótulo e a gaveta abre sem deslizar.
- Em tela de trabalho, nada se mexe enquanto o professor lê o painel. A contagem roda uma vez por carregamento e para.
- As etapas da criação do repositório avançam por tempo e são ilustrativas. A última só é marcada quando o servidor responde; se a resposta demorar, a penúltima fica em espera.

### 3.3 Componentes de movimento

Os seis componentes vivem em `vv-motion.css` e `vv-motion.js`. Cada um é ativado por um atributo ou uma classe no HTML, sem build. Os que dependem de um momento da tela (a espera que começa, a troca de etapa) têm também uma chamada curta em `VVMotion`, sem opções de configuração.

| # | Componente | Onde aparece | Regra |
| --- | --- | --- | --- |
| 1 | **Fundo de pontos** (`data-dot-field`) | só no topo da landing | Grade de pontos num `<canvas>`. Perto do cursor, os pontos se afastam alguns pixels e ficam verdes, como testes passando, e voltam devagar. Em toque ou movimento reduzido, fica parado. Pausa fora da tela. |
| 2 | **Matriz de pontos** (`data-dot-matrix="scan\|wave\|flip"`) e **linha de status** (`.status-line`, item 4.14) | toda espera real | Matriz 4×4 de pontos redondos. Cada padrão é uma etapa: `scan` (varredura em amarelo) para ler a vaga e carregar o painel, `wave` (onda diagonal em roxo) para montar o plano e para o revisor, `flip` (coral vira verde, um por vez) para criar o repositório. Substitui as três bolinhas do botão carregando e o esqueleto com varredura. |
| 3 | **Termo vira competência** (`data-decrypt`) | passo 3 da extração (item 3.1) e demo da landing | O nome da competência se resolve caractere por caractere a partir de símbolos, da esquerda para a direita, em até 600 ms. Só com nomes que a API devolveu. Leitores de tela recebem o texto final direto. |
| 4 | **Marca de etapa** (`.step-mark`, item 4.15) | criação do repositório, entrada na turma | Anel parado (futuro), arco girando (atual), visto verde desenhado (feito). A regra do item 3.2 continua: a última etapa só fica verde quando o servidor responde. |
| 5 | **Troca em pontos** (`data-dot-swap`) | da etapa Plano para a etapa Repositório (aluno e convite) | Uma malha de pontos roxos cobre o bloco em ordem aleatória, o conteúdo troca por baixo e a malha some. Cerca de 500 ms no total. Uma única vez por fluxo. |
| 6 | **Contagem** (`data-count-up`) | resumo do painel | O número sobe de 0 ao valor em 600 ms, em Bricolage 600, com o ponto da situação ao lado do rótulo. Uma vez por carregamento. |

Contrato de marcação, o que o HTML escreve e o que `vv-motion.js` faz:

- **`VVMotion.init(root?)`** liga todo atributo `data-` acima dentro de `root` (o documento, se omitido). Pode ser chamada de novo depois de inserir HTML: o que já está ligado não é ligado duas vezes.
- **`data-dot-field`** vai num elemento que envolve o conteúdo do topo. O JS insere o `<canvas>` atrás desse conteúdo; o HTML não escreve o `<canvas>`.
- **`data-dot-matrix="scan|wave|flip"`** é um elemento vazio. O JS injeta os 16 pontos; o valor do atributo escolhe o padrão.
- **`.status-line`** é escrita no HTML com a matriz, `.status-label` e `.status-timer` vazio. Quem atualiza o cronômetro é o JS: `VVMotion.statusLine(el, label)` mostra a linha com o rótulo, começa a contar e devolve `{done(), stop()}`; `VVMotion.statusLine(el, label, since)` conta a partir de `since` (uma leitura de `performance.now()`), para uma espera que começou antes de a linha aparecer; `done()` fecha a espera com sucesso e `stop()` a interrompe (erro ou cancelamento). As duas param o cronômetro e o laço da matriz.
- **`data-decrypt`** resolve para o valor do atributo, ou para o texto do elemento quando o atributo está vazio. O texto final real no DOM é a fonte de verdade: durante a animação, os símbolos ficam ocultos para leitores de tela e uma cópia visualmente oculta guarda o texto final; ao terminar, o elemento traz só o texto final. Leitores de tela recebem o texto final direto, sem `aria-label`. Para um nome que chega depois, `VVMotion.decrypt(el, text)`.
- **`data-dot-swap`** marca o bloco que vai trocar: o próprio elemento é coberto pela malha. A troca não dispara sozinha; ela acontece em `VVMotion.dotSwap(el, swap)`, em que `swap` é a função que troca o conteúdo por baixo da malha.
- **`data-count-up`** lê o número final do valor do atributo (`data-count-up="18"`). O texto do elemento já traz esse número, para quem não roda o JS e para movimento reduzido.
- **`.step-mark`** não tem atributo: o estado é a classe (item 4.15), trocada pela página.

## 4. Componentes

Os nomes são as classes de `vv.css` e, nos itens 4.14 e 4.15, de `vv-motion.css`. A grade de testes (item 4.16) fica em `vv.css`.

### 4.1 Botão (`.btn`)

- **Primário:** fundo `--purple`, texto branco, raio 6 px, 14 × 22 px, DM Sans 700. Hover escurece 6% e sobe 1 px. Um só por tela, inclusive para a ação final de um fluxo ("Criar meu repositório").
- **Vazado (`.ghost`):** transparente, borda 1 px `--line`, texto tinta, raio 6 px. Hover: borda tinta.
- **Seta:** o primário pode levar uma seta branca à direita do texto, que anda 3 px no hover.
- **Carregando:** `aria-busy="true"` e o botão desabilitado. O conteúdo vira a linha de status (item 4.14) com o rótulo no gerúndio ("Lendo a vaga · 1,2 s"). Dentro do primário, os pontos da matriz ficam brancos; o padrão é o da etapa.
- **Alvo mínimo:** 44 px de altura.

### 4.2 Campos (`.field`)

- Rótulo acima, em DM Sans 600. O texto de ajuda fica abaixo do rótulo, em 14 px `--ink-2`.
- Campo com fundo `--card`, borda 1 px `--line`, raio 6 px. No foco: borda `--purple` e o contorno de foco da seção 7.
- Erro: borda coral, mensagem abaixo em `--coral-ink` com ícone, ligada por `aria-describedby`. A mensagem diz como corrigir.
- Contador de caracteres à direita, em mono, quando há limite (nome do aluno: 40).
- **Caixa de marcação (`.check`):** quadrado de 22 px, raio 6 px; marcada fica roxa com o visto desenhado em `--t-fast`.

### 4.3 Passos (`.steps`)

Trilho horizontal com três passos numerados em selos mono de 4 px de raio. O passo atual é em `--ink` (preto no claro, claro no escuro); os feitos levam o visto; os futuros são vazados. Em telas estreitas, só o passo atual mostra o nome.

### 4.4 Cartão de missão (`.mission`)

Linha com: número em selo mono, título (h3), resumo, barras de dificuldade (3 barras, em tinta até o nível e `--line` depois) e um selo de motivo:

| Motivo | Selo | Aparência |
| --- | --- | --- |
| `CORE` | central | tinta |
| `SKILL_MATCH` | pela vaga | roxo suave |
| `DEFAULT` | padrão | neutro, vazado |

As competências que casaram com a vaga aparecem como chips abaixo do resumo.

### 4.5 Aviso (`.banner`)

Faixa com ícone, título curto e texto, raio 8 px. Variantes: `info` (fundo roxo suave), `warn` e `error` (fundo `--card`, borda `--line` e uma faixa de 4 px à esquerda, tinta no `warn` e coral no `error`). Usada para erros de rede e o consentimento. Não há aviso chapado em amarelo ou coral.

### 4.6 Painel escuro (`.night-panel`)

Bloco `--night-2` de 8 px de raio para tudo que é código, GitHub, demo do produto ou sistema trabalhando: nome do repositório em mono, lista de issues com pontos de teste, trecho de terminal, criação do repositório e espera do revisor.

### 4.7 Campo de cópia (`.copy`)

A única pílula do sistema, com o link em mono e um botão "Copiar". Ao copiar, o botão vira "Copiado" com visto por 2 s, e um `aria-live` anuncia.

### 4.8 Resumo da turma (`.summary`, `.stat`)

Um único cartão com duas partes. Em cima, uma barra dividida na proporção das situações, que cresce da esquerda ao carregar. Embaixo, quatro contadores (`data-count-up`), cada um com o número em Bricolage 600 e o rótulo com o ponto da situação ao lado, sem ficha de ícone. O contador é também um filtro: `aria-pressed="true"` deixa o bloco com borda 2 px `--ink` e esmaece os cartões das outras situações. Clicar de novo limpa o filtro.

### 4.9 Coluna e cartão do aluno (`.column`, `.student`)

- **Coluna:** cabeçalho com o número da missão em selo, o título em 18 px e a quantidade de alunos; fundo `--paper-2`, raio 8 px, 20 px de respiro por dentro e 16 px entre colunas. Uma coluna com alunos tem pelo menos 280 px de largura e 340 px de altura.
- **Coluna vazia:** encolhe para um trilho de 76 px, com o número e o título na vertical. Assim, o espaço vai para as missões onde há alunos. Não há texto de erro nem traço.
- **Cartão:** é um `<button>` com 18 px de respiro e raio 8 px. Mostra as iniciais no avatar de 44 px, o nome em Bricolage 18 px, o selo de situação logo abaixo do nome, a grade de testes (item 4.16) e, separada por uma linha fina, a frase de motivo em 14,5 px. Hover: borda tinta. Entre cartões, 12 px.
- **Densidade:** o quadro é para ler de relance, não para caber tudo. Na dúvida entre apertar e rolar, o quadro rola.
- **Faixa "Concluído":** fica abaixo do quadro, em largura total, com fundo `--purple-soft` e os cartões lado a lado. Quem concluiu é quem menos precisa de atenção, e a faixa libera uma coluna inteira para as missões. Sem ninguém concluído, a faixa não aparece.

### 4.10 Gaveta da ficha (`.drawer`)

`<dialog>` modal ancorado à direita, com 480 px de largura; abaixo de 760 px vira uma folha que sobe de baixo e ocupa 92% da altura. Contém:

1. Cabeçalho: iniciais, nome, login em mono, selo de situação e o botão de fechar.
2. Missão atual e frase de motivo.
3. **Testes que falham:** no topo, a linha da grade de testes da missão atual (item 4.16); abaixo, a lista com ponto coral, nome do teste em mono e mensagem.
4. **Última pergunta do revisor:** balão em `--purple-soft` com o avatar "?" do revisor.
5. **Dicas pedidas:** três pontos (preenchidos até a quantidade) e o texto "2 de 3 nesta missão".
6. Rodapé com "Abrir o PR no GitHub" e "Abrir o repositório".

`Esc` e o clique no fundo fecham. O foco entra no botão de fechar e volta para o cartão de origem.

### 4.11 Outros

- **Chip (`.chip`):** mono de 12 px, raio 4 px, para stack, nível, domínio e competências.
- **Avatar (`.avatar`):** círculo `--paper-2` com as iniciais em JetBrains Mono 600, em tinta. Não tem cor por pessoa.
- **Aviso flutuante (`.toast`):** bloco `#141414` (preto nos dois modos) de 6 px de raio que sobe do rodapé por 3 s, com `role="status"`.
- **Estado vazio (`.empty`):** uma ficha de ícone do que falta, um título e a próxima ação.
- **Lista de vantagens (`.benefits`):** até três linhas, cada uma com uma ficha de ícone e uma frase.

### 4.12 Recorte da vaga (`.clip`)

Papel `#f4f1ea` com texto `#141414` nos dois modos, raio 8 px, borda tracejada interna e rótulo mono com a tesoura. Não inclina. Tem dois usos: como campo, com o `<textarea>` dentro, e como leitura, com o texto e as marcas. No foco, ganha borda roxa e o contorno de foco.

### 4.13 Achados (`.findings`)

Lista de grupos dentro de um cartão. Cada grupo tem a ficha de ícone, o nome em mono e os chips. O chip de competência traz, em letra menor, o termo que a evidencia, e o nome da competência se decifra na extração (`data-decrypt`). O chip de "Fora deste projeto" tem contorno coral e fundo transparente.

### 4.14 Linha de status (`.status-line`)

Uma linha só, em mono, que mostra que o sistema está trabalhando e há quanto tempo:

1. a matriz de pontos (`data-dot-matrix="scan|wave|flip"`), com o padrão da etapa;
2. o rótulo (`.status-label`), no gerúndio: "Lendo a vaga", "Montando o plano", "Criando o repositório", "Carregando a turma";
3. o cronômetro (`.status-timer`), em segundos com uma casa e vírgula: "2,3 s".

Juntas, leem "Montando o plano · 2,3 s". A linha existe só durante espera real e some quando a resposta chega. A matriz e o cronômetro ficam fora da árvore de acessibilidade; uma região `role="status"` anuncia o rótulo uma vez. Sobre painel escuro, a linha usa texto `#f4f1ea` nos dois modos e os pontos mantêm a cor do padrão.

### 4.15 Marca de etapa (`.step-mark`)

Ícone de 18 px ao lado do nome de cada etapa, numa lista vertical. Três estados:

| Estado | Desenho | Quando |
| --- | --- | --- |
| `is-todo` | anel parado em `--dot-empty` | etapa futura |
| `is-current` | arco girando em `--focus` (roxo no claro, roxo claro no escuro e sobre painel escuro, para ter 3:1) | etapa em andamento |
| `is-done` | visto verde desenhado | etapa feita |

O nome da etapa diz o estado também em texto para leitores de tela ("feito", "em andamento"). O disco verde fica abaixo de 3:1 sobre o branco; a cor não é o único sinal: o visto e o texto dizem o mesmo. A última etapa só passa para `is-done` quando o servidor responde (item 3.2).

### 4.16 Grade de testes (`.test-grid`)

Mostra onde o aluno está no plano, um ponto por teste. Aparece no cartão do aluno, no topo da gaveta (só a missão atual), nas linhas da turma e na demo do painel da landing, com dados fictícios.

- **Linha (`.test-row`):** uma por missão do plano, na ordem do plano. À esquerda, o número da missão em mono (`.test-mission`); no meio, os pontos (`.test-dots`); à direita, "passando/total" em mono (`.test-count`), como "3/4".
- **Ponto (`.td`):** quadrado de 12 px com 4 px de raio, 4 px entre pontos. Três estados:

| Estado | Cor | Quando |
| --- | --- | --- |
| `is-pass` | `--green` | teste passando, em missão antes da atual ou na atual |
| `is-fail` | `--coral` | teste falhando, em missão antes da atual ou na atual |
| `is-empty` | contorno `--dot-empty`, sem preenchimento | todo teste de missão depois da atual |

Regras:
- **Missões depois da atual** ficam com todos os pontos vazios, mesmo que os testes delas falhem no relatório, e a contagem mostra "—". A suíte inteira roda a cada push, então os testes das missões futuras sempre falham; pintá-los de coral deixaria todo cartão alarmante sem informar nada.
- **Aluno concluído:** tudo verde.
- **A cor não é o único sinal:** o verde e o coral cheios ficam abaixo de 3:1 sobre o branco. A contagem "3/4" ao lado dos pontos e o texto de cada linha dizem o mesmo que as cores.
- **`total`** é a quantidade de testes da missão que rodaram no último relatório, sem os `skipped`.
- **Missão sem contagem** (o aluno ainda não fez push depois da mudança, ou a missão não rodou): a linha mostra só o número da missão e "—", sem pontos.
- **Acessibilidade:** os pontos ficam fora da árvore de acessibilidade, e cada linha tem o texto equivalente: "Missão 2: 3 de 4 testes passando" ou "Missão 4: ainda não começou".
- **Linhas da turma** (topo do painel, opcional, conforme o espaço): a mesma grade, uma linha por aluno, em ordem alfabética do nome. Nunca do pior para o melhor, nunca com soma ou total da turma.
- A grade nunca vira ranking ou nota: nada ordena alunos pela quantidade de pontos verdes nem soma pontos como placar.

## 5. Telas

### 5.1 Aluno: da vaga ao repositório (`aluno.html`, rota `/`)

Fluxo em três passos. Os dois primeiros usam a largura de 1160 px; o terceiro, a coluna de 760 px.

1. **Vaga.** Duas colunas em telas largas. À esquerda, o título "Cole uma vaga. Receba um projeto." e três vantagens com fichas de ícone. À direita, as abas "Colar o texto" e "Usar um link" (a segunda usa `POST /api/job-postings/extraction/from-url`), o recorte da vaga como campo, "Usar uma vaga de exemplo", o botão "Montar meu plano" e a nota de privacidade com cadeado.
2. **Plano.** Duas colunas: o recorte da vaga à esquerda, fixo, e os achados à direita. A extração do item 3.1 acontece aqui. Quando termina, o plano aparece abaixo dos achados, com as missões em cascata e o botão primário "Criar meu repositório no GitHub". "Trocar a vaga" fica sob o recorte.
3. **Repositório.** A passagem do Plano para cá usa a troca em pontos (`data-dot-swap`). Durante a criação: um painel escuro com a linha de status em `flip` e quatro etapas com a marca de etapa (copiando o modelo, gravando o plano, abrindo as missões, escrevendo o README). No fim: o visto surge, o título ganha o marca-texto e o painel escuro mostra o repositório e as issues, todas com ponto coral e a legenda "os testes ficam vermelhos até você implementar". Botão "Abrir a Missão 1".

### 5.2 Professor: turmas (`pages/classrooms.html`, rota `/classrooms`)

- **Sem login:** bloco central com o título "Sua turma, de relance.", três linhas do que o professor ganha e "Entrar com o GitHub".
- **Com login:** duas colunas em telas largas.
  - **Nova turma**, em três passos: colar a vaga-base, conferir o plano (os mesmos cartões de missão) e preencher nome da turma, e-mail e o consentimento do resumo semanal. No fim, o campo de cópia com o link do convite e "Abrir o painel".
  - **Suas turmas:** cartões com o nome, a quantidade de alunos, uma barra empilhada com as situações e uma legenda em texto.
- **Sem permissão (403):** aviso `info` com a mensagem do servidor ("Só professores do piloto podem criar turmas.").

### 5.3 Convite (`pages/join.html`, rota `/join/<código>`)

Um único bloco de 760 px:

1. Selo "Convite", o nome da turma como título e "com @professor".
2. Chips do arquétipo e a lista das missões em trilha numerada.
3. **O que o professor vê.** Duas colunas lado a lado: "Vê" (o nome que você escrever, sua missão atual, quantos testes passam e quais falham, as perguntas do revisor, quantas dicas você pediu) e "Não vê" (seu código, seus comentários, nada fora deste projeto). Abaixo, a frase de consentimento exata do desenho e a caixa de marcação.
4. Campo "Como você quer ser chamado(a)?" com contador até 40.
5. Botão "Entrar com o GitHub e criar meu repositório", desabilitado até o nome e o consentimento estarem preenchidos. A razão aparece ao lado do botão.

Depois de criar, a tela passa pela troca em pontos e mostra o mesmo passo 3 da página do aluno, com a marca de etapa na entrada na turma. Quem já está na turma vê direto o cartão do repositório, com "Você já está nesta turma."

### 5.4 Painel da turma (`pages/classroom.html`, rota `/classrooms/<id>`)

- **Cabeçalho:** nome da turma, chips (stack, quantidade de alunos), o campo de cópia do convite e a hora da última atualização.
- **Resumo da turma:** a barra de proporção e os quatro contadores (item 4.8), com a contagem animada. Clicar filtra.
- **Linhas da turma (opcional):** a grade de testes por aluno, em ordem alfabética (item 4.16), se couber com as sete colunas.
- **Quadro:** uma coluna por missão e, abaixo, a faixa "Concluído". Cada cartão traz a grade de testes do aluno. As colunas com alunos dividem a largura por igual, com pelo menos 280 px cada, e as vazias viram trilhos de 76 px. Quando não cabem, o quadro rola na horizontal por dentro, com encaixe por coluna. Abaixo de 760 px, cada missão vira uma seção empilhada, e as vazias ficam recolhidas.
- **Ficha:** a gaveta do item 4.10.
- **Turma sem alunos:** estado vazio com o link do convite em destaque.
- **Carregando:** a linha de status em `scan` ("Carregando a turma"). **Erro:** aviso com "Tentar de novo".

### 5.5 E-mail semanal (`email.html`)

Tabela de 600 px, fundo papel, uma coluna, estilos em linha e fontes de sistema (`Arial`, sem fontes da web). Da direção B, o e-mail recebe só os tokens de cor e os raios; não tem pontos nem animação.

1. Marca e "Resumo da semana de 18/11".
2. Três blocos de número lado a lado: travados (coral), parados (cinza) e avançando (verde).
3. Lista "Vale uma conversa": nome, situação em texto, missão e motivo. Abaixo, uma linha com quem concluiu na semana.
4. Botão "Abrir o painel" (roxo, texto branco, raio 6 px).
5. Rodapé: por que a pessoa recebe o e-mail e como parar de receber.

Sem ninguém travado ou parado, o item 3 vira "Ninguém travado ou parado nesta semana."

## 6. Voz

- **Segunda pessoa, direta:** "Cole a vaga", "Seu repositório está pronto".
- **Botões dizem o que acontece:** "Montar meu plano", e não "Enviar".
- **Erros dizem o que houve e o que fazer:** "O GitHub não conseguiu criar o repositório agora. Tente de novo em instantes."
- **Situações são descritas, nunca julgadas:** "4 dias na missão 2", e não "atrasado".
- **Vocabulário do `CONTEXT.md`:** aluno, professor, turma, vaga, missão, plano, revisor, pergunta, dica. Evite "dashboard", "template", "issue" (no texto para o aluno, "missão").

## 7. Acessibilidade

- Contraste mínimo de 4.5:1 para texto e 3:1 para bordas de controles, ícones e pontos vazios, nos dois modos.
- Foco visível em tudo: contorno de 3 px em `--focus` (roxo) com 3 px de afastamento.
- Tudo funciona pelo teclado: abas com setas, contadores com `aria-pressed`, cartões como `<button>`, gaveta como `<dialog>`.
- Mudanças assíncronas são anunciadas por uma região `role="status"`.
- Pontos, matriz e cronômetro são decorativos para leitores de tela; a informação vem em texto (itens 4.14 a 4.16), e o texto decifrado chega pronto.
- Alvos de toque com pelo menos 44 px.
- As telas funcionam de 375 px a 1440 px sem rolagem horizontal da página. Só o quadro do painel rola por dentro.
- `prefers-reduced-motion` e `prefers-color-scheme` são respeitados. Com movimento reduzido, todo componente mostra o estado final, parado.
- A auditoria com axe da semana 3 cobre todas as telas, em 1440 px e 375 px.

## 8. Implementação

- Sem framework e sem etapa de build: HTML, CSS e JavaScript puros, servidos de `src/main/resources/static`, como hoje. Sem React, three.js ou dependência de npm na interface.
- `vv.css` e `vv.js` dos protótipos são o ponto de partida do CSS e do JavaScript compartilhados das páginas reais.
- Os componentes de movimento ficam em `vv-motion.css` e `vv-motion.js`, ao lado de `vv.css` e `vv.js`, e são usados pelos protótipos, pelas páginas reais e pela landing. Cada componente funciona só com atributos e classes no HTML e respeita movimento reduzido. Eles foram inspirados no React Bits e no Matrix dot loader da transitions.dev, e reescritos do zero: nenhum dos dois é dependência.
- A landing (`site/index.html`) é publicada sozinha, com o CSS dentro do arquivo. Ela recebe uma cópia dos tokens e dos componentes que usa, marcada com um comentário de origem.
- As fontes vêm do Google Fonts, como na landing page, com `display=swap` e fontes de sistema como reserva.
- Os dados dos protótipos têm o mesmo formato das respostas da API descritas em `docs/historic/plans/2026-11-11-classroom-mode.md`. A grade de testes lê a lista `tests` do cartão (`missionId`, `passed`, `total`, na ordem do plano).

## 9. O que evitar

Esta lista existe porque estes padrões deixam a interface com cara de infantil ou de genérica. Antes de entregar uma tela, confira cada item.

- **Rótulo em maiúsculas acima de todo título.** O rótulo mono só entra quando traz uma informação que o título não tem, como "Convite". No máximo um por tela.
- **Selos e bolinhas dentro do título.** O título é texto. Selo, ponto, ícone e balão de "?" ficam fora dele.
- **Marca-texto em todo título.** Uma palavra marcada por tela, e só em telas de entrada ou de sucesso.
- **Rabiscos e carimbos.** Nem ziguezague, nem carimbo flutuante, nem rabisco à mão. Os motivos são o ponto e o marca-texto.
- **Forma de brinquedo.** Cantos de 28 a 36 px, botões em pílula de 999 px, sombra dura colorida, mola (`--ease-spring`), Bricolage 800 em todo título e as quatro cores juntas em toda tela.
- **Enfeite repetido.** Cartão dentro de cartão e sombras em tudo. Cada tela tem no máximo um bloco com sombra de repouso.
- **Quatro cartões de número lado a lado.** Números da turma vão juntos, num único resumo, com a barra de proporção.
- **Grade de testes como ranking ou nota.** Alunos ordenados pela quantidade de pontos verdes, ou pontos da turma somados como placar.
- **Ícones de bancos genéricos misturados**, emoji como ícone, ou ícone de brilho para "IA".
- **Parede de logotipos de tecnologia.** A stack aparece como chips onde ela importa, não como uma parede de selos "Feito com".
- **Três bolinhas cinzas como estado vazio.** O estado vazio usa a ficha de ícone do que falta e diz o próximo passo.
- **Animação que não vem de dado real ou de espera real.** Barra de progresso inventada, texto "pensando…", palavras marcadas que o servidor não reconheceu, ou movimento em tela de trabalho que distrai o professor de ler o painel.
- **Efeitos sem significado (cursor, aurora, vidro, 3D).** Nem cartões inclinados, crachá 3D, cursor personalizado ou brilho neon.
- **Template de SaaS genérico.** Fundo escuro com degradê roxo, vidro fosco e brilhos. A marca é chapada.
- **Texto de enchimento.** Frases como "Bem-vindo à sua jornada" ou "Potencialize seu aprendizado". Cada frase diz o que acontece ou o que fazer.
