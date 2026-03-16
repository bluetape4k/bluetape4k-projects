package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.LibraryName
import io.bluetape4k.spring.retrofit2.Retrofit2Client
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * [Json Place Holder](https://jsonplaceholder.typicode.com/) 에서 제공하는 API 로서
 * Json 데이터 통신에 대한 테스트를 손쉽게 할 수 있습니다.
 *
 * 여기서는 API 통신을 Call을 이용합니다 (IO-Bounded Asynchronous 는 아닙니다!!!)
 */
@Retrofit2Client(name = "jsonPlaceHolderApi", baseUrl = "\${$LibraryName.retrofit2.services.jsonPlaceHolder}")
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
