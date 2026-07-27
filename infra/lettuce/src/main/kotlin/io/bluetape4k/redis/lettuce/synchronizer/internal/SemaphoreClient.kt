package io.bluetape4k.redis.lettuce.synchronizer.internal

import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.synchronizer.PermitAcquireResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitHandle
import io.bluetape4k.redis.lettuce.synchronizer.PermitInspectResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitMutationResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitReconcileResult
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreConfig
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreInitializationResult
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreOwnerId
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreRequestId
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerRecoveryAction
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerScripts
import io.bluetape4k.redis.lettuce.synchronizer.requirePositive
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

internal class SemaphoreClient private constructor(
    private val keys: SemaphoreKeys,
    private val config: SemaphoreConfig,
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
    private val poller: SynchronizerAsyncPoller,
) {
    private val closed = AtomicBoolean()

    fun trySetPermits(permits: Int): SemaphoreInitializationResult {
        if (permits !in 1..config.maxPermits) return SemaphoreInitializationResult.InvalidCapacity
        if (closed.get()) return SemaphoreInitializationResult.Closed
        return classified(
            { SemaphoreInitializationResult.BackendFailure(it) },
            { SemaphoreInitializationResult.IntegrityFailure(it) },
        ) { decodeInitialization(run(SynchronizerScripts.TRY_SET_PERMITS_SCRIPT, keys.regular, permits.toString())) }
    }

    fun trySetPermitsAsync(permits: Int): CompletableFuture<SemaphoreInitializationResult> {
        if (permits !in 1..config.maxPermits) {
            return CompletableFuture.completedFuture(SemaphoreInitializationResult.InvalidCapacity)
        }
        if (closed.get()) return CompletableFuture.completedFuture(SemaphoreInitializationResult.Closed)
        return runAsync(SynchronizerScripts.TRY_SET_PERMITS_SCRIPT, keys.regular, permits.toString())
            .classifiedFuture(
                { decodeInitialization(it) },
                { SemaphoreInitializationResult.BackendFailure(it) },
                { SemaphoreInitializationResult.IntegrityFailure(it) },
            )
    }

    fun availablePermits(): Int {
        if (closed.get()) return -1
        val raw = run(SynchronizerScripts.AVAILABLE_SCRIPT, keys.regular)
        return decodeReply(raw)?.singleOrNull()?.toIntOrNull() ?: -1
    }

    fun availablePermitsAsync(): CompletableFuture<Int> {
        if (closed.get()) return CompletableFuture.completedFuture(-1)
        return runAsync(SynchronizerScripts.AVAILABLE_SCRIPT, keys.regular)
            .thenApply { decodeReply(it)?.singleOrNull()?.toIntOrNull() ?: -1 }
    }

    fun tryAcquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
    ): PermitAcquireResult<PermitHandle> {
        validatePermits(permits)
        if (closed.get()) return PermitAcquireResult.Closed
        return classified(
            { ambiguousAcquire(it, requestId) },
            { PermitAcquireResult.IntegrityFailure(it) },
        ) {
            decodeAcquire(
                run(
                    SynchronizerScripts.ACQUIRE_SCRIPT,
                    keys.regular,
                    permits.toString(),
                    ownerId.value,
                    requestId.value,
                    randomToken(),
                ),
                ownerId,
                requestId,
            )
        }
    }

    fun tryAcquireAsync(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
    ): CompletableFuture<PermitAcquireResult<PermitHandle>> {
        validatePermits(permits)
        if (closed.get()) return CompletableFuture.completedFuture(PermitAcquireResult.Closed)
        return runAsync(
            SynchronizerScripts.ACQUIRE_SCRIPT,
            keys.regular,
            permits.toString(),
            ownerId.value,
            requestId.value,
            randomToken(),
        ).classifiedFuture(
            { decodeAcquire(it, ownerId, requestId) },
            { ambiguousAcquire(it, requestId) },
            { PermitAcquireResult.IntegrityFailure(it) },
        )
    }

    fun acquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ): PermitAcquireResult<PermitHandle> {
        validateWait(waitTime)
        val deadline = System.nanoTime() + waitTime.toNanos()
        do {
            when (val result = tryAcquire(ownerId, requestId, permits)) {
                PermitAcquireResult.Unavailable -> Thread.sleep(config.pollInterval.toMillis().coerceAtLeast(1))
                else -> return result
            }
        } while (System.nanoTime() < deadline)
        return PermitAcquireResult.TimedOut
    }

    fun acquireAsync(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ): CompletableFuture<PermitAcquireResult<PermitHandle>> {
        validateWait(waitTime)
        return poller.poll(
            deadlineNanos = System.nanoTime() + waitTime.toNanos(),
            interval = config.pollInterval,
            closedResult = PermitAcquireResult.Closed,
            capacityResult = PermitAcquireResult.CapacityExceeded,
            timedOutResult = PermitAcquireResult.TimedOut,
            shouldRetry = { it == PermitAcquireResult.Unavailable },
            attempt = { tryAcquireAsync(ownerId, requestId, permits) },
        )
    }

    suspend fun acquireSuspending(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ): PermitAcquireResult<PermitHandle> {
        validateWait(waitTime)
        val deadline = System.nanoTime() + waitTime.toNanos()
        do {
            when (val result = tryAcquireAsync(ownerId, requestId, permits).await()) {
                PermitAcquireResult.Unavailable -> delay(config.pollInterval.toMillis().coerceAtLeast(1))
                else -> return result
            }
        } while (System.nanoTime() < deadline)
        return PermitAcquireResult.TimedOut
    }

    fun inspect(handle: PermitHandle): PermitInspectResult<PermitHandle> {
        validateHandle(handle)
        if (closed.get()) return PermitInspectResult.Closed
        return classified(
            { PermitInspectResult.BackendFailure(it) },
            { PermitInspectResult.IntegrityFailure(it) },
        ) { decodeInspect(run(SynchronizerScripts.INSPECT_SCRIPT, keys.regular, *handleArgs(handle)), handle) }
    }

    fun inspectAsync(handle: PermitHandle): CompletableFuture<PermitInspectResult<PermitHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(PermitInspectResult.Closed)
        return runAsync(SynchronizerScripts.INSPECT_SCRIPT, keys.regular, *handleArgs(handle))
            .classifiedFuture(
                { decodeInspect(it, handle) },
                { PermitInspectResult.BackendFailure(it) },
                { PermitInspectResult.IntegrityFailure(it) },
            )
    }

    fun release(handle: PermitHandle): PermitMutationResult<PermitHandle> {
        validateHandle(handle)
        if (closed.get()) return PermitMutationResult.Closed
        return classified(
            { ambiguousMutation(it, handle.requestId) },
            { PermitMutationResult.IntegrityFailure(it) },
        ) { decodeRelease(run(SynchronizerScripts.RELEASE_SCRIPT, keys.regular, *handleArgs(handle)), handle) }
    }

    fun releaseAsync(handle: PermitHandle): CompletableFuture<PermitMutationResult<PermitHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(PermitMutationResult.Closed)
        return runAsync(SynchronizerScripts.RELEASE_SCRIPT, keys.regular, *handleArgs(handle))
            .classifiedFuture(
                { decodeRelease(it, handle) },
                { ambiguousMutation(it, handle.requestId) },
                { PermitMutationResult.IntegrityFailure(it) },
            )
    }

    fun reconcile(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): PermitReconcileResult<PermitHandle> {
        if (closed.get()) return PermitReconcileResult.Closed
        return classified(
            { PermitReconcileResult.BackendFailure(it) },
            { PermitReconcileResult.IntegrityFailure(it) },
        ) {
            decodeReconcile(
                run(SynchronizerScripts.RECONCILE_SCRIPT, keys.regular, ownerId.value, requestId.value),
                ownerId,
                requestId,
            )
        }
    }

    fun reconcileAsync(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): CompletableFuture<PermitReconcileResult<PermitHandle>> {
        if (closed.get()) return CompletableFuture.completedFuture(PermitReconcileResult.Closed)
        return runAsync(SynchronizerScripts.RECONCILE_SCRIPT, keys.regular, ownerId.value, requestId.value)
            .classifiedFuture(
                { decodeReconcile(it, ownerId, requestId) },
                { PermitReconcileResult.BackendFailure(it) },
                { PermitReconcileResult.IntegrityFailure(it) },
            )
    }

    fun close() {
        if (closed.compareAndSet(false, true)) poller.close()
    }

    private fun run(script: RedisScript, scriptKeys: Array<String>, vararg args: String): List<String> =
        RedisScriptRunner.run(sync, script, ScriptOutputType.MULTI, scriptKeys, *args)

    private fun runAsync(
        script: RedisScript,
        scriptKeys: Array<String>,
        vararg args: String,
    ): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(async, script, ScriptOutputType.MULTI, scriptKeys, *args)

    private fun decodeInitialization(raw: List<String>): SemaphoreInitializationResult {
        val reply = decodeReply(raw) ?: return integrityInitialization()
        return when (reply[0]) {
            "INITIALIZED" -> reply.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0 }
                ?.let(SemaphoreInitializationResult::Initialized) ?: integrityInitialization()
            "ALREADY_INITIALIZED" -> SemaphoreInitializationResult.AlreadyInitialized
            "INVALID_CAPACITY" -> SemaphoreInitializationResult.InvalidCapacity
            else -> integrityInitialization()
        }
    }

    private fun decodeAcquire(
        raw: List<String>,
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): PermitAcquireResult<PermitHandle> {
        val reply = decodeReply(raw) ?: return integrityAcquire()
        return when (reply[0]) {
            "ACQUIRED" -> {
                val generation = reply.getOrNull(2)?.toLongOrNull()
                val permits = reply.getOrNull(3)?.toIntOrNull()
                if (generation == null || permits == null) integrityAcquire()
                else PermitAcquireResult.Acquired(
                    PermitHandle(keys.fingerprint, ownerId, generation, requestId, permits, reply[1]),
                )
            }
            "UNAVAILABLE", "NOT_INITIALIZED", "REQUEST_COMPLETED" -> PermitAcquireResult.Unavailable
            "CAPACITY_EXCEEDED" -> PermitAcquireResult.CapacityExceeded
            else -> integrityAcquire()
        }
    }

    private fun decodeInspect(raw: List<String>, handle: PermitHandle): PermitInspectResult<PermitHandle> {
        val reply = decodeReply(raw) ?: return integrityInspect()
        return when (reply[0]) {
            "OWNED" -> reply.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 }
                ?.let { PermitInspectResult.Owned(handle, it) } ?: integrityInspect()
            "RELEASED" -> PermitInspectResult.Released
            "STALE_GENERATION" -> PermitInspectResult.StaleGeneration
            else -> integrityInspect()
        }
    }

    private fun decodeRelease(raw: List<String>, handle: PermitHandle): PermitMutationResult<PermitHandle> {
        val reply = decodeReply(raw) ?: return integrityMutation()
        return when (reply[0]) {
            "RELEASED" -> reply.getOrNull(1)?.toIntOrNull()?.takeIf { it >= 0 }
                ?.let { PermitMutationResult.Released(handle, it) } ?: integrityMutation()
            "ALREADY_RELEASED" -> PermitMutationResult.AlreadyReleased
            "STALE_GENERATION" -> PermitMutationResult.StaleGeneration
            else -> integrityMutation()
        }
    }

    private fun decodeReconcile(
        raw: List<String>,
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): PermitReconcileResult<PermitHandle> {
        val reply = decodeReply(raw) ?: return integrityReconcile()
        return when (reply[0]) {
            "OWNED" -> {
                val generation = reply.getOrNull(2)?.toLongOrNull()
                val permits = reply.getOrNull(3)?.toIntOrNull()
                val remaining = reply.getOrNull(4)?.toIntOrNull()
                if (generation == null || permits == null || remaining == null) integrityReconcile()
                else PermitReconcileResult.Owned(
                    PermitHandle(keys.fingerprint, ownerId, generation, requestId, permits, reply[1]),
                    remaining,
                )
            }
            "RELEASED" -> PermitReconcileResult.Released
            "NOT_FOUND" -> PermitReconcileResult.NotFound
            else -> integrityReconcile()
        }
    }

    private fun validateHandle(handle: PermitHandle) {
        require(handle.objectFingerprint == keys.fingerprint) { "Permit handle belongs to another object." }
    }

    private fun validatePermits(permits: Int) {
        require(permits in 1..config.maxPermits) { "permits must be within configured bounds." }
    }

    private fun validateWait(waitTime: Duration) {
        requirePositive(waitTime, "waitTime")
        require(waitTime <= Duration.ofHours(24)) { "waitTime exceeds 24 hours." }
    }

    private fun handleArgs(handle: PermitHandle): Array<String> =
        arrayOf(handle.token, handle.generation.toString(), handle.ownerId.value, handle.requestId.value)

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ): SemaphoreClient {
            val keys = deriveSemaphoreKeys(name, config, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return SemaphoreClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                SynchronizerAsyncPoller(registration),
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SemaphoreConfig,
        ): SemaphoreClient {
            val keys = deriveSemaphoreKeys(name, config, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return SemaphoreClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                SynchronizerAsyncPoller(registration),
            )
        }
    }
}

