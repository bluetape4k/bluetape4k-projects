package io.bluetape4k.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.HttpbinServer
import org.junit.jupiter.api.fail

abstract class AbstractHttpTest {

    companion object: KLogging() {
        const val HELLOWORLD_URL = "https://publicobject.com/helloworld.txt"
        const val HTTPBIN_URL = "https://httpbin.org"
        const val NGHTTP2_HTTPBIN_URL = "https://nghttp2.org/httpbin"
        const val JSON_PLACEHOLDER_URL = "https://jsonplaceholder.typicode.com"
        const val JSON_PLACEHOLDER_TODOS_URL = "$JSON_PLACEHOLDER_URL/todos"

        /**
         * http://httpbin.org 에 접속하는 테스트를 로컬에서 실행할 수 있도록 합니다.
         */
        @JvmStatic
        protected val httpbinServer by lazy { HttpbinServer.Launcher.httpbin }

        @JvmStatic
        protected val httpbinBaseUrl by lazy { httpbinServer.url }
    }

    fun assertResponse(okResponse: okhttp3.Response?) {
        if (okResponse == null) {
            fail { "Response is null" }
        }
        if (!okResponse.isSuccessful) {
            fail { "Unexpected code ${okResponse.code}" }
        }
    }
}
