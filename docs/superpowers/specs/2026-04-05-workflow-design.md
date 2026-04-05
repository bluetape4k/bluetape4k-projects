# bluetape4k-workflow 모듈 설계 스펙

> **작성일**: 2026-04-05  
> **상태**: Draft  
> **레퍼런스**: [j-easy/easy-flows](https://github.com/j-easy/easy-flows)  
> **대상**: bluetape4k-projects `utils/workflow/`

---

## 1. 개요

easy-flows의 Work/WorkFlow 패턴을 Kotlin 이디엄으로 재설계하여 `bluetape4k-workflow` 모듈을 신규 생성한다.
동기(Virtual Threads 포함) 및 비동기(Coroutines suspend/Flow) 두 가지 실행 모델을 제공하며,
Kotlin DSL 빌더를 통해 워크플로를 선언적으로 구성할 수 있도록 한다.

### 레퍼런스 분석 (easy-flows 핵심 개념)

| easy-flows 개념 | 설명 |
|-----------------|------|
| `Work` | 작업 단위 인터페이스 — `execute(WorkContext): WorkReport` |
| `WorkReport` | 실행 결과 (status + context + error) |
| `WorkStatus` | `COMPLETED`, `FAILED` |
| `WorkContext` | `ConcurrentHashMap` 기반 키-값 저장소 |
| `SequentialFlow` | 작업 순차 실행, 하나라도 FAILED면 중단 |
| `ParallelFlow` | ExecutorService로 병렬 실행, 전체 완료 대기 |
| `ConditionalFlow` | Predicate 평가 후 then/otherwise 분기 |
| `RepeatFlow` | Predicate가 true인 동안 반복 실행 |

### 범위

| 범위 | 동기 | 코루틴 |
|------|:----:|:------:|
| 핵심 Work/WorkReport 추상화 | O | O |
| SequentialFlow | O | O |
| ParallelFlow (Virtual Threads / async) | O | O |
| ConditionalFlow | O | O |
| RepeatFlow | O | O |
| DSL 빌더 | O | O |
| WorkContext 스레드/코루틴 안전성 | O | O |
| Flow\<WorkReport\> 스트리밍 | - | O |
| 에러 처리 전략 (stop/continue/retry) | O | O |

---

## 2. 모듈 위치

```
utils/workflow/                   # bluetape4k-workflow
├── build.gradle.kts
├── README.md
├── README.ko.md
└── src/
    ├── main/kotlin/io/bluetape4k/workflow/
    │   ├── WorkflowDefaults.kt
    │   ├── api/                  # 핵심 추상화
    │   ├── core/                 # 동기 구현 + DSL
    │   └── coroutines/           # 코루틴 구현 + DSL
    └── test/kotlin/io/bluetape4k/workflow/
        ├── core/
        └── coroutines/
```

---

## 3. 핵심 추상화 (`api/`)

### 3.1 WorkStatus

```kotlin
package io.bluetape4k.workflow.api

/**
 * 작업 실행 상태를 나타냅니다.
 */
enum class WorkStatus {
    /** 작업이 성공적으로 완료됨 */
    COMPLETED,
    /** 작업이 실패함 */
    FAILED,
    /** 일부 작업이 실패했으나 나머지는 성공 (CONTINUE 전략) */
    PARTIAL,
}
```

### 3.2 WorkReport — sealed interface

```kotlin
package io.bluetape4k.workflow.api

/**
 * 작업 실행 결과를 표현하는 sealed 인터페이스입니다.
 *
 * ```kotlin
 * val report = work.execute(context)
 * when (report) {
 *     is WorkReport.Success -> println("완료: ${report.context}")
 *     is WorkReport.Failure -> println("실패: ${report.error?.message}")
 *     is WorkReport.PartialSuccess -> println("부분 성공: ${report.failedReports.size}개 실패")
 * }
 * ```
 */
sealed interface WorkReport {
    /** 실행 상태 */
    val status: WorkStatus
    /** 실행 컨텍스트 */
    val context: WorkContext
    /** 실행 중 발생한 에러 (없으면 null) */
    val error: Throwable?

    /**
     * 성공 결과입니다.
     */
    data class Success(
        override val context: WorkContext,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.COMPLETED
        override val error: Throwable? = null
    }

    /**
     * 실패 결과입니다.
     */
    data class Failure(
        override val context: WorkContext,
        override val error: Throwable? = null,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.FAILED
    }

    /**
     * CONTINUE 전략으로 실행 완료됐으나 하나 이상의 Work가 실패한 경우입니다.
     *
     * [failedReports]에 실패한 각 Work의 [WorkReport]가 누적됩니다.
     * 마지막 Work가 성공했더라도, 중간 실패가 있으면 이 타입으로 반환됩니다.
     *
     * ```kotlin
     * val report = sequentialFlow.execute(context)
     * if (report is WorkReport.PartialSuccess) {
     *     report.failedReports.forEach { println("실패: ${it.error?.message}") }
     * }
     * ```
     */
    data class PartialSuccess(
        override val context: WorkContext,
        val failedReports: List<WorkReport>,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.PARTIAL
        override val error: Throwable? = failedReports.firstOrNull()?.error
    }
}
```

### 3.3 WorkContext — 스레드 안전 키-값 저장소

```kotlin
package io.bluetape4k.workflow.api

import java.util.concurrent.ConcurrentHashMap
import java.util.function.BiFunction

/**
 * 워크플로 실행 컨텍스트입니다.
 *
 * 내부적으로 [ConcurrentHashMap]을 사용해 스레드 안전성을 보장합니다.
 * ParallelFlow에서 여러 Work가 동시에 접근하더라도 안전합니다.
 *
 * ### 병렬 사용 시 주의사항
 * - 개별 `get`/`set` 연산은 스레드 안전합니다.
 * - **read-modify-write** 패턴(예: 값을 읽고 → 변환하고 → 다시 저장)은
 *   race condition이 발생할 수 있으므로, [compute] 메서드를 사용하세요.
 * - 병렬 플로우에서는 각 Work가 **서로 다른 키**를 사용하는 것을 권장합니다.
 *
 * ```kotlin
 * val ctx = WorkContext()
 * ctx["orderId"] = 42L
 * val orderId: Long? = ctx.get("orderId")
 *
 * // read-modify-write 안전 패턴
 * ctx.compute("counter") { _, old -> ((old as? Int) ?: 0) + 1 }
 * ```
 */
class WorkContext(
    private val store: ConcurrentHashMap<String, Any> = ConcurrentHashMap(),
) {
    /** 키로 값을 조회합니다. */
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Any> get(key: String): T? = store[key] as? T

    /** 키-값을 저장합니다. */
    operator fun set(key: String, value: Any) {
        store[key] = value
    }

    /** 키를 제거합니다. */
    fun remove(key: String): Any? = store.remove(key)

    /** 키 존재 여부를 반환합니다. */
    fun contains(key: String): Boolean = store.containsKey(key)

    /**
     * 원자적 read-modify-write 연산을 수행합니다.
     *
     * [ConcurrentHashMap.compute]에 위임하여 race condition 없이 값을 갱신합니다.
     * 병렬 플로우에서 동일 키를 갱신해야 할 때 반드시 이 메서드를 사용하세요.
     *
     * @param key 대상 키
     * @param remapper (키, 기존값?) -> 새 값 (null 반환 시 키 제거)
     * @return 갱신된 값 또는 null
     */
    fun compute(key: String, remapper: BiFunction<String, Any?, Any?>): Any? =
        store.compute(key, remapper)

    /** 스냅샷 복사본을 반환합니다. */
    fun snapshot(): Map<String, Any> = store.toMap()

    /** 컨텍스트를 병합합니다. other의 키가 우선합니다. */
    fun merge(other: WorkContext): WorkContext {
        val merged = ConcurrentHashMap(store)
        merged.putAll(other.store)
        return WorkContext(merged)
    }

    override fun toString(): String = "WorkContext(${store.keys})"
}

/** 빈 [WorkContext]를 생성합니다. */
fun workContext(vararg pairs: Pair<String, Any>): WorkContext =
    WorkContext(ConcurrentHashMap(pairs.toMap()))
```

### 3.4 Work — fun interface (동기)

```kotlin
package io.bluetape4k.workflow.api

/**
 * 동기 작업 단위 인터페이스입니다.
 *
 * SAM 인터페이스이므로 람다로 간단히 생성할 수 있습니다.
 * 작업 이름이 필요한 경우 [NamedWork] 래퍼 또는 [Work] 팩토리 함수를 사용하세요.
 *
 * ```kotlin
 * // SAM 변환 — 이름 없이 간단히 생성
 * val work = Work { ctx ->
 *     ctx["result"] = compute()
 *     WorkReport.Success(ctx)
 * }
 *
 * // 이름 지정 — 팩토리 함수 사용
 * val namedWork = Work("validate-order") { ctx ->
 *     ctx["valid"] = true
 *     WorkReport.Success(ctx)
 * }
 * ```
 */
fun interface Work {
    /**
     * 작업을 실행합니다.
     *
     * @param context 실행 컨텍스트
     * @return 실행 결과
     */
    fun execute(context: WorkContext): WorkReport
}

/**
 * 이름을 가진 [Work] 래퍼입니다.
 *
 * [Work]가 `fun interface`이므로 default property를 가질 수 없습니다.
 * 작업 이름이 필요한 경우(로깅, 디버깅 등) 이 클래스를 사용하세요.
 *
 * ```kotlin
 * val work = NamedWork("validate") { ctx ->
 *     ctx["valid"] = true
 *     WorkReport.Success(ctx)
 * }
 * println(work.name) // "validate"
 * ```
 */
class NamedWork(
    val name: String,
    private val delegate: Work,
) : Work {
    override fun execute(context: WorkContext): WorkReport = delegate.execute(context)
    override fun toString(): String = "NamedWork($name)"
}

/**
 * 이름 지정 [Work] 팩토리 함수입니다.
 *
 * @param name 작업 이름
 * @param block 작업 실행 로직
 * @return 이름이 부여된 [NamedWork]
 */
fun Work(name: String, block: (WorkContext) -> WorkReport): NamedWork =
    NamedWork(name, block)
```

### 3.5 SuspendWork — fun interface (코루틴)

```kotlin
package io.bluetape4k.workflow.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * 코루틴 기반 비동기 작업 단위 인터페이스입니다.
 *
 * SAM 인터페이스이므로 람다로 간단히 생성할 수 있습니다.
 * 작업 이름이 필요한 경우 [NamedSuspendWork] 래퍼 또는 [SuspendWork] 팩토리 함수를 사용하세요.
 *
 * ```kotlin
 * // SAM 변환
 * val work = SuspendWork { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 *
 * // 이름 지정
 * val namedWork = SuspendWork("fetch-data") { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 * ```
 */
fun interface SuspendWork {
    /**
     * 작업을 비동기로 실행합니다.
     *
     * @param context 실행 컨텍스트
     * @return 실행 결과
     */
    suspend fun execute(context: WorkContext): WorkReport
}

/**
 * 이름을 가진 [SuspendWork] 래퍼입니다.
 *
 * [SuspendWork]가 `fun interface`이므로 default property를 가질 수 없습니다.
 * 작업 이름이 필요한 경우(로깅, 디버깅 등) 이 클래스를 사용하세요.
 *
 * ```kotlin
 * val work = NamedSuspendWork("fetch-data") { ctx ->
 *     val data = httpClient.fetchAsync(ctx.get<String>("url")!!)
 *     ctx["data"] = data
 *     WorkReport.Success(ctx)
 * }
 * ```
 */
class NamedSuspendWork(
    val name: String,
    private val delegate: SuspendWork,
) : SuspendWork {
    override suspend fun execute(context: WorkContext): WorkReport = delegate.execute(context)
    override fun toString(): String = "NamedSuspendWork($name)"
}

/**
 * 이름 지정 [SuspendWork] 팩토리 함수입니다.
 *
 * @param name 작업 이름
 * @param block 작업 실행 로직
 * @return 이름이 부여된 [NamedSuspendWork]
 */
fun SuspendWork(name: String, block: suspend (WorkContext) -> WorkReport): NamedSuspendWork =
    NamedSuspendWork(name, block)

/**
 * 동기 [Work]를 코루틴 [SuspendWork]로 변환합니다.
 *
 * [Dispatchers.IO]에서 블로킹 실행을 래핑합니다.
 *
 * ```kotlin
 * val suspendWork = blockingWork.asSuspend()
 * ```
 */
fun Work.asSuspend(): SuspendWork = SuspendWork { ctx ->
    withContext(Dispatchers.IO) { execute(ctx) }
}

/**
 * 코루틴 [SuspendWork]를 동기 [Work]로 변환합니다.
 *
 * 내부에서 [runBlocking]을 사용하므로 코루틴 컨텍스트 내에서는 사용하지 마세요.
 *
 * ```kotlin
 * val blockingWork = suspendWork.asBlocking()
 * ```
 */
fun SuspendWork.asBlocking(): Work = Work { ctx ->
    runBlocking { execute(ctx) }
}
```

### 3.6 WorkFlow — 마커 인터페이스

```kotlin
package io.bluetape4k.workflow.api

/**
 * 워크플로 인터페이스입니다.
 *
 * [Work]를 확장하여 워크플로 자체도 작업 단위로 합성(composite)할 수 있습니다.
 */
interface WorkFlow : Work

/**
 * 코루틴 워크플로 인터페이스입니다.
 *
 * [SuspendWork]를 확장하여 코루틴 워크플로도 합성할 수 있습니다.
 */
interface SuspendWorkFlow : SuspendWork
```

### 3.7 에러 처리 전략

```kotlin
package io.bluetape4k.workflow.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 워크플로 실행 중 에러 발생 시 동작을 결정하는 전략입니다.
 *
 * ```kotlin
 * val flow = sequentialFlow {
 *     errorStrategy = ErrorStrategy.CONTINUE
 *     execute(work1)
 *     execute(work2)
 * }
 * ```
 */
enum class ErrorStrategy {
    /** 에러 발생 시 즉시 중단하고 Failure를 반환 (기본값) */
    STOP,
    /** 에러를 누적하고 다음 작업을 계속 실행. 실패가 있으면 PartialSuccess 반환 */
    CONTINUE,
}

/**
 * 재시도 정책입니다.
 *
 * [Duration] 기반의 타입 안전한 지연 시간을 사용합니다.
 *
 * @property maxAttempts 최대 총 시도 횟수. 최초 실행 1회 + 재시도 횟수.
 *   예: maxAttempts = 3 이면 최초 실행 1회 + 재시도 2회 = 총 3회 시도.
 *   (1 = 재시도 없음, 최초 실행만)
 * @property delay 재시도 간 대기 시간
 * @property backoffMultiplier 지수 백오프 배율 (1.0 = 고정 지연)
 * @property maxDelay 백오프 적용 시 최대 지연 시간 상한
 */
data class RetryPolicy(
    val maxAttempts: Int = 1,
    val delay: Duration = Duration.ZERO,
    val backoffMultiplier: Double = 1.0,
    val maxDelay: Duration = 1.minutes,
) {
    /** 편의 프로퍼티: 재시도 횟수 (= maxAttempts - 1) */
    val maxRetries: Int get() = maxAttempts - 1

    companion object {
        /** 재시도 없음 (최초 실행 1회만) */
        val NONE = RetryPolicy()

        /** 기본값: 총 3회 시도 (최초 1회 + 재시도 2회), 100ms 간격, 지수 백오프 x2, 최대 1분 */
        val DEFAULT = RetryPolicy(
            maxAttempts = 3,
            delay = Duration.parse("100ms"),
            backoffMultiplier = 2.0,
            maxDelay = 1.minutes,
        )
    }
}

// NOTE: CONTINUE 전략에서의 실패 누적은 WorkReport.PartialSuccess.failedReports로
// 반환하므로, WorkContext에 별도 well-known 키를 사용하지 않습니다.
```

---

## 4. 동기 구현 (`core/`)

### 4.1 SequentialWorkFlow

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*

/**
 * 작업을 순차적으로 실행하는 워크플로입니다.
 *
 * [ErrorStrategy.STOP] (기본값)이면 실패 시 즉시 중단하고,
 * [ErrorStrategy.CONTINUE]이면 실패를 누적하고 다음 작업을 계속 실행합니다.
 * 실패가 하나라도 있으면 [WorkReport.PartialSuccess]를 반환합니다.
 *
 * ```kotlin
 * val flow = SequentialWorkFlow(
 *     works = listOf(work1, work2, work3),
 *     errorStrategy = ErrorStrategy.STOP,
 * )
 * val report = flow.execute(WorkContext())
 * ```
 */
class SequentialWorkFlow(
    private val works: List<Work>,
    private val errorStrategy: ErrorStrategy = ErrorStrategy.STOP,
    private val flowName: String = "sequential",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        var lastReport: WorkReport = WorkReport.Success(context)
        val failedReports = mutableListOf<WorkReport>()

        for (work in works) {
            val workName = (work as? NamedWork)?.name ?: work::class.simpleName ?: "anonymous"
            log.debug { "Executing work: $workName" }
            lastReport = work.execute(context)
            log.debug { "Work completed with status: ${lastReport.status}" }

            if (lastReport is WorkReport.Failure) {
                if (errorStrategy == ErrorStrategy.STOP) {
                    return lastReport
                }
                // CONTINUE 전략: 실패를 누적
                failedReports.add(lastReport)
            }
        }

        // CONTINUE 전략에서 실패가 하나라도 있으면 PartialSuccess 반환
        return if (failedReports.isNotEmpty()) {
            WorkReport.PartialSuccess(context, failedReports.toList())
        } else {
            lastReport
        }
    }
}
```

### 4.2 ParallelWorkFlow (Virtual Threads)

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * 작업을 병렬로 실행하는 워크플로입니다.
 *
 * 기본적으로 `Executors.newVirtualThreadPerTaskExecutor()`를 사용하여 병렬 실행합니다.
 * 커스텀 [ExecutorService]를 주입할 수도 있습니다.
 *
 * ### Executor 라이프사이클
 * - 외부 executor 주입 시: 호출자가 라이프사이클을 관리합니다.
 * - 내부 executor (기본): 매 실행 시 `newVirtualThreadPerTaskExecutor()`를 생성하고
 *   `shutdown()` + `awaitTermination()`으로 안전하게 종료합니다.
 *   Virtual Thread executor는 경량이므로 매번 생성해도 오버헤드가 미미합니다.
 *
 * ```kotlin
 * val flow = ParallelWorkFlow(
 *     works = listOf(work1, work2, work3),
 * )
 * val report = flow.execute(WorkContext())
 * ```
 */
class ParallelWorkFlow(
    private val works: List<Work>,
    private val executorService: ExecutorService? = null,
    private val timeout: Duration = 5.minutes,
    private val flowName: String = "parallel",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        val executor = executorService ?: Executors.newVirtualThreadPerTaskExecutor()
        val useInternalExecutor = executorService == null

        try {
            // invokeAll에 timeout을 직접 전달하여 실제 시간 제한을 보장
            val callables = works.map { work ->
                val workName = (work as? NamedWork)?.name ?: work::class.simpleName ?: "anonymous"
                log.debug { "Submitting work: $workName" }
                java.util.concurrent.Callable<WorkReport> { work.execute(context) }
            }

            val futures = executor.invokeAll(
                callables,
                timeout.inWholeMilliseconds,
                TimeUnit.MILLISECONDS,
            )

            // invokeAll이 timeout 초과 시 미완료 태스크는 cancel 처리됨
            val reports = futures.map { future ->
                if (future.isDone && !future.isCancelled) {
                    future.get()
                } else {
                    WorkReport.Failure(
                        context,
                        java.util.concurrent.TimeoutException("Work did not complete within $timeout"),
                    )
                }
            }

            val failures = reports.filterIsInstance<WorkReport.Failure>()

            return if (failures.isEmpty()) {
                log.debug { "All parallel works completed successfully" }
                WorkReport.Success(context)
            } else {
                log.debug { "Parallel flow completed with ${failures.size} failure(s)" }
                WorkReport.Failure(context, failures.first().error)
            }
        } finally {
            if (useInternalExecutor) {
                executor.shutdown()
                if (!executor.awaitTermination(timeout.inWholeSeconds, TimeUnit.SECONDS)) {
                    log.debug { "Executor did not terminate within timeout, forcing shutdown" }
                    executor.shutdownNow()
                }
            }
        }
    }
}
```

### 4.3 ConditionalWorkFlow

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*

/**
 * 조건 평가 후 분기 실행하는 워크플로입니다.
 *
 * [predicate]가 true이면 [thenWork]를, false이면 [otherwiseWork]를 실행합니다.
 * [otherwiseWork]가 null이면 no-op(Success)를 반환합니다.
 *
 * ```kotlin
 * val flow = ConditionalWorkFlow(
 *     predicate = { ctx -> ctx.get<Boolean>("isVip") == true },
 *     thenWork = vipWork,
 *     otherwiseWork = normalWork,
 * )
 * ```
 */
class ConditionalWorkFlow(
    private val predicate: (WorkContext) -> Boolean,
    private val thenWork: Work,
    private val otherwiseWork: Work? = null,
    private val flowName: String = "conditional",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        val result = predicate(context)
        log.debug { "Condition evaluated to: $result" }
        return if (result) {
            thenWork.execute(context)
        } else {
            otherwiseWork?.execute(context) ?: WorkReport.Success(context)
        }
    }
}
```

### 4.4 RepeatWorkFlow

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*

/**
 * 조건이 충족되는 동안 작업을 반복 실행하는 워크플로입니다.
 *
 * [repeatPredicate]가 true를 반환하는 동안 [work]를 반복 실행합니다.
 * [maxIterations]로 무한 루프를 방지합니다.
 *
 * ```kotlin
 * val flow = RepeatWorkFlow(
 *     work = incrementWork,
 *     repeatPredicate = { report -> report is WorkReport.Success },
 *     maxIterations = 10,
 * )
 * ```
 */
class RepeatWorkFlow(
    private val work: Work,
    private val repeatPredicate: (WorkReport) -> Boolean,
    private val maxIterations: Int = Int.MAX_VALUE,
    private val flowName: String = "repeat",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        var report: WorkReport = work.execute(context)
        var iteration = 1
        log.debug { "Repeat iteration 1 completed with status: ${report.status}" }

        while (repeatPredicate(report) && iteration < maxIterations) {
            report = work.execute(context)
            iteration++
            log.debug { "Repeat iteration $iteration completed with status: ${report.status}" }
        }
        return report
    }
}
```

### 4.5 RetryWorkFlow

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import kotlin.time.Duration

/**
 * 실패 시 [RetryPolicy]에 따라 재시도하는 워크플로입니다.
 *
 * ```kotlin
 * val flow = RetryWorkFlow(
 *     work = unreliableWork,
 *     retryPolicy = RetryPolicy(maxAttempts = 3, delay = Duration.parse("100ms"), backoffMultiplier = 2.0),
 * )
 * ```
 */
class RetryWorkFlow(
    private val work: Work,
    private val retryPolicy: RetryPolicy,
    private val flowName: String = "retry",
) : WorkFlow {

    companion object : KLogging()

    override fun execute(context: WorkContext): WorkReport {
        var lastReport: WorkReport = WorkReport.Failure(context, IllegalStateException("Not started"))
        var currentDelay = retryPolicy.delay

        repeat(retryPolicy.maxAttempts) { attempt ->
            log.debug { "Retry attempt ${attempt + 1}/${retryPolicy.maxAttempts}" }
            lastReport = work.execute(context)
            log.debug { "Attempt ${attempt + 1} completed with status: ${lastReport.status}" }
            if (lastReport is WorkReport.Success) return lastReport

            if (attempt < retryPolicy.maxAttempts - 1 && currentDelay > Duration.ZERO) {
                Thread.sleep(currentDelay.inWholeMilliseconds)
                currentDelay = minOf(currentDelay * retryPolicy.backoffMultiplier, retryPolicy.maxDelay)
            }
        }
        return lastReport
    }
}

/**
 * [Duration]에 [Double]을 곱하는 확장 연산자입니다.
 */
private operator fun Duration.times(factor: Double): Duration =
    (this.inWholeMilliseconds * factor).toLong().let { Duration.parse("${it}ms") }
```

---

## 5. 코루틴 구현 (`coroutines/`)

### 5.1 SuspendSequentialFlow

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * 코루틴 기반 순차 워크플로입니다.
 *
 * [ErrorStrategy.CONTINUE] 전략 사용 시, 실패를 누적하고 다음 작업을 계속 실행합니다.
 * 실패가 하나라도 있으면 [WorkReport.PartialSuccess]를 반환합니다.
 * 각 작업 실행 전 코루틴 취소 여부를 확인합니다.
 *
 * ```kotlin
 * val flow = SuspendSequentialFlow(
 *     works = listOf(suspendWork1, suspendWork2),
 * )
 * val report = flow.execute(WorkContext())
 * ```
 */
class SuspendSequentialFlow(
    private val works: List<SuspendWork>,
    private val errorStrategy: ErrorStrategy = ErrorStrategy.STOP,
    private val flowName: String = "suspend-sequential",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        var lastReport: WorkReport = WorkReport.Success(context)
        val failedReports = mutableListOf<WorkReport>()

        for (work in works) {
            coroutineContext.ensureActive()
            val workName = (work as? NamedSuspendWork)?.name ?: work::class.simpleName ?: "anonymous"
            log.debug { "Executing suspend work: $workName" }
            lastReport = work.execute(context)
            log.debug { "Suspend work completed with status: ${lastReport.status}" }

            if (lastReport is WorkReport.Failure) {
                if (errorStrategy == ErrorStrategy.STOP) {
                    return lastReport
                }
                // CONTINUE 전략: 실패를 누적
                failedReports.add(lastReport)
            }
        }

        // CONTINUE 전략에서 실패가 하나라도 있으면 PartialSuccess 반환
        return if (failedReports.isNotEmpty()) {
            WorkReport.PartialSuccess(context, failedReports.toList())
        } else {
            lastReport
        }
    }
}
```

### 5.2 SuspendParallelFlow

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 코루틴 기반 병렬 워크플로입니다.
 *
 * [coroutineScope] + [async]를 활용하여 구조화된 동시성을 보장합니다.
 *
 * ```kotlin
 * val flow = SuspendParallelFlow(
 *     works = listOf(suspendWork1, suspendWork2, suspendWork3),
 * )
 * val report = flow.execute(WorkContext())
 * ```
 */
class SuspendParallelFlow(
    private val works: List<SuspendWork>,
    private val flowName: String = "suspend-parallel",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport = coroutineScope {
        log.debug { "Starting ${works.size} parallel suspend works" }
        val reports = works.map { work ->
            async { work.execute(context) }
        }.awaitAll()

        val failures = reports.filterIsInstance<WorkReport.Failure>()
        if (failures.isEmpty()) {
            log.debug { "All parallel suspend works completed successfully" }
            WorkReport.Success(context)
        } else {
            log.debug { "Parallel suspend flow completed with ${failures.size} failure(s)" }
            WorkReport.Failure(context, failures.first().error)
        }
    }
}
```

### 5.3 SuspendConditionalFlow

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*

/**
 * 코루틴 기반 조건 분기 워크플로입니다.
 *
 * [otherwiseWork]가 null이면 no-op(Success)를 반환합니다.
 *
 * ```kotlin
 * val flow = SuspendConditionalFlow(
 *     predicate = { ctx -> ctx.get<Boolean>("approved") == true },
 *     thenWork = processWork,
 *     otherwiseWork = rejectWork,
 * )
 * ```
 */
class SuspendConditionalFlow(
    private val predicate: suspend (WorkContext) -> Boolean,
    private val thenWork: SuspendWork,
    private val otherwiseWork: SuspendWork? = null,
    private val flowName: String = "suspend-conditional",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        val result = predicate(context)
        log.debug { "Suspend condition evaluated to: $result" }
        return if (result) {
            thenWork.execute(context)
        } else {
            otherwiseWork?.execute(context) ?: WorkReport.Success(context)
        }
    }
}
```

### 5.4 SuspendRepeatFlow

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration

/**
 * 코루틴 기반 반복 워크플로입니다.
 *
 * 각 반복 전 코루틴 취소 여부를 확인합니다.
 *
 * ```kotlin
 * val flow = SuspendRepeatFlow(
 *     work = pollWork,
 *     repeatPredicate = { report -> report is WorkReport.Failure },
 *     maxIterations = 5,
 *     repeatDelay = Duration.parse("500ms"),
 * )
 * ```
 */
class SuspendRepeatFlow(
    private val work: SuspendWork,
    private val repeatPredicate: suspend (WorkReport) -> Boolean,
    private val maxIterations: Int = Int.MAX_VALUE,
    private val repeatDelay: Duration = Duration.ZERO,
    private val flowName: String = "suspend-repeat",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        var report: WorkReport = work.execute(context)
        var iteration = 1
        log.debug { "Suspend repeat iteration 1 completed with status: ${report.status}" }

        while (repeatPredicate(report) && iteration < maxIterations) {
            coroutineContext.ensureActive()
            if (repeatDelay > Duration.ZERO) delay(repeatDelay)
            report = work.execute(context)
            iteration++
            log.debug { "Suspend repeat iteration $iteration completed with status: ${report.status}" }
        }
        return report
    }
}
```

### 5.5 SuspendRetryFlow

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.workflow.api.*
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * 코루틴 기반 재시도 워크플로입니다.
 *
 * ```kotlin
 * val flow = SuspendRetryFlow(
 *     work = unreliableWork,
 *     retryPolicy = RetryPolicy.DEFAULT,
 * )
 * ```
 */
class SuspendRetryFlow(
    private val work: SuspendWork,
    private val retryPolicy: RetryPolicy,
    private val flowName: String = "suspend-retry",
) : SuspendWorkFlow {

    companion object : KLogging()

    override suspend fun execute(context: WorkContext): WorkReport {
        var lastReport: WorkReport = WorkReport.Failure(context, IllegalStateException("Not started"))
        var currentDelay = retryPolicy.delay

        repeat(retryPolicy.maxAttempts) { attempt ->
            log.debug { "Suspend retry attempt ${attempt + 1}/${retryPolicy.maxAttempts}" }
            lastReport = work.execute(context)
            log.debug { "Attempt ${attempt + 1} completed with status: ${lastReport.status}" }
            if (lastReport is WorkReport.Success) return lastReport

            if (attempt < retryPolicy.maxAttempts - 1 && currentDelay > Duration.ZERO) {
                delay(currentDelay)
                currentDelay = minOf(currentDelay * retryPolicy.backoffMultiplier, retryPolicy.maxDelay)
            }
        }
        return lastReport
    }
}

/**
 * [Duration]에 [Double]을 곱하는 확장 연산자입니다.
 */
private operator fun Duration.times(factor: Double): Duration =
    (this.inWholeMilliseconds * factor).toLong().let { Duration.parse("${it}ms") }
```

