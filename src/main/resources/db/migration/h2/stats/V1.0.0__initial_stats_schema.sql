create table client (
    id bigint auto_increment,
    address character varying not null,
    query_count bigint,
    error_count bigint,
    total_milliseconds bigint,
    primary key (id)
);

create table question (
    id bigint auto_increment,
    type integer not null,
    hostname character varying not null,
    total_milliseconds bigint,
    query_count bigint,
    primary key (id)
);

CREATE INDEX question_type ON question(type);