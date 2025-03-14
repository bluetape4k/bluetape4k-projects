package io.bluetape4k.exposed.dao

import io.bluetape4k.exposed.shared.entities.BlogSchema.Post
import io.bluetape4k.exposed.shared.entities.BlogSchema.PostDetail
import io.bluetape4k.exposed.shared.entities.BlogSchema.blogTables
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class EntityExtensionsTest: AbstractExposedTest() {

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create post by DAO`(testDB: TestDB) {
        withTables(testDB, *blogTables) {

            val post = Post.new { title = "Post 1" }
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
}
