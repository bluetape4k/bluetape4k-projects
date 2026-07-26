package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.MultiLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

/** Suspending adapter for the same-slot all-or-nothing [LettuceMultiLock]. */
class LettuceSuspendMultiLock internal constructor(
    private val client: MultiLockClient,
): AutoCloseable {

    suspend fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.tryAcquireSuspending(ownerId, requestId, leasePolicy)
    }

    suspend fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> =
        client.acquireSuspending(ownerId, requestId, waitTime, leasePolicy)

    suspend fun inspect(handle: MultiLockHandle): LockInspectResult<MultiLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.inspectSuspending(handle)
    }

    suspend fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<MultiLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.reconcileSuspending(ownerId, requestId)
    }

    suspend fun renew(
        handle: MultiLockHandle,
        extension: Duration,
    ): LockMutationResult<MultiLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.renewSuspending(handle, extension)
    }

    suspend fun release(handle: MultiLockHandle): LockMutationResult<MultiLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.releaseSuspending(handle)
    }

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
        ): LettuceSuspendMultiLock =
            create(connection, names, MultiLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
        ): LettuceSuspendMultiLock =
            LettuceSuspendMultiLock(MultiLockClient.create(connection, names, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
        ): LettuceSuspendMultiLock =
            create(connection, names, MultiLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
        ): LettuceSuspendMultiLock =
            LettuceSuspendMultiLock(MultiLockClient.create(connection, names, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendMultiLock =
            LettuceSuspendMultiLock(MultiLockClient.create(connection, names, config, scheduler, observationSink))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendMultiLock =
            LettuceSuspendMultiLock(MultiLockClient.create(connection, names, config, scheduler, observationSink))
    }
}
