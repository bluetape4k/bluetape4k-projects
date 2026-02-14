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
    private var buffer: ByteBuffer,
): OutputStream() {

    companion object: KLogging() {

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBuffer())
        }

        /**
         * I/O 처리용 인스턴스 생성을 위한 진입점을 제공합니다.
         */
        @JvmStatic
        operator fun invoke(buffer: ByteBuffer): ByteBufferOutputStream {
            return ByteBufferOutputStream(buffer)
        }

        /**
         * I/O 처리에서 `direct` 함수를 제공합니다.
         */
        @JvmStatic
        fun direct(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        /**
         * I/O 처리에서 `direct` 함수를 제공합니다.
         */
        @JvmStatic
        fun direct(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBufferDirect())
        }
    }

    /**
     * I/O 처리에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(b: Int) {
        ensureCapacity(1)
        buffer.put(b.toByte())
    }

    /**
     * I/O 처리에서 데이터를 기록하는 `write` 함수를 제공합니다.
     */
    override fun write(b: ByteArray, off: Int, len: Int) {
        off.assertZeroOrPositiveNumber("off")
        len.assertZeroOrPositiveNumber("len")
        require(off + len <= b.size) { "off+len must be <= b.size (off=$off, len=$len, size=${b.size})" }

        ensureCapacity(len)
        buffer.put(b, off, len)
    }

    /**
     * I/O 처리 타입 변환을 위한 `toByteArray` 함수를 제공합니다.
     */
    fun toByteArray(): ByteArray {
        val dup = buffer.duplicate()
        dup.flip()
        return dup.getBytes()
    }

    private fun ensureCapacity(additional: Int) {
        if (additional <= buffer.remaining()) return
        val required = buffer.position() + additional
        val newCapacity = maxOf(buffer.capacity() * 2, required)
        val newBuffer = if (buffer.isDirect) {
            ByteBuffer.allocateDirect(newCapacity)
        } else {
            ByteBuffer.allocate(newCapacity)
        }
        buffer.flip()
        newBuffer.put(buffer)
        buffer = newBuffer
    }
}
