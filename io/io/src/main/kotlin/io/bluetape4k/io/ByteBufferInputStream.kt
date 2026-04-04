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
         * 지정한 크기의 힙 [ByteBuffer]를 저장소로 사용하는 [ByteBufferInputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferInputStream(1024)
         * val read = stream.read() // -1 (빈 버퍼)
         * ```
         *
         * @param bufferSize 할당할 버퍼 크기 (바이트)
         */
        @JvmStatic
        operator fun invoke(bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE): ByteBufferInputStream {
            return ByteBufferInputStream(ByteBuffer.allocate(bufferSize))
        }

        /**
         * [ByteArray]를 저장소로 사용하는 [ByteBufferInputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferInputStream(byteArrayOf(1, 2, 3, 4, 5))
         * val first = stream.read() // 1
         * ```
         *
         * @param bytes 읽어들일 바이트 배열
         */
        @JvmStatic
        operator fun invoke(bytes: ByteArray): ByteBufferInputStream {
            return ByteBufferInputStream(bytes.toByteBuffer())
        }

        /**
         * 기존 [ByteBuffer]를 저장소로 사용하는 [ByteBufferInputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val buffer = ByteBuffer.wrap(byteArrayOf(10, 20, 30))
         * val stream = ByteBufferInputStream(buffer)
         * val first = stream.read() // 10
         * ```
         *
         * @param buffer 읽어들일 [ByteBuffer]
         */
        @JvmStatic
        operator fun invoke(buffer: ByteBuffer): ByteBufferInputStream {
            return ByteBufferInputStream(buffer)
        }

        /**
         * 지정한 크기의 Direct [ByteBuffer]를 저장소로 사용하는 [ByteBufferInputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferInputStream.direct(4096)
         * val available = stream.available() // 0
         * ```
         *
         * @param bufferSize 할당할 다이렉트 버퍼 크기 (바이트)
         */
        @JvmStatic
        fun direct(bufferSize: Int = kotlin.io.DEFAULT_BUFFER_SIZE): ByteBufferInputStream {
            return ByteBufferInputStream(ByteBuffer.allocateDirect(bufferSize))
        }

        /**
         * [ByteArray]를 Direct [ByteBuffer]에 복사하여 사용하는 [ByteBufferInputStream] 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val stream = ByteBufferInputStream.direct(byteArrayOf(7, 8, 9))
         * val first = stream.read() // 7
         * ```
         *
         * @param bytes 읽어들일 바이트 배열
         */
        @JvmStatic
        fun direct(bytes: ByteArray): ByteBufferInputStream {
            return ByteBufferInputStream(bytes.toByteBufferDirect())
        }
    }

    /**
     * 버퍼에서 1바이트를 읽어 0~255 범위의 정수로 반환합니다. 버퍼가 소진된 경우 -1을 반환합니다.
     *
     * ```kotlin
     * val stream = ByteBufferInputStream(byteArrayOf(0xFF.toByte()))
     * val value = stream.read() // 255
     * ```
     */
    override fun read(): Int {
        return if (buffer.hasRemaining()) (buffer.get().toInt() and 0xFF) else -1
    }

    /**
     * 버퍼에서 최대 [len] 바이트를 읽어 [b] 배열의 [off] 위치부터 저장합니다.
     *
     * ```kotlin
     * val stream = ByteBufferInputStream(byteArrayOf(1, 2, 3, 4, 5))
     * val buf = ByteArray(3)
     * val count = stream.read(buf, 0, 3) // 3
     * // buf = [1, 2, 3]
     * ```
     *
     * @param b   읽은 데이터를 저장할 배열
     * @param off 저장 시작 오프셋
     * @param len 읽을 최대 바이트 수
     * @return 실제로 읽은 바이트 수, 버퍼 소진 시 -1
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
     * 현재 버퍼에서 읽을 수 있는 잔여 바이트 수를 반환합니다.
     *
     * ```kotlin
     * val stream = ByteBufferInputStream(byteArrayOf(1, 2, 3))
     * stream.available() // 3
     * stream.read()
     * stream.available() // 2
     * ```
     */
    override fun available(): Int = buffer.remaining()
}
