# Design do Vaga Viva

Este documento é a fonte única das decisões visuais e de interação do produto: página do aluno, área do professor, convite da turma, painel e e-mail. A landing page (`site/index.html`) é a origem da identidade; aqui ela vira um sistema para telas de trabalho.

Os protótipos navegáveis ficam em `docs/design/prototipos/`. Eles usam dados fictícios e implementam tudo o que está escrito aqui. O CSS de referência é `docs/design/prototipos/vv.css`.

## 1. Princípios

1. **Papel, tinta e quatro cores.** O fundo é papel quente, o texto é tinta quase preta, e roxo, amarelo, verde e coral aparecem em blocos chapados. Não há degradês nem sombras difusas coloridas.
2. **A interface mostra o que o produto faz.** Os motivos visuais vêm do próprio produto: bolinhas de teste (vermelhas e verdes), o marca-texto sobre a vaga, o balão de pergunta do revisor e o ziguezague do progresso.
3. **Uma ação principal por tela.** Cada tela tem um único botão preenchido. O resto é botão vazado ou link.
4. **O sistema avisa, não avalia.** "Travado" e "parado" são avisos para o professor conversar com o aluno. Eles sempre vêm com o motivo e nunca com tom de nota, ranking ou alarme.
5. **Movimento explica.** Uma animação existe para mostrar de onde algo veio, o que mudou ou o que está acontecendo. Nada se mexe só para enfeitar uma tela de trabalho.
6. **O aluno sabe o que é visto.** Onde há acompanhamento, a tela diz o que o professor vê e o que não vê, antes de qualquer ação.

## 2. Fundamentos

### 2.1 Cor

Os valores são tokens CSS em `:root`. O modo escuro troca os tokens neutros; as quatro cores de marca não mudam.

| Token | Claro | Escuro | Uso |
| --- | --- | --- | --- |
| `--paper` | `#f4f1ea` | `#161513` | fundo da página |
| `--paper-2` | `#ebe7dc` | `#201e1b` | fundo de áreas rebaixadas, trilhos |
| `--card` | `#ffffff` | `#1f1d1a` | superfícies |
| `--ink` | `#141414` | `#f4f1ea` | texto e bordas fortes |
| `--ink-2` | `#4a4740` | `#b4afa3` | texto secundário |
| `--line` | `#dcd7ca` | `#34312c` | bordas finas |
| `--purple` | `#5b3df5` | igual | marca, foco, revisor, concluído |
| `--purple-soft` | `#e6e0ff` | `#2c2266` | fundos do revisor e de seleção |
| `--yellow` | `#ffd23f` | igual | destaque, marca-texto, dificuldade |
| `--green` | `#33c47a` | igual | teste passando, avançando |
| `--coral` | `#ff6b4a` | igual | teste falhando, travado, erro |
| `--green-ink` | `#0e6b3c` | `#5ee39c` | texto verde sobre papel |
| `--coral-ink` | `#b3341a` | `#ff9a82` | texto coral sobre papel |
| `--idle` | `#8b8678` | `#8b8678` | parado |
| `--night`, `--night-2`, `--night-3` | `#121212`, `#1e1e1e`, `#2a2a2a` | `#0b0b0a`, `#1a1917`, `#26241f` | painéis escuros (repositório, terminal) |

Regras:
- Sobre amarelo, verde ou coral, o texto é sempre `#141414`, nos dois modos. Sobre roxo, o texto é sempre branco.
- Texto colorido sobre papel usa as variantes `-ink`. Nunca use `--green` ou `--coral` como cor de texto pequeno.
- Cor nunca é o único sinal. Toda situação tem também um ícone e uma palavra.

### 2.2 Situações do aluno

| Situação | Cor | Ícone | Rótulo | Frase de motivo |
| --- | --- | --- | --- | --- |
| `STUCK` | `--coral` | seta em volta | travado | "4 dias na missão 2, 3 PRs nesse período" |
| `IDLE` | `--idle` | lua | parado | "sem atividade há 6 dias" |
| `ADVANCING` | `--green` | seta para cima | avançando | "há 2 dias na missão" |
| `COMPLETED` | `--purple` | visto | concluído | "todas as missões passando" |

