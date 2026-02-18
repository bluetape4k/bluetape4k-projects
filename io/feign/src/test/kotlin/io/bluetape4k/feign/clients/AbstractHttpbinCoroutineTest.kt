package io.bluetape4k.feign.clients

import feign.kotlin.CoroutineFeign
import io.bluetape4k.feign.coroutines.client
import io.bluetape4k.feign.services.HttpbinService
import io.bluetape4k.feign.services.Post
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
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
    fun `create retrofit2 api instance`() {
        client.shouldNotBeNull()
    }

    @Test
    fun `get posts`() = runSuspendIO {
        client.posts().verify("GET", "/anything/posts")
    }

    @Test
    fun `get post by postId`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = postIds.map { postId ->
            async(Dispatchers.IO) {
                postId to client.getPost(postId)
            }
        }

        val responses = deferred.awaitAll()
        responses.map { it.first } shouldContainSame postIds
        responses.forEach { (postId, response) ->
            response.verify("GET", "/anything/posts/$postId")
        }
    }

    @Test
    fun `get user's posts`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = userIds.map { userId ->
            async {
                userId to client.getUserPosts(userId)
            }
        }
        val userPosts = deferred.awaitAll()

        userPosts.map { it.first } shouldContainSame userIds
        userPosts.forEach { (userId, response) ->
            response.verify("GET", "/anything/posts")
            response.verifyQuery("userId", userId)
        }
    }

    @Test
    open fun `get post's comments`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 20) }.distinct()

        val deferred = postIds.map { postId ->
            async {
                postId to client.getPostComments(postId)
            }
        }

        val postComments = deferred.awaitAll()

        postComments.map { it.first } shouldContainSame postIds
        postComments.forEach { (postId, response) ->
            response.verify("GET", "/anything/post/$postId/comments")
        }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        client.getUsers().verify("GET", "/anything/users")
    }

    @Test
    fun `get albums by userId`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = userIds.map { userId ->
            async {
                userId to client.getAlbumsByUserId(userId)
            }
        }

        val userAlbums = deferred.awaitAll()

        userAlbums.map { it.first } shouldContainSame userIds
        userAlbums.forEach { (userId, response) ->
            response.verify("GET", "/anything/albums")
            response.verifyQuery("userId", userId)
        }
    }

    @Test
    fun `create new post`(
        @RandomValue(type = Post::class, size = ITEM_SIZE) posts: List<Post>,
    ) = runSuspendIO {
        val requestPosts = posts.map { post -> post.copy(userId = post.userId.absoluteValue) }
        val deferred = requestPosts.map { post ->
            async {
                client.createPost(post)
            }
        }

        val newPosts = deferred.awaitAll()
        newPosts.forEachIndexed { idx, response ->
            response.verify("POST", "/anything/posts")
            response.verifyJsonPost(requestPosts[idx])
        }
    }

    @Test
    fun `update exists post`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = postIds.map { postId ->
            async {
                val post = Post(
                    userId = postId,
                    id = postId,
                    title = "Updated title-$postId",
                    body = "Updated body-$postId"
                )
                post to client.updatePost(post, postId)
            }
        }

        val updated = deferred.awaitAll()
        updated.map { it.first.id } shouldContainSame postIds
        updated.forEach { (post, response) ->
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
