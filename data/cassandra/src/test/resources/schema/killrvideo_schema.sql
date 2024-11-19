// User credentials, keyed by email address so we can authenticate
CREATE TABLE IF NOT EXISTS user_credentials
(
    email
    text,
    password
    text,
    user_id
    uuid,
    PRIMARY
    KEY
(
    email
)
    );

// Users keyed by id
CREATE TABLE IF NOT EXISTS users
(
    user_id
    uuid,
    firstname
    text,
    lastname
    text,
    email
    text,
    created_date
    timestamp,
    PRIMARY
    KEY
(
    user_id
)
    );

// Videos by id
CREATE TABLE IF NOT EXISTS videos
(
    video_id
    uuid,
    user_id
    uuid,
    name
    text,
    description
    text,
    location
    text,
    location_type
    int,
    preview_image_location
    text,
    tags
    set<
    text >,
    added_date
    timestamp,
    PRIMARY
    KEY
(
    video_id
)
    );

// One-to-many from user point of view (lookup table)
CREATE TABLE IF NOT EXISTS user_videos
(
    user_id
    uuid,
    added_date
    timestamp,
    video_id
    uuid,
    name
    text,
    preview_image_location
    text,
    PRIMARY
    KEY
(
    user_id,
    added_date,
    video_id
)
    )
    WITH CLUSTERING ORDER BY
        (
        added_date
        DESC,
        video_id
        ASC
        );

// Track latest videos, grouped by day (if we ever develop a bad hotspot from the daily grouping here, we could mitigate by
// splitting the row using an arbitrary group number, making the partition key (yyyymmdd, group_number))
CREATE TABLE IF NOT EXISTS latest_videos
(
    yyyymmdd
    text,
    added_date
    timestamp,
    video_id
    uuid,
    user_id
    uuid,
    name
    text,
    preview_image_location
    text,
    PRIMARY
    KEY
(
    yyyymmdd,
    added_date,
    video_id
)
    )
    WITH CLUSTERING ORDER BY
        (
        added_date
        DESC,
        video_id
        ASC
        );

// Video ratings (counter table)
CREATE TABLE IF NOT EXISTS video_ratings
(
    video_id
    uuid,
    rating_counter
    counter,
    rating_total
    counter,
    PRIMARY
    KEY
(
    video_id
)
    );

// Video ratings by user (to try and mitigate voting multiple times)
CREATE TABLE IF NOT EXISTS video_ratings_by_user
(
    video_id
    uuid,
    user_id
    uuid,
    rating
    int,
    PRIMARY
    KEY
(
    video_id,
    user_id
)
    );

// Records the number of views/playbacks of a video
CREATE TABLE IF NOT EXISTS video_playback_stats
(
    video_id
    uuid,
    views
    counter,
    PRIMARY
    KEY
(
    video_id
)
    );

// Recommendations by user (powered by Spark), with the newest videos added to the site always first
CREATE TABLE IF NOT EXISTS video_recommendations
(
    user_id
    uuid,
    added_date
    timestamp,
    video_id
    uuid,
    rating
    float,
    authorid
    uuid,
    name
    text,
    preview_image_location
    text,
    PRIMARY
    KEY
(
    user_id,
    added_date,
    video_id
)
    )
    WITH CLUSTERING ORDER BY
        (
        added_date
        DESC,
        video_id
        ASC
        );

// Recommendations by video (powered by Spark)
CREATE TABLE IF NOT EXISTS video_recommendations_by_video
(
    video_id
    uuid,
    user_id
    uuid,
    rating
    float,
    added_date
    timestamp
    STATIC,
    authorid
    uuid
    STATIC,
    name
    text
    STATIC,
    preview_image_location
    text
    STATIC,
    PRIMARY
    KEY
(
    video_id,
    user_id
)
    );

// Index for tag keywords
CREATE TABLE IF NOT EXISTS videos_by_tag
(
    tag
    text,
    video_id
    uuid,
    added_date
    timestamp,
    user_id
    uuid,
    name
    text,
    preview_image_location
    text,
    tagged_date
    timestamp,
    PRIMARY
    KEY
(
    tag,
    video_id
)
    );

// Index for tags by first letter in the tag
CREATE TABLE IF NOT EXISTS tags_by_letter
(
    first_letter
    text,
    tag
    text,
    PRIMARY
    KEY
(
    first_letter,
    tag
)
    );

// Comments for a given video
CREATE TABLE IF NOT EXISTS comments_by_video
(
    video_id
    uuid,
    comment_id
    timeuuid,
    user_id
    uuid,
    comment
    text,
    PRIMARY
    KEY
(
    video_id,
    comment_id
)
    )
    WITH CLUSTERING ORDER BY
        (
        comment_id DESC
        );

// Comments for a given user
CREATE TABLE IF NOT EXISTS comments_by_user
(
    user_id
    uuid,
    comment_id
    timeuuid,
    video_id
    uuid,
    comment
    text,
    PRIMARY
    KEY
(
    user_id,
    comment_id
)
    )
    WITH CLUSTERING ORDER BY
        (
        comment_id DESC
        );
