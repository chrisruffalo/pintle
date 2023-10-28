-- a client produces questions for pintle to answer
create table client (
    id bigint generated by default as identity,
    address character varying not null,
    query_count bigint,
    error_count bigint,
    total_milliseconds bigint,
    primary key (id)
);

-- a question is a dns query (type/question)
create table question (
    id bigint generated by default as identity,
    type integer not null,
    hostname character varying not null,
    total_milliseconds bigint,
    query_count bigint,
    primary key (id)
);

CREATE INDEX question_type ON question(type);