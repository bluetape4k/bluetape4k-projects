package io.bluetape4k.support

import kotlin.uuid.Uuid

fun Uuid.uuidVersion(): Int = (toByteArray()[6].toInt() ushr 4) and 0x0F

fun Uuid.v7Timestamp(): Long = toByteArray()
    .take(6)
    .fold(0L) { timestamp, byte ->
        (timestamp shl 8) or (byte.toInt() and 0xFF).toLong()
    }
