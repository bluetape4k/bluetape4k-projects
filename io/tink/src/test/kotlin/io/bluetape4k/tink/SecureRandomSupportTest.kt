package io.bluetape4k.tink

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import org.junit.jupiter.api.Test

class SecureRandomSupportTest {

    companion object: KLogging()

    @Test
    fun `randomBytes with positive size`() {
        val bytes = randomBytes(16)
        bytes shouldHaveSize 16
    }

    @Test
    fun `randomBytes with zero size returns empty`() {
        val bytes = randomBytes(0)
        bytes shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `randomBytes with negative size returns empty`() {
        val bytes = randomBytes(-1)
        bytes shouldBeEqualTo emptyByteArray
    }

    @Test
    fun `randomBytes generates different values each time`() {
        val bytes = List(100) { randomBytes(32) }
        bytes.distinct() shouldHaveSize bytes.size
    }
}
