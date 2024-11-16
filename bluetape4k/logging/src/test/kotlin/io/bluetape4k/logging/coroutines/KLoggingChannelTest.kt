package io.bluetape4k.logging.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.time.Instant

class KLoggingChannelTest {

    companion object: KLoggingChannel()

    private val error get() = RuntimeException("Boom!")

    @AfterAll
    fun cleanup() {
        Thread.sleep(100)
    }

    @Test
    fun `logging trace`() = runBlocking<Unit> {
        trace { "Message at ${Instant.now()}" }
        trace(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging debug`() = runBlocking<Unit> {
        debug { "Message at ${Instant.now()}" }
        debug(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging info`() = runBlocking<Unit> {
        info { "Message at ${Instant.now()}" }
        info(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging warn`() = runBlocking<Unit> {
        warn { "Message at ${Instant.now()}" }
        warn(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging error`() = runBlocking<Unit> {
        error { "Message at ${Instant.now()}" }
        error(error) { "Error at ${Instant.now()}" }
    }

    @Test
    fun `logging in coroutines`() = runBlocking<Unit> {
        val jobs = List(10) {
            launch(Dispatchers.IO) {
                debug { "Message at $it" }
            }
        }
        jobs.joinAll()
    }

    @Test
    fun `log message with suspend function`() = runBlocking {
        debug { "delay=${runSuspending(100)}" }
    }

    private suspend fun runSuspending(delayMillis: Long = 100): Long {
        delay(delayMillis)
        return delayMillis
    }
}