O rótulo é o nome da situação e não flexiona por gênero, porque o sistema não guarda esse dado; por isso ele aparece sempre como selo, em maiúsculas mono, e nunca dentro de uma frase sobre a pessoa.

No cartão do aluno, a situação aparece como uma faixa de 6 px à esquerda, um selo com ícone e rótulo, e a frase de motivo. O painel não usa vermelho piscando, contagem regressiva nem ordenação "do pior para o melhor".

### 2.3 Tipografia

| Papel | Fonte | Peso | Tamanho |
| --- | --- | --- | --- |
| Título de página (`h1`) | Bricolage Grotesque | 800 | `clamp(34px, 5vw, 56px)`, entrelinha 1.04, espaçamento -0.035em |
| Título de seção (`h2`) | Bricolage Grotesque | 800 | 28 px |
| Título de cartão (`h3`) | Bricolage Grotesque | 700 | 19 px |
| Corpo | DM Sans | 400 e 500 | 16 px, entrelinha 1.55 |
| Texto de apoio | DM Sans | 400 | 14 px |
| Rótulo, número, código, nome de teste | JetBrains Mono | 600 | 12 a 13 px, maiúsculas com espaçamento 0.08em nos rótulos |

- Os números dos contadores usam Bricolage 800 com `font-variant-numeric: tabular-nums`.
- Títulos em frases curtas, com ponto final quando forem afirmações ("Sua turma, de relance.").

### 2.4 Espaço, forma e elevação

- **Escala de espaço:** 4, 8, 12, 16, 24, 32, 48, 72 px.
- **Largura:** `.wrap` de 1160 px como padrão, 760 px de conteúdo (`.wrap.narrow`) para fluxos de formulário e 1440 px (`.wrap.wide`) para o painel, onde cabem as sete colunas.
- **Raios:** 12 px (campos, itens de lista), 16 px (cartões pequenos), 28 px (cartões e painéis), 36 px (bloco principal da tela), 999 px (botões, selos, chips).
- **Bordas:** 1 px `--line` em superfícies; 2 px `--ink` em botões e campos focados.
- **Elevação tem dois níveis:**
  - *Repouso:* `0 30px 60px -30px rgba(20,20,20,.25)`, só no bloco principal da tela e na gaveta.
  - *Sombra dura:* `0 6px 0 <cor>`, sem desfoque. É a assinatura do sistema e aparece no hover de botões e cartões clicáveis (roxo no primário, amarelo no vazado, a cor da situação no cartão do aluno).

### 2.5 Iconografia

O produto tem uma família própria de ícones, desenhada para ele. A fonte é o mapa `ICON` de `vv.js`.

- **Desenho:** grade de 24 px, traço de 2,2 px na cor do texto, pontas e junções redondas, sem preenchimento. Formas geométricas simples, com no máximo três traços por ícone.
- **Ficha de ícone (`.ic`):** o ícone nunca leva cor. Quando precisa de destaque, ele vai em tinta sobre um círculo de cor chapada: 28, 40 ou 56 px. É o mesmo tratamento dos princípios na landing page.
- **Cor da ficha tem significado fixo:**

| Cor | Significa | Exemplos |
| --- | --- | --- |
| amarelo | a vaga e as tecnologias | vaga, tecnologias, convite |
| lilás | nível e o revisor | nível, pergunta, e-mail |
| verde | competências e progresso | competências, missão, avançando |
| coral | o que pede atenção | fora do projeto, travado, erro |
| roxo | concluído | concluído |
| cinza | neutro | parado, estados vazios |

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

