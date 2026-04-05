package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.ErrorStrategy
import io.bluetape4k.workflow.api.ParallelPolicy
import io.bluetape4k.workflow.api.RetryPolicy
import io.bluetape4k.workflow.api.Work
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import io.bluetape4k.workflow.api.WorkflowDsl
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

// ──────────────────────────────────────────────────────────────────────────────
// SequentialFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SequentialWorkFlow]를 구성하는 DSL 빌더입니다.
 *
 * 작업 목록을 순차적으로 실행하는 워크플로를 선언적으로 정의할 수 있습니다.
 * 내부에 [ParallelFlowBuilder], [ConditionalFlowBuilder], [RepeatFlowBuilder], [RetryFlowBuilder]를
 * 중첩하여 복합 워크플로를 구성할 수 있습니다.
 *
 * ```kotlin
 * val flow = sequentialFlow("order-processing") {
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
class SequentialFlowBuilder(private val name: String = "sequential-flow") {
    private val works = mutableListOf<Work>()
    private var errorStrategy = ErrorStrategy.STOP

    /**
     * 이미 생성된 [Work] 인스턴스를 실행 목록에 추가합니다.
     *
     * @param work 추가할 작업
     */
    fun execute(work: Work) {
        works.add(work)
    }

    /**
     * 이름과 실행 블록으로 작업을 생성하여 실행 목록에 추가합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun execute(name: String, block: (WorkContext) -> WorkReport) {
        works.add(Work(name, block))
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
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallel(name: String = "parallel-flow", block: ParallelFlowBuilder.() -> Unit) {
        works.add(ParallelFlowBuilder(name).apply(block).build())
    }

    /**
     * ALL 정책의 병렬 워크플로를 중첩 작업으로 추가합니다.
     * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
     *
     * @param name 병렬 워크플로 이름
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallelAll(name: String = "parallel-all-flow", block: ParallelFlowBuilder.() -> Unit) {
        works.add(parallelAllFlow(name, block))
    }

    /**
     * ANY 정책의 병렬 워크플로를 중첩 작업으로 추가합니다.
     * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
     *
     * @param name 병렬 워크플로 이름
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallelAny(name: String = "parallel-any-flow", block: ParallelFlowBuilder.() -> Unit) {
        works.add(parallelAnyFlow(name, block))
    }

    /**
     * 조건 분기 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 조건 워크플로 이름
     * @param block [ConditionalFlowBuilder] DSL 블록
     */
    fun conditional(name: String = "conditional-flow", block: ConditionalFlowBuilder.() -> Unit) {
        works.add(ConditionalFlowBuilder(name).apply(block).build())
    }

    /**
     * 반복 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 반복 워크플로 이름
     * @param block [RepeatFlowBuilder] DSL 블록
     */
    fun repeat(name: String = "repeat-flow", block: RepeatFlowBuilder.() -> Unit) {
        works.add(RepeatFlowBuilder(name).apply(block).build())
    }

    /**
     * 재시도 워크플로를 중첩 작업으로 추가합니다.
     *
     * @param name 재시도 워크플로 이름
     * @param block [RetryFlowBuilder] DSL 블록
     */
    fun retry(name: String = "retry-flow", block: RetryFlowBuilder.() -> Unit) {
        works.add(RetryFlowBuilder(name).apply(block).build())
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): SequentialWorkFlow = SequentialWorkFlow(works.toList(), errorStrategy, name)
}

// ──────────────────────────────────────────────────────────────────────────────
// ParallelFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [ParallelWorkFlow]를 구성하는 DSL 빌더입니다.
 *
 * 여러 작업을 동시에 병렬 실행하는 워크플로를 선언적으로 정의합니다.
 * [StructuredTaskScopes][io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes] 기반으로 실행되며,
 * [policy]로 실행 전략을 선택할 수 있습니다.
 *
 * ```kotlin
 * // ALL 정책 (기본값) — 모든 작업 완료 대기
 * val flow = parallelFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 *     timeout(30.seconds)
 * }
 *
 * // ANY 정책 — 첫 성공 즉시 반환
 * val raceFlow = parallelFlow("race") {
 *     execute("fast") { ctx -> WorkReport.Success(ctx) }
 *     execute("slow") { ctx -> WorkReport.Success(ctx) }
 *     policy(ParallelPolicy.ANY)
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class ParallelFlowBuilder(private val name: String = "parallel-flow") {
    private val works = mutableListOf<Work>()
    private var timeout = 1.minutes
    private var policy = ParallelPolicy.ALL

    /**
     * 이미 생성된 [Work] 인스턴스를 병렬 실행 목록에 추가합니다.
     *
     * @param work 추가할 작업
     */
    fun execute(work: Work) {
        works.add(work)
    }

    /**
     * 이름과 실행 블록으로 작업을 생성하여 병렬 실행 목록에 추가합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun execute(name: String, block: (WorkContext) -> WorkReport) {
        works.add(Work(name, block))
    }

    /**
     * 전체 병렬 실행 타임아웃을 설정합니다.
     *
     * @param duration 타임아웃 기간
     */
    fun timeout(duration: Duration) {
        this.timeout = duration
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
    internal fun build(): ParallelWorkFlow =
        ParallelWorkFlow(works.toList(), policy = policy, timeout = timeout, flowName = name)
}

// ──────────────────────────────────────────────────────────────────────────────
// ConditionalFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [ConditionalWorkFlow]를 구성하는 DSL 빌더입니다.
 *
 * 조건(predicate)에 따라 then 또는 otherwise 작업을 실행하는 워크플로를 선언적으로 정의합니다.
 * [otherwise]는 선택 사항이며, 생략 시 조건이 false이면 [WorkReport.Success]가 반환됩니다.
 *
 * ```kotlin
 * val flow = conditionalFlow("check-valid") {
 *     condition { ctx -> ctx.get<Boolean>("valid") == true }
 *     then("process") { ctx -> WorkReport.Success(ctx) }
 *     otherwise("reject") { ctx -> WorkReport.Failure(ctx) }
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class ConditionalFlowBuilder(private val name: String = "conditional-flow") {
    private var predicate: ((WorkContext) -> Boolean)? = null
    private var thenWork: Work? = null
    private var otherwiseWork: Work? = null

    /**
     * 분기 조건 함수를 설정합니다.
     *
     * @param block 컨텍스트를 받아 Boolean을 반환하는 조건 람다
     */
    fun condition(block: (WorkContext) -> Boolean) {
        predicate = block
    }

    /**
     * 조건이 true일 때 실행할 작업을 설정합니다.
     *
     * @param work 실행할 [Work] 인스턴스
     */
    fun then(work: Work) {
        thenWork = work
    }

    /**
     * 조건이 true일 때 실행할 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun then(name: String, block: (WorkContext) -> WorkReport) {
        thenWork = Work(name, block)
    }

    /**
     * 조건이 false일 때 실행할 작업을 설정합니다 (선택 사항).
     *
     * @param work 실행할 [Work] 인스턴스
     */
    fun otherwise(work: Work) {
        otherwiseWork = work
    }

    /**
     * 조건이 false일 때 실행할 작업을 이름과 블록으로 설정합니다 (선택 사항).
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun otherwise(name: String, block: (WorkContext) -> WorkReport) {
        otherwiseWork = Work(name, block)
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): ConditionalWorkFlow {
        val p = requireNotNull(predicate) { "condition {} 블록이 필요합니다." }
        val t = requireNotNull(thenWork) { "then {} 블록이 필요합니다." }
        return ConditionalWorkFlow(p, t, otherwiseWork, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RepeatFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [RepeatWorkFlow]를 구성하는 DSL 빌더입니다.
 *
 * 반복 조건이 true인 동안 작업을 반복 실행하는 워크플로를 선언적으로 정의합니다.
 * [maxIterations]로 최대 반복 횟수를 제한할 수 있습니다.
 *
 * ```kotlin
 * val flow = repeatFlow("poll-status") {
 *     execute("check") { ctx ->
 *         ctx["count"] = (ctx.getOrDefault("count", 0) as Int) + 1
 *         WorkReport.Success(ctx)
 *     }
 *     repeatWhile { report -> report.isSuccess && report.context.get<Int>("count")!! < 5 }
 *     maxIterations(10)
 * }
 * ```
 *
 * @property name 워크플로 이름 (로깅용)
 */
@WorkflowDsl
class RepeatFlowBuilder(private val name: String = "repeat-flow") {
    private var work: Work? = null
    private var repeatPredicate: (WorkReport) -> Boolean = { it.isSuccess }
    private var maxIterations: Int = Int.MAX_VALUE

    /**
     * 반복 실행할 작업을 설정합니다.
     *
     * @param work 반복할 [Work] 인스턴스
     */
    fun execute(work: Work) {
        this.work = work
    }

    /**
     * 반복 실행할 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun execute(name: String, block: (WorkContext) -> WorkReport) {
        this.work = Work(name, block)
    }

    /**
     * 반복 조건을 설정합니다. 조건이 true인 동안 반복합니다.
     *
     * @param predicate 이전 실행 결과를 받아 계속 반복할지 여부를 반환하는 람다
     */
    fun repeatWhile(predicate: (WorkReport) -> Boolean) {
        repeatPredicate = predicate
    }

    /**
     * 종료 조건을 설정합니다. 조건이 true가 되면 반복을 종료합니다.
     * [repeatWhile]의 반전 표현입니다.
     *
     * @param predicate 이전 실행 결과를 받아 반복을 중단할지 여부를 반환하는 람다
     */
    fun until(predicate: (WorkReport) -> Boolean) {
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

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): RepeatWorkFlow {
        val w = requireNotNull(work) { "execute {} 블록이 필요합니다." }
        return RepeatWorkFlow(w, repeatPredicate, maxIterations, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RetryFlowBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [RetryWorkFlow]를 구성하는 DSL 빌더입니다.
 *
 * 작업 실패 시 [RetryPolicy]에 따라 재시도하는 워크플로를 선언적으로 정의합니다.
 * [policy] 블록을 통해 [RetryPolicyBuilder]로 재시도 정책을 인라인으로 구성할 수 있습니다.
 *
 * ```kotlin
 * val flow = retryFlow("call-api") {
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
class RetryFlowBuilder(private val name: String = "retry-flow") {
    private var work: Work? = null
    private var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    /**
     * 재시도 대상 작업을 설정합니다.
     *
     * @param work 재시도할 [Work] 인스턴스
     */
    fun execute(work: Work) {
        this.work = work
    }

    /**
     * 재시도 대상 작업을 이름과 블록으로 설정합니다.
     *
     * @param name 작업 이름
     * @param block 작업 실행 로직
     */
    fun execute(name: String, block: (WorkContext) -> WorkReport) {
        this.work = Work(name, block)
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
     * [RetryPolicyBuilder] DSL 블록으로 재시도 정책을 구성합니다.
     *
     * @param block [RetryPolicyBuilder] DSL 블록
     */
    fun policy(block: RetryPolicyBuilder.() -> Unit) {
        retryPolicy = RetryPolicyBuilder().apply(block).build()
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): RetryWorkFlow {
        val w = requireNotNull(work) { "execute {} 블록이 필요합니다." }
        return RetryWorkFlow(w, retryPolicy, name)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// RetryPolicyBuilder
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [RetryPolicy]를 구성하는 DSL 빌더입니다.
 *
 * [RetryFlowBuilder.policy] 블록 내에서 사용하여 재시도 정책을 선언적으로 정의합니다.
 *
 * ```kotlin
 * retryFlow {
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
class RetryPolicyBuilder {
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
// WorkflowBuilder (최상위)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * 전체 워크플로의 루트를 구성하는 최상위 DSL 빌더입니다.
 *
 * [sequential], [parallel], [conditional], [repeat] 중 하나를 루트 플로우로 지정합니다.
 * 루트 플로우는 단 하나만 지정할 수 있습니다.
 *
 * ```kotlin
 * val root: Work = workflow("order-flow") {
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
class WorkflowBuilder(private val name: String = "workflow") {
    private var rootWork: Work? = null

    private fun setRoot(work: Work) {
        require(rootWork == null) { "루트 워크플로가 이미 선언되었습니다. 루트는 하나만 지정할 수 있습니다." }
        rootWork = work
    }

    /**
     * 순차 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [SequentialFlowBuilder] DSL 블록
     */
    fun sequential(name: String = "sequential-flow", block: SequentialFlowBuilder.() -> Unit) {
        setRoot(SequentialFlowBuilder(name).apply(block).build())
    }

    /**
     * 병렬 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallel(name: String = "parallel-flow", block: ParallelFlowBuilder.() -> Unit) {
        setRoot(ParallelFlowBuilder(name).apply(block).build())
    }

    /**
     * ALL 정책의 병렬 워크플로를 루트 플로우로 설정합니다.
     * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
     *
     * @param name 워크플로 이름
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallelAll(name: String = "parallel-all-flow", block: ParallelFlowBuilder.() -> Unit) {
        setRoot(parallelAllFlow(name, block))
    }

    /**
     * ANY 정책의 병렬 워크플로를 루트 플로우로 설정합니다.
     * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
     *
     * @param name 워크플로 이름
     * @param block [ParallelFlowBuilder] DSL 블록
     */
    fun parallelAny(name: String = "parallel-any-flow", block: ParallelFlowBuilder.() -> Unit) {
        setRoot(parallelAnyFlow(name, block))
    }

    /**
     * 조건 분기 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [ConditionalFlowBuilder] DSL 블록
     */
    fun conditional(name: String = "conditional-flow", block: ConditionalFlowBuilder.() -> Unit) {
        setRoot(ConditionalFlowBuilder(name).apply(block).build())
    }

    /**
     * 반복 워크플로를 루트 플로우로 설정합니다.
     *
     * @param name 워크플로 이름
     * @param block [RepeatFlowBuilder] DSL 블록
     */
    fun repeat(name: String = "repeat-flow", block: RepeatFlowBuilder.() -> Unit) {
        setRoot(RepeatFlowBuilder(name).apply(block).build())
    }

    /** @suppress 내부 빌드 메서드 */
    internal fun build(): Work = requireNotNull(rootWork) { "워크플로에 루트 플로우가 없습니다." }
}

// ──────────────────────────────────────────────────────────────────────────────
// Top-level DSL 함수
// ──────────────────────────────────────────────────────────────────────────────

/**
 * [SequentialWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = sequentialFlow("order-processing") {
 *     execute("validate") { ctx -> WorkReport.Success(ctx) }
 *     execute("save") { ctx -> WorkReport.Success(ctx) }
 *     errorStrategy(ErrorStrategy.CONTINUE)
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "sequential-flow")
 * @param block [SequentialFlowBuilder] DSL 블록
 * @return 구성된 [SequentialWorkFlow]
 */
fun sequentialFlow(
    name: String = "sequential-flow",
    block: SequentialFlowBuilder.() -> Unit,
): SequentialWorkFlow = SequentialFlowBuilder(name).apply(block).build()

/**
 * [ParallelWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = parallelFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 *     timeout(30.seconds)
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "parallel-flow")
 * @param block [ParallelFlowBuilder] DSL 블록
 * @return 구성된 [ParallelWorkFlow]
 */
fun parallelFlow(
    name: String = "parallel-flow",
    block: ParallelFlowBuilder.() -> Unit,
): ParallelWorkFlow = ParallelFlowBuilder(name).apply(block).build()

/**
 * ALL 정책의 [ParallelWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 * 모든 작업이 성공해야 하며, 하나라도 실패 시 나머지를 취소합니다.
 * [ParallelPolicy.ALL]의 단축 함수입니다.
 *
 * ```kotlin
 * val flow = parallelAllFlow("fetch-all") {
 *     execute("fetch-a") { ctx -> WorkReport.Success(ctx) }
 *     execute("fetch-b") { ctx -> WorkReport.Success(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "parallel-all-flow")
 * @param block [ParallelFlowBuilder] DSL 블록
 * @return 구성된 [ParallelWorkFlow]
 */
fun parallelAllFlow(
    name: String = "parallel-all-flow",
    block: ParallelFlowBuilder.() -> Unit,
): ParallelWorkFlow = ParallelFlowBuilder(name).apply { all() }.apply(block).build()

/**
 * ANY 정책의 [ParallelWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 * 첫 번째 성공한 작업 결과를 반환하고 나머지를 취소합니다.
 * [ParallelPolicy.ANY]의 단축 함수입니다.
 *
 * ```kotlin
 * val flow = parallelAnyFlow("race") {
 *     execute("fast") { ctx -> WorkReport.Success(ctx) }
 *     execute("slow") { ctx -> WorkReport.Success(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "parallel-any-flow")
 * @param block [ParallelFlowBuilder] DSL 블록
 * @return 구성된 [ParallelWorkFlow]
 */
fun parallelAnyFlow(
    name: String = "parallel-any-flow",
    block: ParallelFlowBuilder.() -> Unit,
): ParallelWorkFlow = ParallelFlowBuilder(name).apply { any() }.apply(block).build()

/**
 * [ConditionalWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = conditionalFlow("check-valid") {
 *     condition { ctx -> ctx.get<Boolean>("valid") == true }
 *     then("process") { ctx -> WorkReport.Success(ctx) }
 *     otherwise("reject") { ctx -> WorkReport.Failure(ctx) }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "conditional-flow")
 * @param block [ConditionalFlowBuilder] DSL 블록
 * @return 구성된 [ConditionalWorkFlow]
 */
fun conditionalFlow(
    name: String = "conditional-flow",
    block: ConditionalFlowBuilder.() -> Unit,
): ConditionalWorkFlow = ConditionalFlowBuilder(name).apply(block).build()

/**
 * [RepeatWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = repeatFlow("poll") {
 *     execute("check") { ctx -> WorkReport.Success(ctx) }
 *     until { report -> report.context.get<Boolean>("done") == true }
 *     maxIterations(100)
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "repeat-flow")
 * @param block [RepeatFlowBuilder] DSL 블록
 * @return 구성된 [RepeatWorkFlow]
 */
fun repeatFlow(
    name: String = "repeat-flow",
    block: RepeatFlowBuilder.() -> Unit,
): RepeatWorkFlow = RepeatFlowBuilder(name).apply(block).build()

/**
 * [RetryWorkFlow]를 DSL로 생성하는 최상위 함수입니다.
 *
 * ```kotlin
 * val flow = retryFlow("call-api") {
 *     execute("http-call") { ctx -> WorkReport.Success(ctx) }
 *     policy {
 *         maxAttempts = 3
 *         delay = 100.milliseconds
 *         backoffMultiplier = 2.0
 *     }
 * }
 * ```
 *
 * @param name 워크플로 이름 (기본값: "retry-flow")
 * @param block [RetryFlowBuilder] DSL 블록
 * @return 구성된 [RetryWorkFlow]
 */
fun retryFlow(
    name: String = "retry-flow",
    block: RetryFlowBuilder.() -> Unit,
): RetryWorkFlow = RetryFlowBuilder(name).apply(block).build()

/**
 * 최상위 워크플로를 DSL로 생성하는 함수입니다.
 *
 * `sequential`, `parallel`, `conditional`, `repeat` 중 하나를 루트 플로우로 선언합니다.
 * 반환 타입은 [Work]이므로 다른 워크플로에 중첩하거나 직접 실행할 수 있습니다.
 *
 * ```kotlin
 * val root = workflow("order-flow") {
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
 * @param name 워크플로 이름 (기본값: "workflow")
 * @param block [WorkflowBuilder] DSL 블록
 * @return 루트 [Work] 인스턴스
 */
fun workflow(
    name: String = "workflow",
    block: WorkflowBuilder.() -> Unit,
): Work = WorkflowBuilder(name).apply(block).build()
