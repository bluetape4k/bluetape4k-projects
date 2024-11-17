package io.bluetape4k.concurrent.virtualthread.examples

import io.bluetape4k.concurrent.virtualthread.AbstractVirtualThreadTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors


/**
 * ### Rule 3
 *
 * Virtual thread 사용 시, ThreadPool 방식을 사용하려면 `Executors.newVirtualThreadPerTaskExecutor()` 를 사용해야 한다
 *
 * - Virtual Thread 는 기본적으로 Pool 에 넣지 않는 것이 좋다.
 * - Virtual Thread 는 생성 비용이 낮기 때문에, 작업당 하나씩 생성하는 것이 좋다.
 */
class Rule3DoNotPooledVirtualThreads: AbstractVirtualThreadTest() {

    companion object: KLogging() {
        private const val TASK_SIZE = 1000
    }

    @Nested
    inner class DoNot {
        /**
         * 비추천 - 기본 Thread Pool 에 Virtual Thread 용 Factory 를 사용하는 방식
         *
         * 이유:
         *   - 기본 Thread Pool은 최대 Thread 수가 제한되어 있다.
         *   - 그래서 생성된 Virtual Thread 를 재활용하려고 한다.
         */
        @Test
        fun `비추천 - Thread Pool 내부에서 Virtual Thread 생성하기`() {
            Executors.newCachedThreadPool(Thread.ofVirtual().factory()).use { executor ->
                executor.javaClass.name shouldBeEqualTo "java.util.concurrent.ThreadPoolExecutor"

                val tasks = List(TASK_SIZE) {
                    executor.submit {
                        sleep(1000)
                        log.debug { "$it run ${Thread.currentThread()}" }
                    }
                }
                tasks.forEach { it.get() }
            }
        }
    }

    @Nested
    inner class Do {
        /**
         * 추천 - virtual thread per task 로 virtual thread 생성하기
         *
         * 이유:
         *  - Virtual Thread 생성 갯수에 제한이 없고, 재활용하지 않는다
         */
        @Test
        fun `추천 - VirtualThreadPerTask 로 Virtual Thread 생성하기`() {
            // NOTE: 작업당 Virtual Thread가 생성되도록 한다.
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                executor.javaClass.name shouldBeEqualTo "java.util.concurrent.ThreadPerTaskExecutor"

                val tasks = List(TASK_SIZE) {
                    executor.submit {
                        sleep(1000)
                        log.debug { "$it run ${Thread.currentThread()}" }
                    }
                }
                tasks.forEach { it.get() }
            }
        }
    }
}
