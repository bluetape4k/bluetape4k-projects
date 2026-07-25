package io.bluetape4k.redis.lettuce.lock

import java.io.Serializable

/** Outcomes of acquiring an exclusive, read, write, fenced, spin, fair, or multi-lock handle. */
sealed interface LockAcquireResult<out H: Serializable>: Serializable {

    /** The first request-bound hold was acquired. */
    data class Acquired<H: Serializable>(
        val handle: H,
    ): LockAcquireResult<H> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.Acquired") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The same logical owner acquired one additional request-bound hold. */
    data class Reentered<H: Serializable>(
        val handle: H,
        val holdCount: Int,
    ): LockAcquireResult<H> {
        init {
            require(holdCount > 1) { "Reentered hold count must be greater than one." }
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.Reentered") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Another owner currently holds the lock. */
    data class Contended(
        val remainingTtlMillis: Long,
    ): LockAcquireResult<Nothing> {
        init {
            require(remainingTtlMillis >= 0L) { "Remaining TTL must not be negative." }
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.Contended") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The bounded caller wait elapsed before acquisition. */
    data object TimedOut: LockAcquireResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = TimedOut
    }

    /** A fair waiter cleanup remains ambiguous and requires reconciliation. */
    data object CleanupPending: LockAcquireResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = CleanupPending
    }

    /** A bounded queue, watchdog, task, or request-hold capacity rejected the operation. */
    data object CapacityExceeded: LockAcquireResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = CapacityExceeded
    }

    /** The lock object closed before the operation could complete. */
    data object Closed: LockAcquireResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Closed
    }

    /** Redis transport or command execution failed with a sanitized category. */
    data class BackendFailure(
        val failure: LockBackendFailure,
    ): LockAcquireResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.BackendFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Redis state or reply violated a fail-closed invariant. */
    data class IntegrityFailure(
        val failure: LockIntegrityFailure,
    ): LockAcquireResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.IntegrityFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Dispatch may have completed and the same owner/request pair must be reconciled. */
    data class Ambiguous(
        val ownerId: LockOwnerId,
        val requestId: LockRequestId,
        val recoveryAction: LockRecoveryAction,
    ): LockAcquireResult<Nothing> {
        override fun toString(): String =
            "Ambiguous(ownerId=<redacted>, requestId=<redacted>, recoveryAction=$recoveryAction)"

        private fun readResolve(): Any = restoreLockSerializedValue("LockAcquireResult.Ambiguous") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Outcomes of inspecting one generation-bound lock handle. */
sealed interface LockInspectResult<out H: Serializable>: Serializable {

    /** Redis still recognizes the handle as an active owner. */
    data class Owned<H: Serializable>(
        val handle: H,
        val holdCount: Int,
        val remainingTtlMillis: Long,
    ): LockInspectResult<H> {
        init {
            validateOwnedPayload(holdCount, remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockInspectResult.Owned") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The handle was released successfully. */
    data object Released: LockInspectResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Released
    }

    /** Redis no longer has active ownership for this handle and no terminal release marker was found. */
    data object Expired: LockInspectResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Expired
    }

    /** A newer generation replaced the inspected handle. */
    data object StaleGeneration: LockInspectResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = StaleGeneration
    }

    /** The handle's ownership was lost while it was locally active. */
    data object OwnershipLost: LockInspectResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = OwnershipLost
    }

    /** The lock object is closed. */
    data object Closed: LockInspectResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Closed
    }

    /** Redis transport or command execution failed. */
    data class BackendFailure(
        val failure: LockBackendFailure,
    ): LockInspectResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockInspectResult.BackendFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Redis state or reply violated an invariant. */
    data class IntegrityFailure(
        val failure: LockIntegrityFailure,
    ): LockInspectResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockInspectResult.IntegrityFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Outcomes of reconciling an ambiguous request or fair waiter. */
sealed interface LockReconcileResult<out H: Serializable>: Serializable {

    /** The reconciled request owns an active handle. */
    data class Owned<H: Serializable>(
        val handle: H,
        val holdCount: Int,
        val remainingTtlMillis: Long,
    ): LockReconcileResult<H> {
        init {
            validateOwnedPayload(holdCount, remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockReconcileResult.Owned") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The request remains in the fair admission queue. */
    data class Queued(
        val waiter: FairWaiterState,
    ): LockReconcileResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockReconcileResult.Queued") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The exact waiter identity was removed. */
    data object Removed: LockReconcileResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Removed
    }

    /** The reconciled request completed release. */
    data object Released: LockReconcileResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Released
    }

    /** Redis has no active or terminal state for this request. */
    data object NotFound: LockReconcileResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = NotFound
    }

