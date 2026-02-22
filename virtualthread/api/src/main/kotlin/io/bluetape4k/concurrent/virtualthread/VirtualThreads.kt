package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

/**
 * 서비스 로더로 등록된 JDK별 구현체 중 현재 런타임에 맞는 구현체를 선택합니다.
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
     * 현재 런타임에 맞는 구현체를 반환합니다.
     * 구현체가 없으면 플랫폼 스레드 fallback을 사용합니다.
     */
    fun runtime(): VirtualThreadRuntime = providers.firstOrNull() ?: PlatformThreadRuntime

    fun runtimeName(): String = runtime().runtimeName

    fun threadFactory(prefix: String = "vt-"): ThreadFactory = runtime().threadFactory(prefix)

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
