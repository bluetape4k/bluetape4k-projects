package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest

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
}
