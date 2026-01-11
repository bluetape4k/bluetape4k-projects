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

        if (bytesRead < 0) {
            return -1 // End of stream
        }

        val decompressed = compressor.decompress(sourceBuffer.readByteArray())
        log.debug { "압축 복원: compressed=$bytesRead bytes, decompressed=${decompressed.size} bytes" }
        sink.write(bufferOf(decompressed), decompressed.size.toLong())
        return decompressed.size.toLong()
    }
}

fun okio.Source.asDecompressSource(compressor: Compressor): DecompressableSource {
    return DecompressableSource(this, compressor)
}
