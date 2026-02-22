package io.bluetape4k.io.okio.compress

import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.StreamingCompressor
import io.bluetape4k.io.compressor.asCompressor
import io.bluetape4k.io.okio.bufferOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireInRange
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

    /**
     * Okio 압축/해제에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: Buffer, byteCount: Long) {
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")

        // 요청된 길이만큼만 압축한다.
        val bytesToRead = byteCount
        val plainBytes = source.readByteArray(bytesToRead)

        // 압축
        val compressed = compressor.compress(plainBytes)
        log.debug { "압축: source=${plainBytes.size} bytes, compressed=${compressed.size} bytes" }
        super.write(bufferOf(compressed), compressed.size.toLong())
    }
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asCompressSink` 함수를 제공합니다.
 */
fun okio.Sink.asCompressSink(compressor: Compressor): CompressableSink {
    return CompressableSink(this, compressor)
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asCompressSink` 함수를 제공합니다.
 */
fun okio.Sink.asCompressSink(compressor: StreamingCompressor): CompressableSink {
    return CompressableSink(this, compressor.asCompressor())
}
