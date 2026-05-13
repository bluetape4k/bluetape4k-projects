package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.lang.ScopedValue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * [TaskContext] — ScopedValue 기반 컨텍스트 전파 테스트
 */
class TaskContextTest {

    companion object : KLogging()

    // ── 키 생성 ───────────────────────────────────────────────────────────────

    @Test
    fun `newKey 는 새로운 ScopedValue 인스턴스를 반환한다`() {
        val key1: ScopedValue<String> = TaskContext.newKey()
        val key2: ScopedValue<String> = TaskContext.newKey()
        (key1 !== key2).shouldBeTrue()
    }

    // ── 단일 바인딩 ───────────────────────────────────────────────────────────

    @Test
    fun `run 으로 단일 키 바인딩 후 get 으로 값을 조회한다`() {
        val key = TaskContext.newKey<String>()

        val result = TaskContext.run(key, "req-001") {
            TaskContext.get(key)
        }

        result shouldBeEqualTo "req-001"
    }

    @Test
    fun `run 블록 외부에서 get 은 null 을 반환한다`() {
        val key = TaskContext.newKey<String>()
        TaskContext.get(key).shouldBeNull()
    }

    @Test
    fun `isBound 는 바인딩 스코프 내에서 true 를 반환한다`() {
        val key = TaskContext.newKey<Int>()

        TaskContext.isBound(key).shouldBeFalse()

        TaskContext.run(key, 42) {
            TaskContext.isBound(key).shouldBeTrue()
        }

        TaskContext.isBound(key).shouldBeFalse()
    }

    @Test
    fun `getOrDefault 는 바인딩되지 않은 경우 기본값을 반환한다`() {
        val key = TaskContext.newKey<String>()

        val value = TaskContext.getOrDefault(key, "default")
        value shouldBeEqualTo "default"
    }

    @Test
    fun `getOrDefault 는 바인딩된 경우 바인딩값을 반환한다`() {
        val key = TaskContext.newKey<String>()

        val value = TaskContext.run(key, "bound") {
            TaskContext.getOrDefault(key, "default")
        }

        value shouldBeEqualTo "bound"
    }

    @Test
    fun `run 블록의 반환값이 올바르게 전달된다`() {
        val key = TaskContext.newKey<Int>()

        val result = TaskContext.run(key, 10) {
            TaskContext.getOrDefault(key, 0) * 2
        }

        result shouldBeEqualTo 20
    }

    // ── 다중 바인딩 ───────────────────────────────────────────────────────────

    @Test
    fun `bind-and 체이닝으로 여러 키를 동시에 바인딩한다`() {
        val requestId = TaskContext.newKey<String>()
        val tenantId = TaskContext.newKey<String>()

        TaskContext.bind(requestId, "req-001")
            .and(tenantId, "tenant-42")
            .run {
                TaskContext.get(requestId) shouldBeEqualTo "req-001"
                TaskContext.get(tenantId) shouldBeEqualTo "tenant-42"
            }
    }

    @Test
    fun `bind-and-call 체이닝으로 결과를 반환한다`() {
        val key1 = TaskContext.newKey<Int>()
        val key2 = TaskContext.newKey<Int>()

        val result = TaskContext.bind(key1, 10)
            .and(key2, 20)
            .call {
                TaskContext.getOrDefault(key1, 0) + TaskContext.getOrDefault(key2, 0)
            }

        result shouldBeEqualTo 30
    }

    @Test
    fun `중첩 바인딩에서 내부 스코프가 외부 스코프를 섀도잉한다`() {
        val key = TaskContext.newKey<String>()

        TaskContext.run(key, "outer") {
            TaskContext.get(key) shouldBeEqualTo "outer"

            TaskContext.run(key, "inner") {
                TaskContext.get(key) shouldBeEqualTo "inner"
            }

            // 내부 스코프 종료 후 외부값 복원
            TaskContext.get(key) shouldBeEqualTo "outer"
        }
    }

    // ── Virtual Thread forked subtask 자동 전파 ───────────────────────────────

