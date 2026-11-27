-- A company's public career page, shared by every student who follows it. No student data.
create table career_page (
    id bigserial primary key,
    url varchar(500) not null unique,
    consecutive_failures integer not null default 0,
    last_checked_at timestamptz,
    created_at timestamptz not null
);

-- A student following a company. Unsubscribing from the weekly e-mail deletes their follows.
create table company_follow (
    student_login varchar(100) not null references weekly_email_subscription (student_login) on delete cascade,
    career_page_id bigint not null references career_page (id) on delete cascade,
    name varchar(80) not null,
    created_at timestamptz not null,
    primary key (student_login, career_page_id)
);

create index company_follow_career_page_idx on company_follow (career_page_id);

-- The posting links already seen on a page. Public, no student data; purged 90 days after last seen.
-- alert marks a tech posting found by the weekly check, which the next weekly e-mail shows.
create table seen_posting (
    career_page_id bigint not null references career_page (id) on delete cascade,
    url varchar(500) not null,
    title varchar(150) not null default '',
    alert boolean not null default false,
    archetype varchar(50),
    first_seen_at timestamptz not null,
    last_seen_at timestamptz not null,
    primary key (career_page_id, url)
);

create index seen_posting_last_seen_idx on seen_posting (last_seen_at);

-- A page nobody follows any more goes at once, with its links, whatever deleted the last follow:
-- an unfollow, an unsubscribe or the end of the project.
create function delete_unfollowed_career_pages() returns trigger
language plpgsql as $$
begin
    delete from career_page p
    where not exists (select 1 from company_follow f where f.career_page_id = p.id);
    return null;
end;
$$;

create trigger company_follow_deleted
    after delete on company_follow
    for each statement execute function delete_unfollowed_career_pages();
