package io.bluetape4k.codec

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertFailsWith

@RandomizedTest
class Url62Test {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `encode uuid and decode url62 text`(@RandomValue(type = UUID::class, size = 20) uuids: List<UUID>) {
        uuids.forEach { uuid ->
            val encoded = Url62.encode(uuid)
            log.debug { "uuid=$uuid, encoded=$encoded" }
            Url62.decode(encoded) shouldBeEqualTo uuid
        }
    }

    @Test
    fun `fail when illegal character`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("Foo Bar")
        }
    }

    @Test
    fun `fail when blank string`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("")
        }

        assertFailsWith<IllegalArgumentException> {
            Url62.decode(" \t ")
        }
    }

    @Test
    fun `fail when text contains more than 128 bit information`() {
        assertFailsWith<IllegalArgumentException> {
            Url62.decode("7NLCAyd6sKR7kDHxgAWFPas")
        }
    }

    @Test
    fun `encode decode in multi-threading`() {
        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(4)
            .add {
                val url = UUID.randomUUID()
                val converted = Url62.decode(Url62.encode(url))
                converted shouldBeEqualTo url
            }
            .run()
    }
}