- **Bolinhas de teste:** círculos de 10 a 18 px, coral para falhando e verde para passando. São usadas no progresso das missões e na criação do repositório.
- **Marca-texto:** faixa que se desenha da esquerda para a direita atrás de um trecho. Amarela para tecnologias, lilás para o nível e coral claro para o que fica fora do projeto.
- **Recorte:** a vaga aparece como um recorte de jornal: papel claro nos dois modos, borda tracejada de corte, uma tesoura no rótulo e meio grau de inclinação.
- **Ziguezague:** traço grosso com pontas redondas, em uma cor de marca, recortado pela borda do bloco. Só nas telas de entrada e de sucesso, no máximo um por tela, sempre atrás do conteúdo.
- **Selo "pill":** forma de pílula preta com conteúdo mono, para número de missão, código de convite e nome de repositório.

## 3. Movimento

| Token | Valor | Uso |
| --- | --- | --- |
| `--t-fast` | 150 ms | hover, foco, pressionar |
| `--t-base` | 280 ms | entrada de itens, troca de estado |
| `--t-slow` | 520 ms | gaveta, marca-texto, traço do ziguezague |
| `--ease-out` | `cubic-bezier(.2, .8, .2, 1)` | entradas e saídas |
| `--ease-spring` | `cubic-bezier(.34, 1.56, .64, 1)` | estouro de selos e bolinhas |

Padrões:

| Nome | O que faz | Onde |
| --- | --- | --- |
| **Subir** (`rise`) | opacidade 0 → 1 e 10 px para cima, `--t-base` | qualquer bloco que aparece |
| **Cascata** (`stagger`) | filhos sobem com 60 ms de atraso entre si, até 8 itens | missões do plano, cartões do painel |
| **Estouro** (`pop`) | escala 0.6 → 1 com `--ease-spring` | selos, bolinhas, visto de sucesso |
| **Marca-texto** (`marker`) | a faixa cresce de 0 a 100% em `--t-slow` | termo reconhecido, título do sucesso |
| **Extração** | a sequência do item 3.1 | leitura da vaga |
| **Leitura** (`scan`) | uma faixa amarela translúcida varre linhas-esqueleto | carregamento do painel |
| **Virada do teste** | a bolinha coral gira e vira verde, uma por vez | etapas da criação do repositório |
| **Contagem** | o número sobe de 0 ao valor em 600 ms | contadores do painel, uma vez por carregamento |
| **Gaveta** | desliza 100% → 0 em `--t-slow`; o fundo escurece para 45% | ficha do aluno |
| **Sombra dura** | sobe 2 px e ganha `0 6px 0 <cor>` em `--t-fast` | hover de botões e cartões |
| **Pressionar** | desce 1 px e a sombra dura encolhe para 2 px | `:active` |

### 3.1 A extração da vaga

É o momento principal do produto: o aluno vê o que foi lido na vaga dele. A animação mostra dados reais e nada além deles. Cada palavra marcada é um termo que `POST /api/job-postings/extraction` devolveu (`matchedTerms` e `evidence`). O que o sistema não reconheceu fica sem marca.

A sequência dura cerca de 4 segundos:

1. **Varredura (0,9 s).** O campo vira o recorte da vaga, à esquerda, e uma linha de leitura amarela desce por ele uma vez.
2. **Palavras (0,4 s cada, sobrepostas).** Na ordem do texto, cada termo reconhecido ganha o marca-texto. Na primeira vez que aparece, a palavra se solta do recorte, faz um arco com leve giro e pousa como chip no seu grupo, à direita: Nível, Tecnologias ou Fora deste projeto. A próxima palavra se acende enquanto a anterior ainda está no ar. O título conta: "3 termos reconhecidos".
3. **Competências (0,13 s cada).** Para cada competência, o chip do termo que a evidencia pulsa, e o chip da competência estoura no grupo, dizendo de onde veio: "Programação em Java, por 'Java'".
4. **Plano.** O título vira "O que a vaga pede", e as missões entram em cascata abaixo.

