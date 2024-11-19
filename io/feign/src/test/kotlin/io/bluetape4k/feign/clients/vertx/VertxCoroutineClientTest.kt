package io.bluetape4k.feign.clients.vertx

import feign.Logger
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractCoroutineClientTest
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.logging.KLogging

class VertxCoroutineClientTest: AbstractCoroutineClientTest() {

    companion object: KLogging()

    override fun newCoroutineBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncVertxHttpClient())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
