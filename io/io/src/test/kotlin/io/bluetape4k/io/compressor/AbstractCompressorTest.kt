package io.bluetape4k.io.compressor

import io.bluetape4k.LibraryName
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.emptyByteArray
import io.bluetape4k.support.toUtf8Bytes
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.xerial.snappy.Snappy

@RandomizedTest
abstract class AbstractCompressorTest {

    companion object: KLogging() {
        protected const val REPEAT_SIZE = 5

        @JvmStatic
        protected val faker = Fakers.faker

        init {
            // Snappy 는 이렇게 한 번 초기화 해주어야 제대로 성능을 알 수 있다.
            Snappy.cleanUp()
            val compressed = Snappy.compress(LibraryName)
            Snappy.uncompress(compressed)
        }

        fun getRandomString(min: Int = 4096, max: Int = 8192, replica: Int = 4): String {
            return Fakers.randomString(min, max).repeat(replica)
        }
    }

    abstract val compressor: Compressor

    @Test
    fun `compress null or empty`() {
        compressor.compress(null).shouldBeEmpty()
        compressor.compress(emptyByteArray).shouldBeEmpty()
    }

    @Test
    fun `decompress null or empty`() {
        compressor.decompress(null).shouldBeEmpty()
        compressor.decompress(emptyByteArray).shouldBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compress String`() {
        val expected = getRandomString()

        val compressed: String = compressor.compress(expected)
        val decompressed: String = compressor.decompress(compressed)

        printCompressResult(compressed.length, expected.length)
        decompressed shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `compress ByteArray`() {
        val expected = getRandomString().toUtf8Bytes()

        val compressed: ByteArray = compressor.compress(expected)
        val decompressed: ByteArray = compressor.decompress(compressed)

        printCompressResult(compressed.size, expected.size)
        decompressed shouldBeEqualTo expected
    }

    private fun printCompressResult(compressedSize: Int, plainSize: Int) {
        log.debug {
            "${compressor.javaClass.simpleName} ratio=${compressedSize * 100.0 / plainSize} " +
                    "compressedSize=$compressedSize, plainSize=$plainSize"
        }
    }
}
