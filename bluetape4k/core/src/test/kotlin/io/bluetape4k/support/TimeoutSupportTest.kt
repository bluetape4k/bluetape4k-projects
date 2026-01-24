package io.bluetape4k.support

import io.bluetape4k.concurrent.FutureUtils
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class TimeoutSupportTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간이 적용된 비동기 작업을 수행한다`() {
        val future = asyncRunWithTimeout(1000) {
            Thread.sleep(100)
        }

        future.get() // 완료되어야 함
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간이 적용된 복수의 비동기 작업을 수행`() {
        val futures = List(1000) {
            asyncRunWithTimeout(1000) {
                Thread.sleep(10)
            }
        }

        FutureUtils.allAsList(futures)
            .orTimeout(1000, TimeUnit.MILLISECONDS)
            .get() // 모두 완료되어야 함
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간을 초과하는 비동기 작업은 예외를 발생시킨다`() {
        assertFailsWith<ExecutionException> {
            asyncRunWithTimeout(100) {
                Thread.sleep(1000)
            }.get()
        }.cause shouldBeInstanceOf TimeoutException::class
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간을 초과하는 비동기 작업에 대한 Non-Blocking 방법`() = runTest {
        assertFailsWith<TimeoutException> {
            val future = asyncRunWithTimeout(100) {
                Thread.sleep(1000)
            }
            future.await()
        }
    }


    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `함수 실행이 timeout에 걸릴때는 예외를 발생시키는 CompletableFuture 반환한다`() {
        val isWorking = AtomicBoolean(false)
        val future = asyncRunWithTimeout(100) {
            var i = 0
            isWorking.set(true)
            while (true) {
                Thread.sleep(100)
                log.trace { "Working... $i" }
                i++
            }
            isWorking.set(false)
            "Hello"
        }
        Thread.sleep(200)
        future.isDone.shouldBeTrue()
        future.isCompletedExceptionally.shouldBeTrue()

        assertFailsWith<ExecutionException> {
            future.get()
        }
        log.trace { "작업 종료: ${isWorking.get()}" }
    }

    @Test
    fun `함수 실행이 timeout 에 걸리지 않으면 작업 결과를 반환한다`() {
        val isWorking = AtomicBoolean(false)
        val future = asyncRunWithTimeout(100) {
            isWorking.set(true)
            var i = 0
            while (i < 2) {
                Thread.sleep(10)
                log.trace { "Working... $i" }
                i++
            }
            isWorking.set(false)
            "Hello"
        }

        await until { !isWorking.get() }

        future.get() shouldBeEqualTo "Hello"
        log.trace { "작업 종료: ${isWorking.get()}" }
    }

    @Test
    fun `함수 실행이 timeout에 걸릴때는 null을 반환한다`() {
        val workIsStarted = AtomicBoolean(false)
        val result = withTimeoutOrNull(100) {
            var i = 0
            workIsStarted.set(true)
            while (true) {
                Thread.sleep(10)
                log.trace { "Working... $i" }
                i++
            }
            //            workIsStarted.value = false
            //            "Hello"
        }

        result.shouldBeNull()
        workIsStarted.get().shouldBeTrue()
        Thread.sleep(10)
        log.trace { "작업 시작 여부: ${workIsStarted.get()}" }
    }

    @Test
    fun `함수 실행이 timeout 보다 빨리 끝나면 함수 실행 반환값을 반환한다`() {
        val isWorking = AtomicBoolean(false)
        val result = withTimeoutOrNull(500) {
            isWorking.set(true)
            var i = 0
            while (i < 2) {
                Thread.sleep(100)
                log.trace { "Working... $i" }
                i++
            }
            isWorking.set(false)
            "Hello"
        }

        result shouldBeEqualTo "Hello"
        log.trace { "작업 종료: ${isWorking.get()}" }
        isWorking.get().shouldBeFalse()
    }
}
