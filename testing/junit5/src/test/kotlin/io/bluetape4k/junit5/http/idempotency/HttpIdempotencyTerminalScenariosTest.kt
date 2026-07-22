package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test

class HttpIdempotencyTerminalScenariosTest {

    @Test
    fun `terminal scenario group preserves one execution and security scope isolation`() = runSuspendIO {
        val limits = config()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        runConformanceScenarios(adapter, limits, terminalScenarios())

        adapter.completedScenarioCount shouldBeEqualTo 6
        adapter.maximumObservedWaiters shouldBeEqualTo limits.maxWaitersPerKey
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `terminal failure diagnostics redact scope key and body across suppressed cleanup`() = runSuspendIO {
        val limits = config()
        val delegate = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)
        val adapter = object: BoundedWaitHttpIdempotencyAdapter by delegate {
            override suspend fun resetScenario() {
                delegate.resetScenario()
                throw IllegalStateException("cleanup-redacted")
            }
        }

        val failure = assertFailsWith<AssertionError> {
            runConformanceScenarios(
                adapter,
                limits,
                listOf(
                    ConformanceScenario("redacted-diagnostics") { _, _ ->
                        request(
                            authenticationProfile = "scope-secret",
                            idempotencyKeys = listOf("key-secret"),
                            requestBody = "body-secret",
                        ) shouldBeEqualTo request(
                            authenticationProfile = "other-scope-secret",
                            idempotencyKeys = listOf("other-key-secret"),
                            requestBody = "other-body-secret",
                        )
                    },
                ),
            )
        }

        failure.suppressed.shouldNotBeEmpty()
        throwableText(failure).also { text ->
            listOf(
                "scope-secret",
                "key-secret",
                "body-secret",
                "other-scope-secret",
                "other-key-secret",
                "other-body-secret",
            ).forEach { sentinel -> text shouldNotContain sentinel }
        }
    }

    private fun throwableText(failure: Throwable): String = buildString {
        val visited = mutableSetOf<Throwable>()
        fun appendFailure(current: Throwable?) {
            if (current == null || !visited.add(current)) return
            append(current.message.orEmpty())
            current.suppressed.forEach(::appendFailure)
            appendFailure(current.cause)
        }
        appendFailure(failure)
    }
}
