package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeBlank
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.StructuredTaskScope
import kotlin.test.assertFailsWith

/**
 * [StructuredTaskScopes], [StructuredTaskScopeProvider], [StructuredSubtask], [StructuredTaskScopeAll],
 * [StructuredTaskScopeAny] 인터페이스 및 기본 구현 커버리지 테스트입니다.
 */
class StructuredScopesTest {

    companion object: KLogging()

    // StructuredTaskScopes.provider() 는 jdk21/jdk25 provider 가 classpath 에 없을 때 ISE 를 던지지만,
    // test 런타임에는 jdk21 provider 가 등록되어 있으므로 정상 반환됩니다.
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
        (provider.priority > 0).shouldBeTrue()
    }

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
        // StructuredTaskScopeAll.joinUntil 기본 구현 (interface default) 테스트
        // 직접 mock 구현체를 만들어 joinUntil 이 join 위임을 검증
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

        // joinUntil 기본 구현은 join() 을 호출한다
        scope.joinUntil(java.time.Instant.now().plusSeconds(5))
        joinCalled.shouldBeTrue()
    }

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

    @Test
    fun `all scope name 파라미터를 지정해도 정상 동작해야 한다`() {
        val result = StructuredTaskScopes.all(name = "test-scope", factory = Thread.ofVirtual().factory()) { scope ->
            val task = scope.fork { 42 }
            scope.join().throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 42
    }

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
}
