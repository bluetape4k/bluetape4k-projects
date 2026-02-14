package io.bluetape4k.io.okio.base64

import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Source
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.test.assertFailsWith

abstract class AbstractBaseNSourceTest: AbstractOkioTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    protected abstract fun getSource(delegate: Source): Source
    protected abstract fun getEncodedString(plainString: String): String

    @RepeatedTest(REPEAT_SIZE)
    fun `read from fixed string`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)

        val output = bufferOf(source)
        output.readUtf8() shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read from random long string`() {
        val content = faker.lorem().paragraph()
        val source = getDecodedSource(content)

        val output = bufferOf(source)
        output.readUtf8() shouldBeEqualTo content
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read partial read`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)

        val output = Buffer()
        source.read(output, 5)

        output.readUtf8() shouldBeEqualTo content.substring(0, 5)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `read stops on source end`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)

        val output = Buffer()
        while (source.read(output, 1) > 0) {
            // do nothing
        }

        output.readUtf8() shouldBeEqualTo content

        val readMore = source.read(output, 1)
        readMore shouldBeEqualTo -1
    }

    @Test
    fun `read with Long_MAX_VALUE`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)

        val output = Buffer()
        val read = source.read(output, Long.MAX_VALUE)

        read shouldBeEqualTo content.length.toLong()
        output.readUtf8() shouldBeEqualTo content
    }

    @Test
    fun `read at stream end returns remaining bytes first then -1`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)
        val output = Buffer()

        val firstRead = source.read(output, 1024L)
        firstRead shouldBeEqualTo content.length.toLong()
        output.readUtf8() shouldBeEqualTo content

        val secondRead = source.read(output, 1L)
        secondRead shouldBeEqualTo -1L
    }

    @Test
    fun `binary bytes round-trip without utf8 conversion loss`() {
        val original = byteArrayOf(
            0x00,
            0x7F,
            0x80.toByte(),
            0xFF.toByte(),
            0x10,
            0x20
        )
        val encoded = original.encodeBase64String()

        val source = getSource(bufferOf(encoded))
        val output = Buffer()
        source.read(output, 1024L)

        output.readByteArray() shouldBeEqualTo original
    }

    @Test
    fun `read throws when delegate repeatedly makes no progress`() {
        val noProgressDelegate = object: Source {
            override fun read(sink: Buffer, byteCount: Long): Long = 0L
            override fun timeout() = okio.Timeout.NONE
            override fun close() {}
        }
        val source = getSource(noProgressDelegate)
        val output = Buffer()

        assertFailsWith<IOException> {
            source.read(output, 8L)
        }
    }

    private fun getDecodedSource(plainText: String): Source {
        val base64String = getEncodedString(plainText)
        return getSource(bufferOf(base64String))
    }
}
