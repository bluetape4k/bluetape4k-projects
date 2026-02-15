package io.bluetape4k.feign.clients

import io.bluetape4k.feign.client
import io.bluetape4k.feign.services.HttpbinService
import io.bluetape4k.feign.services.Post
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

abstract class AbstractHttpbinSyncTest: AbstractHttpbinTest() {

    companion object: KLogging()

    protected abstract fun newBuilder(): feign.Feign.Builder
    private lateinit var client: HttpbinService.HttpbinClient

    @BeforeAll
    fun beforeAll() {
        client = newBuilder().client(testBaseUrl)
    }

    @Test
    fun `create retrofit2 api instance`() {
        client.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get posts`() {
        client.posts().verify("GET", "/anything/posts")
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get post by post id`() {
        client.getPost(1).verify("GET", "/anything/posts/1")
        client.getPost(2).verify("GET", "/anything/posts/2")
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get user's posts`() {
        val user1Posts = client.getUserPosts(1)
        val user2Posts = client.getUserPosts(2)

        user1Posts.verify("GET", "/anything/posts")
        user1Posts.verifyQuery("userId", 1)
        user2Posts.verify("GET", "/anything/posts")
        user2Posts.verifyQuery("userId", 2)
    }

    @Test
    fun `get post's comments`() {
        val post1Comments = client.getPostComments(1)
        val post2Comments = client.getPostComments(2)

        post1Comments.verify("GET", "/anything/post/1/comments")
        post2Comments.verify("GET", "/anything/post/2/comments")
    }

    @Test
    fun `get all users`() {
        client.getUsers().verify("GET", "/anything/users")
    }

    @Test
    fun `get albums by userId`() {
        val albums = client.getAlbumsByUserId(1)
        albums.verify("GET", "/anything/albums")
        albums.verifyQuery("userId", 1)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create new post`(@RandomValue post: Post) {
        val newPost = client.createPost(post)
        log.debug { "newPost=$newPost" }
        newPost.verify("POST", "/anything/posts")
        newPost.verifyJsonPost(post)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `update exist post`() {
        val post = Post(
            userId = 10,
            id = 10,
            title = "updated title-10",
            body = "updated body-10"
        )

        val updated = client.updatePost(post, post.id)
        updated.verify("PUT", "/anything/posts/10")
        updated.verifyJsonPost(post)
    }

    @Test
    fun `delete post`(@RandomValue post: Post) {
        val deleted = client.deletePost(post.id.absoluteValue.coerceAtLeast(1))
        log.debug { "deleted=$deleted" }
        deleted.verify("DELETE", "/anything/posts/")
    }
}
