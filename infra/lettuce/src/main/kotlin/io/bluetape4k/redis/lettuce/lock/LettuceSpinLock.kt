package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockClient
import io.bluetape4k.redis.lettuce.lock.internal.LockObservationRecorder
import io.bluetape4k.redis.lettuce.lock.internal.LockRetryPolicy
import io.bluetape4k.redis.lettuce.lock.internal.LockWaitObservation
import io.bluetape4k.redis.lettuce.lock.internal.LockWaitSupport
import io.bluetape4k.redis.lettuce.lock.internal.SpinLockRetryPolicy
import io.bluetape4k.redis.lettuce.lock.internal.withObjectKind
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A distributed lock that resolves contention with bounded, scheduled Redis retries.
 *
 * Ownership, reentry, reconciliation, and fencing against stale handles are exactly the same as
 * [LettuceDistributedLock]. Only the contention wait policy differs.
 */
class LettuceSpinLock internal constructor(
    private val client: SpinLockClient,
): AutoCloseable {

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.tryAcquire(ownerId, requestId, leasePolicy)

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.tryAcquireAsync(ownerId, requestId, leasePolicy)

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        client.acquire(ownerId, requestId, waitTime, leasePolicy)

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        client.acquireAsync(ownerId, requestId, waitTime, leasePolicy)

    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> =
        client.inspect(handle)

    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> =
        client.inspectAsync(handle)

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> =
        client.reconcile(ownerId, requestId)

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        client.reconcileAsync(ownerId, requestId)

    fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> =
        client.renew(handle, extension)

    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> =
        client.renewAsync(handle, extension)

    fun release(handle: LockHandle): LockMutationResult<LockHandle> =
        client.release(handle)

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> =
        client.releaseAsync(handle)

    override fun close() {
        client.close()
    }

    companion object {
        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
        ): LettuceSpinLock =
            create(connection, name, SpinLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SpinLockConfig,
        ): LettuceSpinLock =
            LettuceSpinLock(SpinLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
        ): LettuceSpinLock =
            create(connection, name, SpinLockConfig())

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SpinLockConfig,
        ): LettuceSpinLock =
            LettuceSpinLock(SpinLockClient.create(connection, name, config))

        @JvmStatic
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSpinLock =
            LettuceSpinLock(SpinLockClient.create(connection, name, config, scheduler, observationSink))

        @JvmStatic
        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService,
            observationSink: LockObservationSink,
        ): LettuceSpinLock =
            LettuceSpinLock(SpinLockClient.create(connection, name, config, scheduler, observationSink))
    }
}

internal class SpinLockClient private constructor(
    private val distributed: DistributedLockClient,
    private val registration: CoordinationRuntime.CoordinationObjectRegistration,
    retryPolicy: LockRetryPolicy,
    waitObservation: LockWaitObservation,
) {
    private val closed = AtomicBoolean()
    private val waitSupport = LockWaitSupport(
        registration = registration,
        isClosed = closed::get,
        retryPolicy = retryPolicy,
        waitObservation = waitObservation,
    )

    fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        distributed.tryAcquire(ownerId, requestId, leasePolicy)

    fun tryAcquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        distributed.tryAcquireAsync(ownerId, requestId, leasePolicy)

    suspend fun tryAcquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        distributed.tryAcquireSuspending(ownerId, requestId, leasePolicy)

    fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        waitSupport.acquire(waitTime) {
            distributed.tryAcquire(ownerId, requestId, leasePolicy)
        }

    fun acquireAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): CompletableFuture<LockAcquireResult<LockHandle>> =
        waitSupport.acquireAsync(waitTime) {
            distributed.tryAcquireAsync(ownerId, requestId, leasePolicy)
        }

    suspend fun acquireSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle> =
        waitSupport.acquireSuspending(waitTime) {
            distributed.tryAcquireSuspending(ownerId, requestId, leasePolicy)
        }

    fun inspect(handle: LockHandle): LockInspectResult<LockHandle> =
        distributed.inspect(handle)

    fun inspectAsync(handle: LockHandle): CompletableFuture<LockInspectResult<LockHandle>> =
        distributed.inspectAsync(handle)

    suspend fun inspectSuspending(handle: LockHandle): LockInspectResult<LockHandle> =
        distributed.inspectSuspending(handle)

    fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> =
        distributed.reconcile(ownerId, requestId)

    fun reconcileAsync(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): CompletableFuture<LockReconcileResult<LockHandle>> =
        distributed.reconcileAsync(ownerId, requestId)

    suspend fun reconcileSuspending(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle> =
        distributed.reconcileSuspending(ownerId, requestId)

    fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> =
        distributed.renew(handle, extension)

    fun renewAsync(
        handle: LockHandle,
        extension: Duration,
    ): CompletableFuture<LockMutationResult<LockHandle>> =
        distributed.renewAsync(handle, extension)

    suspend fun renewSuspending(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle> =
        distributed.renewSuspending(handle, extension)

    fun release(handle: LockHandle): LockMutationResult<LockHandle> =
        distributed.release(handle)

    fun releaseAsync(handle: LockHandle): CompletableFuture<LockMutationResult<LockHandle>> =
        distributed.releaseAsync(handle)

    suspend fun releaseSuspending(handle: LockHandle): LockMutationResult<LockHandle> =
        distributed.releaseSuspending(handle)

    fun close() {
        if (closed.compareAndSet(false, true)) {
            distributed.close()
        }
    }

    companion object {
        fun create(
            connection: StatefulRedisConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): SpinLockClient {
            val retryPolicy = SpinLockRetryPolicy(config)
            val spinSink = observationSink.withObjectKind(LockKind.SPIN)
            val waitObservation = LockWaitObservation(LockObservationRecorder(LockKind.SPIN, spinSink))
            val distributed = DistributedLockClient.create(connection, name, config.lock, scheduler, spinSink)
            return SpinLockClient(
                distributed,
                distributed.registration,
                retryPolicy,
                waitObservation,
            )
        }

        fun create(
            connection: StatefulRedisClusterConnection<String, String>,
            name: String,
            config: SpinLockConfig,
            scheduler: ScheduledExecutorService? = null,
            observationSink: LockObservationSink = LockObservationSink.NOOP,
        ): SpinLockClient {
            val retryPolicy = SpinLockRetryPolicy(config)
            val spinSink = observationSink.withObjectKind(LockKind.SPIN)
            val waitObservation = LockWaitObservation(LockObservationRecorder(LockKind.SPIN, spinSink))
            val distributed = DistributedLockClient.create(connection, name, config.lock, scheduler, spinSink)
            return SpinLockClient(
                distributed,
                distributed.registration,
                retryPolicy,
                waitObservation,
            )
        }
    }
}
