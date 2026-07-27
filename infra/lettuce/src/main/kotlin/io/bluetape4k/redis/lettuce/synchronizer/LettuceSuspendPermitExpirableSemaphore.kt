package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.synchronizer.internal.ExpirableSemaphoreClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.future.await
import java.time.Duration

/** Cancellable suspending adapter for [LettucePermitExpirableSemaphore]. */
class LettuceSuspendPermitExpirableSemaphore internal constructor(
    private val client: ExpirableSemaphoreClient,
): AutoCloseable {
    /** Initializes capacity once without resetting existing allocations. */
    suspend fun trySetPermits(permits: Int) = client.trySetPermitsAsync(permits).await()
    /** Runs bounded expiry cleanup and returns available permits. */
    suspend fun availablePermits() = client.availablePermitsAsync().await()
    /** Attempts one request-idempotent allocation without waiting. */
    suspend fun tryAcquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquireSuspending(ownerId, requestId, permits)
    /** Waits cancellably for at most [waitTime] while trying to allocate [permits]. */
    suspend fun acquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ) = client.acquireSuspending(ownerId, requestId, permits, waitTime)
    /** Inspects ownership and expiry using Redis server time. */
    suspend fun inspect(handle: ExpirablePermitHandle) = client.inspectSuspending(handle)
    /** Reconciles an ambiguous allocation using its original request identity. */
    suspend fun reconcile(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) =
        client.reconcileAsync(ownerId, requestId).await()
    /** Atomically releases the complete allocation represented by [handle]. */
    suspend fun release(handle: ExpirablePermitHandle) = client.releaseSuspending(handle)
    /** Renews every unit lease in [handle] by [extension] using Redis server time. */
    suspend fun renew(handle: ExpirablePermitHandle, extension: Duration) =
        client.renewSuspending(handle, extension)
    /** Closes this client view and terminates pending waits. */
    override fun close() = client.close()

    companion object {
        @JvmStatic fun create(connection: StatefulRedisConnection<String, String>, name: String) =
            create(connection, name, ExpirableSemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ) = LettuceSuspendPermitExpirableSemaphore(ExpirableSemaphoreClient.create(connection, name, config))
        @JvmStatic fun create(connection: StatefulRedisClusterConnection<String, String>, name: String) =
            create(connection, name, ExpirableSemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ) = LettuceSuspendPermitExpirableSemaphore(ExpirableSemaphoreClient.create(connection, name, config))
    }
}
