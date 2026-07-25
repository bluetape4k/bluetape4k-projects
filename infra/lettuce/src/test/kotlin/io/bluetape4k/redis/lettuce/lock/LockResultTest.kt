package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldNotContain
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.io.Serializable
import java.time.Duration

class LockResultTest {

    @Test
    fun `operation results expose typed handles and stable singleton states`() {
        val handle = lockHandle()
        val read = ReadLockHandle(handle.copy(kind = LockKind.READ))

        val acquired: LockAcquireResult<LockHandle> = LockAcquireResult.Acquired(handle)
        val negative: LockAcquireResult<LockHandle> = LockAcquireResult.TimedOut
        val downgrade: DowngradeResult = DowngradeResult.Downgraded(read)

        (acquired as LockAcquireResult.Acquired).handle shouldBeEqualTo handle
        negative shouldBeSameInstanceAs LockAcquireResult.TimedOut
        (downgrade as DowngradeResult.Downgraded).handle shouldBeEqualTo read
    }

    @Test
    fun `result payload bounds fail closed`() {
        val handle = lockHandle()

        assertFailsWith<IllegalArgumentException> { LockAcquireResult.Reentered(handle, 0) }
        assertFailsWith<IllegalArgumentException> { LockAcquireResult.Contended(-1) }
        assertFailsWith<IllegalArgumentException> { LockInspectResult.Owned(handle, 0, 1) }
        assertFailsWith<IllegalArgumentException> { LockInspectResult.Owned(handle, 1, -1) }
        assertFailsWith<IllegalArgumentException> { LockMutationResult.Renewed(handle, -1) }
        assertFailsWith<IllegalArgumentException> { LockMutationResult.Released(-1) }
    }

    @Test
    fun `ambiguous acquire diagnostics do not expose owner or request ids`() {
        val result = LockAcquireResult.Ambiguous(
            LockOwnerId.from("owner-secret"),
            LockRequestId.from("request-secret"),
            LockRecoveryAction.RECONCILE_REQUEST,
        )

        result.toString() shouldNotContain "owner-secret"
        result.toString() shouldNotContain "request-secret"
    }

    @Test
    fun `failures results and variants have stable validated serialization`() {
        val handle = lockHandle()
        val backend = LockBackendFailure(LockBackendFailureKind.TIMEOUT, LockRecoveryAction.RECONCILE_REQUEST)
        val integrity = LockIntegrityFailure(LockIntegrityFailureKind.INVALID_GENERATION)
        val samples = listOf<Serializable>(
            backend,
            integrity,
            LockAcquireResult.Acquired(handle),
            LockAcquireResult.Reentered(handle, 2),
            LockAcquireResult.Contended(1),
            LockAcquireResult.TimedOut,
            LockAcquireResult.CleanupPending,
            LockAcquireResult.CapacityExceeded,
            LockAcquireResult.Closed,
            LockAcquireResult.BackendFailure(backend),
            LockAcquireResult.IntegrityFailure(integrity),
            LockAcquireResult.Ambiguous(handle.ownerId, handle.requestId, LockRecoveryAction.RECONCILE_REQUEST),
            LockInspectResult.Owned(handle, 1, 1),
            LockInspectResult.Released,
            LockInspectResult.Expired,
            LockInspectResult.StaleGeneration,
            LockInspectResult.OwnershipLost,
            LockInspectResult.Closed,
            LockInspectResult.BackendFailure(backend),
            LockInspectResult.IntegrityFailure(integrity),
            LockReconcileResult.Owned(handle, 1, 1),
            LockReconcileResult.Queued(FairWaiterState(FairWaiterStatus.QUEUED, 1, 1)),
            LockReconcileResult.Removed,
            LockReconcileResult.Released,
            LockReconcileResult.NotFound,
            LockReconcileResult.StaleGeneration,
            LockReconcileResult.Closed,
            LockReconcileResult.BackendFailure(backend),
            LockReconcileResult.IntegrityFailure(integrity),
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST),
            LockMutationResult.Renewed(handle, 1),
            LockMutationResult.Released(0),
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.StaleGeneration,
            LockMutationResult.OwnershipLost,
            LockMutationResult.Closed,
            LockMutationResult.BackendFailure(backend),
            LockMutationResult.IntegrityFailure(integrity),
            LockMutationResult.Ambiguous(LockRecoveryAction.RETRY_SAME_HANDLE),
            DowngradeResult.Downgraded(ReadLockHandle(handle.copy(kind = LockKind.READ))),
            DowngradeResult.Expired,
            DowngradeResult.StaleGeneration,
            DowngradeResult.OwnershipLost,
            DowngradeResult.Closed,
            DowngradeResult.BackendFailure(backend),
            DowngradeResult.IntegrityFailure(integrity),
            DowngradeResult.Ambiguous(LockRecoveryAction.INSPECT_HANDLE),
        )

        samples.forEach { original ->
            javaRoundTrip(original) shouldBeEqualTo original
            ObjectStreamClass.lookup(original.javaClass).serialVersionUID shouldBeEqualTo 1L
        }
        listOf(
            LockAcquireResult.TimedOut,
            LockInspectResult.OwnershipLost,
            LockReconcileResult.NotFound,
            LockMutationResult.AlreadyReleased,
            DowngradeResult.Expired,
        ).forEach { singleton ->
            javaRoundTrip(singleton) shouldBeSameInstanceAs singleton
        }
    }

    @Test
    fun `deserialization revalidates result payloads without echoing values`() {
        val invalid = LockAcquireResult.Contended(1).withField("remainingTtlMillis", -1L)
        val error = assertFailsWith<InvalidObjectException> { javaRoundTrip(invalid) }

        error.message shouldBeEqualTo "Invalid serialized LockAcquireResult.Contended."
        error.message shouldNotContain "-1"
    }

    private fun lockHandle(): LockHandle =
        LockHandle(
            objectFingerprint = "fingerprint",
            ownerId = LockOwnerId.from("owner"),
            generation = LockGeneration(1),
            requestId = LockRequestId.from("request"),
            leasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(1)),
            kind = LockKind.DISTRIBUTED,
        )

    private fun <T: Serializable> T.withField(name: String, value: Any?): T = apply {
        javaClass.getDeclaredField(name).also { field ->
            field.isAccessible = true
            field.set(this, value)
        }
    }

    private fun javaRoundTrip(original: Serializable): Any =
        ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(original) }
            ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() }
        }
}
