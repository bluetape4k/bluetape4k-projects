package io.bluetape4k.netty.buffer

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.util.ByteProcessor
import java.io.IOException
import java.nio.charset.Charset

internal const val HALF_BYTE = 128

fun ByteBuf.getByteNeg(index: Int): Byte = (-getByte(index)).toByte()
fun ByteBuf.getByteAdd(index: Int): Byte = (getByte(index) - HALF_BYTE).toByte()
fun ByteBuf.getByteSub(index: Int): Byte = (HALF_BYTE - getByte(index)).toByte()


fun ByteBuf.getUByteNeg(index: Int): UByte = (-getByte(index) and 0xFF).toUByte()
fun ByteBuf.getUByteAdd(index: Int): UByte = (getByte(index) - HALF_BYTE).toUByte()
fun ByteBuf.getUByteSub(index: Int): UByte = (HALF_BYTE - getByte(index)).toUByte()

fun ByteBuf.getShortAdd(index: Int): Short =
    ((getByte(index).toInt() shl Byte.SIZE_BITS) or
            ((getByte(index + 1) - HALF_BYTE) and 0xFF)).toShort()

fun ByteBuf.getShortLEAdd(index: Int): Short =
    (((getByte(index) - HALF_BYTE) and 0xFF) or
            (getByte(index + 1).toInt() shl Byte.SIZE_BITS)).toShort()

fun ByteBuf.getUShortAdd(index: Int): UShort =
    ((getUnsignedByte(index).toInt() shl Byte.SIZE_BITS) or
            ((getByte(index + 1) - HALF_BYTE) and 0xFF)).toUShort()

fun ByteBuf.getUShortLEAdd(index: Int): UShort =
    (((getByte(index) - HALF_BYTE) and 0xFF) or
            (getUnsignedByte(index + 1).toInt() shl Byte.SIZE_BITS)).toUShort()

fun ByteBuf.getMediumLME(index: Int): Int =
    (getShortLE(index).toInt() shl Byte.SIZE_BITS) or
            getUnsignedByte(index + Short.SIZE_BYTES).toInt()

fun ByteBuf.getMediumRME(index: Int): Int =
    (getByte(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Byte.SIZE_BYTES)

fun ByteBuf.getUMediumLME(index: Int): Int =
    (getUnsignedShortLE(index) shl Byte.SIZE_BITS) or
            getUnsignedByte(index + Short.SIZE_BYTES).toInt()

fun ByteBuf.getUMediumRME(index: Int): Int =
    (getUnsignedByte(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Byte.SIZE_BYTES)

fun ByteBuf.getIntME(index: Int): Int =
    (getShortLE(index).toInt() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Short.SIZE_BYTES)

fun ByteBuf.getIntIME(index: Int): Int =
    getUnsignedShort(index) or
            (getShort(index + Short.SIZE_BYTES).toInt() shl Short.SIZE_BITS)

fun ByteBuf.getUIntME(index: Int): Long =
    (getUnsignedShortLE(index).toLong() shl Short.SIZE_BITS) or
            getUnsignedShortLE(index + Short.SIZE_BYTES).toLong()

fun ByteBuf.getUIntIME(index: Int): Long =
    getUnsignedShort(index).toLong() or
            (getUnsignedShort(index + Short.SIZE_BYTES).toLong() shl Short.SIZE_BITS)

fun ByteBuf.getSmallLong(index: Int): Long =
    (getMedium(index).toLong() shl Medium.SIZE_BITS) or
            getUnsignedMedium(index + Medium.SIZE_BYTES).toLong()

fun ByteBuf.getUSmallLong(index: Int): Long =
    (getUnsignedMedium(index).toLong() shl Medium.SIZE_BITS) or
            getUnsignedMedium(index + Medium.SIZE_BYTES).toLong()

fun ByteBuf.getBytes(
    start: Int = readerIndex(),
    length: Int = readableBytes(),
    copy: Boolean = true,
): ByteArray =
    ByteBufUtil.getBytes(this, start, length, copy)


fun ByteBuf.getBytesReversed(
    index: Int = readerIndex(),
    length: Int = readableBytes(),
): ByteArray =
    ByteArray(length).apply {
        getBytesReversed(index, this)
    }

fun ByteBuf.getBytesReversed(index: Int, dest: ByteArray) = apply {
    val endReaderIndex = index + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo index).withIndex()) {
        dest[writerIndex] = getByte(readerIndex)
    }
}

