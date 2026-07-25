package io.bluetape4k.redis.lettuce.lock

import java.io.Serializable

/** Allowlisted Redis backend failure categories. */
enum class LockBackendFailureKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
}

/** Allowlisted lock-state integrity failure categories. */
enum class LockIntegrityFailureKind {
    MALFORMED_REPLY,
    INVALID_STATE,
    INVALID_GENERATION,
    PARTIAL_MULTI_LOCK,
    COUNTER_REGRESSION,
}

/** Caller action that safely continues an ambiguous or lost lock operation. */
enum class LockRecoveryAction {
    RECONCILE_REQUEST,
    INSPECT_HANDLE,
    RETRY_SAME_HANDLE,
    STOP_AND_REACQUIRE,
}

/** Sanitized backend failure without raw exception, command, key, owner, or reply data. */
data class LockBackendFailure(
    val kind: LockBackendFailureKind,
    val recoveryAction: LockRecoveryAction,
): Serializable {
    private fun readResolve(): Any = restoreLockSerializedValue("LockBackendFailure") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Sanitized invariant failure without raw Redis state or caller identity. */
data class LockIntegrityFailure(
    val kind: LockIntegrityFailureKind,
): Serializable {
    private fun readResolve(): Any = restoreLockSerializedValue("LockIntegrityFailure") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
