package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.synchronizer.internal.ExpirableSemaphoreClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration

/** Redis-time-based semaphore whose individual permit units expire atomically by allocation. */
class LettucePermitExpirableSemaphore internal constructor(
    private val client: ExpirableSemaphoreClient,
): AutoCloseable {
    /** Initializes capacity once without resetting existing allocations. */
    fun trySetPermits(permits: Int) = client.trySetPermits(permits)
    /** Asynchronously initializes capacity once. */
    fun trySetPermitsAsync(permits: Int) = client.trySetPermitsAsync(permits)
    /** Runs bounded expiry cleanup and returns the Redis-authoritative available count. */
    fun availablePermits() = client.availablePermits()
    /** Asynchronously runs bounded expiry cleanup and returns available permits. */
    fun availablePermitsAsync() = client.availablePermitsAsync()
    /** Attempts one request-idempotent allocation with one Redis deadline per permit unit. */
    fun tryAcquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquire(ownerId, requestId, permits)
    /** Asynchronously attempts one expirable allocation without waiting. */
    fun tryAcquireAsync(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquireAsync(ownerId, requestId, permits)
    /** Blocks for at most [waitTime] while trying to allocate [permits]. */
    fun acquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ) = client.acquire(ownerId, requestId, permits, waitTime)
    /** Waits asynchronously on the connection-owned bounded coordination runtime. */
    fun acquireAsync(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ) = client.acquireAsync(ownerId, requestId, permits, waitTime)
    /** Inspects ownership and expiry using Redis server time. */
    fun inspect(handle: ExpirablePermitHandle) = client.inspect(handle)
    /** Asynchronously inspects ownership and expiry using Redis server time. */
    fun inspectAsync(handle: ExpirablePermitHandle) = client.inspectAsync(handle)
    /** Reconciles an ambiguous allocation using its original request identity. */
    fun reconcile(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) =
        client.reconcile(ownerId, requestId)
    /** Asynchronously reconciles an ambiguous allocation. */
    fun reconcileAsync(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) =
        client.reconcileAsync(ownerId, requestId)
    /** Atomically releases the complete allocation represented by [handle]. */
    fun release(handle: ExpirablePermitHandle) = client.release(handle)
    /** Asynchronously releases [handle]. */
    fun releaseAsync(handle: ExpirablePermitHandle) = client.releaseAsync(handle)
    /** Renews every unit lease in [handle] by [extension] using Redis server time. */
    fun renew(handle: ExpirablePermitHandle, extension: Duration) = client.renew(handle, extension)
    /** Asynchronously renews every unit lease in [handle]. */
    fun renewAsync(handle: ExpirablePermitHandle, extension: Duration) = client.renewAsync(handle, extension)
    /** Closes this client view and terminates pending asynchronous acquisitions. */
    override fun close() = client.close()

    companion object {
        @JvmStatic fun create(connection: StatefulRedisConnection<String, String>, name: String) =
            create(connection, name, ExpirableSemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ) = LettucePermitExpirableSemaphore(ExpirableSemaphoreClient.create(connection, name, config))
        @JvmStatic fun create(connection: StatefulRedisClusterConnection<String, String>, name: String) =
            create(connection, name, ExpirableSemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ) = LettucePermitExpirableSemaphore(ExpirableSemaphoreClient.create(connection, name, config))
    }
}
