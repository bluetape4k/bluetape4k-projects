package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.output.OutputCapture
import io.bluetape4k.junit5.output.OutputCapturer
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldContain
import org.junit.jupiter.api.Test

@OutputCapture
class LoggingSupportTest {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    @Test
    fun `job logging`(capturer: OutputCapturer) = runTest {
        val job = launch {
            delay(10)
            log.debug { "Hello world!" }
        }.log("TestJob")

        job.join()

        if (log.isDebugEnabled) {
            val captured = capturer.capture()
            captured shouldContain "Hello world!"
            captured shouldContain "[TestJob] Completed"
        }
    }

    @Test
    fun `coroutine logging`(capturer: OutputCapturer) = runTest {
        val scope = CoroutineScope(coroutineContext + CoroutineName("LOGGING"))

        scope
            .launch {
                coLogging { "Hello world!" }
            }
            .join()

        // yield()
        if (log.isDebugEnabled) {
            capturer.capture() shouldContain "[LOGGING] Hello world!"
        }
    }
}
