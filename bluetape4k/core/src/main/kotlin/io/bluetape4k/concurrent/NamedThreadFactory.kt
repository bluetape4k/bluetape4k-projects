package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import java.util.concurrent.ThreadFactory

/**
 * 이름 접두사가 있는 스레드를 생성하는 [ThreadFactory] 구현체입니다.
 *
 * 생성되는 스레드 이름은 `$prefix-$number` 형태이며, 번호는 1부터 자동 증가합니다.
 *
 * ```kotlin
 * val factory = NamedThreadFactory("worker", isDaemon = true)
 *
 * val thread1 = factory.newThread { println("thread1 실행") }  // 이름: "worker-1"
 * val thread2 = factory.newThread { println("thread2 실행") }  // 이름: "worker-2"
 *
 * thread1.start()
 * thread2.start()
 * thread1.join()
 * thread2.join()
 * ```
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
        operator fun invoke(prefix: String? = DEFAULT_PREFIX, isDaemon: Boolean = false): ThreadFactory =
            NamedThreadFactory(prefix ?: DEFAULT_PREFIX, isDaemon)
    }

    val name: String = prefix

    val group: ThreadGroup by lazy { ThreadGroup(Thread.currentThread().threadGroup, name) }

    private val threadNumber = atomic(1)

    /**
     * 람다 블록을 실행하는 새 스레드를 생성합니다.
     *
     * ```kotlin
     * val factory = NamedThreadFactory("worker")
     * val thread = factory.newThread {
     *     println("현재 스레드: ${Thread.currentThread().name}") // "현재 스레드: worker-1"
     * }
     * thread.start()
     * thread.join()
     * ```
     *
     * @param body 스레드에서 실행할 람다
     * @return 새로운 스레드 객체
     */
    fun newThread(body: () -> Unit): Thread = newThread(Runnable(body))

    /**
     * [Runnable]을 실행하는 새 스레드를 생성합니다.
     *
     * ```kotlin
     * val factory = NamedThreadFactory("worker")
     * val thread = factory.newThread(Runnable {
     *     println("현재 스레드: ${Thread.currentThread().name}") // "현재 스레드: worker-1"
     * })
     * thread.start()
     * thread.join()
     * ```
     *
     * @param runnable 스레드에서 실행할 [Runnable]
     * @return 새로운 스레드 객체
     */
    override fun newThread(runnable: Runnable): Thread {
        val threadName = "$name-${threadNumber.getAndIncrement()}"
        return Thread(group, runnable, threadName)
            .also {
                it.isDaemon = isDaemon
                it.priority = Thread.NORM_PRIORITY
                log.debug { "Create new thread. name=$threadName" }
            }
    }
}
