package io.bluetape4k.tokenizer.utils

import io.bluetape4k.support.assertGe
import io.bluetape4k.support.assertZeroOrPositiveNumber
import io.bluetape4k.support.requireInRange
import java.io.Reader
import java.io.Serializable

/**
 * [CharacterUtils] provides a unified interface to Character-related operations.
 */
abstract class CharacterUtils: Serializable {

    companion object {
        @JvmStatic
        private val JAVA_5: CharacterUtils = Java5CharacterUtils()

        @JvmStatic
        fun getInstance(): CharacterUtils = JAVA_5

        @JvmStatic
        fun newCharacterBuffer(bufferSize: Int): CharacterBuffer {
            assert(bufferSize >= 2) { "buffer size must be >= 2" }
            return CharacterBuffer(CharArray(bufferSize), 0, 0)
        }

        @JvmStatic
        fun readFully(reader: Reader, dest: CharArray, offset: Int, len: Int): Int {
            var read = 0
            while (read < len) {
                val r = reader.read(dest, offset + read, len - read)
                if (r == -1) {
                    break
                }
                read += r
            }
            return read
        }
    }

    /**
     * [CharSequence] 의 주어진 인덱스에 있는 유니코드 code point를 반환합니다.
     *
     * @param seq    문자열
     * @param offset 오프셋
     * @return [offset]의 유니코드 code point
     */
    abstract fun codePointAt(seq: CharSequence, offset: Int = 0): Int

    /**
     * [CharArray]의 주어진 인덱스에 있는 유니코드 code point를 반환합니다.
     *
     * @param chars  문자 배열
     * @param offset 오프셋 (시작 인덱스)
     * @param limit  코드 포인트를 가져올 끝 인덱스
     * @return [offset]의 유니코드 code point
     */
    abstract fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int

    /**
     * [seq]의 문자 수를 반환합니다.
     */
    abstract fun codePointCount(seq: CharSequence): Int

    /**
     * [buffer]의 [offset]부터 [limit]까지의 문자를 소문자로 변환합니다.
     */
    fun toLowerCase(buffer: CharArray, offset: Int, limit: Int) {
        buffer.size.assertGe(limit, "buffer size")
        offset.requireInRange(0, buffer.size, "offset")

        var i = offset
        while (i < limit) {
            i += Character.toChars(Character.toLowerCase(codePointAt(buffer, i, limit)), buffer, i)
        }
    }

    /**
     * [buffer]의 [offset]부터 [limit]까지의 문자를 대문자로 변환합니다.
     */
    fun toUpperCase(buffer: CharArray, offset: Int, limit: Int) {
        buffer.size.assertGe(limit, "buffer size")
        offset.requireInRange(0, buffer.size, "offset")

        var i = offset
        while (i < limit) {
            i += Character.toChars(Character.toUpperCase(codePointAt(buffer, i, limit)), buffer, i)
        }
    }

    /**
     * [src]의 [srcOff]부터 [srcLen]까지의 문자를 code points로 변환하여 [dest]에 저장합니다.
     *
     * @param src    변환할 문자 배열
     * @param srcOff 변환할 문자 배열의 시작 인덱스
     * @param srcLen 변환할 문자 배열의 길이
     * @param dest   변환된 code points를 저장할 배열
     * @param destOff 변환된 code points를 저장할 배열의 시작 인덱스
     * @return 변환된 code points의 수
     */
    fun toCodePoints(src: CharArray, srcOff: Int, srcLen: Int, dest: IntArray, destOff: Int): Int {
        srcLen.assertZeroOrPositiveNumber("srcLen")

        var codePointCount = 0
        var i = 0
        while (i < srcLen) {
            val cp = codePointAt(src, srcOff + i, srcOff + srcLen)
            val charCount = Character.charCount(cp)
            dest[destOff + codePointCount++] = cp
            i += charCount
        }
        return codePointCount
    }

    /**
     * [src]의 [srcOff]부터 [srcLen]까지의 code points를 문자로 변환하여 [dest]에 저장합니다.
     *
     * @param src    변환할 code points 배열
     * @param srcOff 변환할 code points 배열의 시작 인덱스
     * @param srcLen 변환할 code points 배열의 길이
     * @param dest   변환된 문자를 저장할 배열
     * @param destOff 변환된 문자를 저장할 배열의 시작 인덱스
     * @return 변환된 문자의 수
     */
    fun toChars(src: IntArray, srcOff: Int, srcLen: Int, dest: CharArray, destOff: Int): Int {
        srcLen.assertZeroOrPositiveNumber("srcLen")

        var written = 0
        for (i in 0 until srcLen) {
            written += Character.toChars(src[srcOff + i], dest, destOff + written)
        }
        return written
    }

