package io.bluetape4k.feign.clients.vertx

import feign.Feign
import feign.Logger
import feign.slf4j.Slf4jLogger
import io.bluetape4k.feign.clients.AbstractClientTest
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Assumptions

class VertxClientTest: AbstractClientTest() {

    companion object: KLogging()

    override fun newBuilder(): Feign.Builder {
        return feignBuilder {
            client(VertxHttpClient())
            logger(Slf4jLogger(javaClass))
            logLevel(Logger.Level.FULL)
        }
    }

    override fun `very long response null length`() {
        Assumptions.assumeTrue(
            false,
            "Vertx client seems to hang with response size equalto Long.MAX"
        )
    }
}
