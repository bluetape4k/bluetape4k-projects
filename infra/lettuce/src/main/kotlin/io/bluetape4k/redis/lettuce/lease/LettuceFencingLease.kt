package io.bluetape4k.redis.lettuce.lease

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * Provides blocking and [CompletableFuture]-based access to one Redis fencing-lease ordering domain.
 *
 * A fencing token proves ordering, not durable business correctness. Persist every accepted token together with the
 * resource identity and reject downstream writes whose token is not strictly greater than the stored token. Redis Lua
 * serializes acquisition for a hot resource, but exactly-once work and business idempotency remain caller concerns.
 * The supplied config binds the instance to one namespace, resource, and durable epoch.
 */
class LettuceFencingLease private constructor(
    private val executor: FencingScriptExecutor,
    codec: RedisCodec<String, String>,
    private val config: LettuceFencingLeaseConfig,
) {
    private val keys: FencingLeaseKeys = deriveFencingLeaseKeys(config, codec)

    /** Creates a config-bound fencing lease after validating that encoded lease/counter keys share one slot. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(DefaultFencingScriptExecutor(connection.sync(), connection.async()), connection.codec, config)

    /** Creates a Redis Cluster fencing lease after validating the codec-produced lease/counter key slot. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(DefaultFencingScriptExecutor(connection.sync(), connection.async()), connection.codec, config)

    /** Initializes a new externally approved epoch; this never reconstructs or repairs lost same-epoch history. */
    fun bootstrap(): FencingBootstrapResult = classified(
        FencingLeaseOperation.BOOTSTRAP,
        FencingBootstrapResult::BackendFailure,
    ) {
        decodeFencingBootstrap(executor.run(FencingLeaseOperation.BOOTSTRAP, keys, fencingBootstrapArgs(config)))
    }

    /** Asynchronously initializes a new externally approved epoch without repairing lost history. */
    fun bootstrapAsync(): CompletableFuture<FencingBootstrapResult> =
        executor.runAsync(FencingLeaseOperation.BOOTSTRAP, keys, fencingBootstrapArgs(config))
            .decodeCancellable(
                FencingLeaseOperation.BOOTSTRAP,
                config::domainFingerprint,
                ::decodeFencingBootstrap,
                FencingBootstrapResult::BackendFailure,
            )

    /** Acquires the lease; retry ambiguous completion only with the same [ownerId]. */
    fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult {
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.ACQUIRE, FencingAcquireResult::BackendFailure) {
            decodeFencingAcquire(
                executor.run(FencingLeaseOperation.ACQUIRE, keys, fencingAcquireArgs(config, ownerId, leaseTimeMillis)),
            )
        }
    }

    /**
     * Asynchronously acquires the lease while preserving cancellation of the active Redis command.
     *
     * Cancellation does not prove that Redis skipped the mutation; reconcile with the same [ownerId].
     */
    fun acquireAsync(
        ownerId: FencingOwnerId,
        leaseTime: Duration,
    ): CompletableFuture<FencingAcquireResult> {
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return executor.runAsync(
            FencingLeaseOperation.ACQUIRE,
            keys,
            fencingAcquireArgs(config, ownerId, leaseTimeMillis),
        ).decodeCancellable(
            FencingLeaseOperation.ACQUIRE,
            config::domainFingerprint,
            ::decodeFencingAcquire,
            FencingAcquireResult::BackendFailure,
        )
    }

    /** Inspects current ownership without extending TTL or mutating the counter. */
    fun inspect(ownerId: FencingOwnerId): FencingInspectResult = classified(
        FencingLeaseOperation.INSPECT,
        FencingInspectResult::BackendFailure,
    ) {
        decodeFencingInspect(executor.run(FencingLeaseOperation.INSPECT, keys, fencingInspectArgs(config, ownerId)))
    }

    /** Asynchronously inspects current ownership without extending TTL or mutating the counter. */
    fun inspectAsync(ownerId: FencingOwnerId): CompletableFuture<FencingInspectResult> =
        executor.runAsync(
            FencingLeaseOperation.INSPECT,
            keys,
            fencingInspectArgs(config, ownerId),
        ).decodeCancellable(
            FencingLeaseOperation.INSPECT,
            config::domainFingerprint,
            ::decodeFencingInspect,
            FencingInspectResult::BackendFailure,
        )

    /** Renews only the matching owner/token; a cross-epoch token is rejected before Redis dispatch. */
    fun renew(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): FencingRenewResult {
        requireFencingTokenEpoch(config, token)
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return classified(FencingLeaseOperation.RENEW, FencingRenewResult::BackendFailure) {
            decodeFencingRenew(
                executor.run(FencingLeaseOperation.RENEW, keys, fencingRenewArgs(ownerId, token, leaseTimeMillis)),
            )
        }
    }

    /** Asynchronously renews the matching owner/token and rejects a cross-epoch token before dispatch. */
    fun renewAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
        leaseTime: Duration,
    ): CompletableFuture<FencingRenewResult> {
        requireFencingTokenEpoch(config, token)
        val leaseTimeMillis = leaseTime.validatedFencingLeaseTimeMillis()
        return executor.runAsync(
            FencingLeaseOperation.RENEW,
            keys,
            fencingRenewArgs(ownerId, token, leaseTimeMillis),
        ).decodeCancellable(
            FencingLeaseOperation.RENEW,
            config::domainFingerprint,
            ::decodeFencingRenew,
            FencingRenewResult::BackendFailure,
        )
    }

    /** Releases only the matching owner/token; a cross-epoch token is rejected before Redis dispatch. */
    fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult {
        requireFencingTokenEpoch(config, token)
        return classified(FencingLeaseOperation.RELEASE, FencingReleaseResult::BackendFailure) {
            decodeFencingRelease(executor.run(FencingLeaseOperation.RELEASE, keys, fencingReleaseArgs(ownerId, token)))
        }
    }

    /** Asynchronously releases the matching owner/token and rejects a cross-epoch token before dispatch. */
    fun releaseAsync(
        ownerId: FencingOwnerId,
        token: FencingToken,
    ): CompletableFuture<FencingReleaseResult> {
        requireFencingTokenEpoch(config, token)
        return executor.runAsync(
            FencingLeaseOperation.RELEASE,
            keys,
            fencingReleaseArgs(ownerId, token),
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
        internal fun createForTesting(
            executor: FencingScriptExecutor,
            codec: RedisCodec<String, String>,
            config: LettuceFencingLeaseConfig,
        ): LettuceFencingLease = LettuceFencingLease(executor, codec, config)
    }
}

internal fun Duration.validatedFencingLeaseTimeMillis(): Long =
    requireFencingLeaseMillis().requireRedisFencingLeaseTimeMillis()
