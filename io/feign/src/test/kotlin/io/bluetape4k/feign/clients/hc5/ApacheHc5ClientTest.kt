package io.bluetape4k.feign.clients.hc5

import feign.Feign
import feign.Logger
import feign.hc5.ApacheHttp5Client
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractClientTest
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Assumptions

class ApacheHc5ClientTest: AbstractClientTest() {

    companion object: KLogging()

    override fun newBuilder(): Feign.Builder {
        return feignBuilder {
            client(ApacheHttp5Client())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }

    override fun `very long response null length`() {
        Assumptions.assumeTrue(false, "HC5 client seems to hang with response size equalto Long.MAX")
    }
}
