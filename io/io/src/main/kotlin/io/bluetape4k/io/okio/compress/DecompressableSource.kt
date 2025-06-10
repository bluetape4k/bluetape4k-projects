package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireGt
import okio.Buffer

/**
 * 데이터를 압축 해제하여 [okio.Source]로 읽는 [okio.ForwardingSource] 구현체.
 *
 * @see CompressableSink
 */
open class DecompressableSource(
    delegate: okio.Source,
    val compressor: Compressor,
): okio.ForwardingSource(delegate) {

    companion object: KLogging()

    override fun read(sink: Buffer, byteCount: Long): Long {
        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        byteCount.requireGt(0, "byteCount")

        val sourceBuffer = Buffer()
        val decompressedBuffer = Buffer()

        var streamEnd = false
        while (sourceBuffer.size < byteCount && !streamEnd) {
            val bytesRead = super.read(sourceBuffer, byteCount - sourceBuffer.size)
            log.trace { "byteCount=$byteCount, sourceBuffer.size=${sourceBuffer.size}" }
            if (bytesRead < 0) {
                streamEnd = true
            }
        }

        val bytes = sourceBuffer.readByteArray()
        log.trace { "source buffer bytes: ${bytes.size}" }
        val decompressed = compressor.decompress(bytes)
        log.trace { "decompressed bytes: ${decompressed.size}" }
        decompressedBuffer.write(decompressed)

        sink.write(decompressedBuffer, decompressedBuffer.size)

        return decompressedBuffer.size
    }
}
