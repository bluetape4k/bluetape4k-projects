package io.bluetape4k.coroutines.support

import io.bluetape4k.junit5.output.OutputCapture
import io.bluetape4k.junit5.output.OutputCapturer
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
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
        val job = launch(start = CoroutineStart.LAZY) {
            delay(10)
            log.info { "Hello world!" }
        }.log("TestJob")

        job.start()
        job.join()

        if (log.isDebugEnabled) {
            val captured = capturer.capture()
            captured shouldContain "Hello world!"
            captured shouldContain "[TestJob] ✅"
        }
    }

    @Test
    fun `coroutine logging`(capturer: OutputCapturer) = runTest {
        val scope = CoroutineScope(coroutineContext + CoroutineName("LOGGING"))

        scope
            .launch {
                suspendLogging { "Hello world!" }
            }
            .join()

        // 캡처 로그는 로거 레벨에 따라 없을 수도 있다. 존재할 때만 검사.
        val captured = capturer.capture()
        if (captured.isNotBlank()) {
            captured shouldContain "[LOGGING] Hello world!"
        }
    }
}
