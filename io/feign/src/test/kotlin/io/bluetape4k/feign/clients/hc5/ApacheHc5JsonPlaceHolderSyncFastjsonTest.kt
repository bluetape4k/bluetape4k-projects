package io.bluetape4k.feign.clients.hc5

import feign.Feign
import feign.Logger
import feign.hc5.ApacheHttp5Client
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractJsonPlaceHolderSyncTest
import io.bluetape4k.feign.codec.FeignFastjsonDecoder
import io.bluetape4k.feign.codec.FeignFastjsonEncoder
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.KLogging

class ApacheHc5JsonPlaceHolderSyncFastjsonTest: AbstractJsonPlaceHolderSyncTest() {

    companion object: KLogging()

    override fun newBuilder(): Feign.Builder {
        return feignBuilder {
            client(ApacheHttp5Client())
            encoder(FeignFastjsonEncoder.INSTANCE)
            decoder(FeignFastjsonDecoder.INSTANCE)
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
