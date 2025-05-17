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
        delay(10)
        resumable.await()
        delay(10)

        resumable.resume()
        resumable.await()

        delay(10)

        resumable.resume()
        resumable.resume()
        delay(10)
        resumable.await()
    }
}