private fun randomToken(): String = UUID.randomUUID().toString().replace("-", "")

private fun backend(error: Throwable): SynchronizerBackendFailure {
    val cause = generateSequence(error) { it.cause }.last()
    val kind = when (cause) {
        is RedisCommandTimeoutException -> SynchronizerBackendFailureKind.TIMEOUT
        is RedisConnectionException -> SynchronizerBackendFailureKind.CONNECTION
        else -> SynchronizerBackendFailureKind.COMMAND
    }
    return SynchronizerBackendFailure(kind, SynchronizerRecoveryAction.RETRY)
}

private fun integrity(): SynchronizerIntegrityFailure =
    SynchronizerIntegrityFailure(SynchronizerIntegrityFailureKind.MALFORMED_REPLY)

private inline fun <T> classified(
    backendResult: (SynchronizerBackendFailure) -> T,
    integrityResult: (SynchronizerIntegrityFailure) -> T,
    block: () -> T,
): T = try {
    block()
} catch (_: IndexOutOfBoundsException) {
    integrityResult(integrity())
} catch (_: NumberFormatException) {
    integrityResult(integrity())
} catch (error: Exception) {
    backendResult(backend(error))
}

private fun <T> CompletableFuture<List<String>>.classifiedFuture(
    decode: (List<String>) -> T,
    backendResult: (SynchronizerBackendFailure) -> T,
    integrityResult: (SynchronizerIntegrityFailure) -> T,
): CompletableFuture<T> = handle { raw, error ->
    when {
        error != null -> backendResult(backend(error))
        else -> try {
            decode(raw)
        } catch (_: RuntimeException) {
            integrityResult(integrity())
        }
    }
}

