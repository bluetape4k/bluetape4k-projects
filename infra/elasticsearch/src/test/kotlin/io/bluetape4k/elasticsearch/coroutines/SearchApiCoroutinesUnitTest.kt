package io.bluetape4k.elasticsearch.coroutines

import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class SearchApiCoroutinesUnitTest {

    @Test
    fun `PIT cleanup completes from cancelled coroutine context`() = runTest(timeout = 10.seconds) {
        val jobStarted = CompletableDeferred<Unit>()
        var cleanupCompleted = false

        val job = launch {
            try {
                jobStarted.complete(Unit)
                delay(1.seconds)
            } finally {
                closePointInTimeBestEffort("pit-id") {
                    delay(10)
                    cleanupCompleted = true
                    true
                }
            }
        }

        jobStarted.await()
        job.cancelAndJoin()

        cleanupCompleted.shouldBeTrue()
    }

    @Test
    fun `PIT cleanup swallows cancellation and ordinary close failures`() = runTest {
        var cancellationInvoked = false
        closePointInTimeBestEffort("cancelled-pit") {
            cancellationInvoked = true
            throw CancellationException("collector cancelled")
        }
        cancellationInvoked.shouldBeTrue()

        var failureInvoked = false
        closePointInTimeBestEffort("failed-pit") {
            failureInvoked = true
            throw IllegalStateException("close failed")
        }
        failureInvoked.shouldBeTrue()
    }

    @Test
    fun `PIT cleanup rejects a blank id before invoking close`() = runTest {
        assertFailsWith<IllegalArgumentException> {
            closePointInTimeBestEffort(" ") { true }
        }
    }
}