### 5.6 Flow\<WorkReport\> 스트리밍 지원

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [SuspendWork] 목록을 순차 실행하면서 각 결과를 [Flow]로 방출합니다.
 *
 * ```kotlin
 * val reports: Flow<WorkReport> = workReportFlow(listOf(work1, work2, work3), context)
 * reports.collect { report -> println(report.status) }
 * ```
 */
fun workReportFlow(
    works: List<SuspendWork>,
    context: WorkContext,
): Flow<WorkReport> = flow {
    for (work in works) {
        emit(work.execute(context))
    }
}

/**
 * [SuspendWork] 목록을 순차 실행하면서 성공 결과만 [Flow]로 방출합니다.
 * 실패 시 [ErrorStrategy]에 따라 동작합니다.
 *
 * ```kotlin
 * val reports = workReportFlow(works, context)
 *     .onEach { if (it is WorkReport.Failure) log.warn { "실패: ${it.error}" } }
 *     .filter { it is WorkReport.Success }
 * ```
 */
fun SuspendWork.asFlow(context: WorkContext): Flow<WorkReport> = flow {
    emit(execute(context))
}
```

---

## 6. DSL 빌더

### 6.1 DSL 마커 어노테이션

```kotlin
package io.bluetape4k.workflow.api

/**
 * Workflow DSL 마커 어노테이션입니다.
 *
 * 동기/코루틴 DSL 모두 이 단일 마커를 공유합니다.
 */
