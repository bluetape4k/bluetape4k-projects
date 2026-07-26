package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationCapacityException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationFailureClassification
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationProtocolException
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRenewalOutcome
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockAcquireResult
import io.bluetape4k.redis.lettuce.lock.LockBackendFailure
import io.bluetape4k.redis.lettuce.lock.LockBackendFailureKind
import io.bluetape4k.redis.lettuce.lock.LockConfig
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockDimensions
import io.bluetape4k.redis.lettuce.lock.LockEvent
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockLeasePolicyKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOperation
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.lock.recordSafely
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.io.Serializable
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.toKotlinDuration

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

internal class DistributedLockClient(
    private val keys: DistributedLockKeys,
    private val config: LockConfig,
    private val executor: LockCommandExecutor,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val observationSink: LockObservationSink,
) {
    private val closed = AtomicBoolean()
    private val waitSupport = LockWaitSupport(registration, closed::get)
    private val watchdogs = ConcurrentHashMap<LockHandle, CoordinationRuntime.CoordinationTaskRegistration>()

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> {
        val args = acquireArgs(ownerId, requestId, leasePolicy, config.maxReentrantHolds)
        if (closed.get()) return LockAcquireResult.Closed
        return registerWatchdog(classifiedAcquire(ownerId, requestId) {
            executor.run(DistributedLockOperation.ACQUIRE, keys, args)
        })
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
                decode = { registerWatchdog(decodeAcquire(it, keys, ownerId, requestId)) },
                backend = { acquireBackendResult(ownerId, requestId, it) },
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
            backend = { acquireBackendResult(ownerId, requestId, it) },
            integrity = { LockAcquireResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(decodeAcquire(
                executor.runSuspending(DistributedLockOperation.ACQUIRE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
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
            registerWatchdog(decodeReconcile(
                executor.run(DistributedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
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
                decode = { registerWatchdog(decodeReconcile(it, keys, ownerId, requestId)) },
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
            registerWatchdog(decodeReconcile(
                executor.runSuspending(DistributedLockOperation.RECONCILE, keys, args),
                keys,
                ownerId,
                requestId,
            ))
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
            recordRenewOutcome(handle, decodeRenew(executor.run(DistributedLockOperation.RENEW, keys, args), handle))
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
                decode = { recordRenewOutcome(handle, decodeRenew(it, handle)) },
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
            recordRenewOutcome(
                handle,
                decodeRenew(executor.runSuspending(DistributedLockOperation.RENEW, keys, args), handle),
            )
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
            recordReleaseOutcome(handle, decodeRelease(executor.run(DistributedLockOperation.RELEASE, keys, args)))
        }
    }

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> {
        validateHandle(handle)
        val args = handleArgs(handle) + TERMINAL_TTL_MILLIS.toString()
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return executor.runAsync(DistributedLockOperation.RELEASE, keys, args)
            .mapResult(
                decode = { recordReleaseOutcome(handle, decodeRelease(it)) },
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
            recordReleaseOutcome(
                handle,
                decodeRelease(executor.runSuspending(DistributedLockOperation.RELEASE, keys, args)),
            )
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            waitSupport.close()
            watchdogs.values.forEach(CoordinationRuntime.CoordinationTaskRegistration::close)
            watchdogs.clear()
            registration.close()
        }
    }

    private fun registerWatchdog(
        result: LockAcquireResult<LockHandle>,
    ): LockAcquireResult<LockHandle> {
        val handle = when (result) {
            is LockAcquireResult.Acquired -> result.handle
            is LockAcquireResult.Reentered -> result.handle
            else -> return result
        }
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockAcquireResult.Closed
        } else {
            LockAcquireResult.Ambiguous(
                handle.ownerId,
                handle.requestId,
                LockRecoveryAction.RECONCILE_REQUEST,
            )
        }
    }

    private fun registerWatchdog(
        result: LockReconcileResult<LockHandle>,
    ): LockReconcileResult<LockHandle> {
        val handle = (result as? LockReconcileResult.Owned)?.handle ?: return result
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockReconcileResult.Closed
        } else {
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST)
        }
    }

    private fun ensureWatchdog(handle: LockHandle): Boolean {
        val policy = handle.leasePolicy as? LeasePolicy.Watchdog ?: return true
        discardClosedWatchdogs()
        var registered = true
        var capacityRejected = false
        watchdogs.compute(handle) { _, current ->
            if (current != null && !current.isClosed) {
                current
            } else {
                try {
                    registration.registerWatchdog(
                        ttl = policy.ttl.toKotlinDuration(),
                        renewalInterval = policy.renewalInterval.toKotlinDuration(),
                        generation = handle.generation.value,
                        maxLifetime = policy.maxLifetime.toKotlinDuration(),
                        onOwnershipLost = {
                            watchdogs.computeIfPresent(handle) { _, current ->
                                current.takeUnless { it.isClosed }
                            }
                            recordOwnershipLoss(policy)
                        },
                    ) {
                        renewForWatchdog(handle, policy)
                    }
                } catch (_: CoordinationCapacityException) {
                    capacityRejected = true
                    registered = false
                    null
                } catch (_: IllegalStateException) {
                    registered = false
                    null
                }
            }
        }
        if (capacityRejected) {
            recordCapacityRejection(policy)
        }
        return registered
    }

    private fun renewForWatchdog(
        handle: LockHandle,
        policy: LeasePolicy.Watchdog,
    ): CompletableFuture<CoordinationRenewalOutcome> {
        if (closed.get()) {
            return CompletableFuture.completedFuture(CoordinationRenewalOutcome.OWNERSHIP_LOST)
        }
        return executor.runAsync(
            DistributedLockOperation.RENEW,
            keys,
            renewArgs(handle, policy.ttl),
        ).mapResult(
            decode = { decodeRenew(it, handle) },
            backend = { LockMutationResult.BackendFailure(it) },
            integrity = { LockMutationResult.IntegrityFailure(it) },
            recoveryAction = LockRecoveryAction.RETRY_SAME_HANDLE,
        ).thenApply { result ->
            when (result) {
                is LockMutationResult.Renewed -> CoordinationRenewalOutcome.RENEWED
                else -> CoordinationRenewalOutcome.OWNERSHIP_LOST
            }
        }
    }

    private fun recordRenewOutcome(
        handle: LockHandle,
        result: LockMutationResult<LockHandle>,
    ): LockMutationResult<LockHandle> {
        when (result) {
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun recordReleaseOutcome(
        handle: LockHandle,
        result: LockMutationResult<LockHandle>,
    ): LockMutationResult<LockHandle> {
        when (result) {
            is LockMutationResult.Released,
            LockMutationResult.AlreadyReleased,
            -> removeWatchdog(handle)
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun removeWatchdog(handle: LockHandle) {
        watchdogs.remove(handle)?.close()
    }

    private fun discardClosedWatchdogs() {
        watchdogs.entries.removeIf { it.value.isClosed }
    }

    private fun recordCapacityRejection(policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.CAPACITY_REJECTION_TOTAL,
            operation = LockOperation.ACQUIRE,
            outcome = LockOutcome.CAPACITY_REJECTED,
            policy = policy,
        )
    }

    private fun recordOwnershipLoss(policy: LeasePolicy) {
        recordObservation(
            counter = LockCounterName.OWNERSHIP_LOSS_TOTAL,
            operation = LockOperation.RENEW,
            outcome = LockOutcome.OWNERSHIP_LOST,
            policy = policy,
        )
    }

    private fun recordObservation(
        counter: LockCounterName,
        operation: LockOperation,
        outcome: LockOutcome,
        policy: LeasePolicy,
    ) {
        val leasePolicy = when (policy) {
            is LeasePolicy.Fixed -> LockLeasePolicyKind.FIXED
            is LeasePolicy.Watchdog -> LockLeasePolicyKind.WATCHDOG
        }
        val dimensions = LockDimensions(
            objectKind = LockKind.DISTRIBUTED,
            operation = operation,
            outcome = outcome,
            failureKind = null,
            leasePolicy = leasePolicy,
        )
        observationSink.recordSafely(
            LockObservation.Counter(
                name = counter,
                delta = 1L,
                dimensions = dimensions,
            ),
        )
        observationSink.recordSafely(
            LockObservation.Event(
                LockEvent(
                    objectKind = dimensions.objectKind,
                    operation = dimensions.operation,
                    outcome = dimensions.outcome,
                    failureKind = dimensions.failureKind,
                    leasePolicy = dimensions.leasePolicy,
                ),
            ),
        )
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
            backend = { acquireBackendResult(ownerId, requestId, it) },
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

internal fun <H: Serializable> acquireBackendResult(
    ownerId: LockOwnerId,
    requestId: LockRequestId,
    failure: LockBackendFailure,
): LockAcquireResult<H> =
    when (failure.kind) {
        LockBackendFailureKind.CONNECTION,
        LockBackendFailureKind.TIMEOUT,
        -> LockAcquireResult.Ambiguous(ownerId, requestId, LockRecoveryAction.RECONCILE_REQUEST)
        LockBackendFailureKind.COMMAND -> LockAcquireResult.BackendFailure(failure)
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
): CompletableFuture<R> {
    val mapped = CompletableFuture<R>()
    whenComplete { value, error ->
        if (mapped.isDone) return@whenComplete
        try {
            val result = if (error == null) {
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
            mapped.complete(result)
        } catch (failure: Throwable) {
            mapped.completeExceptionally(failure)
        }
    }
    mapped.whenComplete { _, _ ->
        if (mapped.isCancelled && !isDone) {
            cancel(false)
        }
    }
    return mapped
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
