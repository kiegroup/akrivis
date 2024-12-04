create table job
(
    id         serial primary key,
    type       varchar(255) not null,
    endpoint   varchar(255) not null,
    cron       varchar(255) not null,
    status     varchar(255) not null,
    created_at timestamp    not null default now(),
    last_run   timestamp null
);

create table raw_data
(
    id           serial primary key,
    data         jsonb     not null,
    created_at   timestamp not null default now(),
    fk_job serial    not null,
    foreign key (fk_job) references job (id)
);

create table card_configuration
(
    id         serial primary key,
    definition jsonb not null
);

create table card
(
    id                    serial primary key,
    definition            jsonb not null,
    configuration_id serial not null,
    foreign key (configuration_id) references card_configuration (id)
);

create table run_result
(
    id                 serial primary key,
    status             varchar(255) not null,
    measure_name       varchar(255) not null,
    measure_value      jsonb        not null,
    run_time           timestamp    not null default now(),
    card_data          jsonb        not null,
    configuration_data jsonb        not null,
    card_id            serial       not null,
    foreign key (card_id) references card (id)
);

