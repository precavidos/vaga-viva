# Revisão final da direção B (layout, movimento e acessibilidade)

Revisão feita em 14/11/2026, na branch `feat/direcao-b`, sobre as três frentes da direção B: os protótipos (`docs/design/prototipos/`), as páginas reais do Spring (`src/main/resources/static/`) e a landing (`site/index.html`). As referências são o `DESIGN.md` e `docs/superpowers/specs/2026-11-13-direcao-b-pontos-design.md`.

## Como foi verificado

- **Verificador de layout da skill** (`design-taste-frontend`), na landing, em 1440×900, 1024×768, 768×1024 e 390×844, com `--no-stress`. O perfil do Chrome fica em `.taste-skill/`, agora no `.gitignore`.
- **Varredura por página**, num Chrome sem janela controlado pelo protocolo de depuração: cada página nas quatro larguras, nos modos claro e escuro, e de novo com `prefers-reduced-motion: reduce` emulado de verdade (não por troca de `matchMedia`). Em cada carga: rolagem horizontal da página (no topo e depois de rolar até o fim), blocos que saem da tela, alvos de toque abaixo de 24 px e abaixo de 44 px, ordem dos títulos e quantidade de `h1` visíveis, controles sem nome, contraste do texto contra o fundo real, textos que se sobrepõem, animações que continuam rodando com movimento reduzido, erros e avisos do console. Em 1440 claro, um passeio pelo teclado (Tab) até o botão principal, conferindo o contorno de foco em cada parada.
- **Páginas cobertas:** protótipos `index`, `aluno` (início, lendo a vaga, pronto), `professor` (sem login, com turmas, negado), `turma` (convite, código inválido), `painel` (com alunos, vazio, erro), `email`, `movimento`; páginas reais `/` (aluno), `/professor` (sem login e com login), `/turma/{código}` (sem login e com login), `/turmas/{id}` (sem login e com o painel); e a landing. As telas com login usam respostas simuladas do `fetch` (o login pelo GitHub não completa localmente), com os mesmos dados fictícios dos protótipos.
- **Fluxos com dados simulados**, para as correções: leitura da vaga com um termo que só casa por dobra de maiúsculas, retomada depois do login com o plano falhando, convite com resposta que não é JSON, painel com resposta que ele não consegue desenhar, nomes dos cartões do painel.

## O que o verificador de layout encontrou na landing

Contagem da primeira passada, nas quatro larguras: 19 `edge-left`, 18 `screen-short`, 17 `neighbour`, 11 `screen-height`, 9 `hero-fold`, 2 `sparse`. Nenhum `spill`, `bleed-one-side`, `lines`, `one-sided` nem rolagem horizontal.

| Achado | O que é | Decisão |
| --- | --- | --- |
| `edge-left` (19) | A segunda coluna dos blocos de duas colunas começava em quatro lugares diferentes em 1440 (664, 730, 744 e 794 px): cada bloco tinha a sua proporção. | **Corrigido** (`f3163df`): todo bloco de duas colunas divide como o cabeçalho de seção, em duas metades com 48 px entre elas, e o painel do repositório começa embaixo do parágrafo acima dele. Agora toda segunda coluna começa em 744 px (1440) e em 536 px (1024). O verificador continua apontando essas linhas porque mede contra a grade de 12 colunas do `system.css` da própria skill, que o desenho não usa: **não se aplica**. |
| `screen-short`, `screen-height`, `neighbour`, `sparse` | Regra "uma seção por tela": cada seção teria de ter exatamente uma ou duas alturas de tela. | **Não se aplica.** O desenho recusa essa regra: as seções têm a altura do conteúdo. |
| `hero-fold` | O topo não preenche a primeira tela em 1440 e 1024, a próxima seção aparece embaixo, e em 390 ele passa 7 px da dobra. | **Não se aplica** em 1440/1024 (mesma regra). Em 390 o topo inteiro (texto, botões e quase toda a demo) cabe na primeira tela; os 7 px que passam são o fim da demo. Fica como está. |

