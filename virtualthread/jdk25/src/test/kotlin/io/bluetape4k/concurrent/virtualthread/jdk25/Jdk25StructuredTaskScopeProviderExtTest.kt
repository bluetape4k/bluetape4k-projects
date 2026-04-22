package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.time.Instant
import java.util.concurrent.StructuredTaskScope
import kotlin.test.assertFailsWith

/**
 * [Jdk25StructuredTaskScopeProvider] 추가 커버리지 테스트입니다.
 * - providerName, priority, isSupported 검증
 * - AllScope/AnyScope 추가 시나리오 (name, joinUntil, subtask 상태)
 * - Jdk25Subtask 상태 검증
 */
@EnabledOnJre(JRE.JAVA_25)
class Jdk25StructuredTaskScopeProviderExtTest {

    companion object: KLoggingChannel()

    private val provider = Jdk25StructuredTaskScopeProvider()

    @Test
    fun `providerName 이 올바른 값이어야 한다`() {
        provider.providerName shouldBeEqualTo Jdk25StructuredTaskScopeProvider.PROVIDER_NAME
        provider.providerName.shouldNotBeBlank()
    }

    @Test
    fun `priority 가 25 여야 한다`() {
        provider.priority shouldBeEqualTo Jdk25StructuredTaskScopeProvider.PRIORITY
        provider.priority shouldBeEqualTo 25
    }

    @Test
    fun `isSupported 가 true 여야 한다`() {
        provider.isSupported().shouldBeTrue()
    }

    @Test
    fun `withAll scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = provider.withAll(name = "jdk25-test-scope") { scope ->
            val task = scope.fork { 200 }
            scope.join().throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 200
    }

    @Test
    fun `withAll throwIfFailed 핸들러가 호출되어야 한다`() {
        var handlerCalled = false
        assertFailsWith<RuntimeException> {
            provider.withAll { scope ->
                scope.fork<Int> { throw RuntimeException("jdk25-error") }
                scope.join().throwIfFailed { handlerCalled = true }
                0
            }
        }
        handlerCalled.shouldBeTrue()
    }

    @Test
    fun `withAll joinUntil 미래 데드라인으로 정상 완료해야 한다`() {
        val result = provider.withAll { scope ->
            val task = scope.fork { 66 }
            scope.joinUntil(Instant.now().plusSeconds(5)).throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 66
    }

    @Test
    fun `withAll joinUntil 이미 지난 데드라인에서 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<java.util.concurrent.TimeoutException> {
            provider.withAll { scope ->
                scope.fork { Thread.sleep(500); 1 }
                scope.joinUntil(Instant.now().minusSeconds(1))
                scope.throwIfFailed()
            }
        }
    }

    @Test
    fun `subtask 성공 상태와 값을 확인해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.StructuredSubtask<Int>? = null
        provider.withAll { scope ->
            capturedSubtask = scope.fork { 88 }
            scope.join().throwIfFailed()
        }

        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.get() shouldBeEqualTo 88
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        subtask.exceptionOrNull().shouldBeNull()
    }

    @Test
    fun `subtask 실패 상태에서 exceptionOrNull 이 예외를 반환해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.StructuredSubtask<Int>? = null
        assertFailsWith<RuntimeException> {
            provider.withAll { scope ->
                capturedSubtask = scope.fork<Int> { throw RuntimeException("jdk25-fail") }
                scope.join().throwIfFailed()
                0
            }
        }
        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.FAILED
        subtask.exceptionOrNull().shouldNotBeNull().shouldBeInstanceOf<RuntimeException>()
    }

    @Test
    fun `withAny scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = provider.withAny<String>(name = "jdk25-any-scope") { scope ->
            scope.fork { "single-task" }
            scope.join().result { IllegalStateException(it) }
        }
        result shouldBeEqualTo "single-task"
    }

    @Test
    fun `withAny 모든 subtask 실패 시 mapper 예외가 발생해야 한다`() {
        assertFailsWith<IllegalStateException> {
            provider.withAny<String> { scope ->
                scope.fork<String> { throw RuntimeException("fail-a") }
                scope.fork<String> { throw RuntimeException("fail-b") }
                scope.join().result { IllegalStateException("all failed: $it") }
            }
        }
    }

    @Test
    fun `withAny join 없이 result 호출해도 동작해야 한다`() {
        // join() 없이 result() 직접 호출하는 경우도 커버
        val result = provider.withAny<Int> { scope ->
            scope.fork { 123 }
            // join() 생략, result() 내부에서 join 수행
            scope.result { IllegalStateException(it) }
        }
        result shouldBeEqualTo 123
    }

    @Test
    fun `withAny subtask 성공 상태를 확인해야 한다`() {
        var capturedSubtask: io.bluetape4k.concurrent.virtualthread.StructuredSubtask<String>? = null
        provider.withAny<String> { scope ->
            capturedSubtask = scope.fork { "jdk25-winner" }
            scope.join().result { IllegalStateException(it) }
        }
        val subtask = capturedSubtask.shouldNotBeNull()
        subtask.state() shouldBeEqualTo StructuredTaskScope.Subtask.State.SUCCESS
        subtask.get() shouldBeEqualTo "jdk25-winner"
        subtask.exceptionOrNull().shouldBeNull()
    }

    @Test
    fun `withAll 여러 subtask 합산이 정확해야 한다`() {
        val result = provider.withAll { scope ->
            val tasks = (1..5).map { n -> scope.fork { n * n } }
            scope.join().throwIfFailed()
            tasks.sumOf { it.get() }
        }
        // 1+4+9+16+25 = 55
        result shouldBeEqualTo 55
    }
}
