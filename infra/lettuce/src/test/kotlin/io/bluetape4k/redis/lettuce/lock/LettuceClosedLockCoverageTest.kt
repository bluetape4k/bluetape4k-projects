package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveMultiLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveReadWriteLockKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.time.Duration

/** 공개 lock adapter가 종료 이후 모든 실행 모델에서 동일한 terminal 결과를 유지하는지 검증합니다. */
internal class LettuceClosedLockCoverageTest {

    @Test
    fun `multi lock surfaces return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingConfig = multiConfig("blocking")
            val suspendingConfig = multiConfig("suspending")
            val blocking = LettuceMultiLock.create(connection, NAMES, blockingConfig)
            val suspending = LettuceSuspendMultiLock.create(connection, NAMES, suspendingConfig)
            val blockingHandle = blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            val suspendingHandle = suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            try {
                blocking.close()
                blocking.close()
                suspending.close()
                suspending.close()

                blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blocking.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
                blocking.acquire(OWNER_1, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blocking.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
                blocking.inspect(blockingHandle) shouldBeEqualTo LockInspectResult.Closed
                blocking.inspectAsync(blockingHandle).await() shouldBeEqualTo LockInspectResult.Closed
                blocking.reconcile(OWNER_1, REQUEST_1) shouldBeEqualTo LockReconcileResult.Closed
                blocking.reconcileAsync(OWNER_1, REQUEST_1).await() shouldBeEqualTo LockReconcileResult.Closed
                blocking.renew(blockingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                blocking.renewAsync(blockingHandle, EXTENSION).await() shouldBeEqualTo LockMutationResult.Closed
                blocking.release(blockingHandle) shouldBeEqualTo LockMutationResult.Closed
                blocking.releaseAsync(blockingHandle).await() shouldBeEqualTo LockMutationResult.Closed

                suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspending.acquire(OWNER_2, REQUEST_2, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspending.inspect(suspendingHandle) shouldBeEqualTo LockInspectResult.Closed
                suspending.reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.Closed
                suspending.renew(suspendingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                suspending.release(suspendingHandle) shouldBeEqualTo LockMutationResult.Closed
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*deriveMultiLockKeys(NAMES, blockingConfig, connection.codec).all.toTypedArray())
                connection.sync().del(*deriveMultiLockKeys(NAMES, suspendingConfig, connection.codec).all.toTypedArray())
            }
        }
    }

    @Test
    fun `read write lock views return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingName = "closed-rw-blocking-${System.nanoTime()}"
            val suspendingName = "closed-rw-suspending-${System.nanoTime()}"
            val config = ReadWriteLockConfig()
            val blocking = LettuceReadWriteLock.create(connection, blockingName, config)
            val suspending = LettuceSuspendReadWriteLock.create(connection, suspendingName, config)
            val blockingRead = blocking.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            blocking.readLock().release(blockingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val blockingWrite = blocking.writeLock().tryAcquire(OWNER_1, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            val suspendingRead = suspending.readLock().tryAcquire(OWNER_2, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            suspending.readLock().release(suspendingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val suspendingWrite = suspending.writeLock().tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            try {
                blocking.close()
                blocking.close()
                suspending.close()
                suspending.close()

                verifyBlockingReadClosed(blocking.readLock(), blockingRead)
                verifyBlockingWriteClosed(blocking.writeLock(), blockingWrite)
                blocking.downgrade(blockingWrite) shouldBeEqualTo DowngradeResult.Closed
                blocking.downgradeAsync(blockingWrite).await() shouldBeEqualTo DowngradeResult.Closed

                verifySuspendingReadClosed(suspending.readLock(), suspendingRead)
                verifySuspendingWriteClosed(suspending.writeLock(), suspendingWrite)
                suspending.downgrade(suspendingWrite) shouldBeEqualTo DowngradeResult.Closed
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*deriveReadWriteLockKeys(blockingName, config, connection.codec).all)
                connection.sync().del(*deriveReadWriteLockKeys(suspendingName, config, connection.codec).all)
            }
        }
    }

    @Test
    fun `fenced lock surfaces return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingName = "closed-fenced-blocking-${System.nanoTime()}"
            val suspendingName = "closed-fenced-suspending-${System.nanoTime()}"
            val config = FencedLockConfig(epoch = 71)
            val blocking = LettuceFencedLock.create(connection, blockingName, config)
            val suspending = LettuceSuspendFencedLock.create(connection, suspendingName, config)
            blocking.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
            suspending.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
            val blockingHandle = blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                .handle
            val suspendingHandle = suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                .handle
            try {
                blocking.close()
                blocking.close()
                suspending.close()
                suspending.close()

                blocking.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Closed
                blocking.bootstrapFencingAsync().await() shouldBeEqualTo FencedBootstrapResult.Closed
                blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blocking.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
                blocking.acquire(OWNER_1, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blocking.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
                blocking.inspect(blockingHandle) shouldBeEqualTo LockInspectResult.Closed
                blocking.inspectAsync(blockingHandle).await() shouldBeEqualTo LockInspectResult.Closed
                blocking.reconcile(OWNER_1, REQUEST_1) shouldBeEqualTo LockReconcileResult.Closed
                blocking.reconcileAsync(OWNER_1, REQUEST_1).await() shouldBeEqualTo LockReconcileResult.Closed
                blocking.renew(blockingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                blocking.renewAsync(blockingHandle, EXTENSION).await() shouldBeEqualTo LockMutationResult.Closed
                blocking.release(blockingHandle) shouldBeEqualTo LockMutationResult.Closed
                blocking.releaseAsync(blockingHandle).await() shouldBeEqualTo LockMutationResult.Closed

                suspending.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Closed
                suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspending.acquire(OWNER_2, REQUEST_2, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspending.inspect(suspendingHandle) shouldBeEqualTo LockInspectResult.Closed
                suspending.reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.Closed
                suspending.renew(suspendingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                suspending.release(suspendingHandle) shouldBeEqualTo LockMutationResult.Closed
            } finally {
                blocking.close()
                suspending.close()
                connection.sync().del(*deriveFencedLockKeys(blockingName, config, connection.codec).all)
                connection.sync().del(*deriveFencedLockKeys(suspendingName, config, connection.codec).all)
            }
        }
    }

    @Test
    fun `multi lock surfaces return backend failure after disconnect`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val blockingConfig = multiConfig("disconnected-blocking")
        val suspendingConfig = multiConfig("disconnected-suspending")
        val blocking = LettuceMultiLock.create(connection, NAMES, blockingConfig)
        val suspending = LettuceSuspendMultiLock.create(connection, NAMES, suspendingConfig)
        try {
            val blockingHandle = blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            val suspendingHandle = suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            connection.close()

            blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            blocking.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await()
                .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            blocking.acquire(OWNER_1, REQUEST_1, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            blocking.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await()
                .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            blocking.inspect(blockingHandle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
            blocking.inspectAsync(blockingHandle).await().shouldBeInstanceOf<LockInspectResult.BackendFailure>()
            blocking.reconcile(OWNER_1, REQUEST_1).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
            blocking.reconcileAsync(OWNER_1, REQUEST_1).await().shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
            blocking.renew(blockingHandle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
            blocking.renewAsync(blockingHandle, EXTENSION).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
            blocking.release(blockingHandle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
            blocking.releaseAsync(blockingHandle).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()

            suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            suspending.acquire(OWNER_2, REQUEST_2, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
            suspending.inspect(suspendingHandle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
            suspending.reconcile(OWNER_2, REQUEST_2).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
            suspending.renew(suspendingHandle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
            suspending.release(suspendingHandle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        } finally {
            blocking.close()
            suspending.close()
            connection.close()
            LettuceTestUtils.client.connect(StringCodec.UTF8).use { cleanupConnection ->
                val cleanupKeys = deriveMultiLockKeys(NAMES, blockingConfig, cleanupConnection.codec)
                    .all.toTypedArray() + deriveMultiLockKeys(NAMES, suspendingConfig, cleanupConnection.codec)
                    .all.toTypedArray()
                cleanupConnection.sync().del(*cleanupKeys)
                cleanupConnection.sync().exists(*cleanupKeys) shouldBeEqualTo 0L
            }
        }
    }

    @Test
    fun `read write lock surfaces return backend failure after disconnect`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val blockingName = "disconnected-rw-blocking-${System.nanoTime()}"
        val suspendingName = "disconnected-rw-suspending-${System.nanoTime()}"
        val config = ReadWriteLockConfig()
        val blocking = LettuceReadWriteLock.create(connection, blockingName, config)
        val suspending = LettuceSuspendReadWriteLock.create(connection, suspendingName, config)
        try {
            val blockingRead = blocking.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            blocking.readLock().release(blockingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val blockingWrite = blocking.writeLock().tryAcquire(OWNER_1, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            val suspendingRead = suspending.readLock().tryAcquire(OWNER_2, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            suspending.readLock().release(suspendingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val suspendingWrite = suspending.writeLock().tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            connection.close()

            verifyBlockingReadBackendFailure(blocking.readLock(), blockingRead)
            verifyBlockingWriteBackendFailure(blocking.writeLock(), blockingWrite)
            blocking.downgrade(blockingWrite).shouldBeInstanceOf<DowngradeResult.BackendFailure>()
            blocking.downgradeAsync(blockingWrite).await().shouldBeInstanceOf<DowngradeResult.BackendFailure>()
            verifySuspendingReadBackendFailure(suspending.readLock(), suspendingRead)
            verifySuspendingWriteBackendFailure(suspending.writeLock(), suspendingWrite)
            suspending.downgrade(suspendingWrite).shouldBeInstanceOf<DowngradeResult.BackendFailure>()
        } finally {
            blocking.close()
            suspending.close()
            connection.close()
            LettuceTestUtils.client.connect(StringCodec.UTF8).use { cleanupConnection ->
                val cleanupKeys = deriveReadWriteLockKeys(blockingName, config, cleanupConnection.codec).all +
                    deriveReadWriteLockKeys(suspendingName, config, cleanupConnection.codec).all
                cleanupConnection.sync().del(*cleanupKeys)
                cleanupConnection.sync().exists(*cleanupKeys) shouldBeEqualTo 0L
            }
        }
    }

    private suspend fun verifyBlockingReadClosed(
        view: LettuceReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
        view.acquire(OWNER_1, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
        view.inspect(handle) shouldBeEqualTo LockInspectResult.Closed
        view.inspectAsync(handle).await() shouldBeEqualTo LockInspectResult.Closed
        view.reconcile(OWNER_1, REQUEST_1) shouldBeEqualTo LockReconcileResult.Closed
        view.reconcileAsync(OWNER_1, REQUEST_1).await() shouldBeEqualTo LockReconcileResult.Closed
        view.renew(handle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
        view.renewAsync(handle, EXTENSION).await() shouldBeEqualTo LockMutationResult.Closed
        view.release(handle) shouldBeEqualTo LockMutationResult.Closed
        view.releaseAsync(handle).await() shouldBeEqualTo LockMutationResult.Closed
    }

    private suspend fun verifyBlockingWriteClosed(
        view: LettuceReadWriteLock.WriteLockView,
        handle: WriteLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_2, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.tryAcquireAsync(OWNER_1, REQUEST_2, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
        view.acquire(OWNER_1, REQUEST_2, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.acquireAsync(OWNER_1, REQUEST_2, WAIT, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
        view.inspect(handle) shouldBeEqualTo LockInspectResult.Closed
        view.inspectAsync(handle).await() shouldBeEqualTo LockInspectResult.Closed
        view.reconcile(OWNER_1, REQUEST_2) shouldBeEqualTo LockReconcileResult.Closed
        view.reconcileAsync(OWNER_1, REQUEST_2).await() shouldBeEqualTo LockReconcileResult.Closed
        view.renew(handle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
        view.renewAsync(handle, EXTENSION).await() shouldBeEqualTo LockMutationResult.Closed
        view.release(handle) shouldBeEqualTo LockMutationResult.Closed
        view.releaseAsync(handle).await() shouldBeEqualTo LockMutationResult.Closed
    }

    private suspend fun verifySuspendingReadClosed(
        view: LettuceSuspendReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_2, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.acquire(OWNER_2, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.inspect(handle) shouldBeEqualTo LockInspectResult.Closed
        view.reconcile(OWNER_2, REQUEST_1) shouldBeEqualTo LockReconcileResult.Closed
        view.renew(handle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
        view.release(handle) shouldBeEqualTo LockMutationResult.Closed
    }

    private suspend fun verifySuspendingWriteClosed(
        view: LettuceSuspendReadWriteLock.WriteLockView,
        handle: WriteLockHandle,
    ) {
        view.tryAcquire(OWNER_2, REQUEST_2, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.acquire(OWNER_2, REQUEST_2, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.inspect(handle) shouldBeEqualTo LockInspectResult.Closed
        view.reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.Closed
        view.renew(handle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
        view.release(handle) shouldBeEqualTo LockMutationResult.Closed
    }

    private suspend fun verifyBlockingReadBackendFailure(
        view: LettuceReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_1, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await().shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquire(OWNER_1, REQUEST_1, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await().shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.inspectAsync(handle).await().shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.reconcile(OWNER_1, REQUEST_1).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.reconcileAsync(OWNER_1, REQUEST_1).await().shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.renewAsync(handle, EXTENSION).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.releaseAsync(handle).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    private suspend fun verifyBlockingWriteBackendFailure(
        view: LettuceReadWriteLock.WriteLockView,
        handle: WriteLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_2, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.tryAcquireAsync(OWNER_1, REQUEST_2, LEASE).await().shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquire(OWNER_1, REQUEST_2, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquireAsync(OWNER_1, REQUEST_2, WAIT, LEASE).await().shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.inspectAsync(handle).await().shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.reconcile(OWNER_1, REQUEST_2).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.reconcileAsync(OWNER_1, REQUEST_2).await().shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.renewAsync(handle, EXTENSION).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.releaseAsync(handle).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    private suspend fun verifySuspendingReadBackendFailure(
        view: LettuceSuspendReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_2, REQUEST_1, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquire(OWNER_2, REQUEST_1, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.reconcile(OWNER_2, REQUEST_1).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    private suspend fun verifySuspendingWriteBackendFailure(
        view: LettuceSuspendReadWriteLock.WriteLockView,
        handle: WriteLockHandle,
    ) {
        view.tryAcquire(OWNER_2, REQUEST_2, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquire(OWNER_2, REQUEST_2, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        view.reconcile(OWNER_2, REQUEST_2).shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        view.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        view.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    private fun multiConfig(suffix: String): MultiLockConfig =
        MultiLockConfig(lock = LockConfig(hashTag = "closed-multi-$suffix-${System.nanoTime()}"))

    private companion object {
        val NAMES = listOf("account", "inventory")
        val OWNER_1 = LockOwnerId.from("closed-owner-1")
        val OWNER_2 = LockOwnerId.from("closed-owner-2")
        val REQUEST_1 = LockRequestId.from("closed-request-1")
        val REQUEST_2 = LockRequestId.from("closed-request-2")
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
        val WAIT: Duration = Duration.ofMillis(20)
        val EXTENSION: Duration = Duration.ofSeconds(1)
    }
}
