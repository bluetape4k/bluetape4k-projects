package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.FencedLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

/**
 * A reentrant Redis lock whose fresh ownership generations receive monotonically increasing fencing tokens.
 *
 * A token is an ordering signal, not stale-work prevention by itself. Downstream state must persist the accepted
 * `(epoch, fencingToken)` pair and reject updates whose pair is not strictly greater.
 */
class LettuceFencedLock internal constructor(
    private val client: FencedLockClient,
) : AutoCloseable {

    /** Explicitly initializes this epoch's persistent fencing counter. This operation is safe to retry. */
    fun bootstrapFencing(): FencedBootstrapResult =
        client.bootstrapFencing()

    /** Explicitly initializes this epoch's persistent fencing counter asynchronously. */
    fun bootstrapFencingAsync(): CompletableFuture<FencedBootstrapResult> =
        client.bootstrapFencingAsync()

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> =
        client.tryAcquire(ownerId, requestId, leasePolicy)

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<FencedLockHandle>> =
        client.tryAcquireAsync(ownerId, requestId, leasePolicy)

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> =
        client.acquire(ownerId, requestId, waitTime, leasePolicy)

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<FencedLockHandle>> =
        client.acquireAsync(ownerId, requestId, waitTime, leasePolicy)

    fun inspect(handle: FencedLockHandle): LockInspectResult<FencedLockHandle> =
        client.inspect(handle)

    fun inspectAsync(handle: FencedLockHandle): CompletableFuture<LockInspectResult<FencedLockHandle>> =
        client.inspectAsync(handle)

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<FencedLockHandle> =
        client.reconcile(ownerId, requestId)

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<FencedLockHandle>> =
        client.reconcileAsync(ownerId, requestId)

    fun renew(
        handle: FencedLockHandle,
        extension: Duration,
    ): LockMutationResult<FencedLockHandle> =
        client.renew(handle, extension)

    fun renewAsync(
        handle: FencedLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<FencedLockHandle>> =
        client.renewAsync(handle, extension)

    fun release(handle: FencedLockHandle): LockMutationResult<FencedLockHandle> =
        client.release(handle)

    fun releaseAsync(handle: FencedLockHandle): CompletableFuture<LockMutationResult<FencedLockHandle>> =
        client.releaseAsync(handle)

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FencedLockConfig,
        ): LettuceFencedLock =
            LettuceFencedLock(FencedLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FencedLockConfig,
        ): LettuceFencedLock =
            LettuceFencedLock(FencedLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceFencedLock =
            LettuceFencedLock(
                FencedLockClient.create(connection, name, config, scheduler, observationSink),
            )

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceFencedLock =
            LettuceFencedLock(
                FencedLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
