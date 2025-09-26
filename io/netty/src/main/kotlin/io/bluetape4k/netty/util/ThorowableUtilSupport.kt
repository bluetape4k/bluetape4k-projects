package io.bluetape4k.netty.util

import io.netty.util.internal.ThrowableUtil
import java.io.PrintWriter
import java.io.StringWriter

fun <T: Throwable> T.unknownStackTrace(clazz: Class<*>, method: String): T =
    ThrowableUtil.unknownStackTrace(this, clazz, method)

fun Throwable.stackTraceToString(): String =
    StringWriter().use { sw ->
        PrintWriter(sw).use { pw ->
            this.printStackTrace(pw)
        }
        sw.toString()
    }

fun Throwable.addSuppressedAndClear(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressedAndClear(this, suppressed)
}

fun Throwable.addSuppressed(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressed(this, suppressed)
}