@DslMarker
annotation class WorkflowDsl
```

### 6.2 동기 DSL

```kotlin
package io.bluetape4k.workflow.core

import io.bluetape4k.workflow.api.*

/**
 * 동기 순차 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = sequentialFlow("order-process") {
 *     errorStrategy = ErrorStrategy.STOP
 *     execute(validateWork)
 *     execute(processWork)
 *     execute(notifyWork)
 * }
 * ```
 */
@WorkflowDsl
class SequentialFlowBuilder(private val name: String = "sequential") {
    var errorStrategy: ErrorStrategy = ErrorStrategy.STOP
    private val works = mutableListOf<Work>()

    /** [Work]를 추가합니다. */
    fun execute(work: Work) {
        works.add(work)
    }

    /** 람다로 이름 지정 [Work]를 추가합니다. */
    fun execute(name: String = "anonymous", block: (WorkContext) -> WorkReport) {
        works.add(NamedWork(name, block))
    }

    /** 내부에 병렬 플로우를 삽입합니다. */
    fun parallel(name: String = "parallel", block: ParallelFlowBuilder.() -> Unit) {
        works.add(parallelFlow(name, block))
    }

    /** 내부에 조건 분기 플로우를 삽입합니다. */
    fun conditional(name: String = "conditional", block: ConditionalFlowBuilder.() -> Unit) {
        works.add(conditionalFlow(name, block))
    }

