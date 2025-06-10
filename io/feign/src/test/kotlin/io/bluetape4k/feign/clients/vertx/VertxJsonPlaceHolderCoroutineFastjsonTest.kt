package io.bluetape4k.feign.clients.vertx

import feign.Logger
import feign.kotlin.CoroutineFeign
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractJsonPlaceHolderCoroutineTest
import io.bluetape4k.feign.codec.FeignFastjsonDecoder
import io.bluetape4k.feign.codec.FeignFastjsonEncoder
import io.bluetape4k.feign.coroutines.coroutineFeignBuilder

class VertxJsonPlaceHolderCoroutineFastjsonTest: AbstractJsonPlaceHolderCoroutineTest() {

    override fun newBuilder(): CoroutineFeign.CoroutineBuilder<*> {
        return coroutineFeignBuilder {
            client(AsyncVertxHttpClient())
            encoder(FeignFastjsonEncoder.INSTANCE)
            decoder(FeignFastjsonDecoder.INSTANCE)
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
