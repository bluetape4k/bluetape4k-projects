package io.bluetape4k.workshop.virtualthread.part3

import io.bluetape4k.concurrent.virtualthread.VT
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.RepeatedTest

/**
 * 동기 코드를 Coroutines 환경에서 `Executors.newVirtualThreadPerTaskExecutor()` 를 이용해서 비동기로 실행할 수 있습니다.
 */
class CoroutineWithVirtualThread {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 3
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `동기 코드를 Virtual Thread 를 사용하는 Coroutine Context 에서 실행하기`() = runTest {
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
        Thread.sleep(1000)
        print(".")
    }
}
