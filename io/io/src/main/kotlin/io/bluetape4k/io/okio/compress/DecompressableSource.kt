package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
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
        val sourceBuffer = okio.Buffer()

        // 압축 복원은 한 번에 모든 데이터를 복원해야 함
        val bytesRead = super.read(sourceBuffer, Long.MAX_VALUE)

        if (bytesRead < 0L) {
            return -1 // End of stream
        }

        val sourceSize = sourceBuffer.size
        val decompressed = compressor.decompress(sourceBuffer.readByteArray())
        val decompressedBuffer = bufferOf(decompressed)
        log.debug { "압축 복원: compressed=$sourceSize bytes, decompressed=${decompressedBuffer.size} bytes" }
        sink.write(decompressedBuffer, decompressedBuffer.size)

        return decompressedBuffer.size
    }
}

fun okio.Source.asDecompressSource(compressor: Compressor): DecompressableSource {
    return DecompressableSource(this, compressor)
}
