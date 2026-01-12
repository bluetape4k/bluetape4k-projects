package io.bluetape4k.io.okio

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.asByte
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class BufferKotlinTest: AbstractOkioTest() {

    companion object: KLogging()

    @Test
    fun `get from buffer`() {
        val actual = bufferOf("abc")

        actual[0] shouldBeEqualTo 'a'.asByte()
        actual[1] shouldBeEqualTo 'b'.asByte()
        actual[2] shouldBeEqualTo 'c'.asByte()

        kotlin.test.assertFailsWith<IndexOutOfBoundsException> {
            actual[-1]
        }

        kotlin.test.assertFailsWith<IndexOutOfBoundsException> {
            actual[3]
        }
    }

    @Test
    fun `copy to output stream`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream())   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with offset`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 2)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(2)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with byte count`() {
        val expectedText = Fakers.randomString(256, 256)

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), byteCount = 3)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with offset and byte count`() {
        val expectedText = Fakers.randomString(256, 256)

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 1, byteCount = 3)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(1, 4)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `write to output stream`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.writeTo(target.outputStream())   // source -> target  (move)

        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo ""
    }

    @Test
    fun `write to output stream with byteCount`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)

        val target = Buffer()
        source.writeTo(target.outputStream(), 3)   // source -> target  (move)

        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText.substring(3)
    }
}
