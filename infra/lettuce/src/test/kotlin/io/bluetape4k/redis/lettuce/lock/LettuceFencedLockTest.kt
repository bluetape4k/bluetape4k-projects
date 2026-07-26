package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
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

internal class LettuceFencedLockTest {

    @Test
    fun `fresh generation increments token while replay and reentry keep it stable`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val fixture = FencedFixture(connection, "fenced-token-${System.nanoTime()}", epoch = 11)
            fixture.use { lock ->
                lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.AlreadyInitialized

                val first = lock.tryAcquire(OWNER, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                val replay = lock.tryAcquire(OWNER, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                val reentered = lock.tryAcquire(OWNER, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Reentered<FencedLockHandle>>().handle

                replay.fencingToken shouldBeEqualTo first.fencingToken
                reentered.fencingToken shouldBeEqualTo first.fencingToken
                reentered.epoch shouldBeEqualTo 11L
                lock.release(reentered) shouldBeEqualTo LockMutationResult.Released(1)
                lock.release(first) shouldBeEqualTo LockMutationResult.Released(0)

                val next = lock.tryAcquire(OTHER_OWNER, REQUEST_3, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                next.fencingToken shouldBeGreaterThan first.fencingToken
            }
        }
    }

    @Test
    fun `malformed counter and exhausted exact range fail closed`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val malformed = FencedFixture(connection, "fenced-malformed-${System.nanoTime()}", epoch = 3)
            malformed.use { lock ->
                lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                connection.sync().set(malformed.keys.counter, "not-a-number")
                lock.tryAcquire(OWNER, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
                    .failure.kind shouldBeEqualTo LockIntegrityFailureKind.COUNTER_REGRESSION
            }

            val exhausted = FencedFixture(connection, "fenced-exhausted-${System.nanoTime()}", epoch = 3)
            exhausted.use { lock ->
                lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                connection.sync().set(exhausted.keys.counter, MAX_LUA_EXACT_INTEGER.toString())
                lock.tryAcquire(OWNER, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.CapacityExceeded
            }
        }
    }

    @Test
    fun `epoch authority is isolated and downstream acceptance is strict greater`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fenced-epoch-${System.nanoTime()}"
            val firstEpoch = FencedFixture(connection, name, epoch = 20)
            val nextEpoch = FencedFixture(connection, name, epoch = 21)
            firstEpoch.use { first ->
                nextEpoch.use { next ->
                    first.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                    next.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                    val firstHandle = first.tryAcquire(OWNER, REQUEST_1, LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                    first.release(firstHandle) shouldBeEqualTo LockMutationResult.Released(0)
                    val nextHandle = next.tryAcquire(OTHER_OWNER, REQUEST_2, LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle

                    acceptsAfter(firstHandle, nextHandle) shouldBeEqualTo true
                    acceptsAfter(nextHandle, nextHandle) shouldBeEqualTo false
                    acceptsAfter(nextHandle, firstHandle) shouldBeEqualTo false
                }
            }
        }
    }

    @Test
    fun `future and suspend surfaces preserve fenced handles`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fenced-parity-${System.nanoTime()}"
            val config = FencedLockConfig(epoch = 31)
            val blocking = LettuceFencedLock.create(connection, name, config)
            val suspending = LettuceSuspendFencedLock.create(connection, name, config)
            val keys = deriveFencedLockKeys(name, config, connection.codec)
            try {
                blocking.bootstrapFencingAsync().await() shouldBeEqualTo FencedBootstrapResult.Initialized
                val futureHandle = blocking.tryAcquireAsync(OWNER, REQUEST_1, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                futureHandle.lock.kind shouldBeEqualTo LockKind.FENCED
                blocking.releaseAsync(futureHandle).await() shouldBeEqualTo LockMutationResult.Released(0)

                suspending.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.AlreadyInitialized
                val suspendHandle = suspending.tryAcquire(OTHER_OWNER, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                suspendHandle.fencingToken shouldBeGreaterThan futureHandle.fencingToken
                suspending.release(suspendHandle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*keys.all)
            }
        }
    }

    @Test
    fun `future cancellation stops the bounded wait without acquiring later`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val fixture = FencedFixture(connection, "fenced-cancel-${System.nanoTime()}", epoch = 41)
            fixture.use { lock ->
                lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                val holder = lock.tryAcquire(OWNER, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                val pending = lock.acquireAsync(
                    OTHER_OWNER,
                    REQUEST_2,
                    Duration.ofSeconds(2),
                    LEASE,
                )

                pending.cancel(false) shouldBeEqualTo true
                lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
                val explicit = lock.tryAcquire(OTHER_OWNER, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                explicit.fencingToken shouldBeGreaterThan holder.fencingToken
            }
        }
    }

    @Test
    fun `suspend cancellation stops retry registration and preserves reconciliation identity`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "fenced-suspend-cancel-${System.nanoTime()}"
            val config = FencedLockConfig(epoch = 42)
            val blocking = LettuceFencedLock.create(connection, name, config)
            val suspending = LettuceSuspendFencedLock.create(connection, name, config)
            val keys = deriveFencedLockKeys(name, config, connection.codec)
            try {
                blocking.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                val holder = blocking.tryAcquire(OWNER, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                val pending = async {
                    suspending.acquire(
                        OTHER_OWNER,
                        REQUEST_2,
                        Duration.ofSeconds(2),
                        LEASE,
                    )
                }
                yield()

                pending.cancelAndJoin()
                suspending.reconcile(OTHER_OWNER, REQUEST_2) shouldBeEqualTo LockReconcileResult.NotFound
                blocking.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
                val explicit = suspending.tryAcquire(OTHER_OWNER, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                explicit.fencingToken shouldBeGreaterThan holder.fencingToken
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*keys.all)
            }
        }
    }

    private fun acceptsAfter(previous: FencedLockHandle, incoming: FencedLockHandle): Boolean =
        incoming.epoch > previous.epoch ||
            incoming.epoch == previous.epoch && incoming.fencingToken > previous.fencingToken

    private class FencedFixture(
        private val connection: StatefulRedisConnection<String, String>,
        name: String,
        epoch: Long,
    ) : AutoCloseable {
        private val config = FencedLockConfig(epoch = epoch)
        val keys = deriveFencedLockKeys(name, config, connection.codec)
        private val delegate = LettuceFencedLock.create(connection, name, config)

        fun use(block: (LettuceFencedLock) -> Unit) {
            try {
                block(delegate)
            } finally {
                close()
            }
        }

        override fun close() {
            delegate.close()
            connection.sync().del(*keys.all)
        }
    }

    private companion object {
        val OWNER = LockOwnerId.from("fenced-owner")
        val OTHER_OWNER = LockOwnerId.from("fenced-other-owner")
        val REQUEST_1 = LockRequestId.from("fenced-request-1")
        val REQUEST_2 = LockRequestId.from("fenced-request-2")
        val REQUEST_3 = LockRequestId.from("fenced-request-3")
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

internal class ClusterLettuceFencedLockTest {

    @Test
    @Timeout(30)
    fun `cluster factories preserve bootstrap and fenced handle semantics`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val names = listOf("fenced-cluster-blocking", "fenced-cluster-suspend")
                val configs = listOf(FencedLockConfig(epoch = 51), FencedLockConfig(epoch = 52))
                try {
                    LettuceFencedLock.create(connection, names[0], configs[0]).use { blocking ->
                        blocking.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                        val handle = blocking.tryAcquire(CLUSTER_OWNER, CLUSTER_REQUEST, CLUSTER_LEASE)
                            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                        handle.lock.kind shouldBeEqualTo LockKind.FENCED
                        blocking.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }
                    LettuceSuspendFencedLock.create(connection, names[1], configs[1]).use { suspending ->
                        suspending.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                        val handle = suspending.tryAcquire(CLUSTER_OWNER, CLUSTER_REQUEST, CLUSTER_LEASE)
                            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
                        handle.lock.kind shouldBeEqualTo LockKind.FENCED
                        suspending.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                    }
                } finally {
                    names.zip(configs).forEach { (name, config) ->
                        connection.sync().del(*deriveFencedLockKeys(name, config, connection.codec).all)
                    }
                }
            }
        }
    }

    private companion object {
        val CLUSTER_OWNER = LockOwnerId.from("fenced-cluster-owner")
        val CLUSTER_REQUEST = LockRequestId.from("fenced-cluster-request")
        val CLUSTER_LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

@Suppress("unused")
private fun compileFencedLockFactories(
    standalone: StatefulRedisConnection<String, String>,
    cluster: StatefulRedisClusterConnection<String, String>,
    name: String,
    config: FencedLockConfig,
    sink: LockObservationSink,
) {
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    try {
        listOf(
            LettuceFencedLock.create(standalone, name, config),
            LettuceFencedLock.create(standalone, name, config, scheduler, sink),
            LettuceFencedLock.create(cluster, name, config),
            LettuceFencedLock.create(cluster, name, config, scheduler, sink),
        )
        listOf(
            LettuceSuspendFencedLock.create(standalone, name, config),
            LettuceSuspendFencedLock.create(standalone, name, config, scheduler, sink),
            LettuceSuspendFencedLock.create(cluster, name, config),
            LettuceSuspendFencedLock.create(cluster, name, config, scheduler, sink),
        )
    } finally {
        scheduler.shutdownNow()
    }
}
