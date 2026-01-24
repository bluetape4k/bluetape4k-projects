package io.bluetape4k.concurrent.virtualthread.examples

import io.bluetape4k.concurrent.virtualthread.AbstractVirtualThreadTest
import io.bluetape4k.concurrent.virtualthread.awaitAll
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * ### Rule 6
 *
 * Virtual Thread 사용 시에는 `synchronized` 블록을 사용하지 말고, 명시적으로 [ReentrantLock] 사용하세요
 */
class Rule6UseSynchronizedBlocksAndMethodsCarefully: AbstractVirtualThreadTest() {

    companion object: KLogging() {
        private const val TASK_SIZE = 1000
    }

    @Nested
    inner class DoNot {

        private val lockObject = Any()

        @Test
        fun `비추천 - 리소스를 독점적으로 사용할 목적으로 synchronized 사용하기`() {
            var counter = 0
            val tasks = List(TASK_SIZE) {
                virtualThread {
                    synchronized(lockObject) {
                        // critical section
                        exclusiveResouce()
                        counter += 1
                    }
                }
            }
            tasks.forEach { it.join() }
            log.trace { "counter=$counter" }
        }
    }

    @Nested
    inner class Do {
        private val lock = ReentrantLock()

        @Test
        fun `추천 - 리소스를 독점적으로 사용할 때 ReentrantLock을 이용`() {
            var counter = 0
            val tasks = List(TASK_SIZE) {
                virtualThread {
                    lock.lock()
                    try {
                        // critical section
                        exclusiveResouce()
                        counter += 1
                    } finally {
                        lock.unlock()
                    }
                }
            }
            tasks.forEach { it.join() }
            log.trace { "counter=$counter" }
            counter shouldBeEqualTo TASK_SIZE
        }

        @Test
        fun `추천 - 리소스를 독점적으로 사용할 때 ReentrantLock을 이용하세요 - Kotlin withLock 함수`() {
            var counter = 0
            val tasks = List(TASK_SIZE) {
                virtualFuture {
                    lock.withLock {
                        // critical section
                        exclusiveResouce()
                        counter += 1
                    }
                }
            }
            tasks.awaitAll()
            log.trace { "counter=$counter" }
            counter shouldBeEqualTo TASK_SIZE
        }
    }

    private fun exclusiveResouce(): String {
        sleep(2)
        return "result"
    }
}