private fun integrityInitialization() =
    SemaphoreInitializationResult.IntegrityFailure(integrity())
private fun integrityAcquire() =
    PermitAcquireResult.IntegrityFailure(integrity())
private fun integrityInspect() =
    PermitInspectResult.IntegrityFailure(integrity())
private fun integrityMutation() =
    PermitMutationResult.IntegrityFailure(integrity())
private fun integrityReconcile() =
    PermitReconcileResult.IntegrityFailure(integrity())

private fun ambiguousAcquire(
    failure: SynchronizerBackendFailure,
    requestId: SemaphoreRequestId,
): PermitAcquireResult<Nothing> =
    if (failure.kind in setOf(SynchronizerBackendFailureKind.TIMEOUT, SynchronizerBackendFailureKind.CONNECTION)) {
        PermitAcquireResult.Ambiguous(requestId)
    } else {
        PermitAcquireResult.BackendFailure(failure)
    }

private fun ambiguousMutation(
    failure: SynchronizerBackendFailure,
    requestId: SemaphoreRequestId,
): PermitMutationResult<Nothing> =
    if (failure.kind in setOf(SynchronizerBackendFailureKind.TIMEOUT, SynchronizerBackendFailureKind.CONNECTION)) {
        PermitMutationResult.Ambiguous(requestId)
    } else {
        PermitMutationResult.BackendFailure(failure)
    }
