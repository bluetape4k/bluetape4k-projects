package io.bluetape4k.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.util.AsciiString
import java.nio.CharBuffer
import java.nio.charset.Charset

/**
 * Netty 처리에서 `ensureAccessible` 함수를 제공합니다.
 */
fun ByteBuf.ensureAccessible(): ByteBuf =
    ByteBufUtil.ensureAccessible(this)

/**
 * Netty 처리에서 `hexDump` 함수를 제공합니다.
 */
fun ByteBuf.hexDump(
    fromIndex: Int = readerIndex(),
    length: Int = readableBytes(),
): String =
    ByteBufUtil.hexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `hexDump` 함수를 제공합니다.
 */
fun ByteArray.hexDump(
    fromIndex: Int = 0,
    length: Int = size,
): String =
    ByteBufUtil.hexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `decodeHexByte` 함수를 제공합니다.
 */
fun CharSequence.decodeHexByte(pos: Int): Byte =
    ByteBufUtil.decodeHexByte(this, pos)

/**
 * Netty 처리에서 `decodeHexDump` 함수를 제공합니다.
 */
fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray =
    ByteBufUtil.decodeHexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `indexOf` 함수를 제공합니다.
 */
fun ByteBuf.indexOf(haystack: ByteBuf): Int =
    ByteBufUtil.indexOf(this, haystack)

/**
 * Netty 처리에서 `equals` 함수를 제공합니다.
 */
fun ByteBuf.equals(
    thisIndex: Int,
    other: ByteBuf,
    otherStartIndex: Int,
    length: Int,
): Boolean =
    ByteBufUtil.equals(this, thisIndex, other, otherStartIndex, length)

/**
 * Netty 처리에서 `equalsEx` 함수를 제공합니다.
 */
fun ByteBuf.equalsEx(other: ByteBuf): Boolean =
    ByteBufUtil.equals(this, other)

/**
 * Netty 처리에서 `compare` 함수를 제공합니다.
 */
fun ByteBuf.compare(other: ByteBuf): Int =
    ByteBufUtil.compare(this, other)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 */
fun Short.swap(): Short = java.lang.Short.reverseBytes(this)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 */
fun Int.swap(): Int = Integer.reverseBytes(this)

/**
 * Netty 처리에서 `swapMedium` 함수를 제공합니다.
 */
fun Int.swapMedium(): Int = ByteBufUtil.swapMedium(this)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 */
fun Long.swap(): Long = java.lang.Long.reverseBytes(this)

/**
 * Netty 처리에서 데이터를 기록하는 `writeShortBE` 함수를 제공합니다.
 */
fun ByteBuf.writeShortBE(shortValue: Int): ByteBuf =
    ByteBufUtil.writeShortBE(this, shortValue)

/**
 * Netty 처리에서 `setShortBE` 함수를 제공합니다.
 */
fun ByteBuf.setShortBE(index: Int, shortValue: Int): ByteBuf =
    ByteBufUtil.setShortBE(this, index, shortValue)

/**
 * Netty 처리에서 데이터를 기록하는 `writeMediumBE` 함수를 제공합니다.
 */
fun ByteBuf.writeMediumBE(mediumValue: Int): ByteBuf =
    ByteBufUtil.writeMediumBE(this, mediumValue)

/**
 * Read the given amount of bytes into a new {@link ByteBuf} that is allocated from the {@link ByteBufAllocator}.
 */
fun ByteBufAllocator.readBytes(srcBuffer: ByteBuf, length: Int): ByteBuf =
    ByteBufUtil.readBytes(this, srcBuffer, length)

/**
 * Netty 처리에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
 */
fun ByteBufAllocator.writeUtf8(seq: CharSequence): ByteBuf =
    ByteBufUtil.writeUtf8(this, seq)

/**
 * Netty 처리에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
 */
fun ByteBuf.writeUtf8(seq: CharSequence, start: Int, end: Int): Int =
    ByteBufUtil.writeUtf8(this, seq, start, end)

/**
 * Netty 처리에서 `reserveAndWriteUtf8` 함수를 제공합니다.
 */
fun ByteBuf.reserveAndWriteUtf8(
    seq: CharSequence,
    reserveBytes: Int,
    start: Int = 0,
    end: Int = seq.length,
): Int =
    ByteBufUtil.reserveAndWriteUtf8(this, seq, start, end, reserveBytes)

/**
 * Netty 처리에서 `utf8Bytes` 함수를 제공합니다.
 */
fun CharSequence.utf8Bytes(
    start: Int = 0,
    end: Int = this.length,
): Int =
    ByteBufUtil.utf8Bytes(this, start, end)

/**
 * Netty 처리에서 데이터를 기록하는 `writeAscii` 함수를 제공합니다.
 */
fun ByteBufAllocator.writeAscii(seq: CharSequence): ByteBuf =
    ByteBufUtil.writeAscii(this, seq)

/**
 * Netty 처리에서 데이터를 기록하는 `writeAscii` 함수를 제공합니다.
 */
fun ByteBuf.writeAscii(seq: CharSequence): Int =
    ByteBufUtil.writeAscii(this, seq)

/**
 * Netty 처리에서 `encodeString` 함수를 제공합니다.
 */
fun ByteBufAllocator.encodeString(
    src: CharBuffer,
    charset: Charset = Charsets.UTF_8,
    extraCapacity: Int = 0,
): ByteBuf =
    ByteBufUtil.encodeString(this, src, charset, extraCapacity)

/**
 * Netty 처리에서 `threadLocalDirectBufferOf` 함수를 제공합니다.
 */
fun threadLocalDirectBufferOf(): ByteBuf =
    ByteBufUtil.threadLocalDirectBuffer()

/**
 * Netty 처리에서 `copyTo` 함수를 제공합니다.
 */
fun AsciiString.copyTo(
    srcIndex: Int = 0,
    dst: ByteBuf,
    dstIndex: Int = dst.writerIndex(),
    length: Int = this.length,
) {
    ByteBufUtil.copy(this, srcIndex, dst, dstIndex, length)
}

/**
 * Netty 처리에서 `prettyHexDump` 함수를 제공합니다.
 */
fun ByteBuf.prettyHexDump(
    offst: Int = readerIndex(),
    length: Int = readableBytes(),
): String =
    ByteBufUtil.prettyHexDump(this, offst, length)

/**
 * Netty 처리에서 `appendPrettyHexDumpTo` 함수를 제공합니다.
 */
fun ByteBuf.appendPrettyHexDumpTo(dump: StringBuilder) {
    ByteBufUtil.appendPrettyHexDump(dump, this)
}

/**
 * Netty 처리에서 `isText` 함수를 제공합니다.
 */
fun ByteBuf.isText(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
    charset: Charset = Charsets.UTF_8,
): Boolean =
    ByteBufUtil.isText(this, index, length, charset)

/**
 * Netty 처리에서 `isUtf8` 함수를 제공합니다.
 */
fun ByteBuf.isUtf8(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean =
    ByteBufUtil.isText(this, index, length, Charsets.UTF_8)

/**
 * Netty 처리에서 `isAscii` 함수를 제공합니다.
 */
fun ByteBuf.isAscii(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean =
    ByteBufUtil.isText(this, index, length, Charsets.US_ASCII)
