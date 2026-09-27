package io.bluetape4k.examples.virtualthreads.part1

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.concurrent.await
import io.bluetape4k.examples.virtualthreads.AbstractVirtualThreadTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import kotlin.time.Duration.Companion.seconds

class Example3_CreateStartedAndUnstartedVirtualThread: AbstractVirtualThreadTest() {

    companion object: KLoggingChannel()

    @Test
    fun `자동 시작하는 Virtual Thread 생성하기`() {
        val builder = Thread.ofVirtual()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        val thread = builder.start {
            started.countDown()
            release.await(1.seconds)
            println("Virtual thread running")
        }

        started.await(1.seconds).shouldBeTrue()
        thread.isVirtual.shouldBeTrue()
        thread.isAlive.shouldBeTrue()
        release.countDown()
        thread.join()
    }

    @Test
    fun `수동으로 시작하는 Virtual Thread 생성하기`() {
        val builder = Thread.ofVirtual()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)

        val thread = builder.unstarted {
            started.countDown()
            release.await(1.seconds)
            println("Virtual thread running")
        }
        thread.state shouldBeEqualTo Thread.State.NEW
        thread.start()

        started.await(1.seconds).shouldBeTrue()
        thread.isVirtual.shouldBeTrue()
        thread.isAlive.shouldBeTrue()
        release.countDown()
        thread.join()
    }
}
