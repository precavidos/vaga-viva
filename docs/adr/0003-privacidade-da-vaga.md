# Privacidade da vaga e dados mínimos do aluno

O texto da vaga é lido em memória e descartado depois da geração do plano; guardamos só as competências reconhecidas, o nível e o arquétipo. Do aluno ficam apenas o login do GitHub e o e-mail, com consentimento, e, para quem pede o e-mail semanal, o repositório e um token de descadastro; o descadastro apaga tudo. Isso reduz o risco perante a LGPD, já que vagas podem trazer nomes de recrutadores e contatos, e evita guardar dados de terceiros na demo e no vídeo, o que o regulamento proíbe.

## Empresas seguidas (27/11/2026)

Quem recebe o e-mail semanal pode seguir até 3 empresas. Do aluno ficam, além do que já estava acima, o link da página de vagas de cada empresa e o nome que ele deu a ela; o descadastro do e-mail semanal apaga tudo isso. Das páginas seguidas ficam só dados públicos, sem ligação com o aluno: o link da página e os links e títulos das vagas listadas nela. O título vem do texto do link na lista, nunca do texto da vaga, que continua lido só em memória.

## Consequences

- Não dá para regerar um plano a partir da vaga original: o aluno precisa colar a vaga de novo.
- A validação da extração (com 20 vagas rotuladas) usa um conjunto separado, guardado fora do sistema.
- Vagas recebidas por e-mail encaminhado seguem a mesma regra: o corpo da mensagem não é armazenado.
