package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.requireGe
import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * 데이터를 압축하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * @see DecompressableSource
 */
open class CompressableSink(
    delegate: Sink,
    val compressor: Compressor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        // Compressor는 한 번에 모든 데이터를 압축해야 함
        byteCount.requireGe(source.size, "byteCount")

        // 요청한 바이트 수(또는 가능한 모든 바이트) 반환
        val bytesToRead = byteCount.coerceAtMost(source.size)
        val plainBytes = source.readByteArray(bytesToRead)
        log.trace { "Compressing: ${plainBytes.size} bytes" }

        // 압축
        val compressed = compressor.compress(plainBytes)
        val compressedSink = Buffer().write(compressed)
        super.write(compressedSink, compressedSink.size)
    }
}