    /** 내부에 반복 플로우를 삽입합니다. */
    fun repeat(name: String = "repeat", block: RepeatFlowBuilder.() -> Unit) {
        works.add(repeatFlow(name, block))
    }

    /** 내부에 재시도 플로우를 삽입합니다. */
    fun retry(name: String = "retry", block: RetryFlowBuilder.() -> Unit) {
        works.add(retryFlow(name, block))
    }

    internal fun build(): SequentialWorkFlow = SequentialWorkFlow(works.toList(), errorStrategy, name)
}

fun sequentialFlow(
    name: String = "sequential",
    block: SequentialFlowBuilder.() -> Unit,
): SequentialWorkFlow = SequentialFlowBuilder(name).apply(block).build()

/**
 * 동기 병렬 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = parallelFlow("fetch-all") {
 *     execute(fetchUserWork)
 *     execute(fetchOrderWork)
 *     execute(fetchInventoryWork)
 * }
 * ```
 */
@WorkflowDsl
class ParallelFlowBuilder(private val name: String = "parallel") {
    private val works = mutableListOf<Work>()
    var executorService: java.util.concurrent.ExecutorService? = null

    fun execute(work: Work) {
        works.add(work)
    }

    fun execute(name: String = "anonymous", block: (WorkContext) -> WorkReport) {
        works.add(NamedWork(name, block))
    }

