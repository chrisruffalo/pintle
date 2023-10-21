create table log_item (
    id bigint auto_increment,

    -- identify the querying address by ip
    clientAddress character varying,

    -- the queried hostname (name)
    hostname character varying,

    -- the type, unfortunately, goes above 128
    type smallint,

    -- these codes do not need values over 128
    rcode tinyint check (result between 0 and 23),
    service tinyint check (result between 0 and 2),
    result tinyint check (result between 0 and 3),

    -- the time in ms that have elapsed since the start time, practically speaking this won't
    -- be longer than the timeout of ~5000. smallint can store enough ms to cover 30s. in
    -- practical terms as long as the timeout is less than 30s this would never be hit.
    elapsed_time smallint,

    -- the timestamp with precision down to hundredth's of a millisecond
    start_time timestamp(2) with time zone,

    primary key (id)
);

CREATE INDEX log_item_type ON log_item(type);
CREATE INDEX log_item_result ON log_item(result);
CREATE INDEX log_item_service ON log_item(service);

create table log_answer (
    id bigint auto_increment,
    type smallint,
    data character varying not null,
    log_item_id bigint,
    primary key (id)
);

alter table if exists log_answer
    add constraint fk_log_item_id
    foreign key (log_item_id)
    references log_item;