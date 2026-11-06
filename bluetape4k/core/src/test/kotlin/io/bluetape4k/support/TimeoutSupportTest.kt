package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.fail
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.concurrent.FutureUtils
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TimeoutSupportTest: AbstractCoreTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    private class TrackingExecutor(
        private val rejectSubmissions: Boolean = false,
    ): AbstractExecutorService() {
        private val delegate = Executors.newVirtualThreadPerTaskExecutor()
        private val shutdownSignal = CountDownLatch(1)

        override fun execute(command: Runnable) {
            if (rejectSubmissions) {
                throw RejectedExecutionException("synthetic submission rejection")
            }
            delegate.execute(command)
        }

        override fun shutdown() {
            delegate.shutdown()
            shutdownSignal.countDown()
        }

        override fun shutdownNow(): MutableList<Runnable> = delegate.shutdownNow()

        override fun isShutdown(): Boolean = delegate.isShutdown

        override fun isTerminated(): Boolean = delegate.isTerminated

        override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean =
            delegate.awaitTermination(timeout, unit)

        fun awaitShutdown(timeout: Long = 1, unit: TimeUnit = TimeUnit.SECONDS): Boolean =
            shutdownSignal.await(timeout, unit)
    }

    @Test
    fun `Duration 경계값은 millisecond timeout 정책으로 정규화된다`() {
        (-1).milliseconds.toTimeoutMillis() shouldBeEqualTo MIN_TIMEOUT_MILLIS
        Duration.ZERO.toTimeoutMillis() shouldBeEqualTo MIN_TIMEOUT_MILLIS
        500.microseconds.toTimeoutMillis() shouldBeEqualTo MIN_TIMEOUT_MILLIS
        Long.MAX_VALUE.nanoseconds.toTimeoutMillis() shouldBeEqualTo 9_223_372_036_854L
        Duration.INFINITE.toTimeoutMillis() shouldBeEqualTo Long.MAX_VALUE
    }

    @Test
    fun `Duration INFINITE은 즉시 완료되는 작업을 timeout시키지 않는다`() {
        asyncRunWithTimeout(Duration.INFINITE) { "ok" }.get(1, TimeUnit.SECONDS) shouldBeEqualTo "ok"
    }

    @Test
    fun `동기 제출 거부에서도 소유 executor를 종료한다`() {
        val executor = TrackingExecutor(rejectSubmissions = true)

        assertFailsWith<RejectedExecutionException> {
            asyncRunWithTimeout(timeoutMillis = 100, executor = executor) { "never" }
        }

        executor.isShutdown.shouldBeTrue()
    }

    @Test
    fun `성공 timeout 취소 완료 시 소유 executor를 종료한다`() {
        val successExecutor = TrackingExecutor()
        asyncRunWithTimeout(timeoutMillis = 1000, executor = successExecutor) { "ok" }.get()
        successExecutor.awaitShutdown().shouldBeTrue()

        val timeoutExecutor = TrackingExecutor()
        val timeoutRelease = CountDownLatch(1)
        try {
            assertFailsWith<ExecutionException> {
                asyncRunWithTimeout(timeoutMillis = 100, executor = timeoutExecutor) {
                    timeoutRelease.await()
                    "late"
                }.get()
            }.cause.shouldBeInstanceOf<TimeoutException>()
            timeoutExecutor.awaitShutdown().shouldBeTrue()
        } finally {
            timeoutRelease.countDown()
            timeoutExecutor.shutdownNow()
        }

        val cancelExecutor = TrackingExecutor()
        val started = CountDownLatch(1)
        val cancelRelease = CountDownLatch(1)
        try {
            val future = asyncRunWithTimeout(timeoutMillis = 1000, executor = cancelExecutor) {
                started.countDown()
                cancelRelease.await()
                "late"
            }
            started.await(1, TimeUnit.SECONDS).shouldBeTrue()
            future.cancel(false).shouldBeTrue()
            cancelExecutor.awaitShutdown().shouldBeTrue()
        } finally {
            cancelRelease.countDown()
            cancelExecutor.shutdownNow()
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간이 적용된 비동기 작업을 수행한다`() {
        var executed = false

        val future = asyncRunWithTimeout(1000) {
            Thread.sleep(100)
            executed = true
        }

        future.get() // 완료되어야 함
        executed.shouldBeTrue()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간이 적용된 복수의 비동기 작업을 수행`() {
        val taskSize = 1000
        val done = ConcurrentLinkedQueue<Int>()

        val futures = List(taskSize) {
            asyncRunWithTimeout(1000) {
                Thread.sleep(10)
                done.add(it)
            }
        }

        FutureUtils.allAsList(futures, Executors.newVirtualThreadPerTaskExecutor())
            .orTimeout(1000, TimeUnit.MILLISECONDS)
            .get()
            .all { it }.shouldBeTrue() // 모두 완료되어야 함

        done.size shouldBeEqualTo taskSize
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간을 초과하는 비동기 작업은 예외를 발생시킨다`() {
        var executed = false
        assertFailsWith<ExecutionException> {
            asyncRunWithTimeout(100) {
                Thread.sleep(1000)
                executed = true
            }.get()
        }.cause.shouldBeInstanceOf<TimeoutException>()

        executed.shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `제한시간을 초과하는 비동기 작업에 대한 Non-Blocking 방법`() = runTest {
        var executed = false

        assertFailsWith<TimeoutException> {
            val future = asyncRunWithTimeout(100) {
                Thread.sleep(1000)
                executed = true
                fail("타임아웃으로 중단되어야 합니다")
            }
            future.await()

        }
        executed.shouldBeFalse()
    }


    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `함수 실행이 timeout에 걸릴 때는 예외로 완료된 CompletableFuture 반환한다`() {
        val isWorking = AtomicBoolean(false)

        val future = asyncRunWithTimeout(100) {
            var i = 0
            isWorking.set(true)
            while (true) {
                Thread.sleep(100)
                log.debug { "Working... $i" }
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
        log.debug { "작업 진행 중: ${isWorking.get()}" }

        isWorking.get().shouldBeTrue()
    }

    @Test
    fun `함수 실행이 timeout 에 걸리지 않으면 작업 결과를 반환한다`() {
        val isWorking = AtomicBoolean(false)
        val future = asyncRunWithTimeout(100) {
            isWorking.set(true)
            var i = 0
            while (i < 2) {
                Thread.sleep(10)
                log.debug { "Working... $i" }
                i++
            }
            isWorking.set(false)
            "Hello"
        }

        await until { !isWorking.get() }

        future.get() shouldBeEqualTo "Hello"
        log.debug { "작업 진행 중: ${isWorking.get()}" }
        isWorking.get().shouldBeFalse()
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `함수 실행이 timeout에 걸릴때는 null을 반환한다`() {
        val workIsStarted = AtomicBoolean(false)
        val result = withTimeoutOrNull(100) {
            var i = 0
            workIsStarted.set(true)
            while (true) {
                Thread.sleep(10)
                log.debug { "Working... $i" }
                i++
            }
            workIsStarted.set(false)
            "Hello"
        }

        result.shouldBeNull()
        workIsStarted.get().shouldBeTrue()
        Thread.sleep(10)
        log.debug { "작업 시작 여부: ${workIsStarted.get()}" }
        workIsStarted.get().shouldBeTrue()
    }

    @Test
    fun `함수 실행이 timeout 보다 빨리 끝나면 함수 실행 반환값을 반환한다`() {
        val isWorking = AtomicBoolean(false)
        val result = withTimeoutOrNull(500) {
            isWorking.set(true)
            var i = 0
            while (i < 2) {
                Thread.sleep(100)
                log.debug { "Working... $i" }
                i++
            }
            isWorking.set(false)
            "Hello"
        }

        result shouldBeEqualTo "Hello"
        log.debug { "작업 진행 중: ${isWorking.get()}" }
        isWorking.get().shouldBeFalse()
    }

    @Test
    fun `withTimeoutOrNull은 timeout 외 예외를 null로 삼키지 않고 전파한다`() {
        // H1 수정 검증: TimeoutException 이외의 예외는 rethrow 되어야 함
        assertFailsWith<ExecutionException> {
            withTimeoutOrNull(1000) {
                throw IllegalStateException("비즈니스 오류")
                @Suppress("UNREACHABLE_CODE")
                "result"
            }
        }
    }

    @Test
    fun `retryWithTimeoutOrNull은 제한 횟수만큼 시도하고 모두 timeout되면 null을 반환한다`() {
        val attempts = AtomicInteger()
        val startedActions = CountDownLatch(2)
        val finishedActions = CountDownLatch(2)

        val result = retryWithTimeoutOrNull(maxRetries = 2, timeout = 100.milliseconds) {
            attempts.incrementAndGet()
            startedActions.countDown()
            try {
                Thread.sleep(300)
            } finally {
                finishedActions.countDown()
            }
            "late"
        }

        result.shouldBeNull()
        attempts.get() shouldBeEqualTo 2
        startedActions.await(1, TimeUnit.SECONDS).shouldBeTrue()
        finishedActions.await(1, TimeUnit.SECONDS).shouldBeTrue()
    }

    @Test
    fun `retryWithTimeoutOrNull은 첫 성공 결과를 반환하고 양수 시도 횟수를 요구한다`() {
        val attempts = AtomicInteger()

        retryWithTimeoutOrNull(maxRetries = 3, timeout = 1.seconds) {
            attempts.incrementAndGet()
            "result"
        } shouldBeEqualTo "result"
        attempts.get() shouldBeEqualTo 1

        assertFailsWith<IllegalArgumentException> {
            retryWithTimeoutOrNull(maxRetries = 0, timeout = 1.seconds) { "unreachable" }
        }
    }
}
