package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService

/**
 * Suspending adapter for the logical-owner, request-idempotent Redis distributed lock.
 *
 * Coroutine cancellation is propagated and the same owner/request pair remains the reconciliation identity after an
 * ambiguous Redis mutation.
 */
class LettuceSuspendDistributedLock internal constructor(
    private val client: DistributedLockClient,
) : AutoCloseable {

    /** Attempts one immediate request-bound acquisition. */
    suspend fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.tryAcquireSuspending(ownerId, requestId, leasePolicy)
    }

    /** Waits up to [waitTime] using cancellable coroutine delay between attempts. */
    suspend fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.acquireSuspending(ownerId, requestId, waitTime, leasePolicy)

    /** Inspects one generation- and request-bound handle. */
    suspend fun inspect(handle: LockHandle): LockInspectResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.inspectSuspending(handle)
    }

    /** Reconciles one acquisition request after an ambiguous dispatch. */
    suspend fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.reconcileSuspending(ownerId, requestId)
    }

    /** Replaces the active Redis TTL with [extension]. */
    suspend fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.renewSuspending(handle, extension)
    }

    /** Releases exactly the request-bound hold represented by [handle]. */
    suspend fun release(handle: LockHandle): LockMutationResult<LockHandle> {
        currentCoroutineContext().ensureActive()
        return client.releaseSuspending(handle)
    }

    /** Closes this lock object without closing its Redis connection or an injected scheduler. */
    override fun close() {
        client.close()
    }

    companion object {
        /** Creates a standalone suspending lock with [LockConfig] defaults. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceSuspendDistributedLock =
            create(connection, name, LockConfig())

        /** Creates a standalone suspending lock with explicit configuration. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LockConfig,
        ): LettuceSuspendDistributedLock =
            LettuceSuspendDistributedLock(DistributedLockClient.create(connection, name, config))

        /** Creates a Redis Cluster suspending lock with [LockConfig] defaults. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceSuspendDistributedLock =
            create(connection, name, LockConfig())

        /** Creates a Redis Cluster suspending lock with explicit configuration. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LockConfig,
        ): LettuceSuspendDistributedLock =
            LettuceSuspendDistributedLock(DistributedLockClient.create(connection, name, config))

        /** Creates a standalone suspending lock that uses but never shuts down [scheduler]. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendDistributedLock =
            LettuceSuspendDistributedLock(
                DistributedLockClient.create(connection, name, config, scheduler, observationSink),
            )

        /** Creates a Redis Cluster suspending lock that uses but never shuts down [scheduler]. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSuspendDistributedLock =
            LettuceSuspendDistributedLock(
                DistributedLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
