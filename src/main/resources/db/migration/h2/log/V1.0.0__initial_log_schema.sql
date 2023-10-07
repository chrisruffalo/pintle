create sequence log_item_SEQ start with 1 increment by 50;

create table log_item (
    id bigint not null,

    answer blob,

    client_ip varchar(45),
    hostname text,
    type varchar(8),
    rcode varchar(10),

    service varchar(4),

    result varchar(15) check (result in ('ERROR','RESOLVED','CACHED')),

    elapsed_time bigint,

    end_time timestamp(6) with time zone,
    start_time timestamp(6) with time zone,

    primary key (id)
);

CREATE INDEX log_item_hostname ON log_item(hostname);
CREATE INDEX log_item_result ON log_item(result);
CREATE INDEX log_item_type ON log_item(type);
CREATE INDEX log_item_rcode ON log_item(rcode);
CREATE INDEX log_item_service ON log_item(service);
CREATE INDEX log_item_client_ip ON log_item(client_ip);