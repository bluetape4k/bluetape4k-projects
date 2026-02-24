package io.bluetape4k.workshop.virtualthread.part1

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class Example3_CreateStartedAndUnstartedVirtualThread {

    companion object: KLoggingChannel()

    @Test
    fun `자동 시작하는 Virtual Thread 생성하기`() {
        val builder = Thread.ofVirtual()

        val thread = builder.start {
            Thread.sleep(1000)
            println("Virtual thread running")
        }
        thread.state shouldBeEqualTo Thread.State.RUNNABLE
        thread.join()
    }

    @Test
    fun `수동으로 시작하는 Virtual Thread 생성하기`() {
        val builder = Thread.ofVirtual()

        val thread = builder.unstarted {
            println("Virtual thread running")
        }
        thread.state shouldBeEqualTo Thread.State.NEW
        thread.start()

        thread.state shouldBeEqualTo Thread.State.RUNNABLE
        thread.join()
    }
}
