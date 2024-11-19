package io.bluetape4k.retrofit2.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

object DynamicUrlService {

    interface DynamicUrlApi {
        @GET
        fun get(@Url url: String): Call<Any?>

        @POST
        fun post(@Url url: String): Call<Any?>
    }

    interface DynamicUrlCoroutineApi {
        @GET
        suspend fun get(@Url url: String): Any?

        @POST
        suspend fun post(@Url url: String): Any?
    }
}