fun ByteBuf.getBytesAdd(index: Int, length: Int): ByteArray =
    ByteArray(length).apply {
        getBytesAdd(index, this)
    }

fun ByteBuf.getBytesAdd(index: Int, dest: ByteArray) = apply {
    for (writerIndex in dest.indices) {
        dest[writerIndex] = getByteAdd(index + writerIndex)
    }
}

fun ByteBuf.getBytesReversedAdd(index: Int, length: Int): ByteArray =
    ByteArray(length).apply {
        getBytesReversedAdd(index, this)
    }

fun ByteBuf.getBytesReversedAdd(index: Int, dest: ByteArray) = apply {
    val endReaderIndex = index + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo index).withIndex()) {
        dest[writerIndex] = getByteAdd(readerIndex)
    }
}

fun ByteBuf.setByteNeg(index: Int, value: Int): ByteBuf = setByte(index, -value)
fun ByteBuf.setByteAdd(index: Int, value: Int): ByteBuf = setByte(index, value + HALF_BYTE)
fun ByteBuf.setByteSub(index: Int, value: Int): ByteBuf = setByte(index, HALF_BYTE - value)

fun ByteBuf.setShortAdd(index: Int, value: Int): ByteBuf = apply {
    setByte(index, value shr Byte.SIZE_BITS)
    setByte(index + Byte.SIZE_BITS, value + HALF_BYTE)
}

fun ByteBuf.setMediumLME(index: Int, value: Int): ByteBuf = apply {
    setShortLE(index, value shr Byte.SIZE_BITS)
    setByte(index + Short.SIZE_BYTES, value)
}

fun ByteBuf.setMediumRME(index: Int, value: Int): ByteBuf = apply {
    setByte(index, value shr Short.SIZE_BITS)
    setShortLE(index + Byte.SIZE_BYTES, value)
}

fun ByteBuf.setIntME(index: Int, value: Int): ByteBuf = apply {
    setShortLE(index, value shr Short.SIZE_BITS)
    setShortLE(index + Short.SIZE_BYTES, value)
}

fun ByteBuf.setIntIME(index: Int, value: Int): ByteBuf = apply {
    setShort(index, value)
    setShort(index + Short.SIZE_BYTES, value shr Short.SIZE_BITS)
}

fun ByteBuf.setSmallLong(index: Int, value: Long): ByteBuf = apply {
    setMedium(index, (value shr Medium.SIZE_BITS).toInt())
    setMedium(index + Medium.SIZE_BYTES, value.toInt())
}

fun ByteBuf.setBytesReversed(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.reversedArray())
}

fun ByteBuf.setBytesReversed(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        setByte(j++, src.getByte(i).toInt())
    }
}

fun ByteBuf.setBytesAdd(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.map { (it + HALF_BYTE).toByte() }.toByteArray())
}

fun ByteBuf.setBytesAdd(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.readerIndex() until src.writerIndex()) {
        setByte(j++, src.getByte(i) + HALF_BYTE)
    }
}

fun ByteBuf.setBytesReversedAdd(index: Int, src: ByteArray): ByteBuf = apply {
    setBytes(index, src.map { (it + HALF_BYTE).toByte() }.reversed().toByteArray())
}

