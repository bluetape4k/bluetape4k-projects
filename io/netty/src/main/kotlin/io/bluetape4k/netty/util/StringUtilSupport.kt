package io.bluetape4k.netty.util

import io.netty.util.internal.StringUtil

fun <T: Appendable> T.byteToHexStringPadded(value: Int): T =
    StringUtil.byteToHexStringPadded(this, value)

fun ByteArray.toHexStringPadded(offset: Int = 0, length: Int = size): String =
    StringUtil.toHexStringPadded(this, offset, length)

fun <T: Appendable> ByteArray.toHexStringPaddedAs(dest: T, offset: Int = 0, length: Int = size): T =
    StringUtil.toHexStringPadded(dest, this, offset, length)

fun <T: Appendable> T.byteToHexString(value: Int): T =
    StringUtil.byteToHexStringPadded(this, value)

fun ByteArray.toHexString(offset: Int = 0, length: Int = size): String =
    StringUtil.toHexStringPadded(this, offset, length)

fun <T: Appendable> ByteArray.toHexStringAs(dest: T, offset: Int = 0, length: Int = size): T =
    StringUtil.toHexStringPadded(dest, this, offset, length)

fun Char.decodeHexNibble(): Int = StringUtil.decodeHexNibble(this)
fun CharSequence.decodeHexByte(pos: Int): Byte = StringUtil.decodeHexByte(this, pos)
fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray =
    StringUtil.decodeHexDump(this, fromIndex, length)

/**
 * [Class.getSimpleName]과 유사하지만, anonymous class 에서도 잘 작동한다
 */
val Class<*>.simpleClassName: String get() = StringUtil.simpleClassName(this)

fun CharSequence.escapeCsv(trimWhiteSpace: Boolean = false): CharSequence =
    StringUtil.escapeCsv(this, trimWhiteSpace)

fun CharSequence.unescapeCsv(): CharSequence = StringUtil.unescapeCsv(this)
fun CharSequence.unescapeCsvFields(): List<CharSequence> = StringUtil.unescapeCsvFields(this)

fun CharSequence.indexOfNonWhiteSpace(offset: Int = 0): Int =
    StringUtil.indexOfNonWhiteSpace(this, offset)

fun CharSequence.indexOfWhiteSpace(offset: Int = 0): Int =
    StringUtil.indexOfWhiteSpace(this, offset)

val Char.isSurrogate: Boolean get() = StringUtil.isSurrogate(this)
val Char.isDoubleQuote: Boolean get() = StringUtil.DOUBLE_QUOTE == this

fun CharSequence.trimOws(): CharSequence = StringUtil.trimOws(this)

fun <T: CharSequence> Iterable<T>.join(separator: CharSequence = ","): CharSequence =
    StringUtil.join(separator, this)
