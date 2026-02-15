package io.bluetape4k.http.hc5.examples

import io.bluetape4k.http.hc5.AbstractHc5Test
import io.bluetape4k.http.hc5.async.minimalHttpAsyncClientOf
import io.bluetape4k.http.hc5.http.basicHttpRequest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.Header
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.http.HttpResponse
import org.apache.hc.core5.http.Method
import org.apache.hc.core5.http.message.StatusLine
import org.apache.hc.core5.http.nio.AsyncClientExchangeHandler
import org.apache.hc.core5.http.nio.CapacityChannel
import org.apache.hc.core5.http.nio.DataStreamChannel
import org.apache.hc.core5.http.nio.RequestChannel
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityProducer
import org.apache.hc.core5.http.nio.entity.StringAsyncEntityConsumer
import org.apache.hc.core5.http.nio.support.BasicRequestProducer
import org.apache.hc.core5.http.nio.support.BasicResponseConsumer
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.io.CloseMode
import org.junit.jupiter.api.Test
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AsyncClientH2FullDuplexExchange: AbstractHc5Test() {

    companion object: KLoggingChannel()

    @Test
    fun `full-duplex streaming HTTP 2 message exchange`() {
        val baseUri = URI(NGHTTP2_HTTPBIN_URL)
        val httpHost = HttpHost(baseUri.scheme, baseUri.host, baseUri.port)

        val client = minimalHttpAsyncClientOf()
        client.start()

        val request = basicHttpRequest(Method.POST) {
            setHttpHost(httpHost)
            setPath("/post")
        }
        val requestProducer = BasicRequestProducer(request, BasicAsyncEntityProducer("stuff", ContentType.TEXT_PLAIN))
        val responseConsumer = BasicResponseConsumer(StringAsyncEntityConsumer())

        log.debug { "Executing request $request" }
        val latch = CountDownLatch(1)

        client.execute(object: AsyncClientExchangeHandler {
            override fun releaseResources() {
                requestProducer.releaseResources()
                responseConsumer.releaseResources()
                latch.countDown()
            }

            override fun updateCapacity(capacityChannel: CapacityChannel?) {
                responseConsumer.updateCapacity(capacityChannel)
            }

            override fun consume(src: ByteBuffer?) {
                responseConsumer.consume(src)
            }

            override fun streamEnd(trailers: MutableList<out Header>?) {
                responseConsumer.streamEnd(trailers)
            }

            override fun available(): Int {
                return requestProducer.available()
            }

            override fun produce(channel: DataStreamChannel?) {
                requestProducer.produce(channel)
            }

            override fun failed(cause: Exception?) {
                log.warn(cause) { "Failed to request $request" }
            }

            override fun produceRequest(channel: RequestChannel?, context: HttpContext?) {
                requestProducer.sendRequest(channel, context)
            }

            override fun consumeResponse(
                response: HttpResponse?,
                entityDetails: EntityDetails?,
                context: HttpContext?,
            ) {
                log.debug { "$request -> ${StatusLine(response)}" }
                responseConsumer.consumeResponse(response, entityDetails, context, null)
            }

            override fun consumeInformation(response: HttpResponse?, context: HttpContext?) {
                log.debug { "$request -> ${StatusLine(response)}" }
            }

            override fun cancel() {
                log.info { "$request cancelled" }
            }
        })

        latch.await(1, TimeUnit.MINUTES)

        log.debug { "Shutting down" }
        client.close(CloseMode.GRACEFUL)
    }
}
