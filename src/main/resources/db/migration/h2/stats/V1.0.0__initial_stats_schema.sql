-- a client produces questions for pintle to answer
create table client (
    address character varying not null,
    query_count bigint,
    error_count bigint,
    total_milliseconds bigint,
    primary key (address)
);

-- a question is a dns query (type/question)
create table question (
    type integer not null,
    hostname character varying not null,
    total_milliseconds bigint,
    query_count bigint,
    primary key (type, hostname)
);

CREATE INDEX idx_question_hostname ON question(hostname);
CREATE INDEX idx_question_type ON question(type);
