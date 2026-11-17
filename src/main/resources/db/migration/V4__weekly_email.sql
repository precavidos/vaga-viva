create table weekly_email_subscription (
    student_login varchar(100) primary key,
    email varchar(254) not null,
    repository_full_name varchar(200) not null,
    consented_at timestamptz not null,
    unsubscribe_token varchar(64) not null unique,
    last_sent_at timestamptz
);
