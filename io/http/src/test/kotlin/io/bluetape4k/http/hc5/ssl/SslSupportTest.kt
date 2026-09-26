package io.bluetape4k.http.hc5.ssl

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.core5.reactor.ssl.SSLBufferMode
import org.junit.jupiter.api.Test

class SslSupportTest {

    companion object: KLogging()

    @Test
    fun `sslContextOf creates default SSLContext`() {
        val sslContext = sslContextOf()

        sslContext.shouldNotBeNull()
        log.debug { "protocol: ${sslContext.protocol}, provider:${sslContext.provider}" }
        sslContext.protocol shouldBeEqualTo "TLS"
        sslContext.provider.shouldNotBeEmpty()
    }

    @Test
    fun `sslContextOfSystem creates system default SSLContext`() {
        val sslContext = sslContextOfSystem()

        sslContext.shouldNotBeNull()
        log.debug { "protocol: ${sslContext.protocol}, provider:${sslContext.provider}" }
        sslContext.protocol shouldBeEqualTo "Default"
        sslContext.provider.shouldNotBeEmpty()
    }

    @Test
    fun `sslContext DSL creates custom SSLContext`() {
        val sslContext = sslContext {
            // basic custom build without loading key/trust material
        }
        log.debug { "protocol: ${sslContext.protocol}, provider:${sslContext.provider}" }
        sslContext.shouldNotBeNull()
    }

    @Test
    fun `sslContextOf and sslContextOfSystem return non-null SSLContext`() {
        val defaultCtx = sslContextOf()
        val systemCtx = sslContextOfSystem()

        defaultCtx.shouldNotBeNull()
        systemCtx.shouldNotBeNull()
    }

    @Test
    fun `defaultHostnameVerifier is non-null`() {
        defaultHostnameVerifier.shouldNotBeNull()
    }

    @Test
    fun `tlsStrategy DSL creates TlsStrategy`() {
        val strategy = tlsStrategy {
            setSslContext(sslContextOf())
            setSslBufferMode(SSLBufferMode.STATIC)
            setHostnameVerifier(defaultHostnameVerifier)
        }

        log.debug { "strategy: $strategy" }
        strategy.shouldNotBeNull()

    }

    @Test
    fun `tlsStrategyOf with defaults creates TlsStrategy`() {
        val strategy = tlsStrategyOf()

        log.debug { "strategy: $strategy" }
        strategy.shouldNotBeNull()
    }

    @Test
    fun `tlsStrategyOf with sslContext creates TlsStrategy`() {
        val sslContext = sslContextOf()
        val strategy = tlsStrategyOf(sslContext = sslContext)

        log.debug { "strategy: $strategy" }
        strategy.shouldNotBeNull()
    }

    @Test
    fun `tlsStrategyOf with sslBufferMode STATIC creates TlsStrategy`() {
        val strategy = tlsStrategyOf(
            sslContext = sslContextOf(),
            sslBufferMode = SSLBufferMode.STATIC,
        )
        log.debug { "strategy: $strategy" }
        strategy.shouldNotBeNull()
    }

    @Test
    fun `tlsStrategyOf with hostnameVerifier creates TlsStrategy`() {
        val strategy = tlsStrategyOf(
            sslContext = sslContextOf(),
            hostnameVerifier = defaultHostnameVerifier,
        )
        log.debug { "strategy: $strategy" }
        strategy.shouldNotBeNull()
    }

    @Test
    fun `sslContextOf protocol contains TLS`() {
        val sslContext = sslContextOf()

        sslContext.protocol shouldContain "TLS"
    }
}
