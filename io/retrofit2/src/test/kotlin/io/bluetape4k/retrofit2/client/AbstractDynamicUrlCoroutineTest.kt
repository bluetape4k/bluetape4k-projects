package io.bluetape4k.retrofit2.client

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.retrofit2.AbstractRetrofitTest
import io.bluetape4k.retrofit2.defaultJsonConverterFactory
import io.bluetape4k.retrofit2.retrofitOf
import io.bluetape4k.retrofit2.service
import io.bluetape4k.retrofit2.services.DynamicUrlService
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

abstract class AbstractDynamicUrlCoroutineTest: AbstractRetrofitTest() {

    companion object: KLoggingChannel()

    protected abstract val callFactory: okhttp3.Call.Factory

    // 동적 URL은 완전한 URL을 직접 사용하므로 trailing slash 없는 httpbinBaseUrl 사용
    private val httpbinUrlGet: String get() = "$httpbinBaseUrl/get"
    private val httpbinUrlPost: String get() = "$httpbinBaseUrl/post"

    private val api: DynamicUrlService.DynamicUrlCoroutineApi by lazy {
        // 동적 Url 사용 시에는 baseUrl 값을 overriding 합니다. (baseUrl은 사용되지 않습니다.)
        // 단 생성 시에 validate 를 하므로, Url 형식의 아무 값이나 사용해도 됩니다.
        retrofitOf("$testBaseUrl/", callFactory, defaultJsonConverterFactory).service()
    }

    @Test
    fun `create retrofit2 dynamic url api instance`() {
        api.shouldNotBeNull()
    }

    @Test
    fun `get content by dynamic url`() = runSuspendIO {
        val content = api.get(httpbinUrlGet)!!
        log.debug { "content=$content" }
    }

    @Test
    fun `post by dynamic url`() = runSuspendIO {
        val content = api.post(httpbinUrlPost)
        log.debug { "content=$content" }
        content.shouldNotBeNull()
    }

}
