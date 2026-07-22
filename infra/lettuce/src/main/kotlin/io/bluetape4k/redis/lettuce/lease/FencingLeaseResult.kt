package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.support.requireZeroOrPositiveNumber
import java.io.Serializable

/** Classifies allowlisted Redis backend failures without exposing exception details. */
enum class FencingBackendFailureKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
}

/**
 * Describes a Redis backend failure without retaining a cause, message, command, key, owner ID, or raw reply.
 *
 * @property kind allowlisted backend failure category
 */
data class FencingLeaseBackendFailure(
    val kind: FencingBackendFailureKind,
): Serializable {
    private fun readResolve(): Any = restoreFencingSerializedValue("FencingLeaseBackendFailure") {
        FencingLeaseBackendFailure(kind)
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Classifies fencing-lease state that violates a durable Redis invariant. */
enum class FencingIntegrityFailureKind {
    MALFORMED_LEASE,
    INVALID_COUNTER,
    COUNTER_BEHIND_LEASE,
}

/**
 * Describes a fencing-lease integrity failure without exposing stored values or Redis keys.
 *
 * @property kind violated integrity category
 */
data class FencingLeaseIntegrityFailure(
    val kind: FencingIntegrityFailureKind,
): Serializable {
    private fun readResolve(): Any = restoreFencingSerializedValue("FencingLeaseIntegrityFailure") {
        FencingLeaseIntegrityFailure(kind)
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Represents every outcome of explicitly initializing a new fencing counter. */
sealed interface FencingBootstrapResult: Serializable {

    /** Indicates that the previously absent counter was initialized to zero. */
    data object Initialized: FencingBootstrapResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Initialized
    }

    /** Indicates that a valid counter already existed. */
    data object AlreadyInitialized: FencingBootstrapResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = AlreadyInitialized
    }

    /** Indicates that stored lease or counter state violated an integrity invariant. */
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ): FencingBootstrapResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingBootstrapResult.IntegrityFailure") {
            IntegrityFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an allowlisted Redis backend failure with ambiguous completion. */
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ): FencingBootstrapResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingBootstrapResult.BackendFailure") {
            BackendFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Represents every outcome of acquiring a fencing lease. */
sealed interface FencingAcquireResult: Serializable {

    /** Indicates that a new lease and token were created for the owner. */
    data class Acquired(
        val token: FencingToken,
    ): FencingAcquireResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingAcquireResult.Acquired") {
            Acquired(token)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates deterministic recovery of the same owner's active lease without allocating a new token. */
    data class AlreadyOwned(
        val token: FencingToken,
        val remainingTtlMillis: Long,
    ): FencingAcquireResult {
        init {
            requireRemainingTtl(remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreFencingSerializedValue("FencingAcquireResult.AlreadyOwned") {
            AlreadyOwned(token, remainingTtlMillis)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that another owner currently holds the lease. */
    data class Contended(
        val remainingTtlMillis: Long,
    ): FencingAcquireResult {
        init {
            requireRemainingTtl(remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreFencingSerializedValue("FencingAcquireResult.Contended") {
            Contended(remainingTtlMillis)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that acquisition cannot proceed because the fencing counter is absent. */
    data object CounterUnavailable: FencingAcquireResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = CounterUnavailable
    }

    /** Indicates that the current epoch has allocated its final sequence. */
    data object SequenceExhausted: FencingAcquireResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = SequenceExhausted
    }

    /** Indicates that stored lease or counter state violated an integrity invariant. */
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ): FencingAcquireResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingAcquireResult.IntegrityFailure") {
            IntegrityFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an allowlisted Redis backend failure with ambiguous completion. */
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ): FencingAcquireResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingAcquireResult.BackendFailure") {
            BackendFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Represents every outcome of inspecting a fencing lease without mutation. */
sealed interface FencingInspectResult: Serializable {

    /** Indicates that the requested owner still holds the active lease. */
    data class Owned(
        val token: FencingToken,
        val remainingTtlMillis: Long,
    ): FencingInspectResult {
        init {
            requireRemainingTtl(remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreFencingSerializedValue("FencingInspectResult.Owned") {
            Owned(token, remainingTtlMillis)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that no active lease exists. */
    data object Lost: FencingInspectResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Lost
    }

    /** Indicates that another owner currently holds the active lease. */
    data class Contended(
        val remainingTtlMillis: Long,
    ): FencingInspectResult {
        init {
            requireRemainingTtl(remainingTtlMillis)
        }

        private fun readResolve(): Any = restoreFencingSerializedValue("FencingInspectResult.Contended") {
            Contended(remainingTtlMillis)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that stored lease or counter state violated an integrity invariant. */
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ): FencingInspectResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingInspectResult.IntegrityFailure") {
            IntegrityFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an allowlisted Redis backend failure. */
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ): FencingInspectResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingInspectResult.BackendFailure") {
            BackendFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Represents every outcome of renewing an existing fencing lease. */
sealed interface FencingRenewResult: Serializable {

    /** Indicates that the matching lease TTL was renewed. */
    data object Renewed: FencingRenewResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Renewed
    }

    /** Indicates that no active lease exists. */
    data object Lost: FencingRenewResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Lost
    }

    /** Indicates that the owner or token did not match the active lease. */
    data object OwnershipMismatch: FencingRenewResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = OwnershipMismatch
    }

    /** Indicates that stored lease or counter state violated an integrity invariant. */
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ): FencingRenewResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingRenewResult.IntegrityFailure") {
            IntegrityFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an allowlisted Redis backend failure with ambiguous completion. */
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ): FencingRenewResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingRenewResult.BackendFailure") {
            BackendFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Represents every outcome of releasing an existing fencing lease. */
sealed interface FencingReleaseResult: Serializable {

    /** Indicates that the matching active lease was removed. */
    data object Released: FencingReleaseResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Released
    }

    /** Indicates that no active lease exists. */
    data object Lost: FencingReleaseResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = Lost
    }

    /** Indicates that the owner or token did not match the active lease. */
    data object OwnershipMismatch: FencingReleaseResult {
        private const val serialVersionUID: Long = 1L
        private fun readResolve(): Any = OwnershipMismatch
    }

    /** Indicates that stored lease or counter state violated an integrity invariant. */
    data class IntegrityFailure(
        val failure: FencingLeaseIntegrityFailure,
    ): FencingReleaseResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingReleaseResult.IntegrityFailure") {
            IntegrityFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an allowlisted Redis backend failure with ambiguous completion. */
    data class BackendFailure(
        val failure: FencingLeaseBackendFailure,
    ): FencingReleaseResult {
        private fun readResolve(): Any = restoreFencingSerializedValue("FencingReleaseResult.BackendFailure") {
            BackendFailure(failure)
        }

        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

private fun requireRemainingTtl(remainingTtlMillis: Long) {
    remainingTtlMillis.requireZeroOrPositiveNumber("remainingTtlMillis")
}
