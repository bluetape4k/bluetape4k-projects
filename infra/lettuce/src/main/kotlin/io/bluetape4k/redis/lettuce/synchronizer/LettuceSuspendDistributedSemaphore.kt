package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.synchronizer.internal.SemaphoreClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.future.await
import java.time.Duration

/** Cancellable suspending adapter for [LettuceDistributedSemaphore]. */
class LettuceSuspendDistributedSemaphore internal constructor(
    private val client: SemaphoreClient,
): AutoCloseable {
    /** Initializes capacity once without resetting an existing semaphore. */
    suspend fun trySetPermits(permits: Int): SemaphoreInitializationResult =
        client.trySetPermitsAsync(permits).await()
    /** Returns the Redis-authoritative available permit count. */
    suspend fun availablePermits(): Int = client.availablePermitsAsync().await()
    /** Attempts one request-idempotent acquisition without waiting. */
    suspend fun tryAcquire(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId, permits: Int = 1) =
        client.tryAcquireAsync(ownerId, requestId, permits).await()
    /** Waits cancellably for at most [waitTime] while trying to acquire [permits]. */
    suspend fun acquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ) = client.acquireSuspending(ownerId, requestId, permits, waitTime)
    /** Inspects a handle against Redis-authoritative ownership state. */
    suspend fun inspect(handle: PermitHandle): PermitInspectResult<PermitHandle> {
        currentCoroutineContext().ensureActive()
        return client.inspectAsync(handle).await()
    }
    /** Reconciles an ambiguous acquisition using its original identity. */
    suspend fun reconcile(ownerId: SemaphoreOwnerId, requestId: SemaphoreRequestId) =
        client.reconcileAsync(ownerId, requestId).await()
    /** Releases exactly the permits represented by [handle]. */
    suspend fun release(handle: PermitHandle): PermitMutationResult<PermitHandle> {
        currentCoroutineContext().ensureActive()
        return client.releaseAsync(handle).await()
    }
    /** Closes this client view and terminates pending waits. */
    override fun close() = client.close()

    companion object {
        @JvmStatic fun create(connection: StatefulRedisConnection<String, String>, name: String) =
            create(connection, name, SemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ) = LettuceSuspendDistributedSemaphore(SemaphoreClient.create(connection, name, config))
        @JvmStatic fun create(connection: StatefulRedisClusterConnection<String, String>, name: String) =
            create(connection, name, SemaphoreConfig())
        @JvmStatic fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ) = LettuceSuspendDistributedSemaphore(SemaphoreClient.create(connection, name, config))
    }
}
