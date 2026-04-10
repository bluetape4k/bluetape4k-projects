package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.SuspendWorkFlow
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.CancellationException

/**
 * 조건에 따라 분기 실행하는 코루틴 워크플로입니다.
 *
 * [predicate]가 `true`이면 [thenWork]를, `false`이면 [otherwiseWork]를 실행합니다.
 * [otherwiseWork]가 `null`이면 조건 불일치 시 [WorkReport.Success]를 반환합니다.
 *
 * ```kotlin
 * val flow = SuspendConditionalFlow(
 *     predicate = { ctx -> ctx.get<Boolean>("enabled") == true },
 *     thenWork = SuspendWork { ctx -> WorkReport.success(ctx) },
 *     otherwiseWork = SuspendWork { ctx -> WorkReport.failure(ctx) },
 * )
 * val report = flow.execute(context)
 * ```
 *
 * @property predicate 분기 조건 (suspend 함수)
 * @property thenWork 조건이 참일 때 실행할 작업
 * @property otherwiseWork 조건이 거짓일 때 실행할 작업 (null이면 [WorkReport.Success] 반환)
 * @property flowName 워크플로 이름 (로깅용)
 */
class SuspendConditionalFlow(
    private val predicate: suspend (WorkContext) -> Boolean,
    private val thenWork: SuspendWork,
    private val otherwiseWork: SuspendWork? = null,
    private val flowName: String = "suspend-conditional-flow",
): SuspendWorkFlow {

    companion object: KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        val condition = runCatching { predicate(context) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                log.debug { "$flowName: 조건 평가 중 예외 발생 - ${e.message}" }
                return WorkReport.Failure(context, e)
            }

        log.debug { "$flowName: 조건 평가 결과=$condition" }

        return if (condition) {
            log.debug { "$flowName: thenWork 실행" }
            runCatching { thenWork.execute(context) }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    log.debug { "$flowName: thenWork 예외 발생 - ${e.message}" }
                    WorkReport.Failure(context, e)
                }
        } else {
            val elseWork = otherwiseWork
            if (elseWork != null) {
                log.debug { "$flowName: otherwiseWork 실행" }
                runCatching { elseWork.execute(context) }
                    .getOrElse { e ->
                        if (e is CancellationException) throw e
                        log.debug { "$flowName: otherwiseWork 예외 발생 - ${e.message}" }
                        WorkReport.Failure(context, e)
                    }
            } else {
                log.debug { "$flowName: 조건 불일치, otherwiseWork 없음 - Success 반환" }
                WorkReport.success(context)
            }
        }
    }

    override fun toString(): String = "SuspendConditionalFlow($flowName)"
}
