package io.bluetape4k.io.serializer

import org.amshove.kluent.shouldBeEqualTo
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
}
