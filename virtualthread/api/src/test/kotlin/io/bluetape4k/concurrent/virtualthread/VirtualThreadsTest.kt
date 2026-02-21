package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeBlank
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
}
