package io.bluetape4k.io.okio.encrypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireGe
import okio.Buffer
import okio.ForwardingSource
import okio.Source

/**
 * 데이터를 복호화하여 [Source]로 읽는 [Source] 구현체.
 *
 * @see EncryptSink
 */
open class DecryptSource(
    delegate: Source,
    private val encryptor: Encryptor,
): ForwardingSource(delegate) {

    companion object: KLogging()

    private val sourceBuffer = Buffer()
    private val decryptedBuffer = Buffer()

    override fun read(sink: Buffer, byteCount: Long): Long {
        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        byteCount.requireGe(0, "byteCount")

        var streamEnd = false
        while (sourceBuffer.size < byteCount && !streamEnd) {
            val bytesRead = super.read(sourceBuffer, byteCount - sourceBuffer.size)
            log.debug { "byteCount=$byteCount, sourceBuffer.size=${sourceBuffer.size}" }
            if (bytesRead < 0) {
                streamEnd = true
            }
        }

        // PBE 암호화는 한 번에 모든 데이터를 읽어야 함
        if (!streamEnd) {
            throw UnsupportedOperationException("support only reading all bytes")
        }

        // source로 부터 읽은 데이터를 복호화
        val bytes = sourceBuffer.readByteArray()
        log.debug { "source buffer bytes: ${bytes.size}" }
        if (bytes.isNotEmpty()) {
            val decrypted = encryptor.decrypt(bytes)
            log.debug { "decrypted bytes: ${decrypted.size}" }
            decryptedBuffer.write(decrypted)
        }

        // 요청한 바이트 수(또는 가능한 모든 바이트) 만큼 sink에 쓰기
        val bytesToReturn = byteCount.coerceAtMost(decryptedBuffer.size)
        sink.write(decryptedBuffer, bytesToReturn)

        return if (bytesToReturn > 0) bytesToReturn else -1
    }
}
