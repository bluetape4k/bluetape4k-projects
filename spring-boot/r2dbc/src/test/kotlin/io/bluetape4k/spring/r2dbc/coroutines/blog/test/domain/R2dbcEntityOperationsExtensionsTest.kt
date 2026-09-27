package io.bluetape4k.spring.r2dbc.coroutines.blog.test.domain

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Post
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import io.bluetape4k.spring.r2dbc.coroutines.countAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.deleteSuspending
import io.bluetape4k.spring.r2dbc.coroutines.existsSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.insertSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectOneSuspending
import io.bluetape4k.spring.r2dbc.coroutines.updateSuspending
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.query.isEqual

class R2dbcEntityOperationsExtensionsTest(
    @param:Autowired private val operations: R2dbcEntityOperations,
): AbstractR2dbcBlogApplicationTest() {

    @Test
    fun `select and count extensions`() = runSuspendIO {
        val count = operations.countAllSuspending<Post>()
        count shouldBeGreaterOrEqualTo 2

        val posts = operations.selectAllSuspending<Post>().log("post").toList()
        posts.shouldNotBeEmpty()
    }

    @Test
    fun `find one by id variants`() = runSuspendIO {
        val post = operations.findOneByIdSuspending<Post>(1L)
        post.id shouldBeEqualTo 1L

        operations.findOneByIdOrNullSuspending<Post>(-1L).shouldBeNull()
    }

    @Test
    fun `insert update delete extensions`() = runSuspendIO {
        val newPost = createPost()
        val savedPost = operations.insertSuspending(newPost)
        log.debug { "saved new post: $newPost" }
        savedPost.id.shouldNotBeNull()

        val query = Query.query(Criteria.where(Post::id.name).isEqual(savedPost.id))
        val updated = operations.updateSuspending<Post>(query, Update.update("title", "Updated"))
        updated shouldBeEqualTo 1L

        val loaded = operations.selectOneSuspending<Post>(query).shouldNotBeNull()
        log.debug { "loaded post: $loaded" }
        loaded.title shouldBeEqualTo "Updated"

        val deleted = operations.deleteSuspending<Post>(query)
        deleted shouldBeEqualTo 1L

        operations.existsSuspending<Post>(query).shouldBeFalse()
    }
}
