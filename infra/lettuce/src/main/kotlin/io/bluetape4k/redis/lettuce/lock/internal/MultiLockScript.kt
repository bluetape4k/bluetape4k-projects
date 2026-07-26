package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationDeadline
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
import io.bluetape4k.redis.lettuce.lock.LockGeneration
import io.bluetape4k.redis.lettuce.lock.LockHandle
import io.bluetape4k.redis.lettuce.lock.LockInspectResult
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailure
import io.bluetape4k.redis.lettuce.lock.LockIntegrityFailureKind
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockDimensions
import io.bluetape4k.redis.lettuce.lock.LockEvent
import io.bluetape4k.redis.lettuce.lock.LockLeasePolicyKind
import io.bluetape4k.redis.lettuce.lock.LockMutationResult
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOperation
import io.bluetape4k.redis.lettuce.lock.LockOwnerId
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.LockReconcileResult
import io.bluetape4k.redis.lettuce.lock.LockRecoveryAction
import io.bluetape4k.redis.lettuce.lock.LockRequestId
import io.bluetape4k.redis.lettuce.lock.MultiLockConfig
import io.bluetape4k.redis.lettuce.lock.MultiLockHandle
import io.bluetape4k.redis.lettuce.lock.toRedisMillisCeil
import io.bluetape4k.redis.lettuce.lock.recordSafely
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
import io.lettuce.core.RedisException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.RedisCodec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.LockSupport
import kotlin.time.toKotlinDuration

internal data class MultiLockKeys(
    val states: List<String>,
    val generation: String,
    val holds: String,
    val terminal: String,
    val fingerprint: String,
) {
    val all: List<String> = states + generation + holds + terminal

    val protocolKeys: DistributedLockKeys =
        DistributedLockKeys(states.first(), generation, holds, terminal, fingerprint)
}

internal fun deriveMultiLockKeys(
    names: Collection<String>,
    config: MultiLockConfig,
    codec: RedisCodec<String, String>,
): MultiLockKeys {
    val snapshot = ArrayList<String>(minOf(config.maxKeys, 32))
    val distinct = HashSet<String>(minOf(config.maxKeys, 32))
    val iterator = names.iterator()
    require(iterator.hasNext()) { "Multi-lock names must not be empty." }
    while (iterator.hasNext()) {
        config.validateInputKeyCount(snapshot.size + 1)
        val name = config.lock.validateResourceName(iterator.next())
        require(distinct.add(name)) { "Multi-lock names must be distinct." }
        snapshot += name
    }
    val states = snapshot
        .map { name ->
            val hashTag = config.lock.hashTag ?: DEFAULT_MULTI_HASH_TAG
            config.lock.validateDerivedKey("${config.lock.namespace}:{$hashTag}:multi-lock:$name:state")
        }
        .sortedWith { left, right -> compareWireBytes(codec.encodeKey(left), codec.encodeKey(right)) }
    val digest = MessageDigest.getInstance("SHA-256")
    states.forEach { state ->
        val bytes = codec.encodeKey(state).remainingBytes()
        digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
        digest.update(bytes)
    }
    val fingerprint = digest.digest().take(MULTI_FINGERPRINT_BYTES).joinToString("") { "%02x".format(it) }
    val tag = config.lock.hashTag ?: DEFAULT_MULTI_HASH_TAG
    val prefix = "${config.lock.namespace}:{$tag}:multi-lock-group:$fingerprint"
    val generation = config.lock.validateDerivedKey("$prefix:generation")
    val holds = config.lock.validateDerivedKey("$prefix:holds")
    val terminal = config.lock.validateDerivedKey("$prefix:terminal")
    return MultiLockKeys(states, generation, holds, terminal, fingerprint).also { keys ->
        require(keys.all.map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size == 1) {
            "Derived multi-lock keys must share one Redis Cluster slot."
        }
    }
}

