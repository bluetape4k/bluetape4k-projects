package io.bluetape4k.coroutines.flow.extensions

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
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
