package io.bluetape4k.feign.clients.vertx

import feign.Logger
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractHttpbinCoroutineTest
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.logging.coroutines.KLoggingChannel

class VertxHttpbinCoroutineJacksonTest: AbstractHttpbinCoroutineTest() {

    companion object: KLoggingChannel()

    override fun newBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncVertxHttpClient())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
