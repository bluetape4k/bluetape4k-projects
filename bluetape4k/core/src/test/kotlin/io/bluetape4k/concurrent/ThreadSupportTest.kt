package io.bluetape4k.concurrent

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock

class ThreadSupportTest {

    companion object: KLogging()

    @Test
    fun `최소한 1개 이상의 active thread 가 있다`() {
        getAllThreads().isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `최소 1개 이상의 active thread group 이 있다`() {
        getAllThreadGroups().isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `complex thread groups`() {
        val threadGroup1 = ThreadGroup("thread_group_1__")
        val threadGroup2 = ThreadGroup("thread_group_2__")
        val threadGroup3 = ThreadGroup(threadGroup2, "thread_group_3__")
        val threadGroup4 = ThreadGroup(threadGroup2, "thread_group_4__")
        val threadGroup5 = ThreadGroup(threadGroup1, "thread_group_5__")
        val threadGroup6 = ThreadGroup(threadGroup4, "thread_group_6__")
        val threadGroup7 = ThreadGroup(threadGroup4, "thread_group_7__")
        val threadGroup7Doubled = ThreadGroup(threadGroup4, "thread_group_7__")
        val threadGroups = listOf(
            threadGroup1, threadGroup2, threadGroup3,
            threadGroup4, threadGroup5, threadGroup6,
            threadGroup7, threadGroup7Doubled
        )

        val t1: Thread = TestThread("thread1_X__")
        val t2: Thread = TestThread(threadGroup1, "thread2_X__")
        val t3: Thread = TestThread(threadGroup2, "thread3_X__")
        val t4: Thread = TestThread(threadGroup3, "thread4_X__")
        val t5: Thread = TestThread(threadGroup4, "thread5_X__")
        val t6: Thread = TestThread(threadGroup5, "thread6_X__")
        val t7: Thread = TestThread(threadGroup6, "thread7_X__")
        val t8: Thread = TestThread(threadGroup4, "thread8_X__")
        val t9: Thread = TestThread(threadGroup6, "thread9_X__")
        val t10: Thread = TestThread(threadGroup3, "thread10_X__")
        val t11: Thread = TestThread(threadGroup7, "thread11_X__")
        val t11Doubled: Thread = TestThread(threadGroup7Doubled, "thread11_X__")

        val threads = listOf(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t11Doubled)

        try {
            for (thread in threads) {
                thread.start()
            }
            getAllThreadGroups().size shouldBeGreaterOrEqualTo 7

            getAllThreads().size shouldBeGreaterOrEqualTo 11

            findThreads { true }.size shouldBeGreaterOrEqualTo 11

            findThreadByName(t4.name, threadGroup3.name).size shouldBeEqualTo 1
            findThreadByName(t4.name, threadGroup2.name).size shouldBeEqualTo 0
            findThreadByName(t11.name, threadGroup7.name).size shouldBeEqualTo 2
        } finally {
            for (thread in threads) {
                thread.interrupt()
                thread.join()
            }
        }
    }

    private class TestThread: Thread {

        private val lock = ReentrantLock()
        private val latch = CountDownLatch(1)

        constructor(name: String): super(name)

        constructor(group: ThreadGroup?, name: String): super(group, name)

        override fun run() {
            latch.countDown()
            try {
                synchronized(lock) {
                    (lock as java.lang.Object).wait()
                    // (this as java.lang.Object).wait()
                }
            } catch (e: InterruptedException) {
                currentThread().interrupt()
            }
        }

        override fun start() {
            super.start()
            try {
                latch.await()
            } catch (e: InterruptedException) {
                currentThread().interrupt()
            }
        }
    }
}
