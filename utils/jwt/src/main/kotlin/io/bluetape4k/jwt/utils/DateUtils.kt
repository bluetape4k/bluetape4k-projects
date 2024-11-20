package io.bluetape4k.jwt.utils

import java.util.*

val Date.epochSeconds: Long
    get() = this.time / 1000L

val Date?.epochSecondsOrNull: Long?
    get() = if (this != null) time / 1000L else null

val Date?.epochSecondsOrMaxValue: Long
    get() = if (this != null) time / 1000L else Long.MAX_VALUE

fun dateOfEpochSeconds(epochSeconds: Long): Date =
    Date(epochSeconds * 1000L)

fun Long.millisToSeconds(): Long = this / 1000L
