package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.concurrent.virtualthread.TaskContext
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.CopyOnWriteArrayList

/**
 * JDK 25 (stable ScopedValue API) 환경에서 [TaskContext] ScopedValue 전파 통합 테스트
 */
@EnabledForJreRange(min = JRE.JAVA_25)
class Jdk25TaskContextTest {

    companion object : KLogging()

    private val provider = Jdk25StructuredTaskScopeProvider()

    @Test
    fun `jdk25 withSupervised fork 에서 ScopedValue 가 자동 전파된다`() {
        val requestId = TaskContext.newKey<String>()
        val tenantId = TaskContext.newKey<String>()

        val results = TaskContext.bind(requestId, "req-jdk25")
            .and(tenantId, "tenant-25")
            .call {
                provider.withSupervised<String, List<String>> { scope ->
                    repeat(3) { i ->
                        scope.fork {
                            val rid = TaskContext.get(requestId)
                            val tid = TaskContext.get(tenantId)
                            log.debug { "Subtask $i: requestId=$rid, tenantId=$tid" }
                            "$rid:$tid"
                        }
                    }
                    scope.join()
                    scope.successfulResults()
                }
            }

        results.size shouldBeEqualTo 3
        results.all { it == "req-jdk25:tenant-25" }.shouldBeTrue()
    }

    @Test
    fun `jdk25 withFailFast fork 에서 ScopedValue 가 자동 전파된다`() {
        val traceId = TaskContext.newKey<String>()
        val collected = CopyOnWriteArrayList<String>()

        TaskContext.run(traceId, "trace-jdk25") {
            provider.withFailFast { scope ->
                repeat(4) { i ->
                    scope.fork {
                        val value = TaskContext.get(traceId)
                        log.debug { "Subtask $i: traceId=$value" }
                        value?.also { collected.add(it) }
                    }
                }
                scope.join().throwIfFailed()
            }
        }

        collected.size shouldBeEqualTo 4
        collected.all { it == "trace-jdk25" }.shouldBeTrue()
    }

    @Test
    fun `jdk25 supervised scope results 에서 ScopedValue 기반 컨텍스트가 Result 로 수집된다`() {
        val requestId = TaskContext.newKey<String>()

        val results = TaskContext.run(requestId, "req-result") {
            provider.withSupervised<String, List<Result<String>>> { scope ->
                scope.fork { TaskContext.get(requestId) ?: "NOT_FOUND" }
                scope.fork { TaskContext.get(requestId) ?: "NOT_FOUND" }
                scope.fork { throw RuntimeException("intentional failure") }
                scope.join()
                scope.results()
            }
        }

        results.size shouldBeEqualTo 3
        results.count { it.isSuccess } shouldBeEqualTo 2
        results.count { it.isFailure } shouldBeEqualTo 1
        results.filter { it.isSuccess }.all { it.getOrThrow() == "req-result" }.shouldBeTrue()
    }

    @Test
    fun `jdk25 ScopedValue null 바인딩이 허용된다 (JDK 25)`() {
        // JDK 25 stable API: ScopedValue.where(key, null) 은 NPE 없이 성공하며 null 값이 바인딩된다
        val key = TaskContext.newKey<String?>()
        TaskContext.run(key, null) {
            TaskContext.get(key).shouldBeNull()
        }
    }
}
