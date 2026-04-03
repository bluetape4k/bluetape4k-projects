package io.bluetape4k.tink.keyset

import io.bluetape4k.tink.EMPTY_BYTES
import io.bluetape4k.tink.aead.TinkAead
import java.time.Duration
import java.util.*

/**
 * versioned keyset 저장소를 사용하는 AEAD 암호화 래퍼입니다.
 *
 * 새 암호화는 active keyset으로 수행하고, 암호문 앞에 keyset version을 함께 저장해
 * 과거 version으로 암호화한 데이터도 계속 복호화할 수 있게 합니다.
 */
class VersionedTinkAead(
    private val keysetStore: VersionedKeysetStore,
) {

    fun currentVersion(): Long = keysetStore.current().version

    fun rotate(): VersionedKeysetHandle = keysetStore.rotate()

    fun rotateIfDue(rotationPeriod: Duration): VersionedKeysetHandle = keysetStore.rotateIfDue(rotationPeriod)

    fun encrypt(plaintext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray {
        val keyset = keysetStore.current()
        val ciphertext = TinkAead(keyset.keysetHandle).encrypt(plaintext, associatedData)
        return packVersionedCiphertext(keyset.version, ciphertext)
    }

    fun decrypt(payload: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray {
        val (version, ciphertext) = unpackVersionedCiphertext(payload)
        val keyset = requireNotNull(keysetStore.find(version)) { "No keyset for version=$version" }
        return TinkAead(keyset.keysetHandle).decrypt(ciphertext, associatedData)
    }

    fun encrypt(plaintext: String, associatedData: ByteArray = EMPTY_BYTES): String =
        Base64.getEncoder().encodeToString(encrypt(plaintext.toByteArray(Charsets.UTF_8), associatedData))

    fun decrypt(payload: String, associatedData: ByteArray = EMPTY_BYTES): String =
        decrypt(Base64.getDecoder().decode(payload), associatedData).toString(Charsets.UTF_8)
}
