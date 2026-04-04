package io.bluetape4k.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import io.netty.util.AsciiString
import java.nio.CharBuffer
import java.nio.charset.Charset

/**
 * Netty 처리에서 `ensureAccessible` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer(4)
 * val accessible = buf.ensureAccessible()
 * // accessible === buf
 * ```
 */
fun ByteBuf.ensureAccessible(): ByteBuf = ByteBufUtil.ensureAccessible(this)

/**
 * Netty 처리에서 `hexDump` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.wrappedBuffer(byteArrayOf(0xAB.toByte(), 0xCD.toByte()))
 * val hex = buf.hexDump()
 * // hex == "abcd"
 * ```
 */
fun ByteBuf.hexDump(
    fromIndex: Int = readerIndex(),
    length: Int = readableBytes(),
): String = ByteBufUtil.hexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `hexDump` 함수를 제공합니다.
 *
 * ```kotlin
 * val bytes = byteArrayOf(0xAB.toByte(), 0xCD.toByte())
 * val hex = bytes.hexDump()
 * // hex == "abcd"
 * ```
 */
fun ByteArray.hexDump(
    fromIndex: Int = 0,
    length: Int = size,
): String = ByteBufUtil.hexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `decodeHexByte` 함수를 제공합니다.
 *
 * ```kotlin
 * val hex = "abcd"
 * val byte = hex.decodeHexByte(0)
 * // byte == 0xAB.toByte()
 * ```
 */
fun CharSequence.decodeHexByte(pos: Int): Byte = ByteBufUtil.decodeHexByte(this, pos)

/**
 * Netty 처리에서 `decodeHexDump` 함수를 제공합니다.
 *
 * ```kotlin
 * val hex = "abcd"
 * val bytes = hex.decodeHexDump()
 * // bytes.toList() == listOf<Byte>(0xAB.toByte(), 0xCD.toByte())
 * ```
 */
fun CharSequence.decodeHexDump(
    fromIndex: Int = 0,
    length: Int = this.length,
): ByteArray = ByteBufUtil.decodeHexDump(this, fromIndex, length)

/**
 * Netty 처리에서 `indexOf` 함수를 제공합니다.
 *
 * ```kotlin
 * val haystack = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3, 4))
 * val needle = Unpooled.wrappedBuffer(byteArrayOf(2, 3))
 * val index = haystack.indexOf(needle)
 * // index == 1
 * ```
 */
fun ByteBuf.indexOf(haystack: ByteBuf): Int = ByteBufUtil.indexOf(this, haystack)

/**
 * Netty 처리에서 `equals` 함수를 제공합니다.
 *
 * ```kotlin
 * val a = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
 * val b = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
 * val equal = a.equals(0, b, 0, 3)
 * // equal == true
 * ```
 */
fun ByteBuf.equals(
    thisIndex: Int,
    other: ByteBuf,
    otherStartIndex: Int,
    length: Int,
): Boolean = ByteBufUtil.equals(this, thisIndex, other, otherStartIndex, length)

/**
 * Netty 처리에서 `equalsEx` 함수를 제공합니다.
 *
 * ```kotlin
 * val a = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
 * val b = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))
 * val equal = a.equalsEx(b)
 * // equal == true
 * ```
 */
fun ByteBuf.equalsEx(other: ByteBuf): Boolean = ByteBufUtil.equals(this, other)

/**
 * Netty 처리에서 `compare` 함수를 제공합니다.
 *
 * ```kotlin
 * val a = Unpooled.wrappedBuffer(byteArrayOf(1, 2))
 * val b = Unpooled.wrappedBuffer(byteArrayOf(1, 3))
 * val cmp = a.compare(b)
 * // cmp < 0
 * ```
 */
fun ByteBuf.compare(other: ByteBuf): Int = ByteBufUtil.compare(this, other)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 *
 * ```kotlin
 * val swapped = 0x0102.toShort().swap()
 * // swapped == 0x0201.toShort()
 * ```
 */