Outras regras da skill que o desenho recusa e que, por isso, não entram como correção: a proibição de roxo (o roxo é a marca), fotos reais, cenas de shader ou WebGL e o `system.css` próprio da skill.

## O que a varredura encontrou

| Achado | Onde | Decisão |
| --- | --- | --- |
| Logo quase invisível no escuro: quadrado `#141414` sobre papel `#161513`. | Todas as páginas do produto (o cabeçalho vem de `vv.js`). | **Corrigido** (`793b814`): o logo segue a landing, quadrado em `--ink` com visto verde, que no escuro vira quadrado claro com visto verde escuro. |
| Arco da marca de etapa em `--purple`: 2,7:1 sobre painel escuro e sobre o cartão escuro. | Criação do repositório, entrada na turma, demo da landing. | **Corrigido** (`e108809`): o arco usa `--focus`, e `#8a75ff` sobre painel escuro. |
| Foco perdido ao abrir o passo 2: o botão focado some junto com o formulário e o foco cai na página. | Aluno (protótipo e real). | **Corrigido** (`caffdba`): o foco vai para o título da leitura. |
| Nome acessível do cartão do painel com a grade inteira, seis frases por aluno. | Painel real. | **Corrigido** (`3916c66`): nome, situação, testes da missão atual e motivo. A grade continua no cartão e na ficha. |
| Botão "Ver no GitHub" da landing com espaço duplo entre as palavras (o `gap` do botão caía entre "Ver no" e "GitHub"). | Landing, cabeçalho. | **Corrigido** (`b436d06`). |
| Links do menu, logo, link de pular e listas do rodapé com 24 a 34 px de altura, abaixo dos 44 px do `DESIGN.md` 7. | Landing. | **Corrigido** (`f1c26c7`). Os links dentro de frase (ESCO, CC BY 4.0, Simple Icons, 22 px) ficam: são texto corrido, a exceção de alvo de toque vale para eles. |
| Nota da demo do topo sem os 10 px de respiro (uma segunda regra `.demo-note` sobrescrevia a primeira, inclusive o tamanho no celular). | Landing. | **Corrigido** (`89ec4fd`). |
| `aria-label` num `<pre>` sem papel, que leitores de tela ignoram. | Landing, demo do revisor. | **Corrigido** (`89ec4fd`): o terminal é um grupo nomeado. |
| `h1` duplicado, colisões de texto e contraste baixo no `mark` do título. | Protótipos. | Falsos positivos da varredura: o segundo `h1` está num passo escondido; as colisões eram do menu "Protótipo" (fechado) e da caixa de uma linha inteira; o texto do `mark` está sobre o amarelo, que é imagem de fundo. Corrigida a varredura e repassada. |
| Campo de texto do aluno sem contorno de foco próprio. | Aluno. | Falso positivo: o contorno fica no recorte em volta (`.clip:focus-within`), como o `DESIGN.md` 4.12 pede. |
| Selo "parado" com texto branco sobre `--idle`: 3,6:1 em 11 px. | Painel (protótipo e real), cartões e ficha. | **Corrigido** (`3e272fc`): texto `#141414`, 5,1:1; o `DESIGN.md` 2.2 passa a incluir o cinza de parado. |
| Painel vazio passava 1 px da largura da tela em 390 e 768 (o link do convite empurrava a coluna da grade). | Painel vazio. | **Corrigido** (`3e272fc`): a coluna encolhe e o link encurta com reticências. |
| Logo (32 px) e links pequenos isolados ("Usar uma vaga de exemplo", "Trocar a vaga", "Pular a animação", "← Suas turmas", 25 px) abaixo de 44 px. | Produto inteiro. | **Corrigido** (`3e272fc`). A caixa do aceite tem 22 px, mas o rótulo inteiro é clicável e passa de 44 px: fica. |
| Estados sem `h1` visível: passo 2 do aluno, painel sem login, convite inválido do protótipo. | Aluno, painel, convite. | **Corrigido** (`90980fc`): o título do estado vira `h1`, no mesmo tamanho. |
| Texto do link do convite "sobre" o botão Copiar; texto da vaga "sobre" o trilho de passos. | Painel, aluno. | Falsos positivos: o texto cortado por reticências e o texto que rola dentro do recorte contam como caixa inteira na varredura; na tela não há sobreposição. |
| Erros no console. | Todas. | Só os 401 esperados das páginas sem login e o `favicon.ico` que não existe nos protótipos e no Spring. Nenhuma exceção de JavaScript. |

