package io.bluetape4k.junit5.awaitility

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollDelay
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AwaitilityCoroutinesTest {

    companion object: KLoggingChannel()

    @Nested
    inner class UntilSuspending {

        @Test
        fun `awaitSuspending - awaiting suspend function`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await awaitSuspending {
                log.debug { "awaiting in suspend function." }
                delay(100.milliseconds)
                log.debug { "finish suspend function." }
            }
            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilSuspending - until suspend function`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await untilSuspending {
                log.debug { "await suspendUntil ..." }
                delay(10.milliseconds)
                System.currentTimeMillis() > end
            }
            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `awaitSuspending - awaiting suspend function with poll interval`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await
                .withPollDelay(100.milliseconds)
                .awaitSuspending {
                    log.debug { "awaiting in suspend function." }
                    delay(100.milliseconds)
                    log.debug { "finish suspend function." }
                }
            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilSuspending - until suspend function with poll interval`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await
                .withPollDelay(50.milliseconds)
                .untilSuspending {
                    log.debug { "await suspendUntil ..." }
                    delay(10.milliseconds)
                    System.currentTimeMillis() > end
                }

            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilSuspending - block 예외는 전파된다`() = runSuspendIO {
            assertFailsWith<IllegalStateException> {
                await
                    .withPollDelay(10.milliseconds)
                    .untilSuspending {
                        throw IllegalStateException("boom")
                    }
            }
        }

        @Test
        fun `untilSuspending - 조건이 계속 false 이면 timeout 예외가 발생한다`() = runSuspendIO {
            assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .withPollInterval(10.milliseconds)
                    .untilSuspending { false }
            }
        }

        @Test
        fun `untilSuspending - 무시된 예외로 timeout 되면 마지막 원인을 유지한다`() = runSuspendIO {
            val exception = assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .withPollInterval(10.milliseconds)
                    .ignoreExceptions()
                    .untilSuspending {
                        throw IllegalStateException("poll timed out")
                    }
            }

            exception.findRootCause().shouldBeInstanceOf<IllegalStateException>()
        }

        @Test
        fun `untilSuspending - atMost 보다 긴 poll block 도 전체 timeout 내에서 중단한다`() = runSuspendIO {
            val start = System.currentTimeMillis()

            assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .untilSuspending {
                        delay(500.milliseconds)
                        false
                    }
            }

            val elapsed = System.currentTimeMillis() - start
            elapsed shouldBeLessOrEqualTo 400
        }

        @Test
        fun `untilSuspending - block 내부 timeout 은 그대로 전파한다`() = runSuspendIO {
            assertFailsWith<TimeoutCancellationException> {
                await
                    .atMost(1.seconds)
                    .withPollDelay(Duration.ZERO)
                    .untilSuspending {
                        withTimeout(50.milliseconds) {
                            delay(500.milliseconds)
                            true
                        }
                    }
            }
        }
    }

    @Nested
    inner class UntilAssertedSuspending {

        @Test
        fun `untilAssertedSuspending - until suspend function`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await untilAssertedSuspending {
                log.debug { "await suspendUntil ..." }
                delay(10.milliseconds)
                System.currentTimeMillis() shouldBeGreaterThan end
            }
            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilAssertedSuspending - awaiting suspend function with poll interval`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await
                .withPollDelay(100.milliseconds)
                .untilAssertedSuspending {
                    log.debug { "awaiting in suspend function." }
                    delay(100.milliseconds)
                    log.debug { "finish suspend function." }
                }
            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilAssertedSuspending - until suspend function with poll interval`() = runSuspendIO {
            val start = System.currentTimeMillis()
            val end = start + 100

            await
                .withPollDelay(50.milliseconds)
                .untilAssertedSuspending {
                    log.debug { "await suspendUntil ..." }
                    delay(10.milliseconds)
                    System.currentTimeMillis() shouldBeGreaterThan end
                }

            yield()

            System.currentTimeMillis() shouldBeGreaterThan end
        }

        @Test
        fun `untilAssertedSuspending - block 예외는 전파된다`() = runSuspendIO {
            assertFailsWith<IllegalStateException> {
                await
                    .withPollDelay(10.milliseconds)
                    .untilAssertedSuspending {
                        throw IllegalStateException("boom")
                    }
            }
        }

        @Test
        fun `untilAssertedSuspending - 조건이 계속 false 이면 timeout 예외가 발생한다`() = runSuspendIO {
            assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .withPollInterval(10.milliseconds)
                    .untilAssertedSuspending {
                        fail("condition remains false")
                    }
            }
        }

        @Test
        fun `untilAssertedSuspending - timeout 은 마지막 assertion 원인을 보존한다`() = runSuspendIO {
            val exception = assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .withPollInterval(10.milliseconds)
                    .untilAssertedSuspending {
                        fail("status never became ready")
                    }
            }

            exception.findRootCause().message shouldContain "status never became ready"
        }

        @Test
        fun `untilAssertedSuspending - 무시된 예외로 timeout 되면 마지막 원인을 유지한다`() = runSuspendIO {
            val exception = assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .withPollInterval(10.milliseconds)
                    .ignoreExceptions()
                    .untilAssertedSuspending {
                        throw IllegalStateException("poll timed out")
                    }
            }

            exception.findRootCause().shouldBeInstanceOf<IllegalStateException>()
        }

        @Test
        fun `untilAssertedSuspending - atMost 보다 긴 poll block 도 전체 timeout 내에서 중단한다`() = runSuspendIO {
            val start = System.currentTimeMillis()

            assertFailsWith<ConditionTimeoutException> {
                await
                    .atMost(150.milliseconds)
                    .withPollDelay(Duration.ZERO)
                    .untilAssertedSuspending {
                        delay(500.milliseconds)
                        fail("poll delay timed out")
                    }
            }

            val elapsed = System.currentTimeMillis() - start
            elapsed shouldBeLessOrEqualTo 400
        }

        @Test
        fun `untilAssertedSuspending - block 내부 timeout 은 그대로 전파한다`() = runSuspendIO {
            assertFailsWith<TimeoutCancellationException> {
                await
                    .atMost(1.seconds)
                    .withPollDelay(Duration.ZERO)
                    .untilAssertedSuspending {
                        withTimeout(50.milliseconds) {
                            delay(500.milliseconds)
                        }
                    }
            }
        }
    }

    private tailrec fun Throwable.findRootCause(): Throwable = cause?.findRootCause() ?: this
}
