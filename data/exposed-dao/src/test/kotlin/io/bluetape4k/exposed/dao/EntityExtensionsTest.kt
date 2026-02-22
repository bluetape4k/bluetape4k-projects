package io.bluetape4k.exposed.dao

import io.bluetape4k.exposed.shared.entities.BlogSchema.Post
import io.bluetape4k.exposed.shared.entities.BlogSchema.PostDetail
import io.bluetape4k.exposed.shared.entities.BlogSchema.blogTables
import io.bluetape4k.exposed.shared.entities.BoardSchema
import io.bluetape4k.exposed.shared.entities.BoardSchema.Boards
import io.bluetape4k.exposed.shared.entities.BoardSchema.Categories
import io.bluetape4k.exposed.shared.entities.BoardSchema.Posts
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class EntityExtensionsTest: AbstractExposedTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 Post 생성하기`(testDB: TestDB) {
        withTables(testDB, *blogTables) {

            val post = Post.new {
                title = "Post 1"

            }
            log.debug { "Post=$post" }

            // one-to-one 관계에서 ownership 을 가진 Post의 id 값을 지정합니다.
            val postDetail = PostDetail.new(post.id.value) {
                createdOn = LocalDate.now()
                createdBy = "admin"
            }
            log.debug { "PostDetail=$postDetail" }

            flushCache()
            entityCache.clear()

            val loadedPost = Post.findById(post.id)!!

            loadedPost shouldBeEqualTo post

            log.debug { "postDetails id table=${postDetail.id.table.tableName}" }     // post_details
            log.debug { "loadedPost.details id table=${loadedPost.detail.id.table.tableName}" }  // posts

            loadedPost.detail shouldBeEqualTo postDetail
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `flush 없이 Child 추가`(testDB: TestDB) {
        withTables(testDB, Boards, Posts, Categories) {

            val parent = BoardSchema.Post.new {
                this.category = BoardSchema.Category.new { title = "title" }
            }
            BoardSchema.Post.new { this.parent = parent }

            BoardSchema.Post.all().count() shouldBeEqualTo 2L

            /**
             * one-to-many의 count 만 따로 수행하는 쿼리가 된다.
             *
             * JPA 에서는 `@Formula` 를 사용해서 직접 쿼리를 작성해야 합니다.
             * ```
             * @Formula("(select count(p.id) from post p where p.parent_id= id)")
             * private var childrenCount: Long = 0
             * ```
             *
             * Hibernate의 `@LazyCollection(EXTRA)` 사용하면 되는데, Deprecated 되었습니다.
             *
             * ```sql
             * SELECT COUNT(*) FROM posts WHERE posts.parent_id = 1
             * ```
             */
            parent.children.count() shouldBeEqualTo 1L
        }
    }

    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `id 관련 확장 함수는 일관된 값을 제공한다`(testDB: TestDB) {
        withTables(testDB, *blogTables) {
            val post = Post.new {
                title = "Post with extensions"
            }

            post.idValue shouldBeEqualTo post.id._value
            post.idHashCode() shouldBeEqualTo post.idValue.hashCode()

            post.entityToStringBuilder().toString() shouldContain "id="
            post.toStringBuilder().toString() shouldContain "id="
        }
    }
}
