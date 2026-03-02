package io.bluetape4k.concurrent.virtualthread

import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadFactory

/**
 * 런타임별 가상 스레드 기능을 공통 API로 노출하는 추상화 인터페이스입니다.
 *
 * ## 동작/계약
 * - 구현체는 현재 JDK에서 지원되는 가상 스레드 생성 전략을 캡슐화합니다.
 * - [priority]가 높은 구현체가 우선 선택되며, 선택 로직은 [VirtualThreads.runtime]이 담당합니다.
 * - `threadFactory`/`executorService`는 호출 시 새 인스턴스를 반환할 수 있습니다.
 *
 * ```kotlin
 * val runtime = VirtualThreads.runtime()
 * val result = runtime.executorService().use { it.submit<Int> { 42 }.get() }
 * // result == 42
 * ```
 */
interface VirtualThreadRuntime {

    /**
     * 구현체 식별 이름입니다.
     *
     * ## 동작/계약
     * - 로깅/진단용 식별자이며 사람이 읽기 쉬운 문자열을 반환합니다.
     * - 런타임 상태가 같으면 동일한 값을 반환해야 합니다.
     *
     * ```kotlin
     * val name = VirtualThreads.runtime().runtimeName
     * // name.isNotBlank() == true
     * ```
     */
    val runtimeName: String

    /**
     * 구현체 선택 우선순위입니다.
     *
     * ## 동작/계약
     * - 값이 클수록 우선 선택됩니다.
     * - 동순위 구현체가 여러 개이면 `ServiceLoader` 탐색 순서의 영향을 받을 수 있습니다.
     *
     * ```kotlin
     * val p = VirtualThreads.runtime().priority
     * // p is Int
     * ```
     */
    val priority: Int

    /**
     * 현재 JVM 환경에서 이 구현체 사용 가능 여부를 반환합니다.
     *
     * ## 동작/계약
     * - 지원되지 않는 JDK에서는 `false`를 반환해야 합니다.
     * - 예외를 던지기보다 불가 여부를 boolean으로 표현하는 것이 권장됩니다.
     *
     * ```kotlin
     * val supported = VirtualThreads.runtime().isSupported()
     * // supported == true
     * ```
     */
    fun isSupported(): Boolean

    /**
     * 가상 스레드 생성용 [ThreadFactory]를 반환합니다.
     *
     * ## 동작/계약
     * - [prefix]는 생성되는 스레드 이름 접두사로 사용됩니다.
     * - 반환된 팩토리는 호출 시점 런타임 구현에 종속됩니다.
     *
     * ```kotlin
     * val factory = VirtualThreads.runtime().threadFactory("vt-demo-")
     * val thread = factory.newThread {}
     * // thread.name.startsWith("vt-demo-") == true
     * ```
     *
     * @param prefix 생성 스레드 이름 접두사
     */
    fun threadFactory(prefix: String = "vt-"): ThreadFactory

    /**
     * task-per-thread 실행 모델의 [ExecutorService]를 반환합니다.
     *
     * ## 동작/계약
     * - 제출된 작업은 구현체 정책에 따라 가상 스레드(또는 fallback 스레드)에서 실행됩니다.
     * - 반환된 ExecutorService의 종료 책임은 호출자에게 있습니다.
     *
     * ```kotlin
     * val result = VirtualThreads.runtime().executorService().use { executor ->
     *     executor.submit<Int> { 7 * 6 }.get()
     * }
     * // result == 42
     * ```
     */
    fun executorService(): ExecutorService
}
