package io.bluetape4k.io.compressor

import io.bluetape4k.io.toByteArray
import io.bluetape4k.io.toInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * [StreamingCompressor] 구현 도우미를 제공하는 팩토리 객체입니다.
 *
 * 기존 [Compressor] 또는 스트림 래퍼 팩토리 함수로부터 [StreamingCompressor]를 생성할 수 있습니다.
 *
 * ```kotlin
 * // 기존 Compressor를 StreamingCompressor로 변환
 * val streaming = StreamingCompressors.from(Compressors.LZ4)
 *
 * val plain = "hello streaming world".toByteArray()
 * val compressed = streaming.compress(plain)
 * val restored = streaming.decompress(compressed)
 * println(String(restored)) // "hello streaming world"
 * ```
 */
object StreamingCompressors {

    /**
     * [compressor]를 스트리밍 계약으로 감싸는 어댑터를 생성합니다.
     *
     * 내부적으로 전체 바이트를 메모리에 올리는 one-shot 동작입니다.
     * 대용량 데이터보다는 기존 [Compressor]를 [StreamingCompressor] 인터페이스로 사용해야 할 때 활용하세요.
     *
     * ```kotlin
     * val streaming = StreamingCompressors.from(Compressors.Zstd)
     *
     * val out = ByteArrayOutputStream()
     * streaming.compressing(out).use { it.write("data".toByteArray()) }
     * val compressed = out.toByteArray()
     *
     * val restored = streaming.decompress(compressed)
     * println(String(restored)) // "data"
     * ```
     *
     * @param compressor 래핑할 [Compressor] 구현체
     * @return [StreamingCompressor] 어댑터
     */
    @JvmStatic
    fun from(compressor: Compressor): StreamingCompressor = OneShotStreamingCompressor(compressor)

    /**
     * 출력/입력 스트림 래퍼 팩토리 함수 쌍으로 [StreamingCompressor]를 생성합니다.
     *
     * 진짜 스트리밍 압축 라이브러리(예: Zstd OutputStream)를 직접 연결할 때 사용합니다.
     *
     * ```kotlin
     * val streaming = StreamingCompressors.of(
     *     compressing   = { out -> GZIPOutputStream(out) },
     *     decompressing = { input -> GZIPInputStream(input) },
     * )
     *
     * val plain = "hello gzip streaming".toByteArray()
     * val compressed = streaming.compress(plain)
     * val restored = streaming.decompress(compressed)
     * println(String(restored)) // "hello gzip streaming"
     * ```
     *
     * @param compressing   압축 스트림 래퍼 팩토리 — `(OutputStream) -> OutputStream`
     * @param decompressing 복원 스트림 래퍼 팩토리 — `(InputStream) -> InputStream`
     * @return [StreamingCompressor] 구현체
     */
    @JvmStatic
    fun of(
        compressing: (OutputStream) -> OutputStream,
        decompressing: (InputStream) -> InputStream,
    ): StreamingCompressor = StreamWrappingStreamingCompressor(compressing, decompressing)
}

/**
 * [Compressor]를 [StreamingCompressor]로 변환합니다.
 *
 * 내부적으로 one-shot 어댑터를 사용하므로, 전체 바이트가 메모리에 버퍼링됩니다.
 *
 * ```kotlin
 * val streaming = Compressors.LZ4.asStreamingCompressor()
 *
 * val plain = "hello".toByteArray()
 * val compressed = streaming.compress(plain)
 * val restored = streaming.decompress(compressed)
 * println(String(restored)) // "hello"
 * ```
 */
fun Compressor.asStreamingCompressor(): StreamingCompressor =
    StreamingCompressors.from(this)

/**
 * [StreamingCompressor]를 [Compressor]로 변환합니다.
 *
 * 내부적으로 바이트 배열 전체를 읽는 one-shot 어댑터를 사용합니다.
 *
 * ```kotlin
 * val compressor = Compressors.Streaming.Zstd.asCompressor()
 *
 * val plain = "hello".toByteArray()
 * val compressed = compressor.compress(plain)
 * val restored = compressor.decompress(compressed)
 * println(String(restored)) // "hello"
 * ```
 */
fun StreamingCompressor.asCompressor(): Compressor =
    StreamingCompressorAdapter(this)

private class OneShotStreamingCompressor(
    private val compressor: Compressor,
): StreamingCompressor {

    override fun compressing(output: OutputStream): OutputStream =
        OneShotCompressOutputStream(output, compressor)

    override fun decompressing(input: InputStream): InputStream =
        compressor.decompress(input.toByteArray()).toInputStream()
}

private class StreamingCompressorAdapter(
    private val streaming: StreamingCompressor,
): Compressor {
    override fun compress(plain: ByteArray?): ByteArray = streaming.compress(plain)

    override fun decompress(compressed: ByteArray?): ByteArray = streaming.decompress(compressed)
}

private class StreamWrappingStreamingCompressor(
    private val compressingFactory: (OutputStream) -> OutputStream,
    private val decompressingFactory: (InputStream) -> InputStream,
): StreamingCompressor {

    override fun compressing(output: OutputStream): OutputStream = compressingFactory(output)

    override fun decompressing(input: InputStream): InputStream = decompressingFactory(input)
}

private class OneShotCompressOutputStream(
    private val delegate: OutputStream,
    private val compressor: Compressor,
): OutputStream() {

    private val buffer = java.io.ByteArrayOutputStream()
    private var closed = false

    override fun write(b: Int) {
        ensureOpen()
        buffer.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        ensureOpen()
        buffer.write(b, off, len)
    }

    override fun flush() {
        ensureOpen()
    }

    override fun close() {
        if (closed) {
            return
        }

        closed = true
        var thrown: Throwable? = null
        try {
            val compressed = compressor.compress(buffer.toByteArray())
            delegate.write(compressed)
            delegate.flush()
        } catch (e: Throwable) {
            thrown = e
            throw e
        } finally {
            try {
                delegate.close()
            } catch (closeEx: Throwable) {
                if (thrown != null) {
                    thrown.addSuppressed(closeEx)
                } else {
                    throw closeEx
                }
            }
        }
    }

    private fun ensureOpen() {
        check(!closed) { "Stream is already closed." }
    }
}
