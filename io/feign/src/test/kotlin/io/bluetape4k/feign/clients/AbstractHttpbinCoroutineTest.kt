package io.bluetape4k.feign.clients

import feign.kotlin.CoroutineFeign
import io.bluetape4k.assertions.shouldContainSame
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.coroutines.flow.async
import io.bluetape4k.feign.coroutines.client
import io.bluetape4k.feign.services.HttpbinService
import io.bluetape4k.feign.services.Post
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.random.Random

abstract class AbstractHttpbinCoroutineTest: AbstractHttpbinTest() {

    companion object: KLoggingChannel() {
        private const val ITEM_SIZE = 5
    }

    protected abstract fun newBuilder(): CoroutineFeign.CoroutineBuilder<*>
    private lateinit var client: HttpbinService.HttpbinCoroutineClient

    @BeforeAll
    fun beforeAll() {
        client = newBuilder().client(testBaseUrl)
    }

    @Test
    fun `create feign api instance`() {
        client.shouldNotBeNull()
    }

    @Test
    fun `get posts`() = runSuspendIO {
        client.posts().verify("GET", "/anything/posts")
    }

    @Test
    fun `get post by postId`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val responses = postIds.asFlow()
            .async { client.getPost(it) }
            .toList()
        responses.forEachIndexed { index, response ->
            response.verify("GET", "/anything/posts/${postIds[index]}")
        }
    }

    @Test
    fun `get user's posts`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val responses = userIds.asFlow()
            .async { client.getUserPosts(it) }
            .toList()

        responses.forEachIndexed { index, response ->
            response.verify("GET", "/anything/posts")
            response.verifyQuery("userId", userIds[index])
        }
    }

    @Test
    open fun `get post's comments`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 20) }.distinct()

        val responses = postIds.asFlow()
            .async { client.getPostComments(it) }
            .toList()

        responses.forEachIndexed { index, response ->
            response.verify("GET", "/anything/post/${postIds[index]}/comments")
        }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        client.getUsers().verify("GET", "/anything/users")
    }

    @Test
    fun `get albums by userId`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val responses = userIds.asFlow()
            .async { client.getAlbumsByUserId(it) }
            .toList()

        responses.forEachIndexed { index, response ->
            response.verify("GET", "/anything/albums")
            response.verifyQuery("userId", userIds[index])
        }
    }

    @Test
    fun `create new post`(@RandomValue(type = Post::class, size = ITEM_SIZE) posts: List<Post>) = runSuspendIO {
        val requestPosts = posts.map { post -> post.copy(userId = post.userId.absoluteValue) }

        val responses = requestPosts.asFlow()
            .async { client.createPost(it) }
            .toList()

        responses.forEachIndexed { index, response ->
            response.verify("POST", "/anything/posts")
            response.verifyJsonPost(requestPosts[index])
        }
    }

    @Test
    fun `update exists post`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val responses = postIds.asFlow()
            .async { postId ->
                val post = Post(
                    userId = postId,
                    id = postId,
                    title = "Updated title-$postId",
                    body = "Updated body-$postId"
                )
                post to client.updatePost(post, postId)
            }
            .toList()
        responses.map { it.first.id } shouldContainSame postIds
        responses.forEach { (post, response) ->
            response.verify("PUT", "/anything/posts/${post.id}")
            response.verifyJsonPost(post)
        }
    }

    @Test
    fun `delete post`(@RandomValue post: Post) = runSuspendIO {
        val savedPostId = post.id.absoluteValue.coerceAtLeast(1)
        val deleted = client.deletePost(savedPostId)
        log.debug { "deleted=$deleted" }
        deleted.verify("DELETE", "/anything/posts/$savedPostId")
    }
}