fun ByteBuf.setBytesReversedAdd(index: Int, src: ByteBuf): ByteBuf = apply {
    var j = index
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        setByte(j++, src.getByte(i) + HALF_BYTE)
    }
}

fun ByteBuf.readByteNeg(): Byte = (-readByte()).toByte()
fun ByteBuf.readByteAdd(): Byte = (readByte() - HALF_BYTE).toByte()
fun ByteBuf.readByteSub(): Byte = (HALF_BYTE - readByte()).toByte()

fun ByteBuf.readUByteNeg(): UByte = (-readByte() and 0xFF).toUByte()
fun ByteBuf.readUByteAdd(): UByte = ((readByte() - HALF_BYTE) and 0xFF).toUByte()
fun ByteBuf.readUByteSub(): UByte = ((HALF_BYTE - readByte()) and 0xFF).toUByte()

fun ByteBuf.readShortAdd(): Short =
    ((readByte().toInt() shl Byte.SIZE_BITS) or ((readByte() - HALF_BYTE) and 0xFF)).toShort()

fun ByteBuf.readShortLEAdd(): Short =
    (((readByte() - HALF_BYTE) and 0xFF) or (readByte().toInt() shl Byte.SIZE_BITS)).toShort()

fun ByteBuf.readUShortAdd(): Int =
    (readUnsignedByte().toInt() shl Byte.SIZE_BITS) or ((readByte() - HALF_BYTE) and 0xFF)

fun ByteBuf.readUShortLEAdd(): Int =
    ((readByte() - HALF_BYTE) and 0xFF) or (readUnsignedByte().toInt() shl Byte.SIZE_BITS)

fun ByteBuf.readMediumLME(): Int =
    (readShortLE().toInt() shl Byte.SIZE_BITS) or readUnsignedByte().toInt()

fun ByteBuf.readMediumRME(): Int =
    (readByte().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

fun ByteBuf.readUMediumLME(): Int =
    (readUnsignedShortLE() shl Byte.SIZE_BITS) or readUnsignedByte().toInt()

fun ByteBuf.readUMediumRME(): Int =
    (readUnsignedByte().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

fun ByteBuf.readIntME(): Int =
    (readShortLE().toInt() shl Short.SIZE_BITS) or readUnsignedShortLE()

fun ByteBuf.readIntIME(): Int =
    readUnsignedShort() or (readShort().toInt() shl Short.SIZE_BITS)

fun ByteBuf.readUIntME(): Long =
    (readUnsignedShortLE().toLong() shl Short.SIZE_BITS) or readUnsignedShortLE().toLong()

fun ByteBuf.readUIntIME(): Long =
    readUnsignedShort().toLong() or (readUnsignedShort().toLong() shl Short.SIZE_BITS)

fun ByteBuf.readSmallLong(): Long =
    (readMedium().toLong() shl Medium.SIZE_BITS) or readUnsignedMedium().toLong()

fun ByteBuf.readUSmallLong(): Long =
    (readUnsignedMedium().toLong() shl Medium.SIZE_BITS) or readUnsignedMedium().toLong()

fun ByteBuf.readShortSmart(): Short {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        (readUnsignedByte().toInt() - Smart.BYTE_MOD).toShort()
    } else {
        ((readUnsignedShort() and Short.MAX_VALUE.toInt()) - Smart.SHORT_MOD).toShort()
    }
}

fun ByteBuf.readUShortSmart(): Short {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedByte()
    } else {
        (readUnsignedShort() and Short.MAX_VALUE.toInt()).toShort()
    }
}

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

fun ByteBuf.readIntSmart(): Int {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedShort() - Smart.SHORT_MOD
    } else {
        (readInt() and Int.MAX_VALUE) - Smart.INT_MOD
    }
}

fun ByteBuf.readUIntSmart(): Int {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        readUnsignedShort()
    } else {
        readInt() and Int.MAX_VALUE
    }
}