Resultado da repassada depois das correções: ver a seção "Resultado final".

## Achados menores adiados das tarefas anteriores

Cada item do registro de progresso ("minor (deferred)") ficou corrigido ou decidido.

### Tarefa 4 (tokens e documento)

| Item | Decisão |
| --- | --- |
| Pontos verde e coral cheios e o visto feito abaixo de 3:1 sobre o branco. | **Corrigido no `DESIGN.md`** (`b8ce920`): a cor nunca é o único sinal; a contagem "3/4", o texto de cada linha e o visto dizem o mesmo. |
| Coral usado em erro e em "fora do projeto", além de estado de teste e situação. | **Corrigido no `DESIGN.md`** (`b8ce920`): a regra de cor diz que o coral também marca erro e o que fica fora do projeto. |
| Tamanho e espaço do `.td`; hover ambíguo; DM Sans 600/700 fora da tabela; "preto" no escuro; raio da gaveta. | **Corrigido no `DESIGN.md`** (`b8ce920`): 12 px com 4 px entre pontos; cartões não sobem no hover; 600 em rótulos e 700 em botões; selo e passo atual em `--ink`, aviso flutuante preto nos dois modos; gaveta sem raio ao lado e com 12 px em cima no celular. |

### Tarefa 5 (componentes de movimento)

| Item | Decisão |
| --- | --- |
| Intervalo do `flip` continua se a matriz sai da página sem `stop()`. | **Corrigido** (`e108809`), nas duas cópias e na landing. |
| Matrizes não pausam fora da tela. | Já corrigido na Tarefa 7 (`.is-offscreen` e o `IntersectionObserver` da matriz). |
| Ponta redonda do visto pode deixar um ponto nas marcas a fazer e atuais. | **Corrigido** (`e108809`): o visto fica oculto até a marca estar feita. |
| Varredura amarela com ~1,4:1 sobre papel claro. | **Fica.** A matriz é decorativa (`aria-hidden`); o rótulo e o cronômetro ao lado dizem o estado. Amarelo é a cor da varredura no `DESIGN.md`. |
| `reduced()` lido uma vez. | Já corrigido: é lido a cada uso. |
| `countUp` arredonda. | **Fica.** Os contadores são inteiros. |
| Componente marcado como ligado antes de ligar. | **Fica.** Se a ligação falhar, ligar de novo também falharia; evita laço. |

### Tarefa 6 (campo de pontos, decifrar, troca em pontos)

| Item | Decisão |
| --- | --- |
| Resíduo ao assentar o campo, sem ajuste final; sem tratar troca de densidade de tela; ponteiro antigo depois de rolar. | **Ficam.** Diferenças de um pixel num fundo decorativo; nenhuma aparece numa apresentação. |
| Cor `rgb()` cai para preto no campo. | **Fica.** Os tokens usados são hexadecimais; o canvas normaliza cores opacas para hexadecimal. |
| Decifrar roda abaixo da dobra; guarda para texto vazio; forma `data-decrypt="Nome"` com elemento vazio. | **Ficam.** Nenhuma página usa a forma com elemento vazio; o texto final real está sempre no DOM. |
| Malha da troca precisa de posicionamento; malha medida antes da troca. | Já corrigido na Tarefa 9 (`[data-dot-swap]` com `position: relative` e o raio do bloco). |
| Cauda do ruído cortada em nomes de uma linha; alinhamento em nomes que quebram linha. | **Ficam.** O ruído dura frações de segundo e termina no nome certo. |

### Tarefa 7 (protótipos)

