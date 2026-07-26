package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.ReadWriteLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

/**
 * A Redis-authoritative phase-fair read/write lock.
 *
 * Readers already queued before the current writer boundary form one bounded reader phase. A write-to-read
 * [downgrade] is atomic; read-to-write upgrade is deliberately unsupported because it cannot preserve progress.
 */
class LettuceReadWriteLock internal constructor(
    private val client: ReadWriteLockClient,
) : AutoCloseable {

    private val readView = ReadLockView(client)
    private val writeView = WriteLockView(client)

    fun readLock(): ReadLockView = readView

    fun writeLock(): WriteLockView = writeView

    fun downgrade(handle: WriteLockHandle): DowngradeResult =
        client.downgrade(handle)

    fun downgradeAsync(handle: WriteLockHandle): CompletableFuture<DowngradeResult> =
        client.downgradeAsync(handle)

    override fun close() {
        client.close()
    }

    class ReadLockView internal constructor(
        private val client: ReadWriteLockClient,
    ) {
        fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<ReadLockHandle> =
            client.tryAcquireRead(ownerId, requestId, leasePolicy)

        fun tryAcquireAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): CompletableFuture<LockAcquireResult<ReadLockHandle>> =
            client.tryAcquireReadAsync(ownerId, requestId, leasePolicy)

        fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<ReadLockHandle> =
            client.acquireRead(ownerId, requestId, waitTime, leasePolicy)

        fun acquireAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): CompletableFuture<LockAcquireResult<ReadLockHandle>> =
            client.acquireReadAsync(ownerId, requestId, waitTime, leasePolicy)

        fun inspect(handle: ReadLockHandle): LockInspectResult<ReadLockHandle> =
            client.inspectRead(handle)

        fun inspectAsync(handle: ReadLockHandle): CompletableFuture<LockInspectResult<ReadLockHandle>> =
            client.inspectReadAsync(handle)

        fun reconcile(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): LockReconcileResult<ReadLockHandle> =
            client.reconcileRead(ownerId, requestId)

        fun reconcileAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): CompletableFuture<LockReconcileResult<ReadLockHandle>> =
            client.reconcileReadAsync(ownerId, requestId)

        fun renew(
            handle: ReadLockHandle,
            extension: Duration,
        ): LockMutationResult<ReadLockHandle> =
            client.renewRead(handle, extension)

        fun renewAsync(
            handle: ReadLockHandle,
            extension: Duration,
        ): CompletableFuture<LockMutationResult<ReadLockHandle>> =
            client.renewReadAsync(handle, extension)

        fun release(handle: ReadLockHandle): LockMutationResult<ReadLockHandle> =
            client.releaseRead(handle)

        fun releaseAsync(handle: ReadLockHandle): CompletableFuture<LockMutationResult<ReadLockHandle>> =
            client.releaseReadAsync(handle)
    }

    class WriteLockView internal constructor(
        private val client: ReadWriteLockClient,
    ) {
        fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<WriteLockHandle> =
            client.tryAcquireWrite(ownerId, requestId, leasePolicy)

        fun tryAcquireAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): CompletableFuture<LockAcquireResult<WriteLockHandle>> =
            client.tryAcquireWriteAsync(ownerId, requestId, leasePolicy)

        fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<WriteLockHandle> =
            client.acquireWrite(ownerId, requestId, waitTime, leasePolicy)

        fun acquireAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): CompletableFuture<LockAcquireResult<WriteLockHandle>> =
            client.acquireWriteAsync(ownerId, requestId, waitTime, leasePolicy)

        fun inspect(handle: WriteLockHandle): LockInspectResult<WriteLockHandle> =
            client.inspectWrite(handle)

        fun inspectAsync(handle: WriteLockHandle): CompletableFuture<LockInspectResult<WriteLockHandle>> =
            client.inspectWriteAsync(handle)

        fun reconcile(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): LockReconcileResult<WriteLockHandle> =
            client.reconcileWrite(ownerId, requestId)

        fun reconcileAsync(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): CompletableFuture<LockReconcileResult<WriteLockHandle>> =
            client.reconcileWriteAsync(ownerId, requestId)

        fun renew(
            handle: WriteLockHandle,
            extension: Duration,
        ): LockMutationResult<WriteLockHandle> =
            client.renewWrite(handle, extension)

        fun renewAsync(
            handle: WriteLockHandle,
            extension: Duration,
        ): CompletableFuture<LockMutationResult<WriteLockHandle>> =
            client.renewWriteAsync(handle, extension)

        fun release(handle: WriteLockHandle): LockMutationResult<WriteLockHandle> =
            client.releaseWrite(handle)

        fun releaseAsync(handle: WriteLockHandle): CompletableFuture<LockMutationResult<WriteLockHandle>> =
            client.releaseWriteAsync(handle)
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceReadWriteLock =
            create(connection, name, ReadWriteLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
        ): LettuceReadWriteLock =
            LettuceReadWriteLock(ReadWriteLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceReadWriteLock =
            create(connection, name, ReadWriteLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
        ): LettuceReadWriteLock =
            LettuceReadWriteLock(ReadWriteLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceReadWriteLock =
            LettuceReadWriteLock(
                ReadWriteLockClient.create(connection, name, config, scheduler, observationSink),
            )

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceReadWriteLock =
            LettuceReadWriteLock(
                ReadWriteLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
