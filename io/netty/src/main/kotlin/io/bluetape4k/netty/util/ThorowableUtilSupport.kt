package io.bluetape4k.netty.util

import io.netty.util.internal.ThrowableUtil
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Netty 처리에서 `unknownStackTrace` 함수를 제공합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("test").unknownStackTrace(String::class.java, "someMethod")
 * // ex.stackTrace.isEmpty() == true  // stack trace가 unknown으로 대체됨
 * ```
 */
fun <T: Throwable> T.unknownStackTrace(clazz: Class<*>, method: String): T =
    ThrowableUtil.unknownStackTrace(this, clazz, method)

/**
 * Netty 처리에서 `stackTraceToString` 함수를 제공합니다.
 *
 * ```kotlin
 * val trace = RuntimeException("boom").stackTraceToString()
 * // trace.contains("RuntimeException") == true
 * ```
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
 *
 * ```kotlin
 * val ex = RuntimeException("main")
 * val suppressed = mutableListOf<Throwable>(IllegalStateException("s1"))
 * ex.addSuppressedAndClear(suppressed)
 * // suppressed.isEmpty() == true  // 목록이 비워짐
 * ```
 */
fun Throwable.addSuppressedAndClear(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressedAndClear(this, suppressed)
}

/**
 * Netty 처리에서 `addSuppressed` 함수를 제공합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("main")
 * ex.addSuppressed(listOf(IllegalStateException("s1")))
 * // ex.suppressed.size == 1
 * ```
 */
fun Throwable.addSuppressed(suppressed: List<Throwable>) {
    ThrowableUtil.addSuppressed(this, suppressed)
}
