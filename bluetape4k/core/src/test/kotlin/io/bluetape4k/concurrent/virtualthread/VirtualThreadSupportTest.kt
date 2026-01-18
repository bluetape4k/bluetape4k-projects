package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class VirtualThreadSupportTest: AbstractVirtualThreadTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `platformThreadBuilder를 이용하여 PlatformThread 생성하기`() {
        val builder: Thread.Builder.OfPlatform = platformThreadBuilder {
            daemon(false)
            priority(10)
            stackSize(1024)
            name("platform-thread")
            inheritInheritableThreadLocals(false)
            uncaughtExceptionHandler { thread, ex ->
                log.warn(ex) { "Thread[$thread] failed with exception." }
            }
        }

        log.debug { "Builder class=${builder.javaClass.name}" }
        builder.javaClass.name shouldBeEqualTo $$"java.lang.ThreadBuilders$PlatformThreadBuilder"

        val thread = builder.unstarted {
            log.debug { "Unstarted Platform Thread" }
        }

        thread.javaClass.name shouldBeEqualTo "java.lang.Thread"
        thread.name shouldBeEqualTo "platform-thread"
        thread.isDaemon shouldBeEqualTo false
        thread.priority shouldBeEqualTo 10

        thread.start()
        thread.join()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `virtualThreadBuilder 를 이용하여 VirtualThread 생성하기`() {
        val builder: Thread.Builder.OfVirtual = virtualThreadBuilder {
            name("virtual-thread")
            inheritInheritableThreadLocals(false)
            uncaughtExceptionHandler { thread, ex ->
                log.warn(ex) { "Thread[$thread] failed with exception." }
            }
        }

        log.debug { "Builder class=${builder.javaClass.name}" }
        builder.javaClass.name shouldBeEqualTo $$"java.lang.ThreadBuilders$VirtualThreadBuilder"

        val thread = builder.unstarted {
            log.debug { "Unstarted Virtual Thread" }
        }

        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.name shouldBeEqualTo "virtual-thread"
        thread.start()
        thread.join()
    }

    @Test
    fun `virtualThreadFactory 를 이용하여 virtual thread 생성하기`() {
        val factory = virtualThreadFactory {
            name("virtual-thread-", 0)
            inheritInheritableThreadLocals(false)
            uncaughtExceptionHandler { thread, ex ->
                log.warn(ex) { "Thread[$thread] failed with exception." }
            }
        }

        log.debug { "Factory class=${factory.javaClass.name}" }
        factory.javaClass.name shouldBeEqualTo $$"java.lang.ThreadBuilders$VirtualThreadFactory"

        val thread = factory.newThread {
            Thread.sleep(100)
            log.debug { "New Virtual Thread" }
            Thread.sleep(100)
        }

        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.name shouldBeEqualTo "virtual-thread-0"
        thread.isDaemon shouldBeEqualTo true
        thread.priority shouldBeEqualTo 5

        // Thread가 시작되지 않았으므로 NEW 상태
        thread.state shouldBeEqualTo Thread.State.NEW

        thread.start()

        await until { thread.state == Thread.State.RUNNABLE || thread.state == Thread.State.TIMED_WAITING }

        // Thread가 시작되었으므로 RUNNABLE or TIMED_WAITING 상태 (Thread.sleep() 때문에)
        thread.state shouldNotBeEqualTo Thread.State.NEW
        thread.state shouldNotBeEqualTo Thread.State.TERMINATED

        thread.join()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread 자동 시작하기`() {
        val thread = virtualThread(start = true, prefix = "active-thread-") {
            sleep(1000)
            log.debug { "Virtual Thread auto running" }
        }
        thread.isAlive.shouldBeTrue()

        // Thread가 시작되었으므로 RUNNABLE or TIMED_WAITING 상태이어야 한다 (Thread.sleep() 때문에)
        thread.state shouldNotBeEqualTo Thread.State.NEW
        thread.state shouldNotBeEqualTo Thread.State.TERMINATED
        thread.join()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `Virtual Thread 수동 시작하기 `() {
        val thread = virtualThread(start = false, prefix = "passive-thread-") {
            sleep(1000)
            log.debug { "Virtual Thread manual running" }
        }
        // Thread가 시작되지 않았으므로 NEW 상태
        thread.state shouldBeEqualTo Thread.State.NEW

        thread.start()

        // Thread가 시작되었으므로 RUNNABLE or TIMED_WAITING 상태 (Thread.sleep() 때문에)
        thread.state shouldNotBeEqualTo Thread.State.NEW
        thread.state shouldNotBeEqualTo Thread.State.TERMINATED

        thread.join()
    }
}
