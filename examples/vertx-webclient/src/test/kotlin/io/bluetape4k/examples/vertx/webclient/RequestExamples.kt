package io.bluetape4k.examples.vertx.webclient

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.tests.withSuspendTestContext
import io.bluetape4k.vertx.web.suspendHandler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class RequestExamples {

    companion object: KLoggingChannel() {
        private const val port: Int = 9989
    }

    class PostStringServer: CoroutineVerticle() {
        override suspend fun start() {
            val router = Router.router(vertx)

            // Request Body를 얻기 위해서는 `BodyHandler` 를 추가해야 합니다.
            router.route().handler(BodyHandler.create())

            router.route("/simple").suspendHandler { ctx ->
                log.debug { "routing context=${ctx.body().asString()}" }
                ctx.response().end("OK")
            }

            vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .coAwait()

            log.debug { "Server started. http://localhost:$port" }
        }
    }

    @Test
    fun `put simple string as request body`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.withSuspendTestContext(testContext) {
            vertx.deployVerticle(PostStringServer()).coAwait()

            val client = WebClient.create(vertx)
            val body = Buffer.buffer("Hello World!")
            val response = client
                .put(port, "localhost", "/simple")
                .`as`(BodyCodec.string())
                .sendBuffer(body)
                .coAwait()

            log.debug { "Response body=${response.body()}" }
            response.statusCode() shouldBeEqualTo 200
            response.body() shouldBeEqualTo "OK"
        }
    }
}
