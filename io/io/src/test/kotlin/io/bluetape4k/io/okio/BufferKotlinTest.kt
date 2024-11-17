package io.bluetape4k.io.okio

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BufferKotlinTest {

    companion object: KLogging() {
        val faker = Fakers.faker
    }

    @Test
    fun `get from buffer`() {
        val actual = Buffer().writeUtf8("abc")

        actual[0] shouldBeEqualTo 'a'.code.toByte()
        actual[1] shouldBeEqualTo 'b'.code.toByte()
        actual[2] shouldBeEqualTo 'c'.code.toByte()

        assertFailsWith<IndexOutOfBoundsException> {
            actual[-1]
        }
        assertFailsWith<IndexOutOfBoundsException> {
            actual[3]
        }
    }

    @Test
    fun `copy to output stream`() {
        val expectedText = Fakers.randomString()

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream())
        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with offset`() {
        val expectedText = Fakers.randomString()

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 2)
        target.readUtf8() shouldBeEqualTo expectedText.substring(2)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with byte count`() {
        val expectedText = Fakers.randomString(256)

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), byteCount = 3)
        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with offset and byte count`() {
        val expectedText = Fakers.randomString(256)

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 1, byteCount = 3)
        target.readUtf8() shouldBeEqualTo expectedText.substring(1, 4)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `write to output stream`() {
        val expectedText = Fakers.randomString()

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.writeTo(target.outputStream())
        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo ""
    }

    @Test
    fun `write to output stream with byteCount`() {
        val expectedText = Fakers.randomString()

        val source = Buffer()
        source.writeUtf8(expectedText)

        val target = Buffer()
        source.writeTo(target.outputStream(), byteCount = 3)
        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText.substring(3)
    }
}
