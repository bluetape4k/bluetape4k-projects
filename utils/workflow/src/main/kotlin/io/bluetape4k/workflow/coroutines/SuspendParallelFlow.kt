package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedSuspendWork
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.SuspendWorkFlow
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 코루틴 기반으로 작업 목록을 병렬로 실행하는 워크플로입니다.
 *
 * [coroutineScope]를 사용하여 구조화된 동시성을 보장합니다.
 * [policy]에 따라 실행 전략이 달라집니다:
 * - [ParallelPolicy.ALL]: 모든 작업 완료를 대기하며, 하나라도 예외를 발생시키면 나머지가 자동 취소됩니다.
 * - [ParallelPolicy.ANY]: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
 *
 * 결과 우선순위 (ALL): [Aborted][WorkReport.Aborted] > [Cancelled][WorkReport.Cancelled] >
 * [Failure][WorkReport.Failure] > [Success][WorkReport.Success]
 *
 * ```kotlin
 * // ALL 정책 (기본값)
 * val allFlow = SuspendParallelFlow(
 *     works = listOf(work1, work2, work3),
 *     policy = ParallelPolicy.ALL,
 * )
 * val report = allFlow.execute(context)
 *
 * // ANY 정책 — 첫 성공 즉시 반환
 * val anyFlow = SuspendParallelFlow(
 *     works = listOf(work1, work2, work3),
 *     policy = ParallelPolicy.ANY,
 * )
 * val winner = anyFlow.execute(context)
 * ```
 *
 * @property works 병렬 실행할 작업 목록
 * @property policy 병렬 실행 정책 (기본값: [ParallelPolicy.ALL])
 * @property flowName 워크플로 이름 (로깅용)
 */
class SuspendParallelFlow(
    private val works: List<SuspendWork>,
    private val policy: ParallelPolicy = ParallelPolicy.ALL,
    private val flowName: String = "suspend-parallel-flow",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        log.debug { "$flowName 시작. works=${works.size}, policy=$policy" }

        return when (policy) {
            ParallelPolicy.ALL -> executeAll(context)
            ParallelPolicy.ANY -> executeAny(context)
        }
    }

    /**
     * ALL 정책: 모든 작업 완료를 대기합니다.
     * [coroutineScope] + [awaitAll]을 사용하여 구조화된 동시성을 보장합니다.
     */
    private suspend fun executeAll(context: WorkContext): WorkReport {
        val reports = coroutineScope {
            works.map { work ->
                val workName = (work as? NamedSuspendWork)?.name ?: work.javaClass.simpleName
                async {
                    log.debug { "$flowName: '$workName' 병렬 실행 시작 (ALL)" }
                    val report = runCatching { work.execute(context) }
                        .getOrElse { e ->
                            log.debug { "$flowName: '$workName' 예외 발생 - ${e.message}" }
                            WorkReport.Failure(context, e)
                        }
                    log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }
                    report
                }
            }.awaitAll()
        }

        // 우선순위: ABORTED > CANCELLED > FAILED > SUCCESS
        return reports.firstOrNull { it.isAborted }
            ?: reports.firstOrNull { it.isCancelled }
            ?: reports.firstOrNull { it.isFailure }
            ?: run {
                log.debug { "$flowName 완료 - 모두 성공 (ALL)" }
                WorkReport.success(context)
            }
    }

    /**
     * ANY 정책: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
     * [Channel]을 통해 결과를 수집하며, 첫 [WorkReport.Success] 수신 시 모든 job을 취소합니다.
     */
    private suspend fun executeAny(context: WorkContext): WorkReport {
        return coroutineScope {
            val channel = Channel<WorkReport>(capacity = works.size)
            val jobs = works.map { work ->
                val workName = (work as? NamedSuspendWork)?.name ?: work.javaClass.simpleName
                launch {
                    log.debug { "$flowName: '$workName' 병렬 실행 시작 (ANY)" }
                    val report = runCatching { work.execute(context) }
                        .getOrElse { e ->
                            log.debug { "$flowName: '$workName' 예외 발생 - ${e.message}" }
                            WorkReport.Failure(context, e)
                        }
                    log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }
                    channel.trySend(report)
                }
            }

            val failedReports = mutableListOf<WorkReport>()
            var firstSuccess: WorkReport? = null
            var received = 0

            while (received < works.size && firstSuccess == null) {
                val report = channel.receive()
                received++
                if (report.isSuccess) {
                    firstSuccess = report
                    log.debug { "$flowName: 성공 수신 — 나머지 취소 (ANY)" }
                    jobs.forEach { it.cancel() }
                } else {
                    failedReports.add(report)
                }
            }
            channel.close()

            firstSuccess ?: run {
                log.debug { "$flowName: 모든 작업이 실패했습니다 (ANY)" }
                failedReports.firstOrNull { it.isAborted }
                    ?: failedReports.firstOrNull { it.isCancelled }
                    ?: failedReports.firstOrNull { it.isFailure }
                    ?: WorkReport.failure(context, RuntimeException("$flowName: 모든 작업이 실패했습니다"))
            }
        }
    }

    override fun toString(): String = "SuspendParallelFlow($flowName, works=${works.size}, policy=$policy)"
}
