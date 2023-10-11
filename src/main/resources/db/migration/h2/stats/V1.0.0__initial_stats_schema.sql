create table client (
    id varchar(40) not null,
    address varchar(45),
    query_count bigint,
    error_count bigint,
    total_milliseconds bigint,
    primary key (id)
);

create table question (
    type integer,
    hostname character varying,
    total_milliseconds bigint,
    query_count bigint,
    primary key (type,hostname)
);

CREATE INDEX question_type ON question(type);