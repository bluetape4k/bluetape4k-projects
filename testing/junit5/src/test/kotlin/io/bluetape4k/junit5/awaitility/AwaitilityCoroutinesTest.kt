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

class AwaitilityCoroutinesTest {

    companion object: KLogging()

    @Test
    fun `coAwait - awaiting suspend function`() = runSuspendTest {
        val start = System.currentTimeMillis()
        val end = start + 100

        await suspendAwait {
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

        await suspendUntil {
            log.debug { "await untilSuspending ..." }
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

        await.suspendAwait(Duration.ofMillis(10)) {
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

        await.suspendUntil(Duration.ofMillis(50)) {
            log.debug { "await untilSuspending ..." }
            delay(10)
            System.currentTimeMillis() > end
        }
        yield()

        System.currentTimeMillis() shouldBeGreaterThan end
    }
}
