package io.bluetape4k.examples.vertx.webclient

import io.bluetape4k.jackson.Jackson
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.tests.withSuspendTestContext
import io.bluetape4k.vertx.web.suspendHandler
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.coAwait
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class ResponseExamples {

    companion object: KLoggingChannel() {
        private const val port: Int = 9999
    }

    data class User(
        val firstname: String = "",
        val lastname: String = "",
        val male: Boolean = true,
    ): java.io.Serializable

    private val expectedUser = User("John", "Dow", true)
    private val mapper = Jackson.defaultJsonMapper

    class JsonServer: CoroutineVerticle() {

        private val user = User("John", "Dow", true)
        private val mapper = Jackson.defaultJsonMapper

        override suspend fun start() {
            val router = Router.router(vertx)

            router.route("/").suspendHandler { ctx ->
                ctx.request().response()
                    .putHeader("content-type", "application/json")
                    .end(mapper.writeValueAsString(user))
            }

            vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .coAwait()

            log.debug { "Server started. http://localhost:$port" }
        }
    }

    @Test
    fun `response as json object`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.withSuspendTestContext(testContext) {
            vertx.deployVerticle(JsonServer()).coAwait()

            val client = WebClient.create(vertx)
            val response = client
                .put(port, "localhost", "/")
                .`as`(BodyCodec.jsonObject())
                .send()
                .coAwait()

            log.debug { "Response body=${response.body().encodePrettily()}" }
            response.statusCode() shouldBeEqualTo 200
        }
    }

    @Test
    fun `response as custom class`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.withSuspendTestContext(testContext) {
            vertx.deployVerticle(JsonServer()).coAwait()

            val client = WebClient.create(vertx)
            val response = client
                .put(port, "localhost", "/")
                .`as`(BodyCodec.json(User::class.java))
                .send()
                .coAwait()

            log.debug { "Response body=${response.body()}" }
            response.statusCode() shouldBeEqualTo 200
            val responseUser = response.body()
            responseUser shouldBeEqualTo expectedUser
        }
    }

}
