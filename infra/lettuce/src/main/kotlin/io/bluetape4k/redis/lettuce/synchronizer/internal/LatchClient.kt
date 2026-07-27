package io.bluetape4k.redis.lettuce.synchronizer.internal

import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.synchronizer.LatchAwaitResult
import io.bluetape4k.redis.lettuce.synchronizer.LatchConfig
import io.bluetape4k.redis.lettuce.synchronizer.LatchCountResult
import io.bluetape4k.redis.lettuce.synchronizer.LatchGeneration
import io.bluetape4k.redis.lettuce.synchronizer.LatchMutationResult
import io.bluetape4k.redis.lettuce.synchronizer.LatchRequestId
import io.bluetape4k.redis.lettuce.synchronizer.LatchScripts
import io.bluetape4k.redis.lettuce.synchronizer.LatchSetCountResult
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerRecoveryAction
import io.bluetape4k.redis.lettuce.synchronizer.requirePositive
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.concurrent.atomic.AtomicBoolean

internal class LatchClient private constructor(
    private val keys: LatchKeys,
    private val config: LatchConfig,
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
    private val poller: SynchronizerAsyncPoller,
) {
    private val closed = AtomicBoolean()

    fun trySetCount(count: Long, requestId: LatchRequestId): LatchSetCountResult {
        if (count !in 0..config.maxCount) return LatchSetCountResult.InvalidCount
        if (closed.get()) return LatchSetCountResult.Closed
        return try {
            decodeSet(run(LatchScripts.TRY_SET_COUNT_SCRIPT, count.toString(), requestId.value))
        } catch (error: Exception) {
            LatchSetCountResult.BackendFailure(backend(error))
        }
    }

    fun trySetCountAsync(count: Long, requestId: LatchRequestId): CompletableFuture<LatchSetCountResult> {
        if (count !in 0..config.maxCount) return CompletableFuture.completedFuture(LatchSetCountResult.InvalidCount)
        if (closed.get()) return CompletableFuture.completedFuture(LatchSetCountResult.Closed)
        return runAsync(LatchScripts.TRY_SET_COUNT_SCRIPT, count.toString(), requestId.value)
            .handle { raw, error ->
                if (error != null) LatchSetCountResult.BackendFailure(backend(error)) else decodeSet(raw)
            }
    }

    fun getCount(generation: LatchGeneration): LatchCountResult {
        if (closed.get()) return LatchCountResult.Closed
        return try {
            decodeCount(run(LatchScripts.GET_COUNT_SCRIPT, generation.value.toString()))
        } catch (error: Exception) {
            LatchCountResult.BackendFailure(backend(error))
        }
    }

    fun getCountAsync(generation: LatchGeneration): CompletableFuture<LatchCountResult> {
        if (closed.get()) return CompletableFuture.completedFuture(LatchCountResult.Closed)
        return runAsync(LatchScripts.GET_COUNT_SCRIPT, generation.value.toString()).handle { raw, error ->
            if (error != null) LatchCountResult.BackendFailure(backend(error)) else decodeCount(raw)
        }
    }

    fun countDown(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult {
        if (closed.get()) return LatchMutationResult.Closed
        return try {
            decodeMutation(
                run(LatchScripts.COUNT_DOWN_SCRIPT, generation.value.toString(), requestId.value),
            )
        } catch (error: Exception) {
            ambiguousMutation(error, requestId)
        }
    }

    fun countDownAsync(
        generation: LatchGeneration,
        requestId: LatchRequestId,
    ): CompletableFuture<LatchMutationResult> {
        if (closed.get()) return CompletableFuture.completedFuture(LatchMutationResult.Closed)
        return runAsync(LatchScripts.COUNT_DOWN_SCRIPT, generation.value.toString(), requestId.value)
            .handle { raw, error ->
                if (error != null) ambiguousMutation(error, requestId) else decodeMutation(raw)
            }
    }

    fun delete(generation: LatchGeneration, requestId: LatchRequestId): LatchMutationResult {
        if (closed.get()) return LatchMutationResult.Closed
        return try {
            decodeMutation(run(LatchScripts.DELETE_SCRIPT, generation.value.toString(), requestId.value))
        } catch (error: Exception) {
            ambiguousMutation(error, requestId)
        }
    }

    fun deleteAsync(
        generation: LatchGeneration,
        requestId: LatchRequestId,
    ): CompletableFuture<LatchMutationResult> {
        if (closed.get()) return CompletableFuture.completedFuture(LatchMutationResult.Closed)
        return runAsync(LatchScripts.DELETE_SCRIPT, generation.value.toString(), requestId.value)
            .handle { raw, error ->
                if (error != null) ambiguousMutation(error, requestId) else decodeMutation(raw)
            }
    }

    fun await(
        generation: LatchGeneration,
        requestId: LatchRequestId,
        waitTime: Duration,
    ): LatchAwaitResult {
        validateWait(waitTime)
        if (closed.get()) return LatchAwaitResult.Closed
        val registered = try {
            decodeAwait(
                run(
                    LatchScripts.REGISTER_WAITER_SCRIPT,
                    generation.value.toString(),
                    requestId.value,
                    waitTime.toMillis().toString(),
                    config.maxWaiters.toString(),
                    config.waiterCleanupGrace.toMillis().toString(),
                ),
            )
        } catch (error: Exception) {
            return ambiguousAwait(error, requestId)
        }
        if (registered != null) return registered
        val outcome = try {
            val deadline = System.nanoTime() + waitTime.toNanos()
            var result: LatchAwaitResult? = null
            do {
                when (val count = getCount(generation)) {
                    is LatchCountResult.Active -> {
                        // Keep polling while this generation remains active.
                    }
                    is LatchCountResult.Completed -> result = LatchAwaitResult.Completed
                    LatchCountResult.Deleted -> result = LatchAwaitResult.Deleted
                    LatchCountResult.StaleGeneration -> result = LatchAwaitResult.StaleGeneration
                    LatchCountResult.Closed -> result = LatchAwaitResult.Closed
                    is LatchCountResult.BackendFailure -> result = LatchAwaitResult.BackendFailure(count.failure)
                    is LatchCountResult.IntegrityFailure -> result = LatchAwaitResult.IntegrityFailure(count.failure)
                }
                if (result != null) break
                Thread.sleep(config.pollInterval.toMillis().coerceAtLeast(1))
            } while (System.nanoTime() < deadline)
            result ?: LatchAwaitResult.TimedOut
        } catch (error: Exception) {
            ambiguousAwait(error, requestId)
        }
        return try {
            run(LatchScripts.UNREGISTER_WAITER_SCRIPT, generation.value.toString(), requestId.value)
            outcome
        } catch (error: Exception) {
            ambiguousAwait(error, requestId)
        }
    }

    fun awaitAsync(
        generation: LatchGeneration,
        requestId: LatchRequestId,
        waitTime: Duration,
    ): CompletableFuture<LatchAwaitResult> {
        validateWait(waitTime)
        if (closed.get()) return CompletableFuture.completedFuture(LatchAwaitResult.Closed)
        val source = runAsync(
            LatchScripts.REGISTER_WAITER_SCRIPT,
            generation.value.toString(),
            requestId.value,
            waitTime.toMillis().toString(),
            config.maxWaiters.toString(),
            config.waiterCleanupGrace.toMillis().toString(),
        )
            .thenCompose { raw ->
                decodeAwait(raw)?.let { CompletableFuture.completedFuture(it) }
                    ?: poller.poll<LatchAwaitResult?>(
                            deadlineNanos = System.nanoTime() + waitTime.toNanos(),
                            interval = config.pollInterval,
                            closedResult = LatchAwaitResult.Closed,
                            capacityResult = LatchAwaitResult.CapacityExceeded,
                            timedOutResult = LatchAwaitResult.TimedOut,
                            shouldRetry = { it == null },
                            attempt = { getCountAsync(generation).thenApply(::countToAwaitResult) },
                        ).thenApply { it ?: LatchAwaitResult.TimedOut }
            }.exceptionally { ambiguousAwait(it, requestId) }
        return cleanupAfter(source, generation, requestId)
    }

    suspend fun awaitSuspending(
        generation: LatchGeneration,
        requestId: LatchRequestId,
        waitTime: Duration,
    ): LatchAwaitResult {
        validateWait(waitTime)
        if (closed.get()) return LatchAwaitResult.Closed
        val registered = try {
            decodeAwait(
                runAsync(
                    LatchScripts.REGISTER_WAITER_SCRIPT,
                    generation.value.toString(),
                    requestId.value,
                    waitTime.toMillis().toString(),
                    config.maxWaiters.toString(),
                    config.waiterCleanupGrace.toMillis().toString(),
                ).await(),
            )
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                runAsync(
                    LatchScripts.UNREGISTER_WAITER_SCRIPT,
                    generation.value.toString(),
                    requestId.value,
                ).await()
            }
            throw cancelled
        } catch (error: Exception) {
            return ambiguousAwait(error, requestId)
        }
        if (registered != null) return registered
        return try {
            val deadline = System.nanoTime() + waitTime.toNanos()
            do {
                when (val count = getCountAsync(generation).await()) {
                    is LatchCountResult.Active -> {
                        // Keep polling while this generation remains active.
                    }
                    is LatchCountResult.Completed -> return LatchAwaitResult.Completed
                    LatchCountResult.Deleted -> return LatchAwaitResult.Deleted
                    LatchCountResult.StaleGeneration -> return LatchAwaitResult.StaleGeneration
                    LatchCountResult.Closed -> return LatchAwaitResult.Closed
                    is LatchCountResult.BackendFailure -> return LatchAwaitResult.BackendFailure(count.failure)
                    is LatchCountResult.IntegrityFailure -> return LatchAwaitResult.IntegrityFailure(count.failure)
                }
                delay(config.pollInterval.toMillis().coerceAtLeast(1))
            } while (System.nanoTime() < deadline)
            LatchAwaitResult.TimedOut
        } finally {
            withContext(NonCancellable) {
                runAsync(
                    LatchScripts.UNREGISTER_WAITER_SCRIPT,
                    generation.value.toString(),
                    requestId.value,
                ).await()
            }
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) poller.close()
    }

    private fun run(script: RedisScript, vararg args: String): List<String> =
        RedisScriptRunner.run(sync, script, ScriptOutputType.MULTI, keys.array, *args)
    private fun runAsync(script: RedisScript, vararg args: String): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(async, script, ScriptOutputType.MULTI, keys.array, *args)

    private fun decodeSet(raw: List<String>): LatchSetCountResult {
        val reply = decodeReply(raw) ?: return integritySet()
        val generation = reply.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }?.let(::LatchGeneration)
        return when (reply[0]) {
            "CREATED" -> generation?.let(LatchSetCountResult::Created) ?: integritySet()
            "ACTIVE_GENERATION" -> {
                val count = reply.getOrNull(2)?.toLongOrNull()
                if (generation == null || count == null || count < 0) integritySet()
                else LatchSetCountResult.ActiveGeneration(generation, count)
            }
            "INVALID_COUNT" -> LatchSetCountResult.InvalidCount
            else -> integritySet()
        }
    }

    private fun decodeCount(raw: List<String>): LatchCountResult {
        val reply = decodeReply(raw) ?: return integrityCount()
        return when (reply[0]) {
            "ACTIVE" -> {
                val generation = reply.getOrNull(1)?.toLongOrNull()
                val count = reply.getOrNull(2)?.toLongOrNull()
                val waiters = reply.getOrNull(3)?.toIntOrNull()
                if (generation == null || generation <= 0 || count == null || count <= 0 || waiters == null || waiters < 0) {
                    integrityCount()
                } else {
                    LatchCountResult.Active(LatchGeneration(generation), count, waiters)
                }
            }
            "COMPLETED" -> reply.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }
                ?.let { LatchCountResult.Completed(LatchGeneration(it)) } ?: integrityCount()
            "DELETED" -> LatchCountResult.Deleted
            "STALE_GENERATION" -> LatchCountResult.StaleGeneration
            else -> integrityCount()
        }
    }

    private fun decodeMutation(raw: List<String>): LatchMutationResult {
        val reply = decodeReply(raw) ?: return integrityMutation()
        return when (reply[0]) {
            "DECREMENTED" -> reply.getOrNull(1)?.toLongOrNull()?.let(LatchMutationResult::Decremented)
                ?: integrityMutation()
            "COMPLETED" -> LatchMutationResult.Completed
            "ALREADY_COMPLETED" -> LatchMutationResult.AlreadyCompleted
            "DELETED" -> LatchMutationResult.Deleted
            "NOT_FOUND" -> LatchMutationResult.NotFound
            "STALE_GENERATION" -> LatchMutationResult.StaleGeneration
            "ACTIVE_WAITERS" -> reply.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }
                ?.let(LatchMutationResult::ActiveWaiters) ?: integrityMutation()
            else -> integrityMutation()
        }
    }

    private fun decodeAwait(raw: List<String>): LatchAwaitResult? {
        val reply = decodeReply(raw) ?: return LatchAwaitResult.IntegrityFailure(integrity())
        return when (reply[0]) {
            "REGISTERED" -> null
            "COMPLETED" -> LatchAwaitResult.Completed
            "DELETED" -> LatchAwaitResult.Deleted
            "STALE_GENERATION" -> LatchAwaitResult.StaleGeneration
            "CAPACITY_EXCEEDED" -> LatchAwaitResult.CapacityExceeded
            else -> LatchAwaitResult.IntegrityFailure(integrity())
        }
    }

    private fun validateWait(waitTime: Duration) {
        requirePositive(waitTime, "waitTime")
        require(waitTime <= Duration.ofHours(24))
    }

    private fun countToAwaitResult(count: LatchCountResult): LatchAwaitResult? = when (count) {
        is LatchCountResult.Active -> null
        is LatchCountResult.Completed -> LatchAwaitResult.Completed
        LatchCountResult.Deleted -> LatchAwaitResult.Deleted
        LatchCountResult.StaleGeneration -> LatchAwaitResult.StaleGeneration
        LatchCountResult.Closed -> LatchAwaitResult.Closed
        is LatchCountResult.BackendFailure -> LatchAwaitResult.BackendFailure(count.failure)
        is LatchCountResult.IntegrityFailure -> LatchAwaitResult.IntegrityFailure(count.failure)
    }

    private fun cleanupAfter(
        source: CompletableFuture<LatchAwaitResult>,
        generation: LatchGeneration,
        requestId: LatchRequestId,
    ): CompletableFuture<LatchAwaitResult> {
        val result = CleanupCompletableFuture<LatchAwaitResult>(
            cleanup = {
                runAsync(
                    LatchScripts.UNREGISTER_WAITER_SCRIPT,
                    generation.value.toString(),
                    requestId.value,
                )
            },
            cleanupFailure = { ambiguousAwait(it, requestId) },
            cancelSource = { source.cancel(false) },
        )
        source.whenComplete(result::completeFrom)
        return result
    }

    companion object {
        fun create(connection: StatefulRedisConnection<String, String>, name: String, config: LatchConfig): LatchClient {
            val keys = deriveLatchKeys(name, config, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return LatchClient(keys, config, connection.sync(), connection.async(), SynchronizerAsyncPoller(registration))
        }
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LatchConfig,
        ): LatchClient {
            val keys = deriveLatchKeys(name, config, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return LatchClient(keys, config, connection.sync(), connection.async(), SynchronizerAsyncPoller(registration))
        }
    }
}

