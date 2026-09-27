package io.bluetape4k.tink.keyset

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.bluetape4k.tink.EMPTY_BYTES
import io.bluetape4k.tink.daead.TinkDeterministicAead
import java.time.Duration

/**
 * versioned keyset 저장소를 사용하는 Deterministic AEAD 래퍼입니다.
 *
 * 같은 version 안에서는 결정적 암호화 특성을 유지하고, version이 바뀌면 새 키로 암호화합니다.
 */
class VersionedTinkDaead(
    private val keysetStore: VersionedKeysetStore,
) {

    companion object: KLogging()

    fun currentVersion(): Long = keysetStore.current().version

    fun rotate(): VersionedKeysetHandle = keysetStore.rotate()

    fun rotateIfDue(rotationPeriod: Duration): VersionedKeysetHandle = keysetStore.rotateIfDue(rotationPeriod)

    fun encryptDeterministically(plaintext: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray {
        val keyset = keysetStore.current()
        val ciphertext = TinkDeterministicAead(keyset.keysetHandle).encryptDeterministically(plaintext, associatedData)
        return packVersionedCiphertext(keyset.version, ciphertext)
    }

    fun decryptDeterministically(payload: ByteArray, associatedData: ByteArray = EMPTY_BYTES): ByteArray {
        val (version, ciphertext) = unpackVersionedCiphertext(payload)
        val keyset = requireNotNull(keysetStore.find(version)) { "No keyset for version=$version" }
        return TinkDeterministicAead(keyset.keysetHandle).decryptDeterministically(ciphertext, associatedData)
    }

    fun encryptDeterministically(plaintext: String, associatedData: ByteArray = EMPTY_BYTES): String =
        encryptDeterministically(plaintext.toUtf8Bytes(), associatedData).encodeBase64String()

    fun decryptDeterministically(payload: String, associatedData: ByteArray = EMPTY_BYTES): String =
        decryptDeterministically(payload.decodeBase64ByteArray(), associatedData).toUtf8String()
}
