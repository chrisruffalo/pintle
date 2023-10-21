create table list (
    id bigint auto_increment,
    action tinyint check (action between 0 and 1),
    last_configuration_id char varying,
    name char varying,
    type tinyint check (type between 0 and 1),
    cache_hash char varying,
    primary key (id)
);

create table source (
    id bigint auto_increment,
    uri character varying not null,
    compression character varying,
    cache_until timestamp(2) with time zone,
    cache_path character varying,
    etag character varying,
    hash character varying,
    primary key (uri)
);

create table line (
   id bigint auto_increment not null,
   list_id bigint not null,
   source_id bigint not null,
   hostname char varying not null,
   resolve_to char varying(39),
   primary key (list_id,source_id,hostname)
);

CREATE INDEX line_list_id ON line(list_id);
CREATE INDEX line_source_id ON line(source_id);
CREATE INDEX line_hostname ON line(hostname);