Regras:
- **"Pular a animação"** fica visível durante toda a sequência e leva direto ao estado final.
- Com `prefers-reduced-motion`, a tela já abre no estado final, com todas as marcas e chips.
- O recorte fica fixo ao lado do plano em telas largas, para o aluno conferir cada marca. Em telas estreitas, ele fica acima, com altura limitada e rolagem própria.
- Uma região `role="status"` anuncia só o resultado ("Leitura concluída: nível júnior, 5 tecnologias e 5 competências"), e não cada palavra.
- O que a vaga cita e o projeto não pratica entra no grupo "Fora deste projeto", com contorno coral e uma frase de explicação. Isso substitui o aviso amarelo de compatibilidade.

### 3.2 Regras gerais

- Só `transform` e `opacity` são animados, além de `background-size` no marca-texto.
- Nada entra em laço infinito, exceto o indicador de carregamento enquanto houver espera real.
- Com `prefers-reduced-motion: reduce`, todas as animações e transições caem para 0.01 ms: o estado final aparece direto, a contagem mostra o número final e a gaveta abre sem deslizar.
- As etapas da criação do repositório avançam por tempo e são ilustrativas. A última só é marcada quando o servidor responde; se a resposta demorar, a penúltima fica em espera.

## 4. Componentes

Os nomes são as classes de `vv.css`.

### 4.1 Botão (`.btn`)

- **Primário:** fundo `--ink`, texto `--paper`, pílula, 14 × 22 px, DM Sans 700. Hover com sombra dura roxa.
- **Vazado (`.ghost`):** transparente, borda 2 px `--ink`. Hover com sombra dura amarela.
- **Marca (`.brand-btn`):** fundo roxo, texto branco; só para a ação final de um fluxo ("Criar meu repositório").
- **Seta:** o primário pode levar um círculo amarelo de 24 px com uma seta, que anda 3 px no hover.
- **Carregando:** `aria-busy="true"`, texto trocado para o gerúndio ("Lendo a vaga…") e três bolinhas pulsando no lugar da seta. O botão fica desabilitado.
- **Alvo mínimo:** 44 px de altura.

### 4.2 Campos (`.field`)

- Rótulo acima, em DM Sans 600. O texto de ajuda fica abaixo do rótulo, em 14 px `--ink-2`.
- Campo com fundo `--card`, borda 1 px `--line`, raio 12 px. No foco: borda 2 px `--ink` e sombra dura roxa de 4 px.
- Erro: borda coral, mensagem abaixo em `--coral-ink` com ícone, ligada por `aria-describedby`. A mensagem diz como corrigir.
- Contador de caracteres à direita, em mono, quando há limite (nome do aluno: 40).
- **Caixa de marcação (`.check`):** quadrado de 22 px, raio 6 px; marcada fica roxa com o visto desenhado em `--t-fast`.

### 4.3 Passos (`.steps`)

Trilho horizontal com três passos numerados em selos mono. O passo atual é preto; os feitos são verdes com visto; os futuros são vazados. Em telas estreitas, só o passo atual mostra o nome.

### 4.4 Cartão de missão (`.mission`)

Linha com: número em selo mono, título (h3), resumo, barras de dificuldade (3 barras, amarelas até o nível) e um selo de motivo:

| Motivo | Selo | Cor |
| --- | --- | --- |
| `CORE` | central | verde |
| `SKILL_MATCH` | pela vaga | roxo |
| `DEFAULT` | padrão | neutro |

As competências que casaram com a vaga aparecem como chips abaixo do resumo.

### 4.5 Aviso (`.banner`)

Faixa com ícone, título curto e texto. Variantes: `info` (roxo suave), `warn` (amarelo) e `error` (coral). Usada para a compatibilidade com o arquétipo, erros de rede e o consentimento.

### 4.6 Painel escuro (`.night-panel`)

Bloco `--night-2` para tudo que representa o GitHub: nome do repositório em mono, lista de issues com bolinhas de teste e o trecho de terminal.

### 4.7 Campo de cópia (`.copy`)

Pílula com o link em mono e um botão "Copiar". Ao copiar, o botão vira "Copiado" com visto por 2 s, e um `aria-live` anuncia.

### 4.8 Resumo da turma (`.summary`, `.stat`)

