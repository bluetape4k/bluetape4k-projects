package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Provides blocking and [CompletableFuture]-based access to one Redis fencing-lease ordering domain.
 *
 * A fencing token proves ordering, not durable business correctness. Persist every accepted token together with the
 * resource identity and reject downstream writes whose token is not strictly greater than the stored token.
 */
class LettuceFencingLease private constructor(
    private val syncCommands: RedisScriptingCommands<String, String>,
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
    codec: RedisCodec<String, String>,
    private val config: LettuceFencingLeaseConfig,
) {
    private val keys: FencingLeaseKeys = deriveFencingLeaseKeys(config, codec)

    /** Creates a fencing lease over a standalone Redis connection. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(connection.sync(), connection.async(), connection.codec, config)

    /** Creates a fencing lease over a Redis Cluster connection after validating the encoded key slot. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(connection.sync(), connection.async(), connection.codec, config)

    /** Initializes the counter for a new, externally approved epoch without repairing old history. */
    fun bootstrap(): FencingBootstrapResult = classified(
        FencingLeaseOperation.BOOTSTRAP,
        FencingBootstrapResult::BackendFailure,
    ) {
        runFencingBootstrap(syncCommands, keys, config)
    }

    /** Asynchronously initializes the counter for a new, externally approved epoch. */
    fun bootstrapAsync(): CompletableFuture<FencingBootstrapResult> =
        runFencingScriptAsync(asyncCommands, FencingLeaseScripts.BOOTSTRAP, keys, config.epoch.toString())
            .decodeCancellable(
                FencingLeaseOperation.BOOTSTRAP,
                config::domainFingerprint,
                ::decodeFencingBootstrap,
                FencingBootstrapResult::BackendFailure,
            )

    /** Acquires the lease or reports the active competing owner without exposing its identity. */
    fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult {
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.ACQUIRE, FencingAcquireResult::BackendFailure) {
            runFencingAcquire(syncCommands, keys, config, ownerId, leaseTimeMillis)
        }
    }

    /** Asynchronously acquires the lease while preserving cancellation of the active Redis command. */
    fun acquireAsync(
        ownerId: FencingOwnerId,
        leaseTime: Duration,
    ): CompletableFuture<FencingAcquireResult> {
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return runFencingScriptAsync(
            asyncCommands,
            FencingLeaseScripts.ACQUIRE,
            keys,
            ownerId.value,
            config.epoch.toString(),
            leaseTimeMillis.toString(),
        ).decodeCancellable(
            FencingLeaseOperation.ACQUIRE,
            config::domainFingerprint,
            ::decodeFencingAcquire,
            FencingAcquireResult::BackendFailure,
        )
    }

    /** Inspects current ownership without extending the lease TTL. */
    fun inspect(ownerId: FencingOwnerId): FencingInspectResult = classified(
        FencingLeaseOperation.INSPECT,
        FencingInspectResult::BackendFailure,
    ) {
        runFencingInspect(syncCommands, keys, config, ownerId)
    }

    /** Asynchronously inspects current ownership without extending the lease TTL. */
    fun inspectAsync(ownerId: FencingOwnerId): CompletableFuture<FencingInspectResult> =
        runFencingScriptAsync(
            asyncCommands,
            FencingLeaseScripts.INSPECT,
            keys,
            ownerId.value,
            config.epoch.toString(),
        ).decodeCancellable(
            FencingLeaseOperation.INSPECT,
            config::domainFingerprint,
            ::decodeFencingInspect,
            FencingInspectResult::BackendFailure,
        )

    /** Renews the lease only when both owner ID and fencing token still match. */
    fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult {
        requireFencingTokenEpoch(config, token)
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.RENEW, FencingRenewResult::BackendFailure) {
            runFencingRenew(syncCommands, keys, config, ownerId, token, leaseTimeMillis)
        }
    }

    /** Asynchronously renews the lease only when both owner ID and fencing token still match. */
    fun renewAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): CompletableFuture<FencingRenewResult> {
        requireFencingTokenEpoch(config, token)
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return runFencingScriptAsync(
            asyncCommands,
            FencingLeaseScripts.RENEW,
            keys,
            ownerId.value,
            token.epoch.toString(),
            token.sequence.toString(),
            leaseTimeMillis.toString(),
        ).decodeCancellable(
            FencingLeaseOperation.RENEW,
            config::domainFingerprint,
            ::decodeFencingRenew,
            FencingRenewResult::BackendFailure,
        )
    }

    /** Releases the lease only when both owner ID and fencing token still match. */
    fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult {
        requireFencingTokenEpoch(config, token)
        return classified(FencingLeaseOperation.RELEASE, FencingReleaseResult::BackendFailure) {
            runFencingRelease(syncCommands, keys, config, ownerId, token)
        }
    }

    /** Asynchronously releases the lease only when both owner ID and fencing token still match. */
    fun releaseAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
    ): CompletableFuture<FencingReleaseResult> {
        requireFencingTokenEpoch(config, token)
        return runFencingScriptAsync(
            asyncCommands,
            FencingLeaseScripts.RELEASE,
            keys,
            ownerId.value,
            token.epoch.toString(),
            token.sequence.toString(),
        ).decodeCancellable(
            FencingLeaseOperation.RELEASE,
            config::domainFingerprint,
            ::decodeFencingRelease,
            FencingReleaseResult::BackendFailure,
        )
    }

    private inline fun <R> classified(
        operation: FencingLeaseOperation,
        backendFailure: (FencingLeaseBackendFailure) -> R,
        block: () -> R,
    ): R = try {
        block()
    } catch (error: Exception) {
        backendFailure(classifyFencingBackendFailure(operation, error, config::domainFingerprint))
    }

    internal companion object {
        fun fromCommands(
            syncCommands: RedisScriptingCommands<String, String>,
            asyncCommands: RedisScriptingAsyncCommands<String, String>,
            codec: RedisCodec<String, String>,
            config: LettuceFencingLeaseConfig,
        ): LettuceFencingLease = LettuceFencingLease(syncCommands, asyncCommands, codec, config)
    }
}

internal fun Duration.validatedFencingLeaseTimeMillis(): Long =
    requireFencingLeaseMillis().requireRedisFencingLeaseTimeMillis()
