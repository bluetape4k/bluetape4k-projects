package io.bluetape4k.logback.kafka.utils

import java.nio.ByteBuffer

fun String?.hashBytes(): ByteArray? {
    return if (this.isNullOrBlank()) null
    else this.hashCode().toByteArray()
}

internal fun Int.toByteArray(): ByteArray =
    ByteBuffer.allocate(4).putInt(this).array()
