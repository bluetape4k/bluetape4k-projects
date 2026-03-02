package io.bluetape4k.coroutines

import io.bluetape4k.support.requirePositiveNumber
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlin.coroutines.CoroutineContext

/**
 * 고정 크기 스레드풀 dispatcher를 사용하는 [CloseableCoroutineScope]입니다.
 *
 * ## 동작/계약
 * - 생성 시 `poolSize.requirePositiveNumber("poolSize")`를 검증하며 0 이하이면 `IllegalArgumentException`이 발생합니다.
 * - 내부적으로 `newFixedThreadPoolContext(poolSize, name)`를 생성하고 `SupervisorJob`과 결합해 사용합니다.
 * - `close()`는 코루틴 취소 후 dispatcher를 닫아 스레드풀 자원을 해제합니다.
 *
 * ```kotlin
 * val scope = ThreadPoolCoroutineScope(poolSize = 2, name = "worker")
 * // scope.coroutineContext[Job] != null
 * scope.close()
 * // scope.scopeCancelled == true
 * ```
 *
 * @param poolSize 생성할 고정 스레드 수입니다. 0 이하이면 예외가 발생합니다.
 * @param name 생성할 스레드 이름 prefix입니다.
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

    /**
     * 이 스코프가 사용하는 코루틴 컨텍스트입니다.
     *
     * ## 동작/계약
     * - 고정 스레드풀 dispatcher와 `SupervisorJob`의 합성 컨텍스트를 반환합니다.
     * - 조회 시 상태를 변경하지 않으며 동일 컨텍스트 인스턴스를 반환합니다.
     */
    override val coroutineContext: CoroutineContext = dispatcher + job

    /**
     * 스코프를 닫고 코루틴 및 스레드풀 자원을 해제합니다.
     *
     * ## 동작/계약
     * - 먼저 [clearJobs]로 자식 작업을 취소합니다.
     * - 이어서 `dispatcher.close()`를 호출해 풀을 종료합니다.
     * - 중복 호출 시 취소는 idempotent이고 dispatcher의 중복 close는 구현체 정책을 따릅니다.
     */
    override fun close() {
        clearJobs()
        dispatcher.close()
    }

    override fun toString(): String =
        "ThreadPoolCoroutineScope(coroutineContext=$coroutineContext)"
}
