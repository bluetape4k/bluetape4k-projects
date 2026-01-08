package io.bluetape4k.vertx.examples

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.tests.withSuspendTestContext
import io.vertx.core.Vertx
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.codec.BodyCodec
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail

@ExtendWith(VertxExtension::class)
class SampleVerticleTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 10
    }

    @Test
    fun `count three ticks`(vertx: Vertx, testContext: VertxTestContext) {
        val counter = atomic(0)
        vertx.setPeriodic(100) {
            if (counter.incrementAndGet() == 3) {
                testContext.completeNow()
            }
        }
    }

    @Test
    fun `count three ticks with checkpoints`(vertx: Vertx, testContext: VertxTestContext) {
        val checkpoint = testContext.checkpoint(3)
        vertx.setPeriodic(100) {
            checkpoint.flag()
        }
    }

    @Test
    fun `use SampleVerticle`(vertx: Vertx, testContext: VertxTestContext) {
        val webClient = WebClient.create(vertx)
        val deploymentCheckpoint = testContext.checkpoint()
        val requestCheckpoint = testContext.checkpoint(REPEAT_SIZE)

        vertx
            .deployVerticle(SampleVerticle())
            .onSuccess {
                deploymentCheckpoint.flag()

                repeat(REPEAT_SIZE) {
                    log.debug { "Request $it" }
                    webClient.get(11981, "localhost", "/")
                        .`as`(BodyCodec.string())
                        .send()
                        .onSuccess { resp ->
                            testContext.verify {
                                resp.statusCode() shouldBeEqualTo 200
                                resp.body() shouldContain "Yo!"
                                requestCheckpoint.flag()
                            }
                        }
                        .onFailure {
                            fail(it)
                        }
                }
            }
    }

    @Test
    fun `use SampleVerticle in coroutines`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        vertx.withSuspendTestContext(testContext) {
            val webClient = WebClient.create(vertx)
            val deploymentCheckpoint = testContext.checkpoint()
            val requestCheckpoint = testContext.checkpoint(REPEAT_SIZE)

            log.debug { "Deply SampleVerticle" }
            vertx.deployVerticle(SampleVerticle()).coAwait()
            deploymentCheckpoint.flag()  //testContext 에게 현 단계까지 완료되었음을 알린다.

            val jobs = List(REPEAT_SIZE) { requestIndex ->
                launch {
                    log.debug { "Request $requestIndex" }
                    val resp =
                        webClient.get(11981, "localhost", "/")
                            .`as`(BodyCodec.string())
                            .send()
                            .coAwait()

                    log.debug { "Response $resp" }
                    testContext.verify {
                        resp.statusCode() shouldBeEqualTo 200
                        resp.body() shouldContain "Yo!"
                        // testContext에 완료되었음을 알린다 (CountDownLatch와 유사)
                        // 모두 차감하면 testContext.completeNow() 와 같이 테스트가 종료된다.
                        requestCheckpoint.flag()
                    }
                }
            }
            jobs.joinAll()
        }
    }
}
