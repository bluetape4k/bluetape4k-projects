drop table if exists OrderLine;
drop table if exists ItemMaster;
drop table if exists OrderDetail;
drop table if exists OrderMaster;
drop table if exists Users;

create table Users
(
    user_id   int         not null,
    user_name varchar(30) not null,
    parent_id int null,
    primary key (user_id)
);

create table OrderMaster
(
    order_id   int  not null,
    order_date date not null,
    primary key (order_id)
);

create table OrderDetail
(
    order_id    int         not null,
    line_number int         not null,
    description varchar(30) not null,
    quantity    int         not null,
    primary key (order_id, line_number)
);

create table ItemMaster
(
    item_id     int         not null,
    description varchar(30) not null,
    primary key (item_id)
);

create table OrderLine
(
    order_id    int not null,
    item_id     int not null,
    line_number int not null,
    quantity    int not null,
    primary key (order_id, item_id)
);

insert into OrderMaster(order_id, order_date)
values (1, '2017-01-17');
insert into OrderDetail(order_id, line_number, description, quantity)
values (1, 1, 'Tennis Ball', 3);
insert into OrderDetail(order_id, line_number, Description, quantity)
values (1, 2, 'Tennis Racket', 1);

insert into OrderMaster (order_id, order_date)
values (2, '2017-01-18');
insert into OrderDetail(order_id, line_number, description, quantity)
values (2, 1, 'Football', 2);

insert into ItemMaster(item_id, description)
values (22, 'Helmet');
insert into ItemMaster(item_id, description)
values (33, 'First Base Glove');
insert into ItemMaster(item_id, description)
values (44, 'Outfield Glove');
insert into ItemMaster(item_id, description)
values (55, 'Catcher Glove');

insert into OrderLine(order_id, item_id, line_number, quantity)
values (1, 22, 1, 1);
insert into OrderLine(order_id, item_id, line_number, quantity)
values (1, 33, 1, 1);
insert into OrderLine(order_id, item_id, line_number, quantity)
values (2, 22, 1, 1);
insert into OrderLine(order_id, item_id, line_number, quantity)
values (2, 44, 2, 1);
insert into OrderLine(order_id, item_id, line_number, quantity)
values (2, 66, 3, 6);

insert into Users(user_id, user_name)
values (1, 'Fred');
insert into Users(user_id, user_name)
values (2, 'Barney');
insert into Users(user_id, user_name, parent_id)
values (3, 'Pebbles', 1);
insert into Users(user_id, user_name, parent_id)
values (4, 'Bamm Bamm', 2);
