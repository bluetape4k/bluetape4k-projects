package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.newFixedThreadPoolContext
import java.io.Closeable
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Custom ThreadPool을 사용하는 CoroutineScope를 생성합니다.
 *
 * ```
 * val scope = ThreadPoolCoroutineScope()
 * val jobs = List(100) {
 *      scope.launch {
 *          // 작업 수행
 *          delay(i*10 + 10)
 *          println("Job $it")
 *      }
 * }
 * jobs.joinAll()
 * ```
 *
 * @param poolSize ThreadPool의 크기. default is [Runtime.getRuntime].availableProcessors()
 * @param name ThreadPool의 이름. default is "ThreadPoolCoroutineScope"
 */
class ThreadPoolCoroutineScope(
    poolSize: Int = Runtime.getRuntime().availableProcessors(),
    name: String = "ThreadPoolCoroutineScope",
): CoroutineScope, Closeable {

    private val job = SupervisorJob()
    private val dispatcher = newFixedThreadPoolContext(poolSize, name)

    override val coroutineContext: CoroutineContext = dispatcher + job

    /**
     * 자식의 모든 Job을 취소합니다.
     *
     * @param cause 취소 사유에 해당하는 예외정보. default is null
     */
    fun clearJobs(cause: CancellationException? = null) {
        coroutineContext.cancelChildren(cause)
    }

    /**
     * ThreadPoolCoroutineScope를 종료합니다.
     */
    override fun close() {
        dispatcher.close()
    }

    override fun toString(): String =
        "ThreadPoolCoroutineScope(coroutineContext=$coroutineContext)"
}
