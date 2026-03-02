package io.bluetape4k.junit5.utils

import io.bluetape4k.logging.KLogging
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 여러 예외를 스레드 안전하게 수집한 뒤 한 번에 전파하기 위한 예외 컨테이너입니다.
 *
 * ## 동작/계약
 * - 내부 리스트 접근은 [ReentrantLock]으로 동기화합니다.
 * - [add]에 [MultiException]을 전달하면 내부 nested 예외를 평탄화해 합칩니다.
 * - [throwIfNotEmpty]는 1개면 원본 예외, 2개 이상이면 자기 자신을 던집니다.
 *
 * ```kotlin
 * val me = MultiException()
 * me.add(IllegalStateException("x"))
 * // me.isEmpty() == false
 * ```
 */
class MultiException: RuntimeException("Multiple exceptions") {

    companion object: KLogging() {
        private const val EXCEPTION_SEPARATOR = "\n\t______________________________________\n"
    }

    private val nested = mutableListOf<Throwable>()
    private val lock = ReentrantLock()

    /**
     * 예외 정보를 추가합니다.
     *
     * ## 동작/계약
     * - `throwable`이 null이면 아무 작업도 하지 않습니다.
     * - [MultiException]이면 내부 nested 목록을 병합합니다.
     * - 그 외 예외는 그대로 nested 목록에 append합니다.
     *
     * @param throwable 추가할 예외 정보. null 이면 추가하지 않습니다.
     */
    fun add(throwable: Throwable?) {
        throwable?.let { error ->
            lock.withLock {
                if (error is MultiException) {
                    val otherNested = error.nested
                    nested.addAll(otherNested)
                } else {
                    nested.add(error)
                }
            }
        }
    }

    /**
     * 수집된 예외가 없는지 확인합니다.
     */
    fun isEmpty(): Boolean = lock.withLock {
        nested.isEmpty()
    }

    /**
     * 추가된 예외가 없으면, 아무런 동작을 하지 않습니다.
     * 추가된 예외가 하나라면 [Throwable]을 던집니다.
     * 추가된 예외가 복수개라면 [MultiException]을 던집니다.
     */
    fun throwIfNotEmpty() {
        lock.withLock {
            when {
                nested.size == 1 -> throw nested[0]
                nested.size > 1  -> throw this
                else             -> { /* do nothing */
                }
            }
        }
    }

    override val message: String get() = buildMessage()

    private fun buildMessage(): String = lock.withLock {
        if (nested.isEmpty()) {
            "<no nested exceptions>"
        } else {
            buildString {
                val n = nested.size
                append(n).append(if (n == 1) " nested exception:" else " nested exceptions:")
                nested.forEach { t ->
                    appendLine(EXCEPTION_SEPARATOR)
                    StringWriter().use { sw ->
                        PrintWriter(sw).use { pw ->
                            t.printStackTrace(pw)
                        }
                        append(sw.toString().replace("\n", "\n\t").trim())
                    }
                }
                appendLine(EXCEPTION_SEPARATOR)
            }
        }
    }
}
