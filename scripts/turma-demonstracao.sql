-- Turma de demonstração do Vaga Viva, com alunos FICTÍCIOS, para gravar o painel (cena 4 do vídeo)
-- e testar o resumo semanal do professor.
--
-- Como usar: no console do Neon, abra o SQL Editor da branch production, troque SEU_EMAIL_AQUI
-- pelo e-mail que deve receber o resumo e rode o script inteiro. A última linha mostra o endereço do painel.
-- Os tempos são relativos ao momento em que o script roda: grave em até 2 dias, ou rode de novo
-- (depois de turma-demonstracao-remover.sql) para os alunos voltarem às mesmas situações.
-- Os repositórios demo-vaga-viva/... não existem: não clique nos links do GitHub da ficha durante a gravação.

begin;

with turma as (
  insert into classroom (id, join_code, name, teacher_login, teacher_email, teacher_email_consent_at, plan, created_at)
  values (gen_random_uuid(), 'demovv2026', 'Turma de demonstração', 'davsilvam', 'SEU_EMAIL_AQUI', now(),
          '{"archetype":"java-spring","archetypeFit":{"archetype":"java-spring","status":"COVERED","covered":["Java","Spring Boot"],"uncovered":[]},"seniorityLevel":"JUNIOR","difficultyCap":3,"missions":[{"id":"patients-api","reason":"CORE","matchedSkills":["java","spring","api-design"]},{"id":"persistence","reason":"CORE","matchedSkills":["postgresql","sql"]},{"id":"pagination","reason":"SKILL_MATCH","matchedSkills":["api-design","spring"]},{"id":"ci-pipeline","reason":"SKILL_MATCH","matchedSkills":["continuous-integration","version-control"]},{"id":"scheduling-rules","reason":"CORE","matchedSkills":["automated-testing"]},{"id":"availability-query","reason":"SKILL_MATCH","matchedSkills":["sql","postgresql"]}],"postingSkills":["java","automated-testing","api-design","sql","postgresql","version-control","spring","containers","continuous-integration"],"postingTechnologies":["java","spring-boot","sql","postgresql","git","junit","rest","docker","ci-tools"]}'::jsonb, now() - interval '8 days')
  returning id
),
alunos (login, nome, missao, ha_quanto_na_missao, ultima_atividade, concluido, testes_falhando, prs, pergunta, dicas, contagem) as (
  values
    ('demo-ana', 'Ana', 'availability-query', interval '2 days', interval '1 day', true, '[]'::jsonb, '[11,12]'::jsonb, null, '{"pagination":1}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 4, "total": 4}, "pagination": {"passed": 5, "total": 5}, "ci-pipeline": {"passed": 3, "total": 3}, "scheduling-rules": {"passed": 6, "total": 6}, "availability-query": {"passed": 5, "total": 5}}'::jsonb),
    ('demo-bruno', 'Bruno', 'scheduling-rules', interval '1 day', interval '2 hours', false, '[{"name":"rejectsOverlappingAppointments","message":"esperava 409 para uma consulta sobreposta, veio 201"}]'::jsonb, '[6]'::jsonb, 'O que define que duas consultas do mesmo profissional se sobrepõem?', '{}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 4, "total": 4}, "pagination": {"passed": 5, "total": 5}, "ci-pipeline": {"passed": 3, "total": 3}, "scheduling-rules": {"passed": 4, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb),
    ('demo-carla', 'Carla', 'pagination', interval '4 days', interval '1 day', false, '[{"name":"listsNewestFirst","message":"esperava a consulta mais recente primeiro, veio a mais antiga"},{"name":"countsTotalPages","message":"esperava 3 páginas no total, veio 1"}]'::jsonb, '[7,8,9]'::jsonb, 'Quando duas consultas caem no mesmo dia, o que decide qual vem primeiro?', '{"pagination":2}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 4, "total": 4}, "pagination": {"passed": 2, "total": 5}, "ci-pipeline": {"passed": 0, "total": 3}, "scheduling-rules": {"passed": 0, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb),
    ('demo-diego', 'Diego', 'persistence', interval '6 days', interval '6 days', false, '[{"name":"apiReadsFromDatabase","message":"esperava o nome que está no banco"}]'::jsonb, '[3]'::jsonb, 'De onde a API lê os pacientes hoje, e de onde o contrato diz que deveria ler?', '{}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 1, "total": 4}, "pagination": {"passed": 0, "total": 5}, "ci-pipeline": {"passed": 0, "total": 3}, "scheduling-rules": {"passed": 0, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb),
    ('demo-elisa', 'Elisa', 'ci-pipeline', interval '6 hours', interval '1 hour', false, '[{"name":"runsOnPullRequest","message":"esperava o workflow ci.yml disparando em pull_request"}]'::jsonb, '[10]'::jsonb, 'Em quais eventos do GitHub o seu workflow precisa rodar?', '{}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 4, "total": 4}, "pagination": {"passed": 5, "total": 5}, "ci-pipeline": {"passed": 1, "total": 3}, "scheduling-rules": {"passed": 0, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb),
    ('demo-felipe', 'Felipe', 'persistence', interval '3 days 5 hours', interval '3 hours', false, '[{"name":"apiWritesToDatabase","message":"esperava encontrar na tabela patients a linha com id 17"},{"name":"updateAndDeleteReachDatabase","message":"esperava 0 linhas depois de remover, veio 1"}]'::jsonb, '[2,3,4,5]'::jsonb, 'Quando a API remove um paciente, o que precisa acontecer com a linha na tabela?', '{"persistence":3}'::jsonb, '{"patients-api": {"passed": 5, "total": 5}, "persistence": {"passed": 2, "total": 4}, "pagination": {"passed": 0, "total": 5}, "ci-pipeline": {"passed": 0, "total": 3}, "scheduling-rules": {"passed": 0, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb),
    ('demo-gabriela', 'Gabriela', 'patients-api', interval '1 day', interval '5 hours', false, '[{"name":"updatesAPatient","message":"esperava status 200 ao alterar o paciente, veio 405"}]'::jsonb, '[1]'::jsonb, 'O que acontece quando um endpoint existe para um verbo HTTP e não para outro?', '{"patients-api":1}'::jsonb, '{"patients-api": {"passed": 3, "total": 5}, "persistence": {"passed": 0, "total": 4}, "pagination": {"passed": 0, "total": 5}, "ci-pipeline": {"passed": 0, "total": 3}, "scheduling-rules": {"passed": 0, "total": 6}, "availability-query": {"passed": 0, "total": 5}}'::jsonb)
),
matriculas as (
  insert into enrollment (id, classroom_id, student_login, display_name, repository_full_name, consented_at, enrolled_at)
  select gen_random_uuid(), turma.id, alunos.login, alunos.nome, 'demo-vaga-viva/' || alunos.login,
         now() - interval '8 days', now() - interval '8 days'
  from turma, alunos
  returning id, student_login
)
insert into enrollment_progress (enrollment_id, current_mission_id, current_mission_since, completed, last_activity_at,
                                 failing_tests, pull_requests_on_mission, last_reviewer_question, last_pull_request_url,
                                 hints_by_mission, test_tallies)
select matriculas.id, alunos.missao, now() - alunos.ha_quanto_na_missao, alunos.concluido, now() - alunos.ultima_atividade,
       alunos.testes_falhando, alunos.prs, alunos.pergunta,
       'https://github.com/demo-vaga-viva/' || alunos.login || '/pull/1', alunos.dicas, alunos.contagem
from matriculas join alunos on alunos.login = matriculas.student_login;

commit;

select 'https://vaga-viva-49kj.onrender.com/turmas/' || id as painel from classroom where join_code = 'demovv2026';
