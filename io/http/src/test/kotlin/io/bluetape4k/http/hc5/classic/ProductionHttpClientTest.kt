package io.bluetape4k.http.hc5.classic

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.http.defaultKeepAliveStrategy
import io.bluetape4k.http.hc5.http.defaultRetryStrategy
import io.bluetape4k.http.hc5.http.productionRequestConfigOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.classic.methods.HttpGet
import org.apache.hc.core5.util.TimeValue
import org.junit.jupiter.api.Test

class ProductionHttpClientTest : AbstractHc5Test() {

    companion object : KLogging()

    @Test
    fun `productionHttpClientOf creates client with defaults`() {
        val client = productionHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpClientOf executes GET request successfully`() {
        productionHttpClientOf().use { client ->
            val statusCode = client.execute(HttpGet("$httpbinBaseUrl/get")) { it.code }
            log.debug { "GET /get status=$statusCode" }
            statusCode shouldBeEqualTo 200
        }
    }

    @Test
    fun `productionHttpClientOf accepts custom pool size`() {
        val client = productionHttpClientOf(maxConnTotal = 50, maxConnPerRoute = 25)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpClientOf accepts custom request config`() {
        val client = productionHttpClientOf(
            requestConfig = productionRequestConfigOf(
                responseTimeout = org.apache.hc.core5.util.Timeout.ofSeconds(60),
            ),
        )
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpClientOf accepts custom keepAlive and retry strategies`() {
        val client = productionHttpClientOf(
            keepAliveStrategy = defaultKeepAliveStrategy(TimeValue.ofSeconds(30)),
            retryStrategy = defaultRetryStrategy(maxRetries = 1),
        )
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionVirtualThreadHttpClientOf creates client with defaults`() {
        val client = productionVirtualThreadHttpClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionVirtualThreadHttpClientOf executes GET request successfully`() {
        productionVirtualThreadHttpClientOf().use { client ->
            val statusCode = client.execute(HttpGet("$httpbinBaseUrl/get")) { it.code }
            log.debug { "Virtual-thread GET /get status=$statusCode" }
            statusCode shouldBeEqualTo 200
        }
    }
}
