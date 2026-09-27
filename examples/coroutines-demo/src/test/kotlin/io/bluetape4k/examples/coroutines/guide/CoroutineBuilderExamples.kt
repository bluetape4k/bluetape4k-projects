package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

class CoroutineBuilderExamples {

    companion object: KLoggingChannel()

    @Test
    fun `job example`() = runTest {
        val job = launch {
            advanceTimeBy(100.milliseconds)
        }.log("job")

        job.join()
        job.isCompleted.shouldBeTrue()
    }

    @Test
    fun `async example`() = runTest {
        val task: Deferred<Long> = async {
            advanceTimeBy(1000.milliseconds)
            42L
        }.log("async")

        task.await() shouldBeEqualTo 42L
    }
}
