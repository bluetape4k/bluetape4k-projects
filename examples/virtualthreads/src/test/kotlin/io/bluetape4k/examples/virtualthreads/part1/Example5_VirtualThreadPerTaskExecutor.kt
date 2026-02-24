package io.bluetape4k.workshop.virtualthread.part1

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

class Example5_VirtualThreadPerTaskExecutor {

    companion object: KLoggingChannel()

    @Test
    fun `Virtual threads per Task Executor`() {
        log.info { "Virtual threads per Task Executor" }

        Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            executor.javaClass.name shouldBeEqualTo "java.util.concurrent.ThreadPerTaskExecutor"

            val future = executor.submit {
                Thread.sleep(100)
                println("Run in ${Thread.currentThread()}")
                Thread.currentThread().name.shouldBeEmpty()
            }
            future.get()
        }
    }
}
