package io.bluetape4k.coroutines.tests

import io.bluetape4k.support.forEachCatching
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Coroutines 작업을 이해하기 쉽고, 테스트하기 쉽게 Single Thread에서 실행되도록 합니다.
 *
 * ```
 * withSingleThread { dispatcher ->
 *      val subject = PublishSubject<Int>()
 *      val n = 10_000
 *      val counter = AtomicInteger(0)
 *
 *      val job = launch(dispatcher) {
 *          subject.collect {
 *              counter.lazySet(counter.value + 1)
 *          }
 *      }
 *      ...
 * }
 * ```
 *
 * Coroutines 공식 라이브러리에서 제공하는 `newSingleThreadContext` 를 사용하기를 추천합니다.
 * ```
 * withContext(newSingleThreadContext("Name1")) {
 *     ...
 * }
 * ```
 *
 * @see kotlinx.coroutines.newSingleThreadContext
 * @see kotlinx.coroutines.newFixedThreadPoolContext
 */
suspend inline fun withSingleThread(crossinline block: suspend (executor: CoroutineDispatcher) -> Unit) {
    val executor = Executors.newSingleThreadExecutor()
    try {
        block(executor.asCoroutineDispatcher())
    } finally {
        runCatching {
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.SECONDS)
        }
    }
}

/**
 * 특정 수의 Thread 로 Coroutines 를 실행합니다
 *
 * ```
 * withParallels(4) { dispatchers ->
 *      intArrayOf(1, 2, 3, 4, 5)
 *          .asFlow()
 *          .parallel(dispatchers.size) { dispatchers[it] }
 *          .sequential()
 *          .assertResultSet(1, 2, 3, 4, 5)
 * }
 * ```
 *
 * @param parallelism 병렬 수행 갯수
 * @param block 수행할 코드 블럭
 */
suspend inline fun withParallels(
    parallelism: Int = Runtime.getRuntime().availableProcessors(),
    crossinline block: suspend (executors: List<CoroutineDispatcher>) -> Unit,
) {
    parallelism.requirePositiveNumber("parallelism")

    val executors = Array(parallelism) { Executors.newSingleThreadExecutor() }

    try {
        block(executors.map { it.asCoroutineDispatcher() })
    } finally {
        executors.forEachCatching {
            it.shutdown()
            it.awaitTermination(1, TimeUnit.SECONDS)
        }
    }
}
