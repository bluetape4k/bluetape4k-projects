package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.FairLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

/**
 * A Redis-authoritative FIFO lock ordered by the enqueue sequence assigned inside Redis.
 *
 * Stale waiters are removed in bounded batches. When the cleanup budget is exhausted, acquisition fails closed with
 * [LockAcquireResult.CleanupPending] rather than bypassing an earlier queue position.
 */
class LettuceFairLock internal constructor(
    private val client: FairLockClient,
) : AutoCloseable {

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.tryAcquire(ownerId, requestId, leasePolicy)

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.tryAcquireAsync(ownerId, requestId, leasePolicy)

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.acquire(ownerId, requestId, waitTime, leasePolicy)

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.acquireAsync(ownerId, requestId, waitTime, leasePolicy)

    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> =
        client.inspect(handle)

    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> =
        client.inspectAsync(handle)

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> =
        client.reconcile(ownerId, requestId)

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        client.reconcileAsync(ownerId, requestId)

    fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> =
        client.renew(handle, extension)

    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> =
        client.renewAsync(handle, extension)

    fun release(handle: LockHandle): LockMutationResult<LockHandle> =
        client.release(handle)

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> =
        client.releaseAsync(handle)

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceFairLock =
            create(connection, name, FairLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FairLockConfig,
        ): LettuceFairLock =
            LettuceFairLock(FairLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceFairLock =
            create(connection, name, FairLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FairLockConfig,
        ): LettuceFairLock =
            LettuceFairLock(FairLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FairLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceFairLock =
            LettuceFairLock(FairLockClient.create(connection, name, config, scheduler, observationSink))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FairLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceFairLock =
            LettuceFairLock(FairLockClient.create(connection, name, config, scheduler, observationSink))
    }
}
