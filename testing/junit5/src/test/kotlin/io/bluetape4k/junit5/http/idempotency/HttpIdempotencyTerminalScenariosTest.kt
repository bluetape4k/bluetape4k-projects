package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test

class HttpIdempotencyTerminalScenariosTest {

    @Test
    fun `terminal scenario group preserves one execution and security scope isolation`() = runSuspendIO {
        val limits = config()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        runConformanceScenarios(adapter, limits, terminalScenarios())

        adapter.completedScenarioCount shouldBeEqualTo 6
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }
}
