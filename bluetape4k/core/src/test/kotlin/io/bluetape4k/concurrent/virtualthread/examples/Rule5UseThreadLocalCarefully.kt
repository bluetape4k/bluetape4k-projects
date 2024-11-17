package io.bluetape4k.concurrent.virtualthread.examples

import io.bluetape4k.concurrent.virtualthread.AbstractVirtualThreadTest
import io.bluetape4k.concurrent.virtualthread.runWith
import io.bluetape4k.concurrent.virtualthread.structuredTaskScopeAll
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith


/**
 * ### Rule 5
 *
 * Virtual Thread 환경에서는 ThreadLocal 을 사용하면 오염이 될 수 있습니다. 대신 [ScopedValue] 를 사용하세요
 */
class Rule5UseThreadLocalCarefully: AbstractVirtualThreadTest() {

    companion object: KLogging()

    @Nested
    inner class DoNot {

        private val threadLocal = InheritableThreadLocal<String>()

        @Test
        fun `비추천 - ThreadLocal 변수를 사용하기`() {
            threadLocal.set("zero")
            threadLocal.get() shouldBeEqualTo "zero"

            threadLocal.set("one")
            threadLocal.get() shouldBeEqualTo "one"

            val childThread = Thread {
                threadLocal.get() shouldBeEqualTo "one"
            }
            childThread.start()
            childThread.join()

            threadLocal.remove()
            threadLocal.get().shouldBeNull()
        }
    }

    @Nested
    inner class Do {

        private val scopedValue = ScopedValue.newInstance<String>()

        @Test
        fun `추천 - ScopedValue 사용하기`() {

            scopedValue.runWith("zero") { scopeZero ->
                scopeZero.get() shouldBeEqualTo "zero"

                scopeZero.runWith("one") { scopeOne ->
                    scopeOne.get() shouldBeEqualTo "one"
                }

                scopeZero.get() shouldBeEqualTo "zero"

                try {
                    structuredTaskScopeAll { scope ->
                        scope.fork {
                            scopeZero.get() shouldBeEqualTo "zero"
                            -1
                        }
                        scope.join().throwIfFailed()
                    }
                } catch (e: InterruptedException) {
                    fail(e)
                }
            }

            assertFailsWith<NoSuchElementException> {
                scopedValue.get()
            }
        }
    }
}
