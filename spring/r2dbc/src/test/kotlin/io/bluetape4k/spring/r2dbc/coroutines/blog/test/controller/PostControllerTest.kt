package io.bluetape4k.spring.r2dbc.coroutines.blog.test.controller

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.r2dbc.coroutines.blog.domain.Post
import io.bluetape4k.spring.r2dbc.coroutines.blog.test.AbstractR2dbcBlogApplicationTest
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class PostControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractR2dbcBlogApplicationTest() {

    companion object: KLogging()

    @Test
    fun `find all posts`() = runTest {
        val posts = client.httpGet("/posts")
            .expectBodyList<Post>()
            .returnResult().responseBody!!

        posts.shouldNotBeEmpty()
        posts.forEach { post ->
            log.debug { "post=$post" }
        }
    }

    @Test
    fun `find one post by id`() = runTest {
        val post = client.httpGet("/posts/1")
            .expectBody<Post>()
            .returnResult().responseBody!!

        log.debug { "Post[1]=$post" }
        post.id shouldBeEqualTo 1
    }

    @Test
    fun `find one post by non-existing id`() = runTest {
        client.get()
            .uri("/posts/9999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `save new post`() = runTest {
        val newPost = createPost()

        val savedPost = client.httpPost("/posts", newPost)
            .expectBody<Post>()
            .returnResult().responseBody!!

        savedPost.id.shouldNotBeNull()
        savedPost shouldBeEqualTo newPost.copy(id = savedPost.id)
    }

    @Test
    fun `count of comments by post id`() = runTest {
        val commentCount1 = countOfCommentByPostId(1L)
        val commentCount2 = countOfCommentByPostId(2L)

        commentCount1 shouldBeGreaterOrEqualTo 0
        commentCount2 shouldBeGreaterOrEqualTo 0
    }

    @Test
    fun `count of comments by non-existing post id`() = runTest {
        countOfCommentByPostId(9999L) shouldBeEqualTo 0L
    }

    private fun countOfCommentByPostId(postId: Long): Long {
        return client.httpGet("/posts/$postId/comments/count")
            .expectBody<Long>()
            .returnResult().responseBody!!
    }
}
