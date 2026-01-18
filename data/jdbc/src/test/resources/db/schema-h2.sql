CREATE TABLE IF NOT EXISTS test_bean
(
    id          INT AUTO_INCREMENT,
    description VARCHAR(1024),
    createdAt   TIMESTAMP Default current_timestamp,

    primary key (id)
);

DELETE
FROM test_bean;
