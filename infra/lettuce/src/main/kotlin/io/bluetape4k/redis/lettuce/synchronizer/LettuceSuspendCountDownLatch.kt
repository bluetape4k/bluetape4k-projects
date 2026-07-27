package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.redis.lettuce.synchronizer.internal.LatchClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.future.await
import java.time.Duration

/** Cancellable suspending adapter for [LettuceCountDownLatch]. */
class LettuceSuspendCountDownLatch internal constructor(
    private val client: LatchClient,
): AutoCloseable {
    /** Creates a new generation or reports the currently active generation. */
    suspend fun trySetCount(count: Long, requestId: LatchRequestId) =
        client.trySetCountAsync(count, requestId).await()
    /** Reads the count for the observed [generation]. */
    suspend fun getCount(generation: LatchGeneration) = client.getCountAsync(generation).await()
    /** Inspects count, completion, and waiter state for [generation]. */
    suspend fun inspect(generation: LatchGeneration) = client.getCountAsync(generation).await()
    /** Decrements [generation] exactly once for [requestId]. */
    suspend fun countDown(generation: LatchGeneration, requestId: LatchRequestId) =
        client.countDownAsync(generation, requestId).await()
    /** Waits cancellably without mutating the count and durably removes its waiter. */
    suspend fun await(generation: LatchGeneration, requestId: LatchRequestId, waitTime: Duration) =
        client.awaitSuspending(generation, requestId, waitTime)
    /** Deletes [generation] only when no live waiter remains. */
    suspend fun delete(generation: LatchGeneration, requestId: LatchRequestId) =
        client.deleteAsync(generation, requestId).await()
    /** Closes this client view and terminates its pending waits. */
    override fun close() = client.close()

    companion object {
        @JvmStatic fun create(connection: StatefulRedisConnection<String, String>, name: String) =
            create(connection, name, LatchConfig())
        @JvmStatic fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LatchConfig,
        ) = LettuceSuspendCountDownLatch(LatchClient.create(connection, name, config))
        @JvmStatic fun create(connection: StatefulRedisClusterConnection<String, String>, name: String) =
            create(connection, name, LatchConfig())
        @JvmStatic fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LatchConfig,
        ) = LettuceSuspendCountDownLatch(LatchClient.create(connection, name, config))
    }
}
