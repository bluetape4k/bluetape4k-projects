package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.support.suspendLogging
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.random.Random

class CoroutineContextExamples {

    companion object: KLoggingChannel()

    /**
     * CoroutineScope를 delegate를 이용하면 손쉽게 만들 수 있다
     */
    class Activity: CoroutineScope by CoroutineScope(CoroutineName("activity") + Dispatchers.Default) {
        companion object: KLogging()

        fun destroy() {
            cancel()
        }

        fun doSomething() {
            // 10개의 job을 생성합니다.
            repeat(10) {
                log.debug { "Launch job $it." }
                launch {
                    delay((it + 1) * 100L)
                    log.debug { "Job $it is done." }
                }.log("Job $it")
            }
        }
    }

    @Nested
    inner class Context10 {

        @Test
        fun `CoroutineScope 상속하기`() = runTest(CoroutineName("test")) {
            val activity = Activity()

            suspendLogging { "Launched coroutines ..." }
            activity.doSomething()
            delay(10)

            suspendLogging { "Destroying activity..." }
            activity.destroy()      // cancel all child coroutines
        }
    }

    @Nested
    inner class Context8 {
        @Test
        fun `run two coroutines with name`() = runTest(CoroutineName("main")) {
            suspendLogging { "Started main coroutine" }

            val v1 = async(CoroutineName("v1")) {
                suspendLogging { "Starting v1" }
                delay(Random.nextLong(10))
                suspendLogging { "Computing v1" }
                252
            }.log("coroutines 1")

            val v2 = async(CoroutineName("v2")) {
                suspendLogging { "Starting v2" }
                delay(Random.nextLong(20))
                suspendLogging { "Computing v2" }
                6
            }.log("coroutines 2")

            val result = v1.await() / v2.await()
            suspendLogging { "The answer for v1 / v2 = $result" }
        }
    }

    // @Disabled("발표용 코드입니다")
    @Nested
    inner class Basic {
        private val jobSize = 10_000

        @Test
        fun `run many coroutines`() = runTest {
            val jobs = List(jobSize) {
                launch(Dispatchers.IO) {
                    delay(1000)
                    print(".")
                }
            }
            jobs.joinAll()
        }

        @Test
        fun `run many coroutines with coroutineScope`() = runTest {
            coroutineScope {
                val jobs = List(jobSize) {
                    launch(Dispatchers.IO) {
                        delay(1000)
                        print(".")
                    }
                }
                jobs.joinAll()
            }
        }
    }
}
