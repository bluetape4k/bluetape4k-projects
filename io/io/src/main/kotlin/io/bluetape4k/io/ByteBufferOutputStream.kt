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
         * 지정한 용량의 Direct [ByteBuffer]를 저장소로 사용하는 [ByteBufferOutputStream] 인스턴스를 생성합니다.
         * 버퍼 공간이 부족하면 자동으로 두 배 크기로 확장됩니다.
         *
         * ```kotlin
         * val stream = ByteBufferOutputStream(256)
         * stream.write(42)
         * val bytes = stream.toByteArray() // [42]
         * ```
         *
         * @param capacity 초기 버퍼 용량 (바이트)
         */
        @JvmStatic
        operator fun invoke(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        /**
         * [ByteArray]를 초기 내용으로 사용하는 [ByteBufferOutputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferOutputStream(byteArrayOf(1, 2, 3))
         * stream.write(4)
         * val bytes = stream.toByteArray() // [1, 2, 3, 4]
         * ```
         *
         * @param bytes 초기 데이터로 사용할 바이트 배열
         */
        @JvmStatic
        operator fun invoke(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBuffer())
        }

        /**
         * 기존 [ByteBuffer]를 저장소로 사용하는 [ByteBufferOutputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val buffer = ByteBuffer.allocate(64)
         * val stream = ByteBufferOutputStream(buffer)
         * stream.write(byteArrayOf(10, 20))
         * val bytes = stream.toByteArray() // [10, 20]
         * ```
         *
         * @param buffer 저장소로 사용할 [ByteBuffer]
         */
        @JvmStatic
        operator fun invoke(buffer: ByteBuffer): ByteBufferOutputStream {
            return ByteBufferOutputStream(buffer)
        }

        /**
         * 지정한 용량의 Direct [ByteBuffer]를 저장소로 사용하는 [ByteBufferOutputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferOutputStream.direct(512)
         * stream.write("hello".toByteArray())
         * val bytes = stream.toByteArray() // "hello".toByteArray()
         * ```
         *
         * @param capacity 초기 다이렉트 버퍼 용량 (바이트)
         */
        @JvmStatic
        fun direct(capacity: Int = DEFAULT_BUFFER_SIZE): ByteBufferOutputStream {
            return ByteBufferOutputStream(ByteBuffer.allocateDirect(capacity))
        }

        /**
         * [ByteArray]를 Direct [ByteBuffer]에 복사하여 초기 내용으로 사용하는 [ByteBufferOutputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferOutputStream.direct(byteArrayOf(5, 6, 7))
         * stream.write(8)
         * val bytes = stream.toByteArray() // [5, 6, 7, 8]
         * ```
         *
         * @param bytes 초기 데이터로 사용할 바이트 배열
         */
        @JvmStatic
        fun direct(bytes: ByteArray): ByteBufferOutputStream {
            return ByteBufferOutputStream(bytes.toByteBufferDirect())
        }
    }

    /**
     * 1바이트를 버퍼에 씁니다. 용량이 부족하면 자동으로 확장됩니다.
     *
     * ```kotlin
     * val stream = ByteBufferOutputStream()
     * stream.write(0xAB)
     * val bytes = stream.toByteArray() // [0xAB.toByte()]
     * ```
     *
     * @param b 쓸 바이트 값 (0~255)
     */
    override fun write(b: Int) {
        ensureCapacity(1)
        buffer.put(b.toByte())
    }

    /**
     * [b] 배열의 [off] 위치부터 [len] 바이트를 버퍼에 씁니다.
     *
     * ```kotlin
     * val stream = ByteBufferOutputStream()
     * stream.write(byteArrayOf(1, 2, 3, 4, 5), 1, 3)
     * val bytes = stream.toByteArray() // [2, 3, 4]
     * ```
     *
     * @param b   쓸 데이터가 담긴 배열
     * @param off 읽기 시작 오프셋
     * @param len 쓸 바이트 수
     */
    override fun write(b: ByteArray, off: Int, len: Int) {
        off.assertZeroOrPositiveNumber("off")
        len.assertZeroOrPositiveNumber("len")
        require(off + len <= b.size) { "off+len must be <= b.size (off=$off, len=$len, size=${b.size})" }

        ensureCapacity(len)
        buffer.put(b, off, len)
    }

    /**
     * 현재까지 쓰여진 내용을 [ByteArray]로 반환합니다. 내부 버퍼의 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val stream = ByteBufferOutputStream()
     * stream.write(byteArrayOf(10, 20, 30))
     * val bytes = stream.toByteArray() // [10, 20, 30]
     * ```
     *
     * @return 지금까지 쓰여진 모든 바이트를 담은 [ByteArray]
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
