create table if not exists identity
(
    id             bigserial    not null
    constraint identity_pkey
    primary key,
    creation_time  timestamp,
    last_update    timestamp,
    ida            varchar(255) not null
    constraint uk_q0fdtk2nsfxy44lq45iopucma
    unique,
    key_for_mac    varchar(255) not null,
    key_for_tuples varchar(255) not null
    );

create index if not exists idx_ida
    on identity (ida);