package io.bluetape4k.http.hc5.http

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotBeNull
import org.apache.hc.client5.http.ContextBuilder
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.junit.jupiter.api.Test

class ContextBuilderTest {

    companion object: KLogging()

    @Test
    fun `httpClientContext DSL 로 HttpClientContext 생성`() {
        val context = httpClientContext {
            // 기본 설정만
        }
        context.shouldNotBeNull()
        context.shouldBeInstanceOf<HttpClientContext>()
    }

    @Test
    fun `contextBuilderOf - 기본 ContextBuilder 생성`() {
        val builder = contextBuilderOf()
        builder.shouldNotBeNull()
        builder.shouldBeInstanceOf<ContextBuilder>()
    }

    @Test
    fun `contextBuilderOf - SchemePortResolver 적용한 ContextBuilder 생성`() {
        val resolver = DefaultSchemePortResolver.INSTANCE
        val builder = contextBuilderOf(resolver)
        builder.shouldNotBeNull()
        builder.shouldBeInstanceOf<ContextBuilder>()
    }

    @Test
    fun `contextBuilderOf 로 build 하여 HttpClientContext 생성`() {
        val context = contextBuilderOf().build()
        context.shouldNotBeNull()
        context.shouldBeInstanceOf<HttpClientContext>()
    }
}
