package io.bluetape4k.workshop.virtualthread.part1


import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.workshop.virtualThreads.AbstractVirtualThreadTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class Example4_VirtualThreadFactory: AbstractVirtualThreadTest() {

    companion object: KLoggingChannel()

    @Test
    fun `Virtual Thread Factory를 사용하여 Virtual Thread 생성하기`() {
        val builder = Thread.ofVirtual().name("vt")

        val factory = builder.factory()
        factory.javaClass.name shouldBeEqualTo "java.lang.ThreadBuilders\$VirtualThreadFactory"

        val thread = factory.newThread {
            Thread.sleep(1000)
            log.debug { "Virtual Thread" }
        }

        // 아직 시작되지 않은 상태
        thread.javaClass.name shouldBeEqualTo "java.lang.VirtualThread"
        thread.isVirtual.shouldBeTrue()
        thread.name shouldBeEqualTo "vt"
        thread.state shouldBeEqualTo Thread.State.NEW

        thread.start()

        thread.state shouldBeEqualTo Thread.State.RUNNABLE
        thread.isAlive.shouldBeTrue()

        thread.join()
    }

    @Test
    fun `대량의 Blocking 코드를 Virtual Thread로 실행하기`() {
        // Virtual Thread 는 Coroutines 와 달리 Blocking 코드를 실행할 때도 사용할 수 있습니다.
        val threads = List(100_000) {
            Thread.ofVirtual().unstarted {
                Thread.sleep(1000)
                print(".")
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }
    }

    @Test
    fun `대량의 Blocking 코드를 Coroutines 로 실행하기`() = runSuspendTest {
        // Coroutines 는 대량의 작업을 실행할 때 더 효율적입니다.
        val jobs = List(100_000) {
            launch(Dispatchers.VT) {
                Thread.sleep(1000)
                print(".")
            }
        }

        jobs.joinAll()
    }
}