fun ByteBuf.readNullableUIntSmart(): Int? {
    val peek = getByte(readerIndex()).toInt()
    return if (peek >= 0) {
        val result = readUnsignedShort()
        if (result == Short.MAX_VALUE.toInt()) null else result
    } else {
        readInt() and Int.MAX_VALUE
    }
}

fun ByteBuf.readVarInt(): Int {
    var prev = 0
    var temp = readByte().toInt()
    while (temp < 0) {
        prev = prev or (temp and Byte.MAX_VALUE.toInt()) shl (Byte.SIZE_BITS - 1)
        temp = readByte().toInt()
    }
    return prev or temp
}

fun ByteBuf.readString(charset: Charset = Charsets.UTF_8): String {
    val end = forEachByte(ByteProcessor.FIND_NUL)
    if (end == -1) throw IOException("String does not terminate.")
    val value = toString(readerIndex(), end - readerIndex(), charset)
    readerIndex(end + 1)
    return value
}

fun ByteBuf.readVersionedString(charset: Charset = Charsets.UTF_8, expectedVersion: Int = 0): String {
    val actualVersion = readUnsignedByte().toInt()
    if (actualVersion != expectedVersion) throw IOException("Expected version number did not match actual version.")
    return readString(charset)
}

fun ByteBuf.readBytesReversed(length: Int): ByteArray {
    return ByteArray(length).apply {
        readBytesReversed(this)
    }
}

fun ByteBuf.readBytesReversed(dest: ByteArray): ByteBuf = apply {
    val endReaderIndex = readerIndex() + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo readerIndex()).withIndex()) {
        dest[writerIndex] = getByte(readerIndex)
    }
    readerIndex(endReaderIndex + 1)
}

fun ByteBuf.readBytesAdd(length: Int): ByteArray =
    ByteArray(length).apply {
        readBytesAdd(this)
    }

fun ByteBuf.readBytesAdd(dest: ByteArray): ByteBuf = apply {
    for (writerIndex in dest.indices) {
        dest[writerIndex] = readByteAdd()
    }
}

fun ByteBuf.readBytesReversedAdd(length: Int): ByteArray =
    ByteArray(length).apply {
        readBytesReversedAdd(this)
    }

fun ByteBuf.readBytesReversedAdd(dest: ByteArray): ByteBuf = apply {
    val endReaderIndex = readerIndex() + dest.size - 1
    for ((writerIndex, readerIndex) in (endReaderIndex downTo readerIndex()).withIndex()) {
        dest[writerIndex] = getByteAdd(readerIndex)
    }
    readerIndex(endReaderIndex + 1)
}

fun ByteBuf.writeByteNeg(value: Int): ByteBuf = writeByte(-value)

fun ByteBuf.writeByteAdd(value: Int): ByteBuf = writeByte(value + HALF_BYTE)

fun ByteBuf.writeByteSub(value: Int): ByteBuf = writeByte(HALF_BYTE - value)

fun ByteBuf.writeShortAdd(value: Int): ByteBuf = apply {
    writeByte(value shr Byte.SIZE_BITS)
    writeByte(value + HALF_BYTE)
}

fun ByteBuf.writeShortLEAdd(value: Int): ByteBuf = apply {
    writeByte(value + HALF_BYTE)
    writeByte(value shr Byte.SIZE_BITS)
}

fun ByteBuf.writeMediumLME(value: Int): ByteBuf = apply {
    writeShortLE(value shr Byte.SIZE_BITS)
    writeByte(value)
}

fun ByteBuf.writeMediumRME(value: Int): ByteBuf = apply {
    writeByte(value shr Short.SIZE_BITS)
    writeShortLE(value)
}

fun ByteBuf.writeIntME(value: Int): ByteBuf = apply {
    writeShortLE(value shr Short.SIZE_BITS)
    writeShortLE(value)
}

fun ByteBuf.writeIntIME(value: Int): ByteBuf = apply {
    writeShort(value)
    writeShort(value shr Short.SIZE_BITS)
}

