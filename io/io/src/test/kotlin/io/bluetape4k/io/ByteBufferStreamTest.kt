package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ByteBufferStreamTest: AbstractIOTest() {

    companion object: KLogging() {
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `use ByteBufferInputStream`() {
        val bytes = randomBytes
        val buffer = bytes.toByteBuffer()

        ByteBufferInputStream(bytes).use { inputStream ->
            inputStream.available() shouldBeEqualTo bytes.size
            val actual = ByteArray(bytes.size)
            inputStream.read(actual)

            actual shouldBeEqualTo buffer.getBytes()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `use ByteBufferOutputStream`() {
        val bytes = randomBytes

        ByteBufferOutputStream(bytes.size).use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()

            outputStream.toByteArray() shouldBeEqualTo bytes
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `use ByteBufferInputStream Direct`() {
        val bytes = randomBytes

        ByteBufferInputStream.direct(bytes).use { inputStream ->
            inputStream.available() shouldBeEqualTo bytes.size

            val actual = ByteArray(bytes.size)
            inputStream.read(actual)

            actual shouldBeEqualTo bytes
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `use ByteBufferOutputStream Direct`() {
        val bytes = randomBytes

        ByteBufferOutputStream.direct(bytes.size).use { outputStream ->
            outputStream.write(bytes)
            outputStream.flush()

            outputStream.toByteArray() shouldBeEqualTo bytes
        }
    }

    @Test
    fun `existing ByteBuffer factory remains growable and includes its prefix`() {
        val buffer = ByteBuffer.allocate(4).apply {
            put(1)
            put(2)
        }
        val outputStream = ByteBufferOutputStream(buffer)

        outputStream.write(byteArrayOf(3, 4, 5, 6))

        outputStream.toByteArray() shouldBeEqualTo byteArrayOf(1, 2, 3, 4, 5, 6)
    }
}
