package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory


/**
 * [StructuredTaskScope.ShutdownOnFailure] 를 사용하여 구조화된 작업을 수행합니다.
 *
 * 작업 범위 내에서 다수의 서브 작업을 실행하고, 모든 작업이 완료되도록 대기한 후 결과를 반환한다.
 * 만약 서브 작업 중 하나라도 실패한다면, 즉시 모든 서브 작업을 중단하고, 예외를 던진다.
 *
 * 참고: Kotlin Coroutines 의 [kotlinx.coroutines.coroutineScope]와 작업 방식은 같다.
 *
 * ```
 * val result = structuredTaskScope { scope ->
 *     val result1 = scope.fork { ... }
 *     val result2 = scope.fork { ... }
 *
 *     scope.join().throwIfFailed()
 *
 *     Result(result1, result2)
 * }
 * ```
 * @param T
 * @param block
 * @receiver
 * @return
 */
inline fun <T> structuredTaskScopeAll(
    name: String? = null,
    factory: ThreadFactory = Thread.ofVirtual().factory(),
    block: (scope: StructuredTaskScope.ShutdownOnFailure) -> T,
): T {
    return StructuredTaskScope.ShutdownOnFailure(name, factory).use { scope ->
        block(scope)
    }
}


/**
 * [StructuredTaskScope.ShutdownOnSuccess] 를 사용하여 구조화된 작업을 수행합니다.
 *
 * 작업 범위 내에서 다수의 서브 작업을 실행하고, 첫번째 성공한 작업의 결과를 반환하고, 나머지 서브 작업은 중단하도록 한다.
 *
 * 병렬 프로그래밍의 투기적 실행 (여러개를 동시에 실행하고, 첫번째 결과를 취하고, 나머지 작업은 버린다) 또는 ML 의 앙상블 기법과 같다.
 *
 * ```
 * val result = structuredTaskScopeFirst<String> { scope ->
 *     scope.fork {
 *          Thread.sleep(100)
 *          "result1"
 *     }
 *     scope.fork {
 *          Thread.sleep(200)
 *          "result2"
 *     }
 *
 *     scope.join()
 *     scope.result { ExecutionException(it) }
 * }
 * // 먼저 완료되는 작업의 결과를 반환한다.
 * // result is "result1"
 * ```
 *
 * @param T
 * @param block
 * @receiver
 * @return
 */
inline fun <T> structuredTaskScopeFirst(
    name: String? = null,
    factory: ThreadFactory = Thread.ofVirtual().factory(),
    block: (scope: StructuredTaskScope.ShutdownOnSuccess<T>) -> T,
): T {
    return StructuredTaskScope.ShutdownOnSuccess<T>(name, factory).use { scope ->
        block(scope)
    }
}
