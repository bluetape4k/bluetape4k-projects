CREATE TABLE IF NOT EXISTS test_bean
(
    id
    INT
    IDENTITY
    PRIMARY
    KEY,
    description
    VARCHAR
(
    1024
),
    createdAt TIMESTAMP Default current_timestamp
    );
DELETE
FROM test_bean;
