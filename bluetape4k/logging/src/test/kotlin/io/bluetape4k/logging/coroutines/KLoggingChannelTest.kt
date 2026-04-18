package io.bluetape4k.logging.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KLoggingChannelTest {

    companion object: KLoggingChannel()

    private val error get() = RuntimeException("Boom!")

    @AfterAll
    fun cleanup() {
        // 채널에 남은 이벤트가 처리될 시간을 확보합니다.
        Thread.sleep(100)
    }

    @Test
    fun `logging trace`() = runTest {
        trace { "Message at ${Instant.now()}" }
        trace(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging debug`() = runTest {
        debug { "Message at ${Instant.now()}" }
        debug(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging info`() = runTest {
        info { "Message at ${Instant.now()}" }
        info(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging warn`() = runTest {
        warn { "Message at ${Instant.now()}" }
        warn(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging error`() = runTest {
        error { "Message at ${Instant.now()}" }
        error(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging in coroutines`() = runTest {
        val jobs = List(10) {
            launch(Dispatchers.IO) {
                debug { "Message at $it" }
            }
        }
        jobs.joinAll()
    }

    @Test
    fun `log message with suspend function`() = runTest {
        debug { "delay=${runSuspending(100)}" }
    }

    private suspend fun runSuspending(delayMillis: Long = 100): Long {
        kotlinx.coroutines.delay(delayMillis.milliseconds)
        return delayMillis
    }
}
