package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import java.util.concurrent.ThreadFactory

/**
 * NamedThreadFactory
 *
 * @property prefix   The prefix of thread name
 * @property isDaemon The daemon flag of thread
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

    private val threadNumber = atomic(1)

    /**
     * Create a new thread
     *
     * @param body The body of thread
     * @return The new thread
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
