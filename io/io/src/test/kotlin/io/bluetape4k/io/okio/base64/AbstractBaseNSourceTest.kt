package io.bluetape4k.io.okio.base64

import io.bluetape4k.io.okio.AbstractOkioTest
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import okio.Buffer
import okio.Source
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

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
    fun `read request too much`() {
        val content = Fakers.fixedString(32)
        val source = getDecodedSource(content)

        val output = Buffer()

        assertFailsWith<IllegalArgumentException> {
            source.read(output, Long.MAX_VALUE)
        }
    }

    private fun getDecodedSource(plainText: String): Source {
        val base64String = getEncodedString(plainText)
        return getSource(bufferOf(base64String))
    }
}
