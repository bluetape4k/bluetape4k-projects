package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.await
import kotlinx.coroutines.yield
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.TimeUnit

internal class LettuceReadWriteLockTest {

    @Test
    fun `future and suspend views preserve exact handle kinds and atomic downgrade`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "read-write-parity-${System.nanoTime()}"
            val blocking = LettuceReadWriteLock.create(connection, name)
            val suspending = LettuceSuspendReadWriteLock.create(connection, name)
            try {
                val read = blocking.readLock().tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                read.lock.kind shouldBeEqualTo LockKind.READ
                blocking.readLock().releaseAsync(read).await() shouldBeEqualTo LockMutationResult.Released(0)

                val write = suspending.writeLock().tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                write.lock.kind shouldBeEqualTo LockKind.WRITE
                val downgraded = suspending.downgrade(write)
                    .shouldBeInstanceOf<DowngradeResult.Downgraded>()
                    .handle
                downgraded.lock.kind shouldBeEqualTo LockKind.READ
                suspending.readLock().release(downgraded) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                blocking.close()
                suspending.close()
            }
        }
    }

    @Test
    fun `future cancellation removes only the exact queued writer`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceReadWriteLock.create(connection, "read-write-future-cancel-${System.nanoTime()}")
            try {
                val reader = lock.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                val pending = lock.writeLock().acquireAsync(
                    OWNER_2,
                    REQUEST_2,
                    Duration.ofSeconds(2),
                    LEASE,
                )

                await().atMost(Duration.ofSeconds(2)).untilAsserted {
                    lock.writeLock().reconcile(OWNER_2, REQUEST_2)
                        .shouldBeInstanceOf<LockReconcileResult.Queued>()
                }
                pending.cancel(false) shouldBeEqualTo true
                await().atMost(Duration.ofSeconds(2)).untilAsserted {
                    lock.writeLock().reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.NotFound
                }

                lock.readLock().release(reader) shouldBeEqualTo LockMutationResult.Released(0)
                val replacement = lock.writeLock().tryAcquire(OWNER_3, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                lock.writeLock().release(replacement) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `suspend cancellation removes its queued reader without crossing the writer boundary`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val name = "read-write-suspend-cancel-${System.nanoTime()}"
            val blocking = LettuceReadWriteLock.create(connection, name)
            val suspending = LettuceSuspendReadWriteLock.create(connection, name)
            try {
                val writer = blocking.writeLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                val pending = async {
                    suspending.readLock().acquire(
                        OWNER_2,
                        REQUEST_2,
                        Duration.ofSeconds(2),
                        LEASE,
                    )
                }
                yield()
                await().atMost(Duration.ofSeconds(2)).untilAsserted {
                    blocking.readLock().reconcile(OWNER_2, REQUEST_2)
                        .shouldBeInstanceOf<LockReconcileResult.Queued>()
                }

                pending.cancelAndJoin()
                await().atMost(Duration.ofSeconds(2)).untilAsserted {
                    blocking.readLock().reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.NotFound
                }
                blocking.writeLock().release(writer) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                blocking.close()
                suspending.close()
            }
        }
    }

    @Test
    fun `close is idempotent and all views fail closed`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blocking = LettuceReadWriteLock.create(connection, "read-write-close-${System.nanoTime()}")
            val suspending = LettuceSuspendReadWriteLock.create(connection, "read-write-suspend-close-${System.nanoTime()}")
            val blockingHandle = blocking.writeLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            val suspendHandle = suspending.writeLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            blocking.close()
            blocking.close()
            suspending.close()
            suspending.close()

            blocking.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
            blocking.writeLock().tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
            blocking.downgrade(blockingHandle) shouldBeEqualTo DowngradeResult.Closed
            suspending.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
            suspending.writeLock().tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
            suspending.downgrade(suspendHandle) shouldBeEqualTo DowngradeResult.Closed
        }
    }

    @Test
    fun `watchdog renews reader ownership through the shared runtime`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceReadWriteLock.create(connection, "read-write-watchdog-${System.nanoTime()}")
            val watchdog = LeasePolicy.Watchdog(
                ttl = Duration.ofSeconds(3),
                renewalInterval = Duration.ofMillis(100),
                maxLifetime = Duration.ofSeconds(10),
            )
            try {
                val handle = lock.readLock().tryAcquire(OWNER_1, REQUEST_1, watchdog)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                await()
                    .pollDelay(Duration.ofMillis(3_200))
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted {
                        lock.readLock().inspect(handle)
                            .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
                    }
                lock.readLock().release(handle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `downgrading one watchdog write hold preserves renewal of the remaining write hold`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceReadWriteLock.create(
                connection,
                "read-write-watchdog-downgrade-${System.nanoTime()}",
            )
            val watchdog = LeasePolicy.Watchdog(
                ttl = Duration.ofSeconds(3),
                renewalInterval = Duration.ofMillis(100),
                maxLifetime = Duration.ofSeconds(10),
            )
            try {
                val first = lock.writeLock().tryAcquire(OWNER_1, REQUEST_1, watchdog)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                val remaining = lock.writeLock().tryAcquire(OWNER_1, REQUEST_2, watchdog)
                    .shouldBeInstanceOf<LockAcquireResult.Reentered<WriteLockHandle>>()
                    .handle
                val read = lock.downgrade(first)
                    .shouldBeInstanceOf<DowngradeResult.Downgraded>()
                    .handle

                await()
                    .pollDelay(Duration.ofMillis(3_200))
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted {
                        lock.writeLock().inspect(remaining)
                            .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
                    }
                lock.writeLock().release(remaining) shouldBeEqualTo LockMutationResult.Released(0)
                lock.readLock().release(read) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `close completes a pending future wait with Closed`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceReadWriteLock.create(connection, "read-write-pending-close-${System.nanoTime()}")
            lock.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
            val pending = lock.writeLock().acquireAsync(
                OWNER_2,
                REQUEST_2,
                Duration.ofSeconds(10),
                LEASE,
            )
            await().atMost(Duration.ofSeconds(2)).untilAsserted {
                lock.writeLock().reconcile(OWNER_2, REQUEST_2)
                    .shouldBeInstanceOf<LockReconcileResult.Queued>()
            }

            lock.close()

            pending.get(1, TimeUnit.SECONDS) shouldBeEqualTo LockAcquireResult.Closed
        }
    }

    private companion object {
        val OWNER_1 = LockOwnerId.from("read-write-owner-1")
        val OWNER_2 = LockOwnerId.from("read-write-owner-2")
        val OWNER_3 = LockOwnerId.from("read-write-owner-3")
        val REQUEST_1 = LockRequestId.from("read-write-request-1")
        val REQUEST_2 = LockRequestId.from("read-write-request-2")
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}

internal class ClusterLettuceReadWriteLockTest {

    @Test
    @Timeout(30)
    fun `cluster factories preserve phase compatibility and downgrade semantics`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val name = "read-write-cluster-${System.nanoTime()}"
                val blocking = LettuceReadWriteLock.create(connection, name)
                val suspending = LettuceSuspendReadWriteLock.create(connection, name)
                try {
                    val read = blocking.readLock().tryAcquire(CLUSTER_OWNER_1, CLUSTER_REQUEST_1, CLUSTER_LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                        .handle
                    val peer = suspending.readLock().tryAcquire(CLUSTER_OWNER_2, CLUSTER_REQUEST_2, CLUSTER_LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                        .handle
                    blocking.readLock().release(read) shouldBeEqualTo LockMutationResult.Released(0)
                    suspending.readLock().release(peer) shouldBeEqualTo LockMutationResult.Released(0)

                    val write = suspending.writeLock().tryAcquire(CLUSTER_OWNER_1, CLUSTER_REQUEST_3, CLUSTER_LEASE)
                        .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                        .handle
                    val downgraded = suspending.downgrade(write)
                        .shouldBeInstanceOf<DowngradeResult.Downgraded>()
                        .handle
                    suspending.readLock().release(downgraded) shouldBeEqualTo LockMutationResult.Released(0)
                } finally {
                    blocking.close()
                    suspending.close()
                }
            }
        }
    }

    private companion object {
        val CLUSTER_OWNER_1 = LockOwnerId.from("read-write-cluster-owner-1")
        val CLUSTER_OWNER_2 = LockOwnerId.from("read-write-cluster-owner-2")
        val CLUSTER_REQUEST_1 = LockRequestId.from("read-write-cluster-request-1")
        val CLUSTER_REQUEST_2 = LockRequestId.from("read-write-cluster-request-2")
        val CLUSTER_REQUEST_3 = LockRequestId.from("read-write-cluster-request-3")
        val CLUSTER_LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}
