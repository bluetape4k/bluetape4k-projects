package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.assertCancellationClearsWaiter
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class ResumableTest {

    companion object: KLoggingChannel()

    @Test
    fun `correct state`() = runTest {
        val resumable = Resumable()

        resumable.resume()
        delay(10.milliseconds)
        resumable.await()
        delay(10.milliseconds)

        resumable.resume()
        resumable.await()

        delay(10.milliseconds)

        resumable.resume()
        resumable.resume()
        delay(10.milliseconds)
        resumable.await()
    }

    @Test
    fun `cancelled await clears slot so subsequent await succeeds`() = runTest {
        val resumable = Resumable()

        assertCancellationClearsWaiter(
            awaiter = { resumable.await() },
            releaser = { resumable.resume() },
        )
    }

    @Test
    fun `READY fast path still works after invokeOnCancellation change`() = runTest {
        val resumable = Resumable()

        // resume() fires before await() — READY is set.
        resumable.resume()

        // An await() on a READY Resumable returns immediately (no suspension).
        // The invokeOnCancellation handler must not overwrite the null reset
        // that getAndSet(null) performs at the end of await().
        resumable.await()

        // A subsequent resume+await cycle must still work.
        var reached = false
        launch {
            resumable.await()
            reached = true
        }
        yield()
        resumable.resume()
        yield()

        reached shouldBeEqualTo true
    }
}
