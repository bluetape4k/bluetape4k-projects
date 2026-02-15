package io.bluetape4k.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.http.HttpbinServer
import org.junit.jupiter.api.fail

abstract class AbstractHttpTest {

    companion object: KLogging() {
        const val JSON_PLACEHOLDER_URL = "https://jsonplaceholder.typicode.com"
        const val JSON_PLACEHOLDER_TODOS_URL = "$JSON_PLACEHOLDER_URL/todos"

        /**
         * http://httpbin.org 에 접속하는 테스트를 로컬에서 실행할 수 있도록 합니다.
         */
        @JvmStatic
        protected val httpbinServer by lazy { HttpbinServer.Launcher.httpbin }

        @JvmStatic
        protected val httpbinBaseUrl by lazy { httpbinServer.url }

        /**
         * 외부 `publicobject.com` 대신 로컬 httpbin의 `/html` 응답을 사용합니다.
         */
        @JvmStatic
        protected val HELLOWORLD_URL: String get() = "$httpbinBaseUrl/html"

        /**
         * 외부 `httpbin.org` 대신 로컬 Testcontainers httpbin URL을 사용합니다.
         */
        @JvmStatic
        protected val HTTPBIN_URL: String get() = httpbinBaseUrl

        /**
         * 테스트 안정성을 위해 HTTP/2 전용 외부 URL도 로컬 httpbin URL로 매핑합니다.
         */
        @JvmStatic
        protected val NGHTTP2_HTTPBIN_URL: String get() = httpbinBaseUrl
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
