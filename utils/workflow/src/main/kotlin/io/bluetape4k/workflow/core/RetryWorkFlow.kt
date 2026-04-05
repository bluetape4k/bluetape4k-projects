package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedWork
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkFlow
import io.bluetape4k.workflow.api.WorkReport

/**
 * 작업 실패 시 [RetryPolicy]에 따라 재시도하는 워크플로입니다.
 *
 * 지수 백오프를 지원하며, [RetryPolicy.maxAttempts]번까지 시도합니다 (최초 실행 포함).
 * 각 재시도 사이에 [Thread.sleep]으로 대기합니다.
 *
 * ```kotlin
 * val flow = RetryWorkFlow(
 *     work = unreliableWork,
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
class RetryWorkFlow(
    private val work: Work,
    private val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
    private val flowName: String = "retry-flow",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        val workName = (work as? NamedWork)?.name ?: work.javaClass.simpleName
        log.debug { "$flowName 시작. work='$workName', retryPolicy=$retryPolicy" }

        var lastReport: WorkReport = WorkReport.Failure(context)
        var currentDelay = retryPolicy.delay

        for (attempt in 1..retryPolicy.maxAttempts) {
            log.debug { "$flowName: '$workName' 시도 #$attempt/${retryPolicy.maxAttempts}" }

            lastReport = runCatching { work.execute(context) }
                .getOrElse { e ->
                    log.debug { "$flowName: '$workName' 시도 #$attempt 예외 발생 - ${e.message}" }
                    WorkReport.Failure(context, e)
                }

            log.debug { "$flowName: '$workName' 시도 #$attempt 완료 - status=${lastReport.status}" }

            if (lastReport.isAborted || lastReport.isCancelled || lastReport.isSuccess) {
                log.debug { "$flowName 종료 (시도 #$attempt, status=${lastReport.status})" }
                return lastReport
            }

            if (attempt < retryPolicy.maxAttempts) {
                log.debug { "$flowName: 재시도 대기 $currentDelay" }
                Thread.sleep(currentDelay.inWholeMilliseconds)
                currentDelay = minOf(currentDelay * retryPolicy.backoffMultiplier, retryPolicy.maxDelay)
            }
        }

        log.debug { "$flowName 실패 - 최대 시도 횟수(${retryPolicy.maxAttempts}) 초과" }
        return lastReport
    }

    override fun toString(): String = "RetryWorkFlow($flowName, retryPolicy=$retryPolicy)"
}