| Item | Decisão |
| --- | --- |
| Logo quase invisível no escuro. | **Corrigido** (`793b814`). |
| Espera do plano pulada com movimento reduzido ou "pular". | **Fica.** É o pedido: o estado final aparece sem esperar a animação; a linha de status mede a espera real quando há uma. |
| Vazamento do observador de matriz fora da tela; leitura só da primeira entrada. | Já corrigido: o observador se desliga quando a matriz sai da página e lê a entrada mais nova. |
| `VVMotion.init()` sobrando no `professor.html`. | **Fica.** `init` é idempotente; não liga nada duas vezes. |
| `.eyebrow` sem uso. | **Fica.** É a classe do rótulo mono do `DESIGN.md` 9, disponível para quando um rótulo carregar algo que o título não diz. |
| Borda do vazado e do campo de cópia em `--line`, 1,4:1. | **Fica.** O `DESIGN.md` pede `--line`; o texto do botão (tinta) e o contorno de foco identificam o controle. |
| Malha quadrada sobre o bloco arredondado do convite. | Já corrigido na Tarefa 9. |
| Selo claro no modo escuro. | **Decidido no `DESIGN.md`** (`b8ce920`): o selo é `--ink`, claro no escuro, com texto `--paper`. |
| Foco perdido no estado de carregamento do aluno. | **Corrigido** (`caffdba`): o foco ia para a página quando o passo 2 abria; o botão desabilitado em si mantém o foco no Chrome. |
| Aba selecionada só pelo preenchimento em cores forçadas. | **Fica.** A aba também tem `aria-selected`; em cores forçadas o texto continua legível. Pouco provável no público do piloto. |
| `--focus` sobre painel escuro no modo claro, ~3,06:1. | **Fica.** Passa de 3:1. |

### Tarefa 8 (grade de testes)

| Item | Decisão |
| --- | --- |
| `DESIGN.md` dizia que os itens 4.14 a 4.16 ficam em `vv-motion.css`. | **Corrigido** (`b8ce920`): a grade de testes está em `vv.css`. |
| `movimento.html` carregava Bricolage 700 e 800. | **Corrigido** (`e0ceb5e`). |
| Herdado: `passed: null` virava 0; total não positivo ou inválido contava como total; `passed` passava do total; `t.order` entrava sem escape; grade vazia aparecia na gaveta; coluna da contagem estreita demais para "10/10". | **Corrigido** (`232c249`) em `VV.testGrid`: `passed: null` mostra "—", total não positivo ou inválido vale como sem total, `passed` fica entre 0 e o total, `esc(t.order)`, grade vazia ignorada na gaveta e coluna da contagem com 40 px. |

### Tarefa 9 (páginas reais do aluno, do professor e do convite)

| Item | Decisão |
| --- | --- |
| `since` da linha de status sem documentação. | **Corrigido** (`b8ce920`). |
| Plano do professor mostra ids de competência nos chips. | **Fica.** A API de plano não devolve os rótulos; mostrar os nomes pede mudança no servidor, fora desta revisão. |
| Convite sem o chip de domínio. | **Fica.** A API do convite não devolve o domínio. |
| Movimento reduzido e telas com login não verificados no navegador. | **Feito nesta revisão:** movimento reduzido emulado no Chrome em todas as páginas; telas com login com respostas simuladas. |
| Resposta antiga do plano encerra a linha de uma leitura mais nova. | **Corrigido** (`caffdba`). |
| Retomada depois do login cria o repositório mesmo com o plano falhando, e o erro some. | **Corrigido** (`caffdba`): só cria com o plano na tela; o erro do plano fica visível. |
| Convite com resposta que não é JSON: linha marcada como pronta e página parada. | **Corrigido** (`3c61c08`). |
| Termo que o regex casa por dobra de maiúsculas quebra a leitura. | **Corrigido** (`caffdba`): o trecho fica sem marca. Verificado com "ſQL". |
| Diferença do `Array.isArray` no `testGrid` entre as cópias. | **Fica.** A cópia real recebe dados do servidor e por isso confere; o protótipo usa dados fixos. |
| Criação lenta marca "parado" num botão já escondido. | **Fica.** O botão volta ao rótulo na mesma hora; o "parado" não aparece. |

### Tarefa 10 (painel real)

