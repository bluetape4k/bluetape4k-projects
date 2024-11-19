package io.bluetape4k.feign.clients.hc5

import feign.Logger
import feign.hc5.AsyncApacheHttp5Client
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder
import io.bluetape4k.logging.KLogging

class ApacheHc5JsonPlaceHolderCoroutineTest: AbstractJsonPlaceHolderCoroutineTest() {

    companion object: KLogging()

    override fun newBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncApacheHttp5Client())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
