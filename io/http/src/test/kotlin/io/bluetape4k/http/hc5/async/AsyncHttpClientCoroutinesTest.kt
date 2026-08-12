package io.bluetape4k.http.hc5.async

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.methods.toProducer
import io.bluetape4k.http.hc5.async.execute as deprecatedExecute
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer
import org.apache.hc.client5.http.protocol.HttpClientContext
import org.apache.hc.core5.http.HttpHost
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import java.net.URI

class AsyncHttpClientCoroutinesTest: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `httpAsyncClientOf 로 executeSuspending GET 요청`() = runSuspendIO {
        httpAsyncClient {}.use { client ->
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val response = client.executeSuspending(request)
            log.debug { "GET $httpbinBaseUrl/get status=${response.code}" }
            response.code shouldBeEqualTo 200
        }
    }

    @Test
    fun `여러 URL 병렬 coroutine GET 요청 모두 200`() = runSuspendIO(timeout = 60.seconds) {
        httpAsyncClient {}.use { client ->
            val responses = coroutineScope {
                urisToGet.map { uri ->
                    async {
                        val request = SimpleRequestBuilder.get(uri).build()
                        val response = client.executeSuspending(request)
                        log.debug { "GET $uri status=${response.code}" }
                        response
                    }
                }.awaitAll()
            }
            responses.forEach { response ->
                response.code shouldBeEqualTo 200
            }
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `executeSuspending and deprecated execute overloads support request producer context and target`() = runSuspendIO {
        httpAsyncClient {}.use { client ->
            val request = SimpleRequestBuilder.get("$httpbinBaseUrl/get").build()
            val producer = request.toProducer()
            val target = URI.create(httpbinBaseUrl).let { HttpHost(it.scheme, it.host, it.port) }
            val context = HttpClientContext.create()

            val producerWithContext = client.executeSuspending(
                producer,
                SimpleResponseConsumer.create(),
                null,
                context,
            )
            producerWithContext.code shouldBeEqualTo 200

            val deprecatedProducerWithContext = with(client) {
                deprecatedExecute(
                    producer,
                    SimpleResponseConsumer.create(),
                    null,
                    context,
                )
            }
            deprecatedProducerWithContext.code shouldBeEqualTo 200

            val producerWithCallback = client.executeSuspending(
                requestProducer = producer,
                responseConsumer = SimpleResponseConsumer.create(),
                callback = null,
            )
            producerWithCallback.code shouldBeEqualTo 200

            val deprecatedProducerWithCallback = with(client) {
                deprecatedExecute(
                    requestProducer = producer,
                    responseConsumer = SimpleResponseConsumer.create(),
                    callback = null,
                )
            }
            deprecatedProducerWithCallback.code shouldBeEqualTo 200

            val targeted = client.executeSuspending(
                target,
                producer,
                SimpleResponseConsumer.create(),
                null,
                null,
                null,
            )
            targeted.code shouldBeEqualTo 200

            val deprecatedTargeted = with(client) {
                deprecatedExecute(
                    target,
                    producer,
                    SimpleResponseConsumer.create(),
                    null,
                    null,
                    null,
                )
            }
            deprecatedTargeted.code shouldBeEqualTo 200

            val simpleWithContext = client.executeSuspending(request, context, null)
            simpleWithContext.code shouldBeEqualTo 200

            val deprecatedSimpleWithContext = with(client) { deprecatedExecute(request, context, null) }
            deprecatedSimpleWithContext.code shouldBeEqualTo 200

            val consumerOverload = client.executeSuspending(
                request,
                SimpleResponseConsumer.create(),
                context,
                null,
            )
            consumerOverload.code shouldBeEqualTo 200

            val deprecatedConsumerOverload = with(client) {
                deprecatedExecute(
                    request,
                    SimpleResponseConsumer.create(),
                    context,
                    null,
                )
            }
            deprecatedConsumerOverload.code shouldBeEqualTo 200
        }
    }
}
