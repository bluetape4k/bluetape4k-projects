package io.bluetape4k.vertx.examples

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.AbstractVertxTest
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class VertxJunit5Examples: AbstractVertxTest() {

    companion object: KLoggingChannel()

    /**
     * [A test context for asynchronous executions](https://vertx.io/docs/vertx-junit5/java/#_a_test_context_for_asynchronous_executions)
     */
    @Test
    fun `start http server in junit env`(vertx: Vertx, testContext: VertxTestContext) {
        vertx.createHttpServer()
            .requestHandler { it.response().end() }
            .listen(16969)
            .onComplete(testContext.succeedingThenComplete())  // 성공하면 바로 종료하도록 합니다.

        testContext.awaitCompletion(5, TimeUnit.SECONDS).shouldBeTrue()  // 5초 안에 종료하면 true 반환
        testContext.causeOfFailure().shouldBeNull()
    }

    /**
     * [Use any assertion library](https://vertx.io/docs/vertx-junit5/java/#_use_any_assertion_library)
     */
    @Test
    fun `use any assertion library`(vertx: Vertx, testContext: VertxTestContext) {
        vertx.createHttpServer()
            .requestHandler { req ->
                req.response().end("Plop")
            }
            .listen(8888)
            .onSuccess {
                val client = vertx.createHttpClient()

                // GET http://localhost:8080/ 에 요청하면 "Plop" 이 반환되는지 여부를 확인하는 코드입니다.
                client.request(HttpMethod.GET, 8888, "localhost", "/")
                    .compose { req ->
                        req.send().compose { resp -> resp.body() }
                    }
                    .onComplete(testContext.succeeding { buffer ->
                        testContext.verify {
                            buffer.toString() shouldBeEqualTo "Plop"
                            testContext.completeNow()
                        }
                    })
            }
    }


    /**
     * [Chekpoint when there are multiple success conditions](https://vertx.io/docs/vertx-junit5/java/#_checkpoint_when_there_are_multiple_success_conditions)
     */
    @Test
    fun `chekpoint when there are multiple success conditions`(vertx: Vertx, testContext: VertxTestContext) {
        val serverStarted = testContext.checkpoint()
        val requestesServed = testContext.checkpoint(10)
        val responsesReceived = testContext.checkpoint(10)

        vertx.createHttpServer()
            .requestHandler { req ->
                req.response().end("OK")
                requestesServed.flag()
            }
            .listen(8888)
            .onSuccess {
                serverStarted.flag()

                val client = vertx.createHttpClient()
                repeat(10) {
                    client.request(HttpMethod.GET, 8888, "localhost", "/")
                        .compose { req ->
                            req.send().compose { res -> res.body() }
                        }
                        .onComplete(testContext.succeeding { buffer ->
                            testContext.verify {
                                buffer.toString() shouldBeEqualTo "OK"
                                responsesReceived.flag()
                            }
                        })
                }
            }
    }

    /**
     * [Repeated test example](https://vertx.io/docs/vertx-junit5/java/#_integration_with_junit_5)
     */
    @Nested
    inner class RepeatedTestExample {

        inner class HttpServerVerticle: AbstractVerticle() {
            override fun start() {
                vertx.createHttpServer()
                    .requestHandler { req ->
                        req.response()
                            .putHeader("content-type", "text/html")
                            .end("<html><body><h1>Hello from $this</h1></body></html>")
                    }
                    .listen(9999)
            }
        }

        @BeforeEach
        fun `deploy verticle`(vertx: Vertx, testContext: VertxTestContext) {
            runBlocking(vertx.dispatcher()) {
                vertx.deployVerticle(HttpServerVerticle()).coAwait()
                testContext.completeNow()
            }
        }

        @RepeatedTest(3)
        fun `check http server response`(
            vertx: Vertx,
            testContext: VertxTestContext,
        ) = runSuspendTest(vertx.dispatcher()) {
            val client = vertx.createHttpClient()
            val request = client.request(HttpMethod.GET, 9999, "localhost", "/").coAwait()

            val response = request.send().coAwait()
            val buffer = response.body().coAwait()

            testContext.verify {
                val responseText = buffer.toString()
                log.debug { "response: $responseText" }
                responseText shouldContain "Hello from"
                testContext.completeNow()
            }
        }
    }
}
