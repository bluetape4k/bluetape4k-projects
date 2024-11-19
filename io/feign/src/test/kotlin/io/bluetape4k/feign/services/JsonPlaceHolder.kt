package io.bluetape4k.feign.services

import feign.Headers
import feign.Param
import feign.RequestLine
import io.bluetape4k.logging.KLogging

/**
 * [JsonPlaceHolder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API를 테스트하기 위한 서비스 클래스입니다.
 */
object JsonPlaceHolder: KLogging() {

    const val BASE_URL = "https://jsonplaceholder.typicode.com"

    /**
     * JSON Placeholder API
     *
     * 참고: [JsonPlaceHolder](https://jsonplaceholder.typicode.com/)
     * 참고: [RequestLine with Feign Client](https://www.baeldung.com/feign-requestline)
     */
    // REST API 호출 시, Content-Type 을 명시적으로 지정해야 한다.
    @Headers("Content-Type: application/json; charset=UTF-8")
    interface JsonPlaceholderClient {

        @RequestLine("GET /posts")
        fun posts(): List<Post>

        @RequestLine("GET /posts/{id}")
        fun getPost(@Param("id") postId: Int): Post?

        @RequestLine("GET /posts?userId={userId}")
        fun getUserPosts(@Param("userId") userId: Int): List<Post>

        @RequestLine("GET /post/{id}/comments")
        fun getPostComments(@Param("id") postId: Int): List<Comment>

        @RequestLine("GET /commonts?postId={postId}")
        fun getComments(@Param("postId") postId: Int): List<Comment>

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("POST /posts")
        fun createPost(post: Post): Post

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("PUT /posts/{id}")
        fun updatePost(post: Post, @Param("id") postId: Int): Post

        @RequestLine("DELETE /posts/{id}")
        fun deletePost(@Param("id") postId: Int): Post

        @RequestLine("GET /users")
        fun getUsers(): List<User>

        @RequestLine("GET /albums")
        fun getAlbums(): List<Album>

        @RequestLine("GET /albums?userId={userId}")
        fun getAlbumsByUserId(@Param("userId") userId: Int): List<Album>
    }


    /**
     * JSON Placeholder API with Coroutines
     *
     * 참고: [JsonPlaceHolder](https://jsonplaceholder.typicode.com/)
     * 참고: [RequestLine with Feign Client](https://www.baeldung.com/feign-requestline)
     * 참고: [Dynamic Query Parameters](https://github.com/OpenFeign/feign#dynamic-query-parameters)
     */
    // REST API 호출 시, Content-Type 을 명시적으로 지정해야 한다.
    @Headers("Content-Type: application/json; charset=UTF-8")
    interface JsonPlaceholderCoroutineClient {

        @RequestLine("GET /posts")
        suspend fun posts(): List<Post>

        @RequestLine("GET /posts/{id}")
        suspend fun getPost(@Param("id") postId: Int): Post?

        @RequestLine("GET /posts?userId={userId}")
        suspend fun getUserPosts(@Param("userId") userId: Int): List<Post>

        @RequestLine("GET /post/{id}/comments")
        suspend fun getPostComments(@Param("id") postId: Int): List<Comment>

        @RequestLine("GET /commonts?postId={postId}")
        suspend fun getComments(@Param("postId") postId: Int): List<Comment>

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("POST /posts")
        suspend fun createPost(post: Post): Post

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("PUT /posts/{id}")
        suspend fun updatePost(post: Post, @Param("id") postId: Int): Post

        @RequestLine("DELETE /posts/{id}")
        suspend fun deletePost(@Param("id") postId: Int): Post

        @RequestLine("GET /users")
        suspend fun getUsers(): List<User>

        @RequestLine("GET /albums")
        suspend fun getAlbums(): List<Album>

        @RequestLine("GET /albums?userId={userId}")
        suspend fun getAlbumsByUserId(@Param("userId") userId: Int): List<Album>
    }
}
