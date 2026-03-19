package io.bluetape4k.examples.virtualthreads.part3

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.examples.virtualthreads.AbstractVirtualThreadTest
import io.bluetape4k.junit5.coroutines.runSuspendVT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.atomic.AtomicInteger

/**
 * 동기 코드를 Coroutines 환경에서 `Executors.newVirtualThreadPerTaskExecutor()` 를 이용해서 비동기로 실행할 수 있습니다.
 */
class CoroutineWithVirtualThread: AbstractVirtualThreadTest() {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    private val counter = AtomicInteger(0)

    @BeforeEach
    fun setup() {
        counter.set(0)
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `동기 코드를 Virtual Thread 를 사용하는 Coroutine Context 에서 실행하기`() = runSuspendVT {
        val jobs = List(100_000) {
            launch {
                myAsyncCode()
            }
        }
        jobs.joinAll()
    }

    private suspend fun myAsyncCode() {
        // 동기 코드를 [Dispatchers.VT]를 이용하여 Coroutines 환경에서 실행하기 
        withContext(Dispatchers.VT) {
            mySyncCode()
        }
    }

    private fun mySyncCode() {
        Thread.sleep(100)
        if (counter.incrementAndGet() % 1000L == 0L) {
            println("[${Thread.currentThread().name}] [${counter.get()}] Steel working ...")
        }
    }
}
