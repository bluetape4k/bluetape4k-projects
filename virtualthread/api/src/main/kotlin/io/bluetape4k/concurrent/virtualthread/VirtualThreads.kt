package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * ServiceLoader로 발견한 런타임 구현 중 현재 JVM에서 사용 가능한 가상 스레드 구현을 선택합니다.
 *
 * ## 동작/계약
 * - 등록 구현체를 [VirtualThreadRuntime.priority] 내림차순으로 정렬해 첫 구현을 사용합니다.
 * - 사용 가능한 구현체가 없으면 내부 `platform-fallback` 구현으로 자동 대체합니다.
 * - provider 탐색 중 오류가 발생해도 전체 선택 로직은 계속 진행합니다.
 *
 * ```kotlin
 * val name = VirtualThreads.runtimeName()
 * val answer = VirtualThreads.executorService().use { it.submit<Int> { 42 }.get() }
 * // name.isNotBlank() == true
 * // answer == 42
 * ```
 */
object VirtualThreads: KLogging() {

    private val providers: List<VirtualThreadRuntime> by lazy {
        val loader = ServiceLoader.load(VirtualThreadRuntime::class.java)
        val iterator = loader.iterator()
        val discovered = mutableListOf<VirtualThreadRuntime>()

        while (true) {
            val provider = runCatching {
                if (!iterator.hasNext()) return@runCatching null
                iterator.next()
            }.getOrNull() ?: break

            runCatching {
                if (provider.isSupported()) {
                    discovered += provider
                    log.debug { "Discovered VirtualThreadRuntime provider: ${provider.runtimeName} (priority: ${provider.priority})" }
                }
            }.onFailure { error ->
                log.warn(error) { "Failed to check VirtualThreadRuntime provider: ${provider.javaClass.name}" }
            }
        }

        discovered.sortedByDescending { it.priority }
    }

    /**
     * 현재 런타임에 맞는 [VirtualThreadRuntime] 구현을 반환합니다.
     *
     * ## 동작/계약
     * - 탐색된 provider가 없으면 `platform-fallback` 구현을 반환합니다.
     * - 반환 구현은 객체 내부에서 재사용되며 호출마다 provider 재탐색을 수행하지 않습니다.
     *
     * ```kotlin
     * val runtime = VirtualThreads.runtime()
     * // runtime.runtimeName.isNotBlank() == true
     * ```
     */
    fun runtime(): VirtualThreadRuntime = providers.firstOrNull() ?: PlatformThreadRuntime

    /**
     * 선택된 런타임 구현의 이름을 반환합니다.
     *
     * ## 동작/계약
     * - [runtime]의 [VirtualThreadRuntime.runtimeName]을 그대로 반환합니다.
     * - provider가 없을 때는 `"platform-fallback"`이 반환됩니다.
     *
     * ```kotlin
     * val name = VirtualThreads.runtimeName()
     * // name.isNotBlank() == true
     * ```
     */
    fun runtimeName(): String = runtime().runtimeName

    /**
     * 선택된 런타임 구현의 [ThreadFactory]를 반환합니다.
     *
     * ## 동작/계약
     * - [prefix]는 구현체에 전달되어 스레드 이름 규칙에 반영됩니다.
     * - fallback 경로에서는 플랫폼 스레드 팩토리를 반환합니다.
     *
     * ```kotlin
     * val factory = VirtualThreads.threadFactory("vt-")
     * val thread = factory.newThread {}
     * // thread.name.startsWith("vt-") == true
     * ```
     *
     * @param prefix 생성할 스레드 이름 접두사
     */
    fun threadFactory(prefix: String = "vt-"): ThreadFactory = runtime().threadFactory(prefix)

    /**
     * 선택된 런타임 구현의 [ExecutorService]를 반환합니다.
     *
     * ## 동작/계약
     * - 호출자 책임으로 `close`/`shutdown`을 수행해야 합니다.
     * - fallback 경로에서는 `newCachedThreadPool` 기반 executor를 반환합니다.
     *
     * ```kotlin
     * val result = VirtualThreads.executorService().use { it.submit<Int> { 21 * 2 }.get() }
     * // result == 42
     * ```
     */
    fun executorService(): ExecutorService = runtime().executorService()

    private object PlatformThreadRuntime: VirtualThreadRuntime {
        override val runtimeName: String = "platform-fallback"
        override val priority: Int = Int.MIN_VALUE

        override fun isSupported(): Boolean = true

        override fun threadFactory(prefix: String): ThreadFactory {
            return Thread.ofPlatform().name(prefix, 0).factory()
        }

        override fun executorService(): ExecutorService = Executors.newCachedThreadPool(threadFactory("pt-"))
    }
}
