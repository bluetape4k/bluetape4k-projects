package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.FencedLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

/**
 * Suspending adapter for [LettuceFencedLock].
 *
 * Cancellation is propagated to the Redis future. Fencing still requires downstream strict-greater validation.
 */
class LettuceSuspendFencedLock internal constructor(
    private val client: FencedLockClient,
) : AutoCloseable {

    /** Explicitly initializes this epoch's persistent fencing counter. This operation is safe to retry. */
    suspend fun bootstrapFencing(): FencedBootstrapResult {
        currentCoroutineContext().ensureActive()
        return client.bootstrapFencingSuspending()
    }

    suspend fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.tryAcquireSuspending(ownerId, requestId, leasePolicy)
    }

    suspend fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<FencedLockHandle> =
        client.acquireSuspending(ownerId, requestId, waitTime, leasePolicy)

    suspend fun inspect(handle: FencedLockHandle): LockInspectResult<FencedLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.inspectSuspending(handle)
    }

    suspend fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<FencedLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.reconcileSuspending(ownerId, requestId)
    }

    suspend fun renew(
        handle: FencedLockHandle,
        extension: Duration,
    ): LockMutationResult<FencedLockHandle> {
        currentCoroutineContext().ensureActive()
        return client.renewSuspending(handle, extension)
    }

    suspend fun release(handle: FencedLockHandle): LockMutationResult<FencedLockHandle> {
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
            config: FencedLockConfig,
        ): LettuceSuspendFencedLock =
            LettuceSuspendFencedLock(FencedLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FencedLockConfig,
        ): LettuceSuspendFencedLock =
            LettuceSuspendFencedLock(FencedLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendFencedLock =
            LettuceSuspendFencedLock(
                FencedLockClient.create(connection, name, config, scheduler, observationSink),
            )

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: FencedLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendFencedLock =
            LettuceSuspendFencedLock(
                FencedLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
