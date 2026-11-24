# Auditoria de acessibilidade com axe

Feita em 24/11/2026, como pede o `DESIGN.md` (seção 7): todas as telas, em 1440 px e 375 px, com o axe-core 4.10.2 e as regras WCAG 2.0, 2.1 e 2.2 nível A e AA, mais as boas práticas do axe.

## Como foi feita

As páginas reais de `src/main/resources/static/` e a landing (`site/index.html`) rodaram num servidor local que responde à API com dados fictícios no formato exato das respostas Java. O plano e a extração da vaga vieram das próprias classes do Vaga Viva. Assim deu para abrir cada estado de cada tela, inclusive os que exigem login e dados de turma. Cada tela foi auditada depois de terminar a animação de entrada, e também conferi se havia rolagem horizontal.

## Estados auditados

| Tela | Estados | 1440 px | 375 px |
| --- | --- | --- | --- |
| Aluno (`/`) | vaga vazia, plano pronto, erro de criação, repositório criado | ok | ok |
| Convite (`/turma/<código>`) | convite, erros do formulário, entrada concluída, convite inexistente | ok | ok |
| Professor (`/professor`) | sem login, nova turma, plano, detalhes, erros, turma criada | ok | ok |
| Painel (`/turmas/<id>`) | quadro, filtro por situação, ficha do aluno | corrigido | corrigido |
| Descadastro (`/descadastrar`) | antes e depois de confirmar | ok | ok |
| Landing (`site/index.html`) | página inteira, depois de rolar até o fim | ok | ok |

Nenhuma tela tem rolagem horizontal em 375 px.

## Achados e correções

1. **Painel, filtro por situação** (`color-contrast`, sério). Ao clicar num contador, os cartões das outras situações ficavam com `opacity: .28`, com contraste de 1,8 a 2,4:1, mas continuavam clicáveis. Agora o cartão esmaecido também fica `inert`: sai do clique, do Tab e do leitor de tela, e passa a contar como componente inativo (WCAG 1.4.3). O visual do `DESIGN.md` foi mantido, e a nota "Mostrando só: …" já dizia o que o filtro faz.
2. **Painel, ficha do aluno em 375 px** (`scrollable-region-focusable`). A ficha rola quando é mais alta que a tela, mas não tinha nada focável, e o teclado não conseguia rolar. `#drawer-body` agora é uma região com nome ("Detalhes do aluno") e `tabindex="0"`, e usa o anel de foco global.
3. **Convite, trilha de missões**: o contraste de 1,14:1 apontado no 4º item era a animação de entrada (`.stagger`) no meio do caminho. Com a tela parada, não há violação. Nada a corrigir.

## Limites

- O axe acha cerca de metade dos problemas de acessibilidade. Esta auditoria não substitui um teste com leitor de tela, que ainda não foi feito.
- Os e-mails (semanal do aluno e resumo do professor) não entraram: são HTML de e-mail, sem interação.
