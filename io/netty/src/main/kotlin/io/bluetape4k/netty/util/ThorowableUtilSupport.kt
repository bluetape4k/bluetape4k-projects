package io.bluetape4k.netty.util

import io.netty.util.internal.ThrowableUtil
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Netty 처리에서 `unknownStackTrace` 함수를 제공합니다.
 */
fun <T: Throwable> T.unknownStackTrace(clazz: Class<*>, method: String): T =
    ThrowableUtil.unknownStackTrace(this, clazz, method)

/**
 * Netty 처리에서 `stackTraceToString` 함수를 제공합니다.
 */
fun Throwable.stackTraceToString(): String =
    StringWriter().use { sw ->
        PrintWriter(sw).use { pw ->
            this.printStackTrace(pw)
        }
        sw.toString()
    }

/**
 * Netty 처리에서 `addSuppressedAndClear` 함수를 제공합니다.
 */
fun Throwable.addSuppressedAndClear(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressedAndClear(this, suppressed)
}

/**
 * Netty 처리에서 `addSuppressed` 함수를 제공합니다.
 */
fun Throwable.addSuppressed(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressed(this, suppressed)
}
