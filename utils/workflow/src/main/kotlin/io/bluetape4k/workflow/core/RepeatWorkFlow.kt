package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedWork
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkFlow
import io.bluetape4k.workflow.api.WorkReport

/**
 * 반복 조건이 true인 동안 작업을 반복 실행하는 워크플로입니다.
 *
 * [repeatPredicate]가 true를 반환하는 동안 [work]를 계속 실행합니다.
 * [maxIterations]로 무한 루프를 방지합니다.
 *
 * ```kotlin
 * val flow = RepeatWorkFlow(
 *     work = incrementWork,
 *     repeatPredicate = { report -> report.isSuccess && report.context.get<Int>("count")!! < 10 },
 *     maxIterations = 100,
 * )
 * val report = flow.execute(context)
 * ```
 *
 * @property work 반복 실행할 작업
 * @property repeatPredicate 반복 조건 (true면 계속 반복)
 * @property maxIterations 최대 반복 횟수 (기본값: [Int.MAX_VALUE])
 * @property flowName 워크플로 이름 (로깅용)
 */
class RepeatWorkFlow(
    private val work: Work,
    private val repeatPredicate: (WorkReport) -> Boolean,
    private val maxIterations: Int = Int.MAX_VALUE,
    private val flowName: String = "repeat-flow",
): WorkFlow {

    companion object: KLogging()

    override fun execute(context: WorkContext): WorkReport {
        val workName = (work as? NamedWork)?.name ?: work.javaClass.simpleName
        log.debug { "$flowName 시작. work='$workName', maxIterations=$maxIterations" }

        var report: WorkReport = WorkReport.Success(context)
        var iteration = 0

        do {
            iteration++
            log.debug { "$flowName: '$workName' 반복 실행 #$iteration" }

            report = try {
                work.execute(context)
            } catch (e: Exception) {
                log.debug { "$flowName: '$workName' 반복 #$iteration 예외 발생 - ${e.message}" }
                WorkReport.Failure(context, e)
            }

            log.debug { "$flowName: '$workName' 반복 #$iteration 완료 - status=${report.status}" }

            if (report.isAborted || report.isCancelled) {
                log.debug { "[$flowName] Stopping repeat: ${report.status}" }
                return report
            }
        } while (iteration < maxIterations && repeatPredicate(report))

        log.debug { "$flowName 완료. 총 반복=${iteration}, 최종 status=${report.status}" }
        return report
    }

    override fun toString(): String = "RepeatWorkFlow($flowName, maxIterations=$maxIterations)"
}
