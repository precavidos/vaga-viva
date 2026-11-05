# Vaga Viva

O Vaga Viva transforma uma vaga de emprego real em um projeto de estudo que o aluno constrói no GitHub, guiado por missões e por um revisor que pergunta em vez de responder.

O glossário é em português, a língua do domínio; o código é em inglês e usa o nome indicado em _No código_.

## Pessoas

**Aluno**:
Quem recebe um projeto de estudo e o implementa missão a missão.
_Evitar_: usuário, estudante, candidato
_No código_: `Student`

**Professor**:
Quem cria uma turma e acompanha o progresso dos alunos dela.
_Evitar_: tutor, mentor, instrutor
_No código_: `Teacher`

**Turma**:
Grupo de alunos de um professor que recebem o mesmo projeto, gerado a partir de uma única vaga-base.
_Evitar_: classe, grupo, sala
_No código_: `Classroom`

**Testador**:
Aluno que participa do piloto com consentimento por escrito; é um papel temporário, não um tipo diferente de pessoa.
_Evitar_: beta tester, usuário piloto
_No código_: `Tester`

## Da vaga ao plano

**Vaga**:
Anúncio de emprego real que o aluno cola ou encaminha; é a entrada do sistema e não é guardada.
_Evitar_: oportunidade, job, anúncio
_No código_: `JobPosting`

**Vaga-base**:
A vaga que o professor escolhe para gerar o projeto de toda a turma.
_Evitar_: vaga da turma, vaga-modelo
_No código_: `BaseJobPosting`

**Competência**:
Habilidade do vocabulário de competências reconhecida em uma vaga; uma vaga nunca gera competências fora desse vocabulário.
_Evitar_: skill, habilidade (como termo técnico), requisito
_No código_: `Skill`

**Vocabulário de competências**:
Conjunto fechado de competências aceitas: as da ESCO mais uma lista curta própria.
_Evitar_: taxonomia, lista de skills
_No código_: `SkillVocabulary`

**Tecnologia nomeada**:
Tecnologia reconhecida na vaga só pelo próprio nome ou por termos listados para ela (“Java” não é “JavaScript”).
_Evitar_: stack (para um item isolado), ferramenta
_No código_: `NamedTechnology`

**Arquétipo**:
Tipo de projeto com tecnologia fixa (por exemplo, Java + Spring Boot) que define o repositório-modelo e as missões possíveis.
_Evitar_: template, trilha, stack
_No código_: `Archetype`

**Nível**:
Senioridade pedida pela vaga (por exemplo, estágio ou júnior), que ajusta a dificuldade do plano.
_Evitar_: senioridade, grau
_No código_: `SeniorityLevel`

**Plano do projeto**:
O domínio fictício e a sequência ordenada de missões escolhidos para uma vaga.
_Evitar_: roteiro, trilha, currículo
_No código_: `ProjectPlan`

**Domínio fictício**:
O negócio inventado onde o projeto se passa; cada arquétipo tem um só (no Java/Spring, agendamento de clínica).
_Evitar_: cenário, tema, case
_No código_: `FictionalDomain`

**Catálogo de missões**:
Todas as missões que o repositório-modelo de um arquétipo oferece; o plano do projeto escolhe entre elas.
_Evitar_: banco de missões, lista de exercícios
_No código_: `MissionCatalog`

**Teto de dificuldade**:
A dificuldade máxima das missões de um plano, definida pelo menor nível citado na vaga; vaga sem nível conta como júnior.
_Evitar_: limite, nível máximo
_No código_: `difficultyCap`

## O projeto do aluno

**Missão**:
Unidade de trabalho do plano, com objetivo claro e testes que falham até o aluno implementá-la; as missões vêm em ordem crescente de dificuldade.
_Evitar_: tarefa, exercício, desafio, issue
_No código_: `Mission`

**Missão central**:
Missão que entra em todo plano, qualquer que seja a vaga.
_Evitar_: missão obrigatória, missão base
_No código_: `Mission.core`

**Missão por competência**:
Missão opcional que entra no plano porque exercita competências pedidas pela vaga.
_Evitar_: missão bônus, missão extra
_No código_: `Reason.SKILL_MATCH`

**Missão padrão**:
Missão opcional usada para completar um plano quando a vaga casou com poucas competências.
_Evitar_: missão de reserva, coringa
_No código_: `Reason.DEFAULT`

**Repositório-modelo**:
O repositório de partida de um arquétipo, com o código inicial e os testes de todas as missões possíveis.
_Evitar_: template, boilerplate, scaffold
_No código_: `TemplateRepository`

**Solução de referência**:
Implementação privada que faz todos os testes do repositório-modelo passarem; serve para validar o modelo e nunca é mostrada ao aluno.
_Evitar_: gabarito, resposta
_No código_: `ReferenceSolution`

**Repositório do aluno**:
Cópia do repositório-modelo criada na conta do aluno, com o README e as missões do seu plano.
_Evitar_: projeto (sozinho), fork
_No código_: `StudentRepository`

## Revisão

**Revisor**:
O avaliador automático dos PRs do aluno, que responde com perguntas e dicas, nunca com código da solução.
_Evitar_: corretor, tutor, bot
_No código_: `Reviewer`

**Pergunta**:
Comentário do revisor que aponta o problema por meio de uma questão, sem dizer como resolvê-lo.
_Evitar_: feedback, correção
_No código_: `Question`

**Dica**:
Ajuda progressiva sobre uma missão, em 3 níveis liberados um por vez a pedido do aluno; nem o último nível contém a solução.
_Evitar_: hint, resposta, spoiler
_No código_: `Hint`

## Acompanhamento

**Painel da turma**:
Visão do professor com a missão atual de cada aluno e quem está travado.
_Evitar_: dashboard, relatório
_No código_: `ClassroomDashboard`

**Aluno travado**:
Aluno que está há tempo demais na mesma missão sem progresso; o limite ainda não foi definido.
_Evitar_: aluno parado, aluno atrasado
_No código_: `StuckStudent`
