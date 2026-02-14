package io.bluetape4k.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.util.ByteProcessor
import java.io.IOException
import java.nio.charset.Charset

internal const val HALF_BYTE = 128

/**
 * Netty 처리에서 `getByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.getByteNeg(index: Int): Byte = (-getByte(index)).toByte()

/**
 * Netty 처리에서 `getByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.getByteAdd(index: Int): Byte = (getByte(index) - HALF_BYTE).toByte()

/**
 * Netty 처리에서 `getByteSub` 함수를 제공합니다.
 */
fun ByteBuf.getByteSub(index: Int): Byte = (HALF_BYTE - getByte(index)).toByte()

/**
 * Netty 처리에서 `getUByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.getUByteNeg(index: Int): UByte = (-getByte(index) and 0xFF).toUByte()

/**
 * Netty 처리에서 `getUByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.getUByteAdd(index: Int): UByte = (getByte(index) - HALF_BYTE).toUByte()

/**
 * Netty 처리에서 `getUByteSub` 함수를 제공합니다.
 */
fun ByteBuf.getUByteSub(index: Int): UByte = (HALF_BYTE - getByte(index)).toUByte()

/**
 * Netty 처리에서 `getShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.getShortAdd(index: Int): Short =
    ((getByte(index).toInt() shl Byte.SIZE_BITS) or
            ((getByte(index + 1) - HALF_BYTE) and 0xFF)).toShort()

/**
 * Netty 처리에서 `getShortLEAdd` 함수를 제공합니다.
 */
fun ByteBuf.getShortLEAdd(index: Int): Short =
    (((getByte(index) - HALF_BYTE) and 0xFF) or
            (getByte(index + 1).toInt() shl Byte.SIZE_BITS)).toShort()

/**
 * Netty 처리에서 `getUShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.getUShortAdd(index: Int): UShort =
    ((getUnsignedByte(index).toInt() shl Byte.SIZE_BITS) or
            ((getByte(index + 1) - HALF_BYTE) and 0xFF)).toUShort()

/**
 * Netty 처리에서 `getUShortLEAdd` 함수를 제공합니다.
 */
fun ByteBuf.getUShortLEAdd(index: Int): UShort =
    (((getByte(index) - HALF_BYTE) and 0xFF) or
            (getUnsignedByte(index + 1).toInt() shl Byte.SIZE_BITS)).toUShort()

/**
 * Netty 처리에서 `getMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.getMediumLME(index: Int): Int =
    (getShortLE(index).toInt() shl Byte.SIZE_BITS) or
            getUnsignedByte(index + Short.SIZE_BYTES).toInt()

/**
 * Netty 처리에서 `getMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.getMediumRME(index: Int): Int =
    (getByte(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Byte.SIZE_BYTES)

/**
 * Netty 처리에서 `getUMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.getUMediumLME(index: Int): Int =
    (getUnsignedShortLE(index) shl Byte.SIZE_BITS) or
            getUnsignedByte(index + Short.SIZE_BYTES).toInt()

/**
 * Netty 처리에서 `getUMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.getUMediumRME(index: Int): Int =
    (getUnsignedByte(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Byte.SIZE_BYTES)

/**
 * Netty 처리에서 `getIntME` 함수를 제공합니다.
 */
