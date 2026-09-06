package io.bluetape4k.concurrent.virtualthread.api

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.util.ServiceConfigurationError
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class VirtualThreadsTest {

    companion object: KLogging()

    @Test
    fun `runtime and executor should be available`() {
        VirtualThreads.runtimeName().shouldNotBeBlank()
        log.debug { "Virtual thread runtime name: ${VirtualThreads.runtimeName()}" }  // jdk21 or jdk25

        VirtualThreads.executorService().use { executor ->
            val future = executor.submit<Int> { 42 }
            future.get() shouldBeEqualTo 42
        }
    }

    @Test
    fun `threadFactory 기본 접두사로 스레드를 생성해야 한다`() {
        val factory = VirtualThreads.threadFactory()
        val thread = factory.newThread {}
        thread.shouldNotBeNull()
    }

    @Test
    fun `threadFactory 커스텀 접두사로 스레드를 생성해야 한다`() {
        val factory = VirtualThreads.threadFactory("custom-vt-")
        val thread = factory.newThread {}
        thread.shouldNotBeNull()
        thread.name.contains("custom-vt-").shouldBeTrue()
    }

    @Test
    fun `runtime 이 non-null 이고 runtimeName 이 비어있지 않아야 한다`() {
        val runtime = VirtualThreads.runtime()
        runtime.shouldNotBeNull()
        runtime.runtimeName.shouldNotBeBlank()
        runtime.isSupported().shouldBeTrue()
    }

    @Test
    fun `runtime threadFactory 기본 접두사 없이 호출되어야 한다`() {
        // VirtualThreadRuntime.threadFactory(prefix) interface default param 경로 커버
        val runtime = VirtualThreads.runtime()
        val factory = runtime.threadFactory()
        factory.shouldNotBeNull()
    }

    @Test
    fun `runtime executorService 를 통해 작업을 실행할 수 있어야 한다`() {
        val runtime = VirtualThreads.runtime()
        runtime.executorService().use { executor ->
            val result = executor.submit<String> { "from-runtime" }.get()
            result shouldBeEqualTo "from-runtime"
        }
    }

    @Test
    fun `provider discovery skips broken next entries`() {
        val providers = VirtualThreads.discoverVirtualThreadRuntimes(
            FailingNextThenRuntimeIterator(TestVirtualThreadRuntime("valid", priority = 10)),
        )

        providers.map { it.runtimeName } shouldBeEqualTo listOf("valid")
    }

    @Test
    fun `provider discovery skips unsupported and failing runtime checks`() {
        val providers = VirtualThreads.discoverVirtualThreadRuntimes(
            listOf(
                TestVirtualThreadRuntime("unsupported", priority = 100, supported = false),
                TestVirtualThreadRuntime("broken-check", priority = 90, supportFailure = true),
                TestVirtualThreadRuntime("lower", priority = 10),
                TestVirtualThreadRuntime("higher", priority = 20),
            ).iterator(),
        )

        providers.map { it.runtimeName } shouldBeEqualTo listOf("higher", "lower")
    }

    @Test
    fun `provider discovery stops cleanly when hasNext fails`() {
        val providers = VirtualThreads.discoverVirtualThreadRuntimes(FailingHasNextRuntimeIterator())

        providers shouldBeEqualTo emptyList<VirtualThreadRuntime>()
    }

    private class FailingNextThenRuntimeIterator(
        private val runtime: VirtualThreadRuntime,
    ): Iterator<VirtualThreadRuntime> {
        private var index = 0

        override fun hasNext(): Boolean = index < 2

        override fun next(): VirtualThreadRuntime =
            when (index++) {
                0 -> throw ServiceConfigurationError("broken runtime entry")
                1 -> runtime
                else -> throw NoSuchElementException()
            }
    }

    private class FailingHasNextRuntimeIterator: Iterator<VirtualThreadRuntime> {
        override fun hasNext(): Boolean = throw ServiceConfigurationError("broken runtime index")
        override fun next(): VirtualThreadRuntime = throw NoSuchElementException()
    }

    private class TestVirtualThreadRuntime(
        override val runtimeName: String,
        override val priority: Int,
        private val supported: Boolean = true,
        private val supportFailure: Boolean = false,
    ): VirtualThreadRuntime {
        override fun isSupported(): Boolean {
            check(!supportFailure) { "support check failed" }
            return supported
        }

        override fun threadFactory(prefix: String): ThreadFactory =
            Thread.ofPlatform().name(prefix, 0).factory()

        override fun executorService(): ExecutorService =
            Executors.newSingleThreadExecutor(threadFactory("test-"))
    }
}
