drop table if exists Address;
drop table if exists Person;
drop table if exists Person2;

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
    person_id  int         not null,
    first_name varchar(20) not null,
    last_name  varchar(20) not null,
    gender     varchar(20) not null,
    human_flag char(1)     not null,
    age        int         not null,
    address_id int         not null,
    primary key (person_id)
);

create table Person2
(
    person_id  int         not null,
    first_name varchar(20) not null,
    last_name  varchar(20) not null,
    gender     varchar(20) not null,
    human_flag char(1)     not null,
    age        int         not null,
    address_id int         not null,
    primary key (person_id)
);

insert into Address (address_id, street_address, city, state)
values (1, '123 Main Street', 'Bedrock', 'IN');

insert into Address (address_id, street_address, city, state)
values (2, '456 Main Street', 'Bedrock', 'IN');

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (1, 'Fred', 'Flintstone', 'Male', 'Y', 47, 1);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (2, 'Wilma', 'Flintstone', 'Female', 'Y', 42, 1);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (3, 'Barney', 'Rubble', 'Male', 'Y', 45, 2);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (4, 'Betty', 'Rubble', 'Female', 'Y', 37, 2);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (5, 'Bamm Bamm', 'Rubble', 'Male', 'Y', 3, 2);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (6, 'Pebbles', 'Flintstone', 'Female', 'Y', 2, 1);

insert into Person (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (7, 'Dino', 'Flintstone', 'Male', 'N', 5, 1);

insert into Person2 (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (1, 'Sam', 'Smith', 'Male', 'N', 30, 1);

insert into Person2 (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (2, 'Suzy', 'Smith', 'Female', 'N', 33, 1);

insert into Person2 (person_id, first_name, last_name, gender, human_flag, age, address_id)
values (3, 'Joe', 'Jones', 'Male', 'N', 29, 2);
