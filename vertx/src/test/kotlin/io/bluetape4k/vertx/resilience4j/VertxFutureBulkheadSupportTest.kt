package io.bluetape4k.vertx.resilience4j

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.asCompletableFuture
import io.bluetape4k.vertx.tests.withTestContext
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.kotlin.bulkhead.BulkheadConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

@Suppress("UNUSED_PARAMETER")
class VertxFutureBulkheadSupportTest: AbstractVertxFutureTest() {

    companion object: KLoggingChannel()

    private var permittedEvents = 0
    private var rejectedEvents = 0
    private var finishedEvents = 0

    private fun Bulkhead.registerEventListener(): Bulkhead = apply {
        eventPublisher.apply {
            onCallPermitted {
                permittedEvents++
                log.debug { "call permitted. $it" }
            }
            onCallRejected {
                rejectedEvents++
                log.debug { "call rejected. $it" }
            }
            onCallFinished {
                finishedEvents++
                log.debug { "call finished. $it" }
            }
        }
    }

    @BeforeEach
    fun setup() {
        permittedEvents = 0
        rejectedEvents = 0
        finishedEvents = 0
    }

    @Test
    fun `bulkhead가 허용하면 성공 함수가 실행된다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val bulkhead = Bulkhead.ofDefaults("test").registerEventListener()
            val service = VertxHelloWorldService()

            val future = bulkhead.executeVertxFuture {
                service.returnHelloWorld()
            }

            val result = future.asCompletableFuture().get()

            result shouldBeEqualTo "Hello world"
            permittedEvents shouldBeEqualTo 1
            rejectedEvents shouldBeEqualTo 0
            finishedEvents shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `bulkhead가 허용되지 않으면 함수 실행을 하지 않습니다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val bulkhead = Bulkhead.of("test") {
                BulkheadConfig {
                    maxConcurrentCalls(1)
                    maxWaitDuration(Duration.ZERO)
                }
            }.registerEventListener()

            val service = VertxHelloWorldService()

            val executor = Executors.newFixedThreadPool(2)

            try {

                executor.submit {
                    bulkhead.executeVertxFuture {
                        Thread.sleep(100)
                        service.returnHelloWorld()
                    }
                }
                executor.submit {
                    bulkhead.executeVertxFuture {
                        Thread.sleep(100)
                        service.returnHelloWorld()
                    }
                }
            } finally {
                executor.shutdown()
                executor.awaitTermination(1, TimeUnit.SECONDS)
            }

            permittedEvents shouldBeEqualTo 1
            rejectedEvents shouldBeEqualTo 1
            finishedEvents shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }


    @Test
    fun `bulkhead가 허용하면 예외 함수도 실행된다`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val bulkhead = Bulkhead.ofDefaults("test").registerEventListener()
            val service = VertxHelloWorldService()

            val future = bulkhead.executeVertxFuture {
                service.throwException()
            }

            assertFailsWith<RuntimeException> {
                future.asCompletableFuture().join()
            }

            future.failed().shouldBeTrue()
            permittedEvents shouldBeEqualTo 1
            rejectedEvents shouldBeEqualTo 0
            finishedEvents shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }

    @Test
    fun `decorate vertx future`(vertx: Vertx, testContext: VertxTestContext) =
        withTestContext(testContext) {
            val bulkhead = Bulkhead.ofDefaults("test").registerEventListener()
            val service = VertxHelloWorldService()

            val decorated = bulkhead.decorateVertxFuture {
                service.returnHelloWorld()
            }
            val future = decorated.invoke()
            val result = future.asCompletableFuture().get()

            result shouldBeEqualTo "Hello world"
            permittedEvents shouldBeEqualTo 1
            rejectedEvents shouldBeEqualTo 0
            finishedEvents shouldBeEqualTo 1

            service.invocationCount shouldBeEqualTo 1
        }
}