    internal fun build(): ParallelWorkFlow = ParallelWorkFlow(works.toList(), executorService, flowName = name)
}

fun parallelFlow(
    name: String = "parallel",
    block: ParallelFlowBuilder.() -> Unit,
): ParallelWorkFlow = ParallelFlowBuilder(name).apply(block).build()

/**
 * 동기 조건 분기 워크플로 DSL 빌더입니다.
 *
 * [otherwise]는 선택적이며, 생략 시 no-op(Success)를 반환합니다.
 *
 * ```kotlin
 * val flow = conditionalFlow("check-vip") {
 *     condition { ctx -> ctx.get<Boolean>("isVip") == true }
 *     then(vipProcessWork)
 *     otherwise(normalProcessWork)  // 선택적
 * }
 * ```
 */
@WorkflowDsl
class ConditionalFlowBuilder(private val name: String = "conditional") {
    private var predicate: ((WorkContext) -> Boolean)? = null
    private var thenWork: Work? = null
    private var otherwiseWork: Work? = null

    fun condition(predicate: (WorkContext) -> Boolean) {
        this.predicate = predicate
    }

    fun then(work: Work) {
        this.thenWork = work
    }

    /** otherwise는 선택적입니다. 생략 시 조건 미충족 시 no-op(Success)를 반환합니다. */
    fun otherwise(work: Work) {
        this.otherwiseWork = work
    }

    internal fun build(): ConditionalWorkFlow {
        requireNotNull(predicate) { "condition은 필수입니다" }
        requireNotNull(thenWork) { "then work는 필수입니다" }
        return ConditionalWorkFlow(predicate!!, thenWork!!, otherwiseWork, name)
    }
}

fun conditionalFlow(
    name: String = "conditional",
    block: ConditionalFlowBuilder.() -> Unit,
): ConditionalWorkFlow = ConditionalFlowBuilder(name).apply(block).build()

/**
 * 동기 반복 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = repeatFlow("poll-status") {
 *     execute(pollWork)
 *     until { report -> report is WorkReport.Success }
 *     maxIterations = 10
 * }
 * ```
 */
@WorkflowDsl
class RepeatFlowBuilder(private val name: String = "repeat") {
    private var work: Work? = null
    private var repeatPredicate: ((WorkReport) -> Boolean)? = null
    var maxIterations: Int = Int.MAX_VALUE

    fun execute(work: Work) {
        this.work = work
    }

    /** report가 조건을 만족하는 동안 반복합니다. */
    fun repeatWhile(predicate: (WorkReport) -> Boolean) {
        this.repeatPredicate = predicate
    }

    /** report가 조건을 만족하면 중단합니다. */
    fun until(predicate: (WorkReport) -> Boolean) {
        this.repeatPredicate = { report -> !predicate(report) }
    }

    internal fun build(): RepeatWorkFlow {
        requireNotNull(work) { "execute work는 필수입니다" }
        requireNotNull(repeatPredicate) { "repeatWhile 또는 until 조건은 필수입니다" }
        return RepeatWorkFlow(work!!, repeatPredicate!!, maxIterations, name)
    }
}

fun repeatFlow(
    name: String = "repeat",
    block: RepeatFlowBuilder.() -> Unit,
): RepeatWorkFlow = RepeatFlowBuilder(name).apply(block).build()

/**
 * 동기 재시도 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = retryFlow("retry-unstable") {
 *     execute(unstableWork)
 *     policy(RetryPolicy.DEFAULT)
 * }
 * ```
 */
@WorkflowDsl
class RetryFlowBuilder(private val name: String = "retry") {
    private var work: Work? = null
    private var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    fun execute(work: Work) {
        this.work = work
    }

    fun policy(retryPolicy: RetryPolicy) {
        this.retryPolicy = retryPolicy
    }

