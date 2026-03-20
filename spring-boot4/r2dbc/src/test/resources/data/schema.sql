CREATE TABLE IF NOT EXISTS posts
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    title
    VARCHAR
(
    255
),
    content VARCHAR
(
    1024
),
    PRIMARY KEY
(
    id
)
    );

CREATE TABLE IF NOT EXISTS comments
(
    id
    BIGINT
    GENERATED
    BY
    DEFAULT AS
    IDENTITY,
    post_id
    BIGINT,
    content
    VARCHAR
(
    1024
),
    PRIMARY KEY
(
    id
)
    );
