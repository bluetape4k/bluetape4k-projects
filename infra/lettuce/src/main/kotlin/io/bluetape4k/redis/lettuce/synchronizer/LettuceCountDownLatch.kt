package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.synchronizer.internal.LatchClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture

/** Redis-authoritative count-down latch with monotonic lifecycle generations. */
class LettuceCountDownLatch internal constructor(
    private val client: LatchClient,
): AutoCloseable {

    /** Creates a new generation or reports the currently active generation. */
    fun trySetCount(count: Long, requestId: LatchRequestId): LatchSetCountResult =
        client.trySetCount(count, requestId)

    /** Asynchronously creates a new generation or reports the active generation. */
    fun trySetCountAsync(count: Long, requestId: LatchRequestId): CompletableFuture<LatchSetCountResult> =
        client.trySetCountAsync(count, requestId)

    /** Reads the count for the observed [generation]. */
    fun getCount(generation: LatchGeneration): LatchCountResult =
        client.getCount(generation)

    /** Asynchronously reads the count for the observed [generation]. */
    fun getCountAsync(generation: LatchGeneration): CompletableFuture<LatchCountResult> =
        client.getCountAsync(generation)

    /** Inspects count, completion, and waiter state for [generation]. */
    fun inspect(generation: LatchGeneration): LatchCountResult =
        client.getCount(generation)

    /** Asynchronously inspects [generation]. */
    fun inspectAsync(generation: LatchGeneration): CompletableFuture<LatchCountResult> =
        client.getCountAsync(generation)

    /** Decrements [generation] exactly once for [requestId]. */
    fun countDown(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult =
        client.countDown(generation, requestId)

    /** Asynchronously decrements [generation] exactly once for [requestId]. */
    fun countDownAsync(generation: LatchGeneration, requestId: LatchRequestId): CompletableFuture<LatchMutationResult> =
        client.countDownAsync(generation, requestId)

    /** Blocks until [generation] completes or [waitTime] elapses. */
    fun await(generation: LatchGeneration, requestId: LatchRequestId, waitTime: Duration): LatchAwaitResult =
        client.await(generation, requestId, waitTime)

    /** Waits asynchronously; cancellation completes only after waiter cleanup. */
    fun awaitAsync(
        generation: LatchGeneration,
        requestId: LatchRequestId,
        waitTime: Duration
    ): CompletableFuture<LatchAwaitResult> =
        client.awaitAsync(generation, requestId, waitTime)

    /** Deletes [generation] only when no live waiter remains. */
    fun delete(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult =
        client.delete(generation, requestId)

    /** Asynchronously deletes [generation] only when no live waiter remains. */
    fun deleteAsync(generation: LatchGeneration, requestId: LatchRequestId): CompletableFuture<LatchMutationResult> =
        client.deleteAsync(generation, requestId)

    /** Closes this client view and completes pending waits with `Closed`. */
    override fun close() = client.close()

    companion object: KLogging() {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String
        ): LettuceCountDownLatch =
            create(connection, name, LatchConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LatchConfig,
        ): LettuceCountDownLatch =
            LettuceCountDownLatch(LatchClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String
        ): LettuceCountDownLatch =
            create(connection, name, LatchConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LatchConfig,
        ): LettuceCountDownLatch =
            LettuceCountDownLatch(LatchClient.create(connection, name, config))
    }
}
