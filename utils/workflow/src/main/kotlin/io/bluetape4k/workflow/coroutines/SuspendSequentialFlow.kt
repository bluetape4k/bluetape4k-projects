package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.NamedSuspendWork
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.SuspendWorkFlow
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * 코루틴 기반으로 작업 목록을 순차적으로 실행하는 워크플로입니다.
 *
 * [errorStrategy]에 따라 에러 발생 시 동작이 달라집니다:
 * - [ErrorStrategy.STOP]: 첫 번째 실패 시 즉시 중단하고 [WorkReport.Failure]를 반환합니다.
 * - [ErrorStrategy.CONTINUE]: 실패를 누적하고 다음 작업을 계속 실행합니다.
 *   실패가 하나라도 있으면 [WorkReport.PartialSuccess]를, 모두 성공이면 [WorkReport.Success]를 반환합니다.
 *
 * [Aborted][WorkReport.Aborted] / [Cancelled][WorkReport.Cancelled] 결과는 [ErrorStrategy]와 무관하게
 * 즉시 워크플로를 중단합니다 (while 루프의 `break`에 해당).
 *
 * ```kotlin
 * val flow = SuspendSequentialFlow(
 *     works = listOf(work1, work2, work3),
 *     errorStrategy = ErrorStrategy.CONTINUE,
 * )
 * val report = flow.execute(context)
 * when (report) {
 *     is WorkReport.Success        -> println("모두 성공")
 *     is WorkReport.PartialSuccess -> println("${report.failedReports.size}개 실패")
 *     is WorkReport.Failure        -> println("실패: ${report.error?.message}")
 *     is WorkReport.Aborted        -> println("중단: ${report.reason}")
 *     is WorkReport.Cancelled      -> println("취소: ${report.reason}")
 * }
 * ```
 *
 * @property works 순차 실행할 작업 목록
 * @property errorStrategy 에러 발생 시 동작 전략 (기본값: [ErrorStrategy.STOP])
 * @property flowName 워크플로 이름 (로깅용)
 */
class SuspendSequentialFlow(
    private val works: List<SuspendWork>,
    private val errorStrategy: ErrorStrategy = ErrorStrategy.STOP,
    private val flowName: String = "suspend-sequential-flow",
): SuspendWorkFlow {

    companion object: KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        log.debug { "$flowName 시작. works=${works.size}, errorStrategy=$errorStrategy" }

        val failedReports = mutableListOf<WorkReport>()

        for (work in works) {
            currentCoroutineContext().ensureActive()  // 취소 전파

            val workName = (work as? NamedSuspendWork)?.name ?: work.javaClass.simpleName
            log.debug { "$flowName: '$workName' 실행 시작" }

            val report = runCatching { work.execute(context) }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    log.debug { "$flowName: '$workName' 예외 발생 - ${e.message}" }
                    WorkReport.Failure(context, e)
                }

            log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }

            // ABORTED / CANCELLED: ErrorStrategy 무관하게 즉시 중단 (while의 break)
            if (report.isAborted) {
                log.debug { "$flowName: ABORTED by work. 플로우 즉시 중단." }
                return report
            }
            if (report.isCancelled) {
                log.debug { "$flowName: CANCELLED. 플로우 즉시 중단." }
                return report
            }

            if (report.isFailure) {
                when (errorStrategy) {
                    ErrorStrategy.STOP -> {
                        log.debug { "$flowName: STOP 전략 - 즉시 중단" }
                        return report
                    }

                    ErrorStrategy.CONTINUE -> {
                        log.debug { "$flowName: CONTINUE 전략 - 실패 누적 (누적=${failedReports.size + 1})" }
                        failedReports.add(report)
                    }
                }
            }
        }

        return if (failedReports.isNotEmpty()) {
            log.debug { "$flowName 완료 - 부분 성공 (실패=${failedReports.size})" }
            WorkReport.partialSuccess(context, failedReports)
        } else {
            log.debug { "$flowName 완료 - 모두 성공" }
            WorkReport.success(context)
        }
    }

    override fun toString(): String =
        "SuspendSequentialFlow($flowName, works=${works.size}, errorStrategy=$errorStrategy)"
}
