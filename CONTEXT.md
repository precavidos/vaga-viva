# Vaga Viva

O Vaga Viva transforma uma vaga de emprego real em um projeto de estudo que o aluno constrói no GitHub, guiado por missões e por um revisor que pergunta em vez de responder.

## Pessoas

**Aluno**:
Quem recebe um projeto de estudo e o implementa missão a missão.
_Evitar_: usuário, estudante, candidato

**Professor**:
Quem cria uma turma e acompanha o progresso dos alunos dela.
_Evitar_: tutor, mentor, instrutor

**Turma**:
Grupo de alunos de um professor que recebem o mesmo projeto, gerado a partir de uma única vaga-base.
_Evitar_: classe, grupo, sala

**Testador**:
Aluno que participa do piloto com consentimento por escrito; é um papel temporário, não um tipo diferente de pessoa.
_Evitar_: beta tester, usuário piloto

## Da vaga ao plano

**Vaga**:
Anúncio de emprego real que o aluno cola ou encaminha; é a entrada do sistema e não é guardada.
_Evitar_: oportunidade, job, anúncio

**Vaga-base**:
A vaga que o professor escolhe para gerar o projeto de toda a turma.
_Evitar_: vaga da turma, vaga-modelo

**Competência**:
Habilidade do vocabulário de competências reconhecida em uma vaga; uma vaga nunca gera competências fora desse vocabulário.
_Evitar_: skill, habilidade (como termo técnico), requisito

**Vocabulário de competências**:
Conjunto fechado de competências aceitas: as da ESCO mais uma lista curta própria.
_Evitar_: taxonomia, lista de skills

**Tecnologia nomeada**:
Tecnologia reconhecida na vaga só pelo próprio nome ou por termos listados para ela (“Java” não é “JavaScript”).
_Evitar_: stack (para um item isolado), ferramenta

**Arquétipo**:
Tipo de projeto com tecnologia fixa (por exemplo, Java + Spring Boot) que define o repositório-modelo e as missões possíveis.
_Evitar_: template, trilha, stack

**Nível**:
Senioridade pedida pela vaga (por exemplo, estágio ou júnior), que ajusta a dificuldade do plano.
_Evitar_: senioridade, grau

**Plano do projeto**:
O domínio fictício e a sequência ordenada de missões escolhidos para uma vaga.
_Evitar_: roteiro, trilha, currículo

**Domínio fictício**:
O negócio inventado onde o projeto se passa (por exemplo, agendamento de clínica).
_Evitar_: cenário, tema, case

## O projeto do aluno

**Missão**:
Unidade de trabalho do plano, com objetivo claro e testes que falham até o aluno implementá-la; as missões vêm em ordem crescente de dificuldade.
_Evitar_: tarefa, exercício, desafio, issue

**Repositório-modelo**:
O repositório de partida de um arquétipo, com o código inicial e os testes de todas as missões possíveis.
_Evitar_: template, boilerplate, scaffold

**Solução de referência**:
Implementação privada que faz todos os testes do repositório-modelo passarem; serve para validar o modelo e nunca é mostrada ao aluno.
_Evitar_: gabarito, resposta

**Repositório do aluno**:
Cópia do repositório-modelo criada na conta do aluno, com o README e as missões do seu plano.
_Evitar_: projeto (sozinho), fork

## Revisão

**Revisor**:
O avaliador automático dos PRs do aluno, que responde com perguntas e dicas, nunca com código da solução.
_Evitar_: corretor, tutor, bot

**Pergunta**:
Comentário do revisor que aponta o problema por meio de uma questão, sem dizer como resolvê-lo.
_Evitar_: feedback, correção

**Dica**:
Ajuda progressiva sobre uma missão, em 3 níveis liberados um por vez a pedido do aluno; nem o último nível contém a solução.
_Evitar_: hint, resposta, spoiler

## Acompanhamento

**Painel da turma**:
Visão do professor com a missão atual de cada aluno e quem está travado.
_Evitar_: dashboard, relatório

**Aluno travado**:
Aluno que está há tempo demais na mesma missão sem progresso; o limite ainda não foi definido.
_Evitar_: aluno parado, aluno atrasado