    /** A newer generation replaced the reconciled request. */
    data object StaleGeneration: LockReconcileResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = StaleGeneration
    }

    /** The lock object is closed. */
    data object Closed: LockReconcileResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Closed
    }

    /** Redis transport or command execution failed. */
    data class BackendFailure(
        val failure: LockBackendFailure,
    ): LockReconcileResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockReconcileResult.BackendFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Redis state or reply violated an invariant. */
    data class IntegrityFailure(
        val failure: LockIntegrityFailure,
    ): LockReconcileResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockReconcileResult.IntegrityFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Reconciliation itself completed ambiguously. */
    data class Ambiguous(
        val recoveryAction: LockRecoveryAction,
    ): LockReconcileResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockReconcileResult.Ambiguous") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Outcomes of renewing or releasing one generation-bound lock handle. */
sealed interface LockMutationResult<out H: Serializable>: Serializable {

    /** The handle remains active with the returned authoritative TTL. */
    data class Renewed<H: Serializable>(
        val handle: H,
        val remainingTtlMillis: Long,
    ): LockMutationResult<H> {
        init {
            require(remainingTtlMillis >= 0L) { "Remaining TTL must not be negative." }
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockMutationResult.Renewed") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** One request-bound hold was released. */
    data class Released(
        val remainingHoldCount: Int,
    ): LockMutationResult<Nothing> {
        init {
            require(remainingHoldCount >= 0) { "Remaining hold count must not be negative." }
        }

        private fun readResolve(): Any = restoreLockSerializedValue("LockMutationResult.Released") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** This request-bound hold already reached terminal release. */
    data object AlreadyReleased: LockMutationResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = AlreadyReleased
    }

    /** The handle expired before the mutation could apply. */
    data object Expired: LockMutationResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Expired
    }

    /** A newer generation replaced the mutated handle. */
    data object StaleGeneration: LockMutationResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = StaleGeneration
    }

    /** Ownership was lost while the handle was locally active. */
    data object OwnershipLost: LockMutationResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = OwnershipLost
    }

    /** The lock object is closed. */
    data object Closed: LockMutationResult<Nothing> {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Closed
    }

    /** Redis transport or command execution failed. */
    data class BackendFailure(
        val failure: LockBackendFailure,
    ): LockMutationResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockMutationResult.BackendFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Redis state or reply violated an invariant. */
    data class IntegrityFailure(
        val failure: LockIntegrityFailure,
    ): LockMutationResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockMutationResult.IntegrityFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Mutation dispatch may have completed and the same handle must be retried or inspected. */
    data class Ambiguous(
        val recoveryAction: LockRecoveryAction,
    ): LockMutationResult<Nothing> {
        private fun readResolve(): Any = restoreLockSerializedValue("LockMutationResult.Ambiguous") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Outcomes of atomically converting a write hold into a read hold. */
sealed interface DowngradeResult: Serializable {

    /** The write hold was replaced by the returned read handle. */
    data class Downgraded(
        val handle: ReadLockHandle,
    ): DowngradeResult {
        private fun readResolve(): Any = restoreLockSerializedValue("DowngradeResult.Downgraded") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** The write handle expired before downgrade. */
    data object Expired: DowngradeResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Expired
    }

    /** A newer write generation replaced the handle. */
    data object StaleGeneration: DowngradeResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = StaleGeneration
    }

    /** Write ownership was lost before downgrade. */
    data object OwnershipLost: DowngradeResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = OwnershipLost
    }

    /** The read/write lock object is closed. */
    data object Closed: DowngradeResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Closed
    }

    /** Redis transport or command execution failed. */
    data class BackendFailure(
        val failure: LockBackendFailure,
    ): DowngradeResult {
        private fun readResolve(): Any = restoreLockSerializedValue("DowngradeResult.BackendFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Redis state or reply violated an invariant. */
    data class IntegrityFailure(
        val failure: LockIntegrityFailure,
    ): DowngradeResult {
        private fun readResolve(): Any = restoreLockSerializedValue("DowngradeResult.IntegrityFailure") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Downgrade dispatch may have completed and the handle must be inspected. */
    data class Ambiguous(
        val recoveryAction: LockRecoveryAction,
    ): DowngradeResult {
        private fun readResolve(): Any = restoreLockSerializedValue("DowngradeResult.Ambiguous") { copy() }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private fun validateOwnedPayload(holdCount: Int, remainingTtlMillis: Long) {
    require(holdCount > 0) { "Hold count must be positive." }
    require(remainingTtlMillis >= 0L) { "Remaining TTL must not be negative." }
}
