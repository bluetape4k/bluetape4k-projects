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
 * - [ParallelPolicy.ALL]: 모든 작업이 성공해야 하며, 성공이 아닌 보고서나 예외가 발생하면 나머지를 취소합니다.
 * - [ParallelPolicy.ANY]: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
 *
 * ```kotlin
 * // ALL 정책 (기본값) — 모두 성공해야 하며, 하나라도 성공이 아니면 fail-fast
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
     * ALL 정책: 모든 작업 성공을 대기합니다. 하나라도 성공이 아니거나 예외를 던지면 나머지를 취소합니다.
     *
     * [StructuredTaskScopes.failFast] (ShutdownOnFailure)을 사용합니다.
     */
    private fun executeAll(context: WorkContext): WorkReport {
        val factory = Thread.ofVirtual().name("$flowName-", 0).factory()
        val deadline = Instant.now().plusMillis(timeout.inWholeMilliseconds)

        return try {
            StructuredTaskScopes.failFast(name = flowName, factory = factory) { scope ->
                val tasks = works.map { work ->
                    val workName = (work as? NamedWork)?.name ?: work.javaClass.simpleName
                    scope.fork {
                        log.debug { "$flowName: '$workName' 병렬 실행 시작 (ALL)" }
                        val report = work.execute(context)
                        log.debug { "$flowName: '$workName' 실행 완료 - status=${report.status}" }
                        if (report.isSuccess) report else throw WorkNotSuccessException(report)
                    }
                }

                scope.joinUntil(deadline).throwIfFailed { e ->
                    log.debug { "$flowName: throwIfFailed 감지 - ${e.message}" }
                }

                tasks.forEach { task -> task.get() }

                log.debug { "$flowName 완료 - 모두 성공 (ALL)" }
                WorkReport.success(context)
            }
        } catch (e: WorkNotSuccessException) {
            log.debug { "$flowName: fail-fast report 감지 - status=${e.report.status}" }
            e.report
        } catch (e: TimeoutException) {
            log.debug { "$flowName: timeout 초과 ($timeout) - Cancelled 반환" }
            WorkReport.Cancelled(context)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.debug { "$flowName: interrupted - Cancelled 반환" }
            WorkReport.Cancelled(context)
        } catch (e: Exception) {
            log.debug { "$flowName: fail-fast 예외 발생 - ${e.message}" }
            WorkReport.Failure(context, e)
        }
    }

    /**
     * ANY 정책: 첫 번째 성공한 작업 결과를 즉시 반환하고 나머지를 취소합니다.
     *
     * [StructuredTaskScopes.firstSuccess] (ShutdownOnSuccess)를 사용합니다.
     * Success가 아닌 결과는 예외로 래핑하여 ShutdownOnSuccess가 "성공"으로 간주하지 않도록 합니다.
     */
    private fun executeAny(context: WorkContext): WorkReport {
        val factory = Thread.ofVirtual().name("$flowName-", 0).factory()
        val deadline = Instant.now().plusMillis(timeout.inWholeMilliseconds)
        val failedReports = java.util.concurrent.ConcurrentLinkedQueue<WorkReport>()

        return try {
            runCatching {
                StructuredTaskScopes.firstSuccess<WorkReport>(name = flowName, factory = factory) { scope ->
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

                    scope.joinUntil(deadline).result { e ->
                        RuntimeException("$flowName: 모든 작업이 실패했습니다", e)
                    }
                }
            }.getOrElse { throwable ->
                // TimeoutException은 outer catch로 전파 (runCatching이 삼킴 방지)
                if (throwable is TimeoutException) throw throwable
                // 모두 실패한 경우 failedReports에서 우선순위로 반환
                failedReports.firstOrNull { it.isAborted }
                    ?: failedReports.firstOrNull { it.isCancelled }
                    ?: failedReports.firstOrNull { it.isFailure }
                    ?: WorkReport.failure(context, RuntimeException("$flowName: 모든 작업이 실패했습니다"))
            }
        } catch (e: TimeoutException) {
            log.debug { "$flowName: timeout 초과 ($timeout) - Cancelled 반환 (ANY)" }
            WorkReport.Cancelled(context)
        }
    }

    override fun toString(): String =
        "ParallelWorkFlow($flowName, works=${works.size}, policy=$policy, timeout=$timeout)"
}

/**
 * [ParallelPolicy.ALL]/[ParallelPolicy.ANY] 정책에서 성공이 아닌 [WorkReport]를 예외로 래핑하는 내부 클래스입니다.
 *
 * 구조화된 동시성 primitive는 정상 반환을 성공으로 간주하므로, 실패/중단/취소 결과를 예외로 전환합니다.
 */
internal class WorkNotSuccessException(val report: WorkReport): RuntimeException("Work not successful: ${report.status}") {
    // 제어 흐름용 예외 — 스택 캡처 비용 없이 빠른 경로 처리
    override fun fillInStackTrace(): Throwable = this
}