fun Short.swap(): Short = java.lang.Short.reverseBytes(this)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 *
 * ```kotlin
 * val swapped = 0x01020304.swap()
 * // swapped == 0x04030201
 * ```
 */
fun Int.swap(): Int = Integer.reverseBytes(this)

/**
 * Netty 처리에서 `swapMedium` 함수를 제공합니다.
 *
 * ```kotlin
 * val swapped = 0x010203.swapMedium()
 * // swapped == 0x030201
 * ```
 */
fun Int.swapMedium(): Int = ByteBufUtil.swapMedium(this)

/**
 * Netty 처리에서 `swap` 함수를 제공합니다.
 *
 * ```kotlin
 * val swapped = 0x0102030405060708L.swap()
 * // swapped == 0x0807060504030201L
 * ```
 */
fun Long.swap(): Long = java.lang.Long.reverseBytes(this)

/**
 * Netty 처리에서 데이터를 기록하는 `writeShortBE` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer()
 * buf.writeShortBE(0x0102)
 * val b0 = buf.readByte()
 * // b0 == 0x01.toByte()
 * ```
 */
fun ByteBuf.writeShortBE(shortValue: Int): ByteBuf = ByteBufUtil.writeShortBE(this, shortValue)

/**
 * Netty 처리에서 `setShortBE` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer(4)
 * buf.writeZero(4)
 * buf.setShortBE(0, 0x0102)
 * val b0 = buf.getByte(0)
 * // b0 == 0x01.toByte()
 * ```
 */
fun ByteBuf.setShortBE(
    index: Int,
    shortValue: Int,
): ByteBuf = ByteBufUtil.setShortBE(this, index, shortValue)

/**
 * Netty 처리에서 데이터를 기록하는 `writeMediumBE` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer()
 * buf.writeMediumBE(0x010203)
 * val b0 = buf.readByte()
 * // b0 == 0x01.toByte()
 * ```
 */
fun ByteBuf.writeMediumBE(mediumValue: Int): ByteBuf = ByteBufUtil.writeMediumBE(this, mediumValue)

/**
 * [ByteBufAllocator]에서 할당된 새로운 [ByteBuf]에 지정한 바이트 수만큼 읽어옵니다.
 *
 * @param srcBuffer 읽을 원본 [ByteBuf]
 * @param length 읽을 바이트 수
 * @return 읽은 데이터가 담긴 새 [ByteBuf]
 */
fun ByteBufAllocator.readBytes(
    srcBuffer: ByteBuf,
    length: Int,
): ByteBuf = ByteBufUtil.readBytes(this, srcBuffer, length)

/**
 * Netty 처리에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = ByteBufAllocator.DEFAULT.writeUtf8("hello")
 * // buf.readableBytes() == 5
 * ```
 */
fun ByteBufAllocator.writeUtf8(seq: CharSequence): ByteBuf = ByteBufUtil.writeUtf8(this, seq)

/**
 * Netty 처리에서 데이터를 기록하는 `writeUtf8` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer()
 * val written = buf.writeUtf8("hello", 0, 5)
 * // written == 5
 * ```
 */
fun ByteBuf.writeUtf8(
    seq: CharSequence,
    start: Int,
    end: Int,
): Int = ByteBufUtil.writeUtf8(this, seq, start, end)

/**
 * Netty 처리에서 `reserveAndWriteUtf8` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer()
 * val written = buf.reserveAndWriteUtf8("hello", reserveBytes = 16)
 * // written == 5
 * ```
 */
fun ByteBuf.reserveAndWriteUtf8(
    seq: CharSequence,
    reserveBytes: Int,
    start: Int = 0,
    end: Int = seq.length,
): Int = ByteBufUtil.reserveAndWriteUtf8(this, seq, start, end, reserveBytes)

/**
 * Netty 처리에서 `utf8Bytes` 함수를 제공합니다.
 *
 * ```kotlin
 * val byteCount = "hello".utf8Bytes()
 * // byteCount == 5
 * ```
 */
