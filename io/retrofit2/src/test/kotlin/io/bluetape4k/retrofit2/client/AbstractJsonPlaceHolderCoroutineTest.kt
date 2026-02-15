package io.bluetape4k.retrofit2.client

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.collections.eclipse.multi.toListMultimap
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.Httpbin
import io.bluetape4k.retrofit2.services.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.random.Random

@RandomizedTest
abstract class AbstractJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderTest() {

    companion object: KLoggingChannel() {
        private const val ITEM_SIZE = 5
    }

    private val api: Httpbin.HttpbinCoroutineApi by lazy {
        retrofitOf(testBaseUrl, callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `create retrofit2 api instance`() {
        api.shouldNotBeNull()
    }

    @Test
    fun `get posts`() = runSuspendIO {
        api.posts().verify("GET", "/anything/posts")
    }

    @Test
    fun `get post by postId`() = runSuspendIO {
        val postIds = fastList(ITEM_SIZE) { Random.nextInt(1, 10) }.distinct()

        val deferred = postIds.map { postId ->
            async(Dispatchers.IO) { postId to api.getPost(postId) }
        }

        val responses = deferred.awaitAll()
        responses.forEach { (postId, response) ->
            response.verify("GET", "/anything/posts/$postId")
        }
        responses.map { it.first } shouldContainSame postIds
    }

    @Test
    fun `get user's posts`() = runSuspendIO {
        val userIds = fastList(ITEM_SIZE) { Random.nextInt(1, 10) }.distinct()

        val deferred = userIds.map { userId ->
            async(Dispatchers.IO) {
                userId to api.getUserPosts(userId)
            }
        }
        val userPosts = deferred.awaitAll().toListMultimap()

        userPosts.keysView().size() shouldBeEqualTo userIds.size
        userPosts.keysView() shouldContainSame userIds
        userPosts.forEachKeyValue { userId, response ->
            userIds.contains(userId).shouldBeTrue()
            response.verify("GET", "/anything/posts")
            response.verifyQuery("userId", userId)
        }
    }

    @Test
    fun `get post's comments`() = runSuspendIO {
        val postIds = fastList(ITEM_SIZE) { Random.nextInt(1, 10) }.distinct()

        val deferred = postIds.map { postId ->
            async(Dispatchers.IO) {
                postId to api.getPostComments(postId)
            }
        }

        val postComments = deferred.awaitAll().toListMultimap()

        postComments.keysView().size() shouldBeEqualTo postIds.size
        postComments.keysView() shouldContainSame postIds
        postComments.forEachKeyValue { postId, response ->
            postIds.contains(postId).shouldBeTrue()
            response.verify("GET", "/anything/post/$postId/comments")
        }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        api.getUsers().verify("GET", "/anything/users")
    }

    @Test
    fun `get albums by userId`() = runSuspendIO {
        val userIds = fastList(ITEM_SIZE) { Random.nextInt(1, 10) }.distinct()

        val deferred = userIds.map { userId ->
            async(Dispatchers.IO) {
                userId to api.getAlbumsByUserId(userId)
            }
        }

        val userAlbums = deferred.awaitAll().toListMultimap()

        userAlbums.keysView().size() shouldBeEqualTo userIds.size
        userAlbums.keysView() shouldContainSame userIds
        userAlbums.forEachKeyValue { userId, response ->
            userIds.contains(userId).shouldBeTrue()
            response.verify("GET", "/anything/albums")
            response.verifyQuery("userId", userId)
        }
    }

    @Test
    fun `create new post`(@RandomValue(type = Post::class, size = ITEM_SIZE) posts: List<Post>) = runSuspendIO {
        val deferred = posts.map { post ->
            async(Dispatchers.IO) {
                api.newPost(post.copy(userId = post.userId.absoluteValue))
            }
        }

        val newPosts = deferred.awaitAll()
        newPosts.forEachIndexed { idx, newPost ->
            newPost.verify("POST", "/anything/posts")
            newPost.verifyJsonPost(posts[idx].copy(userId = posts[idx].userId.absoluteValue))
        }
    }

    @Test
    fun `update exists post`() = runSuspendIO {
        val postIds = fastList(ITEM_SIZE) { Random.nextInt(1, 10) }.distinct()

        val deferred = postIds.map { postId ->
            async(Dispatchers.IO) {
                api.updatePost(postId, Post(userId = 1, id = postId, title = "Updated", body = "body-$postId"))
            }
        }

        val updated = deferred.awaitAll()
        updated.size shouldBeEqualTo postIds.size
        updated.forEachIndexed { idx, response ->
            val postId = postIds[idx]
            response.verify("PUT", "/anything/posts/$postId")
            response.verifyJsonPost(Post(userId = 1, id = postId, title = "Updated", body = "body-$postId"))
        }
    }

    @Test
    fun `delete post`(@RandomValue post: Post) = runSuspendIO {
        val postId = post.id.absoluteValue + 1
        val deleted = api.deletePost(postId)
        log.debug { "deleted=$deleted" }
        deleted.verify("DELETE", "/anything/posts/$postId")
    }
}
