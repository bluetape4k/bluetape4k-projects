package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.time.Duration

class SynchronizerTypesTest {

    @Test
    fun `expirable handle preserves one identity and deadline per permit`() {
        val permit = PermitHandle(
            objectFingerprint = "fingerprint",
            ownerId = SemaphoreOwnerId.from("owner"),
            generation = 1,
            requestId = SemaphoreRequestId.from("request"),
            permits = 3,
            token = "allocation",
        )
        val leases = listOf(
            ExpirablePermitLease("permit-1", 1_000),
            ExpirablePermitLease("permit-2", 1_001),
            ExpirablePermitLease("permit-3", 1_002),
        )

        val handle = ExpirablePermitHandle(permit, leases)

        handle.leases.size shouldBeEqualTo permit.permits
        handle.toString() shouldNotContain "permit-1"
        assertFailsWith<IllegalArgumentException> {
            ExpirablePermitHandle(permit, leases.dropLast(1))
        }
    }

    @Test
    fun `identity diagnostics remain redacted`() {
        SemaphoreOwnerId.from("owner-secret").toString() shouldNotContain "owner-secret"
        SemaphoreRequestId.from("request-secret").toString() shouldNotContain "request-secret"
        LatchRequestId.from("latch-secret").toString() shouldNotContain "latch-secret"
        assertFailsWith<IllegalArgumentException> { SemaphoreOwnerId.from("owner|injected") }
        assertFailsWith<IllegalArgumentException> { SemaphoreRequestId.from("request,injected") }
    }

    @Test
    fun `configuration rejects unsafe bounds`() {
        assertFailsWith<IllegalArgumentException> { SemaphoreConfig(maxPermits = 0) }
        assertFailsWith<IllegalArgumentException> { SemaphoreConfig(pollInterval = Duration.ZERO) }
        assertFailsWith<IllegalArgumentException> {
            ExpirableSemaphoreConfig(leaseTime = Duration.ofMillis(99))
        }
        assertFailsWith<IllegalArgumentException> { LatchConfig(maxCount = 0) }
    }

    @Test
    fun `result families expose stable closed and unavailable states`() {
        val acquire: PermitAcquireResult<PermitHandle> = PermitAcquireResult.Unavailable
        val mutation: PermitMutationResult<PermitHandle> = PermitMutationResult.Closed
        val inspect: PermitInspectResult<PermitHandle> = PermitInspectResult.Closed
        val renew: PermitRenewResult<ExpirablePermitHandle> = PermitRenewResult.Closed
        val latch: LatchAwaitResult = LatchAwaitResult.Closed

        acquire shouldBeEqualTo PermitAcquireResult.Unavailable
        mutation shouldBeEqualTo PermitMutationResult.Closed
        inspect shouldBeEqualTo PermitInspectResult.Closed
        renew shouldBeEqualTo PermitRenewResult.Closed
        latch shouldBeEqualTo LatchAwaitResult.Closed
    }
}