    @Test
    fun `일반 Virtual Thread 는 ScopedValue 를 상속하지 않는다`() {
        val requestId = TaskContext.newKey<String>()
        val collected = CopyOnWriteArrayList<String?>()

        TaskContext.run(requestId, "req-vthread") {
            // Thread.ofVirtual()로 직접 생성한 스레드는 부모의 ScopedValue 바인딩을 상속하지 않음
            val threads = (1..4).map {
                Thread.ofVirtual().start {
                    collected.add(TaskContext.get(requestId))  // null 반환 예상
                }
            }
            threads.forEach { it.join() }
        }

        collected.size shouldBeEqualTo 4
        collected.all { it == null }.shouldBeTrue()
    }

    @Test
    fun `StructuredTaskScope fork 로 생성된 Virtual Thread 는 ScopedValue 를 자동 상속한다`() {
        val requestId = TaskContext.newKey<String>()
        val collected = CopyOnWriteArrayList<String>()

        TaskContext.run(requestId, "req-vthread") {
            StructuredTaskScopes.failFast { scope ->
                val subtasks = (1..4).map { i ->
                    scope.fork {
                        val value = TaskContext.get(requestId)
                        log.debug { "Subtask $i 수신: $value" }
                        value?.let { collected.add(it) }
                        value
                    }
                }
                scope.join().throwIfFailed()
                subtasks.map { it.get() }
            }
        }

        collected.size shouldBeEqualTo 4
        collected.all { it == "req-vthread" }.shouldBeTrue()
    }

    @Test
    fun `StructuredTaskScopes fork 에서 ScopedValue 가 자동 전파된다`() {
        val requestId = TaskContext.newKey<String>()
        val tenantId = TaskContext.newKey<String>()

        val results = TaskContext.bind(requestId, "req-777")
            .and(tenantId, "tenant-99")
            .call {
                StructuredTaskScopes.failFast { scope ->
                    val a = scope.fork { TaskContext.get(requestId) }
                    val b = scope.fork { TaskContext.get(tenantId) }
                    scope.join().throwIfFailed()
                    listOf(a.get(), b.get())
                }
            }

        results[0] shouldBeEqualTo "req-777"
        results[1] shouldBeEqualTo "tenant-99"
    }

    @Test
    fun `StructuredTaskScopeTester 반복 실행에서도 ScopedValue 가 자동 전파된다`() {
        val requestId = TaskContext.newKey<String>()
        val completed = AtomicInteger()

        TaskContext.run(requestId, "req-stress") {
            StructuredTaskScopeTester()
                .rounds(32)
                .add {
                    TaskContext.get(requestId) shouldBeEqualTo "req-stress"
                    completed.incrementAndGet()
                }
                .run()
        }

        completed.get() shouldBeEqualTo 32
        TaskContext.get(requestId).shouldBeNull()
    }

    @Test
    fun `supervised scope fork 에서 ScopedValue 가 자동 전파된다`() {
        val traceId = TaskContext.newKey<String>()

        val taskResults = TaskContext.run(traceId, "trace-abc") {
            StructuredTaskScopes.supervised<String, List<Result<String>>> { scope ->
                scope.fork { TaskContext.get(traceId) ?: "NOT_FOUND" }
                scope.fork { TaskContext.get(traceId) ?: "NOT_FOUND" }
                scope.join()
                scope.results()
            }
        }

        taskResults.size shouldBeEqualTo 2
        taskResults.all { it.isSuccess }.shouldBeTrue()
        taskResults.all { it.getOrThrow() == "trace-abc" }.shouldBeTrue()
    }

    // ── Nullable T ────────────────────────────────────────────────────────────

    @Test
    @EnabledForJreRange(max = JRE.JAVA_24)
    fun `ScopedValue 에 null 바인딩 후 get 은 null 을 반환한다 (JDK 21)`() {
        // JDK 21 preview API: ScopedValue.where(key, null) 허용; JDK 25에서는 NPE (스펙 강화)
        val key = TaskContext.newKey<String?>()
        TaskContext.run(key, null) {
            TaskContext.get(key).shouldBeNull()
        }
    }

