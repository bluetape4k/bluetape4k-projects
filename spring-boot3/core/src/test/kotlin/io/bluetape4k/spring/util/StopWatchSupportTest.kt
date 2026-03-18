package io.bluetape4k.spring.util

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.AbstractSpringTest
import kotlinx.coroutines.delay
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import org.springframework.util.StopWatch

class StopWatchSupportTest: AbstractSpringTest() {

    companion object: KLogging()

    @Test
    fun `run with StopWatch`() {
        val sw = withStopWatch("test") {
            Thread.sleep(100)
        }

        sw.totalTimeMillis shouldBeGreaterOrEqualTo 100L
        println(sw.prettyPrint())
    }

    @Test
    fun `run with StopWatch with coroutines`() = runSuspendTest {
        val sw = withSuspendStopWatch("coroutines") {
            delay(100)
            print("block")
        }

        println(sw.prettyPrint())
        sw.totalTimeMillis shouldBeGreaterOrEqualTo 100L
    }

    @Test
    fun `run tasks`() {
        val sw = StopWatch("run tasks")

        val result1 = sw.task("task1") {
            Thread.sleep(10)
            42
        }

        val result2 = sw.task("task2") {
            Thread.sleep(10)
            45
        }

        // print task1, task2 elapsed times
        println(sw.prettyPrint())

        result1 shouldBeEqualTo 42
        result2 shouldBeEqualTo 45
    }

    @Test
    fun `run tasks with coroutines`() = runSuspendTest {
        val sw = StopWatch("run tasks with coroutines")

        val result1 = sw.suspendTask("task1") {
            delay(10)
            42
        }

        val result2 = sw.suspendTask("task2") {
            delay(10)
            45
        }

        // print task1, task2 elapsed times
        println(sw.prettyPrint())

        result1 shouldBeEqualTo 42
        result2 shouldBeEqualTo 45
    }
}
