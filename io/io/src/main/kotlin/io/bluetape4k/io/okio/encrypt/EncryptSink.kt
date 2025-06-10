package io.bluetape4k.io.okio.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireGe
import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 암호화하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see DecryptSource
 */
open class EncryptSink(
    delegate: Sink,
    private val encryptor: Encryptor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        // Encryptor는 한 번에 모든 데이터를 암호화해야 함
        byteCount.requireGe(source.size, "byteCount")

        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        val bytesToRead = byteCount.coerceAtMost(source.size)
        val plainBytes = source.readByteArray(bytesToRead)
        log.trace { "Encrypting: ${plainBytes.size} bytes" }

        // 암호화
        val encrypted = encryptor.encrypt(plainBytes)
        val encryptedSink = bufferOf(encrypted)
        super.write(encryptedSink, encryptedSink.size)
    }
}