    /**
     * [buffer]에 [reader]로부터 최대 [numChars]만큼의 문자를 채웁니다.
     *
     * @param buffer   문자 버퍼
     * @param reader   문자를 읽을 [Reader]
     * @param numChars 읽을 문자 수
     * @return [buffer]가 완전히 채워졌으면 `true`, 그렇지 않으면 `false`
     */
    abstract fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int = buffer.buffer.size): Boolean

    /**
     * `buf[start:start+count]` 의 offset code points 떨어진 위치를 반환합니다.
     */
    abstract fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int

    private class Java5CharacterUtils: CharacterUtils() {

        override fun codePointAt(seq: CharSequence, offset: Int): Int {
            return Character.codePointAt(seq, offset)
        }

        override fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int {
            return Character.codePointAt(chars, offset, limit)
        }

        override fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int): Boolean {
            buffer.buffer.size.assertGe(2, "buffer size")
            numChars.requireInRange(2, buffer.buffer.size, "numChars")
            // assert(numChars in 2..buffer.buffer.size) { "numCharrs must be 2 .. buffer size" }

            val charBuffer = buffer.buffer
            buffer.offset = 0

            // Install the previously saved ending high surrogate:
            val offset = if (buffer.lastTrailingHighSurrogate != 0.toChar()) {
                charBuffer[0] = buffer.lastTrailingHighSurrogate
                buffer.lastTrailingHighSurrogate = 0.toChar()
                1
            } else {
                0
            }

            val read = readFully(reader, charBuffer, offset, numChars - offset)

            buffer.length = offset + read
            val result = buffer.length == numChars
            if (buffer.length < numChars) {
                // We failed to fill the buffer. Even if the last char is a high
                // surrogate, there is nothing we can do
                return result
            }

            if (Character.isHighSurrogate(charBuffer[buffer.length - 1])) {
                buffer.lastTrailingHighSurrogate = charBuffer[--buffer.length]
            }

            return result
        }

        override fun codePointCount(seq: CharSequence): Int {
            return Character.codePointCount(seq, 0, seq.length)
        }

        override fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int {
            return Character.offsetByCodePoints(buf, start, count, index, offset)
        }
    }

    private class Java4CharacterUtils: CharacterUtils() {

        override fun codePointAt(seq: CharSequence, offset: Int): Int {
            return seq[offset].code
        }

        override fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int {
            require(offset < limit) { "offset[$offset] must be less than limit[$limit]" }
            return chars[offset].code
        }

        override fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int): Boolean {
            assert(buffer.buffer.size >= 1)
            require(numChars in 1..buffer.buffer.size) {
                "numChars must be 1 .. the buffer size[${buffer.buffer.size}]"
            }

            buffer.offset = 0
            val read = readFully(reader, buffer.buffer, 0, numChars)
            buffer.length = read
            buffer.lastTrailingHighSurrogate = 0.toChar()
            return read == numChars
        }

        override fun codePointCount(seq: CharSequence): Int {
            return seq.length
        }

        override fun offsetByCodePoints(buf: CharArray, start: Int, count: Int, index: Int, offset: Int): Int {
            val result = index + offset
            check(result in 0..count) { "index[$index]+offset[$offset] must be 0 .. count[$count]" }
            return result
        }

    }

    class CharacterBuffer private constructor(
        val buffer: CharArray,
    ) {

        companion object {
            @JvmStatic
            operator fun invoke(buffer: CharArray, offset: Int = 0, length: Int = 0): CharacterBuffer {
                return CharacterBuffer(buffer).apply {
                    this.offset = offset
                    this.length = length
                }
            }
        }

        var offset: Int = 0
            internal set
        var length: Int = 0
            internal set

        var lastTrailingHighSurrogate: Char = 0.toChar()

        fun reset() {
            offset = 0
            length = 0
            lastTrailingHighSurrogate = 0.toChar()
        }
    }
}
