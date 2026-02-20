package io.bluetape4k.spring.r2dbc.coroutines.blog.test.domain

import io.bluetape4k.spring.r2dbc.coroutines.countAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.deleteSuspending
import io.bluetape4k.spring.r2dbc.coroutines.existsSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.insertSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectOneSuspending
import io.bluetape4k.spring.r2dbc.coroutines.updateSuspending
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Post
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldBeNull
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
    fun `select and count extensions`() = runTest {
        val count = operations.countAllSuspending<Post>()
        count shouldBeGreaterOrEqualTo 2

        val posts = operations.selectAllSuspending<Post>().toList()
        posts.shouldNotBeEmpty()
    }

    @Test
    fun `find one by id variants`() = runTest {
        val post = operations.findOneByIdSuspending<Post>(1L)
        post.id shouldBeEqualTo 1L

        operations.findOneByIdOrNullSuspending<Post>(-1L).shouldBeNull()
    }

    @Test
    fun `insert update delete extensions`() = runTest {
        val newPost = createPost()
        val savedPost = operations.insertSuspending(newPost)
        savedPost.id.shouldNotBeNull()

        val query = Query.query(Criteria.where(Post::id.name).isEqual(savedPost.id))
        val updated = operations.updateSuspending<Post>(query, Update.update("title", "Updated"))
        updated shouldBeEqualTo 1L

        val loaded = operations.selectOneSuspending<Post>(query)
        loaded.title shouldBeEqualTo "Updated"

        val deleted = operations.deleteSuspending<Post>(query)
        deleted shouldBeEqualTo 1L

        operations.existsSuspending<Post>(query).shouldBeFalse()
    }
}
