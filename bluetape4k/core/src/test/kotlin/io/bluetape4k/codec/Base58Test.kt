package io.bluetape4k.codec

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.utils.Runtimex
import net.datafaker.Faker
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*

@RandomizedTest
class Base58Test {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private val faker = Fakers.faker
        private val fakerKr = Faker(Locale.KOREAN)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `populate Base58 random string`() {
        val size = 100
        val strs = List(size) { Base58.randomString(12) }
        strs.distinct().size shouldBeEqualTo size
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode decode with characters`() {
        val expected = faker.lorem().characters()

        val encoded = Base58.encode(expected.toUtf8Bytes())
        val decoded = Base58.decode(encoded).toUtf8String()

        log.debug { "encoded=$encoded, decoded=$decoded" }
        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode decode with paragraph`() {
        val expected = faker.lorem().paragraph()

        val encoded = Base58.encode(expected.toUtf8Bytes())
        val decoded = Base58.decode(encoded).toUtf8String()

        log.debug { "encoded=$encoded, decoded=$decoded" }
        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode decode with paragraph in korean`() {
        val expected = fakerKr.lorem().paragraph()

        val encoded = Base58.encode(expected.toUtf8Bytes())
        val decoded = Base58.decode(encoded).toUtf8String()

        log.debug { "encoded=$encoded, decoded=$decoded" }
        decoded shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode UUID`() {
        val expects = List(10) { UUID.randomUUID() }
        expects.forEach { uuid ->
            val encoded = Base58.encode(uuid.toString().toUtf8Bytes())
            val decoded = Base58.decode(encoded).toUtf8String()
            log.debug { "encoded=$encoded, decoded=$decoded" }
            UUID.fromString(decoded) shouldBeEqualTo uuid
        }
    }

    @Test
    fun `encode and decode in multi-thread`() {
        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(4)
            .add {
                val expected = faker.lorem().characters()
                val encoded = Base58.encode(expected.toUtf8Bytes())
                val decoded = Base58.decode(encoded).toUtf8String()
                decoded shouldBeEqualTo expected
            }
            .run()
    }
}
