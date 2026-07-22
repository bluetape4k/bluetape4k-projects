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
 * resource identity and reject downstream writes whose token is not strictly greater than the stored token.
 */
class LettuceFencingLease private constructor(
    private val executor: FencingScriptExecutor,
    codec: RedisCodec<String, String>,
    private val config: LettuceFencingLeaseConfig,
) {
    private val keys: FencingLeaseKeys = deriveFencingLeaseKeys(config, codec)

    /** Creates a fencing lease over a standalone Redis connection. */
    constructor(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(DefaultFencingScriptExecutor(connection.sync(), connection.async()), connection.codec, config)

    /** Creates a fencing lease over a Redis Cluster connection after validating the encoded key slot. */
    constructor(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): this(DefaultFencingScriptExecutor(connection.sync(), connection.async()), connection.codec, config)

    /** Initializes the counter for a new, externally approved epoch without repairing old history. */
    fun bootstrap(): FencingBootstrapResult = classified(
        FencingLeaseOperation.BOOTSTRAP,
        FencingBootstrapResult::BackendFailure,
    ) {
        decodeFencingBootstrap(executor.run(FencingLeaseOperation.BOOTSTRAP, keys, fencingBootstrapArgs(config)))
    }

    /** Asynchronously initializes the counter for a new, externally approved epoch. */
    fun bootstrapAsync(): CompletableFuture<FencingBootstrapResult> =
        executor.runAsync(FencingLeaseOperation.BOOTSTRAP, keys, fencingBootstrapArgs(config))
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
            decodeFencingAcquire(
                executor.run(FencingLeaseOperation.ACQUIRE, keys, fencingAcquireArgs(config, ownerId, leaseTimeMillis)),
            )
        }
    }

    /** Asynchronously acquires the lease while preserving cancellation of the active Redis command. */
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

    /** Inspects current ownership without extending the lease TTL. */
    fun inspect(ownerId: FencingOwnerId): FencingInspectResult = classified(
        FencingLeaseOperation.INSPECT,
        FencingInspectResult::BackendFailure,
    ) {
        decodeFencingInspect(executor.run(FencingLeaseOperation.INSPECT, keys, fencingInspectArgs(config, ownerId)))
    }

    /** Asynchronously inspects current ownership without extending the lease TTL. */
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

    /** Renews the lease only when both owner ID and fencing token still match. */
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

    /** Asynchronously renews the lease only when both owner ID and fencing token still match. */
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

    /** Releases the lease only when both owner ID and fencing token still match. */
    fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult {
        requireFencingTokenEpoch(config, token)
        return classified(FencingLeaseOperation.RELEASE, FencingReleaseResult::BackendFailure) {
            decodeFencingRelease(executor.run(FencingLeaseOperation.RELEASE, keys, fencingReleaseArgs(ownerId, token)))
        }
    }

    /** Asynchronously releases the lease only when both owner ID and fencing token still match. */
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
        fun createForTesting(
            executor: FencingScriptExecutor,
            codec: RedisCodec<String, String>,
            config: LettuceFencingLeaseConfig,
        ): LettuceFencingLease = LettuceFencingLease(executor, codec, config)
    }
}

internal fun Duration.validatedFencingLeaseTimeMillis(): Long =
    requireFencingLeaseMillis().requireRedisFencingLeaseTimeMillis()
