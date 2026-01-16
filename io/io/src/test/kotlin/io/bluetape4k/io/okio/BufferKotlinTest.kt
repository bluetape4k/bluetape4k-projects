package io.bluetape4k.io.okio

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.asByte
import okio.Buffer
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BufferKotlinTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Test
    fun `get from buffer`() {
        val actual = bufferOf("abc")

        actual[0] shouldBeEqualTo 'a'.asByte()
        actual[1] shouldBeEqualTo 'b'.asByte()
        actual[2] shouldBeEqualTo 'c'.asByte()

        assertFailsWith<IndexOutOfBoundsException> {
            actual[-1]
        }

        assertFailsWith<IndexOutOfBoundsException> {
            actual[3]
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `copy to output stream`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.copyTo(target.outputStream())   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `copy to output stream with offset`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 2)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(2)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `copy to output stream with byte count`() {
        val expectedText = Fakers.fixedString(256)

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.copyTo(target.outputStream(), byteCount = 3)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @Test
    fun `copy to output stream with offset and byte count`() {
        val expectedText = Fakers.randomString(256, 256)

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.copyTo(target.outputStream(), offset = 1, byteCount = 3)   // source -> target

        target.readUtf8() shouldBeEqualTo expectedText.substring(1, 4)
        source.readUtf8() shouldBeEqualTo expectedText
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `write to output stream`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.writeTo(target.outputStream())   // source -> target  (move)

        target.readUtf8() shouldBeEqualTo expectedText
        source.readUtf8() shouldBeEqualTo ""
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `write to output stream with byteCount`() {
        val expectedText = Fakers.randomString()

        val source = bufferOf(expectedText)
        log.debug { "source=$source" }

        val target = Buffer()
        source.writeTo(target.outputStream(), 3)   // source -> target  (move)

        target.readUtf8() shouldBeEqualTo expectedText.substring(0, 3)
        source.readUtf8() shouldBeEqualTo expectedText.substring(3)
    }
}
