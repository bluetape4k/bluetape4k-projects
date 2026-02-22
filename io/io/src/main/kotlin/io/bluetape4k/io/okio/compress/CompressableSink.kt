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
import okio.buffer

/**
 * 데이터를 압축하여 [Sink]에 쓰는 [Sink] 구현체.
 *
 * `Compressor`는 one-shot 계약이므로, 이 구현은 `write()` 데이터들을 내부에 누적한 뒤
 * `close()` 시점에 한 번만 압축해 delegate에 기록합니다.
 * 따라서 압축 결과를 보장하려면 반드시 `close()`(또는 `use { ... }`)를 호출해야 합니다.
 *
 * @see DecompressableSource
 */
open class CompressableSink(
    delegate: Sink,
    private val compressor: Compressor,
): ForwardingSink(delegate) {

    companion object: KLogging()

    private val plainBuffer = Buffer()
    private var closed = false

    /**
     * Okio 압축/해제에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(source: Buffer, byteCount: Long) {
        ensureOpen()
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")

        // one-shot compressor 계약을 위해 close 시점에 한 번만 압축한다.
        plainBuffer.write(source, byteCount)
    }

    override fun flush() {
        ensureOpen()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true

        val plainBytes = plainBuffer.readByteArray()
        val compressed = compressor.compress(plainBytes)
        log.debug { "압축: source=${plainBytes.size} bytes, compressed=${compressed.size} bytes" }
        super.write(bufferOf(compressed), compressed.size.toLong())
        super.flush()
        super.close()
    }

    private fun ensureOpen() {
        check(!closed) { "Sink is already closed." }
    }
}

/**
 * [StreamingCompressor]를 사용해 스트리밍 방식으로 압축하여 [Sink]에 쓰는 구현체입니다.
 */
open class StreamingCompressSink(
    delegate: Sink,
    private val compressor: StreamingCompressor,
): CompressableSink(delegate, compressor.asCompressor()) {

    private val bufferedDelegate = delegate.buffer()
    private val compressingStream = compressor.compressing(bufferedDelegate.outputStream())
    private var closed = false

    override fun write(source: Buffer, byteCount: Long) {
        ensureOpen()
        if (byteCount <= 0L) return
        byteCount.requireInRange(0, source.size, "byteCount")

        val bytes = source.readByteArray(byteCount)
        compressingStream.write(bytes, 0, bytes.size)
    }

    override fun flush() {
        ensureOpen()
        compressingStream.flush()
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        compressingStream.close()
    }

    private fun ensureOpen() {
        check(!closed) { "Sink is already closed." }
    }
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asCompressSink` 함수를 제공합니다.
 *
 * 반환된 [CompressableSink]는 `close()` 시점에 압축 결과가 확정됩니다.
 */
fun okio.Sink.asCompressSink(compressor: Compressor): CompressableSink {
    return CompressableSink(this, compressor)
}

/**
 * Okio 압축/해제 타입 변환을 위한 `asCompressSink` 함수를 제공합니다.
 *
 * 반환된 sink는 스트리밍 압축을 수행하며, footer/finalize 기록을 위해 `close()`가 필요합니다.
 */
fun okio.Sink.asCompressSink(compressor: StreamingCompressor): CompressableSink {
    return StreamingCompressSink(this, compressor)
}
