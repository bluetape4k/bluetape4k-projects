package io.bluetape4k.netty.util

import io.netty.util.internal.ThrowableUtil

fun <T: Throwable> T.unknownStackTrace(clazz: Class<*>, method: String): T =
    ThrowableUtil.unknownStackTrace(this, clazz, method)

fun Throwable.stackTraceToString(): String =
    ThrowableUtil.stackTraceToString(this)

fun Throwable.addSuppressedAndClear(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressedAndClear(this, suppressed)
}

fun Throwable.addSuppressed(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressed(this, suppressed)
}
