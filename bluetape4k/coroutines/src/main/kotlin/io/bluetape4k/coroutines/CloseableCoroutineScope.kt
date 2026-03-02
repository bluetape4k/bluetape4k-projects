package io.bluetape4k.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 닫기(close) 시 코루틴 작업을 함께 정리하는 [CoroutineScope] 기반 추상 클래스입니다.
 *
 * ## 동작/계약
 * - `close()`와 `clearJobs()`는 원자 플래그로 중복 호출을 방지하는 idempotent 동작입니다.
 * - `clearJobs()`는 자식 Job 취소 후 스코프 컨텍스트 자체를 취소합니다.
 * - 상태 조회 프로퍼티는 원자 플래그 값을 읽기만 하며 상태를 변경하지 않습니다.
 * - 별도 입력 검증은 없고 취소 과정의 예외는 코루틴 취소 규칙을 따릅니다.
 *
 * ```kotlin
 * val scope: CloseableCoroutineScope = object : CloseableCoroutineScope() {
 *     override val coroutineContext = kotlin.coroutines.EmptyCoroutineContext
 * }
 * scope.close()
 * // scope.scopeClosed == true
 * ```
 */
abstract class CloseableCoroutineScope: CoroutineScope, Closeable {

    companion object: KLoggingChannel()

    protected val closed = AtomicBoolean(false)
    protected val cancelled = AtomicBoolean(false)

    /**
     * 스코프가 `close()` 호출로 닫혔는지 반환합니다.
     *
     * ## 동작/계약
     * - 최초 `close()` 성공 후 `true`를 유지합니다.
     * - 조회 전용이며 내부 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val closed = scope.scopeClosed
     * // closed == false || true
     * ```
     */
    val scopeClosed: Boolean get() = closed.get()

    /**
     * 작업 취소 정리가 수행되었는지 반환합니다.
     *
     * ## 동작/계약
     * - `clearJobs()` 최초 성공 호출 이후 `true`를 유지합니다.
     * - 조회 전용이며 내부 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val cancelled = scope.scopeCancelled
     * // cancelled == false || true
     * ```
     */
    val scopeCancelled: Boolean get() = cancelled.get()

    /**
     * 스코프와 자식 코루틴을 취소합니다.
     *
     * ## 동작/계약
     * - 최초 한 번만 실제 취소를 수행하며 이후 호출은 무시됩니다.
     * - 먼저 자식 Job을 취소하고 이어서 스코프 컨텍스트를 취소합니다.
     * - 수신 객체의 취소 상태(`scopeCancelled`)를 변경합니다.
     *
     * ```kotlin
     * scope.clearJobs()
     * // scope.scopeCancelled == true
     * ```
     * @param cause 취소 원인으로 전달할 예외입니다.
     */
    fun clearJobs(cause: CancellationException? = null) {
        if (cancelled.compareAndSet(false, true)) {
            log.debug { "clearJobs: cause=$cause" }
            coroutineContext.cancelChildren(cause)
            coroutineContext.cancel(cause)
        }
    }

    /**
     * 스코프를 닫고 미정리 작업을 취소합니다.
     *
     * ## 동작/계약
     * - 최초 한 번만 닫힘 상태를 `true`로 바꾸고 [clearJobs]를 호출합니다.
     * - 이후 중복 `close()` 호출은 추가 작업 없이 반환합니다.
     * - 수신 객체의 닫힘 상태(`scopeClosed`)를 변경합니다.
     *
     * ```kotlin
     * scope.close()
     * // scope.scopeClosed == true
     * ```
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            clearJobs()
        }
    }
}
