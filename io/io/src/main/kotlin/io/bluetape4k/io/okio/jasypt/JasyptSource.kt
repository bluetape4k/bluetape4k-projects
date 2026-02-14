package io.bluetape4k.io.okio.jasypt

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import java.io.IOException

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

    private val decryptedBuffer = Buffer()
    private var decryptedReady = false

    /**
     * Okio I/O에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        ensureDecrypted()
        if (decryptedBuffer.size == 0L) {
            return -1L
        }

        val bytesToReturn = byteCount.coerceAtMost(decryptedBuffer.size)
        sink.write(decryptedBuffer, bytesToReturn)
        return bytesToReturn
    }

    private fun ensureDecrypted() {
        if (decryptedReady) {
            return
        }
        decryptedReady = true

        val encryptedBuffer = Buffer()
        var noProgressCount = 0
        while (true) {
            val bytesRead = super.read(encryptedBuffer, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead < 0L) {
                break
            }
            if (bytesRead == 0L) {
                noProgressCount++
                if (noProgressCount >= 8) {
                    throw IOException("Unable to read encrypted bytes from source: no progress.")
                }
                continue
            }
            noProgressCount = 0
        }

        if (encryptedBuffer.size == 0L) {
            return
        }

        val decryptedBytes = encryptor.decrypt(encryptedBuffer.readByteArray())
        decryptedBuffer.write(decryptedBytes)
    }

    /**
     * Okio I/O 리소스를 정리하고 닫습니다.
     */
    override fun close() {
        super.close()
        decryptedBuffer.close()
    }
}

/**
 * Okio I/O 타입 변환을 위한 `asDecryptSource` 함수를 제공합니다.
 */
fun Source.asDecryptSource(encryptor: Encryptor): DecryptSource =
    DecryptSource(this, encryptor)
