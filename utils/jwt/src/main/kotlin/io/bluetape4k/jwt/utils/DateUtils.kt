package io.bluetape4k.jwt.utils

import java.util.*

val Date.epochSeconds: Long
    get() = this.time / 1000L

val Date?.epochSecondsOrNull: Long?
    get() = this?.let { epochSeconds }

val Date?.epochSecondsOrMaxValue: Long
    get() = this?.let { epochSeconds } ?: Long.MAX_VALUE

fun dateOfEpochSeconds(epochSeconds: Long): Date =
    Date(epochSeconds * 1000L)

fun Long.millisToSeconds(): Long = this / 1000L
