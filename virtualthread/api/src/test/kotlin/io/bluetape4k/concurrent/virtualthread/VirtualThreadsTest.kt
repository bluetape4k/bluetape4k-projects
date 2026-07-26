package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

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
}