    // ── 예외 발생 시 바인딩 해제 ─────────────────────────────────────────────

    @Test
    fun `run 블록에서 예외 발생 시 바인딩이 해제된다`() {
        val key = TaskContext.newKey<String>()
        assertFailsWith<RuntimeException> {
            TaskContext.run(key, "x") { throw RuntimeException("boom") }
        }
        TaskContext.get(key).shouldBeNull()
    }

    @Test
    fun `TaskContextBindings call 블록에서 예외 발생 시 바인딩이 해제된다`() {
        val key = TaskContext.newKey<String>()
        assertFailsWith<RuntimeException> {
            TaskContext.bind(key, "x").call { throw RuntimeException("boom") }
        }
        TaskContext.get(key).shouldBeNull()
    }

    @Test
    fun `withTaskContext 블록에서 예외 발생 시 바인딩이 해제된다`() {
        val key = TaskContext.newKey<String>()
        assertFailsWith<RuntimeException> {
            withTaskContext(key, "x") { throw RuntimeException("boom") }
        }
        TaskContext.get(key).shouldBeNull()
    }

    // ── 스코프 격리 ───────────────────────────────────────────────────────────

    @Test
    fun `서로 다른 바인딩 스코프는 독립적으로 동작한다`() {
        val key = TaskContext.newKey<String>()

        var scope1Value: String? = null
        var scope2Value: String? = null

        val t1 = Thread.ofVirtual().start {
            TaskContext.run(key, "scope-1") {
                Thread.sleep(50)
                scope1Value = TaskContext.get(key)
            }
        }

        val t2 = Thread.ofVirtual().start {
            TaskContext.run(key, "scope-2") {
                Thread.sleep(50)
                scope2Value = TaskContext.get(key)
            }
        }

        t1.join()
        t2.join()

        scope1Value shouldBeEqualTo "scope-1"
        scope2Value shouldBeEqualTo "scope-2"
    }

    // ── withTaskContext top-level 함수 ────────────────────────────────────────

    @Test
    fun `withTaskContext 는 TaskContext run 과 동일하게 동작한다`() {
        val key = TaskContext.newKey<String>()

        val result = withTaskContext(key, "top-level") {
            TaskContext.get(key)
        }

        result shouldBeEqualTo "top-level"
    }

    @Test
    fun `withTaskContext 블록 내부에서 StructuredTaskScope fork 에 자동 전파된다`() {
        val requestId = TaskContext.newKey<String>()

        val values = withTaskContext(requestId, "req-tl") {
            StructuredTaskScopes.failFast { scope ->
                val a = scope.fork { TaskContext.get(requestId) }
                val b = scope.fork { TaskContext.get(requestId) }
                scope.join().throwIfFailed()
                listOf(a.get(), b.get())
            }
        }

        values.all { it == "req-tl" }.shouldBeTrue()
    }

    // ── TaskContextBindings ───────────────────────────────────────────────────

    @Test
    fun `TaskContextBindings run 블록 내부에서 모든 바인딩을 조회할 수 있다`() {
        val a = TaskContext.newKey<Int>()
        val b = TaskContext.newKey<Int>()
        val c = TaskContext.newKey<Int>()

        var sumInside = 0

        TaskContext.bind(a, 1)
            .and(b, 2)
            .and(c, 3)
            .run {
                sumInside = TaskContext.getOrDefault(a, 0) + TaskContext.getOrDefault(b, 0) + TaskContext.getOrDefault(c, 0)
            }

        sumInside shouldBeEqualTo 6
    }

    @Test
    fun `TaskContextBindings run 블록 종료 후 바인딩이 해제된다`() {
        val key = TaskContext.newKey<String>()

        TaskContext.bind(key, "bound").run {
            TaskContext.get(key).shouldNotBeNull()
        }

        TaskContext.get(key).shouldBeNull()
    }
}
