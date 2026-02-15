package io.bluetape4k.retrofit2.services

import io.bluetape4k.logging.KLogging
import io.reactivex.Maybe
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * `httpbin` 기반 API를 테스트하기 위한 서비스 클래스입니다.
 */
object Httpbin: KLogging() {

    const val BASE_URL = "https://nghttp2.org/httpbin"

    interface HttpbinApi {
        @GET("/anything/posts")
        fun posts(): Call<HttpbinAnythingResponse>

        @GET("/anything/posts/{id}")
        fun getPost(@Path("id") postId: Int): Call<HttpbinAnythingResponse>

        @GET("/anything/posts")
        fun getUserPosts(@Query("userId") userId: Int): Call<HttpbinAnythingResponse>

        @GET("/anything/post/{id}/comments")
        fun getPostComments(@Path("id") postId: Int): Call<HttpbinAnythingResponse>

        @GET("/anything/comments")
        fun getComments(@Query("postId") postId: Int): Call<HttpbinAnythingResponse>

        @POST("/anything/posts")
        fun newPost(@Body post: Post): Call<HttpbinAnythingResponse>

        @PUT("/anything/posts/{id}")
        fun updatePost(@Path("id") postId: Int, @Body post: Post): Call<HttpbinAnythingResponse>

        @DELETE("/anything/posts/{id}")
        fun deletePost(@Path("id") postId: Int): Call<HttpbinAnythingResponse>

        @GET("/anything/users")
        fun getUsers(): Call<HttpbinAnythingResponse>

        @GET("/anything/albums")
        fun getAlbums(): Call<HttpbinAnythingResponse>

        @GET("/anything/albums")
        fun getAlbumsByUserId(@Query("userId") userId: Int): Call<HttpbinAnythingResponse>
    }

    interface HttpbinCoroutineApi {
        @GET("/anything/posts")
        suspend fun posts(): HttpbinAnythingResponse

        @GET("/anything/posts/{id}")
        suspend fun getPost(@Path("id") postId: Int): HttpbinAnythingResponse

        @GET("/anything/posts")
        suspend fun getUserPosts(@Query("userId") userId: Int): HttpbinAnythingResponse

        @GET("/anything/post/{id}/comments")
        suspend fun getPostComments(@Path("id") postId: Int): HttpbinAnythingResponse

        @GET("/anything/comments")
        suspend fun getComments(@Query("postId") postId: Int): HttpbinAnythingResponse

        @POST("/anything/posts")
        suspend fun newPost(@Body post: Post): HttpbinAnythingResponse

        @PUT("/anything/posts/{id}")
        suspend fun updatePost(@Path("id") postId: Int, @Body post: Post): HttpbinAnythingResponse

        @DELETE("/anything/posts/{id}")
        suspend fun deletePost(@Path("id") postId: Int): HttpbinAnythingResponse

        @GET("/anything/users")
        suspend fun getUsers(): HttpbinAnythingResponse

        @GET("/anything/albums")
        suspend fun getAlbums(): HttpbinAnythingResponse

        @GET("/anything/albums")
        suspend fun getAlbumsByUserId(@Query("userId") userId: Int): HttpbinAnythingResponse
    }

    interface HttpbinReactiveApi {
        @GET("/anything/posts")
        fun posts(): Maybe<HttpbinAnythingResponse>

        @GET("/anything/posts/{id}")
        fun getPost(@Path("id") postId: Int): Maybe<HttpbinAnythingResponse>

        @GET("/anything/posts")
        fun getUserPosts(@Query("userId") userId: Int): Maybe<HttpbinAnythingResponse>

        @GET("/anything/post/{id}/comments")
        fun getPostComments(@Path("id") postId: Int): Maybe<HttpbinAnythingResponse>

        @GET("/anything/comments")
        fun getComments(@Query("postId") postId: Int): Maybe<HttpbinAnythingResponse>

        @POST("/anything/posts")
        fun newPost(@Body post: Post): Maybe<HttpbinAnythingResponse>

        @PUT("/anything/posts/{id}")
        fun updatePost(@Path("id") postId: Int, @Body post: Post): Maybe<HttpbinAnythingResponse>

        @DELETE("/anything/posts/{id}")
        fun deletePost(@Path("id") postId: Int): Maybe<HttpbinAnythingResponse>

        @GET("/anything/users")
        fun getUsers(): Maybe<HttpbinAnythingResponse>

        @GET("/anything/albums")
        fun getAlbums(): Maybe<HttpbinAnythingResponse>

        @GET("/anything/albums")
        fun getAlbumsByUserId(@Query("userId") userId: Int): Maybe<HttpbinAnythingResponse>
    }

}
