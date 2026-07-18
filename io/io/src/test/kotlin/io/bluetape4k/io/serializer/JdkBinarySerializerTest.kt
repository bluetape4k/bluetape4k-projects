package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.io.ObjectInputFilter
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException

class JdkBinarySerializerTest: AbstractBinarySerializerTest() {

    companion object: KLogging()

    override val serializer: BinarySerializer = JdkBinarySerializer()

    @Test
    fun `bufferSize 가 1이어도 직렬화 역직렬화가 가능하다`() {
        val serializer = JdkBinarySerializer(bufferSize = 1)
        val expected = "small-buffer-jdk"

        val bytes = serializer.serialize(expected)
        val actual = serializer.deserialize<String>(bytes)

        actual shouldBeEqualTo expected
    }

    @Test
    fun `bufferSize 는 0 이하를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            JdkBinarySerializer(0)
        }
        assertFailsWith<IllegalArgumentException> {
            JdkBinarySerializer(-1)
        }
    }

    @Test
    fun `buffer deserialization preserves nested filter cancellation identity`() {
        val cancellation = CancellationException("filter cancelled")
        var filterInvoked = false
        val filter = ObjectInputFilter {
            filterInvoked = true
            throw cancellation
        }
        val serializer = JdkBinarySerializer(objectInputFilter = filter)
        val wire = serializer.serialize(arrayListOf("filter-probe"))

        val actual = assertFailsWith<CancellationException> {
            serializer.deserializeFrom<ArrayList<String>>(ByteBuffer.wrap(wire).asReadOnlyBuffer())
        }

        filterInvoked.shouldBeTrue()
        actual shouldBeSameInstanceAs cancellation
    }

    @Test
    fun `buffer serialization preserves cancellation identity`() {
        val cancellation = CancellationException("write cancelled")

        val actual = assertFailsWith<CancellationException> {
            serializer.serializeTo(CancellationOnWritePayload(cancellation), ByteBuffer.allocate(1024))
        }

        actual shouldBeSameInstanceAs cancellation
    }
}

private class CancellationOnWritePayload(
    private val cancellation: CancellationException,
): Serializable {

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun writeObject(output: ObjectOutputStream) {
        throw cancellation
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
