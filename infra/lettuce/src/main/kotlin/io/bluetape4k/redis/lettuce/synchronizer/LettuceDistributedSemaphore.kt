package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.synchronizer.internal.SemaphoreClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture

/** Redis-authoritative counting semaphore with request-idempotent permit handles. */
class LettuceDistributedSemaphore internal constructor(
    private val client: SemaphoreClient,
): AutoCloseable {
    /** Initializes capacity once without resetting an existing semaphore. */
    fun trySetPermits(permits: Int): SemaphoreInitializationResult = client.trySetPermits(permits)
    /** Asynchronously initializes capacity once. */
    fun trySetPermitsAsync(permits: Int): CompletableFuture<SemaphoreInitializationResult> =
        client.trySetPermitsAsync(permits)
    /** Returns the Redis-authoritative number of available permits, or `-1` when unavailable. */
    fun availablePermits(): Int = client.availablePermits()
    /** Asynchronously returns the Redis-authoritative available permit count. */
    fun availablePermitsAsync(): CompletableFuture<Int> = client.availablePermitsAsync()
    /** Attempts one request-idempotent acquisition without waiting. */
    fun tryAcquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquire(ownerId, requestId, permits)
    /** Asynchronously attempts one request-idempotent acquisition without waiting. */
    fun tryAcquireAsync(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquireAsync(ownerId, requestId, permits)
    /** Blocks for at most [waitTime] while trying to acquire [permits]. */
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
    /** Inspects a handle against Redis-authoritative ownership state. */
    fun inspect(handle: PermitHandle) = client.inspect(handle)
    /** Asynchronously inspects a handle. */
    fun inspectAsync(handle: PermitHandle) = client.inspectAsync(handle)
    /** Reconciles an ambiguous acquisition using its original identity. */
    fun reconcile(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) = client.reconcile(ownerId, requestId)
    /** Asynchronously reconciles an ambiguous acquisition. */
    fun reconcileAsync(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) =
        client.reconcileAsync(ownerId, requestId)
    /** Releases exactly the permits represented by [handle]. */
    fun release(handle: PermitHandle) = client.release(handle)
    /** Asynchronously releases [handle]. */
    fun releaseAsync(handle: PermitHandle) = client.releaseAsync(handle)
    /** Closes this client view and terminates pending asynchronous acquisitions. */
    override fun close() = client.close()

    companion object {
        @JvmStatic fun create(connection: StatefulRedisConnection<String, String>, name: String) =
            create(connection, name, SemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ) = LettuceDistributedSemaphore(SemaphoreClient.create(connection, name, config))
        @JvmStatic fun create(connection: StatefulRedisClusterConnection<String, String>, name: String) =
            create(connection, name, SemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ) = LettuceDistributedSemaphore(SemaphoreClient.create(connection, name, config))
    }
}
