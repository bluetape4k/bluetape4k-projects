package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CoroutineBuilderExamples {

    companion object: KLoggingChannel()

    @Test
    fun `job example`() = runTest {
        val job = launch(Dispatchers.Default) {
            delay(1000)
        }.log("job")
        job.join()
    }

    @Test
    fun `async example`() = runTest {
        val task: Deferred<Long> = async(Dispatchers.IO) {
            delay(1000)
            42L
        }.log("task")
        task.await()
    }
}
