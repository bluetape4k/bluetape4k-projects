package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class IntervalTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `interval operator with time millis`() = runTest {
        flowRangeOf(0, 10).log("source")
            .interval(100, 100).log("interval")
            .assertResult(flowRangeOf(0, 10))
    }

    @Test
    fun `interval with default time millis`() = runTest {
        flowRangeOf(0, 10)
            .interval()
            .assertResult(flowRangeOf(0, 10))
    }

    @Test
    fun `interval operator with duration`() = runTest {
        flowRangeOf(0, 10)
            .interval(200.milliseconds, 100.milliseconds)
            .assertResult(flowRangeOf(0, 10))
    }

    @Test
    fun `interval with initial duration`() = runTest {
        flowRangeOf(0, 10)
            .interval(Duration.ZERO)
            .assertResult(flowRangeOf(0, 10))
    }

    @Test
    fun `create interval by millis`() = runTest {
        intervalFlowOf(200, 100)
            .take(20)
            .assertResult(flowRangeOf(0L, 20))
    }

    @Test
    fun `create interval by duration`() = runTest {
        intervalFlowOf(200.milliseconds, 100.milliseconds)
            .take(20)
            .assertResult(flowRangeOf(0L, 20))
    }
}
