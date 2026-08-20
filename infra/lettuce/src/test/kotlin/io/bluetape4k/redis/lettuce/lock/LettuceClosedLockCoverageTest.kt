package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveMultiLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveReadWriteLockKeys
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.time.Duration

/**
 * 공개 lock adapter가 종료 이후 모든 실행 모델에서 동일한 terminal 결과를
 * 유지하는지 검증합니다.
 */
internal class LettuceClosedLockCoverageTest {

    @Test
    fun `multi lock surfaces return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingConfig = multiConfig("blocking")
            val suspendingConfig = multiConfig("suspending")
            var blocking: LettuceMultiLock? = null
            var suspending: LettuceSuspendMultiLock? = null
            var bodyFailure: Throwable? = null
            try {
                blocking = LettuceMultiLock.create(connection, NAMES, blockingConfig)
                suspending = LettuceSuspendMultiLock.create(connection, NAMES, suspendingConfig)
                val blockingLock = requireNotNull(blocking)
                val suspendingLock = requireNotNull(suspending)
                val blockingHandle = blockingLock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                val suspendingHandle = suspendingLock.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle

                blockingLock.close()
                blockingLock.close()
                suspendingLock.close()
                suspendingLock.close()

                blockingLock.tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blockingLock.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
                blockingLock.acquire(OWNER_1, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                blockingLock.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await() shouldBeEqualTo
                    LockAcquireResult.Closed
                blockingLock.inspect(blockingHandle) shouldBeEqualTo LockInspectResult.Closed
                blockingLock.inspectAsync(blockingHandle).await() shouldBeEqualTo LockInspectResult.Closed
                blockingLock.reconcile(OWNER_1, REQUEST_1) shouldBeEqualTo LockReconcileResult.Closed
                blockingLock.reconcileAsync(OWNER_1, REQUEST_1).await() shouldBeEqualTo LockReconcileResult.Closed
                blockingLock.renew(blockingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                blockingLock.renewAsync(blockingHandle, EXTENSION).await() shouldBeEqualTo
                    LockMutationResult.Closed
                blockingLock.release(blockingHandle) shouldBeEqualTo LockMutationResult.Closed
                blockingLock.releaseAsync(blockingHandle).await() shouldBeEqualTo LockMutationResult.Closed

                suspendingLock.tryAcquire(OWNER_2, REQUEST_2, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspendingLock.acquire(OWNER_2, REQUEST_2, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                suspendingLock.inspect(suspendingHandle) shouldBeEqualTo LockInspectResult.Closed
                suspendingLock.reconcile(OWNER_2, REQUEST_2) shouldBeEqualTo LockReconcileResult.Closed
                suspendingLock.renew(suspendingHandle, EXTENSION) shouldBeEqualTo LockMutationResult.Closed
                suspendingLock.release(suspendingHandle) shouldBeEqualTo LockMutationResult.Closed
            } catch (failure: Throwable) {
                bodyFailure = failure
                throw failure
            } finally {
                val cleanupFailures = mutableListOf<Throwable>()
                fun cleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
                }
                cleanup { blocking?.close() }
                cleanup { suspending?.close() }
                cleanup {
                    cleanupRedisKeys(
                        deriveMultiLockKeys(NAMES, blockingConfig, StringCodec.UTF8).all,
                        deriveMultiLockKeys(NAMES, suspendingConfig, StringCodec.UTF8).all,
                    )
                }
                reportCleanupFailures(bodyFailure, cleanupFailures)
            }
        }
    }

    @Test
    fun `read write lock views return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingName = "closed-rw-blocking-${System.nanoTime()}"
            val suspendingName = "closed-rw-suspending-${System.nanoTime()}"
            val config = ReadWriteLockConfig()
            var blocking: LettuceReadWriteLock? = null
            var suspending: LettuceSuspendReadWriteLock? = null
            var bodyFailure: Throwable? = null
            try {
                blocking = LettuceReadWriteLock.create(connection, blockingName, config)
                suspending = LettuceSuspendReadWriteLock.create(connection, suspendingName, config)
                val blockingLock = requireNotNull(blocking)
                val suspendingLock = requireNotNull(suspending)
                val blockingRead = blockingLock.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                blockingLock.readLock().release(blockingRead) shouldBeEqualTo LockMutationResult.Released(0)
                val blockingWrite = blockingLock.writeLock().tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle
                val suspendingRead = suspendingLock.readLock().tryAcquire(OWNER_2, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                    .handle
                suspendingLock.readLock().release(suspendingRead) shouldBeEqualTo LockMutationResult.Released(0)
                val suspendingWrite = suspendingLock.writeLock().tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                    .handle

                blockingLock.close()
                blockingLock.close()
                suspendingLock.close()
                suspendingLock.close()

                verifyBlockingReadClosed(blockingLock.readLock(), blockingRead)
                verifyBlockingWriteClosed(blockingLock.writeLock(), blockingWrite)
                blockingLock.downgrade(blockingWrite) shouldBeEqualTo DowngradeResult.Closed
                blockingLock.downgradeAsync(blockingWrite).await() shouldBeEqualTo DowngradeResult.Closed

                verifySuspendingReadClosed(suspendingLock.readLock(), suspendingRead)
                verifySuspendingWriteClosed(suspendingLock.writeLock(), suspendingWrite)
                suspendingLock.downgrade(suspendingWrite) shouldBeEqualTo DowngradeResult.Closed
            } catch (failure: Throwable) {
                bodyFailure = failure
                throw failure
            } finally {
                val cleanupFailures = mutableListOf<Throwable>()
                fun cleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
                }
                cleanup { blocking?.close() }
                cleanup { suspending?.close() }
                cleanup {
                    cleanupRedisKeys(
                        deriveReadWriteLockKeys(blockingName, config, StringCodec.UTF8).all.toList(),
                        deriveReadWriteLockKeys(suspendingName, config, StringCodec.UTF8).all.toList(),
                    )
                }
                reportCleanupFailures(bodyFailure, cleanupFailures)
            }
        }
    }

    @Test
    fun `fenced lock surfaces return Closed after close`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val blockingName = "closed-fenced-blocking-${System.nanoTime()}"
            val suspendingName = "closed-fenced-suspending-${System.nanoTime()}"
            val config = FencedLockConfig(epoch = 71)
            var blocking: LettuceFencedLock? = null
            var suspending: LettuceSuspendFencedLock? = null
            var bodyFailure: Throwable? = null
            try {
                blocking = LettuceFencedLock.create(connection, blockingName, config)
                suspending = LettuceSuspendFencedLock.create(connection, suspendingName, config)
                val blockingLock = requireNotNull(blocking)
                val suspendingLock = requireNotNull(suspending)
                blockingLock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                suspendingLock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                val blockingHandle = blockingLock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                    .handle
                val suspendingHandle = suspendingLock.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
                    .handle

                blockingLock.close()
                blockingLock.close()
                suspendingLock.close()
                suspendingLock.close()

                verifyFencedClosed(blockingLock, blockingHandle, suspendingLock, suspendingHandle)
            } catch (failure: Throwable) {
                bodyFailure = failure
                throw failure
            } finally {
                val cleanupFailures = mutableListOf<Throwable>()
                fun cleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
                }
                cleanup { blocking?.close() }
                cleanup { suspending?.close() }
                cleanup {
                    cleanupRedisKeys(
                        deriveFencedLockKeys(blockingName, config, StringCodec.UTF8).all.toList(),
                        deriveFencedLockKeys(suspendingName, config, StringCodec.UTF8).all.toList(),
                    )
                }
                reportCleanupFailures(bodyFailure, cleanupFailures)
            }
        }
    }

    @Test
    fun `multi lock surfaces return backend failure after disconnect`() = runSuspendIO {
        var connection: StatefulRedisConnection<String, String>? = null
        val blockingConfig = multiConfig("disconnected-blocking")
        val suspendingConfig = multiConfig("disconnected-suspending")
        var blocking: LettuceMultiLock? = null
        var suspending: LettuceSuspendMultiLock? = null
        var bodyFailure: Throwable? = null
        try {
            connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
            val activeConnection = requireNotNull(connection)
            blocking = LettuceMultiLock.create(activeConnection, NAMES, blockingConfig)
            suspending = LettuceSuspendMultiLock.create(activeConnection, NAMES, suspendingConfig)
            val activeBlocking = requireNotNull(blocking)
            val activeSuspending = requireNotNull(suspending)
            val blockingHandle = activeBlocking.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            val suspendingHandle = activeSuspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            activeConnection.close()

            verifyMultiBackendFailure(activeBlocking, blockingHandle)
            verifySuspendingMultiBackendFailure(activeSuspending, suspendingHandle)
        } catch (failure: Throwable) {
            bodyFailure = failure
            throw failure
        } finally {
            val cleanupFailures = mutableListOf<Throwable>()
            fun cleanup(block: () -> Unit) {
                try {
                    block()
                } catch (failure: Throwable) {
                    cleanupFailures += failure
                }
            }
            cleanup { blocking?.close() }
            cleanup { suspending?.close() }
            cleanup { connection?.close() }
            cleanup {
                cleanupRedisKeys(
                    deriveMultiLockKeys(NAMES, blockingConfig, StringCodec.UTF8).all,
                    deriveMultiLockKeys(NAMES, suspendingConfig, StringCodec.UTF8).all,
                )
            }
            reportCleanupFailures(bodyFailure, cleanupFailures)
        }
    }

    private suspend fun verifyMultiBackendFailure(
        blocking: LettuceMultiLock,
        handle: MultiLockHandle,
    ) {
        blocking.tryAcquire(OWNER_1, REQUEST_1, LEASE)
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        blocking.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await()
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        blocking.acquire(OWNER_1, REQUEST_1, WAIT, LEASE)
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        blocking.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await()
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        blocking.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        blocking.inspectAsync(handle).await().shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        blocking.reconcile(OWNER_1, REQUEST_1)
            .shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        blocking.reconcileAsync(OWNER_1, REQUEST_1).await()
            .shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        blocking.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        blocking.renewAsync(handle, EXTENSION).await()
            .shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        blocking.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        blocking.releaseAsync(handle).await().shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    private suspend fun verifySuspendingMultiBackendFailure(
        suspending: LettuceSuspendMultiLock,
        handle: MultiLockHandle,
    ) {
        suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        suspending.acquire(OWNER_2, REQUEST_2, WAIT, LEASE)
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        suspending.inspect(handle).shouldBeInstanceOf<LockInspectResult.BackendFailure>()
        suspending.reconcile(OWNER_2, REQUEST_2)
            .shouldBeInstanceOf<LockReconcileResult.BackendFailure>()
        suspending.renew(handle, EXTENSION).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
        suspending.release(handle).shouldBeInstanceOf<LockMutationResult.BackendFailure>()
    }

    @Test
    fun `read write lock surfaces return backend failure after disconnect`() = runSuspendIO {
        var connection: StatefulRedisConnection<String, String>? = null
        val blockingName = "disconnected-rw-blocking-${System.nanoTime()}"
        val suspendingName = "disconnected-rw-suspending-${System.nanoTime()}"
        val config = ReadWriteLockConfig()
        var blocking: LettuceReadWriteLock? = null
        var suspending: LettuceSuspendReadWriteLock? = null
        var bodyFailure: Throwable? = null
        try {
            connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
            val activeConnection = requireNotNull(connection)
            blocking = LettuceReadWriteLock.create(activeConnection, blockingName, config)
            suspending = LettuceSuspendReadWriteLock.create(activeConnection, suspendingName, config)
            val activeBlocking = requireNotNull(blocking)
            val activeSuspending = requireNotNull(suspending)
            val blockingRead = activeBlocking.readLock().tryAcquire(OWNER_1, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            activeBlocking.readLock().release(blockingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val blockingWrite = activeBlocking.writeLock().tryAcquire(OWNER_1, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            val suspendingRead = activeSuspending.readLock().tryAcquire(OWNER_2, REQUEST_1, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<ReadLockHandle>>()
                .handle
            activeSuspending.readLock().release(suspendingRead) shouldBeEqualTo LockMutationResult.Released(0)
            val suspendingWrite = activeSuspending.writeLock().tryAcquire(OWNER_2, REQUEST_2, LEASE)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
                .handle
            activeConnection.close()

            verifyBlockingReadBackendFailure(activeBlocking.readLock(), blockingRead)
            verifyBlockingWriteBackendFailure(activeBlocking.writeLock(), blockingWrite)
            activeBlocking.downgrade(blockingWrite).shouldBeInstanceOf<DowngradeResult.BackendFailure>()
            activeBlocking.downgradeAsync(blockingWrite).await()
                .shouldBeInstanceOf<DowngradeResult.BackendFailure>()
            verifySuspendingReadBackendFailure(activeSuspending.readLock(), suspendingRead)
            verifySuspendingWriteBackendFailure(activeSuspending.writeLock(), suspendingWrite)
            activeSuspending.downgrade(suspendingWrite)
                .shouldBeInstanceOf<DowngradeResult.BackendFailure>()
        } catch (failure: Throwable) {
            bodyFailure = failure
            throw failure
        } finally {
            val cleanupFailures = mutableListOf<Throwable>()
            fun cleanup(block: () -> Unit) {
                try {
                    block()
                } catch (failure: Throwable) {
                    cleanupFailures += failure
                }
            }
            cleanup { blocking?.close() }
            cleanup { suspending?.close() }
            cleanup { connection?.close() }
            cleanup {
                cleanupRedisKeys(
                    deriveReadWriteLockKeys(blockingName, config, StringCodec.UTF8).all.toList(),
                    deriveReadWriteLockKeys(suspendingName, config, StringCodec.UTF8).all.toList(),
                )
            }
            reportCleanupFailures(bodyFailure, cleanupFailures)
        }
    }

    private suspend fun verifyBlockingReadClosed(
        view: LettuceReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_1, LEASE) shouldBeEqualTo LockAcquireResult.Closed
        view.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await() shouldBeEqualTo LockAcquireResult.Closed
        view.acquire(OWNER_1, REQUEST_1, WAIT, LEASE) shouldBeEqualTo LockAcquireResult.Closed
                view.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await() shouldBeEqualTo
                    LockAcquireResult.Closed
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
                view.acquireAsync(OWNER_1, REQUEST_2, WAIT, LEASE).await() shouldBeEqualTo
                    LockAcquireResult.Closed
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

    private suspend fun verifyFencedClosed(
        blocking: LettuceFencedLock,
        blockingHandle: FencedLockHandle,
        suspending: LettuceSuspendFencedLock,
        suspendingHandle: FencedLockHandle,
    ) {
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
    }

    private suspend fun verifyBlockingReadBackendFailure(
        view: LettuceReadWriteLock.ReadLockView,
        handle: ReadLockHandle,
    ) {
        view.tryAcquire(OWNER_1, REQUEST_1, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await().shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquire(OWNER_1, REQUEST_1, WAIT, LEASE).shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
        view.acquireAsync(OWNER_1, REQUEST_1, WAIT, LEASE).await()
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
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
        view.acquireAsync(OWNER_1, REQUEST_2, WAIT, LEASE).await()
            .shouldBeInstanceOf<LockAcquireResult.BackendFailure>()
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

    private fun cleanupRedisKeys(vararg keySets: List<String>) {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val keys = keySets.asSequence().flatten().toList().toTypedArray()
            connection.sync().del(*keys)
            connection.sync().exists(*keys) shouldBeEqualTo 0L
        }
    }

    private fun reportCleanupFailures(bodyFailure: Throwable?, cleanupFailures: List<Throwable>) {
        cleanupFailures.firstOrNull()?.let { first ->
            cleanupFailures.drop(1).forEach(first::addSuppressed)
            bodyFailure?.addSuppressed(first) ?: throw first
        }
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