    internal fun build(): RetryWorkFlow {
        requireNotNull(work) { "execute work는 필수입니다" }
        return RetryWorkFlow(work!!, retryPolicy, name)
    }
}

fun retryFlow(
    name: String = "retry",
    block: RetryFlowBuilder.() -> Unit,
): RetryWorkFlow = RetryFlowBuilder(name).apply(block).build()

/**
 * 복합 워크플로 DSL입니다. 순차/병렬/조건/반복/재시도를 중첩하여 구성합니다.
 *
 * ```kotlin
 * val pipeline = workflow("order-pipeline") {
 *     sequential {
 *         execute(validateWork)
 *         parallel {
 *             execute(fetchInventoryWork)
 *             execute(calculatePriceWork)
 *         }
 *         conditional {
 *             condition { ctx -> ctx.get<Boolean>("inStock") == true }
 *             then(shipWork)
 *             otherwise(backorderWork)
 *         }
 *         repeat {
 *             execute(pollWork)
 *             until { report -> report is WorkReport.Success }
 *             maxIterations = 5
 *         }
 *     }
 * }
 * ```
 */
@WorkflowDsl
class WorkflowBuilder(private val name: String = "workflow") {
    private var rootWork: Work? = null

    private fun setRoot(work: Work) {
        require(rootWork == null) { "루트 워크플로가 이미 선언되었습니다. 루트는 하나만 지정할 수 있습니다." }
        rootWork = work
    }

    fun sequential(name: String = "sequential", block: SequentialFlowBuilder.() -> Unit) {
        setRoot(sequentialFlow(name, block))
    }

    fun parallel(name: String = "parallel", block: ParallelFlowBuilder.() -> Unit) {
        setRoot(parallelFlow(name, block))
    }

    fun conditional(name: String = "conditional", block: ConditionalFlowBuilder.() -> Unit) {
        setRoot(conditionalFlow(name, block))
    }

    fun repeat(name: String = "repeat", block: RepeatFlowBuilder.() -> Unit) {
        setRoot(repeatFlow(name, block))
    }

    internal fun build(): WorkFlow {
        requireNotNull(rootWork) { "workflow에는 최소 하나의 플로우가 필요합니다" }
        return rootWork as WorkFlow
    }
}

fun workflow(
    name: String = "workflow",
    block: WorkflowBuilder.() -> Unit,
): WorkFlow = WorkflowBuilder(name).apply(block).build()
```

### 6.3 코루틴 DSL

```kotlin
package io.bluetape4k.workflow.coroutines

import io.bluetape4k.workflow.api.*
import kotlin.time.Duration

/**
 * 코루틴 순차 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = suspendSequentialFlow("async-process") {
 *     errorStrategy = ErrorStrategy.STOP
 *     execute(suspendWork1)
 *     execute(suspendWork2)
 * }
 * ```
 */
@WorkflowDsl
class SuspendSequentialFlowBuilder(private val name: String = "suspend-sequential") {
    var errorStrategy: ErrorStrategy = ErrorStrategy.STOP
    private val works = mutableListOf<SuspendWork>()

    fun execute(work: SuspendWork) {
        works.add(work)
    }

    fun execute(name: String = "anonymous", block: suspend (WorkContext) -> WorkReport) {
        works.add(NamedSuspendWork(name, block))
    }

    /** 내부에 병렬 플로우를 삽입합니다. */
    fun parallel(name: String = "parallel", block: SuspendParallelFlowBuilder.() -> Unit) {
        works.add(suspendParallelFlow(name, block))
    }

    /** 내부에 조건 분기 플로우를 삽입합니다. */
    fun conditional(name: String = "conditional", block: SuspendConditionalFlowBuilder.() -> Unit) {
        works.add(suspendConditionalFlow(name, block))
    }

    /** 내부에 반복 플로우를 삽입합니다. */
    fun repeat(name: String = "repeat", block: SuspendRepeatFlowBuilder.() -> Unit) {
        works.add(suspendRepeatFlow(name, block))
    }

    /** 내부에 재시도 플로우를 삽입합니다. */
    fun retry(name: String = "retry", block: SuspendRetryFlowBuilder.() -> Unit) {
        works.add(suspendRetryFlow(name, block))
    }

    internal fun build(): SuspendSequentialFlow =
        SuspendSequentialFlow(works.toList(), errorStrategy, name)
}

fun suspendSequentialFlow(
    name: String = "suspend-sequential",
    block: SuspendSequentialFlowBuilder.() -> Unit,
): SuspendSequentialFlow = SuspendSequentialFlowBuilder(name).apply(block).build()

/**
 * 코루틴 병렬 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = suspendParallelFlow("async-fetch") {
 *     execute(fetchWork1)
 *     execute(fetchWork2)
 * }
 * ```
 */
@WorkflowDsl
class SuspendParallelFlowBuilder(private val name: String = "suspend-parallel") {
    private val works = mutableListOf<SuspendWork>()

    fun execute(work: SuspendWork) {
        works.add(work)
    }

    fun execute(name: String = "anonymous", block: suspend (WorkContext) -> WorkReport) {
        works.add(NamedSuspendWork(name, block))
    }

    internal fun build(): SuspendParallelFlow = SuspendParallelFlow(works.toList(), name)
}

fun suspendParallelFlow(
    name: String = "suspend-parallel",
    block: SuspendParallelFlowBuilder.() -> Unit,
): SuspendParallelFlow = SuspendParallelFlowBuilder(name).apply(block).build()

/**
 * 코루틴 조건 분기 워크플로 DSL 빌더입니다.
 *
 * [otherwise]는 선택적이며, 생략 시 no-op(Success)를 반환합니다.
 *
 * ```kotlin
 * val flow = suspendConditionalFlow("check-auth") {
 *     condition { ctx -> authService.verify(ctx.get<String>("token")!!) }
 *     then(processWork)
 *     otherwise(rejectWork)  // 선택적
 * }
 * ```
 */
@WorkflowDsl
class SuspendConditionalFlowBuilder(private val name: String = "suspend-conditional") {
    private var predicate: (suspend (WorkContext) -> Boolean)? = null
    private var thenWork: SuspendWork? = null
    private var otherwiseWork: SuspendWork? = null

    fun condition(predicate: suspend (WorkContext) -> Boolean) {
        this.predicate = predicate
    }

    fun then(work: SuspendWork) {
        this.thenWork = work
    }

    /** otherwise는 선택적입니다. 생략 시 조건 미충족 시 no-op(Success)를 반환합니다. */
    fun otherwise(work: SuspendWork) {
        this.otherwiseWork = work
    }

    internal fun build(): SuspendConditionalFlow {
        requireNotNull(predicate) { "condition은 필수입니다" }
        requireNotNull(thenWork) { "then work는 필수입니다" }
        return SuspendConditionalFlow(predicate!!, thenWork!!, otherwiseWork, name)
    }
}

fun suspendConditionalFlow(
    name: String = "suspend-conditional",
    block: SuspendConditionalFlowBuilder.() -> Unit,
): SuspendConditionalFlow = SuspendConditionalFlowBuilder(name).apply(block).build()

/**
 * 코루틴 반복 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = suspendRepeatFlow("poll-status") {
 *     execute(pollWork)
 *     until { report -> report is WorkReport.Success }
 *     maxIterations = 10
 *     repeatDelay = Duration.parse("500ms")
 * }
 * ```
 */
@WorkflowDsl
class SuspendRepeatFlowBuilder(private val name: String = "suspend-repeat") {
    private var work: SuspendWork? = null
    private var repeatPredicate: (suspend (WorkReport) -> Boolean)? = null
    var maxIterations: Int = Int.MAX_VALUE
    var repeatDelay: Duration = Duration.ZERO

    fun execute(work: SuspendWork) {
        this.work = work
    }

    fun repeatWhile(predicate: suspend (WorkReport) -> Boolean) {
        this.repeatPredicate = predicate
    }

