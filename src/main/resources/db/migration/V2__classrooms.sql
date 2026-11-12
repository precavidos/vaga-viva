create table classroom (
    id uuid primary key,
    join_code varchar(32) not null unique,
    name varchar(120) not null,
    teacher_login varchar(100) not null,
    teacher_email varchar(254),
    teacher_email_consent_at timestamptz,
    plan jsonb not null,
    created_at timestamptz not null
);

create index classroom_teacher_login_idx on classroom (teacher_login);

create table enrollment (
    id uuid primary key,
    classroom_id uuid not null references classroom (id),
    student_login varchar(100) not null,
    display_name varchar(40) not null,
    repository_full_name varchar(200) not null unique,
    consented_at timestamptz not null,
    enrolled_at timestamptz not null,
    constraint enrollment_classroom_student_uk unique (classroom_id, student_login)
);

create table enrollment_progress (
    enrollment_id uuid primary key references enrollment (id),
    current_mission_id varchar(100),
    current_mission_since timestamptz,
    completed boolean not null default false,
    last_activity_at timestamptz,
    failing_tests jsonb,
    pull_requests_on_mission jsonb,
    last_reviewer_question text,
    last_pull_request_url varchar(500),
    hints_by_mission jsonb
);
