package io.bluetape4k.io.serializer

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.assertFailsWith

class BinarySerializerSupportTest {

    private val serializer: BinarySerializer = JdkBinarySerializer()

    @Test
    fun `serializeAsByteBuffer와 deserialize ByteBuffer`() {
        val buffer: ByteBuffer = serializer.serializeAsByteBuffer("hello-buffer")
        serializer.deserialize<String>(buffer) shouldBeEqualTo "hello-buffer"
    }

    @Test
    fun `serializeAsOkioBuffer와 deserialize Buffer`() {
        val okioBuffer = serializer.serializeAsOkioBuffer(1234L)
        serializer.deserialize<Long>(okioBuffer) shouldBeEqualTo 1234L
    }

    @Test
    fun `deserialize ByteBuffer 는 현재 position 이후 남은 바이트만 사용한다`() {
        val plain = "hello-buffer"
        val bytes = serializer.serialize(plain)
        val buffer = ByteBuffer.allocate(bytes.size + 4)

        buffer.putInt(0xCAFE_BABE.toInt())
        buffer.put(bytes)
        buffer.flip()
        buffer.position(4)

        serializer.deserialize<String>(buffer) shouldBeEqualTo plain
    }

    @Test
    fun `deserialize ByteBuffer 는 남은 바이트가 손상되면 BinarySerializationException 을 던진다`() {
        val plain = "hello-buffer"
        val bytes = serializer.serialize(plain)
        val buffer = ByteBuffer.wrap(bytes.copyOfRange(1, bytes.size))

        assertFailsWith<BinarySerializationException> {
            serializer.deserialize<String>(buffer)
        }
    }
}
