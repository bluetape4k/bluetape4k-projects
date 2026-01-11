package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
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

    override fun read(sink: Buffer, byteCount: Long): Long {
        // Jasypt 는 Cipher랑 달리 한번에 모두 읽어야 한다.
        val sourceBuffer = Buffer()
        super.read(sourceBuffer, Long.MAX_VALUE)

        val encryptedBytes = sourceBuffer.readByteArray()
        if (encryptedBytes.isEmpty()) {
            return -1 // End of stream
        }

        val decryptedBytes = encryptor.decrypt(encryptedBytes)
        sink.write(bufferOf(decryptedBytes), decryptedBytes.size.toLong())

        log.debug { "복호화: 암호 데이터 크기: ${encryptedBytes.size} bytes, 복화화 데이터 크기: ${decryptedBytes.size} bytes." }

        return decryptedBytes.size.toLong()
    }
}

fun Source.asDecryptSource(encryptor: Encryptor): DecryptSource =
    DecryptSource(this, encryptor)
