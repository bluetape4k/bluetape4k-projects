package io.bluetape4k.spring.r2dbc.coroutines.blog.test.controller

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Post
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult

class PostControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractR2dbcBlogApplicationTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find all posts`() = runTest {
        val posts = client.httpGet("/posts")
            .returnResult<Post>().responseBody
            .asFlow()
            .onEach { post -> log.debug { "post=$post" } }
            .toFastList()

        posts.shouldNotBeEmpty()
    }

    @Test
    fun `find one post by id`() = runTest {
        val post = client.httpGet("/posts/1")
            .returnResult<Post>().responseBody
            .awaitSingle()

        log.debug { "Posts[1]=$post" }
        post.id shouldBeEqualTo 1
    }

    @Test
    fun `find one post by non-existing id`() = runTest {
        client.get()
            .uri("/posts/9999")
            .exchange()
            .expectStatus().isNotFound
            .expectBody<String>()
            .consumeWith { result ->
                log.debug { "result=$result" }
            }
    }

    @Test
    fun `save new post`() = runTest {
        val newPost = createPost()

        val savedPost = client.httpPost("/posts", newPost)
            .returnResult<Post>().responseBody
            .awaitSingle()

        savedPost.id.shouldNotBeNull()
        savedPost shouldBeEqualTo newPost.copy(id = savedPost.id)
    }

    @Test
    fun `count of comments by post id`() = runTest {
        val commentCount1 = async { countOfCommentByPostId(1L) }
        val commentCount2 = async { countOfCommentByPostId(2L) }

        commentCount1.await() shouldBeGreaterOrEqualTo 0
        commentCount2.await() shouldBeGreaterOrEqualTo 0
    }

    @Test
    fun `count of comments by non-existing post id`() = runTest {
        countOfCommentByPostId(9999L) shouldBeEqualTo 0L
    }

    private suspend fun countOfCommentByPostId(postId: Long): Long {
        return client.httpGet("/posts/$postId/comments/count")
            .returnResult<Long>().responseBody
            .awaitSingle()
    }
}
