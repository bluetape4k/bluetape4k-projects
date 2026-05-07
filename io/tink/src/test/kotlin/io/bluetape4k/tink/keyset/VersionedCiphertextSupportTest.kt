package io.bluetape4k.tink.keyset

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class VersionedCiphertextSupportTest {

    companion object : KLogging()

    @Test
    fun `버전과 암호문을 pack 후 unpack하면 원래 값 복원`() {
        val version = 42L
        val ciphertext = "hello world".toByteArray()

        val packed = packVersionedCiphertext(version, ciphertext)
        val (unpackedVersion, unpackedCiphertext) = unpackVersionedCiphertext(packed)

        unpackedVersion shouldBeEqualTo version
        unpackedCiphertext shouldBeEqualTo ciphertext
    }

    @Test
    fun `버전 1부터 여러 버전 pack unpack 확인`() {
        listOf(1L, 2L, 100L, Long.MAX_VALUE).forEach { version ->
            val data = "version-$version".toByteArray()
            val packed = packVersionedCiphertext(version, data)
            val (v, d) = unpackVersionedCiphertext(packed)
            v shouldBeEqualTo version
            d shouldBeEqualTo data
        }
    }

    @Test
    fun `packed 크기는 Long SIZE_BYTES + ciphertext size`() {
        val ciphertext = ByteArray(32) { it.toByte() }
        val packed = packVersionedCiphertext(1L, ciphertext)
        packed.size shouldBeEqualTo Long.SIZE_BYTES + 32
    }

    @Test
    fun `packed 크기는 항상 Long SIZE_BYTES 보다 크다`() {
        val packed = packVersionedCiphertext(1L, ByteArray(1))
        (packed.size > Long.SIZE_BYTES).shouldBeTrue()
    }

    @Test
    fun `빈 payload로 unpack시 예외 발생`() {
        assertThrows<IllegalArgumentException> {
            unpackVersionedCiphertext(ByteArray(0))
        }
    }

    @Test
    fun `Long SIZE_BYTES 크기의 payload로 unpack시 예외 발생`() {
        // 버전만 있고 암호문이 없는 경우 (크기가 Long.SIZE_BYTES 이하)
        assertThrows<IllegalArgumentException> {
            unpackVersionedCiphertext(ByteArray(Long.SIZE_BYTES))
        }
    }
}
