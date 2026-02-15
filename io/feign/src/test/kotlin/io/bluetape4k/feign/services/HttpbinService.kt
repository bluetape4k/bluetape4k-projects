package io.bluetape4k.feign.services

import feign.Headers
import feign.Param
import feign.RequestLine
import io.bluetape4k.logging.KLogging

/**
 * Httpbin 형태의 API를 테스트하기 위한 서비스 클래스입니다.
 */
object HttpbinService: KLogging() {

    /**
     * JSON Placeholder API
     *
     * 참고: [RequestLine with Feign Client](https://www.baeldung.com/feign-requestline)
     */
    // httpbin /anything 엔드포인트를 이용해 REST 호출 계약을 검증합니다.
    @Headers("Content-Type: application/json; charset=UTF-8")
    interface HttpbinClient {

        @RequestLine("GET /anything/posts")
        fun posts(): HttpbinAnythingResponse

        @RequestLine("GET /anything/posts/{id}")
        fun getPost(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/posts?userId={userId}")
        fun getUserPosts(@Param("userId") userId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/post/{id}/comments")
        fun getPostComments(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/comments?postId={postId}")
        fun getComments(@Param("postId") postId: Int): HttpbinAnythingResponse

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("POST /anything/posts")
        fun createPost(post: Post): HttpbinAnythingResponse

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("PUT /anything/posts/{id}")
        fun updatePost(post: Post, @Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("DELETE /anything/posts/{id}")
        fun deletePost(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/users")
        fun getUsers(): HttpbinAnythingResponse

        @RequestLine("GET /anything/albums")
        fun getAlbums(): HttpbinAnythingResponse

        @RequestLine("GET /anything/albums?userId={userId}")
        fun getAlbumsByUserId(@Param("userId") userId: Int): HttpbinAnythingResponse
    }


    /**
     * JSON Placeholder API with Coroutines
     *
     * 참고: [RequestLine with Feign Client](https://www.baeldung.com/feign-requestline)
     * 참고: [Dynamic Query Parameters](https://github.com/OpenFeign/feign#dynamic-query-parameters)
     */
    // httpbin /anything 엔드포인트를 이용해 REST 호출 계약을 검증합니다.
    @Headers("Content-Type: application/json; charset=UTF-8")
    interface HttpbinCoroutineClient {

        @RequestLine("GET /anything/posts")
        suspend fun posts(): HttpbinAnythingResponse

        @RequestLine("GET /anything/posts/{id}")
        suspend fun getPost(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/posts?userId={userId}")
        suspend fun getUserPosts(@Param("userId") userId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/post/{id}/comments")
        suspend fun getPostComments(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/comments?postId={postId}")
        suspend fun getComments(@Param("postId") postId: Int): HttpbinAnythingResponse

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("POST /anything/posts")
        suspend fun createPost(post: Post): HttpbinAnythingResponse

        /**
         * `@Body` 어노테이션을 사용하던가, 첫 번째 파라미터를 Request Body 로 사용한다.
         */
        @RequestLine("PUT /anything/posts/{id}")
        suspend fun updatePost(post: Post, @Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("DELETE /anything/posts/{id}")
        suspend fun deletePost(@Param("id") postId: Int): HttpbinAnythingResponse

        @RequestLine("GET /anything/users")
        suspend fun getUsers(): HttpbinAnythingResponse

        @RequestLine("GET /anything/albums")
        suspend fun getAlbums(): HttpbinAnythingResponse

        @RequestLine("GET /anything/albums?userId={userId}")
        suspend fun getAlbumsByUserId(@Param("userId") userId: Int): HttpbinAnythingResponse
    }
}
