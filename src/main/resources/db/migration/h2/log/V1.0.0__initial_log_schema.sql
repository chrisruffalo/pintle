create sequence log_item_SEQ start with 1 increment by 50;

create table log_item (
    id bigint not null,

    client_ip varchar(45),
    hostname character varying,
    type integer,
    rcode tinyint check (result between 0 and 23),

    service tinyint check (result between 0 and 2),

    result tinyint check (result between 0 and 3),

    elapsed_time integer,

    end_time timestamp(6) with time zone,
    start_time timestamp(6) with time zone,

    primary key (id)
);

CREATE INDEX log_item_type ON log_item(type);
CREATE INDEX log_item_result ON log_item(result);
CREATE INDEX log_item_service ON log_item(service);

create sequence log_answer_SEQ start with 1 increment by 50;

create table log_answer (
    id bigint not null,
    data character varying,
    type integer not null,
    log_item_id bigint,
    primary key (id)
);

alter table if exists log_answer
    add constraint fk_log_item_id
    foreign key (log_item_id)
    references log_item;