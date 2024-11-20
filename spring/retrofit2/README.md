# Module bluetape4k-spring-retrofit

## 개요

Spring Framework 기반에서 손쉽게 Retrofit을 사용하기 위한 라이브러리입니다.

## 사용법

### Define Retrofit REST API

```kotlin
@Retrofit2Client(name = "jsonPlaceHolderCoroutineApi", baseUrl = "\${bluetape4k.retrofit2.services.jsonPlaceHolder}")
interface JsonPlaceHolderCoroutineApi {

    @GET("/posts")
    suspend fun posts(): List<Post>

    @GET("/posts/{id}")
    suspend fun getPost(@Path("id") postId: Int): Post

    @GET("/posts")
    suspend fun getUserPosts(@Query("userId") userId: Int): List<Post>

    @GET("/post/{id}/comments")
    suspend fun getPostComments(@Path("id") postId: Int): List<Comment>

    @GET("/commonts")
    suspend fun getComments(@Query("postId") postId: Int): List<Comment>

    @POST("/posts")
    suspend fun newPost(@Body post: Post): Post

    @PUT("/posts/{id}")
    suspend fun updatePost(@Path("id") postId: Int, @Body post: Post): Post

    @DELETE("/posts/{id}")
    suspend fun deletePost(@Path("id") postId: Int): Post

    @GET("/users")
    suspend fun getUsers(): List<User>

    @GET("/albums")
    suspend fun getAlbums(): List<Album>

    @GET("/albums")
    suspend fun getAlbumsByUserId(@Query("userId") userId: Int): List<Album>

}
```

### Spring Config

```kotlin
@SpringBootApplication
@EnableRetrofitClients //  @RetrofitClient 가 적용된 interface를 찾아 Retrofit Service로 만듭니다.
class Retrofit2SpringBootApplication {

    companion object: KLogging()

    @Component
    class Retrofit2SampleRunner: CommandLineRunner {

        @Autowired
        private val httpbinApi: HttpbinApi = uninitialized()

        @Autowired
        private val jsonPlaceHolderApi: JsonPlaceHolderApi = uninitialized()

        override fun run(vararg args: String?) {
            runBlocking {
                val users = jsonPlaceHolderApi.getUsers()
            }
        }
    }
}
```

### RetrofitClient 용 API 테스트

```kotlin
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
```

    <1> Retrofit2용으로 `GithubApi` 서비스를 Bean으로 inject 해줍니다.
