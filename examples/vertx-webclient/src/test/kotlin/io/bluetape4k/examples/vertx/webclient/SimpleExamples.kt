package io.bluetape4k.examples.vertx.webclient

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.info
import io.bluetape4k.vertx.tests.withTestContextSuspending
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class SimpleExamples {

    companion object: KLoggingChannel() {
        private const val port: Int = 8080
    }

    class SimpleServer: AbstractVerticle() {
        override fun start(promise: Promise<Void>) {
            vertx.createHttpServer()
                .requestHandler { req ->
                    req.response().end("Hello World!")
                }
                .listen(port)
                .onSuccess {
                    log.info { "Server started on port 8080" }
                    promise.complete()
                }
                .onFailure { error ->
                    log.error(error) { "Server failed to start" }
                    promise.fail(error)
                }
        }
    }

    @Test
    fun `use webclient to simple server`(vertx: Vertx, testContext: VertxTestContext) = runSuspendTest {
        vertx.withTestContextSuspending(testContext) {
            vertx.deployVerticle(SimpleServer()).coAwait()

            val client = WebClient.create(vertx)
            val response = client.get(port, "localhost", "/")
                .`as`(BodyCodec.string())
                .send()
                .coAwait()

            log.debug { "Response body=${response.body()}" }
            response.body() shouldBeEqualTo "Hello World!"
        }
    }
}
