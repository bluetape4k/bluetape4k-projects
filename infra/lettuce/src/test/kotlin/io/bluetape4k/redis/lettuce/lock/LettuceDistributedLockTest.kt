package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors

internal class LettuceDistributedLockTest : LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter {
        val lock = LettuceDistributedLock.create(connection, name, config)
        return blockingAdapter(lock)
    }

    @Test
    fun `barriered contenders produce exactly one winner`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "winner-${randomName().substringAfter(':')}"
        val keys = io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys(
            name,
            LockConfig(),
            StringCodec.UTF8,
        )
        val lock = LettuceDistributedLock.create(connection, name)
        val executor = Executors.newFixedThreadPool(CONTENDER_COUNT)
        val barrier = CyclicBarrier(CONTENDER_COUNT)
        try {
            connection.sync().del(keys.state, keys.generation, keys.holds, keys.terminal)
            val results = executor.invokeAll(
                (1..CONTENDER_COUNT).map { index ->
                    Callable {
                        barrier.await()
                        lock.tryAcquire(
                            LockOwnerId.from("winner-owner-$index"),
                            LockRequestId.from("winner-request-$index"),
                            LeasePolicy.Fixed(Duration.ofSeconds(3)),
                        )
                    }
                },
            ).map { it.get() }

            results.count { it is LockAcquireResult.Acquired<*> } shouldBeEqualTo 1
            results.count { it is LockAcquireResult.Contended } shouldBeEqualTo CONTENDER_COUNT - 1
        } finally {
            lock.close()
            executor.shutdownNow()
            connection.sync().del(keys.state, keys.generation, keys.holds, keys.terminal)
            connection.close()
        }
    }

    private companion object {
        const val CONTENDER_COUNT = 8
    }
}

internal class FutureLettuceDistributedLockTest : LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter {
        val lock = LettuceDistributedLock.create(connection, name, config)
        return futureAdapter(lock)
    }
}

internal class SuspendLettuceDistributedLockTest : LockContract() {
    override fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter {
        val lock = LettuceSuspendDistributedLock.create(connection, name, config)
        return suspendAdapter(lock)
    }
}

internal class ClusterLettuceDistributedLockTest {

    @Test
    @Timeout(30)
    fun `standalone-equivalent cluster factories execute every API style`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val owner = LockOwnerId.from("cluster-owner")
                val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))
                val names = listOf("cluster-blocking", "cluster-future", "cluster-suspend")
                try {
                    LettuceDistributedLock.create(connection, names[0]).use { blocking ->
                        val handle = blocking.tryAcquire(
                            owner,
                            LockRequestId.from("cluster-blocking-request"),
                            lease,
                        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                        blocking.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }

                    LettuceDistributedLock.create(connection, names[1], LockConfig()).use { future ->
                        val handle = future.tryAcquireAsync(
                            owner,
                            LockRequestId.from("cluster-future-request"),
                            lease,
                        ).await().shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                        future.releaseAsync(handle).await() shouldBeEqualTo LockMutationResult.Released(0)
                    }

                    LettuceSuspendDistributedLock.create(connection, names[2]).use { suspending ->
                        val handle = suspending.tryAcquire(
                            owner,
                            LockRequestId.from("cluster-suspend-request"),
                            lease,
                        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
                        suspending.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }
                } finally {
                    names.forEach { name ->
                        val keys = io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys(
                            name,
                            LockConfig(),
                            connection.codec,
                        )
                        connection.sync().del(*keys.all)
                    }
                }
            }
        }
    }
}

private fun blockingAdapter(lock: LettuceDistributedLock): DistributedLockAdapter =
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

private fun futureAdapter(lock: LettuceDistributedLock): DistributedLockAdapter =
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

        override suspend fun renew(handle: LockHandle, extension: Duration) = lock.renewAsync(handle, extension).await()

        override suspend fun release(handle: LockHandle) = lock.releaseAsync(handle).await()

        override fun close() = lock.close()
    }

private fun suspendAdapter(lock: LettuceSuspendDistributedLock): DistributedLockAdapter =
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
private fun compileDistributedLockFactories(
    standalone: StatefulRedisConnection<String, String>,
    cluster: StatefulRedisClusterConnection<String, String>,
    name: String,
    config: LockConfig,
    sink: LockObservationSink,
) {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    try {
        listOf(
            LettuceDistributedLock.create(standalone, name),
            LettuceDistributedLock.create(standalone, name, config),
            LettuceDistributedLock.create(standalone, name, config, scheduler, sink),
            LettuceDistributedLock.create(cluster, name),
            LettuceDistributedLock.create(cluster, name, config),
            LettuceDistributedLock.create(cluster, name, config, scheduler, sink),
        )
        listOf(
            LettuceSuspendDistributedLock.create(standalone, name),
            LettuceSuspendDistributedLock.create(standalone, name, config),
            LettuceSuspendDistributedLock.create(standalone, name, config, scheduler, sink),
            LettuceSuspendDistributedLock.create(cluster, name),
            LettuceSuspendDistributedLock.create(cluster, name, config),
            LettuceSuspendDistributedLock.create(cluster, name, config, scheduler, sink),
        )
    } finally {
        scheduler.shutdownNow()
    }
}

@Suppress("unused")
private fun compileDistributedLockKeys(keys: DistributedLockKeys) {
    listOf(keys.state, keys.generation, keys.holds, keys.terminal, keys.fingerprint)
}
