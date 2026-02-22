package io.bluetape4k.io.compressor

import io.bluetape4k.io.toByteArray
import io.bluetape4k.io.toInputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * [StreamingCompressor] 구현 도우미를 제공합니다.
 */
object StreamingCompressors {

    /**
     * [compressor]를 스트리밍 계약으로 감싸는 어댑터를 생성합니다.
     *
     * 주의: 이 어댑터는 내부적으로 전체 바이트를 메모리에 올리는 one-shot 동작입니다.
     */
    @JvmStatic
    fun from(compressor: Compressor): StreamingCompressor = OneShotStreamingCompressor(compressor)

    /**
     * 출력/입력 스트림 래퍼 팩토리로 [StreamingCompressor]를 생성합니다.
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
 * 내부적으로 one-shot 어댑터를 사용합니다.
 */
fun Compressor.asStreamingCompressor(): StreamingCompressor =
    StreamingCompressors.from(this)

/**
 * [StreamingCompressor]를 [Compressor]로 변환합니다.
 *
 * 내부적으로 바이트 배열 전체를 읽는 one-shot 어댑터를 사용합니다.
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
