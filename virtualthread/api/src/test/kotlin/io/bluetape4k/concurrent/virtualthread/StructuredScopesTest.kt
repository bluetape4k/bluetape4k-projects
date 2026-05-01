package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.TimeoutException
import kotlin.test.assertFailsWith

/**
 * [StructuredTaskScopes], [StructuredTaskScopeProvider], [StructuredSubtask], [StructuredTaskScopeAll],
 * [StructuredTaskScopeAny] 인터페이스 및 기본 구현 커버리지 테스트입니다.
 */
class StructuredScopesTest {

    companion object: KLogging()

    // test runtime에는 jdk21 provider (testRuntimeOnly)가 classpath에 등록되어 있으므로 정상 반환됩니다.
    @Test
    fun `provider 반환 이름이 비어있지 않아야 한다`() {
        val provider = StructuredTaskScopes.provider()
        provider.providerName.shouldNotBeBlank()
        log.debug { "provider name: ${provider.providerName}" }
    }

    @Test
    fun `providerName 메서드가 비어있지 않은 문자열을 반환해야 한다`() {
        StructuredTaskScopes.providerName().shouldNotBeBlank()
    }

    @Test
    fun `provider isSupported 가 true 여야 한다`() {
        val provider = StructuredTaskScopes.provider()
        provider.isSupported().shouldBeTrue()
    }

    @Test
    fun `provider priority 가 양수여야 한다`() {
        val provider = StructuredTaskScopes.provider()
        provider.priority.shouldBeGreaterThan(0)
    }

    // ── 기존 all/any 회귀 테스트 (deprecated API 동작 검증) ─────────────────────

