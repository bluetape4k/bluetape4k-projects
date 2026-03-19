package io.bluetape4k.examples.coroutines.concurrency

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * [Mutex]와 [Semaphore]를 이용한 코루틴 동시성 제어 예제입니다.
 *
 * - [Mutex]: 상호 배제(mutual exclusion) — 하나의 코루틴만 임계 영역에 진입
 * - [Semaphore]: 동시 접근 수 제한 — N개의 코루틴이 동시에 작업 가능
 */
class MutexExamples {
    companion object: KLoggingChannel()

    /**
     * [Mutex.withLock]을 사용하여 공유 자원에 대한 동시 접근을 방지합니다.
     *
     * Mutex 없이 `counter++` 를 하면 race condition이 발생하여 결과가 달라질 수 있습니다.
     */
    @Test
    fun `Mutex로 공유 자원 보호하기`() = runTest {
        val mutex = Mutex()
        var counter = 0

        val jobs = List(1000) {
            launch(Dispatchers.Default) {
                repeat(100) {
                    mutex.withLock {
                        counter++
                    }
                }
            }
        }
        jobs.joinAll()

        log.debug { "counter=$counter" }
        counter shouldBeEqualTo 100_000
    }

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
                    advanceTimeBy(10)

                    activeCount.decrementAndGet()
                }
            }
        }
        jobs.joinAll()

        log.debug { "최대 동시 실행 수: ${maxActive.get()}" }
        // 동시에 3개 이하만 실행되어야 함
        maxActive.get() shouldBeLessOrEqualTo 3
    }
}
