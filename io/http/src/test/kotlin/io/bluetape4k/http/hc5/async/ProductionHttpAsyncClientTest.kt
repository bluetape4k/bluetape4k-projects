package io.bluetape4k.http.hc5.async

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.methods.toProducer
import io.bluetape4k.http.hc5.http.defaultRetryStrategy
import io.bluetape4k.http.hc5.http.productionRequestConfigOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.util.Timeout
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class ProductionHttpAsyncClientTest : AbstractHc5Test() {

    companion object : KLogging()

    @Test
    fun `productionHttpAsyncClientOf creates client with defaults`() {
        val client = productionHttpAsyncClientOf()
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpAsyncClientOf accepts custom pool size`() {
        val client = productionHttpAsyncClientOf(maxConnTotal = 50, maxConnPerRoute = 25)
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpAsyncClientOf accepts custom request config and retry`() {
        val client = productionHttpAsyncClientOf(
            requestConfig = productionRequestConfigOf(responseTimeout = Timeout.ofSeconds(60)),
            retryStrategy = defaultRetryStrategy(maxRetries = 1),
        )
        client.shouldNotBeNull()
        client.close()
    }

    @Test
    fun `productionHttpAsyncClientOf executes async GET request successfully`() {
        productionHttpAsyncClientOf().use { client ->
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val future = client.execute(
                request.toProducer(),
                SimpleResponseConsumer.create(),
                HttpClientContext.create(),
                null,
            )
            val response = future.get(30, TimeUnit.SECONDS)
            log.debug { "Async GET /get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }
}
