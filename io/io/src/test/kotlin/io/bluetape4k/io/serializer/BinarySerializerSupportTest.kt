package io.bluetape4k.io.serializer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

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
        buffer.position() shouldBeEqualTo 4
    }

    @Test
    fun `deserialize ByteBuffer 확장 함수는 deserializeFrom 기본 메서드로 위임한다`() {
        var delegated = false
        val overriding =
            object: BinarySerializer {
                override fun serialize(graph: Any?): ByteArray = ByteArray(0)

                override fun <T: Any> deserialize(bytes: ByteArray?): T? = error("ByteArray path must not be called")

                @Suppress("UNCHECKED_CAST")
                override fun <T: Any> deserializeFrom(source: ByteBuffer): T? {
                    delegated = true
                    return "from-buffer" as T
                }
            }

        overriding.deserialize<String>(ByteBuffer.allocate(0)) shouldBeEqualTo "from-buffer"
        delegated shouldBeEqualTo true
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
