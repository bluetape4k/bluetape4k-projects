package io.bluetape4k.examples.coroutines.guide

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool

class CoroutineExamples {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 2
        private const val ITEM_SIZE = 10_000
        private const val DELAY_TIME = 10L
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `run tasks in coroutine scope`() = runTest {
        // coroutineScope 내부의 비동기 함수는 모두 완료되도록 대기한다 
        coroutineScope {
            fastList(ITEM_SIZE) {
                launch(Dispatchers.IO) {
                    delay(DELAY_TIME)
                }
            }
            yield()

            fastList(ITEM_SIZE) {
                launch(Dispatchers.IO) {
                    delay(DELAY_TIME)
                }
            }
        }
    }

    @Disabled("Blocking 방식이라 실행이 겁나 오래 걸립니다")
    @RepeatedTest(REPEAT_SIZE)
    fun `run tasks in thread pool`() {
        val latch = CountDownLatch(ITEM_SIZE)
        val executor = ForkJoinPool.commonPool()
        fastList(ITEM_SIZE) {
            executor.execute {
                Thread.sleep(DELAY_TIME)
                latch.countDown()
            }
        }
        latch.await()
    }
}
