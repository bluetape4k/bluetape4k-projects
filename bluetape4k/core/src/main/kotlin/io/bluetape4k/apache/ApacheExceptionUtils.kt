package io.bluetape4k.apache

import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.PrintStream
import java.io.PrintWriter

/**
 * [Throwable]을 [RuntimeException]으로 변환합니다.
 */
fun <T: RuntimeException> Throwable.asRuntimeException(): T = ExceptionUtils.asRuntimeException(this)

/**
 * [Throwable]와 inner cause 들에 대해 주어진 [consumer]를 실행합니다.
 */
fun Throwable.forEach(consumer: (Throwable) -> Unit) = ExceptionUtils.forEach(this, consumer)

/**
 * [Throwable]의 root cause를 반환합니다.
 */
fun Throwable.getRootCause(): Throwable = ExceptionUtils.getRootCause(this)

/**
 * [Throwable]의 root cause message를 반환합니다.
 */
fun Throwable.getRootCauseMessage(): String = ExceptionUtils.getRootCauseMessage(this)

/**
 * [Throwable]의 root cause stack trace를 Array로 반환합니다.
 */
fun Throwable.getRootCauseStackTrace(): Array<String> = ExceptionUtils.getRootCauseStackTrace(this)

/**
 * [Throwable]의 root cause stack trace를 List로 반환합니다.
 */
fun Throwable.getRootCauseStackTraceList(): List<String> = ExceptionUtils.getRootCauseStackTraceList(this)

/**
 * [Throwable]의 stack frame 들을 Array로 반환합니다.
 */
fun Throwable.getStackFrames(): Array<String> = ExceptionUtils.getStackFrames(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]의 개수를 반환합니다.
 */
fun Throwable.getThrowableCount(): Int = ExceptionUtils.getThrowableCount(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]들을 List로 반환합니다.
 */
fun Throwable.getThrowableList(): List<Throwable> = ExceptionUtils.getThrowableList(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]들을 Array로 반환합니다.
 */
fun Throwable.getThrowables(): Array<Throwable> = ExceptionUtils.getThrowables(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]들 중 주어진 [type]과 같은 [Throwable]의 개수를 반환합니다.
 */
fun Throwable.hasCause(type: Class<out Throwable>): Boolean = ExceptionUtils.hasCause(this, type)

/**
 * [Throwable]의 root cause의 stack trace를 [printStream]에 출력합니다.
 */
fun Throwable.printRootCauseStackTrace(printStream: PrintStream = System.err) =
    ExceptionUtils.printRootCauseStackTrace(this, printStream)

/**
 * [Throwable]의 root cause의 stack trace를 [printWriter]에 출력합니다.
 */
fun Throwable.printRootCauseStackTrace(printWriter: PrintWriter) =
    ExceptionUtils.printRootCauseStackTrace(this, printWriter)