internal class MultiLockClient private constructor(
    private val keys: MultiLockKeys,
    private val config: MultiLockConfig,
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    private val observationSink: LockObservationSink,
) {
    private val closed = AtomicBoolean()
    private val pendingAsync = ConcurrentHashMap.newKeySet<CompletableFuture<LockAcquireResult<MultiLockHandle>>>()
    private val watchdogs =
        ConcurrentHashMap<MultiLockHandle, CoordinationRuntime.CoordinationTaskRegistration>()

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> =
        if (closed.get()) LockAcquireResult.Closed else multiClassified(
            { LockAcquireResult.BackendFailure(it) },
            { LockAcquireResult.IntegrityFailure(it) },
            LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(
                decodeMultiAcquire(
                    run(MultiLockOperation.ACQUIRE, acquireArguments(ownerId, requestId, leasePolicy)),
                    ownerId,
                    requestId,
                ),
            )
        }

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<MultiLockHandle>> {
        val raw = tryAcquireAsyncRaw(ownerId, requestId, leasePolicy)
        val target = CompletableFuture<LockAcquireResult<MultiLockHandle>>()
        raw.whenComplete { result, error ->
            if (target.isDone) {
                result.acquiredHandleOrNull()?.let(::releaseAbandoned)
            } else if (error != null) {
                target.completeExceptionally(error)
            } else {
                val registered = registerWatchdog(result)
                if (!target.complete(registered)) {
                    registered.acquiredHandleOrNull()?.let(::releaseAbandoned)
                }
            }
        }
        return target
    }

    suspend fun tryAcquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> =
        if (closed.get()) LockAcquireResult.Closed else multiClassifiedSuspending(
            { LockAcquireResult.BackendFailure(it) },
            { LockAcquireResult.IntegrityFailure(it) },
            LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(
                decodeMultiAcquire(
                    runAsync(MultiLockOperation.ACQUIRE, acquireArguments(ownerId, requestId, leasePolicy)).await(),
                    ownerId,
                    requestId,
                ),
            )
        }

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> {
        val deadline = multiDeadline(waitTime)
        while (true) {
            val result = tryAcquire(ownerId, requestId, leasePolicy)
            if (result !is LockAcquireResult.Contended) return result
            val remaining = deadline.remainingNanos()
            if (remaining == 0L) return LockAcquireResult.TimedOut
            LockSupport.parkNanos(minOf(remaining, MULTI_RETRY_DELAY.toNanos()))
        }
    }

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<MultiLockHandle>> {
        val deadline = multiDeadline(waitTime)
        val target = CompletableFuture<LockAcquireResult<MultiLockHandle>>()
        val scheduled = AtomicReference<CoordinationRuntime.CoordinationTaskRegistration?>()
        val inFlight = AtomicReference<CompletableFuture<LockAcquireResult<MultiLockHandle>>?>()
        pendingAsync += target
        lateinit var retry: () -> Unit
        fun schedule() {
            if (target.isDone) return
            val remaining = deadline.remainingNanos()
            if (remaining == 0L) {
                target.complete(LockAcquireResult.TimedOut)
                return
            }
            try {
                val task = registration.registerTask(
                    Duration.ofNanos(minOf(remaining, MULTI_RETRY_DELAY.toNanos())).toKotlinDuration(),
                    retry,
                )
                scheduled.getAndSet(task)?.close()
                if (target.isDone) scheduled.getAndSet(null)?.close()
            } catch (_: CoordinationCapacityException) {
                target.complete(LockAcquireResult.CapacityExceeded)
            } catch (_: IllegalStateException) {
                target.complete(LockAcquireResult.Closed)
            }
        }
        retry = {
            scheduled.getAndSet(null)?.close()
            if (closed.get()) target.complete(LockAcquireResult.Closed)
            else if (!target.isDone) {
                val pending = try {
                    tryAcquireAsyncRaw(ownerId, requestId, leasePolicy)
                } catch (error: Throwable) {
                    target.completeExceptionally(error)
                    null
                }
                pending?.let {
                    inFlight.set(it)
                    if (target.isDone) inFlight.getAndSet(null)?.cancel(false)
                    it.whenComplete { result, error ->
                        inFlight.compareAndSet(it, null)
                        if (target.isDone) {
                            result.acquiredHandleOrNull()?.let(::releaseAbandoned)
                        } else {
                            if (error != null) target.completeExceptionally(error)
                            else if (result is LockAcquireResult.Contended) schedule()
                            else {
                                val registered = registerWatchdog(result)
                                if (!target.complete(registered)) {
                                    registered.acquiredHandleOrNull()?.let(::releaseAbandoned)
                                }
                            }
                        }
                    }
                }
            }
        }
        target.whenComplete { _, _ ->
            pendingAsync -= target
            scheduled.getAndSet(null)?.close()
            inFlight.getAndSet(null)?.let { pending -> if (!pending.isDone) pending.cancel(false) }
        }
        retry()
        return target
    }

    suspend fun acquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<MultiLockHandle> {
        val deadline = multiDeadline(waitTime)
        while (true) {
            val result = tryAcquireSuspending(ownerId, requestId, leasePolicy)
            if (result !is LockAcquireResult.Contended) return result
            val remaining = deadline.remainingNanos()
            if (remaining == 0L) return LockAcquireResult.TimedOut
            delay(Duration.ofNanos(minOf(remaining, MULTI_RETRY_DELAY.toNanos())).toKotlinDuration())
        }
    }

    fun inspect(handle: MultiLockHandle): LockInspectResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return multiClassified(
            { LockInspectResult.BackendFailure(it) },
            { LockInspectResult.IntegrityFailure(it) },
            LockRecoveryAction.INSPECT_HANDLE,
        ) { decodeMultiInspect(run(MultiLockOperation.INSPECT, handleArguments(handle)), handle) }
    }

    fun inspectAsync(handle: MultiLockHandle): CompletableFuture<LockInspectResult<MultiLockHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(LockInspectResult.Closed)
        return runAsync(MultiLockOperation.INSPECT, handleArguments(handle)).multiMap(
            { decodeMultiInspect(it, handle) },
            { LockInspectResult.BackendFailure(it) },
            { LockInspectResult.IntegrityFailure(it) },
            LockRecoveryAction.INSPECT_HANDLE,
        )
    }

    suspend fun inspectSuspending(handle: MultiLockHandle): LockInspectResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockInspectResult.Closed
        return multiClassifiedSuspending(
            { LockInspectResult.BackendFailure(it) },
            { LockInspectResult.IntegrityFailure(it) },
            LockRecoveryAction.INSPECT_HANDLE,
        ) { decodeMultiInspect(runAsync(MultiLockOperation.INSPECT, handleArguments(handle)).await(), handle) }
    }

    fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId): LockReconcileResult<MultiLockHandle> =
        if (closed.get()) LockReconcileResult.Closed else multiClassified(
            { LockReconcileResult.BackendFailure(it) },
            { LockReconcileResult.IntegrityFailure(it) },
            LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(
                decodeMultiReconcile(
                    run(MultiLockOperation.RECONCILE, requestArguments(ownerId, requestId)),
                    ownerId,
                    requestId,
                ),
            )
        }

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<MultiLockHandle>> =
        if (closed.get()) CompletableFuture.completedFuture(LockReconcileResult.Closed) else
            runAsync(MultiLockOperation.RECONCILE, requestArguments(ownerId, requestId)).multiMap(
                { registerWatchdog(decodeMultiReconcile(it, ownerId, requestId)) },
                { LockReconcileResult.BackendFailure(it) },
                { LockReconcileResult.IntegrityFailure(it) },
                LockRecoveryAction.RECONCILE_REQUEST,
            )

    suspend fun reconcileSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<MultiLockHandle> =
        if (closed.get()) LockReconcileResult.Closed else multiClassifiedSuspending(
            { LockReconcileResult.BackendFailure(it) },
            { LockReconcileResult.IntegrityFailure(it) },
            LockRecoveryAction.RECONCILE_REQUEST,
        ) {
            registerWatchdog(
                decodeMultiReconcile(
                    runAsync(MultiLockOperation.RECONCILE, requestArguments(ownerId, requestId)).await(),
                    ownerId,
                    requestId,
                ),
            )
        }

    fun renew(handle: MultiLockHandle, extension: Duration): LockMutationResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockMutationResult.Closed
        return multiClassified(
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordRenewOutcome(
                handle,
                decodeMultiMutation(
                    run(MultiLockOperation.RENEW, handleArguments(handle) + extension.toRedisMillisCeil()),
                    handle,
                ),
            )
        }
    }

    fun renewAsync(
        handle: MultiLockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<MultiLockHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return runAsync(
            MultiLockOperation.RENEW,
            handleArguments(handle) + extension.toRedisMillisCeil(),
        ).multiMap(
            { recordRenewOutcome(handle, decodeMultiMutation(it, handle)) },
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        )
    }

    suspend fun renewSuspending(
        handle: MultiLockHandle,
        extension: Duration,
    ): LockMutationResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockMutationResult.Closed
        return multiClassifiedSuspending(
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordRenewOutcome(
                handle,
                decodeMultiMutation(
                    runAsync(MultiLockOperation.RENEW, handleArguments(handle) + extension.toRedisMillisCeil()).await(),
                    handle,
                ),
            )
        }
    }

    fun release(handle: MultiLockHandle): LockMutationResult<MultiLockHandle> =
        mutation(handle)

    fun releaseAsync(handle: MultiLockHandle): CompletableFuture<LockMutationResult<MultiLockHandle>> =
        mutationAsync(handle)

    suspend fun releaseSuspending(handle: MultiLockHandle): LockMutationResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockMutationResult.Closed
        return multiClassifiedSuspending(
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordReleaseOutcome(
                handle,
                decodeMultiMutation(
                    runAsync(MultiLockOperation.RELEASE, handleArguments(handle) + MULTI_TERMINAL_TTL_MILLIS).await(),
                    handle,
                ),
            )
        }
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            pendingAsync.forEach { it.complete(LockAcquireResult.Closed) }
            watchdogs.values.forEach(CoordinationRuntime.CoordinationTaskRegistration::close)
            watchdogs.clear()
            registration.close()
        }
    }

    private fun mutation(handle: MultiLockHandle): LockMutationResult<MultiLockHandle> {
        validateHandle(handle)
        if (closed.get()) return LockMutationResult.Closed
        return multiClassified(
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        ) {
            recordReleaseOutcome(
                handle,
                decodeMultiMutation(
                    run(MultiLockOperation.RELEASE, handleArguments(handle) + MULTI_TERMINAL_TTL_MILLIS),
                    handle,
                ),
            )
        }
    }

    private fun mutationAsync(handle: MultiLockHandle): CompletableFuture<LockMutationResult<MultiLockHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(LockMutationResult.Closed)
        return runAsync(MultiLockOperation.RELEASE, handleArguments(handle) + MULTI_TERMINAL_TTL_MILLIS).multiMap(
            { recordReleaseOutcome(handle, decodeMultiMutation(it, handle)) },
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        )
    }

    private fun run(operation: MultiLockOperation, arguments: List<Any>): List<String> =
        RedisScriptRunner.run(
            sync,
            MULTI_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all.toTypedArray(),
            operation.wire,
            keys.states.size.toString(),
            keys.fingerprint,
            *arguments.map(Any::toString).toTypedArray(),
        )

    private fun runAsync(operation: MultiLockOperation, arguments: List<Any>): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(
            async,
            MULTI_LOCK_SCRIPT,
            ScriptOutputType.MULTI,
            keys.all.toTypedArray(),
            operation.wire,
            keys.states.size.toString(),
            keys.fingerprint,
            *arguments.map(Any::toString).toTypedArray(),
        )

    private fun tryAcquireAsyncRaw(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<MultiLockHandle>> =
        if (closed.get()) {
            CompletableFuture.completedFuture(LockAcquireResult.Closed)
        } else {
            runAsync(MultiLockOperation.ACQUIRE, acquireArguments(ownerId, requestId, leasePolicy)).multiMap(
                { decodeMultiAcquire(it, ownerId, requestId) },
                { LockAcquireResult.BackendFailure(it) },
                { LockAcquireResult.IntegrityFailure(it) },
                LockRecoveryAction.RECONCILE_REQUEST,
            )
        }

    private fun releaseAbandoned(handle: MultiLockHandle) {
        removeWatchdog(handle)
        runAsync(
            MultiLockOperation.RELEASE,
            handleArguments(handle) + MULTI_TERMINAL_TTL_MILLIS,
        ).exceptionally { null }
    }

    private fun acquireArguments(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): List<Any> {
        val encoded = encodeLeasePolicy(leasePolicy)
        return listOf(
            ownerId.value,
            requestId.value,
            encoded.wireValue,
            encoded.ttlMillis,
            config.lock.maxReentrantHolds,
            MULTI_TERMINAL_TTL_MILLIS,
        )
    }

    private fun requestArguments(ownerId: LockOwnerId, requestId: LockRequestId): List<Any> =
        listOf(ownerId.value, requestId.value)

    private fun handleArguments(handle: MultiLockHandle): List<Any> =
        listOf(handle.lock.ownerId.value, handle.lock.requestId.value, handle.lock.generation.value)

    private fun validateHandle(handle: MultiLockHandle) {
        require(handle.lock.kind == LockKind.MULTI) { "Handle kind must be MULTI." }
        require(handle.lock.objectFingerprint == keys.fingerprint) {
            "Handle belongs to a different multi-lock object."
        }
        require(handle.constituentCount == keys.states.size) {
            "Handle constituent count does not match this multi-lock object."
        }
    }

    private fun decodeMultiAcquire(
        raw: Any?,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockAcquireResult<MultiLockHandle> =
        decodeAcquire(raw, keys.protocolKeys, ownerId, requestId).toMulti(keys.states.size)

    private fun decodeMultiInspect(raw: Any?, handle: MultiLockHandle): LockInspectResult<MultiLockHandle> =
        decodeInspect(raw, keys.protocolKeys, handle.lock.asDistributed()).toMulti(keys.states.size)

    private fun decodeMultiReconcile(
        raw: Any?,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<MultiLockHandle> =
        decodeReconcile(raw, keys.protocolKeys, ownerId, requestId).toMulti(keys.states.size)

    private fun decodeMultiMutation(
        raw: Any?,
        handle: MultiLockHandle,
    ): LockMutationResult<MultiLockHandle> =
        decodeRenewOrRelease(raw, handle)

    private fun decodeRenewOrRelease(raw: Any?, handle: MultiLockHandle): LockMutationResult<MultiLockHandle> {
        val tag = (raw as? List<*>)?.firstOrNull()?.toString()
        return if (tag == "RENEWED") {
            decodeRenew(raw, handle.lock.asDistributed()).toMulti(keys.states.size)
        } else {
            decodeRelease(raw).toMulti()
        }
    }

    private fun registerWatchdog(
        result: LockAcquireResult<MultiLockHandle>,
    ): LockAcquireResult<MultiLockHandle> {
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
                handle.lock.ownerId,
                handle.lock.requestId,
                LockRecoveryAction.RECONCILE_REQUEST,
            )
        }
    }

    private fun registerWatchdog(
        result: LockReconcileResult<MultiLockHandle>,
    ): LockReconcileResult<MultiLockHandle> {
        val handle = (result as? LockReconcileResult.Owned)?.handle ?: return result
        return if (ensureWatchdog(handle)) {
            result
        } else if (closed.get() || registration.isClosed) {
            LockReconcileResult.Closed
        } else {
            LockReconcileResult.Ambiguous(LockRecoveryAction.RECONCILE_REQUEST)
        }
    }

    private fun ensureWatchdog(handle: MultiLockHandle): Boolean {
        val policy = handle.lock.leasePolicy as? LeasePolicy.Watchdog ?: return true
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
                        generation = handle.lock.generation.value,
                        maxLifetime = policy.maxLifetime.toKotlinDuration(),
                        onOwnershipLost = {
                            watchdogs.computeIfPresent(handle) { _, task -> task.takeUnless { it.isClosed } }
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
        if (capacityRejected) recordCapacityRejection(policy)
        return registered
    }

    private fun renewForWatchdog(
        handle: MultiLockHandle,
        policy: LeasePolicy.Watchdog,
    ): CompletableFuture<CoordinationRenewalOutcome> {
        if (closed.get()) {
            return CompletableFuture.completedFuture(CoordinationRenewalOutcome.OWNERSHIP_LOST)
        }
        return runAsync(
            MultiLockOperation.RENEW,
            handleArguments(handle) + policy.ttl.toRedisMillisCeil(),
        ).multiMap(
            { decodeMultiMutation(it, handle) },
            { LockMutationResult.BackendFailure(it) },
            { LockMutationResult.IntegrityFailure(it) },
            LockRecoveryAction.RETRY_SAME_HANDLE,
        ).thenApply { result ->
            when (result) {
                is LockMutationResult.Renewed -> CoordinationRenewalOutcome.RENEWED
                else -> CoordinationRenewalOutcome.OWNERSHIP_LOST
            }
        }
    }

    private fun recordRenewOutcome(
        handle: MultiLockHandle,
        result: LockMutationResult<MultiLockHandle>,
    ): LockMutationResult<MultiLockHandle> {
        when (result) {
            LockMutationResult.AlreadyReleased,
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.lock.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun recordReleaseOutcome(
        handle: MultiLockHandle,
        result: LockMutationResult<MultiLockHandle>,
    ): LockMutationResult<MultiLockHandle> {
        when (result) {
            is LockMutationResult.Released,
            LockMutationResult.AlreadyReleased,
            -> removeWatchdog(handle)
            LockMutationResult.Expired,
            LockMutationResult.OwnershipLost,
            LockMutationResult.StaleGeneration,
            -> {
                recordOwnershipLoss(handle.lock.leasePolicy)
                removeWatchdog(handle)
            }
            else -> Unit
        }
        return result
    }

    private fun removeWatchdog(handle: MultiLockHandle) {
        watchdogs.remove(handle)?.close()
    }

    private fun discardClosedWatchdogs() {
        watchdogs.entries.removeIf { it.value.isClosed }
    }

    private fun recordCapacityRejection(policy: LeasePolicy) {
        recordObservation(
            LockCounterName.CAPACITY_REJECTION_TOTAL,
            LockOperation.ACQUIRE,
            LockOutcome.CAPACITY_REJECTED,
            policy,
        )
    }

    private fun recordOwnershipLoss(policy: LeasePolicy) {
        recordObservation(
            LockCounterName.OWNERSHIP_LOSS_TOTAL,
            LockOperation.RENEW,
            LockOutcome.OWNERSHIP_LOST,
            policy,
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
            objectKind = LockKind.MULTI,
            operation = operation,
            outcome = outcome,
            failureKind = null,
            leasePolicy = leasePolicy,
        )
        observationSink.recordSafely(LockObservation.Counter(counter, 1L, dimensions))
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

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): MultiLockClient {
            val keys = deriveMultiLockKeys(names, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return MultiLockClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                runtime.registerObject(keys.fingerprint),
                observationSink,
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            names: Collection<String>,
            config: MultiLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): MultiLockClient {
            val keys = deriveMultiLockKeys(names, config, connection.codec)
            val runtime = CoordinationRuntime.forConnection(
                connection,
                scheduler = scheduler?.let(::ScheduledExecutorCoordinationScheduler),
            )
            return MultiLockClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                runtime.registerObject(keys.fingerprint),
                observationSink,
            )
        }
    }
}

private enum class MultiLockOperation(val wire: String) {
    ACQUIRE("ACQUIRE"),
    INSPECT("INSPECT"),
    RECONCILE("RECONCILE"),
    RENEW("RENEW"),
    RELEASE("RELEASE"),
}

private fun LockHandle.asDistributed(): LockHandle = copy(kind = LockKind.DISTRIBUTED)
private fun LockHandle.asMulti(): LockHandle = copy(kind = LockKind.MULTI)

private fun LockAcquireResult<MultiLockHandle>?.acquiredHandleOrNull(): MultiLockHandle? =
    when (this) {
        is LockAcquireResult.Acquired -> handle
        is LockAcquireResult.Reentered -> handle
        else -> null
    }

private fun LockAcquireResult<LockHandle>.toMulti(count: Int): LockAcquireResult<MultiLockHandle> =
    when (this) {
        is LockAcquireResult.Acquired -> LockAcquireResult.Acquired(MultiLockHandle(handle.asMulti(), count))
        is LockAcquireResult.Reentered -> LockAcquireResult.Reentered(MultiLockHandle(handle.asMulti(), count), holdCount)
        else -> @Suppress("UNCHECKED_CAST") (this as LockAcquireResult<MultiLockHandle>)
    }

private fun LockInspectResult<LockHandle>.toMulti(count: Int): LockInspectResult<MultiLockHandle> =
    when (this) {
        is LockInspectResult.Owned ->
            LockInspectResult.Owned(MultiLockHandle(handle.asMulti(), count), holdCount, remainingTtlMillis)
        else -> @Suppress("UNCHECKED_CAST") (this as LockInspectResult<MultiLockHandle>)
    }

private fun LockReconcileResult<LockHandle>.toMulti(count: Int): LockReconcileResult<MultiLockHandle> =
    when (this) {
        is LockReconcileResult.Owned ->
            LockReconcileResult.Owned(MultiLockHandle(handle.asMulti(), count), holdCount, remainingTtlMillis)
        else -> @Suppress("UNCHECKED_CAST") (this as LockReconcileResult<MultiLockHandle>)
    }

private fun LockMutationResult<LockHandle>.toMulti(count: Int): LockMutationResult<MultiLockHandle> =
    when (this) {
        is LockMutationResult.Renewed ->
            LockMutationResult.Renewed(MultiLockHandle(handle.asMulti(), count), remainingTtlMillis)
        else -> @Suppress("UNCHECKED_CAST") (this as LockMutationResult<MultiLockHandle>)
    }

private fun LockMutationResult<LockHandle>.toMulti(): LockMutationResult<MultiLockHandle> =
    @Suppress("UNCHECKED_CAST")
    (this as LockMutationResult<MultiLockHandle>)

private inline fun <R> multiClassified(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
    block: () -> R,
): R =
    try {
        block()
    } catch (error: CoordinationProtocolException) {
        if (error.classification == CoordinationFailureClassification.INTEGRITY) {
            integrity(MULTI_MALFORMED)
        } else {
            backend(multiBackend(error, action))
        }
    } catch (_: IllegalArgumentException) {
        integrity(MULTI_MALFORMED)
    } catch (error: Exception) {
        backend(multiBackend(error, action))
    }

private suspend inline fun <R> multiClassifiedSuspending(
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
    crossinline block: suspend () -> R,
): R =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: CoordinationProtocolException) {
        if (error.classification == CoordinationFailureClassification.INTEGRITY) {
            integrity(MULTI_MALFORMED)
        } else {
            backend(multiBackend(error, action))
        }
    } catch (_: IllegalArgumentException) {
        integrity(MULTI_MALFORMED)
    } catch (error: Exception) {
        backend(multiBackend(error, action))
    }

private fun <R> CompletableFuture<List<String>>.multiMap(
    decode: (List<String>) -> R,
    backend: (LockBackendFailure) -> R,
    integrity: (LockIntegrityFailure) -> R,
    action: LockRecoveryAction,
): CompletableFuture<R> {
    val mapped = CompletableFuture<R>()
    whenComplete { value, error ->
        if (mapped.isDone) return@whenComplete
        if (error == null) {
            try {
                mapped.complete(decode(value))
            } catch (_: CoordinationProtocolException) {
                mapped.complete(integrity(MULTI_MALFORMED))
            } catch (_: IllegalArgumentException) {
                mapped.complete(integrity(MULTI_MALFORMED))
            } catch (failure: Throwable) {
                mapped.completeExceptionally(failure)
            }
        } else {
            try {
                mapped.complete(backend(multiBackend(error, action)))
            } catch (failure: Throwable) {
                mapped.completeExceptionally(failure)
            }
        }
    }
    mapped.whenComplete { _, _ -> if (mapped.isCancelled && !isDone) cancel(false) }
    return mapped
}

private fun multiBackend(error: Throwable, action: LockRecoveryAction): LockBackendFailure {
    val cause = error.multiUnwrap()
    val kind = when (cause) {
        is RedisConnectionException -> LockBackendFailureKind.CONNECTION
        is RedisCommandTimeoutException, is TimeoutException -> LockBackendFailureKind.TIMEOUT
        is RedisException -> LockBackendFailureKind.COMMAND
        else -> throw cause
    }
    return LockBackendFailure(kind, action)
}

private fun Throwable.multiUnwrap(): Throwable {
    val seen = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
    var current = this
    repeat(8) {
        if (current is java.util.concurrent.CancellationException) throw current
        if (!seen.add(current)) return current
        current = when (current) {
            is CompletionException, is ExecutionException -> current.cause ?: return current
            else -> return current
        }
    }
    return current
}

private fun multiDeadline(waitTime: Duration): CoordinationDeadline {
    waitTime.toRedisMillisCeil()
    require(waitTime <= Duration.ofHours(24)) { "Lock wait time must not exceed PT24H." }
    return CoordinationDeadline.after(waitTime.toKotlinDuration())
}

private fun ByteBuffer.remainingBytes(): ByteArray =
    duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }

