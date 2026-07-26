package io.bluetape4k.feign.clients.vertx

import feign.Feign
import feign.Logger
import feign.slf4j.Slf4jLogger
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.feign.clients.AbstractClientTest
import io.bluetape4k.feign.defaultRequestOptions
import io.bluetape4k.feign.feignBuilder
import io.bluetape4k.feign.feignRequestOf
import io.bluetape4k.logging.KLogging
import io.vertx.core.Vertx
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

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

    @Test
    fun `execute fails fast on Vertx event loop thread`() {
        val vertx = Vertx.vertx()
        val client = VertxHttpClient()
        val latch = CountDownLatch(1)
        val errorRef = AtomicReference<Throwable?>()

        try {
            vertx.runOnContext {
                errorRef.set(
                    runCatching {
                        client.execute(
                            feignRequestOf("http://localhost/"),
                            defaultRequestOptions
                        )
                    }.exceptionOrNull()
                )
                latch.countDown()
            }

            latch.await(2, TimeUnit.SECONDS).shouldBeTrue()
            val error = errorRef.get()
            (error is IllegalStateException) shouldBeEqualTo true
            error?.message shouldContain "must not be called from a Vert.x event loop thread"
        } finally {
            client.close()
            vertx.close()
        }
    }
}
