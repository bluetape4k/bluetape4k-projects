package io.bluetape4k.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.util.AsciiString
import java.nio.CharBuffer
import java.nio.charset.Charset

fun ByteBuf.ensureAccessible(): ByteBuf =
    ByteBufUtil.ensureAccessible(this)

fun ByteBuf.hexDump(
    fromIndex: Int = readerIndex(),
    length: Int = readableBytes(),
): String =
    ByteBufUtil.hexDump(this, fromIndex, length)

fun ByteArray.hexDump(
    fromIndex: Int = 0,
    length: Int = size,
): String =
    ByteBufUtil.hexDump(this, fromIndex, length)

fun CharSequence.decodeHexByte(pos: Int): Byte =
    ByteBufUtil.decodeHexByte(this, pos)

fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray =
    ByteBufUtil.decodeHexDump(this, fromIndex, length)

fun ByteBuf.indexOf(haystack: ByteBuf): Int =
    ByteBufUtil.indexOf(this, haystack)

fun ByteBuf.equals(
    thisIndex: Int,
    other: ByteBuf,
    otherStartIndex: Int,
    length: Int,
): Boolean =
    ByteBufUtil.equals(this, thisIndex, other, otherStartIndex, length)

fun ByteBuf.equalsEx(other: ByteBuf): Boolean =
    ByteBufUtil.equals(this, other)

fun ByteBuf.compare(other: ByteBuf): Int =
    ByteBufUtil.compare(this, other)

fun Short.swap(): Short = java.lang.Short.reverseBytes(this)
fun Int.swap(): Int = Integer.reverseBytes(this)
fun Int.swapMedium(): Int = ByteBufUtil.swapMedium(this)
fun Long.swap(): Long = java.lang.Long.reverseBytes(this)

fun ByteBuf.writeShortBE(shortValue: Int): ByteBuf =
    ByteBufUtil.writeShortBE(this, shortValue)

fun ByteBuf.setShortBE(index: Int, shortValue: Int): ByteBuf =
    ByteBufUtil.setShortBE(this, index, shortValue)

fun ByteBuf.writeMediumBE(mediumValue: Int): ByteBuf =
    ByteBufUtil.writeMediumBE(this, mediumValue)

/**
 * Read the given amount of bytes into a new {@link ByteBuf} that is allocated from the {@link ByteBufAllocator}.
 */
fun ByteBufAllocator.readBytes(srcBuffer: ByteBuf, length: Int): ByteBuf =
    ByteBufUtil.readBytes(this, srcBuffer, length)

fun ByteBufAllocator.writeUtf8(seq: CharSequence): ByteBuf =
    ByteBufUtil.writeUtf8(this, seq)

fun ByteBuf.writeUtf8(seq: CharSequence, start: Int, end: Int): Int =
    ByteBufUtil.writeUtf8(this, seq, start, end)

fun ByteBuf.reserveAndWriteUtf8(
    seq: CharSequence,
    reserveBytes: Int,
    start: Int = 0,
    end: Int = seq.length,
): Int =
    ByteBufUtil.reserveAndWriteUtf8(this, seq, start, end, reserveBytes)

fun CharSequence.utf8Bytes(
    start: Int = 0,
    end: Int = this.length,
): Int =
    ByteBufUtil.utf8Bytes(this, start, end)

fun ByteBufAllocator.writeAscii(seq: CharSequence): ByteBuf =
    ByteBufUtil.writeAscii(this, seq)

fun ByteBuf.writeAscii(seq: CharSequence): Int =
    ByteBufUtil.writeAscii(this, seq)

fun ByteBufAllocator.encodeString(
    src: CharBuffer,
    charset: Charset = Charsets.UTF_8,
    extraCapacity: Int = 0,
): ByteBuf =
    ByteBufUtil.encodeString(this, src, charset, extraCapacity)

fun threadLocalDirectBufferOf(): ByteBuf =
    ByteBufUtil.threadLocalDirectBuffer()

fun AsciiString.copyTo(
    srcIndex: Int = 0,
    dst: ByteBuf,
    dstIndex: Int = dst.writerIndex(),
    length: Int = this.length,
) {
    ByteBufUtil.copy(this, srcIndex, dst, dstIndex, length)
}

fun ByteBuf.prettyHexDump(
    offst: Int = readerIndex(),
    length: Int = readableBytes(),
): String =
    ByteBufUtil.prettyHexDump(this, offst, length)

fun ByteBuf.appendPrettyHexDumpTo(dump: StringBuilder) {
    ByteBufUtil.appendPrettyHexDump(dump, this)
}

fun ByteBuf.isText(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
    charset: Charset = Charsets.UTF_8,
): Boolean =
    ByteBufUtil.isText(this, index, length, charset)

fun ByteBuf.isUtf8(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean =
    ByteBufUtil.isText(this, index, length, Charsets.UTF_8)

fun ByteBuf.isAscii(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean =
    ByteBufUtil.isText(this, index, length, Charsets.US_ASCII)
