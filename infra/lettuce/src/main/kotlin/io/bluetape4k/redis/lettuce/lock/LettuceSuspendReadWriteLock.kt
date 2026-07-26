package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.ReadWriteLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

/** Suspending adapter for [LettuceReadWriteLock]'s phase-fair Redis state machine. */
class LettuceSuspendReadWriteLock internal constructor(
    private val client: ReadWriteLockClient,
) : AutoCloseable {

    private val readView = ReadLockView(client)
    private val writeView = WriteLockView(client)

    fun readLock(): ReadLockView = readView

    fun writeLock(): WriteLockView = writeView

    suspend fun downgrade(handle: WriteLockHandle): DowngradeResult {
        currentCoroutineContext().ensureActive()
        return client.downgradeSuspending(handle)
    }

    override fun close() {
        client.close()
    }

    class ReadLockView internal constructor(
        private val client: ReadWriteLockClient,
    ) {
        suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<ReadLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.tryAcquireReadSuspending(ownerId, requestId, leasePolicy)
        }

        suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<ReadLockHandle> =
            client.acquireReadSuspending(ownerId, requestId, waitTime, leasePolicy)

        suspend fun inspect(handle: ReadLockHandle): LockInspectResult<ReadLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.inspectReadSuspending(handle)
        }

        suspend fun reconcile(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): LockReconcileResult<ReadLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.reconcileReadSuspending(ownerId, requestId)
        }

        suspend fun renew(
            handle: ReadLockHandle,
            extension: Duration,
        ): LockMutationResult<ReadLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.renewReadSuspending(handle, extension)
        }

        suspend fun release(handle: ReadLockHandle): LockMutationResult<ReadLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.releaseReadSuspending(handle)
        }
    }

    class WriteLockView internal constructor(
        private val client: ReadWriteLockClient,
    ) {
        suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<WriteLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.tryAcquireWriteSuspending(ownerId, requestId, leasePolicy)
        }

        suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ): LockAcquireResult<WriteLockHandle> =
            client.acquireWriteSuspending(ownerId, requestId, waitTime, leasePolicy)

        suspend fun inspect(handle: WriteLockHandle): LockInspectResult<WriteLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.inspectWriteSuspending(handle)
        }

        suspend fun reconcile(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
        ): LockReconcileResult<WriteLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.reconcileWriteSuspending(ownerId, requestId)
        }

        suspend fun renew(
            handle: WriteLockHandle,
            extension: Duration,
        ): LockMutationResult<WriteLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.renewWriteSuspending(handle, extension)
        }

        suspend fun release(handle: WriteLockHandle): LockMutationResult<WriteLockHandle> {
            currentCoroutineContext().ensureActive()
            return client.releaseWriteSuspending(handle)
        }
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceSuspendReadWriteLock =
            create(connection, name, ReadWriteLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
        ): LettuceSuspendReadWriteLock =
            LettuceSuspendReadWriteLock(ReadWriteLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceSuspendReadWriteLock =
            create(connection, name, ReadWriteLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
        ): LettuceSuspendReadWriteLock =
            LettuceSuspendReadWriteLock(ReadWriteLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendReadWriteLock =
            LettuceSuspendReadWriteLock(
                ReadWriteLockClient.create(connection, name, config, scheduler, observationSink),
            )

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ReadWriteLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendReadWriteLock =
            LettuceSuspendReadWriteLock(
                ReadWriteLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
