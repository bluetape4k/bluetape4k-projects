package io.bluetape4k.okio.tink

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.requireZeroOrPositiveNumber
import io.bluetape4k.tink.encrypt.TinkEncryptor
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import java.io.IOException

/**
 * 데이터를 복호화하여 [Source]로 읽는 [Source] 구현체.
 *
 * [TinkEncryptor]를 사용하여 위임 [Source]의 모든 암호화 데이터를 읽은 후
 * 한 번에 복호화합니다. Tink는 스트림 복호화를 제공하지 않으므로 전체 암호문을
 * 먼저 읽은 뒤 복호화합니다.
 *
 * @see TinkEncryptSink
 */
open class TinkDecryptSource(
    delegate: Source,
    private val encryptor: TinkEncryptor,
): ForwardingSource(delegate) {

    companion object: KLogging()

    private val decryptedBuffer = Buffer()
    private var decryptedReady = false

    /**
     * 복호화된 데이터를 [sink]에 최대 [byteCount] 바이트 읽어옵니다.
     *
     * @param sink 읽은 데이터를 쓸 버퍼
     * @param byteCount 요청할 최대 바이트 수 (음수이면 예외, 0이면 0 반환)
     * @return 실제 읽은 바이트 수 또는 EOF 시 -1
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
     * 내부 복호화 버퍼와 위임 [Source]를 닫습니다.
     */
    override fun close() {
        super.close()
        decryptedBuffer.close()
    }
}

/**
 * 현재 [Source]를 [TinkEncryptor]로 복호화하는 [TinkDecryptSource]로 변환합니다.
 *
 * @param encryptor 복호화에 사용할 [TinkEncryptor]
 * @return 복호화 [TinkDecryptSource] 인스턴스
 */
fun Source.asTinkDecryptSource(encryptor: TinkEncryptor): TinkDecryptSource =
    TinkDecryptSource(this, encryptor)
