package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService

/**
 * A logical-owner, request-idempotent Redis distributed lock.
 *
 * Every successful acquisition returns one request-bound [LockHandle]. Reusing the same request ID replays that hold;
 * a different request ID under the same owner adds one reentrant hold without changing the Redis generation.
 */
class LettuceDistributedLock private constructor(
    private val client: DistributedLockClient,
) : AutoCloseable {

    /** Attempts one immediate request-bound acquisition. */
    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.tryAcquire(ownerId, requestId, leasePolicy)

    /** Attempts one immediate request-bound acquisition asynchronously. */
    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.tryAcquireAsync(ownerId, requestId, leasePolicy)

    /** Waits up to [waitTime] while preserving the supplied owner and request identity. */
    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.acquire(ownerId, requestId, waitTime, leasePolicy)

    /** Waits asynchronously up to [waitTime] without blocking a scheduler thread. */
    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.acquireAsync(ownerId, requestId, waitTime, leasePolicy)

    /** Inspects one generation- and request-bound handle. */
    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> =
        client.inspect(handle)

    /** Inspects one generation- and request-bound handle asynchronously. */
    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> =
        client.inspectAsync(handle)

    /** Reconciles one acquisition request after an ambiguous dispatch. */
    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> =
        client.reconcile(ownerId, requestId)

    /** Reconciles one acquisition request asynchronously after an ambiguous dispatch. */
    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        client.reconcileAsync(ownerId, requestId)

    /** Replaces the active Redis TTL with [extension]. */
    fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> =
        client.renew(handle, extension)

    /** Replaces the active Redis TTL asynchronously with [extension]. */
    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> =
        client.renewAsync(handle, extension)

    /** Releases exactly the request-bound hold represented by [handle]. */
    fun release(handle: LockHandle): LockMutationResult<LockHandle> =
        client.release(handle)

    /** Releases exactly the request-bound hold represented by [handle] asynchronously. */
    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> =
        client.releaseAsync(handle)

    /** Closes this lock object without closing its Redis connection or an injected scheduler. */
    override fun close() {
        client.close()
    }

    companion object {
        /** Creates a standalone lock with [LockConfig] defaults. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceDistributedLock =
            create(connection, name, LockConfig())

        /** Creates a standalone lock with explicit configuration. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LockConfig,
        ): LettuceDistributedLock =
            LettuceDistributedLock(DistributedLockClient.create(connection, name, config))

        /** Creates a Redis Cluster lock with [LockConfig] defaults. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceDistributedLock =
            create(connection, name, LockConfig())

        /** Creates a Redis Cluster lock with explicit configuration. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LockConfig,
        ): LettuceDistributedLock =
            LettuceDistributedLock(DistributedLockClient.create(connection, name, config))

        /** Creates a standalone lock that uses but never shuts down [scheduler]. */
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceDistributedLock =
            LettuceDistributedLock(
                DistributedLockClient.create(connection, name, config, scheduler, observationSink),
            )

        /** Creates a Redis Cluster lock that uses but never shuts down [scheduler]. */
        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceDistributedLock =
            LettuceDistributedLock(
                DistributedLockClient.create(connection, name, config, scheduler, observationSink),
            )
    }
}
