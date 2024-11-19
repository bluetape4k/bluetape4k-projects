package io.bluetape4k.http.hc5.examples

import io.bluetape4k.coroutines.support.coAwait
import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http.ContentTypes
import io.bluetape4k.http.hc5.http.basicRequestProducerOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8ByteBuffer
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Resourcex
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactive.ReactiveEntityProducer
import org.apache.hc.core5.reactive.ReactiveResponseConsumer
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import java.net.URI

/**
 * This example demonstrates a reactive, full-duplex HTTP/1.1 message exchange using Reactor.
 */
class ReactiveClientFullDuplexExchange: AbstractHc5Test() {

    companion object: KLogging()

    @Test
    fun `send reactive flux and receive reactive flux`() = runSuspendIO {
        val client = minimalHttpAsyncClientOf()
        client.start()

        val requestUri = URI("$httpbinBaseUrl/post")
        val requestContent = Resourcex.getString("files/Utf8Samples.txt").toUtf8ByteBuffer()
        val reactiveEntityProducer = ReactiveEntityProducer(
            flowOf(requestContent).asFlux(),  // Flux.just(bs.toByteBuffer()),
            requestContent.remaining().toLong(),
            ContentTypes.TEXT_PLAIN_UTF8,
            null
        )
        val requestProducer = basicRequestProducerOf(Method.POST, requestUri, reactiveEntityProducer)
        val consumer = ReactiveResponseConsumer()

        client.execute(requestProducer, consumer, null)
        val streamingResponse = consumer.responseFuture.coAwait()

        log.debug { "head=${streamingResponse.head}" }
        streamingResponse.head.headers.forEach { header ->
            log.debug { "header=$header" }
        }

//        streamingResponse.body.asFlow()
//            .map { byteBuffer -> byteBuffer.toUtf8String() }
//            .collect {
//                log.debug { "response content=$it" }
//            }
        Flux.from(streamingResponse.body)
            .map { byteBuffer -> byteBuffer.toUtf8String() }
            .materialize()
            .asFlow()
            .collect {
                log.debug { it }
            }

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
