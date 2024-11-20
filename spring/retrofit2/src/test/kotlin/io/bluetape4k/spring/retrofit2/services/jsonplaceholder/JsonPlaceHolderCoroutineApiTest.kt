package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.resilience4j.CoDecorators
import io.bluetape4k.support.uninitialized
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.math.absoluteValue

@SpringBootTest
@RandomizedTest
class JsonPlaceHolderCoroutineApiTest: AbstractJsonPlaceHolderApiTest() {

    companion object: KLogging()

    @Autowired
    private val api: JsonPlaceHolderCoroutineApi = uninitialized()

    @Test
    fun `context loading`() {
        api.shouldNotBeNull()
    }

    @Test
    fun `get posts with coroutines`() = runSuspendIO {
        val posts = api.posts()

        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
    }

    @Test
    fun `get posts with circuit breaker and retry`() = runSuspendIO {
        val circuitBreaker = CircuitBreaker.ofDefaults("post")
        val retry = Retry.ofDefaults("post")

        CoDecorators.ofSupplier { api.posts() }
            .withCircuitBreaker(circuitBreaker)
            .withRetry(retry)
            .invoke()
            .forEach { it.verify() }
    }

    @Test
    fun `get post by postId`(@RandomValue(type = Int::class, size = 3) postIds: List<Int>) = runSuspendIO {
        val deferreds = postIds.map {
            val postId = it.absoluteValue % 50 + 1
            async(Dispatchers.IO) {
                api.getPost(postId)
            }
        }

        val posts = deferreds.awaitAll()
        posts.forEach { it.verify() }
    }

    @Test
    fun `get users posts`(@RandomValue(type = Int::class, size = 3) userIds: List<Int>) = runSuspendIO {
        val deferreds = userIds.map {
            val userId = it.absoluteValue % 50 + 1
            async(Dispatchers.IO) {
                userId to api.getUserPosts(userId)
            }
        }

        val userPosts = deferreds.awaitAll()

        userPosts.forEach { (_, posts) ->
            posts.forEach { it.verify() }
        }
    }

    @Test
    fun `get post's comments`(@RandomValue(type = Int::class, size = 3) postIds: List<Int>) = runSuspendIO {

        val deferreds = postIds.map {
            val postId = it.absoluteValue % 100 + 1
            async(Dispatchers.IO) {
                postId to api.getPostComments(postId)
            }
        }

        val postComments = deferreds.awaitAll()

        postComments.forEach { (_, comments) ->
            comments.forEach { it.verify() }
        }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        val users = api.getUsers()
        users.shouldNotBeEmpty()
    }

    @Test
    fun `get albums by userId`(@RandomValue(type = Int::class, size = 3) userIds: List<Int>) = runSuspendIO {

        val futures = userIds.map {
            val userId = it.absoluteValue % 10 + 1
            async(Dispatchers.IO) {
                userId to api.getAlbumsByUserId(userId)
            }
        }

        val userAlbums = futures.awaitAll()

        userAlbums.forEach { (userId, albums) ->
            albums.forEach {
                it.shouldNotBeNull()
                it.userId shouldBeEqualTo userId
                it.id shouldBeGreaterThan 0
                it.title.shouldNotBeEmpty()
            }
        }
    }

    @Test
    fun `create new post`(@RandomValue(type = Post::class, size = 5) posts: List<Post>) = runSuspendIO {
        val tasks = posts.map { post ->
            async(Dispatchers.IO) {
                api.newPost(post.copy(userId = post.userId.absoluteValue))
            }
        }

        val newPosts = tasks.awaitAll()

        newPosts.forEach { newPost ->
            newPost.verify()
        }
    }

    @Test
    fun `update existing post`(@RandomValue(type = Int::class, size = 5) postIds: List<Int>) = runSuspendIO {
        val deferreds = postIds.map {
            val postId = it.absoluteValue % 50 + 1
            async(Dispatchers.IO) {
                val post = api.getPost(postId)
                val updated = post.copy(title = "Updated " + post.title)

                api.updatePost(updated.id, updated)
            }
        }

        val posts = deferreds.awaitAll()
        posts.forEach { it.verify() }
    }

    @Test
    fun `delete post`(@RandomValue post: Post) = runSuspendIO {
        val newPost = post.copy(userId = post.userId.absoluteValue)
        val saved = api.newPost(newPost)
        val savedPostId = saved.id

        val deleted = api.deletePost(savedPostId)

        deleted.shouldNotBeNull()
        deleted.id shouldBeEqualTo 0
        deleted.userId shouldBeEqualTo 0
        deleted.title.shouldBeNull()
        deleted.body.shouldBeNull()
    }
}
