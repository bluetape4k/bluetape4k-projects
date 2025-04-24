package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.junit5.coroutines.runSuspendVT
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.RepeatedTest
import kotlin.system.measureTimeMillis

class VirtualThreadDispatcherTest {

    companion object: KLogging() {
        private const val TASK_SIZE = 1000
        private const val SLEEP_TIME = 500L

        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread Dispatcher를 이용하여 비동기 작업하기`() = runSuspendTest {
        val dispather = Dispatchers.VT
        val elapsedTime = measureTimeMillis {
            val tasks = List(TASK_SIZE) {
                launch(dispather) {
                    delay(SLEEP_TIME)
                    log.trace { "Task $it is done" }
                }
            }
            tasks.joinAll()
        }
        log.debug { "Elapsed time: $elapsedTime ms" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread Dispatcher를 이용하여 Job 실행하기`() = runSuspendTest(Dispatchers.newVT) {
        val elapsedTime = measureTimeMillis {
            val jobs = List(TASK_SIZE) {
                launch {
                    delay(SLEEP_TIME)
                    log.trace { "Job $it is done" }
                }
            }
            jobs.joinAll()
        }
        log.debug { "Elapsed time: $elapsedTime ms" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread Dispatcher with withContext`() = runSuspendVT {
        val elapsedTime = measureTimeMillis {
            val jobs = List(TASK_SIZE) {
                launch {
                    withContext(coroutineContext) {
                        Thread.sleep(SLEEP_TIME)
                        log.trace { "Job $it is done" }
                    }
                }
            }
            jobs.joinAll()
        }
        log.debug { "Elapsed time: $elapsedTime ms" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `multi job with virtual thread dispatcher`() = runSuspendVT {
        val jobNumber = atomic(0)

        // 1초씩 delay 하는 TASK_SIZE개의 작업을 수행 시 거의 1초에 완료된다 (Virtual Thread)
        MultijobTester()
            .numThreads(TASK_SIZE)
            .roundsPerJob(1)
            .add {
                delay(SLEEP_TIME)
                log.trace { "Job[${jobNumber.incrementAndGet()}] is done" }
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `multi job with default dispatcher`() = runSuspendDefault {
        val jobNumber = atomic(0)

        // 1초씩 delay 하는 TASK_SIZE개의 작업을 수행 시 거의 1초에 완료된다 (Default Dispatcher)
        MultijobTester()
            .numThreads(TASK_SIZE)
            .roundsPerJob(1)
            .add {
                delay(SLEEP_TIME)
                log.trace { "Job[${jobNumber.incrementAndGet()}] is done" }
            }
            .run()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `multi job with IO dispatcher`() = runSuspendIO {
        val jobNumber = atomic(0)

        // 1초씩 delay 하는 TASK_SIZE개의 작업을 수행 시 거의 1초에 완료된다 (Default Dispatcher)
        MultijobTester()
            .numThreads(TASK_SIZE)
            .roundsPerJob(1)
            .add {
                delay(SLEEP_TIME)
                log.trace { "Job[${jobNumber.incrementAndGet()}] is done" }
            }
            .run()
    }
}
