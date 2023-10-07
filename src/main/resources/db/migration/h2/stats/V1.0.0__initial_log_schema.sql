create table client (
    id varchar(40) not null,
    address varchar(45),
    query_count bigint,
    primary key (id)
);

CREATE INDEX client_ip ON client(address);

create table question (
    total_milliseconds bigint,
    hostname text,
    query_count bigint,
    type varchar(10),
    primary key (type,hostname)
);

CREATE INDEX question_hostname ON question(hostname);
CREATE INDEX question_type ON question(type);