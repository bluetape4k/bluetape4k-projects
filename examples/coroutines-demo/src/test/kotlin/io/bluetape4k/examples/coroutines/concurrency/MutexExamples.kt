package io.bluetape4k.examples.coroutines.concurrency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

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
            launch {
                repeat(100) {
                    mutex.withLock {
                        counter++
                    }
                }
            }.log("Job #$it")
        }
        jobs.joinAll()

        log.debug { "counter=$counter" }
        counter shouldBeEqualTo 1000 * 100
    }

}
