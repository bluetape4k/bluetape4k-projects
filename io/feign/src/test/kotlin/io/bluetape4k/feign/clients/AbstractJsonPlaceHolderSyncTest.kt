package io.bluetape4k.feign.clients

import io.bluetape4k.feign.client
import io.bluetape4k.feign.services.JsonPlaceHolder
import io.bluetape4k.feign.services.Post
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.test.assertFailsWith

abstract class AbstractJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderTest() {

    companion object: KLogging()

    protected abstract fun newBuilder(): feign.Feign.Builder
    private lateinit var client: JsonPlaceHolder.JsonPlaceholderClient

    @BeforeAll
    fun beforeAll() {
        client = newBuilder().client(JsonPlaceHolder.BASE_URL)
    }

    @Test
    fun `create retrofit2 api instance`() {
        client.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get posts`() {
        val posts = client.posts()

        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get post by post id`() {
        val post1 = client.getPost(1)!!
        post1.verify()

        val post2 = client.getPost(2)!!
        post2.verify()

        // FIXME: Decoder에서 404 라면 null을 반환하게 해야 한다 !!!
        assertFailsWith<feign.FeignException.NotFound> {
            client.getPost(0)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get user's posts`() {
        val user1Posts = client.getUserPosts(1)
        val user2Posts = client.getUserPosts(2)

        user1Posts.forEach { it.verify() }
        user2Posts.forEach { it.verify() }
    }

    @Test
    fun `get post's commnets`() {
        val post1Comments = client.getPostComments(1)
        val post2Comments = client.getPostComments(2)

        post1Comments.forEach { it.verify() }
        post2Comments.forEach { it.verify() }
    }

    @Test
    fun `get all users`() {
        val users = client.getUsers()
        users.shouldNotBeEmpty()
    }

    @Test
    fun `get albums by userId`() {
        val albums = client.getAlbumsByUserId(1)
        albums.shouldNotBeEmpty()
        albums.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create new post`(@RandomValue post: Post) {
        val newPost = client.createPost(post)
        log.debug { "newPost=$newPost" }

        newPost.title.shouldNotBeNullOrBlank()
        newPost.body.shouldNotBeNullOrBlank()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `update exist post`() {
        val post = client.getPost(10)!!
        post.title = "updated " + post.title

        val updated = client.updatePost(post, post.id)

        updated.id shouldBeEqualTo post.id
        updated.title shouldBeEqualTo post.title
    }

    @Test
    fun `delete post`(@RandomValue post: Post) {
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
