package io.bluetape4k.http.hc5.http

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test

class ProductionRequestConfigTest {

    companion object : KLogging()

    @Test
    @Suppress("DEPRECATION")
    fun `productionRequestConfigOf creates config with sensible defaults`() {
        val config = productionRequestConfigOf()

        config.shouldNotBeNull()
        config.connectionRequestTimeout shouldBeEqualTo Timeout.ofSeconds(5)
        config.connectTimeout shouldBeEqualTo Timeout.ofSeconds(10)
        config.responseTimeout shouldBeEqualTo Timeout.ofSeconds(30)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `productionRequestConfigOf allows overriding individual timeouts`() {
        val config = productionRequestConfigOf(
            connectionRequestTimeout = Timeout.ofSeconds(2),
            connectTimeout = Timeout.ofSeconds(3),
            responseTimeout = Timeout.ofSeconds(60),
        )

        config.connectionRequestTimeout shouldBeEqualTo Timeout.ofSeconds(2)
        config.connectTimeout shouldBeEqualTo Timeout.ofSeconds(3)
        config.responseTimeout shouldBeEqualTo Timeout.ofSeconds(60)
    }

    @Test
    fun `defaultKeepAliveStrategy returns fallback for absent Keep-Alive header`() {
        val strategy = defaultKeepAliveStrategy()
        strategy.shouldNotBeNull()
    }

    @Test
    fun `defaultKeepAliveStrategy accepts custom fallback duration`() {
        val strategy = defaultKeepAliveStrategy(org.apache.hc.core5.util.TimeValue.ofSeconds(30))
        strategy.shouldNotBeNull()
    }

    @Test
    fun `defaultRetryStrategy creates strategy with default params`() {
        val strategy = defaultRetryStrategy()
        strategy.shouldNotBeNull()
    }

    @Test
    fun `defaultRetryStrategy accepts custom maxRetries and interval`() {
        val strategy = defaultRetryStrategy(maxRetries = 5, retryInterval = org.apache.hc.core5.util.TimeValue.ofSeconds(2))
        strategy.shouldNotBeNull()
    }
}
