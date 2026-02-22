package io.bluetape4k.io.okio.cipher

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import javax.crypto.Cipher

/**
 * 암호화된 [Source]를 스트리밍으로 읽어 복호화하는 [Source] 구현입니다.
 */
open class StreamingCipherSource(
    delegate: Source,
    private val cipher: Cipher,
): ForwardingSource(delegate) {

    companion object: KLogging()

    // read 함수를 호출할 때마다 데이터를 읽고 복호화하기 위한 버퍼
    private val sourceBuffer = Buffer()
    private val decipheredBuffer = Buffer()

    /**
     * [byteCount] 만큼 복호화 데이터를 읽어 [sink]로 전달합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        // 복원할 전체 블록과 끝을 확인하기 위한 추가 블록에 대한 계산
        val blockSize = cipher.blockSize.toLong().coerceAtLeast(1L)
        val maxBlocksByBytes = Long.MAX_VALUE / blockSize
        val requestedBlocks = byteCount / blockSize + if (byteCount % blockSize != 0L) 1L else 0L
        val targetBlocks = if (requestedBlocks >= maxBlocksByBytes) {
            maxBlocksByBytes
        } else {
            requestedBlocks + 1L
        }
        val bytesToRead = targetBlocks * blockSize

        log.debug { "암호화된 데이터를 읽어서 sink 에 씁니다. bytes to read=$bytesToRead" }

        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        var streamEnd = false
        while (sourceBuffer.size < bytesToRead && !streamEnd) {
            val bytesRead = super.read(sourceBuffer, bytesToRead - sourceBuffer.size)
            log.trace { "bytesRead=$bytesRead, sourceBuffer=$sourceBuffer" }
            if (bytesRead <= 0) {
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
            log.trace { "Finalize with padding if we are at the end." }
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

    /**
     * 스트림 끝까지 읽어 [sink]에 기록합니다.
     */
    fun readAll(sink: Buffer): Long {
        var totalBytesRead = 0L
        while (true) {
            val bytesRead = read(sink, DEFAULT_BUFFER_SIZE.toLong())
            if (bytesRead <= 0L) break
            totalBytesRead += bytesRead
        }
        return totalBytesRead
    }

    /**
     * 내부 리소스를 정리합니다.
     */
    override fun close() {
        super.close()
        sourceBuffer.close()
        decipheredBuffer.close()
    }
}

/**
 * [Source]를 [StreamingCipherSource]로 변환합니다.
 */
fun Source.asStreamingCipherSource(cipher: Cipher): StreamingCipherSource =
    StreamingCipherSource(this, cipher)
