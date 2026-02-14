package io.bluetape4k.io

import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.assertZeroOrPositiveNumber
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * [java.nio.ByteBuffer]를 저장소로 사용하는  [InputStream] 구현체입니다.
 *
 * ```
 * val bytes = byteArrayOf(1, 2, 3, 4, 5)
 * val inputStream = ByteBufferInputStream(bytes.toByteBuffer())
 * ```
 *
 * @param buffer [ByteBuffer] 인스턴스
 */
open class ByteBufferInputStream private constructor(
    private val buffer: ByteBuffer,
): InputStream() {

    companion object: KLogging() {

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE): ByteBufferInputStream {
            return ByteBufferInputStream(ByteBuffer.allocate(bufferSize))
        }

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(bytes: ByteArray): ByteBufferInputStream {
            return ByteBufferInputStream(bytes.toByteBuffer())
        }

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(buffer: ByteBuffer): ByteBufferInputStream {
            return ByteBufferInputStream(buffer)
        }

        /**
         * I/O 처리에서 `direct` 함수를 제공합니다.
         */
        @JvmStatic
        fun direct(bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE): ByteBufferInputStream {
            return ByteBufferInputStream(ByteBuffer.allocateDirect(bufferSize))
        }

        /**
         * I/O 처리에서 `direct` 함수를 제공합니다.
         */
        @JvmStatic
        fun direct(bytes: ByteArray): ByteBufferInputStream {
            return ByteBufferInputStream(bytes.toByteBufferDirect())
        }
    }

    /**
     * I/O 처리에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(): Int {
        return if (buffer.hasRemaining()) (buffer.get().toInt() and 0xFF) else -1
    }

    /**
     * I/O 처리에서 데이터를 읽어오는 `read` 함수를 제공합니다.
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        off.assertZeroOrPositiveNumber("off")
        len.assertZeroOrPositiveNumber("len")
        require(off + len <= b.size) { "off+len must be <= b.size (off=$off, len=$len, size=${b.size})" }

        if (len == 0) {
            return 0
        }
        val count = minOf(buffer.remaining(), len)
        if (count == 0) {
            return -1
        }

        buffer.get(b, off, count)
        return count
    }

    /**
     * I/O 처리에서 `available` 함수를 제공합니다.
     */
    override fun available(): Int = buffer.remaining()
}
