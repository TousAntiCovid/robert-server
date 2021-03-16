drop table if exists submission_code;

create table submission_code(
id bigint not null , lot float  null,
code varchar(128) not null, type_code char(1) not null,
used boolean default FALSE, date_end_validity timestamp not null,
date_available timestamp  not null,
date_use timestamp,
date_generation timestamp,
CONSTRAINT codepositive_pk PRIMARY KEY(id)
);