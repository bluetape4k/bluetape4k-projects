package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveFairLockKeys
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.await
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.Executors

internal class LettuceFairLockTest : LockContract() {

    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        fairBlockingAdapter(LettuceFairLock.create(connection, name, FairLockConfig(config)))

    @Test
    fun `blocking and future APIs recover by polling Redis authority`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fair-poll-${System.nanoTime()}"
            val lock = LettuceFairLock.create(connection, name)
            try {
                val holder = lock.tryAcquire(
                    LockOwnerId.from("holder"),
                    LockRequestId.from("holder-request"),
                    FIXED_LEASE,
                ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                val pending = lock.acquireAsync(
                    LockOwnerId.from("waiter"),
                    LockRequestId.from("waiter-request"),
                    Duration.ofSeconds(2),
                    FIXED_LEASE,
                )
                eventuallyQueued(lock, LockOwnerId.from("waiter"), LockRequestId.from("waiter-request"))

                lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
                pending.await().shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            } finally {
                lock.close()
                connection.sync().del(*deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8).all)
            }
        }
    }

    @Test
    fun `suspend cancellation removes the exact queued waiter`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fair-cancel-${System.nanoTime()}"
            val blocking = LettuceFairLock.create(connection, name)
            val suspending = LettuceSuspendFairLock.create(connection, name)
            val waiterOwner = LockOwnerId.from("cancelled")
            val waiterRequest = LockRequestId.from("cancelled-request")
            try {
                val holder = blocking.tryAcquire(
                    LockOwnerId.from("holder"),
                    LockRequestId.from("holder-request"),
                    FIXED_LEASE,
                ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                val pending = async {
                    suspending.acquire(
                        waiterOwner,
                        waiterRequest,
                        Duration.ofSeconds(2),
                        FIXED_LEASE,
                    )
                }
                eventuallyQueued(blocking, waiterOwner, waiterRequest)

                pending.cancelAndJoin()
                eventuallyNotQueued(blocking, waiterOwner, waiterRequest)
                blocking.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8).all)
            }
        }
    }

    @Test
    fun `future cancellation removes the exact queued waiter`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fair-future-cancel-${System.nanoTime()}"
            val lock = LettuceFairLock.create(connection, name)
            val waiterOwner = LockOwnerId.from("future-cancelled")
            val waiterRequest = LockRequestId.from("future-cancelled-request")
            try {
                val holder = lock.tryAcquire(
                    LockOwnerId.from("holder"),
                    LockRequestId.from("holder-request"),
                    FIXED_LEASE,
                ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                val pending = lock.acquireAsync(
                    waiterOwner,
                    waiterRequest,
                    Duration.ofSeconds(2),
                    FIXED_LEASE,
                )
                eventuallyQueued(lock, waiterOwner, waiterRequest)

                pending.cancel(false) shouldBeEqualTo true
                eventuallyNotQueued(lock, waiterOwner, waiterRequest)
                lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
                connection.sync().del(*deriveFairLockKeys(name, FairLockConfig(), StringCodec.UTF8).all)
            }
        }
    }

    private suspend fun eventuallyQueued(
        lock: LettuceFairLock,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ) {
        repeat(100) {
            if (lock.reconcile(ownerId, requestId) is LockReconcileResult.Queued) return
            yield()
        }
        error("waiter was not enqueued")
    }

    private suspend fun eventuallyNotQueued(
        lock: LettuceFairLock,
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ) {
        repeat(100) {
            if (lock.reconcile(ownerId, requestId) !is LockReconcileResult.Queued) return
            yield()
        }
        error("waiter was not removed")
    }

    private companion object {
        val FIXED_LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

internal class FutureLettuceFairLockTest : LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        fairFutureAdapter(LettuceFairLock.create(connection, name, FairLockConfig(config)))
}

internal class SuspendLettuceFairLockTest : LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter =
        fairSuspendAdapter(LettuceSuspendFairLock.create(connection, name, FairLockConfig(config)))
}

internal class ClusterLettuceFairLockTest {

