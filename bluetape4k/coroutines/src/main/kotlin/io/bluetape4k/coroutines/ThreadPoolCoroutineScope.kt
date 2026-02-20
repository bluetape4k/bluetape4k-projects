package io.bluetape4k.coroutines

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext

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
): CloseableCoroutineScope() {

    init {
        poolSize.requirePositiveNumber("poolSize")
    }

    private val job: CompletableJob = SupervisorJob()
    private val dispatcher: ExecutorCoroutineDispatcher = newFixedThreadPoolContext(poolSize, name)

    override val coroutineContext: CoroutineContext = dispatcher + job

    /**
     * ThreadPoolCoroutineScope를 종료합니다.
     */
    override fun close() {
        clearJobs()
        dispatcher.close()
    }

    override fun toString(): String =
        "ThreadPoolCoroutineScope(coroutineContext=$coroutineContext)"
}
