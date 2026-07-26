package io.bluetape4k.redis.lettuce.lock

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

/** Suspending adapter for the bounded scheduled-retry [LettuceSpinLock] policy. */
class LettuceSuspendSpinLock internal constructor(
    private val client: SpinLockClient,
): AutoCloseable {

    suspend fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.tryAcquireSuspending(ownerId, requestId, leasePolicy)
    }

    suspend fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.acquireSuspending(ownerId, requestId, waitTime, leasePolicy)

    suspend fun inspect(handle: LockHandle): LockInspectResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.inspectSuspending(handle)
    }

    suspend fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.reconcileSuspending(ownerId, requestId)
    }

    suspend fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.renewSuspending(handle, extension)
    }

    suspend fun release(handle: LockHandle): LockMutationResult<LockHandle> {
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
            name: String,
        ): LettuceSuspendSpinLock =
            create(connection, name, SpinLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SpinLockConfig,
        ): LettuceSuspendSpinLock =
            LettuceSuspendSpinLock(SpinLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceSuspendSpinLock =
            create(connection, name, SpinLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SpinLockConfig,
        ): LettuceSuspendSpinLock =
            LettuceSuspendSpinLock(SpinLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendSpinLock =
            LettuceSuspendSpinLock(SpinLockClient.create(connection, name, config, scheduler, observationSink))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendSpinLock =
            LettuceSuspendSpinLock(SpinLockClient.create(connection, name, config, scheduler, observationSink))
    }
}
