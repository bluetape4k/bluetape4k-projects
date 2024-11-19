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
 * [JsonPlaceHolder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API를 테스트하기 위한 서비스 클래스입니다.
 */
object JsonPlaceHolder: KLogging() {

    const val BASE_URL = "https://jsonplaceholder.typicode.com"

    /**
     * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서
     * Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
     *
     * 여기서는 API 통신을 Call을 이용합니다 (IO-Bounded Asynchronous 는 아닙니다!!!)
     */
    interface JsonPlaceHolderApi {

        @GET("/posts")
        fun posts(): Call<List<Post>>

        @GET("/posts/{id}")
        fun getPost(@Path("id") postId: Int): Call<Post>

        @GET("/posts")
        fun getUserPosts(@Query("userId") userId: Int): Call<List<Post>>

        @GET("/post/{id}/comments")
        fun getPostComments(@Path("id") postId: Int): Call<List<Comment>>

        @GET("/commonts")
        fun getComments(@Query("postId") postId: Int): Call<List<Comment>>

        @POST("/posts")
        fun newPost(@Body post: Post): Call<Post>

        @PUT("/posts/{id}")
        fun updatePost(@Path("id") postId: Int, @Body post: Post): Call<Post>

        @DELETE("/posts/{id}")
        fun deletePost(@Path("id") postId: Int): Call<Post>

        @GET("/users")
        fun getUsers(): Call<List<User>>

        @GET("/albums")
        fun getAlbums(): Call<List<Album>>

        @GET("/albums")
        fun getAlbumsByUserId(@Query("userId") userId: Int): Call<List<Album>>
    }

    /**
     * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서
     * Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
     *
     * 여기서는 API 통신을 Coroutines 를 이용합니다.
     */
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


    /**
     * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서
     * Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
     *
     * 여기서 API 통신는 RxJava2를 이용합니다.
     */
    interface JsonPlaceHolderReactiveApi {

        @GET("/posts")
        fun posts(): Maybe<List<Post>>

        @GET("/posts/{id}")
        fun getPost(@Path("id") postId: Int): Maybe<Post>

        @GET("/posts")
        fun getUserPosts(@Query("userId") userId: Int): Maybe<List<Post>>

        @GET("/post/{id}/comments")
        fun getPostComments(@Path("id") postId: Int): Maybe<List<Comment>>

        @GET("/commonts")
        fun getComments(@Query("postId") postId: Int): Maybe<List<Comment>>

        @POST("/posts")
        fun newPost(@Body post: Post): Maybe<Post>

        @PUT("/posts/{id}")
        fun updatePost(@Path("id") postId: Int, @Body post: Post): Maybe<Post>

        @DELETE("/posts/{id}")
        fun deletePost(@Path("id") postId: Int): Maybe<Post>

        @GET("/users")
        fun getUsers(): Maybe<List<User>>

        @GET("/albums")
        fun getAlbums(): Maybe<List<Album>>

        @GET("/albums")
        fun getAlbumsByUserId(@Query("userId") userId: Int): Maybe<List<Album>>
    }
}
