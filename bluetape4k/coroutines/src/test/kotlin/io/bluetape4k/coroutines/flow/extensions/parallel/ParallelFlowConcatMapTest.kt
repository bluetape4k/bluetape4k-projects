package io.bluetape4k.coroutines.flow.extensions.parallel

import io.bluetape4k.coroutines.flow.extensions.flowRangeOf
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.tests.assertError
import io.bluetape4k.coroutines.tests.assertFailure
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.coroutines.tests.assertResultSet
import io.bluetape4k.coroutines.tests.withParallels
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class ParallelFlowConcatMapTest {

    companion object: KLoggingChannel()

    @Test
    fun `concatMap - parallelism is 1`() = runSuspendTest {
        withParallels(1) { execs ->
            execs shouldHaveSize 1

            flowRangeOf(1, 5).log("source")
                .parallel(execs.size) { execs[it] }
                .concatMap {
                    log.trace { "item=$it" }
                    flowOf(it + 1)
                }
                .sequential().log("sequential")
                .assertResult(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun `concatMap - parallelism is 2`() = runSuspendTest {
        withParallels(2) { execs ->
            execs shouldHaveSize 2

            flowRangeOf(1, 5).log("source")
                .parallel(execs.size) { execs[it] }
                .concatMap {
                    log.trace { "item=$it" }
                    flowOf(it + 1)
                }
                .sequential().log("sequential")
                .assertResultSet(2, 3, 4, 5, 6)
        }
    }

    @Test
    fun `concatMap with Error - parallelism is 1`() = runSuspendTest {
        withParallels(1) { execs ->
            execs shouldHaveSize 1

            flowOf(1, 0).log("source")
                .parallel(execs.size) { execs[it] }
                .concatMap {
                    log.trace { "item=$it" }
                    flowOf(it).map { v -> 1 / v }
                }
                .sequential().log("sequential")
                .assertFailure<Int, ArithmeticException>(1)
        }
    }

    @Test
    fun `concatMap with Error - parallelism is 2`() = runSuspendTest {
        withParallels(2) { execs ->
            execs shouldHaveSize 2

            flowOf(1, 2, 0, 3, 4, 0).log("source")
                .parallel(execs.size) { execs[it] }
                .concatMap {
                    log.trace { "item=$it" }
                    flowOf(it).map { v -> 1 / v }
                }
                .sequential().log("sequential")
                .assertError<ArithmeticException>()
        }
    }
}
