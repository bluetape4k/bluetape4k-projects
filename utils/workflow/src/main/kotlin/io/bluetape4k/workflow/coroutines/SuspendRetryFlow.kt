package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedSuspendWork
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.SuspendWorkFlow
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/**
 * 작업 실패 시 [RetryPolicy]에 따라 재시도하는 코루틴 워크플로입니다.
 *
 * 지수 백오프를 지원하며, [RetryPolicy.maxAttempts]번까지 시도합니다 (최초 실행 포함).
 * 각 재시도 사이에 [kotlinx.coroutines.delay]로 비동기 대기합니다 ([Thread.sleep] 미사용).
 *
 * [Aborted][WorkReport.Aborted] / [Cancelled][WorkReport.Cancelled] / [Success][WorkReport.Success]
 * 결과는 즉시 반환됩니다.
 *
 * ```kotlin
 * val flow = SuspendRetryFlow(
 *     work = unreliableSuspendWork,
 *     retryPolicy = RetryPolicy(
 *         maxAttempts = 3,
 *         delay = 100.milliseconds,
 *         backoffMultiplier = 2.0,
 *         maxDelay = 1.minutes,
 *     ),
 * )
 * val report = flow.execute(context)
 * ```
 *
 * @property work 실행할 작업
 * @property retryPolicy 재시도 정책 (기본값: [RetryPolicy.DEFAULT])
 * @property flowName 워크플로 이름 (로깅용)
 */
@Suppress("DuplicatedCode")
class SuspendRetryFlow(
    private val work: SuspendWork,
    private val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    private val flowName: String = "suspend-retry-flow",
): SuspendWorkFlow {

    companion object: KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        val workName = (work as? NamedSuspendWork)?.name ?: work.javaClass.simpleName
        log.debug { "$flowName 시작. work='$workName', retryPolicy=$retryPolicy" }

        var lastReport: WorkReport = WorkReport.Failure(context)
        var currentDelay = retryPolicy.delay

        for (attempt in 1..retryPolicy.maxAttempts) {
            currentCoroutineContext().ensureActive()  // 취소 전파

            log.debug { "$flowName: '$workName' 시도 #$attempt/${retryPolicy.maxAttempts}" }

            lastReport = runCatching { work.execute(context) }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    log.debug { "$flowName: '$workName' 시도 #$attempt 예외 발생 - ${e.message}" }
                    WorkReport.Failure(context, e)
                }

            log.debug { "$flowName: '$workName' 시도 #$attempt 완료 - status=${lastReport.status}" }

            // ABORTED / CANCELLED / SUCCESS: 즉시 반환
            if (lastReport.isAborted || lastReport.isCancelled || lastReport.isSuccess) {
                log.debug { "$flowName 종료 (시도 #$attempt, status=${lastReport.status})" }
                return lastReport
            }

            // 마지막 시도가 아니면 지수 백오프 대기
            if (attempt < retryPolicy.maxAttempts) {
                log.debug { "$flowName: 재시도 대기 $currentDelay" }
                delay(currentDelay)
                currentDelay = minOf(currentDelay * retryPolicy.backoffMultiplier, retryPolicy.maxDelay)
            }
        }

        log.debug { "$flowName 실패 - 최대 시도 횟수(${retryPolicy.maxAttempts}) 초과" }
        return lastReport
    }

    override fun toString(): String = "SuspendRetryFlow($flowName, retryPolicy=$retryPolicy)"
}
