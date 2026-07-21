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
 * Describes the ownership state observed before a lease mutation.
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

/** Represents every outcome of acquiring a multi-key lease. */
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

    /** Indicates that only some requested leases were owned by the requester. */
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

/** Represents every outcome of inspecting a multi-key lease. */
sealed interface MultiKeyInspectResult: Serializable {

    /** Indicates that every requested lease is owned by the requester. */
    data class Owned(
        val minimumPttlMillis: Long,
    ): MultiKeyInspectResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that none of the requested leases is owned by the requester. */
    data object Lost: MultiKeyInspectResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that only some requested leases are owned by the requester. */
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

/** Represents every outcome of renewing a multi-key lease. */
sealed interface MultiKeyRenewResult: Serializable {

    /** Indicates that every requested lease was renewed. */
    data object Renewed: MultiKeyRenewResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that only some requested leases could be renewed. */
    data class PartialLoss(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyRenewResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that none of the requested leases remained owned. */
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

/** Represents every outcome of releasing a multi-key lease. */
sealed interface MultiKeyReleaseResult: Serializable {

    /** Indicates that every requested lease was released. */
    data object Released: MultiKeyReleaseResult {
        private const val serialVersionUID: Long = 1L
    }

    /** Indicates that only some requested leases could be released. */
    data class PartialRelease(
        val counts: MultiKeyLeaseCounts,
    ): MultiKeyReleaseResult {
        private companion object {
            private const val serialVersionUID: Long = 1L
        }
    }

    /** Indicates that none of the requested leases remained owned. */
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
)

/**
 * Reports an internally inconsistent multi-key lease response without exposing lease secrets.
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
)