    @Test
    @Timeout(30)
    fun `cluster factories preserve fair handle kind across API styles`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val names = listOf("fair-cluster-blocking", "fair-cluster-suspend")
                try {
                    LettuceFairLock.create(connection, names[0]).use { blocking ->
                        val handle = blocking.tryAcquire(
                            LockOwnerId.from("cluster-owner"),
                            LockRequestId.from("cluster-request"),
                            FIXED_CLUSTER_LEASE,
                        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                        handle.kind shouldBeEqualTo LockKind.FAIR
                        blocking.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }
                    LettuceSuspendFairLock.create(connection, names[1]).use { suspending ->
                        val handle = suspending.tryAcquire(
                            LockOwnerId.from("cluster-suspend-owner"),
                            LockRequestId.from("cluster-suspend-request"),
                            FIXED_CLUSTER_LEASE,
                        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                        handle.kind shouldBeEqualTo LockKind.FAIR
                        suspending.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }
                } finally {
                    names.forEach { name ->
                        connection.sync().del(*deriveFairLockKeys(name, FairLockConfig(), connection.codec).all)
                    }
                }
            }
        }
    }

    private companion object {
        val FIXED_CLUSTER_LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

private fun fairBlockingAdapter(lock: LettuceFairLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquire(ownerId, requestId, leasePolicy)

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquire(ownerId, requestId, waitTime, leasePolicy)

        override suspend fun inspect(handle: LockHandle) = lock.inspect(handle)

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcile(ownerId, requestId)

        override suspend fun renew(handle: LockHandle, extension: Duration) = lock.renew(handle, extension)

        override suspend fun release(handle: LockHandle) = lock.release(handle)

        override fun close() = lock.close()
    }

private fun fairFutureAdapter(lock: LettuceFairLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquireAsync(ownerId, requestId, leasePolicy).await()

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquireAsync(ownerId, requestId, waitTime, leasePolicy).await()

        override suspend fun inspect(handle: LockHandle) = lock.inspectAsync(handle).await()

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcileAsync(ownerId, requestId).await()

        override suspend fun renew(handle: LockHandle, extension: Duration) =
            lock.renewAsync(handle, extension).await()

        override suspend fun release(handle: LockHandle) = lock.releaseAsync(handle).await()

        override fun close() = lock.close()
    }

private fun fairSuspendAdapter(lock: LettuceSuspendFairLock): DistributedLockAdapter =
    object: DistributedLockAdapter {
        override suspend fun tryAcquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            leasePolicy: LeasePolicy,
        ) = lock.tryAcquire(ownerId, requestId, leasePolicy)

        override suspend fun acquire(
            ownerId: LockOwnerId,
            requestId: LockRequestId,
            waitTime: Duration,
            leasePolicy: LeasePolicy,
        ) = lock.acquire(ownerId, requestId, waitTime, leasePolicy)

        override suspend fun inspect(handle: LockHandle) = lock.inspect(handle)

        override suspend fun reconcile(ownerId: LockOwnerId, requestId: LockRequestId) =
            lock.reconcile(ownerId, requestId)

        override suspend fun renew(handle: LockHandle, extension: Duration) = lock.renew(handle, extension)

        override suspend fun release(handle: LockHandle) = lock.release(handle)

        override fun close() = lock.close()
    }

@Suppress("unused")
private fun compileFairLockFactories(
    standalone: StatefulRedisConnection<String, String>,
    cluster: StatefulRedisClusterConnection<String, String>,
    name: String,
    config: FairLockConfig,
    sink: LockObservationSink,
) {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    try {
        listOf(
            LettuceFairLock.create(standalone, name),
            LettuceFairLock.create(standalone, name, config),
            LettuceFairLock.create(standalone, name, config, scheduler, sink),
            LettuceFairLock.create(cluster, name),
            LettuceFairLock.create(cluster, name, config),
            LettuceFairLock.create(cluster, name, config, scheduler, sink),
        )
        listOf(
            LettuceSuspendFairLock.create(standalone, name),
            LettuceSuspendFairLock.create(standalone, name, config),
            LettuceSuspendFairLock.create(standalone, name, config, scheduler, sink),
            LettuceSuspendFairLock.create(cluster, name),
            LettuceSuspendFairLock.create(cluster, name, config),
            LettuceSuspendFairLock.create(cluster, name, config, scheduler, sink),
        )
    } finally {
        scheduler.shutdownNow()
    }
}