    @Suppress("DEPRECATION")
    @Test
    fun `all scope 으로 두 subtask 를 합산해야 한다`() {
        val result = StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
            val a = scope.fork { 10 }
            val b = scope.fork { 20 }
            scope.join().throwIfFailed()
            a.get() + b.get()
        }
        result shouldBeEqualTo 30
    }

    @Suppress("DEPRECATION")
    @Test
    fun `all scope 내 subtask 실패 시 예외가 전파되어야 한다`() {
        assertFailsWith<RuntimeException> {
            StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalStateException("subtask failed") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `all scope throwIfFailed 핸들러가 호출되어야 한다`() {
        var handlerCalled = false
        assertFailsWith<RuntimeException> {
            StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
                scope.fork<Int> { throw RuntimeException("error") }
                scope.join().throwIfFailed { handlerCalled = true }
                0
            }
        }
        handlerCalled.shouldBeTrue()
    }

    @Test
    fun `all scope joinUntil 기본 구현이 join 을 호출해야 한다`() {
        var joinCalled = false
        val scope = object : StructuredTaskScopeAll {
            override fun <T> fork(task: () -> T): StructuredSubtask<T> {
                @Suppress("UNCHECKED_CAST")
                return object : StructuredSubtask<T> {
                    override fun get(): T = task()
                    override fun state(): StructuredTaskScope.Subtask.State = StructuredTaskScope.Subtask.State.SUCCESS
                    override fun exceptionOrNull(): Throwable? = null
                }
            }

            override fun join(): StructuredTaskScopeAll {
                joinCalled = true
                return this
            }

            override fun throwIfFailed(handler: (e: Throwable) -> Unit): StructuredTaskScopeAll = this
            override fun close() {}
        }

        scope.joinUntil(Instant.now().plusSeconds(5))
        joinCalled.shouldBeTrue()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `any scope 가 가장 먼저 완료된 subtask 결과를 반환해야 한다`() {
        val result = StructuredTaskScopes.any<String>(factory = Thread.ofVirtual().factory()) { scope ->
            scope.fork {
                Thread.sleep(50)
                "slow"
            }
            scope.fork {
                Thread.sleep(10)
                "fast"
            }
            scope.join().result { IllegalStateException(it) }
        }
        result shouldBeEqualTo "fast"
    }

    @Suppress("DEPRECATION")
    @Test
    fun `any scope 모든 subtask 실패 시 mapper 예외가 발생해야 한다`() {
        assertFailsWith<IllegalStateException> {
            StructuredTaskScopes.any<String>(factory = Thread.ofVirtual().factory()) { scope ->
                scope.fork<String> { throw RuntimeException("fail1") }
                scope.fork<String> { throw RuntimeException("fail2") }
                scope.join().result { IllegalStateException("all failed: $it") }
            }
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `all scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = StructuredTaskScopes.all(name = "test-scope", factory = Thread.ofVirtual().factory()) { scope ->
            val task = scope.fork { 42 }
            scope.join().throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 42
    }

    @Suppress("DEPRECATION")
    @Test
    fun `subtask 성공 상태와 결과를 확인해야 한다`() {
        var capturedSubtask: StructuredSubtask<Int>? = null
        StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
            capturedSubtask = scope.fork { 99 }
            scope.join().throwIfFailed()
        }

        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.get() shouldBeEqualTo 99
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        subtask.exceptionOrNull().shouldBeNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun `subtask 실패 상태에서 exceptionOrNull 이 예외를 반환해야 한다`() {
        var capturedSubtask: StructuredSubtask<Int>? = null
        assertFailsWith<RuntimeException> {
            StructuredTaskScopes.all(factory = Thread.ofVirtual().factory()) { scope ->
                capturedSubtask = scope.fork<Int> { throw RuntimeException("boom") }
                scope.join().throwIfFailed()
                0
            }
        }
        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.FAILED
        subtask.exceptionOrNull().shouldNotBeNull().shouldBeInstanceOf<RuntimeException>()
    }

    // ── failFast 신규 API 테스트 ────────────────────────────────────────────────

    @Test
    fun `failFast scope 으로 두 subtask 를 합산해야 한다`() {
        val result = StructuredTaskScopes.failFast { scope ->
            val a = scope.fork { 10 }
            val b = scope.fork { 20 }
            scope.join().throwIfFailed()
            a.get() + b.get()
        }
        result shouldBeEqualTo 30
    }

    @Test
    fun `failFast scope 내 subtask 실패 시 예외가 전파되어야 한다`() {
        assertFailsWith<RuntimeException> {
            StructuredTaskScopes.failFast { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalStateException("subtask failed") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `failFast scope factory 기본값으로 실행되어야 한다`() {
        // factory 파라미터 생략 → VirtualThreads.threadFactory() 기본값 사용
        val result = StructuredTaskScopes.failFast { scope ->
            val task = scope.fork { 42 }
            scope.join().throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 42
    }

    // ── firstSuccess 신규 API 테스트 ────────────────────────────────────────────

    @Test
    fun `firstSuccess scope 가 가장 먼저 완료된 subtask 결과를 반환해야 한다`() {
        val result = StructuredTaskScopes.firstSuccess<String> { scope ->
            scope.fork {
                Thread.sleep(50)
                "slow"
            }
            scope.fork {
                Thread.sleep(10)
                "fast"
            }
            scope.join().result { IllegalStateException("all failed: ${it.message}") }
        }
        result shouldBeEqualTo "fast"
    }

    @Test
    fun `firstSuccess scope 모든 subtask 실패 시 mapper 예외가 발생해야 한다`() {
        // result(mapper)의 mapper가 만든 예외가 throw — StructuredTaskScope.FailedException 아님
        assertFailsWith<IllegalStateException> {
            StructuredTaskScopes.firstSuccess<String> { scope ->
                scope.fork<String> { throw RuntimeException("task1 fail") }
                scope.fork<String> { throw RuntimeException("task2 fail") }
                scope.join().result { IllegalStateException("all failed: ${it.message}") }
            }
        }
    }

    // ── getOrNull 테스트 ─────────────────────────────────────────────────────────

    @Test
    fun `getOrNull 은 SUCCESS 상태에서 결과를 반환해야 한다`() {
        var capturedSubtask: StructuredSubtask<Int>? = null
        StructuredTaskScopes.failFast { scope ->
            capturedSubtask = scope.fork { 42 }
            scope.join().throwIfFailed()
        }
        capturedSubtask.shouldNotBeNull()
        capturedSubtask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        capturedSubtask!!.getOrNull() shouldBeEqualTo 42
    }

    @Test
    fun `getOrNull 은 FAILED 상태에서 null 을 반환해야 한다`() {
        var capturedSubtask: StructuredSubtask<Int>? = null
        runCatching {
            StructuredTaskScopes.failFast { scope ->
                capturedSubtask = scope.fork<Int> { throw RuntimeException("fail") }
                scope.join().throwIfFailed()
                0
            }
        }
        capturedSubtask.shouldNotBeNull()
        capturedSubtask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.FAILED
        capturedSubtask!!.getOrNull().shouldBeNull()
    }

    @Test
    fun `getOrNull 은 UNAVAILABLE 상태에서 null 을 반환해야 한다`() {
        val subtaskStarted = CountDownLatch(1)
        val proceedToFail = CountDownLatch(1)
        var cancelledTask: StructuredSubtask<Int>? = null

        // failFast<Unit>: 블록 반환 타입을 Unit으로 지정해 타입 불일치 방지
        runCatching {
            StructuredTaskScopes.failFast<Unit> { scope ->
                // subtask1: 실패하여 scope shutdown 트리거
                scope.fork<Unit> {
                    subtaskStarted.countDown()
                    proceedToFail.await()
                    throw RuntimeException("forced failure")
                }
                // subtask2: block 상태에서 shutdown에 의해 취소됨 (UNAVAILABLE)
                // neverRelease 는 해제되지 않으므로 shutdown 시 반드시 UNAVAILABLE 상태가 됨
                val neverRelease = CountDownLatch(1)
                cancelledTask = scope.fork {
                    subtaskStarted.await()
                    proceedToFail.countDown()
                    neverRelease.await()
                    42
                }
                scope.join().throwIfFailed()
            }
        }

        cancelledTask.shouldNotBeNull()
        cancelledTask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.UNAVAILABLE
        cancelledTask!!.getOrNull().shouldBeNull()
    }

    @Test
    fun `getOrNull 은 join 이전 호출에서도 null 을 안전하게 반환해야 한다 (내부 방어)`() {
        // 이 테스트는 내부 방어(internal defense) 테스트입니다.
        // getOrNull()의 KDoc 전제 조건은 "join() 이후 호출"이며, join() 이전 호출은 미정의 동작입니다.
        // ISE-safe try-catch가 join() 이전 상황에서도 안전하게 null을 반환함을 검증합니다.
        val ready = CountDownLatch(1)
        val hold = CountDownLatch(1)
        var earlySubtask: StructuredSubtask<Int>? = null

        StructuredTaskScopes.failFast { scope ->
            earlySubtask = scope.fork {
                ready.countDown()
                hold.await()
                99
            }
            ready.await()
            // join() 이전에 getOrNull() 호출 — ISE 가 아니라 null 반환이어야 한다
            val result = earlySubtask!!.getOrNull()
            result.shouldBeNull()
            hold.countDown()
            scope.join().throwIfFailed()
            earlySubtask!!.getOrNull() shouldBeEqualTo 99
        }
    }

    // ── joinUntil 타임아웃 테스트 ───────────────────────────────────────────────

    @Test
    fun `joinUntil 데드라인 초과 시 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<TimeoutException> {
            StructuredTaskScopes.failFast { scope ->
                scope.fork {
                    Thread.sleep(10_000)
                    42
                }
                // 100ms 데드라인 — subtask보다 훨씬 짧음
                scope.joinUntil(Instant.now().plusMillis(100)).throwIfFailed()
                0
            }
        }
    }

    // ── supervised scope 테스트 ─────────────────────────────────────────────────

    @Test
    fun `supervised scope 일부 성공 일부 실패 시 결과를 분리해야 한다`() {
        val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.fork { 1 }
            scope.fork { throw RuntimeException("fail2") }
            scope.fork { 3 }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.sorted() shouldBeEqualTo listOf(1, 3)
        failures.size shouldBeEqualTo 1
        failures[0].shouldBeInstanceOf<RuntimeException>()
    }

    @Test
    fun `supervised scope 모두 성공 시 successfulResults 에 전부 포함되어야 한다`() {
        val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.fork { 10 }
            scope.fork { 20 }
            scope.fork { 30 }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.sorted() shouldBeEqualTo listOf(10, 20, 30)
        failures.size shouldBeEqualTo 0
    }

    @Test
    fun `supervised scope 모두 실패 시 failedExceptions 에 전부 포함되어야 한다`() {
        val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.fork { throw RuntimeException("fail1") }
            scope.fork { throw IllegalStateException("fail2") }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.size shouldBeEqualTo 0
        failures.size shouldBeEqualTo 2
    }

    @Test
    fun `supervised scope 병렬 실행으로 thread-safe 하게 결과를 수집해야 한다`() {
        val taskCount = 100
        val failEvery = 10  // 10번째마다 실패
        val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            repeat(taskCount) { i ->
                if (i % failEvery == 0) {
                    scope.fork { throw RuntimeException("fail $i") }
                } else {
                    scope.fork { i }
                }
            }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        val expectedSuccessCount = taskCount - taskCount / failEvery
        val expectedFailCount = taskCount / failEvery
        successes.size shouldBeEqualTo expectedSuccessCount
        failures.size shouldBeEqualTo expectedFailCount
    }

    @Test
    fun `supervised scope joinUntil 데드라인 초과 시 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<TimeoutException> {
            StructuredTaskScopes.supervised<Int, Unit> { scope ->
                scope.fork {
                    Thread.sleep(10_000)
                    42
                }
                scope.joinUntil(Instant.now().plusMillis(100))
            }
        }
    }

    @Test
    fun `supervised scope 빈 fork 시 빈 결과를 반환해야 한다`() {
        val (successes, failures) = StructuredTaskScopes.supervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.size shouldBeEqualTo 0
        failures.size shouldBeEqualTo 0
    }

    @Test
    fun `supervised scope nullable T 타입에서 null 성공 결과도 수집되어야 한다`() {
        val (successes, failures) = StructuredTaskScopes.supervised<Int?, Pair<List<Int?>, List<Throwable>>> { scope ->
            scope.fork { 1 }
            scope.fork { null }  // 성공적으로 null 반환
            scope.fork { 3 }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.size shouldBeEqualTo 3  // null 포함
        successes.filterNotNull().sorted() shouldBeEqualTo listOf(1, 3)
        failures.size shouldBeEqualTo 0
    }

    @Test
    fun `supervised scope subtask 상태가 올바르게 반환되어야 한다`() {
        var successTask: StructuredSubtask<Int>? = null
        var failedTask: StructuredSubtask<Int>? = null

        StructuredTaskScopes.supervised<Int, Unit> { scope ->
            successTask = scope.fork { 42 }
            failedTask = scope.fork { throw RuntimeException("fail") }
            scope.join()
        }

        successTask.shouldNotBeNull()
        successTask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        successTask!!.getOrNull() shouldBeEqualTo 42
        successTask!!.exceptionOrNull().shouldBeNull()

        failedTask.shouldNotBeNull()
        failedTask!!.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.FAILED
        failedTask!!.exceptionOrNull().shouldNotBeNull().shouldBeInstanceOf<RuntimeException>()
    }
}
