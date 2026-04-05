package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedSuspendWork
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.SuspendWorkFlow
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/**
 * 조건이 만족하는 동안 작업을 반복 실행하는 코루틴 워크플로입니다.
 *
 * [repeatPredicate]가 `true`를 반환하면 다음 반복을 계속하고,
 * `false`를 반환하면 마지막 결과를 반환합니다.
 * [maxIterations]에 도달하면 반복을 중단합니다.
 *
 * [Aborted][WorkReport.Aborted] / [Cancelled][WorkReport.Cancelled] 결과는
 * [repeatPredicate]와 무관하게 즉시 반복을 중단합니다 (while 루프의 `break`에 해당).
 *
 * ```kotlin
 * var count = 0
 * val flow = SuspendRepeatFlow(
 *     work = SuspendWork { ctx ->
 *         count++
 *         ctx["count"] = count
 *         WorkReport.success(ctx)
 *     },
 *     repeatPredicate = { report -> report.isSuccess },
 *     maxIterations = 5,
 *     repeatDelay = 100.milliseconds,
 * )
 * val report = flow.execute(context)
 * ```
 *
 * @property work 반복 실행할 작업
 * @property repeatPredicate 반복 조건 (true면 계속, false면 중단)
 * @property maxIterations 최대 반복 횟수 (기본값: [Int.MAX_VALUE])
 * @property repeatDelay 반복 간 대기 시간 (기본값: [Duration.ZERO])
 * @property flowName 워크플로 이름 (로깅용)
 */
class SuspendRepeatFlow(
    private val work: SuspendWork,
    private val repeatPredicate: suspend (WorkReport) -> Boolean,
    private val maxIterations: Int = Int.MAX_VALUE,
    private val repeatDelay: Duration = Duration.ZERO,
    private val flowName: String = "suspend-repeat-flow",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        val workName = (work as? NamedSuspendWork)?.name ?: work.javaClass.simpleName
        log.debug { "$flowName 시작. work='$workName', maxIterations=$maxIterations, repeatDelay=$repeatDelay" }

        var iteration = 0
        var lastReport: WorkReport = WorkReport.success(context)

        do {
            currentCoroutineContext().ensureActive()  // 취소 전파

            log.debug { "$flowName: '$workName' 반복 #${iteration + 1}" }

            lastReport = runCatching { work.execute(context) }
                .getOrElse { e ->
                    log.debug { "$flowName: '$workName' 반복 #${iteration + 1} 예외 발생 - ${e.message}" }
                    WorkReport.Failure(context, e)
                }

            log.debug { "$flowName: '$workName' 반복 #${iteration + 1} 완료 - status=${lastReport.status}" }

            // ABORTED / CANCELLED: 즉시 중단 (while의 break)
            if (lastReport.isAborted || lastReport.isCancelled) {
                log.debug { "$flowName: ${lastReport.status} - 반복 즉시 중단" }
                return lastReport
            }

            iteration++

            if (iteration < maxIterations && repeatPredicate(lastReport) && repeatDelay > Duration.ZERO) {
                log.debug { "$flowName: 반복 대기 $repeatDelay" }
                delay(repeatDelay)
            }
        } while (iteration < maxIterations && repeatPredicate(lastReport))

        log.debug { "$flowName 완료 - 총 ${iteration}회 반복" }
        return lastReport
    }

    override fun toString(): String =
        "SuspendRepeatFlow($flowName, maxIterations=$maxIterations, repeatDelay=$repeatDelay)"
}
