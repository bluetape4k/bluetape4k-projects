package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.time.Duration

/** 잔여 lock client 경계를 실제 Redis 상태 전이로 고정합니다. */
internal class LettuceResidualLockCoverageTest {

    @Test
    fun `blocking future read write surfaces cover mutation and downgrade`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceReadWriteLock.create(connection, "residual-rw-${System.nanoTime()}")
            try {
                val read = lock.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                lock.readLock().inspectAsync(read).await()
                    .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
                lock.readLock().reconcileAsync(OWNER_1, REQUEST_1).await()
                    .shouldBeInstanceOf<LockReconcileResult.Owned<ReadLockHandle>>()
                lock.readLock().renewAsync(read, Duration.ofSeconds(1)).await()
                    .shouldBeInstanceOf<LockMutationResult.Renewed<ReadLockHandle>>()
                lock.readLock().releaseAsync(read).await() shouldBeEqualTo LockMutationResult.Released(0)

                val write = lock.writeLock().tryAcquireAsync(OWNER_2, REQUEST_2, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                lock.writeLock().inspectAsync(write).await()
                    .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
                lock.writeLock().reconcileAsync(OWNER_2, REQUEST_2).await()
                    .shouldBeInstanceOf<LockReconcileResult.Owned<WriteLockHandle>>()
                lock.writeLock().renewAsync(write, Duration.ofSeconds(1)).await()
                    .shouldBeInstanceOf<LockMutationResult.Renewed<WriteLockHandle>>()
                val downgraded = lock.downgradeAsync(write).await()
                    .shouldBeInstanceOf<DowngradeResult.Downgraded>()
                    .handle
                lock.readLock().releaseAsync(downgraded).await() shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `suspending read write acquire paths preserve ownership and phase conversion`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceSuspendReadWriteLock.create(connection, "residual-suspend-rw-${System.nanoTime()}")
            try {
                val read = lock.readLock().acquire(OWNER_1, REQUEST_1, Duration.ofMillis(200), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                lock.readLock().inspect(read)
                    .shouldBeInstanceOf<LockInspectResult.Owned<ReadLockHandle>>()
                lock.readLock().reconcile(OWNER_1, REQUEST_1)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<ReadLockHandle>>()
                lock.readLock().renew(read, Duration.ofSeconds(1))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<ReadLockHandle>>()
                lock.readLock().release(read) shouldBeEqualTo LockMutationResult.Released(0)

                val write = lock.writeLock().acquire(OWNER_2, REQUEST_2, Duration.ofMillis(200), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                lock.writeLock().inspect(write)
                    .shouldBeInstanceOf<LockInspectResult.Owned<WriteLockHandle>>()
                lock.writeLock().reconcile(OWNER_2, REQUEST_2)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<WriteLockHandle>>()
                lock.writeLock().renew(write, Duration.ofSeconds(1))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<WriteLockHandle>>()
                val downgraded = lock.downgrade(write)
                    .shouldBeInstanceOf<DowngradeResult.Downgraded>()
                    .handle
                lock.readLock().release(downgraded) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `multi adapters retain request identity`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val multiConfig = MultiLockConfig(lock = LockConfig(hashTag = "residual-multi-${System.nanoTime()}"))
            val multi = LettuceMultiLock.create(connection, listOf("account", "inventory"), multiConfig)
            val suspendingMulti = LettuceSuspendMultiLock.create(
                connection,
                listOf("account", "inventory"),
                multiConfig,
            )
            try {
                val multiHandle = multi.acquireAsync(OWNER_1, REQUEST_1, Duration.ofMillis(200), LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                multi.inspectAsync(multiHandle).await()
                    .shouldBeInstanceOf<LockInspectResult.Owned<MultiLockHandle>>()
                multi.reconcileAsync(OWNER_1, REQUEST_1).await()
                    .shouldBeInstanceOf<LockReconcileResult.Owned<MultiLockHandle>>()
                multi.renewAsync(multiHandle, Duration.ofSeconds(1)).await()
                    .shouldBeInstanceOf<LockMutationResult.Renewed<MultiLockHandle>>()
                multi.releaseAsync(multiHandle).await() shouldBeEqualTo LockMutationResult.Released(0)

                val suspendingHandle = suspendingMulti.acquire(OWNER_2, REQUEST_2, Duration.ofMillis(200), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                suspendingMulti.inspect(suspendingHandle)
                    .shouldBeInstanceOf<LockInspectResult.Owned<MultiLockHandle>>()
                suspendingMulti.reconcile(OWNER_2, REQUEST_2)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<MultiLockHandle>>()
                suspendingMulti.renew(suspendingHandle, Duration.ofSeconds(1))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<MultiLockHandle>>()
                suspendingMulti.release(suspendingHandle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                multi.close()
                suspendingMulti.close()
            }
        }
    }

    @Test
    fun `fenced adapters retain request identity`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val fencedConfig = FencedLockConfig(epoch = 61)
            val fencedName = "residual-fenced-${System.nanoTime()}"
            val fenced = LettuceFencedLock.create(connection, fencedName, fencedConfig)
            val suspendingFenced = LettuceSuspendFencedLock.create(connection, fencedName, fencedConfig)
            val keys = deriveFencedLockKeys(fencedName, fencedConfig, connection.codec)
            try {
                fenced.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                val holder = fenced.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                    .handle
                val pending = fenced.acquireAsync(OWNER_2, REQUEST_2, Duration.ofSeconds(1), LEASE)
                fenced.release(holder) shouldBeEqualTo LockMutationResult.Released(0)

                val acquired = pending.await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                    .handle
                fenced.inspectAsync(acquired).await()
                    .shouldBeInstanceOf<LockInspectResult.Owned<FencedLockHandle>>()
                fenced.reconcileAsync(OWNER_2, REQUEST_2).await()
                    .shouldBeInstanceOf<LockReconcileResult.Owned<FencedLockHandle>>()
                fenced.renewAsync(acquired, Duration.ofSeconds(1)).await()
                    .shouldBeInstanceOf<LockMutationResult.Renewed<FencedLockHandle>>()
                fenced.releaseAsync(acquired).await() shouldBeEqualTo LockMutationResult.Released(0)

                suspendingFenced.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.AlreadyInitialized
                val suspendHandle = suspendingFenced.acquire(OWNER_3, REQUEST_3, Duration.ofMillis(200), LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                    .handle
                suspendingFenced.inspect(suspendHandle)
                    .shouldBeInstanceOf<LockInspectResult.Owned<FencedLockHandle>>()
                suspendingFenced.reconcile(OWNER_3, REQUEST_3)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<FencedLockHandle>>()
                suspendingFenced.renew(suspendHandle, Duration.ofSeconds(1))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<FencedLockHandle>>()
                suspendingFenced.release(suspendHandle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                fenced.close()
                suspendingFenced.close()
                connection.sync().del(*keys.all)
            }
        }
    }

    private companion object {
        val OWNER_1 = LockOwnerId.from("residual-owner-1")
        val OWNER_2 = LockOwnerId.from("residual-owner-2")
        val OWNER_3 = LockOwnerId.from("residual-owner-3")
        val REQUEST_1 = LockRequestId.from("residual-request-1")
        val REQUEST_2 = LockRequestId.from("residual-request-2")
        val REQUEST_3 = LockRequestId.from("residual-request-3")
        val LEASE = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}
