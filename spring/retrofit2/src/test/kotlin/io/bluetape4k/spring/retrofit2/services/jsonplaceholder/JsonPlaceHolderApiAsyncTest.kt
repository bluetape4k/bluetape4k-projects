package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.executeAsync
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.future.await
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.math.absoluteValue

@SpringBootTest
@RandomizedTest
class JsonPlaceHolderApiAsyncTest: AbstractJsonPlaceHolderApiTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @Autowired
    private val api: JsonPlaceHolderApi = uninitialized()

    @Test
    fun `create retrofit2 api instance`() {
        api.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get posts`() = runSuspendIO {
        val posts = api.posts().executeAsync().await().body()!!

        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get post by post id`() = runSuspendIO {
        val post1 = api.getPost(1).executeAsync().await().body()!!
        post1.verify()

        val post2 = api.getPost(2).executeAsync().await().body()!!
        post2.verify()

        // 없는 post id를 조회하면 null이 반환된다.
        api.getPost(0).executeAsync().await().body().shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get user's posts`() = runSuspendIO {
        val user1Posts = api.getUserPosts(1).executeAsync().await().body()!!
        val user2Posts = api.getUserPosts(2).executeAsync().await().body()!!

        user1Posts.forEach { it.verify() }
        user2Posts.forEach { it.verify() }
    }

    @Test
    fun `get post's commnets`() = runSuspendIO {
        val post1Comments = api.getPostComments(1).executeAsync().await().body()!!
        val post2Comments = api.getPostComments(2).executeAsync().await().body()!!

        post1Comments.forEach { it.verify() }
        post2Comments.forEach { it.verify() }
    }

    @Test
    fun `get all users`() = runSuspendIO {
        val users = api.getUsers().executeAsync().await().body()!!
        users.shouldNotBeEmpty()
    }

    @Test
    fun `get albums by userId`() = runSuspendIO {
        val albums = api.getAlbumsByUserId(1).executeAsync().await().body()!!
        albums.shouldNotBeEmpty()
        albums.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create new post`(@RandomValue post: Post) = runSuspendIO {
        val newPost = api.newPost(post).executeAsync().await().body()!!
        log.debug { "newPost=$newPost" }

        newPost.title.shouldNotBeNullOrBlank()
        newPost.body.shouldNotBeNullOrBlank()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `update exist post`() = runSuspendIO {
        val post = api.getPost(10).executeAsync().await().body()!!
        post.title = "updated " + post.title

        val updated = api.updatePost(post.id, post).executeAsync().await().body()!!

        updated.id shouldBeEqualTo post.id
        updated.title shouldBeEqualTo post.title
    }

    @Test
    fun `delete post`(@RandomValue post: Post) = runSuspendIO {
        val newPost = post.copy(userId = post.userId.absoluteValue)
        val saved = api.newPost(newPost).executeAsync().await().body()!!
        val savedPostId = saved.id
        log.debug { "saved=$saved" }

        val deleted = api.deletePost(savedPostId).executeAsync().await().body()!!
        log.debug { "deleted=$deleted" }
        deleted.id shouldBeEqualTo 0
        deleted.userId shouldBeEqualTo 0
        deleted.title.shouldBeNull()
        deleted.body.shouldBeNull()
    }
}
