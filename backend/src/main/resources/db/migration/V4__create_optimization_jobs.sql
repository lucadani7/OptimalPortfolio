-- Optimizations that run in the background; clients follow their progress over SSE
create table optimization_jobs
(
    id           uuid primary key,
    kind         varchar(20)              not null,
    status       varchar(20)              not null,
    progress     integer                  not null default 0,
    request_json jsonb                    not null,
    result_json  jsonb,
    error        varchar(1000),
    created_at   timestamp with time zone not null,
    started_at   timestamp with time zone,
    finished_at  timestamp with time zone,
    version      bigint                   not null default 0,

    constraint ck_optimization_jobs_kind check (kind in ('FRONTIER')),
    constraint ck_optimization_jobs_status check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED')),
    constraint ck_optimization_jobs_progress check (progress between 0 and 100),
    constraint ck_optimization_jobs_result check (status <> 'SUCCEEDED' or result_json is not null)
);

-- Old jobs are deleted by age
create index ix_optimization_jobs_created_at on optimization_jobs (created_at);
