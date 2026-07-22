package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration

/**
 * Provides suspending access to one Redis fencing-lease ordering domain.
 *
 * Coroutine cancellation is propagated and never converted into a lease result. A cancelled mutation can still have
 * reached Redis, so callers must reconcile ambiguous completion with the same owner ID or token.
 */
class LettuceSuspendFencingLease private constructor(
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    private val config: LettuceFencingLeaseConfig,
) {
    private val keys: FencingLeaseKeys = deriveFencingLeaseKeys(config, codec)

    /** Creates a suspending fencing lease over a standalone Redis connection. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(connection.async(), connection.codec, config)

    /** Creates a suspending fencing lease over a Redis Cluster connection after validating the encoded key slot. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(connection.async(), connection.codec, config)

    /** Initializes the counter for a new, externally approved epoch without repairing old history. */
    suspend fun bootstrap(): FencingBootstrapResult = classified(
        FencingLeaseOperation.BOOTSTRAP,
        FencingBootstrapResult::BackendFailure,
    ) {
        decodeFencingBootstrap(
            runFencingScriptSuspending(
                asyncCommands,
                FencingLeaseScripts.BOOTSTRAP,
                keys,
                config.epoch.toString(),
            ),
        )
    }

    /** Acquires the lease or reports the active competing owner without exposing its identity. */
    suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult {
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.ACQUIRE, FencingAcquireResult::BackendFailure) {
            decodeFencingAcquire(
                runFencingScriptSuspending(
                    asyncCommands,
                    FencingLeaseScripts.ACQUIRE,
                    keys,
                    ownerId.value,
                    config.epoch.toString(),
                    leaseTimeMillis.toString(),
                ),
            )
        }
    }

    /** Inspects current ownership without extending the lease TTL. */
    suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult = classified(
        FencingLeaseOperation.INSPECT,
        FencingInspectResult::BackendFailure,
    ) {
        decodeFencingInspect(
            runFencingScriptSuspending(
                asyncCommands,
                FencingLeaseScripts.INSPECT,
                keys,
                ownerId.value,
                config.epoch.toString(),
            ),
        )
    }

    /** Renews the lease only when both owner ID and fencing token still match. */
    suspend fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult {
        requireFencingTokenEpoch(config, token)
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.RENEW, FencingRenewResult::BackendFailure) {
            decodeFencingRenew(
                runFencingScriptSuspending(
                    asyncCommands,
                    FencingLeaseScripts.RENEW,
                    keys,
                    ownerId.value,
                    token.epoch.toString(),
                    token.sequence.toString(),
                    leaseTimeMillis.toString(),
                ),
            )
        }
    }

    /** Releases the lease only when both owner ID and fencing token still match. */
    suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult {
        requireFencingTokenEpoch(config, token)
        return classified(FencingLeaseOperation.RELEASE, FencingReleaseResult::BackendFailure) {
            decodeFencingRelease(
                runFencingScriptSuspending(
                    asyncCommands,
                    FencingLeaseScripts.RELEASE,
                    keys,
                    ownerId.value,
                    token.epoch.toString(),
                    token.sequence.toString(),
                ),
            )
        }
    }

    private suspend inline fun <R> classified(
        operation: FencingLeaseOperation,
        backendFailure: (FencingLeaseBackendFailure) -> R,
        crossinline block: suspend () -> R,
    ): R = try {
        block()
    } catch (error: Exception) {
        backendFailure(classifyFencingBackendFailure(operation, error, config::domainFingerprint))
    }

    internal companion object {
        fun fromCommands(
            asyncCommands: RedisScriptingAsyncCommands<String, String>,
            codec: RedisCodec<String, String>,
            config: LettuceFencingLeaseConfig,
        ): LettuceSuspendFencingLease = LettuceSuspendFencingLease(asyncCommands, codec, config)
    }
}
