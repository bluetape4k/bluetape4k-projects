package io.bluetape4k.io.okio.cipher

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireGe
import okio.Buffer
import okio.ForwardingSource
import javax.crypto.Cipher

/**
 * 암호화된 [okio.Source]를 읽어 복호화 합니다.
 *
 * @see CipherSink
 */
open class CipherSource(
    delegate: okio.Source,
    private val cipher: Cipher,
): ForwardingSource(delegate) {

    companion object: KLogging()

    // read 함수를 호출할 때마다 데이터를 읽고 복호화하기 위한 버퍼
    private val sourceBuffer = Buffer()
    private val decipheredBuffer = Buffer()

    override fun read(sink: Buffer, byteCount: Long): Long {
        // 복원할 전체 블록과 끝을 확인하기 위한 추가 블록에 대한 계산
        val bytesToRead =
            cipher.blockSize * (1 + (byteCount / cipher.blockSize) + if (byteCount % cipher.blockSize > 0) 1 else 0)
        bytesToRead.requireGe(0L, "bytesToRead")
        log.debug { "Read data from source with cipher. bytes to read=$bytesToRead" }

        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        var streamEnd = false
        while (sourceBuffer.size < bytesToRead && !streamEnd) {
            val bytesRead = super.read(sourceBuffer, bytesToRead - sourceBuffer.size)
            log.trace { "bytesRead=$bytesRead, sourceBuffer=$sourceBuffer" }
            if (bytesRead < 0) {
                streamEnd = true
            }
        }

        // source로 부터 읽은 데이터를 복호화
        val bytes = sourceBuffer.readByteArray()
        if (bytes.isNotEmpty()) {
            val decrypted = cipher.update(bytes)
            decipheredBuffer.write(decrypted)
        }
        if (streamEnd) {
            // 끝에 도달하면 (패딩과 함께) 완료
            log.debug { "Finalize with padding if we are at the end." }
            cipher.doFinal()?.let {
                decipheredBuffer.write(it)
            }
        }

        // 요청한 바이트 수(또는 가능한 모든 바이트) 만큼 sink에 쓰기
        val bytesToReturn = byteCount.coerceAtMost(decipheredBuffer.size)
        sink.write(decipheredBuffer, bytesToReturn)

        // 복호화해서 쓴 바이트 수 반환, 더 이상 복호화할 것이 없으면 -1 반환
        return if (bytesToReturn > 0) bytesToReturn else -1L
    }

    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead == -1L) break
            totalBytesRead += bytesRead
        }
        return totalBytesRead
    }

    override fun close() {
        super.close()
        sourceBuffer.close()
        decipheredBuffer.close()
    }
}