Um único cartão com duas partes. Em cima, uma barra dividida na proporção das situações, que cresce da esquerda ao carregar. Embaixo, quatro contadores, cada um com a ficha de ícone da situação, o número e o rótulo. O contador é também um filtro: `aria-pressed="true"` deixa o bloco com borda 2 px `--ink` e esmaece os cartões das outras situações. Clicar de novo limpa o filtro.

### 4.9 Coluna e cartão do aluno (`.column`, `.student`)

- **Coluna:** cabeçalho com o número da missão em selo, o título em 18 px e a quantidade de alunos; fundo `--paper-2`, raio 28 px, 20 px de respiro por dentro e 16 px entre colunas. Uma coluna com alunos tem pelo menos 280 px de largura e 340 px de altura.
- **Coluna vazia:** encolhe para um trilho de 76 px, com o número e o título na vertical. Assim, o espaço vai para as missões onde há alunos. Não há texto de erro nem traço.
- **Cartão:** é um `<button>` com 18 px de respiro. Mostra as iniciais num círculo de 44 px, o nome em Bricolage 18 px, o selo de situação logo abaixo do nome e, separada por uma linha fina, a frase de motivo em 14,5 px. Hover com sombra dura na cor da situação. Entre cartões, 12 px.
- **Densidade:** o quadro é para ler de relance, não para caber tudo. Na dúvida entre apertar e rolar, o quadro rola.
- **Faixa "Concluído":** fica abaixo do quadro, em largura total, com fundo `--purple-soft` e os cartões lado a lado. Quem concluiu é quem menos precisa de atenção, e a faixa libera uma coluna inteira para as missões. Sem ninguém concluído, a faixa não aparece.

### 4.10 Gaveta da ficha (`.drawer`)

`<dialog>` modal ancorado à direita, com 480 px de largura; abaixo de 760 px vira uma folha que sobe de baixo e ocupa 92% da altura. Contém:

1. Cabeçalho: iniciais, nome, login em mono, selo de situação e o botão de fechar.
2. Missão atual e frase de motivo.
3. **Testes que falham:** lista com bolinha coral, nome do teste em mono e mensagem.
4. **Última pergunta do revisor:** balão em `--purple-soft` com o avatar "?" do revisor.
5. **Dicas pedidas:** três bolinhas (preenchidas até a quantidade) e o texto "2 de 3 nesta missão".
6. Rodapé com "Abrir o PR no GitHub" e "Abrir o repositório".

`Esc` e o clique no fundo fecham. O foco entra no botão de fechar e volta para o cartão de origem.

### 4.11 Outros

- **Chip (`.chip`):** pílula mono de 12 px para stack, nível, domínio e competências.
- **Avatar (`.avatar`):** círculo com iniciais em Bricolage 800. A cor vem de uma lista fixa (amarelo, coral, lilás, verde, azul), escolhida pelo login, e não tem significado.
- **Aviso flutuante (`.toast`):** pílula preta que sobe do rodapé por 3 s, com `role="status"`.
- **Esqueleto (`.skeleton`):** linhas `--paper-2` com a varredura de leitura.
- **Estado vazio (`.empty`):** uma ou duas fichas de ícone grandes, um título e a próxima ação.
- **Lista de vantagens (`.benefits`):** até três linhas, cada uma com uma ficha de ícone e uma frase.

### 4.12 Recorte da vaga (`.clip`)

Papel `#f4f1ea` com texto `#141414` nos dois modos, borda tracejada interna, rótulo mono com a tesoura e inclinação de -0,6 grau em telas largas. Tem dois usos: como campo, com o `<textarea>` dentro, e como leitura, com o texto e as marcas. No foco, ganha borda de tinta e a sombra dura roxa.

### 4.13 Achados (`.findings`)

Lista de grupos dentro de um cartão. Cada grupo tem a ficha de ícone, o nome em mono e os chips. O chip de competência traz, em letra menor, o termo que a evidencia. O chip de "Fora deste projeto" tem contorno coral e fundo transparente.

## 5. Telas

