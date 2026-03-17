package io.bluetape4k.coroutines.flow.exceptions

import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class FlowExceptionsTest {
    @Test
    fun `FlowNoElementException은 FlowOperationException을 상속한다`() {
        val exception = FlowNoElementException("empty")

        exception shouldBeInstanceOf FlowOperationException::class
        exception.message shouldBeEqualTo "empty"
    }

    @Test
    fun `FlowNoElementException 기본 생성자는 message가 null이다`() {
        val exception = FlowNoElementException()
        exception.message.shouldBeNull()
    }

    @Test
    fun `FlowNoElementException은 cause와 함께 생성할 수 있다`() {
        val cause = RuntimeException("root cause")
        val exception = FlowNoElementException("no element", cause)

        exception.message shouldBeEqualTo "no element"
        exception.cause shouldBeEqualTo cause
    }

    @Test
    fun `FlowNoElementException은 cause만으로 생성할 수 있다`() {
        val cause = RuntimeException("root cause")
        val exception = FlowNoElementException(cause)

        exception.cause shouldBeEqualTo cause
    }

    @Test
    fun `FlowNoElementException은 직렬화 가능하다`() {
        val original = FlowNoElementException("serializable")

        val bytes =
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
                baos.toByteArray()
            }

        val deserialized =
            ByteArrayInputStream(bytes).use { bais ->
                ObjectInputStream(bais).use { ois -> ois.readObject() }
            }

        (deserialized is FlowNoElementException).shouldBeTrue()
        @Suppress("USELESS_CAST")
        (deserialized as FlowNoElementException).message shouldBeEqualTo "serializable"
    }

    @Test
    fun `FlowOperationException 기본 생성자는 message가 null이다`() {
        val exception = FlowOperationException()
        exception.message.shouldBeNull()
    }

    @Test
    fun `FlowOperationException은 cause와 함께 생성할 수 있다`() {
        val cause = RuntimeException("root cause")
        val exception = FlowOperationException("flow failed", cause)

        exception.message shouldBeEqualTo "flow failed"
        exception.cause shouldBeEqualTo cause
    }

    @Test
    fun `FlowOperationException은 직렬화 가능하다`() {
        val original = FlowOperationException("serializable op")

        val bytes =
            ByteArrayOutputStream().use { baos ->
                ObjectOutputStream(baos).use { oos -> oos.writeObject(original) }
                baos.toByteArray()
            }

        val deserialized =
            ByteArrayInputStream(bytes).use { bais ->
                ObjectInputStream(bais).use { ois -> ois.readObject() }
            }

        (deserialized is FlowOperationException).shouldBeTrue()
        @Suppress("USELESS_CAST")
        (deserialized as FlowOperationException).message shouldBeEqualTo "serializable op"
    }

    @Test
    fun `StopFlowException은 CancellationException을 상속한다`() {
        val exception = StopFlowException("stop")

        exception shouldBeInstanceOf CancellationException::class
        exception.message shouldBeEqualTo "stop"
    }

    @Test
    fun `StopFlowException 기본 생성자는 message가 null이다`() {
        val exception = StopFlowException()
        exception.message.shouldBeNull()
    }

    @Test
    fun `StopFlowException은 cause와 함께 생성할 수 있다`() {
        val cause = RuntimeException("root cause")
        val exception = StopFlowException("stop with cause", cause)

        exception.message shouldBeEqualTo "stop with cause"
        exception.cause shouldBeEqualTo cause
    }

    @Test
    fun `STOP 싱글턴은 StopFlowException 인스턴스이다`() {
        STOP.shouldNotBeNull()
        STOP shouldBeInstanceOf StopFlowException::class
    }

    @Test
    fun `StopException은 CancellationException을 상속한다`() {
        val collector = kotlinx.coroutines.flow.FlowCollector<Int> { }
        val exception = StopException(collector)

        exception shouldBeInstanceOf CancellationException::class
        exception.owner shouldBeEqualTo collector
    }

    @Test
    fun `StopException checkOwnership은 다른 owner이면 예외를 던진다`() {
        val owner1 = kotlinx.coroutines.flow.FlowCollector<Int> { }
        val owner2 = kotlinx.coroutines.flow.FlowCollector<Int> { }
        val exception = StopException(owner1)

        kotlin.test.assertFailsWith<StopException> {
            exception.checkOwnership(owner2)
        }
    }

    @Test
    fun `StopException checkOwnership은 같은 owner이면 예외를 던지지 않는다`() {
        val owner = kotlinx.coroutines.flow.FlowCollector<Int> { }
        val exception = StopException(owner)

        // 같은 owner이면 예외가 발생하지 않음
        exception.checkOwnership(owner)
    }
}
