package io.bluetape4k.workshop.virtualthread.part1

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class Example2_PlatformAndVirtualThreadBuilder {

    companion object: KLoggingChannel()

    @Test
    fun `Platform Thread Builder 사용`() {
        val builder = Thread.ofPlatform()
            .daemon(false)
            .priority(10)
            .stackSize(1024)
            .name("platform-thread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler { thread, ex ->
                print("Thread[$thread] failed with exception: $ex")
            }

        print("Builder class=${builder.javaClass.name}")
        builder.javaClass.name shouldBeEqualTo "java.lang.ThreadBuilders\$PlatformThreadBuilder"

        val thread = builder.unstarted {
            print("Platform Thread")
        }

        thread.javaClass.name shouldBeEqualTo "java.lang.Thread"
        thread.name shouldBeEqualTo "platform-thread"
        thread.isDaemon.shouldBeFalse()
        thread.priority shouldBeEqualTo 10
    }

    @Test
    fun `Virtual Thread Builder 사용하기`() {
        val builder = Thread.ofVirtual()
            .name("virtual-thread")
            .inheritInheritableThreadLocals(false)
            .uncaughtExceptionHandler { thread, ex ->
                log.error(ex) { "Thread[$thread] failed with exception." }
            }

        println("Builder class=${builder.javaClass.name}")
        builder.javaClass.name shouldBeEqualTo "java.lang.ThreadBuilders\$VirtualThreadBuilder"

        val thread = builder.unstarted {
            println("Unstarted Virtual Thread")
        }
        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.name shouldBeEqualTo "virtual-thread"
        thread.isDaemon.shouldBeTrue()
        thread.priority shouldBeEqualTo 5
    }
}