fun ByteBuf.getIntME(index: Int): Int =
    (getShortLE(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Short.SIZE_BYTES)

/**
 * Netty 처리에서 `getIntIME` 함수를 제공합니다.
 */
fun ByteBuf.getIntIME(index: Int): Int =
    getUnsignedShort(index) or
            (getShort(index + Short.SIZE_BYTES).toInt() shl Short.SIZE_BITS)

/**
 * Netty 처리에서 `getUIntME` 함수를 제공합니다.
 */
fun ByteBuf.getUIntME(index: Int): Long =
    (getUnsignedShortLE(index).toLong() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Short.SIZE_BYTES).toLong()

/**
 * Netty 처리에서 `getUIntIME` 함수를 제공합니다.
 */
fun ByteBuf.getUIntIME(index: Int): Long =
    getUnsignedShort(index).toLong() or
            (getUnsignedShort(index + Short.SIZE_BYTES).toLong() shl Short.SIZE_BITS)

/**
 * Netty 처리에서 `getSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.getSmallLong(index: Int): Long =
    (getMedium(index).toLong() shl Medium.SIZE_BITS) or
            getUnsignedMedium(index + Medium.SIZE_BYTES).toLong()

/**
 * Netty 처리에서 `getUSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.getUSmallLong(index: Int): Long =
    (getUnsignedMedium(index).toLong() shl Medium.SIZE_BITS) or
            getUnsignedMedium(index + Medium.SIZE_BYTES).toLong()

/**
 * Netty 처리에서 `getBytes` 함수를 제공합니다.
 */
fun ByteBuf.getBytes(
    start: Int = readerIndex(),
    length: Int = readableBytes(),
    copy: Boolean = true,
): ByteArray =
    ByteBufUtil.getBytes(this, start, length, copy)

/**
 * Netty 처리에서 `getBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.getBytesReversed(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): ByteArray =
    ByteArray(length).apply {
        getBytesReversed(index, this)
    }

/**
 * Netty 처리에서 `getBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.getBytesReversed(index: Int, dest: ByteArray) = apply {
    val endReaderIndex = index + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo index).withIndex()) {
        dest[writerIndex] = getByte(readerIndex)
    }
}

/**
 * Netty 처리에서 `getBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.getBytesAdd(index: Int, length: Int): ByteArray =
    ByteArray(length).apply {
        getBytesAdd(index, this)
    }

/**
 * Netty 처리에서 `getBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.getBytesAdd(index: Int, dest: ByteArray) = apply {
    for (writerIndex in dest.indices) {
        dest[writerIndex] = getByteAdd(index + writerIndex)
    }
}

/**
 * Netty 처리에서 `getBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.getBytesReversedAdd(index: Int, length: Int): ByteArray =
    ByteArray(length).apply {
        getBytesReversedAdd(index, this)
    }

/**
 * Netty 처리에서 `getBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.getBytesReversedAdd(index: Int, dest: ByteArray) = apply {
    val endReaderIndex = index + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo index).withIndex()) {
        dest[writerIndex] = getByteAdd(readerIndex)
    }
}

/**
 * Netty 처리에서 `setByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.setByteNeg(index: Int, value: Int): ByteBuf = setByte(index, -value)

/**
 * Netty 처리에서 `setByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.setByteAdd(index: Int, value: Int): ByteBuf = setByte(index, value + HALF_BYTE)

/**
 * Netty 처리에서 `setByteSub` 함수를 제공합니다.
 */
fun ByteBuf.setByteSub(index: Int, value: Int): ByteBuf = setByte(index, HALF_BYTE - value)

/**
 * Netty 처리에서 `setShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.setShortAdd(index: Int, value: Int): ByteBuf = apply {
    setByte(index, value shr Byte.SIZE_BITS)
    setByte(index + Byte.SIZE_BITS, value + HALF_BYTE)
}

/**
 * Netty 처리에서 `setMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.setMediumLME(index: Int, value: Int): ByteBuf = apply {
    setShortLE(index, value shr Byte.SIZE_BITS)
    setByte(index + Short.SIZE_BYTES, value)
}

/**
 * Netty 처리에서 `setMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.setMediumRME(index: Int, value: Int): ByteBuf = apply {
    setByte(index, value shr Short.SIZE_BITS)
    setShortLE(index + Byte.SIZE_BYTES, value)
}

/**
 * Netty 처리에서 `setIntME` 함수를 제공합니다.
 */
fun ByteBuf.setIntME(index: Int, value: Int): ByteBuf = apply {
    setShortLE(index, value shr Short.SIZE_BITS)
    setShortLE(index + Short.SIZE_BYTES, value)
}

/**
 * Netty 처리에서 `setIntIME` 함수를 제공합니다.
 */
fun ByteBuf.setIntIME(index: Int, value: Int): ByteBuf = apply {
    setShort(index, value)
    setShort(index + Short.SIZE_BYTES, value shr Short.SIZE_BITS)
}

/**
 * Netty 처리에서 `setSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.setSmallLong(index: Int, value: Long): ByteBuf = apply {
    setMedium(index, (value shr Medium.SIZE_BITS).toInt())
    setMedium(index + Medium.SIZE_BYTES, value.toInt())
}

/**
 * Netty 처리에서 `setBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.setBytesReversed(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.reversedArray())
}

/**
 * Netty 처리에서 `setBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.setBytesReversed(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        setByte(j++, src.getByte(i).toInt())
    }
}

/**
 * Netty 처리에서 `setBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.setBytesAdd(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.map { (it + HALF_BYTE).toByte() }.toByteArray())
}

/**
 * Netty 처리에서 `setBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.setBytesAdd(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.readerIndex() until src.writerIndex()) {
        setByte(j++, src.getByte(i) + HALF_BYTE)
    }
}

/**
 * Netty 처리에서 `setBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.setBytesReversedAdd(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.map { (it + HALF_BYTE).toByte() }.reversed().toByteArray())
}

/**
 * Netty 처리에서 `setBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.setBytesReversedAdd(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        setByte(j++, src.getByte(i) + HALF_BYTE)
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.readByteNeg(): Byte = (-readByte()).toByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.readByteAdd(): Byte = (readByte() - HALF_BYTE).toByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readByteSub` 함수를 제공합니다.
 */
fun ByteBuf.readByteSub(): Byte = (HALF_BYTE - readByte()).toByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.readUByteNeg(): UByte = (-readByte() and 0xFF).toUByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.readUByteAdd(): UByte = ((readByte() - HALF_BYTE) and 0xFF).toUByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUByteSub` 함수를 제공합니다.
 */
fun ByteBuf.readUByteSub(): UByte = ((HALF_BYTE - readByte()) and 0xFF).toUByte()

/**
 * Netty 처리에서 데이터를 읽어오는 `readShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.readShortAdd(): Short =
    ((readByte().toInt() shl Byte.SIZE_BITS) or ((readByte() - HALF_BYTE) and 0xFF)).toShort()

/**
 * Netty 처리에서 데이터를 읽어오는 `readShortLEAdd` 함수를 제공합니다.
 */
fun ByteBuf.readShortLEAdd(): Short =
    (((readByte() - HALF_BYTE) and 0xFF) or (readByte().toInt() shl Byte.SIZE_BITS)).toShort()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.readUShortAdd(): Int =
    (readUnsignedByte().toInt() shl Byte.SIZE_BITS) or ((readByte() - HALF_BYTE) and 0xFF)

/**
 * Netty 처리에서 데이터를 읽어오는 `readUShortLEAdd` 함수를 제공합니다.
 */
fun ByteBuf.readUShortLEAdd(): Int =
    ((readByte() - HALF_BYTE) and 0xFF) or (readUnsignedByte().toInt() shl Byte.SIZE_BITS)

/**
 * Netty 처리에서 데이터를 읽어오는 `readMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.readMediumLME(): Int =
    (readShortLE().toInt() shl Byte.SIZE_BITS) or readUnsignedByte().toInt()

/**
 * Netty 처리에서 데이터를 읽어오는 `readMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.readMediumRME(): Int =
    (readByte().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.readUMediumLME(): Int =
    (readUnsignedShortLE() shl Byte.SIZE_BITS) or readUnsignedByte().toInt()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.readUMediumRME(): Int =
    (readUnsignedByte().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

/**
 * Netty 처리에서 데이터를 읽어오는 `readIntME` 함수를 제공합니다.
 */
fun ByteBuf.readIntME(): Int =
    (readShortLE().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

/**
 * Netty 처리에서 데이터를 읽어오는 `readIntIME` 함수를 제공합니다.
 */
fun ByteBuf.readIntIME(): Int =
    readUnsignedShort() or (readShort().toInt() shl Short.SIZE_BITS)

/**
 * Netty 처리에서 데이터를 읽어오는 `readUIntME` 함수를 제공합니다.
 */
fun ByteBuf.readUIntME(): Long =
    (readUnsignedShortLE().toLong() shl Short.SIZE_BITS) or readUnsignedShortLE().toLong()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUIntIME` 함수를 제공합니다.
 */
fun ByteBuf.readUIntIME(): Long =
    readUnsignedShort().toLong() or (readUnsignedShort().toLong() shl Short.SIZE_BITS)

/**
 * Netty 처리에서 데이터를 읽어오는 `readSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.readSmallLong(): Long =
    (readMedium().toLong() shl Medium.SIZE_BITS) or readUnsignedMedium().toLong()

/**
 * Netty 처리에서 데이터를 읽어오는 `readUSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.readUSmallLong(): Long =
    (readUnsignedMedium().toLong() shl Medium.SIZE_BITS) or readUnsignedMedium().toLong()

/**
 * Netty 처리에서 데이터를 읽어오는 `readShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.readShortSmart(): Short {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        (readUnsignedByte().toInt() - Smart.BYTE_MOD).toShort()
    } else {
        ((readUnsignedShort() and Short.MAX_VALUE.toInt()) - Smart.SHORT_MOD).toShort()
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readUShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.readUShortSmart(): Short {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedByte()
    } else {
        (readUnsignedShort() and Short.MAX_VALUE.toInt()).toShort()
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readIncrShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.readIncrShortSmart(): Int {
    var total = 0
    var cur = readUShortSmart()
    while (cur == Short.MAX_VALUE) {
        total += Short.MAX_VALUE.toInt()
        cur = readUShortSmart()
    }
    total += cur
    return total
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.readIntSmart(): Int {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedShort() - Smart.SHORT_MOD
    } else {
        (readInt() and Int.MAX_VALUE) - Smart.INT_MOD
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readUIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.readUIntSmart(): Int {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedShort()
    } else {
        readInt() and Int.MAX_VALUE
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readNullableUIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.readNullableUIntSmart(): Int? {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        val result = readUnsignedShort()
        if (result == Short.MAX_VALUE.toInt()) null else result
    } else {
        readInt() and Int.MAX_VALUE
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readVarInt` 함수를 제공합니다.
 */
fun ByteBuf.readVarInt(): Int {
    var prev = 0
    var temp = readByte().toInt()
    while (temp < 0) {
        prev = prev or (temp and Byte.MAX_VALUE.toInt()) shl (Byte.SIZE_BITS - 1)
        temp = readByte().toInt()
    }
    return prev or temp
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readString` 함수를 제공합니다.
 */
fun ByteBuf.readString(charset: Charset = Charsets.UTF_8): String {
    val end = forEachByte(ByteProcessor.FIND_NUL)
    if (end == -1) throw IOException("String does not terminate.")
    val value = toString(readerIndex(), end - readerIndex(), charset)
    readerIndex(end + 1)
    return value
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readVersionedString` 함수를 제공합니다.
 */
fun ByteBuf.readVersionedString(charset: Charset = Charsets.UTF_8, expectedVersion: Int = 0): String {
    val actualVersion = readUnsignedByte().toInt()
    if (actualVersion != expectedVersion) throw IOException("Expected version number did not match actual version.")
    return readString(charset)
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.readBytesReversed(length: Int): ByteArray {
    return ByteArray(length).apply {
        readBytesReversed(this)
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.readBytesReversed(dest: ByteArray): ByteBuf = apply {
    val endReaderIndex = readerIndex() + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo readerIndex()).withIndex()) {
        dest[writerIndex] = getByte(readerIndex)
    }
    readerIndex(endReaderIndex + 1)
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.readBytesAdd(length: Int): ByteArray =
    ByteArray(length).apply {
        readBytesAdd(this)
    }

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.readBytesAdd(dest: ByteArray): ByteBuf = apply {
    for (writerIndex in dest.indices) {
        dest[writerIndex] = readByteAdd()
    }
}

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.readBytesReversedAdd(length: Int): ByteArray =
    ByteArray(length).apply {
        readBytesReversedAdd(this)
    }

/**
 * Netty 처리에서 데이터를 읽어오는 `readBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.readBytesReversedAdd(dest: ByteArray): ByteBuf = apply {
    val endReaderIndex = readerIndex() + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo readerIndex()).withIndex()) {
        dest[writerIndex] = getByteAdd(readerIndex)
    }
    readerIndex(endReaderIndex + 1)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeByteNeg` 함수를 제공합니다.
 */
fun ByteBuf.writeByteNeg(value: Int): ByteBuf = writeByte(-value)

/**
 * Netty 처리에서 데이터를 기록하는 `writeByteAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeByteAdd(value: Int): ByteBuf = writeByte(value + HALF_BYTE)

/**
 * Netty 처리에서 데이터를 기록하는 `writeByteSub` 함수를 제공합니다.
 */
fun ByteBuf.writeByteSub(value: Int): ByteBuf = writeByte(HALF_BYTE - value)

/**
 * Netty 처리에서 데이터를 기록하는 `writeShortAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeShortAdd(value: Int): ByteBuf = apply {
    writeByte(value shr Byte.SIZE_BITS)
    writeByte(value + HALF_BYTE)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeShortLEAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeShortLEAdd(value: Int): ByteBuf = apply {
    writeByte(value + HALF_BYTE)
    writeByte(value shr Byte.SIZE_BITS)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeMediumLME` 함수를 제공합니다.
 */
fun ByteBuf.writeMediumLME(value: Int): ByteBuf = apply {
    writeShortLE(value shr Byte.SIZE_BITS)
    writeByte(value)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeMediumRME` 함수를 제공합니다.
 */
fun ByteBuf.writeMediumRME(value: Int): ByteBuf = apply {
    writeByte(value shr Short.SIZE_BITS)
    writeShortLE(value)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeIntME` 함수를 제공합니다.
 */
fun ByteBuf.writeIntME(value: Int): ByteBuf = apply {
    writeShortLE(value shr Short.SIZE_BITS)
    writeShortLE(value)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeIntIME` 함수를 제공합니다.
 */
fun ByteBuf.writeIntIME(value: Int): ByteBuf = apply {
    writeShort(value)
    writeShort(value shr Short.SIZE_BITS)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeSmallLong` 함수를 제공합니다.
 */
fun ByteBuf.writeSmallLong(value: Long): ByteBuf {
    writeMedium((value shr Medium.SIZE_BITS).toInt())
    writeMedium(value.toInt())
    return this
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeShortSmart(value: Int): ByteBuf = when (value) {
    in Smart.MIN_BYTE_VALUE..Smart.MAX_BYTE_VALUE   ->
        writeByte(value + Smart.BYTE_MOD)

    in Smart.MIN_SHORT_VALUE..Smart.MAX_SHORT_VALUE ->
        writeShort((Short.MAX_VALUE + 1) or (value + Smart.SHORT_MOD))

    else                                            ->
        throw IllegalArgumentException(
            "Value should be between ${Smart.MIN_SHORT_VALUE} and ${Smart.MAX_SHORT_VALUE}, but was $value."
        )
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeUShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeUShortSmart(value: Int): ByteBuf = when (value) {
    in USmart.MIN_BYTE_VALUE..USmart.MAX_BYTE_VALUE   -> writeByte(value)
    in USmart.MIN_SHORT_VALUE..USmart.MAX_SHORT_VALUE -> writeShort((Short.MAX_VALUE + 1) or value)
    else                                              ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_SHORT_VALUE} and ${USmart.MAX_SHORT_VALUE}, but was $value."
        )
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeIncrShortSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeIncrShortSmart(value: Int): ByteBuf = apply {
    var remaining = value
    while (remaining >= Short.MAX_VALUE.toInt()) {
        writeUShortSmart(Short.MAX_VALUE.toInt())
        remaining -= Short.MAX_VALUE.toInt()
    }
    writeUShortSmart(remaining)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeIntSmart(value: Int): ByteBuf = when (value) {
    in Smart.MIN_SHORT_VALUE..Smart.MAX_SHORT_VALUE ->
        writeShort(value + Smart.SHORT_MOD)

    in Smart.MIN_INT_VALUE..Smart.MAX_INT_VALUE     -> {
        writeInt(Int.MIN_VALUE or (value + Smart.INT_MOD))
    }

    else                                            ->
        throw IllegalArgumentException(
            "Value should be between ${Smart.MIN_INT_VALUE} and ${Smart.MAX_INT_VALUE}, but was $value."
        )
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeUIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeUIntSmart(value: Int): ByteBuf = when (value) {
    in USmart.MIN_SHORT_VALUE..USmart.MAX_SHORT_VALUE -> writeShort(value)
    in USmart.MIN_INT_VALUE..USmart.MAX_INT_VALUE     -> writeInt(Int.MIN_VALUE or value)
    else                                              ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_INT_VALUE} and ${USmart.MAX_INT_VALUE}, but was $value."
        )
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeNullableUIntSmart` 함수를 제공합니다.
 */
fun ByteBuf.writeNullableUIntSmart(value: Int?): ByteBuf = when (value) {
    null                                                   -> writeShort(USmart.MAX_SHORT_VALUE)
    in USmart.MIN_SHORT_VALUE until USmart.MAX_SHORT_VALUE -> writeShort(value)
    in USmart.MIN_INT_VALUE..USmart.MAX_INT_VALUE          -> writeInt(Int.MIN_VALUE or value)
    else                                                   ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_INT_VALUE} and ${USmart.MAX_INT_VALUE}, but was $value."
        )
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeVarInt` 함수를 제공합니다.
 */
fun ByteBuf.writeVarInt(value: Int): ByteBuf {
    if (value and -HALF_BYTE != 0) {
        if (value and -16384 != 0) {
            if (value and -2097152 != 0) {
                if (value and -268435456 != 0) {
                    writeByte(value.ushr(4 * (Byte.SIZE_BITS - 1)) or (Byte.MAX_VALUE.toInt() + 1))
                }
                writeByte(value.ushr(3 * (Byte.SIZE_BITS - 1)) or (Byte.MAX_VALUE.toInt() + 1))
            }
            writeByte(value.ushr(2 * (Byte.SIZE_BITS - 1)) or (Byte.MAX_VALUE.toInt() + 1))
        }
        writeByte(value.ushr((Byte.SIZE_BITS - 1)) or (Byte.MAX_VALUE.toInt() + 1))
    }
    writeByte(value and Byte.MAX_VALUE.toInt())
    return this
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeString` 함수를 제공합니다.
 */
fun ByteBuf.writeString(
    value: String,
    charset: Charset = Charsets.UTF_8,
): ByteBuf = apply {
    writeCharSequence(value, charset)
    writeByte(0)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeVersionedString` 함수를 제공합니다.
 */
fun ByteBuf.writeVersionedString(
    value: String,
    charset: Charset = Charsets.UTF_8,
    version: Int = 0,
): ByteBuf = apply {
    writeByte(version)
    writeString(value, charset)
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesReversed(src: ByteArray): ByteBuf = apply {
    for (i in src.size - 1 downTo 0) {
        writeByte(src[i].toInt())
    }
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesReversed` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesReversed(src: ByteBuf): ByteBuf = apply {
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        writeByte(src.getByte(i).toInt())
    }
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesAdd(src: ByteArray): ByteBuf =
    writeBytes(src.map { (it + HALF_BYTE).toByte() }.toByteArray())

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesAdd(src: ByteBuf): ByteBuf = apply {
    for (i in src.readerIndex() until src.writerIndex()) {
        writeByte(src.getByte(i) + HALF_BYTE)
    }
}

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesReversedAdd(src: ByteArray): ByteBuf =
    writeBytes(src.map { (it + HALF_BYTE).toByte() }.reversed().toByteArray())

/**
 * Netty 처리에서 데이터를 기록하는 `writeBytesReversedAdd` 함수를 제공합니다.
 */
fun ByteBuf.writeBytesReversedAdd(src: ByteBuf): ByteBuf = apply {
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        writeByte(src.getByte(i) + HALF_BYTE)
    }
}
