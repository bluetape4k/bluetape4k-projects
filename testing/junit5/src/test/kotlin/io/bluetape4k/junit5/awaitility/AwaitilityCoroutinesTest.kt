package io.bluetape4k.junit5.awaitility

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.awaitility.core.ConditionTimeoutException
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AwaitilityCoroutinesTest {

    companion object: KLoggingChannel()

    @Test
    fun `awaitSuspending - awaiting suspend function`() = runSuspendIO {
        val start = System.currentTimeMillis()
        val end = start + 100

        await awaitSuspending {
            log.debug { "awaiting in suspend function." }
            delay(100)
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
            delay(10)
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
            .pollDelay(Duration.ofMillis(100))
            .awaitSuspending {
                log.debug { "awaiting in suspend function." }
                delay(100)
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
            .pollDelay(Duration.ofMillis(50))
            .untilSuspending {
                log.debug { "await suspendUntil ..." }
                delay(10)
                System.currentTimeMillis() > end
            }

        yield()

        System.currentTimeMillis() shouldBeGreaterThan end
    }

    @Test
    fun `untilSuspending - block 예외는 전파된다`() = runSuspendIO {
        assertFailsWith<IllegalStateException> {
            await
                .pollDelay(Duration.ofMillis(10))
                .untilSuspending {
                    throw IllegalStateException("boom")
                }
        }
    }

    @Test
    fun `untilSuspending - 조건이 계속 false 이면 timeout 예외가 발생한다`() = runSuspendIO {
        assertFailsWith<ConditionTimeoutException> {
            await
                .atMost(Duration.ofMillis(150))
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(10))
                .untilSuspending { false }
        }
    }

    @Test
    fun `untilSuspending - 무시된 예외로 timeout 되면 마지막 원인을 유지한다`() = runSuspendIO {
        val exception = assertFailsWith<ConditionTimeoutException> {
            await
                .atMost(Duration.ofMillis(150))
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(10))
                .ignoreExceptions()
                .untilSuspending {
                    throw IllegalStateException("poll timed out")
                }
        }

        assertIs<IllegalStateException>(exception.findRootCause())
    }

    @Test
    fun `untilSuspending - atMost 보다 긴 poll block 도 전체 timeout 내에서 중단한다`() = runSuspendIO {
        val start = System.currentTimeMillis()

        assertFailsWith<ConditionTimeoutException> {
            await
                .atMost(Duration.ofMillis(150))
                .pollDelay(Duration.ZERO)
                .untilSuspending {
                    delay(500)
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
                .atMost(Duration.ofSeconds(1))
                .pollDelay(Duration.ZERO)
                .untilSuspending {
                    withTimeout(50) {
                        delay(500)
                        true
                    }
                }
        }
    }

    private tailrec fun Throwable.findRootCause(): Throwable = cause?.findRootCause() ?: this
}
