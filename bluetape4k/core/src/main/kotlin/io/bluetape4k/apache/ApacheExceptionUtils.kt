package io.bluetape4k.apache

import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.PrintStream
import java.io.PrintWriter

/**
 * [Throwable]을 [RuntimeException]으로 변환합니다.
 *
 * Apache Commons Lang3 [ExceptionUtils]의 Kotlin 확장 래퍼입니다.
 *
 * ```kotlin
 * val ex: Throwable = IOException("disk error")
 * val rte: RuntimeException = ex.asRuntimeException()
 * ```
 *
 * @see org.apache.commons.lang3.exception.ExceptionUtils
 */
fun <T: RuntimeException> Throwable.asRuntimeException(): T = ExceptionUtils.asRuntimeException(this)

/**
 * [Throwable]와 inner cause 들에 대해 주어진 [consumer]를 실행합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * ex.forEach { println(it.message) }
 * // outer
 * // inner
 * ```
 *
 * @param consumer 각 예외에 대해 실행할 함수
 */
fun Throwable.forEach(consumer: (Throwable) -> Unit) = ExceptionUtils.forEach(this, consumer)

/**
 * [Throwable]의 root cause를 반환합니다.
 *
 * root cause가 없으면 자신을 반환합니다.
 *
 * ```kotlin
 * val inner = IllegalStateException("inner")
 * val outer = RuntimeException("outer", inner)
 * outer.getRootCause() // IllegalStateException("inner")
 * ```
 *
 * @return root cause 예외 (없으면 자신)
 */
fun Throwable.getRootCause(): Throwable = ExceptionUtils.getRootCause(this) ?: this

/**
 * [Throwable]의 root cause message를 반환합니다.
 *
 * ```kotlin
 * val inner = IllegalStateException("inner error")
 * val outer = RuntimeException("outer error", inner)
 * outer.getRootCauseMessage() // "IllegalStateException: inner error"
 * ```
 *
 * @return root cause의 메시지 문자열
 */
fun Throwable.getRootCauseMessage(): String = ExceptionUtils.getRootCauseMessage(this)

/**
 * [Throwable]의 root cause stack trace를 Array로 반환합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * val stackTrace = ex.getRootCauseStackTrace()
 * // stackTrace[0] == "IllegalStateException: inner"
 * ```
 *
 * @return root cause의 스택 트레이스 문자열 배열
 */
fun Throwable.getRootCauseStackTrace(): Array<String> = ExceptionUtils.getRootCauseStackTrace(this)

/**
 * [Throwable]의 root cause stack trace를 List로 반환합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * val list = ex.getRootCauseStackTraceList()
 * // list.first() == "IllegalStateException: inner"
 * ```
 *
 * @return root cause의 스택 트레이스 문자열 리스트
 */
fun Throwable.getRootCauseStackTraceList(): List<String> = ExceptionUtils.getRootCauseStackTraceList(this)

/**
 * [Throwable]의 stack frame 들을 Array로 반환합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("test error")
 * val frames = ex.getStackFrames()
 * // frames.isNotEmpty() == true
 * ```
 *
 * @return 스택 프레임 문자열 배열
 */
fun Throwable.getStackFrames(): Array<String> = ExceptionUtils.getStackFrames(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]의 개수를 반환합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * ex.getThrowableCount() // 2
 * ```
 *
 * @return exception chain의 예외 개수
 */
fun Throwable.getThrowableCount(): Int = ExceptionUtils.getThrowableCount(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]들을 List로 반환합니다.
 *
 * ```kotlin
 * val inner = IllegalStateException("inner")
 * val outer = RuntimeException("outer", inner)
 * outer.getThrowableList() // [outer, inner]
 * ```
 *
 * @return exception chain의 예외 리스트 (가장 바깥 예외부터 순서대로)
 */
fun Throwable.getThrowableList(): List<Throwable> = ExceptionUtils.getThrowableList(this)

/**
 * [Throwable]의 exception chain에 있는 [Throwable]들을 Array로 반환합니다.
 *
 * ```kotlin
 * val inner = IllegalStateException("inner")
 * val outer = RuntimeException("outer", inner)
 * outer.getThrowables() // [outer, inner]
 * ```
 *
 * @return exception chain의 예외 배열 (가장 바깥 예외부터 순서대로)
 */
fun Throwable.getThrowables(): Array<Throwable> = ExceptionUtils.getThrowables(this)

/**
 * [Throwable]의 exception chain에 주어진 [type]의 예외가 포함되어 있는지 확인합니다.
 *
 * ```kotlin
 * val inner = IllegalStateException("inner")
 * val outer = RuntimeException("outer", inner)
 * outer.hasCause(IllegalStateException::class.java) // true
 * outer.hasCause(IOException::class.java)           // false
 * ```
 *
 * @param type 검색할 예외 타입
 * @return 주어진 타입의 예외가 chain에 포함되어 있으면 true
 */
fun Throwable.hasCause(type: Class<out Throwable>): Boolean = ExceptionUtils.hasCause(this, type)

/**
 * [Throwable]의 root cause의 stack trace를 [printStream]에 출력합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * ex.printRootCauseStackTrace()          // System.err 에 출력
 * ex.printRootCauseStackTrace(System.out) // System.out 에 출력
 * ```
 *
 * @param printStream 출력 대상 스트림 (기본: System.err)
 */
fun Throwable.printRootCauseStackTrace(printStream: PrintStream = System.err) =
    ExceptionUtils.printRootCauseStackTrace(this, printStream)

/**
 * [Throwable]의 root cause의 stack trace를 [printWriter]에 출력합니다.
 *
 * ```kotlin
 * val ex = RuntimeException("outer", IllegalStateException("inner"))
 * val writer = PrintWriter(System.err)
 * ex.printRootCauseStackTrace(writer)
 * ```
 *
 * @param printWriter 출력 대상 writer
 */
fun Throwable.printRootCauseStackTrace(printWriter: PrintWriter) =
    ExceptionUtils.printRootCauseStackTrace(this, printWriter)
