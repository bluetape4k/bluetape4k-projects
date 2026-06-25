package io.bluetape4k.http.hc5.protocol

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.protocol.HttpCoreContext
import org.junit.jupiter.api.Test

class HttpClientContextTest {

    companion object: KLogging()

    @Test
    fun `httpClientContextOf - 기본 생성`() {
        val context = httpClientContextOf()
        context.shouldNotBeNull()
        context.shouldBeInstanceOf<HttpClientContext>()
    }

    @Test
    fun `httpClientContextOf - 기존 코어 컨텍스트 래핑`() {
        val baseContext = HttpCoreContext.create()
        val context = httpClientContextOf(baseContext)
        context.shouldNotBeNull()
        context.shouldBeInstanceOf<HttpClientContext>()
    }

    @Test
    fun `adapt - HttpCoreContext 를 HttpClientContext 로 변환`() {
        val baseContext = HttpCoreContext.create()
        val clientContext = baseContext.adapt()
        clientContext.shouldNotBeNull()
        clientContext.shouldBeInstanceOf<HttpClientContext>()
    }

    @Test
    fun `adapt - 이미 HttpClientContext 인 경우 그대로 반환`() {
        val context = HttpClientContext.create()
        val adapted = context.adapt()
        adapted.shouldNotBeNull()
        adapted.shouldBeInstanceOf<HttpClientContext>()
    }
}
