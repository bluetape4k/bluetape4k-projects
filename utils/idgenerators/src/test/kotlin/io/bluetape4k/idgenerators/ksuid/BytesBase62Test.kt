package io.bluetape4k.idgenerators.ksuid

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class BytesBase62Test {

    companion object: KLogging()

    @Test
    fun `encode decode should preserve byte length`() {
        val sizes = listOf(1, 2, 3, 4, 5, 7, 8, 9, 16, 20, 21, 32)
        sizes.forEach { size ->
            val bytes = Random.nextBytes(size)
            val decoded = BytesBase62.decode(BytesBase62.encode(bytes), expectedBytes = size)
            decoded.size shouldBeEqualTo size
        }
    }

    @Test
    fun `decode trims or pads to expected length`() {
        val data = Random.nextBytes(10)
        val encoded = BytesBase62.encode(data)

        // smaller expectedBytes should truncate
        val trimmed = BytesBase62.decode(encoded, expectedBytes = 5)
        trimmed.size shouldBeEqualTo 5

        // larger expectedBytes should pad with zeros
        val padded = BytesBase62.decode(encoded, expectedBytes = 12)
        padded.size shouldBeEqualTo 12
        padded.copyOfRange(0, data.size) shouldBeEqualTo data
    }
}
