package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.assertZeroOrPositiveNumber
import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * [[java.nio.ByteBuffer]]를 저장소로 사용하는 [OutputStream] 구현체입니다.
 *
 * ```
 * val outputStream = ByteBufferOutputStream()
 * outputStream.write(1)
 * outputStream.write(byteArrayOf(2, 3, 4, 5))
 * val bytes = outputStream.toByteArray()
 * ```
 */
open class ByteBufferOutputStream private constructor(
    private val buffer: ByteBuffer,
): OutputStream() {

    companion object: KLogging() {

        @JvmStatic
        operator fun invoke(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        @JvmStatic
        operator fun invoke(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBuffer())
        }

        @JvmStatic
        operator fun invoke(buffer: ByteBuffer): ByteBufferOutputStream {
            return ByteBufferOutputStream(buffer)
        }

        @JvmStatic
        fun direct(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        @JvmStatic
        fun direct(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBufferDirect())
        }
    }

    override fun write(b: Int) {
        if (!buffer.hasRemaining()) {
            flush()
        }
        buffer.put(b.toByte())
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        off.assertZeroOrPositiveNumber("off")
        len.assertZeroOrPositiveNumber("len")

        if (buffer.remaining() < len) {
            flush()
        }
        buffer.put(b, off, len)
    }

    fun toByteArray(): ByteArray {
        buffer.flip()
        return buffer.getBytes()
    }
}
