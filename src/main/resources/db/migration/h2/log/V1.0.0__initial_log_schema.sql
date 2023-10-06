create table log_item (
    id varchar(255) not null,

    trace varchar(255),

    answer blob,
    question blob,

    result varchar(255) check (result in ('ERROR','RESOLVED','CACHED')),

    elapsed_time bigint,

    end_time timestamp(6) with time zone,
    start_time timestamp(6) with time zone,

    primary key (id)
);

CREATE INDEX log_item_trace ON log_item(trace);