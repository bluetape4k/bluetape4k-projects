package io.bluetape4k.examples.coroutines.concurrency

import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class SemaphoreExamples {

    companion object: KLoggingChannel()

    /**
     * [Semaphore]를 사용하여 동시에 N개의 코루틴만 작업을 수행하도록 제한합니다.
     *
     * 예: 외부 API 호출 시 동시 요청 수를 제한하는 패턴
     */
    @Test
    fun `Semaphore로 동시 접근 수 제한하기`() = runTest {
        val semaphore = Semaphore(permits = 3)
        val activeCount = AtomicInteger(0)
        val maxActive = AtomicInteger(0)

        val jobs = List(100) { index ->
            launch(Dispatchers.Default) {
                semaphore.withPermit {
                    val current = activeCount.incrementAndGet()
                    // 최대 동시 실행 수 기록
                    maxActive.updateAndGet { max -> maxOf(max, current) }
                    log.debug { "Job $index 실행 중 (동시 $current 개)" }

                    // 작업 시뮬레이션
                    // delay(10)
                    advanceTimeBy(10.milliseconds)

                    activeCount.decrementAndGet()
                }
            }.log("Job $index")
        }
        jobs.joinAll()

        log.debug { "최대 동시 실행 수: ${maxActive.get()}" }

        // 동시에 3개 이하만 실행되어야 함
        maxActive.get() shouldBeLessOrEqualTo 3
    }
}
