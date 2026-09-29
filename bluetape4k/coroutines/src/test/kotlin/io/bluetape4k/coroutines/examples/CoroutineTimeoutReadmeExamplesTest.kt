package io.bluetape4k.coroutines.examples

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.coroutines.suspendBlockingLazy
import io.bluetape4k.coroutines.support.awaitUntil
import io.bluetape4k.coroutines.support.awaitUntilOrNull
import io.bluetape4k.coroutines.support.joinUntil
import io.bluetape4k.junit5.coroutines.runSuspendIO
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class CoroutineTimeoutReadmeExamplesTest {

    @Test
    fun `coroutine timeout README examples compile and run`() = runSuspendIO(timeout = 5.seconds) {
        val lazyValue = suspendBlockingLazy { 42 }
        val lazyResult: Int = lazyValue.getUntil(5.seconds)
        lazyResult shouldBeEqualTo 42

        val deferred = CompletableDeferred(42)
        val result: Int = deferred.awaitUntil()
        val optional: Int? = deferred.awaitUntilOrNull(5.seconds)
        result shouldBeEqualTo 42
        optional shouldBeEqualTo 42

        val completedJob = Job().apply { complete() }
        completedJob.joinUntil(5.seconds)
    }
}
