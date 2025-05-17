package io.bluetape4k.retrofit2.client

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.DynamicUrlService
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

abstract class AbstractDynamicUrlCoroutineTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
        private const val HTTPBIN_URL_GET = "https://nghttp2.org/httpbin/get"
        private const val HTTPBIN_URL_POST = "https://nghttp2.org/httpbin/post"
    }

    protected abstract val callFactory: okhttp3.Call.Factory

    private val api: DynamicUrlService.DynamicUrlCoroutineApi by lazy {
        // 동적 Url 사용 시에는 baseUrl 값을 overriding 합니다. (baseUrl은 사용되지 않습니다.)
        // 단 생성 시에 validate 를 하므로, Url 형식의 아무 값이나 사용해도 됩니다.
        retrofitOf("https://nghttp2.org/httpbin/", callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `create retrofit2 dynamic url api instance`() {
        api.shouldNotBeNull()
    }

    @Test
    fun `get content by dynamic url`() = runSuspendTest {
        val content = api.get(HTTPBIN_URL_GET)!!
        log.debug { "content=$content" }
    }

    @Test
    fun `post by dynamic url`() = runSuspendTest {
        val content = api.post(HTTPBIN_URL_POST)
        log.debug { "content=$content" }
        content.shouldNotBeNull()
    }

}