fun CharSequence.utf8Bytes(
    start: Int = 0,
    end: Int = this.length,
): Int = ByteBufUtil.utf8Bytes(this, start, end)

/**
 * Netty 처리에서 데이터를 기록하는 `writeAscii` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = ByteBufAllocator.DEFAULT.writeAscii("hello")
 * // buf.readableBytes() == 5
 * ```
 */
fun ByteBufAllocator.writeAscii(seq: CharSequence): ByteBuf = ByteBufUtil.writeAscii(this, seq)

/**
 * Netty 처리에서 데이터를 기록하는 `writeAscii` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.buffer()
 * val written = buf.writeAscii("hello")
 * // written == 5
 * ```
 */
fun ByteBuf.writeAscii(seq: CharSequence): Int = ByteBufUtil.writeAscii(this, seq)

/**
 * Netty 처리에서 `encodeString` 함수를 제공합니다.
 *
 * ```kotlin
 * val charBuf = CharBuffer.wrap("hello")
 * val buf = ByteBufAllocator.DEFAULT.encodeString(charBuf)
 * // buf.readableBytes() == 5
 * ```
 */
fun ByteBufAllocator.encodeString(
    src: CharBuffer,
    charset: Charset = Charsets.UTF_8,
    extraCapacity: Int = 0,
): ByteBuf = ByteBufUtil.encodeString(this, src, charset, extraCapacity)

/**
 * Netty 처리에서 `threadLocalDirectBufferOf` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = threadLocalDirectBufferOf()
 * // buf != null (thread-local direct buffer or null if not available)
 * ```
 */
fun threadLocalDirectBufferOf(): ByteBuf = ByteBufUtil.threadLocalDirectBuffer()

/**
 * Netty 처리에서 `copyTo` 함수를 제공합니다.
 *
 * ```kotlin
 * val ascii = AsciiString("hello")
 * val dst = Unpooled.buffer(5)
 * ascii.copyTo(dst = dst, length = 5)
 * // dst.readableBytes() == 5
 * ```
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
 *
 * ```kotlin
 * val buf = Unpooled.wrappedBuffer(byteArrayOf(0xAB.toByte(), 0xCD.toByte()))
 * val dump = buf.prettyHexDump()
 * // dump contains "abcd"
 * ```
 */
fun ByteBuf.prettyHexDump(
    offst: Int = readerIndex(),
    length: Int = readableBytes(),
): String = ByteBufUtil.prettyHexDump(this, offst, length)

/**
 * Netty 처리에서 `appendPrettyHexDumpTo` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.wrappedBuffer(byteArrayOf(0xAB.toByte()))
 * val sb = StringBuilder()
 * buf.appendPrettyHexDumpTo(sb)
 * // sb.toString() contains "ab"
 * ```
 */
fun ByteBuf.appendPrettyHexDumpTo(dump: StringBuilder) {
    ByteBufUtil.appendPrettyHexDump(dump, this)
}

/**
 * Netty 처리에서 `isText` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.copiedBuffer("hello", Charsets.UTF_8)
 * val text = buf.isText()
 * // text == true
 * ```
 */
fun ByteBuf.isText(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
    charset: Charset = Charsets.UTF_8,
): Boolean = ByteBufUtil.isText(this, index, length, charset)

/**
 * Netty 처리에서 `isUtf8` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.copiedBuffer("hello", Charsets.UTF_8)
 * val utf8 = buf.isUtf8()
 * // utf8 == true
 * ```
 */
fun ByteBuf.isUtf8(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean = ByteBufUtil.isText(this, index, length, Charsets.UTF_8)

/**
 * Netty 처리에서 `isAscii` 함수를 제공합니다.
 *
 * ```kotlin
 * val buf = Unpooled.copiedBuffer("hello", Charsets.US_ASCII)
 * val ascii = buf.isAscii()
 * // ascii == true
 * ```
 */
fun ByteBuf.isAscii(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): Boolean = ByteBufUtil.isText(this, index, length, Charsets.US_ASCII)
