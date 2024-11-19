package io.bluetape4k.retrofit2.services

import io.bluetape4k.logging.KLogging
import retrofit2.Call
import retrofit2.http.GET

/**
 * [nghttp2.org](https://nghttp2.org/httpbin) 에서 제공하는 API를 테스트하기 위한 서비스 클래스입니다.
 *
 * TODO: kommons-testcontainers 의 HttpbinServer 를 이용하도록 변경하자 (https://httpbin.org 가 너무 느리다)
 */
object Httpbin: KLogging() {

    const val BASE_URL = "https://nghttp2.org/httpbin"

    interface HttpbinApi {

        @GET("/get")
        fun get(): Call<String>

    }

}
