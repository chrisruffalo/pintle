create table client (
    id varchar(255) not null,
    hostname varchar(255),
    ip varchar(255),
    query_count bigint,
    primary key (id)
);

CREATE INDEX client_ip ON client(ip);
CREATE INDEX client_hostname ON client(hostname);

create table question (
    id varchar(255) not null,
    average_milliseconds bigint,
    hostname varchar(255),
    query_count bigint,
    type varchar(255),
    primary key (id)
);

CREATE INDEX question_hostname ON question(hostname);
CREATE INDEX question_type ON question(type);
CREATE INDEX question_type_hostname ON question(type,hostname);