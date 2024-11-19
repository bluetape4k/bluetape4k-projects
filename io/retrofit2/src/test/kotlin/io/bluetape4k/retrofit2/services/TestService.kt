package io.bluetape4k.retrofit2.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

object TestService {

    interface EmptyService {
        @GET("/")
        fun empty(): Call<Unit>

        @HEAD("/")
        fun head(): Call<Unit>
    }

    interface TestInterface {

        @POST("/?foo=bar&foo=baz&qux=")
        @Headers("Foo: Bar", "Foz: Baz", "Qux: ", "Content-Type: text/plain; charset=UTF-8")
        fun post(@Body body: String): Call<String?>

        @POST("/path/{to}/resource")
        @Headers("Accept: text/plain")
        fun post(@Path("to") to: String?, body: String?): Call<String>

        @POST("/?foo=bar&foo=baz&qux=")
        @Headers("Foo: Bar", "Foo: Baz", "Qux: ", "Content-Type: text/plain")
        fun postForString(@Body body: String?): Call<String>

        @GET("/")
        // @Headers("Accept: */*", "Accept-Encoding: gzip, deflate, lz4, snappy, zstd")
        fun get(): Call<String>

        @GET("/")
        fun get(@Query(value = "foo") multiFoo: List<String>): Call<String>

        @GET("/")
        fun getWithHeaders(@Header("Authorization") authorization: String?): Call<String>

        @Headers("Accept: text/plain", "content-type: text/plain")
        @PATCH("/")
        fun patch(): Call<String>

        @POST("/")
        fun noPostBody(): Call<String>

        @PUT("/")
        fun noPutBody(): Call<String>

        @PATCH("/patch/")
        fun noPatchBody(): Call<String>

        @Headers("Foo: Bar", "Foo: Baz", "Qux: ")
        @POST("/?foo=bar&foo=baz&qux=")
        fun postWithContentType(@Body body: String?, @Header("Content-Type") contentType: String): Call<String>
    }
}
