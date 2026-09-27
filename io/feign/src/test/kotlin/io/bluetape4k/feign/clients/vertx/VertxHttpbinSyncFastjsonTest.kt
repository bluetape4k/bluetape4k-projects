package io.bluetape4k.feign.clients.vertx

import feign.Feign
import feign.Logger
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractHttpbinSyncTest
import io.bluetape4k.feign.codec.FeignFastjsonDecoder
import io.bluetape4k.feign.codec.FeignFastjsonEncoder
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.coroutines.KLoggingChannel

class VertxHttpbinSyncFastjsonTest: AbstractHttpbinSyncTest() {

    companion object: KLoggingChannel()

    override fun newBuilder(): Feign.Builder {
        return feignBuilder {
            client(VertxHttpClient())
            encoder(FeignFastjsonEncoder.INSTANCE)
            decoder(FeignFastjsonDecoder.INSTANCE)
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
