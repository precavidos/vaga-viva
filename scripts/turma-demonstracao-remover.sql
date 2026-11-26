-- Remove a turma de demonstração criada por turma-demonstracao.sql, com os alunos fictícios e o progresso deles.
-- As chaves estrangeiras não têm cascata, por isso a ordem: progresso, matrículas, turma.

begin;

delete from enrollment_progress
where enrollment_id in (select e.id from enrollment e join classroom c on c.id = e.classroom_id where c.join_code = 'demovv2026');

delete from enrollment
where classroom_id in (select id from classroom where join_code = 'demovv2026');

delete from classroom where join_code = 'demovv2026';

commit;