private class CleanupCompletableFuture<T>(
    private val cleanup: () -> CompletableFuture<*>,
    private val cleanupFailure: (Throwable) -> T,
    private val cancelSource: () -> Unit,
): CompletableFuture<T>() {
    private val finishing = AtomicBoolean()

    fun completeFrom(value: T?, error: Throwable?) {
        finish(value, error, cancelled = error is CancellationException)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (isDone || !finishing.compareAndSet(false, true)) return false
        cancelSource()
        cleanup().whenComplete { _, cleanupError ->
            if (cleanupError != null) super.complete(cleanupFailure(cleanupError))
            else super.cancel(mayInterruptIfRunning)
        }
        return true
    }

    private fun finish(value: T?, error: Throwable?, cancelled: Boolean) {
        if (!finishing.compareAndSet(false, true)) return
        cleanup().whenComplete { _, cleanupError ->
            when {
                cleanupError != null -> super.complete(cleanupFailure(cleanupError))
                cancelled -> super.cancel(false)
                error != null -> super.completeExceptionally(unwrap(error))
                else -> super.complete(value)
            }
        }
    }

    private fun unwrap(error: Throwable): Throwable =
        if (error is CompletionException) error.cause ?: error else error
}

private fun backend(error: Throwable): SynchronizerBackendFailure {
    val cause = generateSequence(error) { it.cause }.last()
    val kind = when (cause) {
        is RedisCommandTimeoutException -> SynchronizerBackendFailureKind.TIMEOUT
        is RedisConnectionException -> SynchronizerBackendFailureKind.CONNECTION
        else -> SynchronizerBackendFailureKind.COMMAND
    }
    return SynchronizerBackendFailure(kind, SynchronizerRecoveryAction.RETRY)
}
private fun ambiguousMutation(error: Throwable, requestId: LatchRequestId): LatchMutationResult {
    val failure = backend(error)
    return if (failure.kind in setOf(SynchronizerBackendFailureKind.TIMEOUT, SynchronizerBackendFailureKind.CONNECTION)) {
        LatchMutationResult.Ambiguous(requestId)
    } else {
        LatchMutationResult.BackendFailure(failure)
    }
}
private fun ambiguousAwait(error: Throwable, requestId: LatchRequestId): LatchAwaitResult {
    val failure = backend(error)
    return if (failure.kind in setOf(SynchronizerBackendFailureKind.TIMEOUT, SynchronizerBackendFailureKind.CONNECTION)) {
        LatchAwaitResult.Ambiguous(requestId)
    } else {
        LatchAwaitResult.BackendFailure(failure)
    }
}
private fun integrity() = SynchronizerIntegrityFailure(SynchronizerIntegrityFailureKind.MALFORMED_REPLY)
private fun integritySet() = LatchSetCountResult.IntegrityFailure(integrity())
private fun integrityCount() = LatchCountResult.IntegrityFailure(integrity())
private fun integrityMutation() = LatchMutationResult.IntegrityFailure(integrity())
