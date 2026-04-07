package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedWork
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkFlow
import io.bluetape4k.workflow.api.WorkReport

/**
 * 조건(predicate)에 따라 분기 실행하는 워크플로입니다.
 *
 * [predicate]가 true를 반환하면 [thenWork]를 실행하고,
 * false를 반환하면 [otherwiseWork]를 실행합니다.
 * [otherwiseWork]가 null이면 [WorkReport.Success]를 반환합니다.
 *
 * ```kotlin
 * val flow = ConditionalWorkFlow(
 *     predicate = { ctx -> ctx.get<Boolean>("valid") == true },
 *     thenWork = processWork,
 *     otherwiseWork = rejectWork,
 * )
 * val report = flow.execute(context)
 * ```
 *
 * @property predicate 분기 조건 함수
 * @property thenWork 조건이 true일 때 실행할 작업
 * @property otherwiseWork 조건이 false일 때 실행할 작업 (null이면 Success 반환)
 * @property flowName 워크플로 이름 (로깅용)
 */
class ConditionalWorkFlow(
    private val predicate: (WorkContext) -> Boolean,
    private val thenWork: Work,
    private val otherwiseWork: Work? = null,
    private val flowName: String = "conditional-flow",
): WorkFlow {

    companion object: KLogging()

    override fun execute(context: WorkContext): WorkReport {
        log.debug { "$flowName 시작" }

        val conditionResult = predicate(context)
        log.debug { "$flowName: 조건 평가 결과=$conditionResult" }

        return if (conditionResult) {
            val workName = (thenWork as? NamedWork)?.name ?: thenWork.javaClass.simpleName
            log.debug { "$flowName: thenWork '$workName' 실행" }
            val report = thenWork.execute(context)
            log.debug { "$flowName: thenWork '$workName' 완료 - status=${report.status}" }
            report
        } else {
            if (otherwiseWork != null) {
                val workName = (otherwiseWork as? NamedWork)?.name ?: otherwiseWork.javaClass.simpleName
                log.debug { "$flowName: otherwiseWork '$workName' 실행" }
                val report = otherwiseWork.execute(context)
                log.debug { "$flowName: otherwiseWork '$workName' 완료 - status=${report.status}" }
                report
            } else {
                log.debug { "$flowName: otherwiseWork 없음 - Success 반환" }
                WorkReport.Success(context)
            }
        }
    }

    override fun toString(): String = "ConditionalWorkFlow($flowName)"
}
