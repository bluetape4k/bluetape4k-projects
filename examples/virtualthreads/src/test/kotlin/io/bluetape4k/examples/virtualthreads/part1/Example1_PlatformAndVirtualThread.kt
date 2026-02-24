package io.bluetape4k.workshop.virtualthread.part1

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test

class Example1_PlatformAndVirtualThread {

    companion object: KLoggingChannel() {
        private const val THREAD_SIZE = 100_000
    }

    @Test
    fun `Platform thread를 생성자로 생성하기`() {
        val thread = Thread {
            println("Platform Thread")
        }
        thread.start()
        thread.join()

        thread shouldBeInstanceOf Thread::class
        thread.isVirtual.shouldBeFalse()
        thread.name.shouldNotBeEmpty()
    }

    @Test
    fun `Platform thread 를 Builder 로 생성하기`() {
        val thread = Thread.ofPlatform().start {
            log.debug { "Platform Thread" }
        }
        thread.join()

        thread shouldBeInstanceOf Thread::class
        thread.isVirtual.shouldBeFalse()
        thread.name.shouldNotBeEmpty()
    }

    @Test
    fun `Virtual Thread 를 static factory method로 생성하기`() {
        val thread = Thread.startVirtualThread {
            log.debug { "Virtual Thread" }
        }
        thread.join()

        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.isVirtual.shouldBeTrue()
        thread.name.shouldBeEmpty()
    }

    @Test
    fun `Virtual Thread를 Builder로 생성하기`() {
        val thread = Thread.ofVirtual().start {
            log.debug { "Virtual Thread" }
        }
        thread.join()

        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.isVirtual.shouldBeTrue()
        thread.name.shouldBeEmpty()
    }

    @Test
    fun `Platform Thread 로 Blocking 코드 실행하기`() {
        val threads = List(THREAD_SIZE / 100) {
            Thread.ofPlatform().start {
                Thread.sleep(1000)
                log.debug { "Platform Thread $it" }
            }
        }
        threads.forEach { it.join() }
    }

    @Test
    fun `Virtual Thread 로 Blocking 코드 실행하기`() {
        val threads = List(THREAD_SIZE) {
            Thread.ofVirtual().start {
                Thread.sleep(1000)
                log.debug { "Virtual Thread $it" }
            }
        }
        threads.forEach { it.join() }
    }
}
