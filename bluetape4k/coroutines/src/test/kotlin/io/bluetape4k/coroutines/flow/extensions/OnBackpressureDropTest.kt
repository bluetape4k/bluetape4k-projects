package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OnBackpressureDropTest: AbstractFlowTest() {

    companion object: KLogging()

    @Test
    fun `drop backpressure items`() = runTest {
        flowRangeOf(0, 10)
            .onEach { delay(100L) }.log("source", log)
            .onBackpressureDrop()
            // .buffer(2) // buffering 하면 drop을 하지 않습니다.
            .onEach { delay(130L) }.log("backpressure", log)
            .assertResult(0, 2, 4, 6, 8)
    }
}
