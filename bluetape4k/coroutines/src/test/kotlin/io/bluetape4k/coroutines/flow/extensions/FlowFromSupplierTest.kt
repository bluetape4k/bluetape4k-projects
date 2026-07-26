package io.bluetape4k.coroutines.flow.extensions

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FlowFromSupplierTest {

    @Test
    fun `supplier failure propagates`() = runTest {
        val flow = flowFromSupplier { error("boom") }

        val result = runCatching { flow.toList() }

        result.isFailure.shouldBeTrue()
        result.exceptionOrNull()?.message shouldBeEqualTo "boom"
    }

    @Test
    fun `supplier value is emitted once`() = runTest {
        val flow = flowFromSupplier { 42 }

        flow.toList() shouldBeEqualTo listOf(42)
    }
}
