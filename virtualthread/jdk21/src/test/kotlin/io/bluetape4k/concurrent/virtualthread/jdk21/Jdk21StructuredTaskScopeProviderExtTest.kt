package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeBlank
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.JRE
import org.junit.jupiter.api.condition.EnabledForJreRange
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.TimeUnit

/**
 * [Jdk21StructuredTaskScopeProvider] 추가 커버리지 테스트입니다.
 * - providerName, priority, isSupported 검증
 * - Jdk21Subtask 상태 (SUCCESS / FAILED) 검증
 * - joinUntil 검증
 * - withAll name 파라미터 검증
 * - withAny all-failure 검증
 */
@EnabledForJreRange(min = JRE.JAVA_21)
class Jdk21StructuredTaskScopeProviderExtTest {

    companion object: KLoggingChannel()

    private val provider = Jdk21StructuredTaskScopeProvider()

    @Test
    fun `providerName 이 올바른 값이어야 한다`() {
        provider.providerName shouldBeEqualTo Jdk21StructuredTaskScopeProvider.PROVIDER_NAME
        provider.providerName.shouldNotBeBlank()
    }

    @Test
    fun `priority 가 21 이어야 한다`() {
        provider.priority shouldBeEqualTo Jdk21StructuredTaskScopeProvider.PRIORITY
        provider.priority shouldBeEqualTo 21
    }

    @Test
    fun `isSupported 가 true 여야 한다`() {
        provider.isSupported().shouldBeTrue()
    }

    @Test
    fun `withAll scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = provider.withAll(name = "jdk21-test-scope") { scope ->
            val task = scope.fork { 100 }
            scope.join().throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 100
    }

    @Test
    fun `withAll throwIfFailed 핸들러가 호출되어야 한다`() {
        var handlerCalled = false
        assertFailsWith<RuntimeException> {
            provider.withAll { scope ->
                scope.fork<Int> { throw RuntimeException("error") }
                scope.join().throwIfFailed { handlerCalled = true }
            }
        }
        handlerCalled.shouldBeTrue()
    }

    @Test
    fun `withAll joinUntil 이 정상 동작해야 한다`() {
        val result = provider.withAll { scope ->
            val task = scope.fork { 55 }
            scope.joinUntil(Instant.now().plusSeconds(5)).throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 55
    }

    @Test
    fun `withAll join은 기존 owner interrupt를 즉시 전파해야 한다`() {
        try {
            assertFailsWith<InterruptedException> {
                provider.withAll { scope ->
                    scope.fork { Thread.sleep(5_000); 1 }
                    Thread.currentThread().interrupt()
                    scope.join()
                }
            }
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `withAll joinUntil은 외부 owner interrupt를 즉시 전파해야 한다`() {
        val ownerThread = Thread.currentThread()
        val interrupter = Executors.newSingleThreadScheduledExecutor()
        try {
            assertFailsWith<InterruptedException> {
                provider.withAll { scope ->
                    scope.fork { Thread.sleep(5_000); 1 }
                    interrupter.schedule({ ownerThread.interrupt() }, 100, TimeUnit.MILLISECONDS)
                    scope.joinUntil(Instant.now().plusSeconds(5))
                }
            }
        } finally {
            interrupter.shutdownNow()
            Thread.interrupted()
        }
    }

    @Test
    fun `subtask 성공 상태와 값을 확인해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.api.StructuredSubtask<Int>? = null
        provider.withAll { scope ->
            capturedSubtask = scope.fork { 77 }
            scope.join().throwIfFailed()
        }

        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.get() shouldBeEqualTo 77
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        subtask.exceptionOrNull().shouldBeNull()
    }

    @Test
    fun `subtask 실패 상태에서 exceptionOrNull 이 예외를 반환해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.api.StructuredSubtask<Int>? = null
        assertFailsWith<RuntimeException> {
            provider.withAll { scope ->
                capturedSubtask = scope.fork<Int> { throw RuntimeException("fail") }
                scope.join().throwIfFailed()
            }
        }
        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.FAILED
        subtask.exceptionOrNull().shouldNotBeNull().shouldBeInstanceOf<RuntimeException>()
    }

    @Test
    fun `withAny scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = provider.withAny<String>(name = "jdk21-any-scope") { scope ->
            scope.fork { "only-task" }
            scope.join().result { IllegalStateException(it) }
        }
        result shouldBeEqualTo "only-task"
    }

    @Test
    fun `withAny 모든 subtask 실패 시 mapper 예외가 발생해야 한다`() {
        assertFailsWith<IllegalStateException> {
            provider.withAny<String> { scope ->
                scope.fork<String> { throw RuntimeException("fail1") }
                scope.fork<String> { throw RuntimeException("fail2") }
                scope.join().result { IllegalStateException("all failed") }
            }
        }
    }

    @Test
    fun `withAny result는 owner interrupt를 mapper로 변환하지 않아야 한다`() {
        var mapperCalled = false
        try {
            assertFailsWith<InterruptedException> {
                provider.withAny<Int> { scope ->
                    scope.fork { Thread.sleep(5_000); 1 }
                    Thread.currentThread().interrupt()
                    scope.join().result {
                        mapperCalled = true
                        IllegalStateException(it)
                    }
                }
            }
            mapperCalled.shouldBeFalse()
        } finally {
            Thread.interrupted()
        }
    }

    @Test
    fun `withAny subtask 성공 상태를 확인해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.api.StructuredSubtask<String>? = null
        provider.withAny<String> { scope ->
            capturedSubtask = scope.fork { "winner" }
            scope.join().result { IllegalStateException(it) }
        }
        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        subtask.get() shouldBeEqualTo "winner"
        subtask.exceptionOrNull().shouldBeNull()
    }
}
