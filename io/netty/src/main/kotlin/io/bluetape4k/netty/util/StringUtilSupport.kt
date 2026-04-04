package io.bluetape4k.netty.util

import io.netty.util.internal.StringUtil

/**
 * Netty 처리에서 `byteToHexStringPadded` 함수를 제공합니다.
 *
 * ```kotlin
 * val sb = StringBuilder()
 * sb.byteToHexStringPadded(0xAB)
 * // sb.toString() == "ab"
 * ```
 */
fun <T : Appendable> T.byteToHexStringPadded(value: Int): T = StringUtil.byteToHexStringPadded(this, value)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringPadded` 함수를 제공합니다.
 *
 * ```kotlin
 * val hex = byteArrayOf(0xAB.toByte(), 0xCD.toByte()).toHexStringPadded()
 * // hex == "abcd"
 * ```
 */
fun ByteArray.toHexStringPadded(
    offset: Int = 0,
    length: Int = size,
): String = StringUtil.toHexStringPadded(this, offset, length)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringPaddedAs` 함수를 제공합니다.
 *
 * ```kotlin
 * val sb = StringBuilder()
 * byteArrayOf(0xAB.toByte()).toHexStringPaddedAs(sb)
 * // sb.toString() == "ab"
 * ```
 */
fun <T : Appendable> ByteArray.toHexStringPaddedAs(
    dest: T,
    offset: Int = 0,
    length: Int = size,
): T = StringUtil.toHexStringPadded(dest, this, offset, length)

/**
 * Netty 처리에서 `byteToHexString` 함수를 제공합니다.
 *
 * ```kotlin
 * val sb = StringBuilder()
 * sb.byteToHexString(0xAB)
 * // sb.toString() == "ab"
 * ```
 */
fun <T : Appendable> T.byteToHexString(value: Int): T = StringUtil.byteToHexStringPadded(this, value)

/**
 * Netty 처리 타입 변환을 위한 `toHexString` 함수를 제공합니다.
 *
 * ```kotlin
 * val hex = byteArrayOf(0xAB.toByte(), 0xCD.toByte()).toHexString()
 * // hex == "abcd"
 * ```
 */
fun ByteArray.toHexString(
    offset: Int = 0,
    length: Int = size,
): String = StringUtil.toHexStringPadded(this, offset, length)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringAs` 함수를 제공합니다.
 *
 * ```kotlin
 * val sb = StringBuilder()
 * byteArrayOf(0xAB.toByte()).toHexStringAs(sb)
 * // sb.toString() == "ab"
 * ```
 */
fun <T : Appendable> ByteArray.toHexStringAs(
    dest: T,
    offset: Int = 0,
    length: Int = size,
): T = StringUtil.toHexStringPadded(dest, this, offset, length)

/**
 * Netty 처리에서 `decodeHexNibble` 함수를 제공합니다.
 *
 * ```kotlin
 * val nibble = 'a'.decodeHexNibble()
 * // nibble == 10
 * ```
 */
fun Char.decodeHexNibble(): Int = StringUtil.decodeHexNibble(this)

/**
 * Netty 처리에서 `decodeHexByte` 함수를 제공합니다.
 *
 * ```kotlin
 * val byte = "ab".decodeHexByte(0)
 * // byte == 0xAB.toByte()
 * ```
 */
fun CharSequence.decodeHexByte(pos: Int): Byte = StringUtil.decodeHexByte(this, pos)

/**
 * Netty 처리에서 `decodeHexDump` 함수를 제공합니다.
 *
 * ```kotlin
 * val bytes = "abcd".decodeHexDump()
 * // bytes.toList() == listOf<Byte>(0xAB.toByte(), 0xCD.toByte())
 * ```
 */
fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray = StringUtil.decodeHexDump(this, fromIndex, length)

/**
 * [Class.getSimpleName]과 유사하지만, anonymous class 에서도 잘 작동한다
 *
 * ```kotlin
 * val name = String::class.java.simpleClassName
 * // name == "String"
 * ```
 */
val Class<*>.simpleClassName: String get() = StringUtil.simpleClassName(this)

/**
 * Netty 처리에서 `escapeCsv` 함수를 제공합니다.
 *
 * ```kotlin
 * val escaped = "hello, world".escapeCsv()
 * // escaped == "\"hello, world\""
 * ```
 */
fun CharSequence.escapeCsv(trimWhiteSpace: Boolean = false): CharSequence = StringUtil.escapeCsv(this, trimWhiteSpace)

/**
 * Netty 처리에서 `unescapeCsv` 함수를 제공합니다.
 *
 * ```kotlin
 * val unescaped = "\"hello, world\"".unescapeCsv()
 * // unescaped == "hello, world"
 * ```
 */
fun CharSequence.unescapeCsv(): CharSequence = StringUtil.unescapeCsv(this)

/**
 * Netty 처리에서 `unescapeCsvFields` 함수를 제공합니다.
 *
 * ```kotlin
 * val fields = "a,\"b,c\",d".unescapeCsvFields()
 * // fields.size == 3
 * ```
 */
fun CharSequence.unescapeCsvFields(): List<CharSequence> = StringUtil.unescapeCsvFields(this)

/**
 * Netty 처리에서 `indexOfNonWhiteSpace` 함수를 제공합니다.
 *
 * ```kotlin
 * val idx = "  hello".indexOfNonWhiteSpace()
 * // idx == 2
 * ```
 */
fun CharSequence.indexOfNonWhiteSpace(offset: Int = 0): Int = StringUtil.indexOfNonWhiteSpace(this, offset)

/**
 * Netty 처리에서 `indexOfWhiteSpace` 함수를 제공합니다.
 *
 * ```kotlin
 * val idx = "hello world".indexOfWhiteSpace()
 * // idx == 5
 * ```
 */
fun CharSequence.indexOfWhiteSpace(offset: Int = 0): Int = StringUtil.indexOfWhiteSpace(this, offset)

/**
 * 이 문자가 서로게이트(surrogate) 문자인지 여부를 반환합니다.
 *
 * ```kotlin
 * val isSurr = '\uD800'.isSurrogate
 * // isSurr == true
 * ```
 */
val Char.isSurrogate: Boolean get() = StringUtil.isSurrogate(this)

/**
 * 이 문자가 큰따옴표(`"`)인지 여부를 반환합니다.
 *
 * ```kotlin
 * val isDQ = '"'.isDoubleQuote
 * // isDQ == true
 * ```
 */
val Char.isDoubleQuote: Boolean get() = StringUtil.DOUBLE_QUOTE == this

/**
 * Netty 처리에서 `trimOws` 함수를 제공합니다.
 *
 * ```kotlin
 * val trimmed = "  hello  ".trimOws()
 * // trimmed == "hello"
 * ```
 */
fun CharSequence.trimOws(): CharSequence = StringUtil.trimOws(this)

/**
 * Netty 처리에서 `join` 함수를 제공합니다.
 *
 * ```kotlin
 * val joined = listOf("a", "b", "c").join()
 * // joined == "a,b,c"
 * ```
 */
fun <T : CharSequence> Iterable<T>.join(separator: CharSequence = ","): CharSequence = StringUtil.join(separator, this)
