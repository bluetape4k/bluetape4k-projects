package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

/**
 * Virtual Thread를 이용하여 Coroutine 작업을 Blocking 방식으로 수행합니다.
 */
class CoroutineSupportTest {

    companion object: KLogging() {
        private const val TASK_SIZE = 1000
    }

    @Test
    fun `runVirtualBlocking 을 이용하여 suspend 함수 실행`() {
        val result = runVirtualBlocking {
            Thread.sleep(1000)
            log.trace { "Job is done" }
            42
        }
        result shouldBeEqualTo 42
    }

    @Test
    fun `withVirtualContext 를 이용하여 동기 함수를 Coroutines으로 실행하기`() = runTest {
        val elapsedTime = measureTimeMillis {
            val jobs = List(TASK_SIZE) {
                launch {
                    withVirtualContext {
                        Thread.sleep(1000)
                        log.trace { "Job $it is done" }
                    }
                }
            }
            jobs.joinAll()
        }
        log.debug { "Elapsed time: $elapsedTime ms" }
    }
}
