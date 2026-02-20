package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class FlowExceptionsTest {

    @Test
    fun `FlowNoElementException은 FlowOperationException을 상속한다`() {
        val exception = FlowNoElementException("empty")

        exception shouldBeInstanceOf FlowOperationException::class
        exception.message shouldBeEqualTo "empty"
    }

    @Test
    fun `StopFlowException은 CancellationException을 상속한다`() {
        val exception = StopFlowException("stop")

        exception shouldBeInstanceOf CancellationException::class
        exception.message shouldBeEqualTo "stop"
    }
}
