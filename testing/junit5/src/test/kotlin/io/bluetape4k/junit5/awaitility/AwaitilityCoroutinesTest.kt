package io.bluetape4k.junit5.awaitility

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeGreaterThan
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test

class AwaitilityCoroutinesTest {

    companion object: KLogging()

    @Test
    fun `coAwait - awaiting suspend function`() = runSuspendTest {
        val start = System.currentTimeMillis()
        val end = start + 100

        await coAwait {
            log.trace { "awaiting in suspend function." }
            delay(100)
            log.trace { "finish suspend function." }
        }
        yield()

        System.currentTimeMillis() shouldBeGreaterThan end
    }

    @Test
    fun `coUntil - until suspend function`() = runSuspendTest {
        val start = System.currentTimeMillis()
        val end = start + 100

        await coUntil {
            log.trace { "await untilSuspending ..." }
            System.currentTimeMillis() > end
        }

        System.currentTimeMillis() shouldBeGreaterThan end
    }
}
