package io.bluetape4k.tink

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tink.aead.TinkAead
import io.bluetape4k.tink.daead.TinkDeterministicAead
import io.bluetape4k.tink.mac.TinkMac
import org.junit.jupiter.api.Test
import java.security.GeneralSecurityException

class TinkSupportTest {

    companion object: KLogging()

    @Test
    fun `registerTink can be called repeatedly without error`() {
        repeat(3) {
            registerTink()
        }

        true.shouldBeTrue()
    }

    @Test
    fun `aeadKeysetHandle generates independent keys`() {
        val aead1 = TinkAead(aeadKeysetHandle())
        val aead2 = TinkAead(aeadKeysetHandle())
        val ciphertext = aead1.encrypt("hello")

        assertFailsWith<GeneralSecurityException> {
            aead2.decrypt(ciphertext)
        }
    }

    @Test
    fun `daeadKeysetHandle generates independent keys`() {
        val daead1 = TinkDeterministicAead(daeadKeysetHandle())
        val daead2 = TinkDeterministicAead(daeadKeysetHandle())
        val ciphertext = daead1.encryptDeterministically("hello")

        assertFailsWith<GeneralSecurityException> {
            daead2.decryptDeterministically(ciphertext)
        }
    }

    @Test
    fun `macKeysetHandle generates independent keys`() {
        val mac1 = TinkMac(macKeysetHandle())
        val mac2 = TinkMac(macKeysetHandle())
        val tag = mac1.computeMac("hello")

        mac1.verifyMac(tag, "hello").shouldBeTrue()
        mac2.verifyMac(tag, "hello").shouldBeFalse()
    }
}