### 5.1 Aluno: da vaga ao repositório (`aluno.html`, rota `/`)

Fluxo em três passos. Os dois primeiros usam a largura de 1160 px; o terceiro, a coluna de 760 px.

1. **Vaga.** Duas colunas em telas largas. À esquerda, o título "Cole uma vaga. Receba um projeto." e três vantagens com fichas de ícone. À direita, as abas "Colar o texto" e "Usar um link" (a segunda usa `POST /api/job-postings/extraction/from-url`), o recorte da vaga como campo, "Usar uma vaga de exemplo", o botão "Montar meu plano" e a nota de privacidade com cadeado.
2. **Plano.** Duas colunas: o recorte da vaga à esquerda, fixo, e os achados à direita. A extração do item 3.1 acontece aqui. Quando termina, o plano aparece abaixo dos achados, com as missões em cascata e o botão de marca "Criar meu repositório no GitHub". "Trocar a vaga" fica sob o recorte.
3. **Repositório.** Durante a criação: quatro etapas com a virada do teste (copiando o modelo, gravando o plano, abrindo as missões, escrevendo o README). No fim: o visto em estouro, o título com marca-texto e um painel escuro com o repositório e as issues, todas com bolinha coral e a legenda "os testes ficam vermelhos até você implementar". Botão "Abrir a Missão 1".

### 5.2 Professor: turmas (`professor.html`, rota `/professor`)

- **Sem login:** bloco central com o título "Sua turma, de relance.", três linhas do que o professor ganha e "Entrar com o GitHub".
- **Com login:** duas colunas em telas largas.
  - **Nova turma**, em três passos: colar a vaga-base, conferir o plano (os mesmos cartões de missão) e preencher nome da turma, e-mail e o consentimento do resumo semanal. No fim, o campo de cópia com o link do convite e "Abrir o painel".
  - **Suas turmas:** cartões com o nome, a quantidade de alunos, uma barra empilhada com as situações e uma legenda em texto.
- **Sem permissão (403):** aviso `info` com a mensagem do servidor ("Só professores do piloto podem criar turmas.").

### 5.3 Convite (`turma.html`, rota `/turma/<código>`)

Um único bloco de 760 px:

1. Selo "Convite", o nome da turma como título e "com @professor".
2. Chips do arquétipo e a lista das missões em trilha numerada.
3. **O que o professor vê.** Duas colunas lado a lado: "Vê" (o nome que você escrever, sua missão atual, os testes que falham, as perguntas do revisor, quantas dicas você pediu) e "Não vê" (seu código, seus comentários, nada fora deste projeto). Abaixo, a frase de consentimento exata do desenho e a caixa de marcação.
4. Campo "Como você quer ser chamado(a)?" com contador até 40.
5. Botão "Entrar com o GitHub e criar meu repositório", desabilitado até o nome e o consentimento estarem preenchidos. A razão aparece ao lado do botão.

Depois de criar, a tela mostra o mesmo passo 3 da página do aluno. Quem já está na turma vê direto o cartão do repositório, com "Você já está nesta turma."

### 5.4 Painel da turma (`painel.html`, rota `/turmas/<id>`)

- **Cabeçalho:** nome da turma, chips (stack, quantidade de alunos), o campo de cópia do convite e a hora da última atualização.
- **Resumo da turma:** a barra de proporção e os quatro contadores (item 4.8), com a contagem animada. Clicar filtra.
- **Quadro:** uma coluna por missão e, abaixo, a faixa "Concluído". As colunas com alunos dividem a largura por igual, com pelo menos 280 px cada, e as vazias viram trilhos de 76 px. Quando não cabem, o quadro rola na horizontal por dentro, com encaixe por coluna. Abaixo de 760 px, cada missão vira uma seção empilhada, e as vazias ficam recolhidas.
- **Ficha:** a gaveta do item 4.10.
- **Turma sem alunos:** estado vazio com o link do convite em destaque.
- **Carregando:** colunas-esqueleto. **Erro:** aviso com "Tentar de novo".

