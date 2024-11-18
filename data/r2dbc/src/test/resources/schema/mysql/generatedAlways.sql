drop table if exists GeneratedAlways;

create table GeneratedAlways
(
    id         BIGINT AUTO_INCREMENT NOT NULL,
    first_name varchar(30) not null,
    last_name  varchar(30) not null,
    full_name  varchar(255) as (concat(first_name, ' ', last_name)),
    primary key (id)
);
