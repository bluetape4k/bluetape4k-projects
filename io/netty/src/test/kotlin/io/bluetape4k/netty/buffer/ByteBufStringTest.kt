package io.bluetape4k.netty.buffer

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.netty.AbstractNettyTest
import io.bluetape4k.netty.util.use
import io.netty.buffer.ByteBufAllocator
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.charset.Charset

class ByteBufStringTest: AbstractNettyTest() {

    companion object: KLogging() {
        private const val LIST_SIZE = 20
        private const val STRING_SIZE = 1024
    }

    private val testData: List<String> = fastList(LIST_SIZE) {
        Fakers.randomString(STRING_SIZE, STRING_SIZE * 2)
    }

    private fun getCharsets(): List<Charset> = listOf(
        Charsets.UTF_8,
        Charsets.ISO_8859_1,
        Charset.forName("windows-1252"),
        Charset.forName("CESU-8"),
    )

    @ParameterizedTest(name = "read/write string. charset={0}")
    @MethodSource("getCharsets")
    fun `read write string`(charset: Charset) {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            testData.forEach { expected -> buf.writeString(expected, charset) }
            testData.forEach { expected ->
                val actual = buf.readString(charset)
                actual shouldBeEqualTo expected
            }
        }
    }

    @ParameterizedTest(name = "read/write versioned string. charset={0}")
    @MethodSource("getCharsets")
    fun `read write versioned string`(charset: Charset) {
        ByteBufAllocator.DEFAULT.buffer().use { buf ->
            testData.forEach { expected -> buf.writeVersionedString(expected, charset) }
            testData.forEach { expected ->
                val actual = buf.readVersionedString(charset)
                actual shouldBeEqualTo expected
            }
        }
    }
}
