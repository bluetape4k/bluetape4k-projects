package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
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
    private val compressor: Compressor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    override fun write(source: Buffer, byteCount: Long) {
        // byteCount.requirePositiveNumber("byteCount")

        // 압축은 `source`의 모든 데이터를 압축해야 함
        val bytesToRead = source.size
        val plainBytes = source.readByteArray(bytesToRead)

        // 압축
        val compressed = compressor.compress(plainBytes)
        log.debug { "압축: source=${plainBytes.size} bytes, compressed=${compressed.size} bytes" }
        super.write(bufferOf(compressed), compressed.size.toLong())
    }
}

fun okio.Sink.asCompressSink(compressor: Compressor): CompressableSink {
    return CompressableSink(this, compressor)
}
