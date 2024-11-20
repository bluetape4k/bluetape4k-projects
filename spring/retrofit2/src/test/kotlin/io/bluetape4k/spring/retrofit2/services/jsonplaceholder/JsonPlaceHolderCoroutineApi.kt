package io.bluetape4k.spring.retrofit2.services.jsonplaceholder

import io.bluetape4k.LibraryName
import io.bluetape4k.spring.retrofit2.Retrofit2Client
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
 * 여기서는 API 통신을 Coroutines 를 이용합니다.
 */
@Retrofit2Client(name = "jsonPlaceHolderCoroutineApi", baseUrl = "\${$LibraryName.retrofit2.services.jsonPlaceHolder}")
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