| Item | Decisão |
| --- | --- |
| Nome acessível do cartão longo. | **Corrigido** (`3916c66`). |
| Erro em `render()` depois de `line.done()` deixa a tela carregando. | **Corrigido** (`3916c66`): mostra o painel de erro com "Tentar de novo". |
| Ficha sem a linha da grade quando não há linha da missão atual. | **Fica.** Sem contagem não há o que mostrar; a ficha diz "Ainda não há um relatório de testes desta missão". |
| Grade dos cartões esmaecidos abaixo de 3:1 com filtro. | **Fica.** O esmaecido é o "fora do filtro"; a nota acima do quadro diz o que está sendo mostrado, e o cartão continua clicável e com o nome completo. |

### Tarefa 11 (topo da landing)

| Item | Decisão |
| --- | --- |
| Ramo `flip` morto no JS copiado. | Usado pela demo do repositório da Tarefa 12. |
| Decifrar continua um instante fora da tela. | **Fica.** Termina em menos de um segundo. |
| Verde do campo de pontos fora de "verde só para teste". | **Fica.** O `DESIGN.md` 3.3 pede pontos verdes perto do cursor, "como testes passando". |
| Pontos sob o texto do topo. | **Fica.** Os pontos em repouso são `--dot-empty` com transparência e esparsos; conferido na captura em 1440, o texto lê bem. |
| Topo mais alto que a tela em 375 px. | **Fica.** Em 390×844 o topo passa 7 px da dobra; o título, o texto e os dois botões ficam na primeira tela. |

### Tarefa 12 (resto da landing)

| Item | Decisão |
| --- | --- |
| `.demo-note` duplicado sobrescrevendo o topo. | **Corrigido** (`89ec4fd`). |
| `need` calculado uma vez (a demo pode ficar armada). | **Corrigido** (`89ec4fd`): recalculado quando a janela muda de tamanho. |
| Rótulo pisca ao retomar a demo. | **Fica.** A linha de status escreve o rótulo um quadro depois para ser anunciada; na landing ela é `aria-hidden`, mas a troca dura um quadro. |
| Raio de 2 px nas barras de dificuldade (`.diff i`). | **Fica.** Barras de 6 px de largura; com 4 px virariam pílulas. |
| `aria-label` no `<pre>`. | **Corrigido** (`89ec4fd`). |
| Estado final das dicas sem anúncio. | Não existe mais: a demo de dicas saiu na rodada de correção da Tarefa 12 (dicas são "a seguir"). |
| Arco da marca de etapa ~2,5:1 no escuro. | **Corrigido** (`e108809`). |
| Ícone da UE amarelo no rodapé. | **Fica.** É o amarelo do emblema da União Europeia, junto do crédito da ESCO, não o marca-texto. |
| `.btn:disabled` morto. | Já removido na rodada de correção da Tarefa 12. |

## Resultado final

Repassada final em 1440, 768 e 390 px, claro e escuro, mais movimento reduzido, nas 22 combinações de página e estado (176 cargas):

- **Rolagem horizontal da página:** nenhuma.
- **Contraste:** nenhum texto abaixo de 4,5:1 (3:1 no texto grande).
- **Movimento reduzido:** nenhuma animação rodando, nenhuma demo armada, nenhum texto decifrando; todo componente mostra o estado final.
- **Teclado:** em toda tela com botão principal, o Tab chega nele passando só por controles com contorno de foco visível. As telas sem ação (índice dos protótipos, e-mail, movimento, painel) não têm botão principal, como decidido na Tarefa 7.
- **Alvos de toque:** tudo com 44 px ou mais, menos os links dentro de frase do rodapé da landing e a caixa do aceite (rótulo clicável), já explicados acima. O link "Pular para o conteúdo" só aparece no foco, e aí tem 44 px.
- **Console:** sem exceções.
- **`./mvnw verify`:** BUILD SUCCESS, 420 testes.

## O que fica para depois

- Rótulos de competência no plano do professor e domínio no convite pedem mudanças na API.
- Os fluxos reais com o GitHub (login, instalação do app, criação do repositório, entrada na turma) só foram exercitados com respostas simuladas.
