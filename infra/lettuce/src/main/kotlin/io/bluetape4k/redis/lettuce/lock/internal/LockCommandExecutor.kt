package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockConfig
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

internal interface LockCommandExecutor {
    fun run(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String>

    fun runAsync(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>>

    suspend fun runSuspending(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String>
}

internal class DefaultLockCommandExecutor(
    private val syncCommands: RedisScriptingCommands<String, String>,
    private val asyncCommands: RedisScriptingAsyncCommands<String, String>,
): LockCommandExecutor {
    override fun run(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> =
        RedisScriptRunner.run(
            syncCommands,
            DISTRIBUTED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )

    override fun runAsync(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(
            asyncCommands,
            DISTRIBUTED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )

    override suspend fun runSuspending(
        operation: DistributedLockOperation,
        keys: DistributedLockKeys,
        args: List<String>,
    ): List<String> =
        RedisScriptRunner.runSuspending(
            asyncCommands,
            DISTRIBUTED_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all,
            operation.wireValue,
            *args.toTypedArray(),
        )
}

internal class DistributedLockClient private constructor(
    private val keys: DistributedLockKeys,
    private val config: LockConfig,
    private val executor: LockCommandExecutor,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    @Suppress("unused")
    private val observationSink: LockObservationSink,
) {
    private val closed = AtomicBoolean()
    private val waitSupport = LockWaitSupport(registration, closed::get)

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        if (closed.get()) return LockAcquireResult.Closed
        return classifiedAcquire(ownerId, requestId) {
            executor.run(DistributedLockOperation.ACQUIRE, keys, args)
        }
    }

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        if (closed.get()) return CompletableFuture.completedFuture(LockAcquireResult.Closed)
        return executor.runAsync(DistributedLockOperation.ACQUIRE, keys, args)
            .mapResult(
                decode = { decodeAcquire(it, keys, ownerId, requestId) },
                backend = { LockAcquireResult.BackendFailure(it) },
                integrity = { LockAcquireResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
            )
    }

    suspend fun tryAcquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        if (closed.get()) return LockAcquireResult.Closed
        return classifiedSuspending(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAcquire(
                executor.runSuspending(DistributedLockOperation.ACQUIRE, keys, args),
                keys,
                ownerId,
                requestId,
            )
        }
    }

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        return waitSupport.acquire(waitTime) {
            tryAcquire(ownerId, requestId, leasePolicy)
        }
    }

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> {
        acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        return waitSupport.acquireAsync(waitTime) {
            tryAcquireAsync(ownerId, requestId, leasePolicy)
        }
    }

    suspend fun acquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        return waitSupport.acquireSuspending(waitTime) {
            tryAcquireSuspending(ownerId, requestId, leasePolicy)
        }
    }

    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return classified(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeInspect(
                executor.run(DistributedLockOperation.INSPECT, keys, handleArgs(handle)),
                keys,
                handle,
            )
        }
    }

    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> {
        validateHandle(handle)
        val args = handleArgs(handle)
        if (closed.get()) return CompletableFuture.completedFuture(LockInspectResult.Closed)
        return executor.runAsync(DistributedLockOperation.INSPECT, keys, args)
            .mapResult(
                decode = { decodeInspect(it, keys, handle) },
                backend = { LockInspectResult.BackendFailure(it) },
                integrity = { LockInspectResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
            )
    }

    suspend fun inspectSuspending(handle: LockHandle): LockInspectResult<LockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return classifiedSuspending(
            backend = { LockInspectResult.BackendFailure(it) },
            integrity = { LockInspectResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.INSPECT_HANDLE,
        ) {
            decodeInspect(
                executor.runSuspending(DistributedLockOperation.INSPECT, keys, handleArgs(handle)),
                keys,
                handle,
            )
        }
    }

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        val args = reconcileArgs(ownerId, requestId)
        if (closed.get()) return LockReconcileResult.Closed
        return classified(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeReconcile(
                executor.run(DistributedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            )
        }
    }

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> {
        val args = reconcileArgs(ownerId, requestId)
        if (closed.get()) return CompletableFuture.completedFuture(LockReconcileResult.Closed)
        return executor.runAsync(DistributedLockOperation.RECONCILE, keys, args)
            .mapResult(
                decode = { decodeReconcile(it, keys, ownerId, requestId) },
                backend = { LockReconcileResult.BackendFailure(it) },
                integrity = { LockReconcileResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
            )
    }

    suspend fun reconcileSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> {
        val args = reconcileArgs(ownerId, requestId)
        if (closed.get()) return LockReconcileResult.Closed
        return classifiedSuspending(
            backend = { LockReconcileResult.BackendFailure(it) },
            integrity = { LockReconcileResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeReconcile(
                executor.runSuspending(DistributedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            )
        }
    }

    fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> {
        validateHandle(handle)
        val args = renewArgs(handle, extension)
        if (closed.get()) return LockMutationResult.Closed
        return classified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRenew(executor.run(DistributedLockOperation.RENEW, keys, args), handle)
        }
    }

    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> {
        validateHandle(handle)
        val args = renewArgs(handle, extension)
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(DistributedLockOperation.RENEW, keys, args)
            .mapResult(
                decode = { decodeRenew(it, handle) },
                backend = { LockMutationResult.BackendFailure(it) },
                integrity = { LockMutationResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
    }

    suspend fun renewSuspending(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> {
        validateHandle(handle)
        val args = renewArgs(handle, extension)
        if (closed.get()) return LockMutationResult.Closed
        return classifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRenew(executor.runSuspending(DistributedLockOperation.RENEW, keys, args), handle)
        }
    }

    fun release(handle: LockHandle): LockMutationResult<LockHandle> {
        validateHandle(handle)
        val args = handleArgs(handle) + TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return LockMutationResult.Closed
        return classified(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRelease(executor.run(DistributedLockOperation.RELEASE, keys, args))
        }
    }

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> {
        validateHandle(handle)
        val args = handleArgs(handle) + TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(DistributedLockOperation.RELEASE, keys, args)
            .mapResult(
                decode = ::decodeRelease,
                backend = { LockMutationResult.BackendFailure(it) },
                integrity = { LockMutationResult.IntegrityFailure(it) },
                recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
            )
    }

    suspend fun releaseSuspending(handle: LockHandle): LockMutationResult<LockHandle> {
        validateHandle(handle)
        val args = handleArgs(handle) + TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return LockMutationResult.Closed
        return classifiedSuspending(
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            decodeRelease(executor.runSuspending(DistributedLockOperation.RELEASE, keys, args))
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            registration.close()
        }
    }

    private fun validateHandle(handle: LockHandle) {
        require(handle.kind == LockKind.DISTRIBUTED) { "Handle kind must be DISTRIBUTED." }
        require(handle.objectFingerprint == keys.fingerprint) {
            "Handle belongs to a different distributed lock object."
        }
    }

    private fun classifiedAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        command: () -> List<String>,
    ): LockAcquireResult<LockHandle> =
        classified(
            backend = { LockAcquireResult.BackendFailure(it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            decodeAcquire(command(), keys, ownerId, requestId)
        }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): DistributedLockClient {
            val keys = deriveDistributedLockKeys(name, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return DistributedLockClient(
                keys,
                config,
                DefaultLockCommandExecutor(connection.sync(), connection.async()),
                runtime.registerObject(keys.fingerprint),
                observationSink,
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: LockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): DistributedLockClient {
            val keys = deriveDistributedLockKeys(name, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return DistributedLockClient(
                keys,
                config,
                DefaultLockCommandExecutor(connection.sync(), connection.async()),
                runtime.registerObject(keys.fingerprint),
                observationSink,
            )
        }
    }
}

private inline fun <R> classified(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
    block: () -> R,
): R =
    try {
        block()
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND ->
                backend(classifyLockBackendFailure(error, recoveryAction))
            CoordinationFailureClassification.INTEGRITY ->
                integrity(malformedIntegrityFailure())
        }
    } catch (error: IllegalArgumentException) {
        integrity(malformedIntegrityFailure())
    } catch (error: Exception) {
        backend(classifyLockBackendFailure(error, recoveryAction))
    }

private suspend inline fun <R> classifiedSuspending(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
    crossinline block: suspend () -> R,
): R =
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: CoordinationProtocolException) {
        when (error.classification) {
            CoordinationFailureClassification.BACKEND ->
                backend(classifyLockBackendFailure(error, recoveryAction))
            CoordinationFailureClassification.INTEGRITY ->
                integrity(malformedIntegrityFailure())
        }
    } catch (error: IllegalArgumentException) {
        integrity(malformedIntegrityFailure())
    } catch (error: Exception) {
        backend(classifyLockBackendFailure(error, recoveryAction))
    }

private fun <R> CompletableFuture<List<String>>.mapResult(
    decode: (List<String>) -> R,
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    recoveryAction: LockRecoveryAction,
): CompletableFuture<R> =
    handle { value, error ->
        if (error == null) {
            try {
                decode(value)
            } catch (_: CoordinationProtocolException) {
                integrity(malformedIntegrityFailure())
            } catch (_: IllegalArgumentException) {
                integrity(malformedIntegrityFailure())
            }
        } else {
            backend(classifyLockBackendFailure(error, recoveryAction))
        }
    }

private fun classifyLockBackendFailure(
    error: Throwable,
    recoveryAction: LockRecoveryAction,
): LockBackendFailure {
    val cause = error.unwrapCompletionCause()
    val kind = when (cause) {
        is RedisConnectionException -> LockBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException,
        is TimeoutException,
        -> LockBackendFailureKind.TIMEOUT
        is RedisException -> LockBackendFailureKind.COMMAND
        else -> throw cause
    }
    return LockBackendFailure(kind, recoveryAction)
}

private fun Throwable.unwrapCompletionCause(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(MAX_COMPLETION_DEPTH) {
        if (current is CancellationException) throw current
        if (!seen.add(current)) return current
        val next = when (current) {
            is CompletionException,
            is ExecutionException,
            -> current.cause
            else -> null
        } ?: return current
        if (next === current) return current
        current = next
    }
    if (current is CancellationException) throw current
    return current
}

private const val MAX_COMPLETION_DEPTH = 8
private const val TERMINAL_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L
