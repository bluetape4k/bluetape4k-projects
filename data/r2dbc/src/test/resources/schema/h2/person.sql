drop table if exists Address;
drop table if exists Person;

create table Address
(
    address_id     int         not null,
    street_address varchar(50) not null,
    city           varchar(20) not null,
    state          varchar(2)  not null,
    primary key (address_id)
);

create table Person
(
    id         int         not null,
    first_name varchar(30) not null,
    last_name  varchar(30) null,
    birth_date date        not null,
    employed   BOOL        not null,
    occupation varchar(30) null,
    address_id int null,
    primary key (id)
);

insert into Address (address_id, street_address, city, state)
values (1, '123 Main Street', 'Bedrock', 'IN');

insert into Address (address_id, street_address, city, state)
values (2, '456 Main Street', 'Bedrock', 'IN');

insert into Person
values (1, 'Fred', 'Flintstone', '1935-02-01', true, 'Brontosaurus Operator', 1);

insert into Person
values (2, 'Wilma', 'Flintstone', '1940-02-01', true, 'Accountant', 1);

insert into Person(id, first_name, last_name, birth_date, employed, address_id)
values (3, 'Pebbles', 'Flintstone', '1960-05-06', false, 1);

insert into Person
values (4, 'Barney', 'Rubble', '1937-02-01', true, 'Brontosaurus Operator', 2);

insert into Person
values (5, 'Betty', 'Rubble', '1943-02-01', true, 'Engineer', 2);

insert into Person(id, first_name, last_name, birth_date, employed, address_id)
values (6, 'Bamm Bamm', 'Rubble', '1963-07-08', false, 2);
