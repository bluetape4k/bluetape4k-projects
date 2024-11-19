package io.bluetape4k.feign.clients

import feign.kotlin.CoroutineFeign
import io.bluetape4k.feign.coroutines.client
import io.bluetape4k.feign.services.JsonPlaceHolder
import io.bluetape4k.feign.services.Post
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.random.Random

abstract class AbstractJsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderTest() {

    companion object: KLogging() {
        private const val ITEM_SIZE = 5
    }

    protected abstract fun newBuilder(): CoroutineFeign.CoroutineBuilder<*>
    private lateinit var client: JsonPlaceHolder.JsonPlaceholderCoroutineClient

    @BeforeAll
    fun beforeAll() {
        client = newBuilder().client(JsonPlaceHolder.BASE_URL)
    }

    @Test
    fun `create retrofit2 api instance`() {
        client.shouldNotBeNull()
    }

    @Test
    fun `get posts`() = runSuspendIO {
        val posts = client.posts()

        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
    }

    @Test
    fun `get post by postId`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = postIds.map { postId ->
            async(Dispatchers.IO) { client.getPost(postId)!! }
        }

        val posts = deferred.awaitAll()
        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
        posts.map { it.id } shouldContainSame postIds
    }

    @Test
    fun `get user's posts`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = userIds.map { userId ->
            async {
                userId to client.getUserPosts(userId)
            }
        }
        val userPosts = deferred.awaitAll().toMap()

        userPosts.size shouldBeEqualTo userIds.size
        userPosts.keys shouldContainSame userIds
        userPosts.forEach { (userId, posts) ->
            userIds.contains(userId).shouldBeTrue()
            posts.forEach { it.verify() }
        }
    }

    @Test
    fun `get post's comments`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = postIds.map { postId ->
            async {
                postId to client.getPostComments(postId)
            }
        }

        val postComments = deferred.awaitAll().toMap()

        postComments.size shouldBeEqualTo postIds.size
        postComments.keys shouldContainSame postIds
        postComments.forEach { (postId, comments) ->
            postIds.contains(postId).shouldBeTrue()
            comments.forEach { it.verify() }
        }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        val users = client.getUsers()
        users.shouldNotBeEmpty()
    }

    @Test
    fun `get albums by userId`() = runSuspendIO {
        val userIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = userIds.map { userId ->
            async {
                userId to client.getAlbumsByUserId(userId)
            }
        }

        val userAlbums = deferred.awaitAll().toMap()

        userAlbums.size shouldBeEqualTo userIds.size
        userAlbums.keys shouldContainSame userIds
        userAlbums.forEach { (userId, albums) ->
            userIds.contains(userId).shouldBeTrue()
            albums.forEach { it.verify() }
        }
    }

    @Test
    fun `create new post`(@RandomValue(type = Post::class, size = ITEM_SIZE) posts: List<Post>) = runSuspendIO {
        val deferred = posts.map { post ->
            async {
                client.createPost(post.copy(userId = post.userId.absoluteValue))
            }
        }

        val newPosts = deferred.awaitAll()
        newPosts.forEach { newPost -> newPost.verify() }
    }

    @Test
    fun `update exists post`() = runSuspendIO {
        val postIds = List(ITEM_SIZE) { Random.nextInt(1, 100) }.distinct()

        val deferred = postIds.map { postId ->
            async {
                val post = client.getPost(postId)!!
                client.updatePost(post.copy(title = "Updated " + post.title), postId)
            }
        }

        val updated = deferred.awaitAll()
        updated.size shouldBeEqualTo postIds.size
        updated.forEach { it.verify() }
    }

    @Test
    fun `delete post`(@RandomValue post: Post) = runSuspendIO {
        val newPost = post.copy(userId = post.userId.absoluteValue)
        val saved = client.createPost(newPost)
        val savedPostId = saved.id
        log.debug { "saved=$saved" }

        val deleted = client.deletePost(savedPostId)
        log.debug { "deleted=$deleted" }
        deleted.id shouldBeEqualTo 0
        deleted.userId shouldBeEqualTo 0
        deleted.title.shouldBeNull()
        deleted.body.shouldBeNull()
    }
}
