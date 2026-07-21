package io.bluetape4k.redis.lettuce.lease

import java.io.Serializable

/** Identifies the multi-key lease operation that detected an integrity failure. */
enum class MultiKeyLeaseOperation {
    ACQUIRE,
    INSPECT,
    RENEW,
    RELEASE,
}

/**
 * Describes the ownership state observed before a lease mutation. Counts never describe a partially applied write;
 * callers should use them as reconciliation evidence against their durable authority.
 *
 * @property requestedKeys number of requested keys
 * @property ownedKeys number of keys owned by the requester
 * @property missingKeys number of keys without a lease
 * @property mismatchedKeys number of keys owned by another requester
 */
data class MultiKeyLeaseCounts(
    val requestedKeys: Int,
    val ownedKeys: Int,
    val missingKeys: Int,
    val mismatchedKeys: Int,
): Serializable {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Represents every outcome of acquiring a multi-key lease.
 *
 * [AlreadyOwned] is the deterministic same-token replay outcome. [PartialOwnership] and [Conflicted] perform no
 * mutation and require caller policy rather than automatic repair.
 */
sealed interface MultiKeyAcquireResult: Serializable {

    /** Indicates that every requested lease was acquired. */
    data object Acquired: MultiKeyAcquireResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that every requested lease was already owned by the requester. */
    data class AlreadyOwned(
        val minimumPttlMillis: Long,
    ): MultiKeyAcquireResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates an owned-and-missing state with no lease owned by another requester. */
    data class PartialOwnership(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyAcquireResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that at least one requested lease was owned by another requester. */
    data class Conflicted(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyAcquireResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Represents every outcome of inspecting a multi-key lease without mutation.
 *
 * [Lost] cannot distinguish expiry from a previously completed release. Partial or conflicting ownership must be
 * reconciled with a durable authority.
 */
sealed interface MultiKeyInspectResult: Serializable {

    /** Indicates that every requested lease is owned by the requester. */
    data class Owned(
        val minimumPttlMillis: Long,
    ): MultiKeyInspectResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that every requested lease is missing. */
    data object Lost: MultiKeyInspectResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates an owned-and-missing state with no lease owned by another requester. */
    data class PartialOwnership(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyInspectResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that at least one requested lease is owned by another requester. */
    data class Conflicted(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyInspectResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Represents every outcome of renewing a multi-key lease.
 *
 * Renew never recreates missing keys. After ambiguous completion, inspect using the same token before deciding
 * whether to continue. [PartialLoss] and [OwnershipMismatch] require durable reconciliation.
 */
sealed interface MultiKeyRenewResult: Serializable {

    /** Indicates that every requested lease was renewed. */
    data object Renewed: MultiKeyRenewResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates an owned-and-missing state with no lease owned by another requester. */
    data class PartialLoss(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyRenewResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that every requested lease is missing. */
    data object Lost: MultiKeyRenewResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that at least one requested lease belongs to another requester. */
    data class OwnershipMismatch(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyRenewResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Represents every outcome of releasing a multi-key lease.
 *
 * After ambiguous completion, inspect using the same token first. [Lost] cannot distinguish a completed release from
 * expiry. [PartialRelease] and [OwnershipMismatch] require durable reconciliation.
 */
sealed interface MultiKeyReleaseResult: Serializable {

    /** Indicates that every requested lease was released. */
    data object Released: MultiKeyReleaseResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates an owned-and-missing state with no lease owned by another requester. */
    data class PartialRelease(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyReleaseResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that every requested lease is missing. */
    data object Lost: MultiKeyReleaseResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that at least one requested lease belongs to another requester. */
    data class OwnershipMismatch(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyReleaseResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }
}

/**
 * Reports that requested keys span more than one Redis Cluster slot.
 *
 * @property distinctSlotCount number of distinct Redis Cluster slots observed
 */
class MultiKeyLeaseCrossSlotException(
    val distinctSlotCount: Int,
): IllegalArgumentException(
    "Multi-key lease requires one Redis Cluster slot; distinctSlotCount=$distinctSlotCount.",
) {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Reports an internally inconsistent multi-key lease response without exposing lease secrets. A persistent key with
 * the same owner token is one source of this exception because a valid lease must always have a positive TTL.
 *
 * @property operation operation that observed the inconsistent response
 * @property requestedKeyCount number of requested keys
 * @property invalidLeaseKeyCount number of invalid lease entries
 */
class MultiKeyLeaseIntegrityException(
    val operation: MultiKeyLeaseOperation,
    val requestedKeyCount: Int,
    val invalidLeaseKeyCount: Int,
): IllegalStateException(
    "Multi-key lease integrity failure: operation=$operation, " +
        "requestedKeyCount=$requestedKeyCount, invalidLeaseKeyCount=$invalidLeaseKeyCount.",
) {
    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
