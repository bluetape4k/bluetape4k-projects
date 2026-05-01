package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.concurrent.virtualthread.TaskContext
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE

/**
 * JDK 21 (`--enable-preview`) 환경에서 [TaskContext] ScopedValue 전파 통합 테스트
 */
@EnabledForJreRange(min = JRE.JAVA_21, max = JRE.JAVA_21)
class Jdk21TaskContextTest {

    companion object : KLogging()

    private val provider = Jdk21StructuredTaskScopeProvider()

    @Test
    fun `jdk21 withSupervised fork 에서 ScopedValue 가 자동 전파된다`() {
        val requestId = TaskContext.newKey<String>()
        val tenantId = TaskContext.newKey<String>()

        val results = TaskContext.bind(requestId, "req-jdk21")
            .and(tenantId, "tenant-21")
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
        results.all { it == "req-jdk21:tenant-21" }.shouldBeTrue()
    }

    @Test
    fun `jdk21 withFailFast fork 에서 ScopedValue 가 자동 전파된다`() {
        val traceId = TaskContext.newKey<String>()

        val values = TaskContext.run(traceId, "trace-jdk21") {
            provider.withFailFast { scope ->
                val subtasks = (1..4).map { i ->
                    scope.fork {
                        val value = TaskContext.get(traceId)
                        log.debug { "Subtask $i: traceId=$value" }
                        value ?: ""
                    }
                }
                scope.join().throwIfFailed()
                subtasks.map { it.get() }
            }
        }

        values.size shouldBeEqualTo 4
        values.all { it == "trace-jdk21" }.shouldBeTrue()
    }

    @Test
    fun `jdk21 다중 바인딩이 중첩 supervised scope 에서도 격리된다`() {
        val key = TaskContext.newKey<String>()

        val results = TaskContext.run(key, "outer") {
            provider.withSupervised<String, List<String>> { scope ->
                scope.fork { TaskContext.run(key, "inner-1") { TaskContext.get(key) ?: "" } }
                scope.fork { TaskContext.run(key, "inner-2") { TaskContext.get(key) ?: "" } }
                scope.join()
                scope.successfulResults()
            }
        }

        results[0] shouldBeEqualTo "inner-1"
        results[1] shouldBeEqualTo "inner-2"
    }
}
