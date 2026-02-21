package io.bluetape4k.concurrent.virtualthread

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeBlank
import org.junit.jupiter.api.Test

class VirtualThreadsTest {

    @Test
    fun `runtime and executor should be available`() {
        VirtualThreads.runtimeName().shouldNotBeBlank()

        VirtualThreads.executorService().use { executor ->
            val future = executor.submit<Int> { 42 }
            future.get() shouldBeEqualTo 42
        }
    }
}