private fun compareWireBytes(left: ByteBuffer, right: ByteBuffer): Int {
    val a = left.remainingBytes()
    val b = right.remainingBytes()
    val limit = minOf(a.size, b.size)
    for (index in 0 until limit) {
        val comparison = (a[index].toInt() and 0xff).compareTo(b[index].toInt() and 0xff)
        if (comparison != 0) return comparison
    }
    return a.size.compareTo(b.size)
}

private val MULTI_MALFORMED = LockIntegrityFailure(LockIntegrityFailureKind.MALFORMED_REPLY)
private val MULTI_RETRY_DELAY: Duration = Duration.ofMillis(10)
private const val MULTI_FINGERPRINT_BYTES = 16
private const val DEFAULT_MULTI_HASH_TAG = "multi"
private const val MULTI_TERMINAL_TTL_MILLIS = 7L * 24L * 60L * 60L * 1_000L

private val MULTI_LOCK_SCRIPT = RedisScript(
    """
    local operation = ARGV[1]
    local count = tonumber(ARGV[2])
    local group = ARGV[3]
    local generation_key = KEYS[count + 1]
    local holds_key = KEYS[count + 2]
    local terminal_key = KEYS[count + 3]

    local function key_type(key)
      local result = redis.call('TYPE', key)
      return type(result) == 'table' and result.ok or result
    end

    local function positive(value)
      return type(value) == 'string' and string.match(value, '^[1-9][0-9]*$') ~= nil
        and string.len(value) <= 16 and (string.len(value) < 16 or value <= '9007199254740991')
    end

    local function compare_decimal(left, right)
      if string.len(left) ~= string.len(right) then return string.len(left) < string.len(right) and -1 or 1 end
      if left == right then return 0 end
      return left < right and -1 or 1
    end

    local function validate_terminal()
      local kind = key_type(terminal_key)
      if kind == 'none' then return true end
      return kind == 'hash' and redis.call('PTTL', terminal_key) > 0
        and redis.call('HLEN', terminal_key) == 4
        and redis.call('HGET', terminal_key, 'group') == group
        and redis.call('HGET', terminal_key, 'owner') ~= false
        and positive(redis.call('HGET', terminal_key, 'generation'))
        and redis.call('HGET', terminal_key, 'request') ~= false
    end

    local generation_type = key_type(generation_key)
    if generation_type ~= 'none' and generation_type ~= 'string' then return {'INTEGRITY'} end
    if generation_type == 'string' then
      local counter = redis.call('GET', generation_key)
      if not positive(counter) or redis.call('PTTL', generation_key) ~= -1 then return {'INTEGRITY'} end
    end
    if not validate_terminal() then return {'INTEGRITY'} end

    local active = 0
    for index = 1, count do
      local kind = key_type(KEYS[index])
      if kind == 'hash' then active = active + 1 elseif kind ~= 'none' then return {'INTEGRITY'} end
    end
    if active ~= 0 and active ~= count then return {'INTEGRITY'} end

    local current_owner, current_generation, hold_count
    if active == count then
      if key_type(holds_key) ~= 'hash' or redis.call('PTTL', holds_key) <= 0 then return {'INTEGRITY'} end
      for index = 1, count do
        local key = KEYS[index]
        if redis.call('PTTL', key) <= 0 or redis.call('HLEN', key) ~= 4
          or redis.call('HGET', key, 'group') ~= group then return {'INTEGRITY'} end
        local owner = redis.call('HGET', key, 'owner')
        local generation = redis.call('HGET', key, 'generation')
        local holds = redis.call('HGET', key, 'holdCount')
        if owner == false or not positive(generation) or not positive(holds) then return {'INTEGRITY'} end
        if index == 1 then
          current_owner, current_generation, hold_count = owner, generation, holds
        elseif owner ~= current_owner or generation ~= current_generation or holds ~= hold_count then
          return {'INTEGRITY'}
        end
      end
      if tonumber(hold_count) ~= redis.call('HLEN', holds_key) then return {'INTEGRITY'} end
      local counter = redis.call('GET', generation_key)
      if counter == false or compare_decimal(current_generation, counter) > 0 then return {'INTEGRITY'} end
    elseif key_type(holds_key) ~= 'none' then
      return {'INTEGRITY'}
    end

    if operation == 'ACQUIRE' then
      local owner, request, policy = ARGV[4], ARGV[5], ARGV[6]
      local ttl, maximum = tonumber(ARGV[7]), tonumber(ARGV[8])
      if active == 0 then
        local counter = redis.call('GET', generation_key)
        if counter ~= false and compare_decimal(counter, '9007199254740990') > 0 then return {'CAPACITY'} end
        redis.call('INCR', generation_key)
        local generation = redis.call('GET', generation_key)
        redis.call('DEL', terminal_key)
        for index = 1, count do
          redis.call('HSET', KEYS[index], 'group', group, 'owner', owner, 'generation', generation, 'holdCount', 1)
          redis.call('PEXPIRE', KEYS[index], ttl)
        end
        redis.call('HSET', holds_key, request, generation .. '|A|' .. policy)
        redis.call('PEXPIRE', holds_key, ttl)
        return {'ACQUIRED', generation, '1', tostring(ttl), policy}
      end
      if current_owner ~= owner then return {'CONTENDED', tostring(redis.call('PTTL', KEYS[1]))} end
      local existing = redis.call('HGET', holds_key, request)
      if existing ~= false then
        local generation, acquisition, existing_policy = string.match(existing, '^([1-9][0-9]*)|([AR])|(.+)$')
        if generation ~= current_generation then return {'INTEGRITY'} end
        return {acquisition == 'A' and 'REPLAY' or 'REENTERED', generation, hold_count,
          tostring(redis.call('PTTL', KEYS[1])), existing_policy}
      end
      if tonumber(hold_count) >= maximum then return {'CAPACITY'} end
      hold_count = tostring(tonumber(hold_count) + 1)
      for index = 1, count do
        redis.call('HSET', KEYS[index], 'holdCount', hold_count)
        redis.call('PEXPIRE', KEYS[index], ttl)
      end
      redis.call('HSET', holds_key, request, current_generation .. '|R|' .. policy)
      redis.call('PEXPIRE', holds_key, ttl)
      return {'REENTERED', current_generation, hold_count, tostring(ttl), policy}
    end

    local owner, request = ARGV[4], ARGV[5]
    if operation == 'RECONCILE' then
      if active == 0 then
        if key_type(terminal_key) == 'hash' and redis.call('HGET', terminal_key, 'owner') == owner
          and redis.call('HGET', terminal_key, 'request') == request then return {'RELEASED'} end
        return {'NOT_FOUND'}
      end
      if current_owner ~= owner then return {'NOT_FOUND'} end
      local hold = redis.call('HGET', holds_key, request)
      if hold == false then return {'NOT_FOUND'} end
      local generation, _, policy = string.match(hold, '^([1-9][0-9]*)|([AR])|(.+)$')
      if generation ~= current_generation then return {'INTEGRITY'} end
      return {'OWNED', generation, hold_count, tostring(redis.call('PTTL', KEYS[1])), policy}
    end

    local generation = ARGV[6]
    if active == 0 then
      if key_type(terminal_key) == 'hash' and redis.call('HGET', terminal_key, 'group') == group
        and redis.call('HGET', terminal_key, 'owner') == owner
        and redis.call('HGET', terminal_key, 'request') == request
        and redis.call('HGET', terminal_key, 'generation') == generation then return {'ALREADY_RELEASED'} end
      local counter = redis.call('GET', generation_key)
      if counter ~= false and compare_decimal(counter, generation) > 0 then return {'STALE'} end
      return {'EXPIRED'}
    end
    if compare_decimal(current_generation, generation) > 0 then return {'STALE'} end
    if current_generation ~= generation then return {'INTEGRITY'} end
    if current_owner ~= owner then return {'LOST'} end
    local hold = redis.call('HGET', holds_key, request)
    if hold == false then return operation == 'INSPECT' and {'RELEASED'} or {'ALREADY_RELEASED'} end
    local hold_generation, _, policy = string.match(hold, '^([1-9][0-9]*)|([AR])|(.+)$')
    if hold_generation ~= generation then return {'INTEGRITY'} end

    if operation == 'INSPECT' then
      return {'OWNED', generation, hold_count, tostring(redis.call('PTTL', KEYS[1])), policy}
    elseif operation == 'RENEW' then
      local ttl = tonumber(ARGV[7])
      for index = 1, count do redis.call('PEXPIRE', KEYS[index], ttl) end
      redis.call('PEXPIRE', holds_key, ttl)
      return {'RENEWED', tostring(ttl)}
    elseif operation == 'RELEASE' then
      redis.call('HDEL', holds_key, request)
      local remaining = tonumber(hold_count) - 1
      if remaining > 0 then
        for index = 1, count do redis.call('HSET', KEYS[index], 'holdCount', remaining) end
      else
        for index = 1, count do redis.call('DEL', KEYS[index]) end
        redis.call('DEL', holds_key)
        redis.call('HSET', terminal_key, 'group', group, 'owner', owner, 'generation', generation, 'request', request)
        redis.call('PEXPIRE', terminal_key, tonumber(ARGV[7]))
      end
      return {'RELEASED', tostring(remaining)}
    end
    return {'INTEGRITY'}
    """.trimIndent(),
)
