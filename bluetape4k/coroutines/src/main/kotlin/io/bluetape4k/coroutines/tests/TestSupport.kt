package io.bluetape4k.coroutines.tests

import io.bluetape4k.support.forEachCatching
import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 단일 스레드 dispatcher를 만들어 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 내부적으로 single-thread executor를 생성해 `asCoroutineDispatcher()`로 전달합니다.
 * - 블록 실행 후 `shutdown`/`awaitTermination`으로 executor 종료를 시도합니다.
 * - 종료 과정 예외는 `runCatching`으로 무시됩니다.
 *
 * ```kotlin
 * withSingleThread { dispatcher ->
 *   withContext(dispatcher) { /* test */ }
 * }
 * // 블록 종료 후 executor 종료 시도
 * ```
 *
 * @param block 생성된 dispatcher로 실행할 테스트 블록입니다.
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
 * 지정한 개수의 단일 스레드 dispatcher들을 만들어 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - `parallelism.requirePositiveNumber("parallelism")`를 검증하며 0 이하면 예외가 발생합니다.
 * - `parallelism` 개수만큼 executor를 만들고 dispatcher 리스트로 블록에 전달합니다.
 * - 종료 시 각 executor에 대해 `shutdown`/`awaitTermination`을 시도합니다.
 *
 * ```kotlin
 * withParallels(2) { dispatchers ->
 *   // dispatchers.size == 2
 * }
 * ```
 *
 * @param parallelism 생성할 dispatcher 개수입니다. 0 이하면 예외가 발생합니다.
 * @param block 생성된 dispatcher 목록으로 실행할 테스트 블록입니다.
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
