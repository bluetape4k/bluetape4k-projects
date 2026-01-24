package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * NamedThreadFactory
 *
 * @property prefix   스레드 명의 접두사
 * @property isDaemon 스레드 데몬 여부
 */
class NamedThreadFactory private constructor(
    val prefix: String,
    val isDaemon: Boolean,
): ThreadFactory {

    companion object: KLogging() {
        const val DEFAULT_PREFIX = "thread"

        @JvmStatic
        operator fun invoke(prefix: String? = DEFAULT_PREFIX, isDaemon: Boolean = false): ThreadFactory {
            return NamedThreadFactory(prefix ?: DEFAULT_PREFIX, isDaemon)
        }
    }

    val name: String = prefix

    val group: ThreadGroup by lazy { ThreadGroup(Thread.currentThread().threadGroup, name) }

    private val threadNumber = AtomicInteger(1)

    /**
     * Create a new thread
     *
     * @param body 스레드 실행할 람다
     * @return 새로은 스레드 객체
     */
    fun newThread(body: () -> Unit): Thread {
        return newThread(Runnable { body() })
    }

    /**
     * Create a new thread
     *
     * @param runnable The runnable
     * @return The new thread
     */
    override fun newThread(runnable: Runnable): Thread {
        val threadName = name + "-" + threadNumber.getAndIncrement()
        return Thread(group, runnable, threadName)
            .also {
                it.isDaemon = isDaemon
                it.priority = Thread.NORM_PRIORITY
                log.debug { "Create new thread. name=$threadName" }
            }
    }
}
