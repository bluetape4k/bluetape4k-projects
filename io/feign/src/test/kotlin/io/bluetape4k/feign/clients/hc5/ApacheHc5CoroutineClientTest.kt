package io.bluetape4k.feign.clients.hc5

import feign.Logger
import feign.hc5.AsyncApacheHttp5Client
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractCoroutineClientTest
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.http.hc5.async.httpAsyncClientOf
import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.protocol.HttpClientContext

class ApacheHc5CoroutineClientTest: AbstractCoroutineClientTest() {

    companion object: KLogging()

    override fun newCoroutineBuilder(): CoroutineFeign.CoroutineBuilder<HttpClientContext> {
        return coroutineFeignBuilder {
            client(AsyncApacheHttp5Client(httpAsyncClientOf()))
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
