package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.MultiLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

/** An all-or-nothing, same-slot lock over one immutable set of constituent resource names. */
class LettuceMultiLock internal constructor(
    private val client: MultiLockClient,
): AutoCloseable {

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> =
        client.tryAcquire(ownerId, requestId, leasePolicy)

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<MultiLockHandle>> =
        client.tryAcquireAsync(ownerId, requestId, leasePolicy)

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> =
        client.acquire(ownerId, requestId, waitTime, leasePolicy)

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<MultiLockHandle>> =
        client.acquireAsync(ownerId, requestId, waitTime, leasePolicy)

    fun inspect(handle: MultiLockHandle): LockInspectResult<MultiLockHandle> =
        client.inspect(handle)

    fun inspectAsync(handle: MultiLockHandle): CompletableFuture<LockInspectResult<MultiLockHandle>> =
        client.inspectAsync(handle)

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<MultiLockHandle> =
        client.reconcile(ownerId, requestId)

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<MultiLockHandle>> =
        client.reconcileAsync(ownerId, requestId)

    fun renew(
        handle: MultiLockHandle,
        extension: Duration,
    ): LockMutationResult<MultiLockHandle> =
        client.renew(handle, extension)

    fun renewAsync(
        handle: MultiLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<MultiLockHandle>> =
        client.renewAsync(handle, extension)

    fun release(handle: MultiLockHandle): LockMutationResult<MultiLockHandle> =
        client.release(handle)

    fun releaseAsync(handle: MultiLockHandle): CompletableFuture<LockMutationResult<MultiLockHandle>> =
        client.releaseAsync(handle)

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
        ): LettuceMultiLock =
            create(connection, names, MultiLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
        ): LettuceMultiLock =
            LettuceMultiLock(MultiLockClient.create(connection, names, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
        ): LettuceMultiLock =
            create(connection, names, MultiLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
        ): LettuceMultiLock =
            LettuceMultiLock(MultiLockClient.create(connection, names, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceMultiLock =
            LettuceMultiLock(MultiLockClient.create(connection, names, config, scheduler, observationSink))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceMultiLock =
            LettuceMultiLock(MultiLockClient.create(connection, names, config, scheduler, observationSink))
    }
}
