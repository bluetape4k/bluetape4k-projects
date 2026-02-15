package io.bluetape4k.retrofit2.client

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.Httpbin
import io.bluetape4k.retrofit2.services.Post
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

@RandomizedTest
abstract class AbstractJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderTest() {

    companion object: KLogging()

    private val api: Httpbin.HttpbinApi by lazy {
        retrofitOf(testBaseUrl, callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `create retrofit2 api instance`() {
        api.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get posts`() {
        api.posts().execute().body()!!.verify("GET", "/anything/posts")
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get post by post id`() {
        api.getPost(1).execute().body()!!.verify("GET", "/anything/posts/1")
        api.getPost(2).execute().body()!!.verify("GET", "/anything/posts/2")
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get user's posts`() {
        val user1Posts = api.getUserPosts(1).execute().body()!!
        val user2Posts = api.getUserPosts(2).execute().body()!!

        user1Posts.verify("GET", "/anything/posts")
        user1Posts.verifyQuery("userId", 1)
        user2Posts.verify("GET", "/anything/posts")
        user2Posts.verifyQuery("userId", 2)
    }

    @Test
    fun `get post's comments`() {
        val post1Comments = api.getPostComments(1).execute().body()!!
        val post2Comments = api.getPostComments(2).execute().body()!!

        post1Comments.verify("GET", "/anything/post/1/comments")
        post2Comments.verify("GET", "/anything/post/2/comments")
    }

    @Test
    fun `get all users`() {
        api.getUsers().execute().body()!!.verify("GET", "/anything/users")
    }

    @Test
    fun `get albums by userId`() {
        val albums = api.getAlbumsByUserId(1).execute().body()!!
        albums.verify("GET", "/anything/albums")
        albums.verifyQuery("userId", 1)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create new post`(@RandomValue post: Post) {
        val newPost = api.newPost(post).execute().body()!!
        log.debug { "newPost=$newPost" }

        newPost.verify("POST", "/anything/posts")
        newPost.verifyJsonPost(post)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `update exist post`() {
        val post = Post(userId = 1, id = 10, title = "title", body = "body")
        post.title = "updated " + post.title

        val updated = api.updatePost(post.id, post).execute().body()!!
        log.debug { "Updated post=$updated" }

        updated.verify("PUT", "/anything/posts/10")
        updated.verifyJsonPost(post)
    }

    @Test
    fun `delete post`(@RandomValue post: Post) {
        val postId = post.id.absoluteValue + 1
        val deleted = api.deletePost(postId).execute().body()!!
        log.debug { "deleted=$deleted" }
        deleted.verify("DELETE", "/anything/posts/$postId")
    }
}
