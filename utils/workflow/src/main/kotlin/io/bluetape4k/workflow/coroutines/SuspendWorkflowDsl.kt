package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.WorkflowDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

// ──────────────────────────────────────────────────────────────────────────────
// SuspendSequentialFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendSequentialFlow]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * 작업 목록을 순차적으로 실행하는 코루틴 워크플로를 선언적으로 정의할 수 있습니다.
 * 내부에 [SuspendParallelFlowBuilder], [SuspendConditionalFlowBuilder],
 * [SuspendRepeatFlowBuilder], [SuspendRetryFlowBuilder]를 중첩하여 복합 워크플로를 구성할 수 있습니다.
 *
 * ```kotlin
 * val flow = suspendSequentialFlow("order-processing") {
 *     execute("validate") { ctx ->
 *         ctx["valid"] = true
 *         WorkReport.Success(ctx)
 *     }
 *     parallel("fetch-data") {
 *         execute("fetch-user") { ctx -> WorkReport.Success(ctx) }
 *         execute("fetch-inventory") { ctx -> WorkReport.Success(ctx) }
 *     }
 *     errorStrategy(ErrorStrategy.CONTINUE)
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendSequentialFlowBuilder(private val name: String = "suspend-sequential-flow") {
    private val works = mutableListOf<SuspendWork>()
    private var errorStrategy = ErrorStrategy.STOP

    /**
     * 이미 생성된 [SuspendWork] 인스턴스를 실행 목록에 추가합니다.
     *
     * @param work 추가할 작업
     */
    fun execute(work: SuspendWork) {
        works.add(work)
    }

    /**
     * 이름과 실행 블록으로 작업을 생성하여 실행 목록에 추가합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun execute(name: String, block: suspend (WorkContext) -> WorkReport) {
        works.add(SuspendWork(name, block))
    }

    /**
     * 에러 발생 시 동작 전략을 설정합니다.
     *
     * @param strategy [ErrorStrategy.STOP] 또는 [ErrorStrategy.CONTINUE]
     */
    fun errorStrategy(strategy: ErrorStrategy) {
        this.errorStrategy = strategy
    }

    /**
     * 병렬 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 병렬 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallel(name: String = "suspend-parallel-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        works.add(SuspendParallelFlowBuilder(name).apply(block).build())
    }

    /**
     * ALL 정책의 병렬 워크플로를 중첩 작업으로 추가합니다.
     * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
     *
     * @param name 병렬 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallelAll(name: String = "suspend-parallel-all-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        works.add(suspendParallelAllFlow(name, block))
    }

    /**
     * ANY 정책의 병렬 워크플로를 중첩 작업으로 추가합니다.
     * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
     *
     * @param name 병렬 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallelAny(name: String = "suspend-parallel-any-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        works.add(suspendParallelAnyFlow(name, block))
    }

    /**
     * 조건 분기 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 조건 워크플로 이름
     * @param block [SuspendConditionalFlowBuilder] DSL 블록
     */
    fun conditional(name: String = "suspend-conditional-flow", block: SuspendConditionalFlowBuilder.() -> Unit) {
        works.add(SuspendConditionalFlowBuilder(name).apply(block).build())
    }

    /**
     * 반복 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 반복 워크플로 이름
     * @param block [SuspendRepeatFlowBuilder] DSL 블록
     */
    fun repeat(name: String = "suspend-repeat-flow", block: SuspendRepeatFlowBuilder.() -> Unit) {
        works.add(SuspendRepeatFlowBuilder(name).apply(block).build())
    }

    /**
     * 재시도 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 재시도 워크플로 이름
     * @param block [SuspendRetryFlowBuilder] DSL 블록
     */
    fun retry(name: String = "suspend-retry-flow", block: SuspendRetryFlowBuilder.() -> Unit) {
        works.add(SuspendRetryFlowBuilder(name).apply(block).build())
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendSequentialFlow = SuspendSequentialFlow(works.toList(), errorStrategy, name)
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendParallelFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendParallelFlow]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * 여러 작업을 코루틴으로 병렬 실행하는 워크플로를 선언적으로 정의합니다.
 * [coroutineScope][kotlinx.coroutines.coroutineScope]로 구조화된 동시성을 보장합니다.
 * [policy]로 실행 전략을 선택할 수 있습니다.
 *
 * ```kotlin
 * // ALL 정책 (기본값) — 모든 작업 완료 대기
 * val flow = suspendParallelFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 * }
 *
 * // ANY 정책 — 첫 성공 즉시 반환
 * val raceFlow = suspendParallelFlow("race") {
 *     execute("fast") { ctx -> WorkReport.Success(ctx) }
 *     execute("slow") { ctx -> WorkReport.Success(ctx) }
 *     policy(ParallelPolicy.ANY)
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendParallelFlowBuilder(private val name: String = "suspend-parallel-flow") {
    private val works = mutableListOf<SuspendWork>()
    private var policy = ParallelPolicy.ALL

    /**
     * 이미 생성된 [SuspendWork] 인스턴스를 병렬 실행 목록에 추가합니다.
     *
     * @param work 추가할 작업
     */
    fun execute(work: SuspendWork) {
        works.add(work)
    }

    /**
     * 이름과 실행 블록으로 작업을 생성하여 병렬 실행 목록에 추가합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun execute(name: String, block: suspend (WorkContext) -> WorkReport) {
        works.add(SuspendWork(name, block))
    }

    /**
     * 병렬 실행 정책을 설정합니다.
     *
     * @param policy [ParallelPolicy.ALL] 또는 [ParallelPolicy.ANY]
     */
    fun policy(policy: ParallelPolicy) {
        this.policy = policy
    }

    /**
     * 모든 작업 완료 대기 정책으로 설정합니다. [ParallelPolicy.ALL]과 동일합니다.
     */
    fun all() {
        policy = ParallelPolicy.ALL
    }

    /**
     * 첫 성공 즉시 반환 정책으로 설정합니다. [ParallelPolicy.ANY]와 동일합니다.
     */
    fun any() {
        policy = ParallelPolicy.ANY
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendParallelFlow = SuspendParallelFlow(works.toList(), policy, name)
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendConditionalFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendConditionalFlow]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * 조건(predicate)에 따라 then 또는 otherwise 작업을 실행하는 워크플로를 선언적으로 정의합니다.
 * [otherwise]는 선택 사항이며, 생략 시 조건이 false이면 [WorkReport.Success]가 반환됩니다.
 *
 * ```kotlin
 * val flow = suspendConditionalFlow("check-valid") {
 *     condition { ctx -> ctx.get<Boolean>("valid") == true }
 *     then("process") { ctx -> WorkReport.Success(ctx) }
 *     otherwise("reject") { ctx -> WorkReport.Failure(ctx) }
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendConditionalFlowBuilder(private val name: String = "suspend-conditional-flow") {
    private var predicate: (suspend (WorkContext) -> Boolean)? = null
    private var thenWork: SuspendWork? = null
    private var otherwiseWork: SuspendWork? = null

    /**
     * 분기 조건 함수를 설정합니다.
     *
     * @param block 컨텍스트를 받아 Boolean을 반환하는 suspend 조건 람다
     */
    fun condition(block: suspend (WorkContext) -> Boolean) {
        predicate = block
    }

    /**
     * 조건이 true일 때 실행할 작업을 설정합니다.
     *
     * @param work 실행할 [SuspendWork] 인스턴스
     */
    fun then(work: SuspendWork) {
        thenWork = work
    }

    /**
     * 조건이 true일 때 실행할 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun then(name: String, block: suspend (WorkContext) -> WorkReport) {
        thenWork = SuspendWork(name, block)
    }

    /**
     * 조건이 false일 때 실행할 작업을 설정합니다 (선택 사항).
     *
     * @param work 실행할 [SuspendWork] 인스턴스
     */
    fun otherwise(work: SuspendWork) {
        otherwiseWork = work
    }

    /**
     * 조건이 false일 때 실행할 작업을 이름과 블록으로 설정합니다 (선택 사항).
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun otherwise(name: String, block: suspend (WorkContext) -> WorkReport) {
        otherwiseWork = SuspendWork(name, block)
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendConditionalFlow {
        val p = requireNotNull(predicate) { "condition {} 블록이 필요합니다." }
        val t = requireNotNull(thenWork) { "then {} 블록이 필요합니다." }
        return SuspendConditionalFlow(p, t, otherwiseWork, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendRepeatFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendRepeatFlow]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * 반복 조건이 true인 동안 작업을 반복 실행하는 워크플로를 선언적으로 정의합니다.
 * [maxIterations]로 최대 반복 횟수를 제한할 수 있고, [repeatDelay]로 반복 간 대기 시간을 설정할 수 있습니다.
 *
 * ```kotlin
 * val flow = suspendRepeatFlow("poll-status") {
 *     execute("check") { ctx ->
 *         ctx["count"] = (ctx.getOrDefault("count", 0) as Int) + 1
 *         WorkReport.Success(ctx)
 *     }
 *     repeatWhile { report -> report.isSuccess && report.context.get<Int>("count")!! < 5 }
 *     maxIterations(10)
 *     repeatDelay(100.milliseconds)
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendRepeatFlowBuilder(private val name: String = "suspend-repeat-flow") {
    private var work: SuspendWork? = null
    private var repeatPredicate: suspend (WorkReport) -> Boolean = { it.isSuccess }
    private var maxIterations: Int = Int.MAX_VALUE
    private var repeatDelay: Duration = Duration.ZERO

    /**
     * 반복 실행할 작업을 설정합니다.
     *
     * @param work 반복할 [SuspendWork] 인스턴스
     */
    fun execute(work: SuspendWork) {
        this.work = work
    }

    /**
     * 반복 실행할 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun execute(name: String, block: suspend (WorkContext) -> WorkReport) {
        this.work = SuspendWork(name, block)
    }

    /**
     * 반복 조건을 설정합니다. 조건이 true인 동안 반복합니다.
     *
     * @param predicate 이전 실행 결과를 받아 계속 반복할지 여부를 반환하는 suspend 람다
     */
    fun repeatWhile(predicate: suspend (WorkReport) -> Boolean) {
        repeatPredicate = predicate
    }

    /**
     * 종료 조건을 설정합니다. 조건이 true가 되면 반복을 종료합니다.
     * [repeatWhile]의 반전 표현입니다.
     *
     * @param predicate 이전 실행 결과를 받아 반복을 중단할지 여부를 반환하는 suspend 람다
     */
    fun until(predicate: suspend (WorkReport) -> Boolean) {
        repeatPredicate = { !predicate(it) }
    }

    /**
     * 최대 반복 횟수를 설정합니다.
     *
     * @param n 최대 반복 횟수 (기본값: [Int.MAX_VALUE])
     */
    fun maxIterations(n: Int) {
        maxIterations = n
    }

    /**
     * 반복 간 대기 시간을 설정합니다.
     *
     * @param duration 대기 시간 (기본값: [Duration.ZERO])
     */
    fun repeatDelay(duration: Duration) {
        this.repeatDelay = duration
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendRepeatFlow {
        val w = requireNotNull(work) { "execute {} 블록이 필요합니다." }
        return SuspendRepeatFlow(w, repeatPredicate, maxIterations, repeatDelay, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendRetryFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendRetryFlow]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * 작업 실패 시 [RetryPolicy]에 따라 재시도하는 워크플로를 선언적으로 정의합니다.
 * [policy] 블록을 통해 [SuspendRetryPolicyBuilder]로 재시도 정책을 인라인으로 구성할 수 있습니다.
 *
 * ```kotlin
 * val flow = suspendRetryFlow("call-api") {
 *     execute("http-call") { ctx -> WorkReport.Success(ctx) }
 *     policy {
 *         maxAttempts = 5
 *         delay = 200.milliseconds
 *         backoffMultiplier = 2.0
 *         maxDelay = 30.seconds
 *     }
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendRetryFlowBuilder(private val name: String = "suspend-retry-flow") {
    private var work: SuspendWork? = null
    private var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    /**
     * 재시도 대상 작업을 설정합니다.
     *
     * @param work 재시도할 [SuspendWork] 인스턴스
     */
    fun execute(work: SuspendWork) {
        this.work = work
    }

    /**
     * 재시도 대상 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직 (suspend 함수)
     */
    fun execute(name: String, block: suspend (WorkContext) -> WorkReport) {
        this.work = SuspendWork(name, block)
    }

    /**
     * 재시도 정책을 직접 설정합니다.
     *
     * @param retryPolicy 적용할 [RetryPolicy]
     */
    fun policy(retryPolicy: RetryPolicy) {
        this.retryPolicy = retryPolicy
    }

    /**
     * [SuspendRetryPolicyBuilder] DSL 블록으로 재시도 정책을 구성합니다.
     *
     * @param block [SuspendRetryPolicyBuilder] DSL 블록
     */
    fun policy(block: SuspendRetryPolicyBuilder.() -> Unit) {
        retryPolicy = SuspendRetryPolicyBuilder().apply(block).build()
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendRetryFlow {
        val w = requireNotNull(work) { "execute {} 블록이 필요합니다." }
        return SuspendRetryFlow(w, retryPolicy, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendRetryPolicyBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [RetryPolicy]를 구성하는 코루틴 DSL 빌더입니다.
 *
 * [SuspendRetryFlowBuilder.policy] 블록 내에서 사용하여 재시도 정책을 선언적으로 정의합니다.
 *
 * ```kotlin
 * suspendRetryFlow {
 *     execute("work") { ctx -> WorkReport.Success(ctx) }
 *     policy {
 *         maxAttempts = 3
 *         delay = 100.milliseconds
 *         backoffMultiplier = 2.0
 *         maxDelay = 1.minutes
 *     }
 * }
 * ```
 */
@WorkflowDsl
class SuspendRetryPolicyBuilder {
    /** 최대 총 시도 횟수 (최초 실행 포함). 기본값: 3 */
    var maxAttempts: Int = 3

    /** 재시도 간 초기 대기 시간. 기본값: [Duration.ZERO] */
    var delay: Duration = Duration.ZERO

    /** 지수 백오프 배율 (1.0 = 고정 지연). 기본값: 2.0 */
    var backoffMultiplier: Double = 2.0

    /** 백오프 적용 시 최대 지연 시간 상한. 기본값: 1분 */
    var maxDelay: Duration = 1.minutes

    /** @suppress 내부 빌드 메서드 */
    internal fun build() = RetryPolicy(maxAttempts, delay, backoffMultiplier, maxDelay)
}

// ──────────────────────────────────────────────────────────────────────────────
// SuspendWorkflowBuilder (최상위)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 전체 코루틴 워크플로의 루트를 구성하는 최상위 DSL 빌더입니다.
 *
 * [sequential], [parallel], [conditional], [repeat] 중 하나를 루트 플로우로 지정합니다.
 * 루트 플로우는 단 하나만 지정할 수 있습니다.
 *
 * ```kotlin
 * val root: SuspendWork = suspendWorkflow("order-flow") {
 *     sequential {
 *         execute("validate") { ctx -> WorkReport.Success(ctx) }
 *         execute("process") { ctx -> WorkReport.Success(ctx) }
 *     }
 * }
 * val report = root.execute(WorkContext())
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class SuspendWorkflowBuilder(private val name: String = "suspend-workflow") {
    private var rootWork: SuspendWork? = null

    private fun setRoot(work: SuspendWork) {
        require(rootWork == null) { "루트 워크플로가 이미 선언되었습니다. 루트는 하나만 지정할 수 있습니다." }
        rootWork = work
    }

    /**
     * 순차 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendSequentialFlowBuilder] DSL 블록
     */
    fun sequential(name: String = "suspend-sequential-flow", block: SuspendSequentialFlowBuilder.() -> Unit) {
        setRoot(SuspendSequentialFlowBuilder(name).apply(block).build())
    }

    /**
     * 병렬 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallel(name: String = "suspend-parallel-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        setRoot(SuspendParallelFlowBuilder(name).apply(block).build())
    }

    /**
     * ALL 정책의 병렬 워크플로를 루트 플로우로 설정합니다.
     * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallelAll(name: String = "suspend-parallel-all-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        setRoot(suspendParallelAllFlow(name, block))
    }

    /**
     * ANY 정책의 병렬 워크플로를 루트 플로우로 설정합니다.
     * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendParallelFlowBuilder] DSL 블록
     */
    fun parallelAny(name: String = "suspend-parallel-any-flow", block: SuspendParallelFlowBuilder.() -> Unit) {
        setRoot(suspendParallelAnyFlow(name, block))
    }

    /**
     * 조건 분기 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendConditionalFlowBuilder] DSL 블록
     */
    fun conditional(name: String = "suspend-conditional-flow", block: SuspendConditionalFlowBuilder.() -> Unit) {
        setRoot(SuspendConditionalFlowBuilder(name).apply(block).build())
    }

    /**
     * 반복 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [SuspendRepeatFlowBuilder] DSL 블록
     */
    fun repeat(name: String = "suspend-repeat-flow", block: SuspendRepeatFlowBuilder.() -> Unit) {
        setRoot(SuspendRepeatFlowBuilder(name).apply(block).build())
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SuspendWork = requireNotNull(rootWork) { "워크플로에 루트 플로우가 없습니다." }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top-level DSL 함수
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SuspendSequentialFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = suspendSequentialFlow("order-processing") {
 *     execute("validate") { ctx -> WorkReport.Success(ctx) }
 *     execute("save") { ctx -> WorkReport.Success(ctx) }
 *     errorStrategy(ErrorStrategy.CONTINUE)
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-sequential-flow")
 * @param block [SuspendSequentialFlowBuilder] DSL 블록
 * @return 구성된 [SuspendSequentialFlow]
 */
fun suspendSequentialFlow(
    name: String = "suspend-sequential-flow",
    block: SuspendSequentialFlowBuilder.() -> Unit,
): SuspendSequentialFlow = SuspendSequentialFlowBuilder(name).apply(block).build()

/**
 * [SuspendParallelFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = suspendParallelFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-parallel-flow")
 * @param block [SuspendParallelFlowBuilder] DSL 블록
 * @return 구성된 [SuspendParallelFlow]
 */
fun suspendParallelFlow(
    name: String = "suspend-parallel-flow",
    block: SuspendParallelFlowBuilder.() -> Unit,
): SuspendParallelFlow = SuspendParallelFlowBuilder(name).apply(block).build()

/**
 * ALL 정책의 [SuspendParallelFlow]를 DSL로 생성하는 최상위 함수입니다.
 * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
 * [ParallelPolicy.ALL]의 단축 함수입니다.
 *
 * ```kotlin
 * val flow = suspendParallelAllFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-parallel-all-flow")
 * @param block [SuspendParallelFlowBuilder] DSL 블록
 * @return 구성된 [SuspendParallelFlow]
 */
fun suspendParallelAllFlow(
    name: String = "suspend-parallel-all-flow",
    block: SuspendParallelFlowBuilder.() -> Unit,
): SuspendParallelFlow = SuspendParallelFlowBuilder(name).apply { all() }.apply(block).build()

/**
 * ANY 정책의 [SuspendParallelFlow]를 DSL로 생성하는 최상위 함수입니다.
 * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
 * [ParallelPolicy.ANY]의 단축 함수입니다.
 *
 * ```kotlin
 * val flow = suspendParallelAnyFlow("race") {
 *     execute("fast") { ctx -> WorkReport.Success(ctx) }
 *     execute("slow") { ctx -> WorkReport.Success(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-parallel-any-flow")
 * @param block [SuspendParallelFlowBuilder] DSL 블록
 * @return 구성된 [SuspendParallelFlow]
 */
fun suspendParallelAnyFlow(
    name: String = "suspend-parallel-any-flow",
    block: SuspendParallelFlowBuilder.() -> Unit,
): SuspendParallelFlow = SuspendParallelFlowBuilder(name).apply { any() }.apply(block).build()

/**
 * [SuspendConditionalFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = suspendConditionalFlow("check-valid") {
 *     condition { ctx -> ctx.get<Boolean>("valid") == true }
 *     then("process") { ctx -> WorkReport.Success(ctx) }
 *     otherwise("reject") { ctx -> WorkReport.Failure(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-conditional-flow")
 * @param block [SuspendConditionalFlowBuilder] DSL 블록
 * @return 구성된 [SuspendConditionalFlow]
 */
fun suspendConditionalFlow(
    name: String = "suspend-conditional-flow",
    block: SuspendConditionalFlowBuilder.() -> Unit,
): SuspendConditionalFlow = SuspendConditionalFlowBuilder(name).apply(block).build()

/**
 * [SuspendRepeatFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = suspendRepeatFlow("poll") {
 *     execute("check") { ctx -> WorkReport.Success(ctx) }
 *     until { report -> report.context.get<Boolean>("done") == true }
 *     maxIterations(100)
 *     repeatDelay(500.milliseconds)
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-repeat-flow")
 * @param block [SuspendRepeatFlowBuilder] DSL 블록
 * @return 구성된 [SuspendRepeatFlow]
 */
fun suspendRepeatFlow(
    name: String = "suspend-repeat-flow",
    block: SuspendRepeatFlowBuilder.() -> Unit,
): SuspendRepeatFlow = SuspendRepeatFlowBuilder(name).apply(block).build()

/**
 * [SuspendRetryFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = suspendRetryFlow("call-api") {
 *     execute("http-call") { ctx -> WorkReport.Success(ctx) }
 *     policy {
 *         maxAttempts = 3
 *         delay = 100.milliseconds
 *         backoffMultiplier = 2.0
 *     }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-retry-flow")
 * @param block [SuspendRetryFlowBuilder] DSL 블록
 * @return 구성된 [SuspendRetryFlow]
 */
fun suspendRetryFlow(
    name: String = "suspend-retry-flow",
    block: SuspendRetryFlowBuilder.() -> Unit,
): SuspendRetryFlow = SuspendRetryFlowBuilder(name).apply(block).build()

/**
 * 코루틴 최상위 워크플로를 DSL로 생성하는 함수입니다.
 *
 * `sequential`, `parallel`, `conditional`, `repeat` 중 하나를 루트 플로우로 선언합니다.
 * 반환 타입은 [SuspendWork]이므로 다른 워크플로에 중첩하거나 직접 실행할 수 있습니다.
 *
 * ```kotlin
 * val root = suspendWorkflow("order-flow") {
 *     sequential("main") {
 *         execute("validate") { ctx -> WorkReport.Success(ctx) }
 *         parallel("fetch") {
 *             execute("fetch-user") { ctx -> WorkReport.Success(ctx) }
 *             execute("fetch-product") { ctx -> WorkReport.Success(ctx) }
 *         }
 *         execute("save") { ctx -> WorkReport.Success(ctx) }
 *     }
 * }
 * val report = root.execute(WorkContext())
 * ```
 *
 * @param name 워크플로 이름 (기본값: "suspend-workflow")
 * @param block [SuspendWorkflowBuilder] DSL 블록
 * @return 루트 [SuspendWork] 인스턴스
 */
fun suspendWorkflow(
    name: String = "suspend-workflow",
    block: SuspendWorkflowBuilder.() -> Unit,
): SuspendWork = SuspendWorkflowBuilder(name).apply(block).build()
