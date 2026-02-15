package io.bluetape4k.feign.clients.hc5

import feign.Feign
import feign.Logger
import feign.hc5.ApacheHttp5Client
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractHttpbinSyncTest
import io.bluetape4k.feign.codec.JacksonDecoder2
import io.bluetape4k.feign.codec.JacksonEncoder2
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.KLogging

class ApacheHc5HttpbinSyncJacksonTest: AbstractHttpbinSyncTest() {

    companion object: KLogging()

    override fun newBuilder(): Feign.Builder {
        return feignBuilder {
            client(ApacheHttp5Client())
            encoder(JacksonEncoder2())
            decoder(JacksonDecoder2())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }
}
