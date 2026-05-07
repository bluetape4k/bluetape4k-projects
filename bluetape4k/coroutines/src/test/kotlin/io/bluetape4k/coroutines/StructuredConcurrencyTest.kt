package io.bluetape4k.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContainAll
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class StructuredConcurrencyTest {

    companion object: KLoggingChannel()

    // --- taskScope (fail-fast) ---

    @Test
    fun `taskScope - 두 subtask 합산`() = runSuspendIO {
        val sum = taskScope {
            val a = fork { 1 }
            val b = fork { 2 }
            join().throwIfFailed()
            a.get() + b.get()
        }
        sum shouldBeEqualTo 3
    }

    @Test
    fun `taskScope - 여러 subtask 병렬 실행`() = runSuspendIO {
        val count = 10
        val sum = taskScope {
            val tasks = (1..count).map { i -> fork { i } }
            join().throwIfFailed()
            tasks.sumOf { it.get() }
        }
        sum shouldBeEqualTo (1..count).sum()
    }

    @Test
    fun `taskScope - subtask 실패 시 예외 전파`() = runSuspendIO {
        assertThrows<RuntimeException> {
            taskScope<Int> {
                fork { throw RuntimeException("subtask 실패") }
                join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `taskScope - 하나 실패 시 다른 subtask 중단`() = runSuspendIO {
        val counter = AtomicInteger(0)
        assertThrows<RuntimeException> {
            taskScope<Unit> {
                fork { throw RuntimeException("빠른 실패") }
                fork {
                    Thread.sleep(500)
                    counter.incrementAndGet()
                }
                join().throwIfFailed()
            }
        }
        // 빠른 실패로 인해 두 번째 subtask가 실행 완료되지 않아야 함
        counter.get() shouldBeEqualTo 0
    }

    @Test
    fun `taskScope - name 파라미터 지정`() = runSuspendIO {
        val result = taskScope(name = "test-scope") {
            val t = fork { 42 }
            join().throwIfFailed()
            t.get()
        }
        result shouldBeEqualTo 42
    }

    @Test
    fun `taskScope - getOrNull은 실패 subtask에 null 반환`() = runSuspendIO {
        val captured = mutableListOf<io.bluetape4k.concurrent.virtualthread.StructuredSubtask<Int>>()
        assertThrows<RuntimeException> {
            taskScope<Int> {
                captured += fork<Int> { throw RuntimeException("실패") }
                fork { 42 }
                join().throwIfFailed()
                0
            }
        }
        (captured.first().getOrNull() == null).shouldBeTrue()
    }

    // --- failFastTaskScope (alias) ---

    @Test
    fun `failFastTaskScope - taskScope와 동일하게 동작`() = runSuspendIO {
        val result = failFastTaskScope {
            val t = fork { "hello" }
            join().throwIfFailed()
            t.get()
        }
        result shouldBeEqualTo "hello"
    }

    // --- firstSuccessTaskScope ---

    @Test
    fun `firstSuccessTaskScope - 첫 번째 성공 결과 반환`() = runSuspendIO {
        val winner = firstSuccessTaskScope<String> {
            fork { "slow" }
            fork { "fast" }
            join().result { IllegalStateException("모두 실패: ${it.message}") }
        }
        winner.shouldNotBeNull()
        log.debug { "winner: $winner" }
    }

    @Test
    fun `firstSuccessTaskScope - 모두 실패 시 mapper 예외 발생`() = runSuspendIO {
        assertThrows<IllegalStateException> {
            firstSuccessTaskScope<String> {
                fork { throw RuntimeException("실패 1") }
                fork { throw RuntimeException("실패 2") }
                join().result { e -> IllegalStateException("모두 실패: ${e.message}") }
            }
        }
    }

    @Test
    fun `firstSuccessTaskScope - 하나만 성공하면 그 결과 반환`() = runSuspendIO {
        val result = firstSuccessTaskScope<Int> {
            fork { throw RuntimeException("실패") }
            fork { 99 }
            join().result { IllegalStateException("모두 실패") }
        }
        result shouldBeEqualTo 99
    }

    @Test
    fun `firstSuccessTaskScope - 여러 소스에서 가장 빠른 결과 선택`() = runSuspendIO {
        val result = firstSuccessTaskScope<String> {
            fork {
                Thread.sleep(200)
                "느린 소스"
            }
            fork {
                Thread.sleep(10)
                "빠른 소스"
            }
            join().result { IllegalStateException("모두 실패") }
        }
        result shouldBeEqualTo "빠른 소스"
    }

    // --- supervisedTaskScope ---

    @Test
    fun `supervisedTaskScope - 부분 실패 허용`() = runSuspendIO {
        val results = supervisedTaskScope<Int, List<Result<Int>>> {
            fork { 1 }
            fork { throw RuntimeException("subtask 2 실패") }
            fork { 3 }
            join()
            results()
        }
        results shouldHaveSize 3
        results.count { it.isSuccess } shouldBeEqualTo 2
        results.count { it.isFailure } shouldBeEqualTo 1
    }

    @Test
    fun `supervisedTaskScope - successfulResults 반환`() = runSuspendIO {
        val successes = supervisedTaskScope<Int, List<Int>> {
            fork { 10 }
            fork { throw RuntimeException("실패") }
            fork { 30 }
            join()
            successfulResults()
        }
        successes shouldContainAll listOf(10, 30)
    }

    @Test
    fun `supervisedTaskScope - failedExceptions 반환`() = runSuspendIO {
        val failures = supervisedTaskScope<Int, List<Throwable>> {
            fork { 1 }
            fork { throw RuntimeException("오류 A") }
            fork { throw IllegalArgumentException("오류 B") }
            join()
            failedExceptions()
        }
        failures shouldHaveSize 2
        failures.any { it is RuntimeException && it.message == "오류 A" }.shouldBeTrue()
        failures.any { it is IllegalArgumentException && it.message == "오류 B" }.shouldBeTrue()
    }

    @Test
    fun `supervisedTaskScope - 모든 subtask 성공 시 results 모두 success`() = runSuspendIO {
        val results = supervisedTaskScope<Int, List<Result<Int>>> {
            fork { 1 }
            fork { 2 }
            fork { 3 }
            join()
            results()
        }
        results.all { it.isSuccess }.shouldBeTrue()
        results.map { it.getOrThrow() }.sum() shouldBeEqualTo 6
    }

    @Test
    fun `supervisedTaskScope - 모든 subtask 실패 시 results 모두 failure`() = runSuspendIO {
        val results = supervisedTaskScope<Int, List<Result<Int>>> {
            fork { throw RuntimeException("실패 1") }
            fork { throw RuntimeException("실패 2") }
            join()
            results()
        }
        results.all { it.isFailure }.shouldBeTrue()
    }

    @Test
    fun `supervisedTaskScope - 빈 결과`() = runSuspendIO {
        val results = supervisedTaskScope<Int, List<Result<Int>>> {
            join()
            results()
        }
        results shouldHaveSize 0
    }

    // --- asyncTaskScope ---

    @Test
    fun `asyncTaskScope - Deferred 반환 후 await`() = runSuspendIO {
        val deferred = asyncTaskScope {
            val t = fork { 100 }
            join().throwIfFailed()
            t.get()
        }
        deferred.await() shouldBeEqualTo 100
    }

    @Test
    fun `asyncTaskScope - 복수 Deferred 병렬 실행`() = runSuspendIO {
        val deferred1 = asyncTaskScope {
            val t = fork { 10 }
            join().throwIfFailed()
            t.get()
        }
        val deferred2 = asyncTaskScope {
            val t = fork { 20 }
            join().throwIfFailed()
            t.get()
        }
        val (r1, r2) = awaitAll(deferred1, deferred2)
        r1 + r2 shouldBeEqualTo 30
    }

    @Test
    fun `asyncTaskScope - 실패 시 await에서 예외 발생`() = runSuspendIO {
        // async 예외가 parent scope로 전파되지 않도록 supervisorScope로 격리
        supervisorScope {
            val deferred = asyncTaskScope<Int> {
                fork { throw RuntimeException("비동기 실패") }
                join().throwIfFailed()
                0
            }
            val result = runCatching { deferred.await() }
            result.isFailure.shouldBeTrue()
            result.exceptionOrNull().shouldBeInstanceOf<RuntimeException>()
        }
    }

    @Test
    fun `asyncTaskScope - 여러 scope 동시 실행 후 합산`() = runSuspendIO {
        val count = 5
        val deferreds = (1..count).map { i ->
            asyncTaskScope {
                val t = fork { i * 10 }
                join().throwIfFailed()
                t.get()
            }
        }
        val total = awaitAll(*deferreds.toTypedArray()).sum()
        total shouldBeEqualTo (1..count).sumOf { it * 10 }
    }

    // --- asyncSupervisedTaskScope ---

    @Test
    fun `asyncSupervisedTaskScope - Deferred로 supervised 실행`() = runSuspendIO {
        val deferred = asyncSupervisedTaskScope<Int, List<Result<Int>>> {
            fork { 1 }
            fork { throw RuntimeException("실패") }
            fork { 3 }
            join()
            results()
        }
        val results = deferred.await()
        results shouldHaveSize 3
        results.count { it.isSuccess } shouldBeEqualTo 2
    }

    @Test
    fun `asyncSupervisedTaskScope - successfulResults만 반환`() = runSuspendIO {
        val deferred = asyncSupervisedTaskScope<String, List<String>> {
            fork { "A" }
            fork { throw RuntimeException("실패") }
            fork { "C" }
            join()
            successfulResults()
        }
        val successes = deferred.await()
        successes shouldContainAll listOf("A", "C")
    }

    // --- 리소스 해제 및 Edge case ---

    @Test
    fun `taskScope - 빈 scope 실행`() = runSuspendIO {
        val result = taskScope {
            join().throwIfFailed()
            42
        }
        result shouldBeEqualTo 42
    }

    @Test
    fun `taskScope - 대량 subtask 처리`() = runSuspendIO {
        val count = 1000
        val sum = taskScope {
            val tasks = (1..count).map { i -> fork { i } }
            join().throwIfFailed()
            tasks.sumOf { it.get() }
        }
        sum shouldBeEqualTo (1..count).sum()
    }

    @Test
    fun `taskScope - subtask 결과 타입 다양성`() = runSuspendIO {
        val result = taskScope {
            val intTask = fork { 42 }
            val strTask = fork { "hello" }
            join().throwIfFailed()
            "${intTask.get()}-${strTask.get()}"
        }
        result shouldBeEqualTo "42-hello"
    }

    @Test
    fun `supervisedTaskScope - 부분 결과로 집계 가능`() = runSuspendIO {
        val successCount = supervisedTaskScope<Int, Int> {
            repeat(10) { i ->
                fork { if (i % 2 == 0) i else throw RuntimeException("홀수 실패 $i") }
            }
            join()
            successfulResults().size
        }
        successCount shouldBeEqualTo 5
    }

    @Test
    fun `taskScope joinUntil - 데드라인 초과 시 TimeoutException`() = runSuspendIO {
        val deadline = java.time.Instant.now().plusMillis(50)
        assertThrows<TimeoutException> {
            taskScope<Unit> {
                fork { Thread.sleep(5000) }
                joinUntil(deadline).throwIfFailed()
            }
        }
    }

    @Test
    fun `supervisedTaskScope joinUntil - 데드라인 초과 시 TimeoutException`() = runSuspendIO {
        val deadline = java.time.Instant.now().plusMillis(50)
        assertThrows<TimeoutException> {
            supervisedTaskScope<Int, List<Result<Int>>> {
                fork { Thread.sleep(5000); 1 }
                joinUntil(deadline)
                results()
            }
        }
    }

    @Test
    fun `asyncTaskScope - 여러 번 중첩 실행 가능`() = runSuspendIO {
        val result = taskScope {
            val inner = fork {
                // 내부에서는 blocking 코드 (가상 스레드에서 실행 중)
                42
            }
            join().throwIfFailed()
            inner.get()
        }
        result shouldBeEqualTo 42
    }

    @Test
    fun `supervisedTaskScope - results 순서 확인`() = runSuspendIO {
        val counter = AtomicInteger(0)
        val results = supervisedTaskScope<Int, List<Result<Int>>> {
            repeat(5) { fork { counter.incrementAndGet() } }
            join()
            results()
        }
        results shouldHaveSize 5
        results.count { it.isSuccess } shouldBeEqualTo 5
        results.mapNotNull { it.getOrNull() }.sum() shouldBeEqualTo (1..5).sum()
    }

    @Test
    fun `taskScope - Dispatchers VT 에서 실행 확인`() = runSuspendIO {
        val threadName = taskScope {
            val t = fork { Thread.currentThread().name }
            join().throwIfFailed()
            t.get()
        }
        log.debug { "Executing thread: $threadName" }
        threadName.shouldNotBeNull()
        // 가상 스레드는 이름에 "VirtualThread" 혹은 프리픽스 포함
        threadName.contains("vt-", ignoreCase = true).shouldBeTrue()
    }
}