fun ByteBuf.writeSmallLong(value: Long): ByteBuf {
    writeMedium((value shr Medium.SIZE_BITS).toInt())
    writeMedium(value.toInt())
    return this
}

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

fun ByteBuf.writeUShortSmart(value: Int): ByteBuf = when (value) {
    in USmart.MIN_BYTE_VALUE..USmart.MAX_BYTE_VALUE   -> writeByte(value)
    in USmart.MIN_SHORT_VALUE..USmart.MAX_SHORT_VALUE -> writeShort((Short.MAX_VALUE + 1) or value)
    else                                              ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_SHORT_VALUE} and ${USmart.MAX_SHORT_VALUE}, but was $value."
        )
}

fun ByteBuf.writeIncrShortSmart(value: Int): ByteBuf = apply {
    var remaining = value
    while (remaining >= Short.MAX_VALUE.toInt()) {
        writeUShortSmart(Short.MAX_VALUE.toInt())
        remaining -= Short.MAX_VALUE.toInt()
    }
    writeUShortSmart(remaining)
}

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

fun ByteBuf.writeUIntSmart(value: Int): ByteBuf = when (value) {
    in USmart.MIN_SHORT_VALUE..USmart.MAX_SHORT_VALUE -> writeShort(value)
    in USmart.MIN_INT_VALUE..USmart.MAX_INT_VALUE     -> writeInt(Int.MIN_VALUE or value)
    else                                              ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_INT_VALUE} and ${USmart.MAX_INT_VALUE}, but was $value."
        )
}

fun ByteBuf.writeNullableUIntSmart(value: Int?): ByteBuf = when (value) {
    null                                                   -> writeShort(USmart.MAX_SHORT_VALUE)
    in USmart.MIN_SHORT_VALUE until USmart.MAX_SHORT_VALUE -> writeShort(value)
    in USmart.MIN_INT_VALUE..USmart.MAX_INT_VALUE          -> writeInt(Int.MIN_VALUE or value)
    else                                                   ->
        throw IllegalArgumentException(
            "Value should be between ${USmart.MIN_INT_VALUE} and ${USmart.MAX_INT_VALUE}, but was $value."
        )
}

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

fun ByteBuf.writeString(
    value: String,
    charset: Charset = Charsets.UTF_8,
): ByteBuf = apply {
    writeCharSequence(value, charset)
    writeByte(0)
}

fun ByteBuf.writeVersionedString(
    value: String,
    charset: Charset = Charsets.UTF_8,
    version: Int = 0,
): ByteBuf = apply {
    writeByte(version)
    writeString(value, charset)
}

fun ByteBuf.writeBytesReversed(src: ByteArray): ByteBuf = apply {
    for (i in src.size - 1 downTo 0) {
        writeByte(src[i].toInt())
    }
}

fun ByteBuf.writeBytesReversed(src: ByteBuf): ByteBuf = apply {
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        writeByte(src.getByte(i).toInt())
    }
}

fun ByteBuf.writeBytesAdd(src: ByteArray): ByteBuf =
    writeBytes(src.map { (it + HALF_BYTE).toByte() }.toByteArray())

fun ByteBuf.writeBytesAdd(src: ByteBuf): ByteBuf = apply {
    for (i in src.readerIndex() until src.writerIndex()) {
        writeByte(src.getByte(i) + HALF_BYTE)
    }
}

fun ByteBuf.writeBytesReversedAdd(src: ByteArray): ByteBuf =
    writeBytes(src.map { (it + HALF_BYTE).toByte() }.reversed().toByteArray())

fun ByteBuf.writeBytesReversedAdd(src: ByteBuf): ByteBuf = apply {
    for (i in src.writerIndex() - 1 downTo src.readerIndex()) {
        writeByte(src.getByte(i) + HALF_BYTE)
    }
}
