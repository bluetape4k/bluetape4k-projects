package io.bluetape4k.okio.compress

import io.bluetape4k.io.DEFAULT_BUFFER_SIZE
import io.bluetape4k.io.compressor.Compressor
import io.bluetape4k.io.compressor.StreamingCompressor
import io.bluetape4k.io.compressor.asCompressor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.okio.bufferOf
import io.bluetape4k.support.requireZeroOrPositiveNumber
import okio.Buffer
import okio.buffer
import java.io.IOException

/**
 * 압축된 데이터를 해제하여 [okio.Source]로 읽는 [okio.ForwardingSource] 구현체.
 * `close()` 전까지 모든 압축 데이터를 한 번에 로딩 후 복원합니다.
 *
 * ```kotlin
 * val output = Buffer()
 * CompressableSink(output, Compressors.LZ4).use { sink ->
 *     val source = bufferOf("hello world")
 *     sink.write(source, source.size)
 * }
 * val decompressSource = DecompressableSource(output, Compressors.LZ4)
 * val sink = Buffer()
 * decompressSource.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == "hello world"
 * ```
 *
 * @see CompressableSink
 */
open class DecompressableSource(
    delegate: okio.Source,
    val compressor: Compressor,
): okio.ForwardingSource(delegate) {

    companion object: KLogging() {
        const val MAX_NO_PROGRESS_READS = 8
    }

    private val decodedBuffer = Buffer()
    private var decodedReady: Boolean = false

    /**
     * Okio 압축/해제에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(sink: Buffer, byteCount: Long): Long {
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        ensureDecoded()
        if (decodedBuffer.size == 0L) {
            return -1 // End of stream
        }

        val bytesToReturn = byteCount.coerceAtMost(decodedBuffer.size)
        sink.write(decodedBuffer, bytesToReturn)
        return bytesToReturn
    }

    private fun ensureDecoded() {
        if (decodedReady) {
            return
        }
        decodedReady = true

        val sourceBuffer = Buffer()
        var noProgressCount = 0
        while (true) {
            val bytesRead = super.read(sourceBuffer, Long.MAX_VALUE)
            if (bytesRead < 0L) {
                break
            }
            if (bytesRead == 0L) {
                noProgressCount++
                if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to read compressed bytes from source: no progress.")
                }
                continue
            }
            noProgressCount = 0
        }

        val sourceSize = sourceBuffer.size
        val decompressed = compressor.decompress(sourceBuffer.readByteArray())
        decodedBuffer.write(bufferOf(decompressed), decompressed.size.toLong())
        log.debug { "압축 복원: compressed=$sourceSize bytes, decompressed=${decodedBuffer.size} bytes" }
    }
}

/**
 * [okio.Source]를 [Compressor]로 압축 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // 압축된 데이터
 * val source = (compressed as okio.Source).asDecompressSource(Compressors.GZip)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.asDecompressSource(compressor: Compressor): DecompressableSource {
    return DecompressableSource(this, compressor)
}

/**
 * [okio.Source]를 [StreamingCompressor]로 스트리밍 방식으로 압축 해제하는 [DecompressableSource]로 변환합니다.
 *
 * ```kotlin
 * val compressed = Buffer() // 스트리밍 압축된 데이터
 * val source = (compressed as okio.Source).asDecompressSource(
 *     Compressors.GZip as StreamingCompressor)
 * val sink = Buffer()
 * source.read(sink, Long.MAX_VALUE)
 * val text = sink.readUtf8()
 * // text == 원본 데이터
 * ```
 */
fun okio.Source.asDecompressSource(compressor: StreamingCompressor): DecompressableSource {
    return StreamingDecompressSource(this, compressor)
}

/**
 * [StreamingCompressor]를 사용해 스트리밍 방식으로 복원하여 [okio.Source]로 읽는 구현체입니다.
 */
open class StreamingDecompressSource(
    delegate: okio.Source,
    private val streamingCompressor: StreamingCompressor,
): DecompressableSource(delegate, streamingCompressor.asCompressor()) {

    companion object {
        private const val MAX_NO_PROGRESS_READS = 8
    }

    private val bufferedDelegate = delegate.buffer()
    private val decompressingStream = streamingCompressor.decompressing(bufferedDelegate.inputStream())
    private var closed = false

    override fun read(sink: Buffer, byteCount: Long): Long {
        ensureOpen()
        byteCount.requireZeroOrPositiveNumber("byteCount")
        if (byteCount == 0L) return 0L

        val readBufferSize = minOf(byteCount, DEFAULT_BUFFER_SIZE.toLong()).toInt()
        val bytes = ByteArray(readBufferSize)
        var noProgressCount = 0
        while (true) {
            val bytesRead = decompressingStream.read(bytes)
            if (bytesRead < 0) {
                return -1L
            }
            if (bytesRead == 0) {
                noProgressCount++
                if (noProgressCount >= MAX_NO_PROGRESS_READS) {
                    throw IOException("Unable to read decompressed bytes from stream: no progress.")
                }
                continue
            }

            sink.write(bytes, 0, bytesRead)
            return bytesRead.toLong()
        }
    }

    override fun close() {
        if (closed) {
            return
        }
        closed = true
        decompressingStream.close()
    }

    private fun ensureOpen() {
        check(!closed) { "Source is already closed." }
    }
}
