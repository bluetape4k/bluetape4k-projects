package io.bluetape4k.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.emptyByteArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test

class CryptographySupportTest {

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
        val bytes1 = randomBytes(32)
        val bytes2 = randomBytes(32)
        bytes1 shouldNotBeEqualTo bytes2
    }

    @Test
    fun `registerBouncyCastleProvider can be called multiple times safely`() {
        registerBouncyCastleProvider()
        registerBouncyCastleProvider()
        // 예외 없이 정상 실행되면 성공
    }
}
