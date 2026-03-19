package io.bluetape4k.tokenizer.utils

import io.bluetape4k.support.assertGe
import io.bluetape4k.support.assertZeroOrPositiveNumber
import io.bluetape4k.support.requireInRange
import java.io.Reader
import java.io.Serializable

/**
 * 유니코드 코드포인트 기반 문자 처리 연산을 일관된 방식으로 제공하는 추상 유틸리티다.
 *
 * ## 동작/계약
 * - 기본 구현은 `Java5CharacterUtils` singleton으로 제공된다.
 * - surrogate pair를 고려한 읽기/변환 API를 포함한다.
 * - 버퍼 기반 읽기 시 trailing high surrogate를 별도로 보관해 다음 호출과 연결한다.
 *
 * ```kotlin
 * val utils = CharacterUtils.getInstance()
 * val buffer = CharacterUtils.newCharacterBuffer(8)
 * // buffer.length == 0
 * // utils.codePointCount("한글") == 2
 * ```
 */
abstract class CharacterUtils: Serializable {

    companion object {
        @JvmStatic
        private val JAVA_5: CharacterUtils = Java5CharacterUtils()

        @JvmStatic
        /**
         * 런타임 기본 문자 유틸리티 구현 인스턴스를 반환한다.
         *
         * ## 동작/계약
         * - 항상 동일 singleton 인스턴스를 반환한다.
         * - 현재 구현은 `Java5CharacterUtils`로 고정되어 있다.
         *
         * ```kotlin
         * val one = CharacterUtils.getInstance()
         * val two = CharacterUtils.getInstance()
         * // one === two
         * ```
         */
        fun getInstance(): CharacterUtils = JAVA_5

        @JvmStatic
        /**
         * 지정 크기의 문자 버퍼를 생성한다.
         *
         * ## 동작/계약
         * - `bufferSize >= 2`를 `assert`로 검증한다.
         * - 버퍼 내용은 비어 있고 `offset`, `length`는 0으로 초기화된다.
         * - assert 비활성(`-ea` 미적용) 환경에서는 조건 검증이 생략될 수 있다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.buffer.size == 4
         * // buffer.length == 0
         * ```
         */
        fun newCharacterBuffer(bufferSize: Int): CharacterBuffer {
            assert(bufferSize >= 2) { "buffer size must be >= 2" }
            return CharacterBuffer(CharArray(bufferSize), 0, 0)
        }

        @JvmStatic
        /**
         * `Reader`에서 지정 길이만큼 문자를 읽어 대상 배열에 채운다.
         *
         * ## 동작/계약
         * - EOF(`-1`)를 만나거나 `len`만큼 채울 때까지 반복 읽기를 수행한다.
         * - 반환값은 실제로 읽은 문자 수다.
         * - 부분 읽기 상황에서도 이미 읽은 데이터는 `dest`에 유지된다.
         *
         * ```kotlin
         * val out = CharArray(5)
         * val read = CharacterUtils.readFully("abc".reader(), out, 0, 5)
         * // read == 3
         * // String(out, 0, read) == "abc"
         * ```
         */
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
     * `CharSequence`의 지정 위치 코드포인트를 반환한다.
     *
     * ## 동작/계약
     * - 구현체가 surrogate pair를 반영해 코드포인트를 계산한다.
     * - `offset`은 조회 시작 인덱스다.
     *
     * ```kotlin
     * val cp = CharacterUtils.getInstance().codePointAt("abc", 1)
     * // cp == 'b'.code
     * ```
     */
    abstract fun codePointAt(seq: CharSequence, offset: Int = 0): Int

    /**
     * 문자 배열 구간에서 지정 위치 코드포인트를 반환한다.
     *
     * ## 동작/계약
     * - `offset`은 시작 위치, `limit`은 탐색 가능한 끝 경계다.
     * - surrogate pair가 포함된 입력에서도 한 코드포인트를 반환한다.
     *
     * ```kotlin
     * val chars = "한".toCharArray()
     * val cp = CharacterUtils.getInstance().codePointAt(chars, 0, chars.size)
     * // Character.isValidCodePoint(cp) == true
     * ```
     */
    abstract fun codePointAt(chars: CharArray, offset: Int, limit: Int): Int

    /**
     * 입력 문자열의 코드포인트 개수를 반환한다.
     *
     * ## 동작/계약
     * - 구현체 기준으로 surrogate pair를 1개 코드포인트로 계산한다.
     * - 반환값은 문자열 길이와 다를 수 있다.
     *
     * ```kotlin
     * val count = CharacterUtils.getInstance().codePointCount("A😀")
     * // count == 2
     * ```
     */
    abstract fun codePointCount(seq: CharSequence): Int

    /**
     * 버퍼 구간의 문자를 코드포인트 단위로 소문자 변환한다.
     *
     * ## 동작/계약
     * - `limit`이 버퍼 크기를 넘으면 검증 예외가 발생한다.
     * - 변환은 전달된 `buffer`를 직접 수정한다.
     * - 인덱스 이동은 코드포인트 길이(`Character.toChars` 반환값)로 계산한다.
     *
     * ```kotlin
     * val chars = "ABC".toCharArray()
     * CharacterUtils.getInstance().toLowerCase(chars, 0, chars.size)
     * // String(chars) == "abc"
     * ```
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
     * 버퍼 구간의 문자를 코드포인트 단위로 대문자 변환한다.
     *
     * ## 동작/계약
     * - `limit`과 `offset`에 대해 범위 검증을 수행한다.
     * - 변환 결과는 입력 `buffer`에 제자리 반영된다.
     *
     * ```kotlin
     * val chars = "abc".toCharArray()
     * CharacterUtils.getInstance().toUpperCase(chars, 0, chars.size)
     * // String(chars) == "ABC"
     * ```
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
     * 문자 배열 구간을 코드포인트 배열로 변환해 저장한다.
     *
     * ## 동작/계약
     * - `srcLen`은 0 이상이어야 하며 음수면 검증 예외가 발생한다.
     * - 각 코드포인트를 `dest[destOff + index]`에 순차 기록한다.
     * - 반환값은 실제로 기록한 코드포인트 개수다.
     *
     * ```kotlin
     * val out = IntArray(4)
     * val count = CharacterUtils.getInstance().toCodePoints("ab".toCharArray(), 0, 2, out, 0)
     * // count == 2
     * // out[0] == 'a'.code
     * ```
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
     * 코드포인트 배열 구간을 문자 배열로 변환해 저장한다.
     *
     * ## 동작/계약
     * - `srcLen`은 0 이상이어야 한다.
     * - 각 코드포인트를 `Character.toChars`로 변환해 `dest`에 이어서 기록한다.
     * - 반환값은 기록된 문자 수(UTF-16 code unit 수)다.
     *
     * ```kotlin
     * val dest = CharArray(4)
     * val written = CharacterUtils.getInstance().toChars(intArrayOf('a'.code, 'b'.code), 0, 2, dest, 0)
     * // written == 2
     * // String(dest, 0, written) == "ab"
     * ```
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
     * 리더에서 문자를 읽어 버퍼를 채운다.
     *
     * ## 동작/계약
     * - 반환값이 `true`면 요청한 `numChars`만큼 완전히 채운 상태다.
     * - 구현체는 surrogate pair 경계를 보존하기 위해 trailing high surrogate를 보관할 수 있다.
     *
     * ```kotlin
     * val utils = CharacterUtils.getInstance()
     * val buffer = CharacterUtils.newCharacterBuffer(4)
     * val full = utils.fill(buffer, "ab".reader(), 2)
     * // full == true
     * ```
     */
    abstract fun fill(buffer: CharacterBuffer, reader: Reader, numChars: Int = buffer.buffer.size): Boolean

    /**
     * 지정 구간에서 코드포인트 오프셋 이동 후의 인덱스를 계산한다.
     *
     * ## 동작/계약
     * - `buf[start:start+count]` 범위를 기준으로 `index`에서 `offset`만큼 이동한다.
     * - 구현체는 문자 경계를 고려해 올바른 UTF-16 인덱스를 반환한다.
     *
     * ```kotlin
     * val chars = "abcd".toCharArray()
     * val index = CharacterUtils.getInstance().offsetByCodePoints(chars, 0, chars.size, 1, 2)
     * // index == 3
     * ```
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

    /**
     * 문자 버퍼 상태(배열, 오프셋, 길이, trailing surrogate)를 보관하는 컨테이너다.
     *
     * ## 동작/계약
     * - `buffer`는 실제 읽기/변환에 사용되는 가변 배열이다.
     * - `offset`, `length`는 내부 연산으로 갱신되며 외부에서는 읽기 중심으로 사용한다.
     * - `reset()` 호출 시 상태를 초기화해 재사용할 수 있다.
     *
     * ```kotlin
     * val buffer = CharacterUtils.newCharacterBuffer(6)
     * buffer.reset()
     * // buffer.offset == 0
     * // buffer.length == 0
     * ```
     */
    class CharacterBuffer private constructor(
        val buffer: CharArray,
    ) {

        companion object {
            @JvmStatic
            /**
             * 기존 배열을 감싸는 `CharacterBuffer`를 생성한다.
             *
             * ## 동작/계약
             * - 전달한 `offset`, `length`를 그대로 상태값으로 설정한다.
             * - 배열은 복사하지 않고 참조를 공유한다.
             *
             * ```kotlin
             * val raw = CharArray(4)
             * val buffer = CharacterUtils.CharacterBuffer(raw, offset = 1, length = 2)
             * // buffer.offset == 1
             * // buffer.length == 2
             * ```
             */
            operator fun invoke(buffer: CharArray, offset: Int = 0, length: Int = 0): CharacterBuffer {
                return CharacterBuffer(buffer).apply {
                    this.offset = offset
                    this.length = length
                }
            }
        }

        /**
         * 현재 유효 데이터의 시작 오프셋이다.
         *
         * ## 동작/계약
         * - 내부 로직에서만 갱신할 수 있고 외부에서는 읽기 전용으로 사용한다.
         * - `reset()` 호출 시 0으로 초기화된다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * buffer.reset()
         * // buffer.offset == 0
         * ```
         */
        var offset: Int = 0
            internal set
        /**
         * 버퍼에 채워진 유효 문자 수다.
         *
         * ## 동작/계약
         * - `fill` 호출 시 읽은 문자 수에 맞춰 갱신된다.
         * - `reset()` 호출 시 0으로 초기화된다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.length == 0
         * ```
         */
        var length: Int = 0
            internal set

        /**
         * 다음 읽기에서 이어 붙일 trailing high surrogate 문자를 저장한다.
         *
         * ## 동작/계약
         * - 유효한 값이 없을 때는 `0.toChar()`를 사용한다.
         * - `reset()` 호출 시 초기값으로 복원된다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * // buffer.lastTrailingHighSurrogate == 0.toChar()
         * ```
         */
        var lastTrailingHighSurrogate: Char = 0.toChar()

        /**
         * 버퍼 상태를 초기값으로 재설정한다.
         *
         * ## 동작/계약
         * - `offset`, `length`, `lastTrailingHighSurrogate`를 모두 초기화한다.
         * - 내부 배열 `buffer` 내용은 유지되며 메타데이터만 재설정된다.
         *
         * ```kotlin
         * val buffer = CharacterUtils.newCharacterBuffer(4)
         * buffer.reset()
         * // buffer.length == 0
         * ```
         */
        fun reset() {
            offset = 0
            length = 0
            lastTrailingHighSurrogate = 0.toChar()
        }
    }
}