### 5.5 E-mail semanal (`email.html`)

Tabela de 600 px, fundo papel, uma coluna, estilos em linha e fontes de sistema (`Arial`, sem fontes da web).

1. Marca e "Resumo da semana de 18/11".
2. Três blocos de número lado a lado: travados (coral), parados (cinza) e avançando (verde).
3. Lista "Vale uma conversa": nome, situação em texto, missão e motivo. Abaixo, uma linha com quem concluiu na semana.
4. Botão "Abrir o painel" (pílula preta).
5. Rodapé: por que a pessoa recebe o e-mail e como parar de receber.

Sem ninguém travado ou parado, o item 3 vira "Ninguém travado ou parado nesta semana."

## 6. Voz

- **Segunda pessoa, direta:** "Cole a vaga", "Seu repositório está pronto".
- **Botões dizem o que acontece:** "Montar meu plano", e não "Enviar".
- **Erros dizem o que houve e o que fazer:** "O GitHub não conseguiu criar o repositório agora. Tente de novo em instantes."
- **Situações são descritas, nunca julgadas:** "4 dias na missão 2", e não "atrasado".
- **Vocabulário do `CONTEXT.md`:** aluno, professor, turma, vaga, missão, plano, revisor, pergunta, dica. Evite "dashboard", "template", "issue" (no texto para o aluno, "missão").

## 7. Acessibilidade

- Contraste mínimo de 4.5:1 para texto e 3:1 para bordas de controles e ícones, nos dois modos.
- Foco visível em tudo: contorno de 3 px roxo com 3 px de afastamento.
- Tudo funciona pelo teclado: abas com setas, contadores com `aria-pressed`, cartões como `<button>`, gaveta como `<dialog>`.
- Mudanças assíncronas são anunciadas por uma região `role="status"`.
- Alvos de toque com pelo menos 44 px.
- As telas funcionam de 375 px a 1440 px sem rolagem horizontal da página. Só o quadro do painel rola por dentro.
- `prefers-reduced-motion` e `prefers-color-scheme` são respeitados.
- A auditoria com axe da semana 3 cobre todas as telas, em 1440 px e 375 px.

## 8. Implementação

- Sem framework e sem etapa de build: HTML, CSS e JavaScript puros, servidos de `src/main/resources/static`, como hoje.
- `vv.css` e `vv.js` dos protótipos são o ponto de partida do CSS e do JavaScript compartilhados das páginas reais.
- As fontes vêm do Google Fonts, como na landing page, com `display=swap` e fontes de sistema como reserva.
- Os dados dos protótipos têm o mesmo formato das respostas da API descritas em `docs/superpowers/plans/2026-11-11-classroom-mode.md`.

## 9. O que evitar

Esta lista existe porque estes padrões deixam a interface com cara de genérica. Antes de entregar uma tela, confira cada item.

- **Rótulo em maiúsculas acima de todo título.** O rótulo mono só entra quando traz uma informação que o título não tem, como "Convite". No máximo um por tela.
- **Marca-texto em todo título.** Uma palavra marcada por tela, e só em telas de entrada ou de sucesso.
- **Enfeite repetido.** Ziguezague, cartão dentro de cartão e sombras em tudo. Cada tela tem no máximo um bloco com sombra de repouso.
- **Quatro cartões de número lado a lado.** Números da turma vão juntos, num único resumo, com a barra de proporção.
- **Ícones de bancos genéricos misturados**, emoji como ícone, ou ícone de brilho para "IA".
- **Três bolinhas cinzas como estado vazio.** O estado vazio usa a ficha de ícone do que falta e diz o próximo passo.
- **Animação que não vem de dado real.** Barra de progresso inventada, texto "pensando…" ou palavras marcadas que o servidor não reconheceu.
- **Texto de enchimento.** Frases como "Bem-vindo à sua jornada" ou "Potencialize seu aprendizado". Cada frase diz o que acontece ou o que fazer.
- **Degradê, vidro fosco e brilho neon.** A marca é chapada.