    fun until(predicate: suspend (WorkReport) -> Boolean) {
        this.repeatPredicate = { report -> !predicate(report) }
    }

    internal fun build(): SuspendRepeatFlow {
        requireNotNull(work) { "execute work는 필수입니다" }
        requireNotNull(repeatPredicate) { "repeatWhile 또는 until 조건은 필수입니다" }
        return SuspendRepeatFlow(work!!, repeatPredicate!!, maxIterations, repeatDelay, name)
    }
}

fun suspendRepeatFlow(
    name: String = "suspend-repeat",
    block: SuspendRepeatFlowBuilder.() -> Unit,
): SuspendRepeatFlow = SuspendRepeatFlowBuilder(name).apply(block).build()

/**
 * 코루틴 재시도 워크플로 DSL 빌더입니다.
 *
 * ```kotlin
 * val flow = suspendRetryFlow("retry-unstable") {
 *     execute(unstableWork)
 *     policy(RetryPolicy.DEFAULT)
 * }
 * ```
 */
@WorkflowDsl
class SuspendRetryFlowBuilder(private val name: String = "suspend-retry") {
    private var work: SuspendWork? = null
    private var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    fun execute(work: SuspendWork) {
        this.work = work
    }

    fun policy(retryPolicy: RetryPolicy) {
        this.retryPolicy = retryPolicy
    }

    internal fun build(): SuspendRetryFlow {
        requireNotNull(work) { "execute work는 필수입니다" }
        return SuspendRetryFlow(work!!, retryPolicy, name)
    }
}

fun suspendRetryFlow(
    name: String = "suspend-retry",
    block: SuspendRetryFlowBuilder.() -> Unit,
): SuspendRetryFlow = SuspendRetryFlowBuilder(name).apply(block).build()

/**
 * 복합 코루틴 워크플로 DSL입니다.
 *
 * ```kotlin
 * val pipeline = suspendWorkflow("async-pipeline") {
 *     sequential {
 *         execute(validateWork)
 *         parallel {
 *             execute(fetchWork1)
 *             execute(fetchWork2)
 *         }
 *         conditional {
 *             condition { ctx -> ctx.get<Boolean>("approved") == true }
 *             then(processWork)
 *         }
 *         repeat {
 *             execute(pollWork)
 *             until { report -> report is WorkReport.Success }
 *             maxIterations = 5
 *         }
 *         execute(aggregateWork)
 *     }
 * }
 * ```
 */
@WorkflowDsl
class SuspendWorkflowBuilder(private val name: String = "suspend-workflow") {
    private var rootWork: SuspendWork? = null

    private fun setRoot(work: SuspendWork) {
        require(rootWork == null) { "루트 워크플로가 이미 선언되었습니다. 루트는 하나만 지정할 수 있습니다." }
        rootWork = work
    }

    fun sequential(name: String = "sequential", block: SuspendSequentialFlowBuilder.() -> Unit) {
        setRoot(suspendSequentialFlow(name, block))
    }

    fun parallel(name: String = "parallel", block: SuspendParallelFlowBuilder.() -> Unit) {
        setRoot(suspendParallelFlow(name, block))
    }

    fun conditional(name: String = "conditional", block: SuspendConditionalFlowBuilder.() -> Unit) {
        setRoot(suspendConditionalFlow(name, block))
    }

    fun repeat(name: String = "repeat", block: SuspendRepeatFlowBuilder.() -> Unit) {
        setRoot(suspendRepeatFlow(name, block))
    }

    internal fun build(): SuspendWorkFlow {
        requireNotNull(rootWork) { "workflow에는 최소 하나의 플로우가 필요합니다" }
        return rootWork as SuspendWorkFlow
    }
}

