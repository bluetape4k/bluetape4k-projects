package io.bluetape4k.junit5.utils

import io.bluetape4k.logging.KLogging
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * 복수의 예외를 하나의 예외로 던질 수 있도록 합니다.
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
