package io.bluetape4k.tink.keyset

import java.nio.ByteBuffer

internal fun packVersionedCiphertext(version: Long, ciphertext: ByteArray): ByteArray {
    val buffer = ByteBuffer.allocate(Long.SIZE_BYTES + ciphertext.size)
    buffer.putLong(version)
    buffer.put(ciphertext)
    return buffer.array()
}

internal fun unpackVersionedCiphertext(payload: ByteArray): Pair<Long, ByteArray> {
    require(payload.size > Long.SIZE_BYTES) { "payload must contain version prefix and ciphertext" }
    val buffer = ByteBuffer.wrap(payload)
    val version = buffer.long
    val ciphertext = ByteArray(buffer.remaining())
    buffer.get(ciphertext)
    return version to ciphertext
}
