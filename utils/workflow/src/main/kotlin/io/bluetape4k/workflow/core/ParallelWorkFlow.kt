package io.bluetape4k.workflow.core

import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.NamedWork
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkFlow
import io.bluetape4k.workflow.api.WorkReport
import java.time.Instant
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 작업 목록을 병렬로 실행하는 워크플로입니다.
 *
 * [StructuredTaskScopes]를 사용하여 구조화된 동시성을 보장합니다.
 * [policy]에 따라 실행 전략이 달라집니다:
 * - [ParallelPolicy.ALL]: 모든 작업 완료를 대기하며, 하나라도 실패 시 나머지를 취소합니다.
 * - [ParallelPolicy.ANY]: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
 *
 * ```kotlin
 * // ALL 정책 (기본값)
 * val allFlow = ParallelWorkFlow(
 *     works = listOf(work1, work2, work3),
 *     policy = ParallelPolicy.ALL,
 *     timeout = 30.seconds,
 * )
 * val report = allFlow.execute(context)
 *
 * // ANY 정책 — 첫 성공 즉시 반환
 * val anyFlow = ParallelWorkFlow(
 *     works = listOf(work1, work2, work3),
 *     policy = ParallelPolicy.ANY,
 * )
 * val winner = anyFlow.execute(context)
 * ```
 *
 * @property works 병렬 실행할 작업 목록
 * @property policy 병렬 실행 정책 (기본값: [ParallelPolicy.ALL])
 * @property timeout 전체 실행 타임아웃 (기본값: 1분)
 * @property flowName 워크플로 이름 (로깅용)
 */
class ParallelWorkFlow(
    private val works: List<Work>,
    private val policy: ParallelPolicy = ParallelPolicy.ALL,
    private val timeout: Duration = 1.minutes,
    private val flowName: String = "parallel-flow",
): WorkFlow {

    companion object: KLogging()

    override fun execute(context: WorkContext): WorkReport {
        log.debug { "$flowName 시작. works=${works.size}, policy=$policy, timeout=$timeout" }

        return when (policy) {
            ParallelPolicy.ALL -> executeAll(context)
            ParallelPolicy.ANY -> executeAny(context)
        }
    }

    /**
     * ALL 정책: 모든 작업 완료를 대기합니다. 하나라도 실패 시 나머지를 취소합니다.
     *
     * [StructuredTaskScopes.all] (ShutdownOnFailure)을 사용합니다.
     */
    private fun executeAll(context: WorkContext): WorkReport {
        val factory = Thread.ofVirtual().name("$flowName-", 0).factory()
        val deadline = Instant.now().plusMillis(timeout.inWholeMilliseconds)

        return try {
            StructuredTaskScopes.all(name = flowName, factory = factory) { scope ->
                val tasks = works.map { work ->
                    val workName = (work as? NamedWork)?.name ?: work.javaClass.simpleName
                    scope.fork {
                        log.debug { "$flowName: '$workName' 병렬 실행 시작 (ALL)" }
                        val report = runCatching { work.execute(context) }
                            .getOrElse { e ->
                                log.debug { "$flowName: '$workName' 예외 발생 - ${e.message}" }
                                WorkReport.Failure(context, e)
                            }
                        log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }
                        report
                    }
                }

                scope.joinUntil(deadline).throwIfFailed { e ->
                    log.debug { "$flowName: throwIfFailed 감지 - ${e.message}" }
                }

                val reports = tasks.map { task ->
                    runCatching { task.get() }
                        .getOrElse { e -> WorkReport.Failure(context, e) }
                }

                // 우선순위: ABORTED > CANCELLED > FAILED > SUCCESS
                reports.firstOrNull { it.isAborted }
                    ?: reports.firstOrNull { it.isCancelled }
                    ?: reports.firstOrNull { it.isFailure }
                    ?: run {
                        log.debug { "$flowName 완료 - 모두 성공 (ALL)" }
                        WorkReport.success(context)
                    }
            }
        } catch (e: TimeoutException) {
            log.debug { "$flowName: timeout 초과 ($timeout) - Cancelled 반환" }
            WorkReport.Cancelled(context)
        }
    }

    /**
     * ANY 정책: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
     *
     * [StructuredTaskScopes.any] (ShutdownOnSuccess)를 사용합니다.
     * Success가 아닌 결과는 예외로 래핑하여 ShutdownOnSuccess가 "성공"으로 간주하지 않도록 합니다.
     */
    private fun executeAny(context: WorkContext): WorkReport {
        val factory = Thread.ofVirtual().name("$flowName-", 0).factory()
        val failedReports = java.util.concurrent.ConcurrentLinkedQueue<WorkReport>()

        return runCatching {
            StructuredTaskScopes.any<WorkReport>(name = flowName, factory = factory) { scope ->
                works.forEach { work ->
                    val workName = (work as? NamedWork)?.name ?: work.javaClass.simpleName
                    scope.fork {
                        log.debug { "$flowName: '$workName' 병렬 실행 시작 (ANY)" }
                        val report = runCatching { work.execute(context) }
                            .getOrElse { e ->
                                log.debug { "$flowName: '$workName' 예외 발생 - ${e.message}" }
                                WorkReport.Failure(context, e)
                            }
                        log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }

                        if (report.isSuccess) {
                            log.debug { "$flowName: '$workName' 성공 — 나머지 취소 (ANY)" }
                            report
                        } else {
                            failedReports.add(report)
                            throw WorkNotSuccessException(report)
                        }
                    }
                }

                scope.join().result { e ->
                    RuntimeException("$flowName: 모든 작업이 실패했습니다", e)
                }
            }
        }.getOrElse {
            // 모두 실패한 경우 failedReports에서 우선순위로 반환
            failedReports.firstOrNull { it.isAborted }
                ?: failedReports.firstOrNull { it.isCancelled }
                ?: failedReports.firstOrNull { it.isFailure }
                ?: WorkReport.failure(context, RuntimeException("$flowName: 모든 작업이 실패했습니다"))
        }
    }

    override fun toString(): String =
        "ParallelWorkFlow($flowName, works=${works.size}, policy=$policy, timeout=$timeout)"
}

/**
 * [ParallelPolicy.ANY] 정책에서 성공이 아닌 [WorkReport]를 예외로 래핑하는 내부 클래스입니다.
 *
 * [java.util.concurrent.StructuredTaskScope.ShutdownOnSuccess]는 정상 반환만 "성공"으로 간주하므로,
 * 실패/중단/취소 결과를 예외로 전환하여 scope가 계속 다음 성공을 기다리도록 합니다.
 */
internal class WorkNotSuccessException(val report: WorkReport): Exception("Work not successful: ${report.status}")
