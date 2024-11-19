package io.bluetape4k.retrofit2.client

import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.JsonPlaceHolder
import io.bluetape4k.retrofit2.services.Post
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldNotBeNullOrBlank
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue

@RandomizedTest
abstract class AbstractJsonPlaceHolderSyncTest: AbstractJsonPlaceHolderTest() {

    companion object: KLogging()

    private val api: JsonPlaceHolder.JsonPlaceHolderApi by lazy {
        retrofitOf(JsonPlaceHolder.BASE_URL, callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `create retrofit2 api instance`() {
        api.shouldNotBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get posts`() {
        val posts = api.posts().execute().body()!!

        posts.shouldNotBeEmpty()
        posts.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get post by post id`() {
        val post1 = api.getPost(1).execute().body()!!
        post1.verify()

        val post2 = api.getPost(2).execute().body()!!
        post2.verify()

        // 없는 post id를 조회하면 null이 반환된다.
        api.getPost(0).execute().body().shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get user's posts`() {
        val user1Posts = api.getUserPosts(1).execute().body()!!
        val user2Posts = api.getUserPosts(2).execute().body()!!

        user1Posts.forEach { it.verify() }
        user2Posts.forEach { it.verify() }
    }

    @Test
    fun `get post's commnets`() {
        val post1Comments = api.getPostComments(1).execute().body()!!
        val post2Comments = api.getPostComments(2).execute().body()!!

        post1Comments.forEach { it.verify() }
        post2Comments.forEach { it.verify() }
    }

    @Test
    fun `get all users`() {
        val users = api.getUsers().execute().body()!!
        users.shouldNotBeEmpty()
    }

    @Test
    fun `get albums by userId`() {
        val albums = api.getAlbumsByUserId(1).execute().body()!!
        albums.shouldNotBeEmpty()
        albums.forEach { it.verify() }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create new post`(@RandomValue post: Post) {
        val newPost = api.newPost(post).execute().body()!!
        log.debug { "newPost=$newPost" }

        newPost.title.shouldNotBeNullOrBlank()
        newPost.body.shouldNotBeNullOrBlank()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `update exist post`() {
        val post = api.getPost(10).execute().body()!!
        post.title = "updated " + post.title

        val updated = api.updatePost(post.id, post).execute().body()!!
        log.debug { "Updated post=$updated" }

        updated.id shouldBeEqualTo post.id
        updated.title shouldBeEqualTo post.title
    }

    @Test
    fun `delete post`(@RandomValue post: Post) {
        val newPost = post.copy(userId = post.userId.absoluteValue, id = 0)
        val saved = api.newPost(newPost).execute().body()!!
        val savedPostId = saved.id
        log.debug { "saved=$saved" }
        saved.userId shouldBeEqualTo newPost.userId
        saved.title shouldBeEqualTo newPost.title
        saved.body shouldBeEqualTo newPost.body

        val deleted = api.deletePost(savedPostId).execute().body()!!
        log.debug { "deleted=$deleted" }
        deleted.id shouldBeEqualTo 0
        deleted.userId shouldBeEqualTo 0
        deleted.title.shouldBeNull()
        deleted.body.shouldBeNull()
    }
}
