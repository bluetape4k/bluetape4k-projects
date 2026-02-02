package io.bluetape4k.concurrent

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class WorkStealingPoolExamples {

    companion object: KLogging()

    @Test
    fun `invlid parallelism value`() {
        assertFailsWith<AssertionError> {
            withWorkStealingPool(0) { 42L }.join()
        }
    }

    @Test
    fun `execute action with work stealing pool`() {
        val task = withWorkStealingPool(4) {
            Thread.sleep(100)
            log.debug { "Task executed in work stealing pool 111..." }

            Thread.sleep(100)
            log.debug { "Task executed in work stealing pool 222 ..." }
        }
        task.join()
        log.debug { "Finished." }
    }

    @Test
    fun `executeAll multiple actions with work stealing pool`() {
        val tasks = fastList(20) { index ->
            {
                Thread.sleep(Random.nextLong(100, 200))
                log.trace { "Current ${Thread.currentThread().name} thread is executing >>> $index" }
                "callable type thread task: $index"
            }
        }

        val result = withWorkStealingPool(4, tasks).join()
        result.size shouldBeEqualTo 20
    }
}
