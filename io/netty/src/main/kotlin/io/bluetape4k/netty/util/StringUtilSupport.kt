package io.bluetape4k.netty.util

import io.netty.util.internal.StringUtil

/**
 * Netty 처리에서 `byteToHexStringPadded` 함수를 제공합니다.
 */
fun <T: Appendable> T.byteToHexStringPadded(value: Int): T =
    StringUtil.byteToHexStringPadded(this, value)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringPadded` 함수를 제공합니다.
 */
fun ByteArray.toHexStringPadded(offset: Int = 0, length: Int = size): String =
    StringUtil.toHexStringPadded(this, offset, length)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringPaddedAs` 함수를 제공합니다.
 */
fun <T: Appendable> ByteArray.toHexStringPaddedAs(dest: T, offset: Int = 0, length: Int = size): T =
    StringUtil.toHexStringPadded(dest, this, offset, length)

/**
 * Netty 처리에서 `byteToHexString` 함수를 제공합니다.
 */
fun <T: Appendable> T.byteToHexString(value: Int): T =
    StringUtil.byteToHexStringPadded(this, value)

/**
 * Netty 처리 타입 변환을 위한 `toHexString` 함수를 제공합니다.
 */
fun ByteArray.toHexString(offset: Int = 0, length: Int = size): String =
    StringUtil.toHexStringPadded(this, offset, length)

/**
 * Netty 처리 타입 변환을 위한 `toHexStringAs` 함수를 제공합니다.
 */
fun <T: Appendable> ByteArray.toHexStringAs(dest: T, offset: Int = 0, length: Int = size): T =
    StringUtil.toHexStringPadded(dest, this, offset, length)

/**
 * Netty 처리에서 `decodeHexNibble` 함수를 제공합니다.
 */
fun Char.decodeHexNibble(): Int = StringUtil.decodeHexNibble(this)

/**
 * Netty 처리에서 `decodeHexByte` 함수를 제공합니다.
 */
fun CharSequence.decodeHexByte(pos: Int): Byte = StringUtil.decodeHexByte(this, pos)

/**
 * Netty 처리에서 `decodeHexDump` 함수를 제공합니다.
 */
fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray =
    StringUtil.decodeHexDump(this, fromIndex, length)

/**
 * [Class.getSimpleName]과 유사하지만, anonymous class 에서도 잘 작동한다
 */
val Class<*>.simpleClassName: String get() = StringUtil.simpleClassName(this)

/**
 * Netty 처리에서 `escapeCsv` 함수를 제공합니다.
 */
fun CharSequence.escapeCsv(trimWhiteSpace: Boolean = false): CharSequence =
    StringUtil.escapeCsv(this, trimWhiteSpace)

/**
 * Netty 처리에서 `unescapeCsv` 함수를 제공합니다.
 */
fun CharSequence.unescapeCsv(): CharSequence = StringUtil.unescapeCsv(this)

/**
 * Netty 처리에서 `unescapeCsvFields` 함수를 제공합니다.
 */
fun CharSequence.unescapeCsvFields(): List<CharSequence> = StringUtil.unescapeCsvFields(this)

/**
 * Netty 처리에서 `indexOfNonWhiteSpace` 함수를 제공합니다.
 */
fun CharSequence.indexOfNonWhiteSpace(offset: Int = 0): Int =
    StringUtil.indexOfNonWhiteSpace(this, offset)

/**
 * Netty 처리에서 `indexOfWhiteSpace` 함수를 제공합니다.
 */
fun CharSequence.indexOfWhiteSpace(offset: Int = 0): Int =
    StringUtil.indexOfWhiteSpace(this, offset)

val Char.isSurrogate: Boolean get() = StringUtil.isSurrogate(this)
val Char.isDoubleQuote: Boolean get() = StringUtil.DOUBLE_QUOTE == this

/**
 * Netty 처리에서 `trimOws` 함수를 제공합니다.
 */
fun CharSequence.trimOws(): CharSequence = StringUtil.trimOws(this)

/**
 * Netty 처리에서 `join` 함수를 제공합니다.
 */
fun <T: CharSequence> Iterable<T>.join(separator: CharSequence = ","): CharSequence =
    StringUtil.join(separator, this)
