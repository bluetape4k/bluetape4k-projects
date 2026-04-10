package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [WorkReport]를 [Flow]로 변환하는 유틸리티 함수들을 제공합니다.
 */
object WorkReportFlowSupport: KLogging()

/**
 * 여러 [SuspendWork]를 순차적으로 실행하며 각 결과를 [Flow]로 emit합니다.
 *
 * 각 작업의 결과가 발생할 때마다 수집할 수 있어, 전체 완료를 기다리지 않고
 * 실시간으로 결과를 처리할 수 있습니다.
 *
 * ```kotlin
 * workReportFlow(listOf(work1, work2, work3), context)
 *     .collect { report ->
 *         println("결과: ${report.status}")
 *     }
 * ```
 *
 * @param works 순차 실행할 작업 목록
 * @param context 실행 컨텍스트
 * @return 각 작업의 [WorkReport]를 emit하는 [Flow]
 */
fun workReportFlow(
    works: List<SuspendWork>,
    context: WorkContext,
): Flow<WorkReport> = flow {
    for (work in works) {
        val report = runCatching { work.execute(context) }
            .getOrElse { e ->
                if (e is CancellationException) throw e
                WorkReport.Failure(context, e)
            }
        emit(report)
    }
}

/**
 * 단일 [SuspendWork]의 실행 결과를 [Flow]로 변환합니다.
 *
 * ```kotlin
 * val work = SuspendWork { ctx -> WorkReport.success(ctx) }
 * work.executeAsFlow(context)
 *     .collect { report -> println("결과: ${report.status}") }
 * ```
 *
 * @param context 실행 컨텍스트
 * @return 실행 결과 [WorkReport]를 emit하는 [Flow]
 */
fun SuspendWork.executeAsFlow(context: WorkContext): Flow<WorkReport> = flow {
    val report = runCatching { execute(context) }
        .getOrElse { e ->
            if (e is CancellationException) throw e
            WorkReport.Failure(context, e)
        }
    emit(report)
}
