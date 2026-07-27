package io.bluetape4k.redis.lettuce.synchronizer.internal

import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.synchronizer.ExpirablePermitHandle
import io.bluetape4k.redis.lettuce.synchronizer.ExpirablePermitLease
import io.bluetape4k.redis.lettuce.synchronizer.ExpirableSemaphoreConfig
import io.bluetape4k.redis.lettuce.synchronizer.PermitAcquireResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitHandle
import io.bluetape4k.redis.lettuce.synchronizer.PermitInspectResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitMutationResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitReconcileResult
import io.bluetape4k.redis.lettuce.synchronizer.PermitRenewResult
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreInitializationResult
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreOwnerId
import io.bluetape4k.redis.lettuce.synchronizer.SemaphoreRequestId
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerBackendFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailure
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerIntegrityFailureKind
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerRecoveryAction
import io.bluetape4k.redis.lettuce.synchronizer.SynchronizerScripts
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisConnectionException
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

internal class ExpirableSemaphoreClient private constructor(
    private val keys: SemaphoreKeys,
    private val config: ExpirableSemaphoreConfig,
    private val sync: RedisScriptingCommands<String, String>,
    private val async: RedisScriptingAsyncCommands<String, String>,
    private val bootstrap: SemaphoreClient,
    private val poller: SynchronizerAsyncPoller,
) {
    private val closed = AtomicBoolean()
    private val cleanupLimit get() = config.cleanupBatchLimit.toString()

    fun trySetPermits(permits: Int): SemaphoreInitializationResult = bootstrap.trySetPermits(permits)
    fun trySetPermitsAsync(permits: Int) = bootstrap.trySetPermitsAsync(permits)

    fun availablePermits(): Int {
        if (closed.get()) return -1
        return decodeReply(run(SynchronizerScripts.EXPIRABLE_CLEANUP_SCRIPT, cleanupLimit))
            ?.singleOrNull()?.toIntOrNull() ?: -1
    }

    fun availablePermitsAsync(): CompletableFuture<Int> {
        if (closed.get()) return CompletableFuture.completedFuture(-1)
        return runAsync(SynchronizerScripts.EXPIRABLE_CLEANUP_SCRIPT, cleanupLimit)
            .thenApply { decodeReply(it)?.singleOrNull()?.toIntOrNull() ?: -1 }
    }

    fun tryAcquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
    ): PermitAcquireResult<ExpirablePermitHandle> {
        validatePermits(permits)
        if (closed.get()) return PermitAcquireResult.Closed
        return classified(
            { ambiguousAcquire(it, requestId) },
            { PermitAcquireResult.IntegrityFailure(it) },
        ) {
            decodeAcquire(
                run(
                    SynchronizerScripts.EXPIRABLE_ACQUIRE_SCRIPT,
                    cleanupLimit,
                    permits.toString(),
                    ownerId.value,
                    requestId.value,
                    token(),
                    List(permits) { token() }.joinToString(","),
                    config.leaseTime.toMillis().toString(),
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
    ): CompletableFuture<PermitAcquireResult<ExpirablePermitHandle>> {
        validatePermits(permits)
        if (closed.get()) return CompletableFuture.completedFuture(PermitAcquireResult.Closed)
        return runAsync(
            SynchronizerScripts.EXPIRABLE_ACQUIRE_SCRIPT,
            cleanupLimit,
            permits.toString(),
            ownerId.value,
            requestId.value,
            token(),
            List(permits) { token() }.joinToString(","),
            config.leaseTime.toMillis().toString(),
        ).classifiedFuture(
            { decodeAcquire(it, ownerId, requestId) },
            { ambiguousAcquire(it, requestId) },
            { PermitAcquireResult.IntegrityFailure(it) },
        )
    }

    fun inspect(handle: ExpirablePermitHandle): PermitInspectResult<ExpirablePermitHandle> {
        validateHandle(handle)
        if (closed.get()) return PermitInspectResult.Closed
        return classified(
            { PermitInspectResult.BackendFailure(it) },
            { PermitInspectResult.IntegrityFailure(it) },
        ) {
            decodeInspect(run(SynchronizerScripts.EXPIRABLE_INSPECT_SCRIPT, *handleArgs(handle)), handle)
        }
    }

    fun acquire(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
        permits: Int,
        waitTime: Duration,
    ): PermitAcquireResult<ExpirablePermitHandle> {
        validateWait(waitTime)
        val deadline = System.nanoTime() + waitTime.toNanos()
        do {
            when (val result = tryAcquire(ownerId, requestId, permits)) {
                PermitAcquireResult.Unavailable -> Thread.sleep(config.semaphore.pollInterval.toMillis().coerceAtLeast(1))
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
    ): CompletableFuture<PermitAcquireResult<ExpirablePermitHandle>> {
        validateWait(waitTime)
        return poller.poll(
            deadlineNanos = System.nanoTime() + waitTime.toNanos(),
            interval = config.semaphore.pollInterval,
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
    ): PermitAcquireResult<ExpirablePermitHandle> {
        validateWait(waitTime)
        val deadline = System.nanoTime() + waitTime.toNanos()
        do {
            when (val result = tryAcquireAsync(ownerId, requestId, permits).await()) {
                PermitAcquireResult.Unavailable -> delay(config.semaphore.pollInterval.toMillis().coerceAtLeast(1))
                else -> return result
            }
        } while (System.nanoTime() < deadline)
        return PermitAcquireResult.TimedOut
    }

    fun inspectAsync(handle: ExpirablePermitHandle): CompletableFuture<PermitInspectResult<ExpirablePermitHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(PermitInspectResult.Closed)
        return runAsync(SynchronizerScripts.EXPIRABLE_INSPECT_SCRIPT, *handleArgs(handle))
            .classifiedFuture(
                { decodeInspect(it, handle) },
                { PermitInspectResult.BackendFailure(it) },
                { PermitInspectResult.IntegrityFailure(it) },
            )
    }

    fun release(handle: ExpirablePermitHandle): PermitMutationResult<ExpirablePermitHandle> {
        validateHandle(handle)
        if (closed.get()) return PermitMutationResult.Closed
        return classified(
            { ambiguousMutation(it, handle.permit.requestId) },
            { PermitMutationResult.IntegrityFailure(it) },
        ) {
            decodeRelease(run(SynchronizerScripts.EXPIRABLE_RELEASE_SCRIPT, *handleArgs(handle)), handle)
        }
    }

    fun releaseAsync(handle: ExpirablePermitHandle): CompletableFuture<PermitMutationResult<ExpirablePermitHandle>> {
        validateHandle(handle)
        if (closed.get()) return CompletableFuture.completedFuture(PermitMutationResult.Closed)
        return runAsync(SynchronizerScripts.EXPIRABLE_RELEASE_SCRIPT, *handleArgs(handle))
            .classifiedFuture(
                { decodeRelease(it, handle) },
                { ambiguousMutation(it, handle.permit.requestId) },
                { PermitMutationResult.IntegrityFailure(it) },
            )
    }

    fun renew(handle: ExpirablePermitHandle, extension: Duration): PermitRenewResult<ExpirablePermitHandle> {
        validateHandle(handle)
        require(!extension.isZero && !extension.isNegative && extension.toMillis() >= 100)
        if (closed.get()) return PermitRenewResult.Closed
        return classified(
            { ambiguousRenew(it, handle.permit.requestId) },
            { PermitRenewResult.IntegrityFailure(it) },
        ) {
            decodeRenew(
                run(
                    SynchronizerScripts.EXPIRABLE_RENEW_SCRIPT,
                    *handleArgs(handle),
                    extension.toMillis().toString(),
                ),
                handle,
            )
        }
    }

    fun renewAsync(
        handle: ExpirablePermitHandle,
        extension: Duration,
    ): CompletableFuture<PermitRenewResult<ExpirablePermitHandle>> {
        validateHandle(handle)
        require(!extension.isZero && !extension.isNegative && extension.toMillis() >= 100)
        if (closed.get()) return CompletableFuture.completedFuture(PermitRenewResult.Closed)
        return runAsync(
            SynchronizerScripts.EXPIRABLE_RENEW_SCRIPT,
            *handleArgs(handle),
            extension.toMillis().toString(),
        ).classifiedFuture(
            { decodeRenew(it, handle) },
            { ambiguousRenew(it, handle.permit.requestId) },
            { PermitRenewResult.IntegrityFailure(it) },
        )
    }

    suspend fun tryAcquireSuspending(owner: SemaphoreOwnerId, request: SemaphoreRequestId, permits: Int) =
        tryAcquireAsync(owner, request, permits).await()
    suspend fun inspectSuspending(handle: ExpirablePermitHandle) = inspectAsync(handle).await()
    suspend fun releaseSuspending(handle: ExpirablePermitHandle) = releaseAsync(handle).await()
    suspend fun renewSuspending(handle: ExpirablePermitHandle, extension: Duration) =
        renewAsync(handle, extension).await()

    fun reconcile(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): PermitReconcileResult<ExpirablePermitHandle> {
        if (closed.get()) return PermitReconcileResult.Closed
        return classified(
            { PermitReconcileResult.BackendFailure(it) },
            { PermitReconcileResult.IntegrityFailure(it) },
        ) {
            decodeReconcile(
                run(SynchronizerScripts.EXPIRABLE_RECONCILE_SCRIPT, cleanupLimit, ownerId.value, requestId.value),
                ownerId,
                requestId,
            )
        }
    }

    fun reconcileAsync(
        ownerId: SemaphoreOwnerId,
        requestId: SemaphoreRequestId,
    ): CompletableFuture<PermitReconcileResult<ExpirablePermitHandle>> {
        if (closed.get()) return CompletableFuture.completedFuture(PermitReconcileResult.Closed)
        return runAsync(
            SynchronizerScripts.EXPIRABLE_RECONCILE_SCRIPT,
            cleanupLimit,
            ownerId.value,
            requestId.value,
        ).classifiedFuture(
            { decodeReconcile(it, ownerId, requestId) },
            { PermitReconcileResult.BackendFailure(it) },
            { PermitReconcileResult.IntegrityFailure(it) },
        )
    }

    fun close() {
        if (closed.compareAndSet(false, true)) {
            poller.close()
            bootstrap.close()
        }
    }

    private fun run(script: RedisScript, vararg args: String): List<String> =
        RedisScriptRunner.run(sync, script, ScriptOutputType.MULTI, keys.expirable, *args)
    private fun runAsync(script: RedisScript, vararg args: String): CompletableFuture<List<String>> =
        RedisScriptRunner.runAsync(async, script, ScriptOutputType.MULTI, keys.expirable, *args)

    private fun decodeAcquire(
        raw: List<String>,
        owner: SemaphoreOwnerId,
        request: SemaphoreRequestId,
    ): PermitAcquireResult<ExpirablePermitHandle> {
        val reply = decodeReply(raw) ?: return integrityAcquire()
        if (reply[0] in setOf("UNAVAILABLE", "NOT_INITIALIZED", "REQUEST_COMPLETED")) {
            return PermitAcquireResult.Unavailable
        }
        if (reply[0] == "CAPACITY_EXCEEDED") return PermitAcquireResult.CapacityExceeded
        val replay = reply[0] == "REPLAY"
        if (reply[0] != "ACQUIRED" && !replay) return integrityAcquire()
        val allocation = reply.getOrNull(1) ?: return integrityAcquire()
        val (generation, permits, leaseIds, deadline) = if (replay) {
            val fields = reply.getOrNull(2)?.split('|') ?: return integrityAcquire()
            Quad(
                fields.getOrNull(2)?.toLongOrNull(),
                fields.getOrNull(3)?.toIntOrNull(),
                reply.getOrNull(3),
                reply.getOrNull(4)?.toDoubleOrNull()?.toLong(),
            )
        } else {
            Quad(
                reply.getOrNull(2)?.toLongOrNull(),
                reply.getOrNull(3)?.toIntOrNull(),
                reply.getOrNull(5),
                reply.getOrNull(4)?.toLongOrNull(),
            )
        }
        if (generation == null || permits == null || leaseIds == null || deadline == null) return integrityAcquire()
        val ids = leaseIds.split(',').filter(String::isNotBlank)
        if (ids.size != permits || ids.distinct().size != ids.size) return integrityAcquire()
        val permit = PermitHandle(keys.fingerprint, owner, generation, request, permits, allocation)
        return PermitAcquireResult.Acquired(
            ExpirablePermitHandle(permit, ids.map { ExpirablePermitLease(it, deadline) }),
        )
    }

    private fun decodeInspect(
        raw: List<String>,
        handle: ExpirablePermitHandle,
    ): PermitInspectResult<ExpirablePermitHandle> {
        val reply = decodeReply(raw) ?: return integrityInspect()
        return when (reply[0]) {
            "OWNED" -> reply.getOrNull(1)?.toIntOrNull()?.let { PermitInspectResult.Owned(handle, it) }
                ?: integrityInspect()
            "RELEASED" -> PermitInspectResult.Released
            "EXPIRED" -> PermitInspectResult.Expired
            "OWNERSHIP_LOST" -> PermitInspectResult.IntegrityFailure(
                SynchronizerIntegrityFailure(SynchronizerIntegrityFailureKind.STATE_MISMATCH),
            )
            "STALE_GENERATION" -> PermitInspectResult.StaleGeneration
            else -> integrityInspect()
        }
    }

    private fun decodeRelease(
        raw: List<String>,
        handle: ExpirablePermitHandle,
    ): PermitMutationResult<ExpirablePermitHandle> {
        val reply = decodeReply(raw) ?: return integrityMutation()
        return when (reply[0]) {
            "RELEASED" -> reply.getOrNull(1)?.toIntOrNull()?.let { PermitMutationResult.Released(handle, it) }
                ?: integrityMutation()
            "ALREADY_RELEASED" -> PermitMutationResult.AlreadyReleased
            "EXPIRED" -> PermitMutationResult.Expired
            "OWNERSHIP_LOST" -> PermitMutationResult.IntegrityFailure(
                SynchronizerIntegrityFailure(SynchronizerIntegrityFailureKind.STATE_MISMATCH),
            )
            "STALE_GENERATION" -> PermitMutationResult.StaleGeneration
            else -> integrityMutation()
        }
    }

    private fun decodeRenew(
        raw: List<String>,
        handle: ExpirablePermitHandle,
    ): PermitRenewResult<ExpirablePermitHandle> {
        val reply = decodeReply(raw) ?: return integrityRenew()
        return when (reply[0]) {
            "RENEWED" -> {
                val deadline = reply.getOrNull(1)?.toLongOrNull() ?: return integrityRenew()
                val ids = reply.getOrNull(2)?.split(',') ?: return integrityRenew()
                if (ids != handle.leases.map { it.permitId }) return integrityRenew()
                PermitRenewResult.Renewed(handle.copy(leases = ids.map { ExpirablePermitLease(it, deadline) }))
            }
            "EXPIRED" -> PermitRenewResult.Expired
            "RELEASED" -> PermitRenewResult.Released
            "OWNERSHIP_LOST" -> PermitRenewResult.OwnershipLost
            "STALE_GENERATION" -> PermitRenewResult.StaleGeneration
            else -> integrityRenew()
        }
    }

    private fun decodeReconcile(
        raw: List<String>,
        owner: SemaphoreOwnerId,
        request: SemaphoreRequestId,
    ): PermitReconcileResult<ExpirablePermitHandle> {
        val reply = decodeReply(raw) ?: return integrityReconcile()
        return when (reply[0]) {
            "OWNED" -> {
                val generation = reply.getOrNull(2)?.toLongOrNull() ?: return integrityReconcile()
                val permits = reply.getOrNull(3)?.toIntOrNull() ?: return integrityReconcile()
                val ids = reply.getOrNull(4)?.split(',')?.filter(String::isNotBlank) ?: return integrityReconcile()
                val deadline = reply.getOrNull(5)?.toDoubleOrNull()?.toLong() ?: return integrityReconcile()
                val remaining = reply.getOrNull(6)?.toIntOrNull() ?: return integrityReconcile()
                if (ids.size != permits) return integrityReconcile()
                val permit = PermitHandle(keys.fingerprint, owner, generation, request, permits, reply[1])
                PermitReconcileResult.Owned(
                    ExpirablePermitHandle(permit, ids.map { ExpirablePermitLease(it, deadline) }),
                    remaining,
                )
            }
            "RELEASED" -> PermitReconcileResult.Released
            "NOT_FOUND" -> PermitReconcileResult.NotFound
            else -> integrityReconcile()
        }
    }

    private fun handleArgs(handle: ExpirablePermitHandle): Array<String> = arrayOf(
        cleanupLimit,
        handle.permit.token,
        handle.permit.generation.toString(),
        handle.permit.ownerId.value,
        handle.permit.requestId.value,
    )

    private fun validateHandle(handle: ExpirablePermitHandle) {
        require(handle.permit.objectFingerprint == keys.fingerprint) { "Permit handle belongs to another object." }
    }
    private fun validatePermits(permits: Int) {
        require(permits in 1..minOf(config.semaphore.maxPermits, config.maxPermitsPerAcquire))
    }

    private fun validateWait(waitTime: Duration) {
        require(!waitTime.isZero && !waitTime.isNegative)
        require(waitTime <= Duration.ofHours(24))
    }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ): ExpirableSemaphoreClient {
            val keys = deriveSemaphoreKeys(name, config.semaphore, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return ExpirableSemaphoreClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                SemaphoreClient.create(connection, name, config.semaphore),
                SynchronizerAsyncPoller(registration),
            )
        }
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: ExpirableSemaphoreConfig,
        ): ExpirableSemaphoreClient {
            val keys = deriveSemaphoreKeys(name, config.semaphore, connection.codec)
            val registration = CoordinationRuntime.forConnection(connection).registerObject(keys.fingerprint)
            return ExpirableSemaphoreClient(
                keys,
                config,
                connection.sync(),
                connection.async(),
                SemaphoreClient.create(connection, name, config.semaphore),
                SynchronizerAsyncPoller(registration),
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
private fun token(): String = UUID.randomUUID().toString().replace("-", "")
private fun backend(error: Throwable): SynchronizerBackendFailure {
    val cause = generateSequence(error) { it.cause }.last()
    val kind = when (cause) {
        is RedisCommandTimeoutException -> SynchronizerBackendFailureKind.TIMEOUT
        is RedisConnectionException -> SynchronizerBackendFailureKind.CONNECTION
        else -> SynchronizerBackendFailureKind.COMMAND
    }
    return SynchronizerBackendFailure(kind, SynchronizerRecoveryAction.RECONCILE_REQUEST)
}
private fun integrityFailure() = SynchronizerIntegrityFailure(SynchronizerIntegrityFailureKind.MALFORMED_REPLY)
private fun integrityAcquire() = PermitAcquireResult.IntegrityFailure(integrityFailure())
private fun integrityInspect() = PermitInspectResult.IntegrityFailure(integrityFailure())
private fun integrityMutation() = PermitMutationResult.IntegrityFailure(integrityFailure())
private fun integrityRenew() = PermitRenewResult.IntegrityFailure(integrityFailure())
private fun integrityReconcile() = PermitReconcileResult.IntegrityFailure(integrityFailure())

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

private fun ambiguousRenew(
    failure: SynchronizerBackendFailure,
    requestId: SemaphoreRequestId,
): PermitRenewResult<Nothing> =
    if (failure.kind in setOf(SynchronizerBackendFailureKind.TIMEOUT, SynchronizerBackendFailureKind.CONNECTION)) {
        PermitRenewResult.Ambiguous(requestId)
    } else {
        PermitRenewResult.BackendFailure(failure)
    }

private inline fun <T> classified(
    backendResult: (SynchronizerBackendFailure) -> T,
    integrityResult: (SynchronizerIntegrityFailure) -> T,
    block: () -> T,
): T = try {
    block()
} catch (_: IndexOutOfBoundsException) {
    integrityResult(integrityFailure())
} catch (_: NumberFormatException) {
    integrityResult(integrityFailure())
} catch (_: IllegalArgumentException) {
    integrityResult(integrityFailure())
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
            integrityResult(integrityFailure())
        }
    }
}
