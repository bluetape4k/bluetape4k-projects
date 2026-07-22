package io.bluetape4k.junit5.http.idempotency

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.junit5.coroutines.runSuspendIO
import org.junit.jupiter.api.Test

class HttpIdempotencyBoundaryScenariosTest {

    /**
     * Uses a custom barrier-aligned harness because `SuspendedJobTester` does not expose each
     * attempt's result together with the per-key waiter-registration barrier. The bounded workload
     * is five rounds, three keys per round, and at most 105 concurrent requests per round.
     */
    @Test
    fun `boundary scenarios reject unsafe snapshots and elect one owner after expiry`() = runSuspendIO {
        val limits = boundaryConfig()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        runConformanceScenarios(adapter, limits, boundaryScenarios())

        adapter.completedScenarioCount shouldBeEqualTo 4
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
        adapter.persistedReplaySnapshotCount shouldBeGreaterThan 0
        adapter.unsafeReplayPersistCount shouldBeEqualTo 0
    }

    @Test
    fun `public entrypoint executes exactly seventeen isolated scenarios`() = runSuspendIO {
        val limits = boundaryConfig()
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        assertBoundedWaitHttpIdempotencyConformance(adapter, limits)

        adapter.completedScenarioCount shouldBeEqualTo 17
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    @Test
    fun `legal intrinsic replay maxima do not manufacture unrepresentable overflow values`() = runSuspendIO {
        val limits = config(
            maxReplayBodyBytes = 16_777_216,
            maxReplayHeaderNames = 100,
            maxReplayValuesPerHeader = 100,
            maxReplayHeaderValueBytes = 65_536,
            maxReplayHeaderBytes = 1_048_576,
            replayHeaderAllowlist = setOf("etag"),
        )
        val adapter = InMemoryBoundedWaitHttpIdempotencyAdapter(limits)

        val replayBounds = boundaryScenarios().single { scenario -> scenario.name == "replay-snapshot-bounds" }
        runConformanceScenarios(adapter, limits, listOf(replayBounds))

        adapter.completedScenarioCount shouldBeEqualTo 1
        adapter.quiescence() shouldBeEqualTo HttpIdempotencyQuiescence(0, 0, 0)
    }

    private fun boundaryConfig(): BoundedWaitHttpIdempotencyConformanceConfig = config(
        maxRequestBodyBytes = 64,
        replayHeaderAllowlist = buildSet {
            add("etag")
            add("authorization")
            add("cookie")
            add("set-cookie")
            add("proxy-authorization")
            add("www-authenticate")
            add("connection")
            add("x-hop")
            add("x-api-key")
            add("authentication-info")
            add("proxy-authenticate")
            add("proxy-authentication-info")
            add("keep-alive")
            add("te")
            add("trailer")
            add("transfer-encoding")
            add("upgrade")
            add("x-credential")
            add("x-secret")
            add("session-token")
            add("client-api-key")
            repeat(8) { index -> add("x-safe-$index") }
        },
    )
}
