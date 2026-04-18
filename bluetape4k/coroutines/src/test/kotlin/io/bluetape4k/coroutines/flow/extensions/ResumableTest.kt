package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ResumableTest {

    companion object: KLoggingChannel()

    @Test
    fun `correct state`() = runTest {
        val resumable = Resumable()

        resumable.resume()
        delay(timeMillis = 10)
        resumable.await()
        delay(timeMillis = 10)

        resumable.resume()
        resumable.await()

        delay(timeMillis = 10)

        resumable.resume()
        resumable.resume()
        delay(timeMillis = 10)
        resumable.await()
    }
}
