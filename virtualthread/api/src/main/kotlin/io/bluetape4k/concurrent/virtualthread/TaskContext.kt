package io.bluetape4k.concurrent.virtualthread

import java.lang.ScopedValue

/**
 * ScopedValue 기반 컨텍스트 전파 유틸리티입니다.
 *
 * Virtual Thread 간 안전한 컨텍스트 전파를 위해 [ScopedValue]를 래핑합니다.
 * `ThreadLocal`과 달리 [ScopedValue]는 immutable하며 스코프 바깥으로 누출되지 않습니다.
 *
 * ## 자동 전파
 * [ScopedValue]는 [StructuredTaskScope][java.util.concurrent.StructuredTaskScope]의 `fork`로
 * 생성된 Virtual Thread에 자동으로 전파됩니다.
 * 별도 설정 없이 부모 스코프의 바인딩이 모든 forked subtask에서 조회 가능합니다.
 *
 * ## JDK 버전별 API
 * - JDK 21: Preview API — `--enable-preview` 컴파일/실행 플래그 필요
 * - JDK 25: Stable API — 추가 플래그 불필요
 *
 * ## 코루틴과의 호환성
 * [ScopedValue]는 코루틴 중단점(suspension point)을 가로지르지 않습니다.
 * 코루틴 컨텍스트 전파가 필요하면 `ThreadLocal` + `asContextElement()`를 사용하세요.
 *
 * ```kotlin
 * val REQUEST_ID = TaskContext.newKey<String>()
 *
 * // 단일 바인딩 — top-level 함수 스타일 (권장)
 * withTaskContext(REQUEST_ID, "req-001") {
 *     println(TaskContext.get(REQUEST_ID))  // "req-001"
 *     StructuredTaskScopes.failFast { scope ->
 *         scope.fork { TaskContext.get(REQUEST_ID) }  // 자동 전파됨
 *         scope.join().throwIfFailed()
 *     }
 * }
 *
 * // 단일 바인딩 — object 멤버 함수 스타일
 * TaskContext.run(REQUEST_ID, "req-001") {
 *     println(TaskContext.get(REQUEST_ID))  // "req-001"
 * }
 *
 * // 다중 바인딩
 * TaskContext.bind(REQUEST_ID, "req-001")
 *     .and(TENANT_ID, "tenant-42")
 *     .run {
 *         println(TaskContext.get(REQUEST_ID))  // "req-001"
 *         println(TaskContext.get(TENANT_ID))   // "tenant-42"
 *     }
 * ```
 */
object TaskContext {

    /**
     * 새로운 타입 안전 컨텍스트 키를 생성합니다.
     *
     * ```kotlin
     * val REQUEST_ID: ScopedValue<String> = TaskContext.newKey()
     * val USER_ID: ScopedValue<Long> = TaskContext.newKey()
     * ```
     *
     * @return 새로운 [ScopedValue] 인스턴스
     */
    fun <T> newKey(): ScopedValue<T> = ScopedValue.newInstance()

    /**
     * 현재 스코프에서 [key]에 바인딩된 값을 반환합니다.
     * 바인딩되지 않은 경우 `null`을 반환합니다.
     *
     * ```kotlin
     * val value: String? = TaskContext.get(REQUEST_ID)
     * ```
     */
    fun <T> get(key: ScopedValue<T>): T? = if (key.isBound) key.get() else null

    /**
     * 현재 스코프에서 [key]에 바인딩된 값을 반환합니다.
     * 바인딩되지 않은 경우 [defaultValue]를 반환합니다.
     */
    fun <T> getOrDefault(key: ScopedValue<T>, defaultValue: T): T =
        if (key.isBound) key.get() else defaultValue

    /**
     * 현재 스코프에서 [key]가 바인딩되어 있는지 확인합니다.
     */
    fun isBound(key: ScopedValue<*>): Boolean = key.isBound

    /**
     * [key]=[value] 바인딩 블록을 실행하고 결과를 반환합니다.
     * 블록 내 모든 코드 (forked subtask 포함) 에서 [key]로 [value]를 조회할 수 있습니다.
     *
     * top-level [withTaskContext] 함수와 동일합니다.
     *
     * ```kotlin
     * val result = TaskContext.run(REQUEST_ID, "req-001") {
     *     doWork()  // REQUEST_ID 바인딩 범위 내
     * }
     * ```
     */
    fun <T, R> run(key: ScopedValue<T>, value: T, block: () -> R): R =
        ScopedValue.where(key, value).call { block() }

    /**
     * [key]=[value] 첫 번째 바인딩으로 [TaskContextBindings] 빌더를 시작합니다.
     * [TaskContextBindings.and]로 바인딩을 추가하고 [TaskContextBindings.run] 또는
     * [TaskContextBindings.call]로 실행합니다.
     *
     * ```kotlin
     * TaskContext.bind(REQUEST_ID, "req-001")
     *     .and(TENANT_ID, "tenant-42")
     *     .run { doWork() }
     * ```
     */
    fun <T> bind(key: ScopedValue<T>, value: T): TaskContextBindings =
        TaskContextBindings(ScopedValue.where(key, value))
}

/**
 * 여러 [ScopedValue] 바인딩을 체이닝하는 빌더입니다.
 *
 * [TaskContext.bind]로 시작하고 [and]로 추가한 뒤 [run] 또는 [call]로 실행합니다.
 *
 * ```kotlin
 * TaskContext.bind(A_KEY, "a")
 *     .and(B_KEY, 42)
 *     .run {
 *         println(TaskContext.get(A_KEY))  // "a"
 *         println(TaskContext.get(B_KEY))  // 42
 *     }
 * ```
 */
/**
 * [key]=[value] 단일 바인딩 스코프를 실행하고 결과를 반환하는 top-level 함수입니다.
 *
 * [TaskContext.run]과 동일하지만 Kotlin `with*` 관용구(`withContext`, `withLock` 등)와 일관된 이름입니다.
 *
 * ```kotlin
 * val result = withTaskContext(REQUEST_ID, "req-001") {
 *     doWork()  // REQUEST_ID 바인딩 범위 내
 * }
 *
 * // StructuredTaskScope.fork 에서 자동 전파
 * withTaskContext(REQUEST_ID, "req-001") {
 *     StructuredTaskScopes.failFast { scope ->
 *         scope.fork { TaskContext.get(REQUEST_ID) }  // "req-001" 자동 상속
 *         scope.join().throwIfFailed()
 *     }
 * }
 * ```
 *
 * @see TaskContext.run
 */
fun <T, R> withTaskContext(key: ScopedValue<T>, value: T, block: () -> R): R =
    TaskContext.run(key, value, block)

class TaskContextBindings internal constructor(
    private val carrier: ScopedValue.Carrier,
) {
    /**
     * 바인딩을 추가합니다.
     */
    fun <T> and(key: ScopedValue<T>, value: T): TaskContextBindings =
        TaskContextBindings(carrier.where(key, value))

    /**
     * 모든 바인딩을 적용하고 [block]을 실행합니다.
     */
    fun run(block: () -> Unit): Unit = carrier.run(block)

    /**
     * 모든 바인딩을 적용하고 [block]을 실행한 뒤 결과를 반환합니다.
     */
    fun <R> call(block: () -> R): R = carrier.call { block() }
}
