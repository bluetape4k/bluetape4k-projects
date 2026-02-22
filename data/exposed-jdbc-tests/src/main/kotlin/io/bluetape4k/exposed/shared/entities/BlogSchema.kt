package io.bluetape4k.exposed.shared.entities

import io.bluetape4k.ToStringBuilder
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.SizedIterable

object BlogSchema {

    val blogTables = arrayOf(
        PostTable, PostDetailTable, PostCommentTable, PostTagTable, TagTable
    )

    /**
     * ```sql
     * -- Postgres:
     * CREATE TABLE IF NOT EXISTS posts (
     *      id BIGSERIAL PRIMARY KEY,
     *      title VARCHAR(255) NOT NULL
     * )
     * ```
     */
    object PostTable: LongIdTable("posts") {
        val title = varchar("title", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_details (
     *      id BIGINT NOT NULL,
     *      created_on DATE NOT NULL,
     *      created_by VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_post_details_id__id FOREIGN KEY (id)
     *      REFERENCES posts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    object PostDetailTable: IdTable<Long>("post_details") {
        override val id: Column<EntityID<Long>> = reference("id", PostTable)   // one-to-one relationship
        val createdOn = date("created_on")  // .defaultExpression(CurrentDate)
        val createdBy = varchar("created_by", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_comments (
     *      id BIGSERIAL PRIMARY KEY,
     *      post_id BIGINT NOT NULL,
     *      review VARCHAR(255) NOT NULL,
     *
     *      CONSTRAINT fk_post_comments_post_id__id FOREIGN KEY (post_id)
     *      REFERENCES posts(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * CREATE INDEX post_comments_post_id ON post_comments (post_id)
     * ```
     */
    object PostCommentTable: LongIdTable("post_comments") {
        val postId = reference("post_id", PostTable).index()
        val review = varchar("review", 255)
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS tags (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object TagTable: LongIdTable("tags") {
        val name = varchar("name", 255)
    }

    /**
     * Many-to-many relationship table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS post_tags (
     *      id BIGSERIAL PRIMARY KEY,
     *      post_id BIGINT NOT NULL,
     *      tag_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_post_tags_post_id__id FOREIGN KEY (post_id)
     *      REFERENCES posts(id) ON DELETE CASCADE ON UPDATE RESTRICT,
     *      CONSTRAINT fk_post_tags_tag_id__id FOREIGN KEY (tag_id)
     *      REFERENCES tags(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE post_tags
     *      ADD CONSTRAINT post_tags_post_id_tag_id_unique UNIQUE (post_id, tag_id);
     * ```
     */
    object PostTagTable: LongIdTable("post_tags") {
        val postId = reference("post_id", PostTable, onDelete = ReferenceOption.CASCADE)
        val tagId = reference("tag_id", TagTable, onDelete = ReferenceOption.CASCADE)

        init {
            uniqueIndex(postId, tagId)
        }
    }

    class Post(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Post>(PostTable)

        var title by PostTable.title

        val detail: PostDetail by PostDetail backReferencedOn PostDetailTable  // one-to-one relationship
        val comments: SizedIterable<PostComment> by PostComment referrersOn PostCommentTable
        val tags: SizedIterable<Tag> by Tag via PostTagTable // Tag.via (PostTagTable.post, PostTagTable.tag)

        override fun equals(other: Any?): Boolean = other is Post && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("title", title)
            .toString()
    }

    class PostDetail(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<PostDetail>(PostDetailTable)

        val post: Post by Post referencedOn PostDetailTable.id   // one-to-one relationship
        var createdOn by PostDetailTable.createdOn
        var createdBy by PostDetailTable.createdBy

        override fun equals(other: Any?): Boolean = other is PostDetail && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("post_id", post.id)
            .add("createdOn", createdOn)
            .add("createdBy", createdBy)
            .toString()
    }

    class PostComment(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<PostComment>(PostCommentTable)

        // one-to-many relationship
        var post by Post referencedOn PostCommentTable.postId
        var review by PostCommentTable.review

        override fun equals(other: Any?): Boolean = other is PostComment && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("post id", post.id)
            .add("review", review)
            .toString()
    }

    class Tag(id: EntityID<Long>): LongEntity(id) {
        companion object: LongEntityClass<Tag>(TagTable)

        var name by TagTable.name
        val posts: SizedIterable<Post> by Post via PostTagTable // Post.via(PostTagTable.tag, PostTagTable.post) 와 같다.

        override fun equals(other: Any?): Boolean = other is Tag && id._value == other.id._value
        override fun hashCode(): Int = id._value?.hashCode() ?: System.identityHashCode(this)
        override fun toString(): String = ToStringBuilder(this)
            .add("id", id._value)
            .add("name", name)
            .toString()
    }
}
