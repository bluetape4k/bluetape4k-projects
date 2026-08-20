package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
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

    @Test
    fun `all synchronizer result variants preserve their typed outcome and serialization`() {
        val backendFailure = SynchronizerBackendFailure(
            SynchronizerBackendFailureKind.TIMEOUT,
            SynchronizerRecoveryAction.RECONCILE_REQUEST,
        )
        val integrityFailure = SynchronizerIntegrityFailure(
            SynchronizerIntegrityFailureKind.MALFORMED_REPLY,
        )
        val permit = PermitHandle(
            objectFingerprint = "fingerprint",
            ownerId = SemaphoreOwnerId.from("owner"),
            generation = 7,
            requestId = SemaphoreRequestId.from("request"),
            permits = 2,
            token = "allocation",
        )
        val expirable = ExpirablePermitHandle(
            permit = permit,
            leases = listOf(
                ExpirablePermitLease("permit-1", 1_000),
                ExpirablePermitLease("permit-2", 1_001),
            ),
        )
        val generation = LatchGeneration(3)
        val samples = listOf<Serializable>(
            backendFailure,
            integrityFailure,
            SemaphoreInitializationResult.Initialized(1),
            SemaphoreInitializationResult.AlreadyInitialized,
            SemaphoreInitializationResult.InvalidCapacity,
            SemaphoreInitializationResult.Closed,
            SemaphoreInitializationResult.BackendFailure(backendFailure),
            SemaphoreInitializationResult.IntegrityFailure(integrityFailure),
            PermitAcquireResult.Acquired(permit),
            PermitAcquireResult.Unavailable,
            PermitAcquireResult.TimedOut,
            PermitAcquireResult.CapacityExceeded,
            PermitAcquireResult.Closed,
            PermitAcquireResult.BackendFailure(backendFailure),
            PermitAcquireResult.IntegrityFailure(integrityFailure),
            PermitAcquireResult.Ambiguous(SemaphoreRequestId.from("acquire-request")),
            PermitMutationResult.Released(expirable, 1),
            PermitMutationResult.AlreadyReleased,
            PermitMutationResult.Expired,
            PermitMutationResult.StaleGeneration,
            PermitMutationResult.Closed,
            PermitMutationResult.BackendFailure(backendFailure),
            PermitMutationResult.IntegrityFailure(integrityFailure),
            PermitMutationResult.Ambiguous(SemaphoreRequestId.from("mutation-request")),
            PermitInspectResult.Owned(expirable, 1),
            PermitInspectResult.Released,
            PermitInspectResult.Expired,
            PermitInspectResult.StaleGeneration,
            PermitInspectResult.Closed,
            PermitInspectResult.BackendFailure(backendFailure),
            PermitInspectResult.IntegrityFailure(integrityFailure),
            PermitReconcileResult.Owned(expirable, 1),
            PermitReconcileResult.Released,
            PermitReconcileResult.NotFound,
            PermitReconcileResult.StaleGeneration,
            PermitReconcileResult.Closed,
            PermitReconcileResult.BackendFailure(backendFailure),
            PermitReconcileResult.IntegrityFailure(integrityFailure),
            PermitRenewResult.Renewed(expirable),
            PermitRenewResult.Released,
            PermitRenewResult.Expired,
            PermitRenewResult.OwnershipLost,
            PermitRenewResult.StaleGeneration,
            PermitRenewResult.Closed,
            PermitRenewResult.BackendFailure(backendFailure),
            PermitRenewResult.IntegrityFailure(integrityFailure),
            PermitRenewResult.Ambiguous(SemaphoreRequestId.from("renew-request")),
            LatchSetCountResult.Created(generation),
            LatchSetCountResult.ActiveGeneration(generation, 2),
            LatchSetCountResult.InvalidCount,
            LatchSetCountResult.Closed,
            LatchSetCountResult.BackendFailure(backendFailure),
            LatchSetCountResult.IntegrityFailure(integrityFailure),
            LatchCountResult.Active(generation, 2, 1),
            LatchCountResult.Completed(generation),
            LatchCountResult.Deleted,
            LatchCountResult.StaleGeneration,
            LatchCountResult.Closed,
            LatchCountResult.BackendFailure(backendFailure),
            LatchCountResult.IntegrityFailure(integrityFailure),
            LatchAwaitResult.Completed,
            LatchAwaitResult.TimedOut,
            LatchAwaitResult.Deleted,
            LatchAwaitResult.StaleGeneration,
            LatchAwaitResult.CapacityExceeded,
            LatchAwaitResult.Closed,
            LatchAwaitResult.BackendFailure(backendFailure),
            LatchAwaitResult.IntegrityFailure(integrityFailure),
            LatchAwaitResult.Ambiguous(LatchRequestId.from("await-request")),
            LatchMutationResult.Decremented(1),
            LatchMutationResult.Completed,
            LatchMutationResult.AlreadyCompleted,
            LatchMutationResult.Deleted,
            LatchMutationResult.NotFound,
            LatchMutationResult.StaleGeneration,
            LatchMutationResult.ActiveWaiters(1),
            LatchMutationResult.Closed,
            LatchMutationResult.BackendFailure(backendFailure),
            LatchMutationResult.IntegrityFailure(integrityFailure),
            LatchMutationResult.Ambiguous(LatchRequestId.from("mutation-request")),
        )

        samples.forEach { original ->
            val restored = javaRoundTrip(original)
            restored shouldBeEqualTo original
        }
    }

    @Test
    fun `typed value objects reject invalid boundaries and preserve redaction`() {
        assertFailsWith<IllegalArgumentException> { PermitHandle("", SemaphoreOwnerId.from("owner"), 1, SemaphoreRequestId.from("request"), 1, "token") }
        assertFailsWith<IllegalArgumentException> { PermitHandle("fingerprint", SemaphoreOwnerId.from("owner"), 0, SemaphoreRequestId.from("request"), 1, "token") }
        assertFailsWith<IllegalArgumentException> { PermitHandle("fingerprint", SemaphoreOwnerId.from("owner"), 1, SemaphoreRequestId.from("request"), 0, "token") }
        assertFailsWith<IllegalArgumentException> { ExpirablePermitLease("", 1) }
        assertFailsWith<IllegalArgumentException> { ExpirablePermitLease("permit", 0) }
        assertFailsWith<IllegalArgumentException> { ExpirablePermitHandle(PermitHandle("fingerprint", SemaphoreOwnerId.from("owner"), 1, SemaphoreRequestId.from("request"), 2, "token"), listOf(ExpirablePermitLease("same", 1), ExpirablePermitLease("same", 2))) }
        assertFailsWith<IllegalArgumentException> { LatchGeneration(0) }
        assertFailsWith<IllegalArgumentException> { PermitMutationResult.Released(permitHandle, -1) }
        assertFailsWith<IllegalArgumentException> { PermitInspectResult.Owned(permitHandle, -1) }
        assertFailsWith<IllegalArgumentException> { LatchSetCountResult.ActiveGeneration(LatchGeneration(1), -1) }
        assertFailsWith<IllegalArgumentException> { LatchCountResult.Active(LatchGeneration(1), 0, 0) }
        assertFailsWith<IllegalArgumentException> { LatchMutationResult.ActiveWaiters(0) }

        PermitAcquireResult.Ambiguous(SemaphoreRequestId.from("secret-request")).toString() shouldNotContain "secret-request"
        LatchAwaitResult.Ambiguous(LatchRequestId.from("secret-request")).toString() shouldNotContain "secret-request"
    }

    @Test
    fun `configuration validates namespace slot and waiter lifecycle bounds`() {
        SemaphoreConfig(namespace = "orders:v1", hashTag = "orders", maxPermits = 1).maxPermits shouldBeEqualTo 1
        LatchConfig(namespace = "orders:v1", hashTag = "orders", maxCount = 1, maxWaiters = 1).maxWaiters shouldBeEqualTo 1

        listOf("", "white space", "{slot}", "-starts-with-dash").forEach { namespace ->
            assertFailsWith<IllegalArgumentException> { SemaphoreConfig(namespace = namespace) }
            assertFailsWith<IllegalArgumentException> { LatchConfig(namespace = namespace) }
        }
        assertFailsWith<IllegalArgumentException> { SemaphoreConfig(hashTag = "unsafe|tag") }
        assertFailsWith<IllegalArgumentException> { SemaphoreConfig(maxPermits = 1_000_001) }
        assertFailsWith<IllegalArgumentException> { LatchConfig(maxWaiters = 0) }
        assertFailsWith<IllegalArgumentException> { LatchConfig(waiterCleanupGrace = Duration.ofMinutes(6)) }
        assertFailsWith<IllegalArgumentException> { ExpirableSemaphoreConfig(maxPermitsPerAcquire = 65) }
        assertFailsWith<IllegalArgumentException> { ExpirableSemaphoreConfig(cleanupBatchLimit = 1_025) }
    }

    @Test
    fun `deserialization revalidates opaque identities and fails closed`() {
        val invalidOwner = SemaphoreOwnerId.from("owner").withField("value", "owner|bad")
        val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalidOwner) }
        error.cause shouldBeEqualTo null
        error.message shouldBeEqualTo "Invalid serialized ${invalidOwner.javaClass.simpleName}."
    }

    private fun <T: Serializable> T.withField(name: String, value: Any?): T = apply {
        javaClass.getDeclaredField(name).also { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }

    private fun javaRoundTrip(original: Serializable): Any =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { output -> output.writeObject(original) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { input -> input.readObject() }
        }

    private companion object {
        val permitHandle = PermitHandle(
            objectFingerprint = "fingerprint",
            ownerId = SemaphoreOwnerId.from("owner"),
            generation = 1,
            requestId = SemaphoreRequestId.from("request"),
            permits = 1,
            token = "token",
        )
    }
}
