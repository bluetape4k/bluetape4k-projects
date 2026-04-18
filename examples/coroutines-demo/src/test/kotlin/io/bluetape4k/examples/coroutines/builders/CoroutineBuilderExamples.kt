package io.bluetape4k.examples.coroutines.builders

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * 코루틴 빌더(`launch`, `async`)의 기본 사용법을 보여주는 예제입니다.
 *
 * - `launch`: 결과를 반환하지 않는 코루틴 시작 (fire-and-forget)
 * - `async`: [Deferred]를 반환하여 `await()`로 결과를 받을 수 있는 코루틴 시작
 * - `awaitAll`: 복수의 `async` 결과를 한꺼번에 대기
 */
class CoroutineBuilderExamples {

    companion object: KLoggingChannel()

    @Nested
    inner class LaunchExample {
        @Test
        fun `launch coroutines`() = runTest {
            log.debug { "Start" }

            launch {
                delay(100.milliseconds)
                log.debug { "World 1" }
            }
            launch {
                delay(100.milliseconds)
                log.debug { "World 2" }
            }
            log.debug { "Hello, " }
            yield()
            log.debug { "Finish." }
        }
    }

    @Nested
    inner class AsyncExample {

        @Test
        fun `async builder example`() = runTest {
            val resultDeferred: Deferred<Int> = async {
                delay(100L.milliseconds)
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
                    delay(Random.nextLong(50, 100).milliseconds)
                    log.trace { "Return $it" }
                    "Result $it"
                }
            }
            val res = results.awaitAll()
            log.trace { "Result=${res.joinToString()}" }
        }
    }
}
