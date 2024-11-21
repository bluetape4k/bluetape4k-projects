package io.bluetape4k.examples.coroutines.builders

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CoroutineBuilderExamples {

    companion object: KLogging()

    @Nested
    inner class LaunchExample {

        @Test
        fun `launch coroutines`() = runTest {
            log.debug { "Start" }

            launch {
                delay(100)
                log.trace { "World 1" }
            }
            launch {
                delay(100)
                log.trace { "World 2" }
            }
            log.trace { "Hello, " }
            yield()
            log.trace { "Finish." }
        }
    }

    @Nested
    inner class AsyncExample {

        @Test
        fun `async builder example`() = runTest {
            val resultDeferred = async {
                delay(100L)
                log.trace { "Return result=42" }
                42
            }
            log.trace { "Build coroutines and await ..." }
            val result = resultDeferred.await()
            log.trace { "result=$result" }
            log.trace { "result=${resultDeferred.await()}" }
            log.trace { "Finish" }
        }

        @Test
        fun `await returns`() = runTest {
            val results = List(10) {
                async {
                    delay(Random.nextLong(50, 100))
                    log.trace { "Return $it" }
                    "Result $it"
                }
            }
            val res = results.awaitAll()
            log.trace { "Result=${res.joinToString()}" }
        }
    }
}
