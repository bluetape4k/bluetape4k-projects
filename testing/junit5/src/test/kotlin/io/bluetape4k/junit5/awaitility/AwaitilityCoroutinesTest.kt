package io.bluetape4k.junit5.awaitility

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class AwaitilityCoroutinesTest {

    companion object: KLogging()

    @Test
    fun `coAwait - awaiting suspend function`() = runSuspendTest {
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
    fun `coUntil - until suspend function`() = runSuspendTest {
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
    fun `suspendAwait - awaiting suspend function with poll interval`() = runSuspendTest {
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
    fun `suspendUntil - until suspend function with poll interval`() = runSuspendTest {
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
    fun `suspendUntil - block 예외는 전파된다`() = runSuspendTest {
        assertFailsWith<IllegalStateException> {
            await
                .pollDelay(Duration.ofMillis(10))
                .untilSuspending {
                    throw IllegalStateException("boom")
                }
        }
    }
}
