package io.bluetape4k.codec

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.random.Random

@RandomizedTest
abstract class AbstractStringEncoderTest {


    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    protected abstract val encoder: StringEncoder

    @Test
    fun `encode null or empty`() {
        encoder.encode(null).shouldBeEmpty()
        encoder.encode(ByteArray(0)).shouldBeEmpty()
    }

    @Test
    fun `decode null or empty`() {
        encoder.decode(null).shouldBeEmpty()
        encoder.decode("").shouldBeEmpty()
        encoder.decode(" \t ").shouldBeEmpty()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode decode string`(@RandomValue expected: String) {

        val encoded = encoder.encode(expected.toUtf8Bytes())
        val decoded = encoder.decode(encoded)

        decoded.toUtf8String() shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode random bytes`(@RandomValue bytes: ByteArray) {

        val encoded = encoder.encode(bytes)
        val decoded = encoder.decode(encoded)

        decoded shouldBeEqualTo bytes
    }

    @Test
    fun `encode decode in multi-thread`() {
        val bytes = Random.nextBytes(4096)

        MultithreadingTester()
            .numThreads(16)
            .roundsPerThread(4)
            .add {
                val converted = encoder.decode(encoder.encode(bytes))
                converted shouldBeEqualTo bytes
            }
            .run()
    }
}