fun suspendWorkflow(
    name: String = "suspend-workflow",
    block: SuspendWorkflowBuilder.() -> Unit,
): SuspendWorkFlow = SuspendWorkflowBuilder(name).apply(block).build()
```

---

## 7. build.gradle.kts

```kotlin
dependencies {
    api(project(":bluetape4k-core"))
    testImplementation(project(":bluetape4k-junit5"))

    // Virtual Threads
    implementation(project(":bluetape4k-virtualthread-api"))

    // Coroutines
    implementation(project(":bluetape4k-coroutines"))
    implementation(Libs.kotlinx_coroutines_core)
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

---

## 8. 에러 처리 전략 상세

### 8.1 SequentialFlow 에러 처리

| ErrorStrategy | 동작 |
|---------------|------|
| `STOP` | 첫 번째 `Failure` 발생 시 즉시 중단, 해당 `Failure`를 반환 |
| `CONTINUE` | `Failure`를 누적하고 다음 Work 실행. 실패가 하나라도 있으면 `PartialSuccess` 반환, 모두 성공이면 `Success` 반환 |

> **`WorkReport.PartialSuccess`**: `CONTINUE` 전략 사용 시, 중간에 실패한 Work가 있으면
> 마지막 Work 결과와 무관하게 `PartialSuccess`를 반환합니다. `failedReports` 프로퍼티로
> 실패한 각 Work의 `WorkReport`를 조회할 수 있습니다.

### 8.2 ParallelFlow 에러 처리

- `executor.invokeAll(callables, timeout, TimeUnit)`으로 실제 시간 제한을 보장
- timeout 초과 시 미완료 태스크는 `CancellationException`으로 종료되고 `Failure`로 처리
- 하나라도 `Failure`이면 첫 번째 `Failure` 반환
- 향후 확장: `ParallelErrorStrategy` (FAIL_FAST -- 첫 실패 시 나머지 취소)

### 8.3 RetryPolicy

| 필드 | 설명 | 기본값 |
|------|------|--------|
| `maxAttempts` | 최대 총 시도 횟수 (최초 1회 + 재시도 횟수. 1 = 재시도 없음) | 1 |
| `maxRetries` | 재시도 횟수 (= `maxAttempts - 1`, 읽기 전용 편의 프로퍼티) | 0 |
| `delay` | 재시도 간 대기 시간 (`Duration`) | `Duration.ZERO` |
| `backoffMultiplier` | 지수 백오프 배율 | 1.0 |
| `maxDelay` | 백오프 적용 시 최대 지연 상한 (`Duration`) | `1.minutes` |

---

## 9. 테스트 전략

### 9.1 테스트 도구

- JUnit 5 + Kluent + MockK
- `kotlinx-coroutines-test` (`runTest`)

### 9.2 테스트 시나리오

| 플로우 타입 | 테스트 시나리오 |
|------------|----------------|
| SequentialWorkFlow | 전체 성공 / 중간 실패 STOP / 중간 실패 CONTINUE → PartialSuccess 반환 + failedReports 검증 / 전체 실패 CONTINUE → PartialSuccess |
| ParallelWorkFlow | 전체 성공 / 일부 실패 / Virtual Threads 실행 확인 / invokeAll timeout 시 미완료 태스크 Failure 처리 / timeout 내 정상 완료 |
| ConditionalWorkFlow | true 분기 / false 분기 / otherwise 생략 시 no-op / 중첩 조건 |
| RepeatWorkFlow | 조건 충족 시 반복 / maxIterations 제한 / until 조건 |
| RetryWorkFlow | 성공까지 재시도 / maxAttempts 소진 (총 시도 횟수 = maxAttempts 확인) / 지수 백오프 / maxDelay 상한 / maxRetries 편의 프로퍼티 |
| WorkContext | 스레드 안전 동시 쓰기/읽기 / compute() 원자적 갱신 / merge / snapshot |
| NamedWork / NamedSuspendWork | 이름 전달 / SAM 변환 / Work 팩토리 함수 |
| Work/SuspendWork 어댑터 | asSuspend() / asBlocking() 변환 동작 |
| DSL 빌더 | 중첩 sequential -> parallel -> conditional -> repeat -> retry 구성 / 루트 중복 선언 시 IllegalArgumentException |
| SuspendSequentialFlow | suspend 순차 전체 성공/실패 / CONTINUE → PartialSuccess 반환 / ensureActive 취소 전파 |
| SuspendParallelFlow | coroutineScope + async 병렬 실행 / 실패 전파 |
| SuspendConditionalFlow | suspend predicate true/false / otherwise 생략 |
| SuspendRepeatFlow | delay 반복 / 코루틴 취소 전파 / ensureActive |
| SuspendRetryFlow | suspend 재시도 / delay 백오프 / maxDelay 상한 |
| Flow\<WorkReport\> | workReportFlow 수집 / 필터링 |
| WorkReport.PartialSuccess | status == PARTIAL / failedReports 비어있지 않음 / error가 첫 번째 실패의 error와 동일 |

### 9.3 테스트 파일 구조

```
src/test/kotlin/io/bluetape4k/workflow/
├── api/
│   ├── WorkContextTest.kt
│   ├── NamedWorkTest.kt
│   └── WorkAdapterTest.kt
├── core/
│   ├── SequentialWorkFlowTest.kt
│   ├── ParallelWorkFlowTest.kt
│   ├── ConditionalWorkFlowTest.kt
│   ├── RepeatWorkFlowTest.kt
│   ├── RetryWorkFlowTest.kt
│   └── WorkflowDslTest.kt
└── coroutines/
    ├── SuspendSequentialFlowTest.kt
    ├── SuspendParallelFlowTest.kt
    ├── SuspendConditionalFlowTest.kt
    ├── SuspendRepeatFlowTest.kt
    ├── SuspendRetryFlowTest.kt
    ├── WorkReportFlowTest.kt
    └── SuspendWorkflowDslTest.kt
```

---

## 10. 설계 결정

### 10.1 병렬 플로우 공유 WorkContext vs branch-local context

**v1 결정**: 공유 WorkContext 유지 (브랜치 격리 없음)

**근거**:
- 구현 복잡도 최소화 (merge 정책 설계가 복잡하고 충돌 해결 전략이 다양함)
- 각 Work가 서로 다른 키를 사용하는 책임은 호출자에게 위임
- `compute()` API로 atomic read-modify-write 지원하므로 동일 키 갱신도 안전

**향후 확장 (v2 검토)**:
- `IsolatedParallelWorkFlow`: 각 Work에 context 사본(snapshot) 전달 후 merge 함수 제공
- merge 정책: last-write-wins, custom merger `(key: String, left: Any, right: Any) -> Any`
- `SuspendIsolatedParallelFlow`에도 동일 패턴 적용

---

## 11. 향후 확장 고려사항

| 항목 | 설명 | 우선순위 |
|------|------|----------|
| `TimeoutWorkFlow` | 시간 제한 워크플로 (Virtual Threads: `Future.get(timeout)`, Coroutines: `withTimeout`) | Medium |
| `ParallelErrorStrategy.FAIL_FAST` | 첫 실패 시 나머지 작업 취소 | Medium |
| `ParallelPolicy.AtLeast(n)` — Quorum 패턴 | 현재 `ParallelPolicy`는 `ALL`/`ANY` enum이지만, 향후 sealed class로 교체하여 quorum 패턴 지원. 예: `AtLeast(2)` = 3개 중 2개 이상 성공하면 전체 성공 | Medium |
| `WorkFlowListener` | 실행 전/후 이벤트 콜백 (로깅, 메트릭) | Low |
| `WorkReport.Skipped` | 조건 미충족으로 건너뛴 작업 표현 | Low |
| Resilience4j 통합 | CircuitBreaker/RateLimiter와 Work 데코레이터 | Low |
| Spring Integration | `@WorkFlow` 어노테이션 + Bean 자동 감지 | Low |
| 취소/타임아웃 강화 | `SuspendRepeatFlow`/`RepeatWorkFlow`에 선택적 타임아웃 파라미터 | Low |

### 11.1 ParallelPolicy.AtLeast(n) — Quorum 패턴 (v2 검토)

현재 `ParallelPolicy`는 `ALL`/`ANY` enum이지만, 향후 sealed class로 교체하여 quorum 패턴 지원:

```kotlin
sealed class ParallelPolicy {
    data object ALL : ParallelPolicy()           // 전원 성공
    data object ANY : ParallelPolicy()           // 첫 성공
    data class AtLeast(val minSuccess: Int) : ParallelPolicy()  // N개 이상 성공
}
```

DSL:
```kotlin
parallelAtLeast(2) {   // 3개 중 2개 이상 성공하면 PartialSuccess 아닌 Success
    execute(server1)
    execute(server2)
    execute(server3)
}
```

구현: 커스텀 `StructuredTaskScope` 서브클래스 — 성공 카운터가 N에 도달하면 `shutdown()`.
참고: `ANY` = `AtLeast(1)` 로 통합 가능.

---

## 12. 기존 모듈과의 관계

| 모듈 | 관계 |
|------|------|
| `bluetape4k-core` | `KLogging`, 유틸리티 의존 |
| `bluetape4k-coroutines` | `SuspendWork`, `SuspendWorkFlow` 구현에 활용 |
| `bluetape4k-virtualthread-api` | `ParallelWorkFlow`에서 Virtual Threads 활용 |
| `bluetape4k-states` | 상호 보완 -- states는 FSM(상태 전이), workflow는 작업 흐름(파이프라인) |
| `bluetape4k-rule-engine` | 상호 보완 -- rule-engine은 조건-행동, workflow는 흐름 구성 |
| `bluetape4k-resilience4j` | 향후 RetryWorkFlow -> Resilience4j Retry 어댑터 가능 |

---

## 13. 패키지 구조 요약

```
io.bluetape4k.workflow
├── WorkflowDefaults.kt                    # 기본 상수
├── api/
│   ├── WorkStatus.kt                      # enum (COMPLETED / FAILED / PARTIAL)
│   ├── WorkReport.kt                      # sealed interface (Success / Failure / PartialSuccess)
│   ├── WorkContext.kt                      # ConcurrentHashMap 기반 + compute()
│   ├── Work.kt                            # fun interface (동기)
│   ├── NamedWork.kt                       # 이름 래퍼 + 팩토리 함수
│   ├── SuspendWork.kt                     # fun interface (코루틴)
│   ├── NamedSuspendWork.kt                # 이름 래퍼 + 팩토리 함수
│   ├── WorkAdapters.kt                    # asSuspend() / asBlocking() 변환
│   ├── WorkFlow.kt                        # 마커 인터페이스 (동기)
│   ├── SuspendWorkFlow.kt                 # 마커 인터페이스 (코루틴)
│   ├── ErrorStrategy.kt                   # enum (STOP / CONTINUE)
│   ├── RetryPolicy.kt                     # data class (Duration 기반)
│   └── WorkflowDsl.kt                     # @WorkflowDsl 마커 어노테이션
├── core/
│   ├── SequentialWorkFlow.kt
│   ├── ParallelWorkFlow.kt                # invokeAll(timeout) + Virtual Threads
│   ├── ConditionalWorkFlow.kt             # otherwise 선택적
│   ├── RepeatWorkFlow.kt
│   ├── RetryWorkFlow.kt
│   └── WorkflowDsl.kt                     # DSL 빌더 + 최상위 함수 (retry 빌더 포함)
└── coroutines/
    ├── SuspendSequentialFlow.kt            # ensureActive 취소 확인
    ├── SuspendParallelFlow.kt             # coroutineScope + async
    ├── SuspendConditionalFlow.kt          # otherwise 선택적
    ├── SuspendRepeatFlow.kt               # Duration 기반 delay + ensureActive
    ├── SuspendRetryFlow.kt
    ├── WorkReportFlowSupport.kt           # Flow<WorkReport> 확장
    └── SuspendWorkflowDsl.kt             # 코루틴 DSL 빌더 + 최상위 함수 (conditional/repeat/retry 포함)
```
