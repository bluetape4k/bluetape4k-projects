package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test

class LettuceDistributedSemaphoreTest: AbstractLettuceTest() {

    @Test
    fun `invalid bounds and terminal states remain explicit`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-boundaries-${randomName().substringAfter(':')}"
        val config = SemaphoreConfig(maxPermits = 2)
        val keys = deriveSemaphoreKeys(name, config, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val blocking = LettuceDistributedSemaphore.create(connection, name, config)
        val suspending = LettuceSuspendDistributedSemaphore.create(connection, name, config)
        try {
            blocking.trySetPermits(0) shouldBeEqualTo SemaphoreInitializationResult.InvalidCapacity
            blocking.trySetPermitsAsync(3).get() shouldBeEqualTo SemaphoreInitializationResult.InvalidCapacity
            assertFailsWith<IllegalArgumentException> {
                blocking.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), 0)
            }
            assertFailsWith<IllegalArgumentException> {
                blocking.acquire(
                    SemaphoreOwnerId.random(),
                    SemaphoreRequestId.random(),
                    1,
                    java.time.Duration.ZERO,
                )
            }

            blocking.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random()) shouldBeEqualTo
                PermitAcquireResult.Unavailable
            blocking.acquire(
                SemaphoreOwnerId.random(),
                SemaphoreRequestId.random(),
                1,
                java.time.Duration.ofMillis(25),
            ) shouldBeEqualTo PermitAcquireResult.TimedOut

            blocking.trySetPermits(1).shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            val holder = blocking.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random())
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
                .handle
            blocking.acquireAsync(
                SemaphoreOwnerId.random(),
                SemaphoreRequestId.random(),
                1,
                java.time.Duration.ofMillis(25),
            ).get() shouldBeEqualTo PermitAcquireResult.TimedOut
            suspending.acquire(
                SemaphoreOwnerId.random(),
                SemaphoreRequestId.random(),
                1,
                java.time.Duration.ofMillis(25),
            ) shouldBeEqualTo PermitAcquireResult.TimedOut

            blocking.inspect(holder).shouldBeInstanceOf<PermitInspectResult.Owned<PermitHandle>>()
            blocking.release(holder).shouldBeInstanceOf<PermitMutationResult.Released<PermitHandle>>()
            blocking.inspect(holder) shouldBeEqualTo PermitInspectResult.Released
            blocking.reconcile(
                SemaphoreOwnerId.from("missing-owner"),
                SemaphoreRequestId.from("missing-request"),
            ) shouldBeEqualTo PermitReconcileResult.NotFound

            blocking.close()
            blocking.availablePermits() shouldBeEqualTo -1
            blocking.availablePermitsAsync().get() shouldBeEqualTo -1
            blocking.tryAcquire(
                SemaphoreOwnerId.random(),
                SemaphoreRequestId.random(),
            ) shouldBeEqualTo PermitAcquireResult.Closed
            blocking.inspect(holder) shouldBeEqualTo PermitInspectResult.Closed
            blocking.release(holder) shouldBeEqualTo PermitMutationResult.Closed
            blocking.reconcile(
                SemaphoreOwnerId.random(),
                SemaphoreRequestId.random(),
            ) shouldBeEqualTo PermitReconcileResult.Closed
        } finally {
            suspending.close()
            blocking.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `blocking async and suspend adapters preserve request idempotency`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-${randomName().substringAfter(':')}"
        val keys = deriveSemaphoreKeys(name, SemaphoreConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val blocking = LettuceDistributedSemaphore.create(connection, name)
        val suspending = LettuceSuspendDistributedSemaphore.create(connection, name)
        try {
            blocking.trySetPermits(3).shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            val owner = SemaphoreOwnerId.from("owner")
            val request = SemaphoreRequestId.from("request")
            val first = blocking.tryAcquire(owner, request, 2)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
            val replay = blocking.tryAcquireAsync(owner, request, 2).get()
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()

            replay.handle shouldBeEqualTo first.handle
            blocking.availablePermits() shouldBeEqualTo 1
            suspending.inspect(first.handle).shouldBeInstanceOf<PermitInspectResult.Owned<PermitHandle>>()
            suspending.release(first.handle)
                .shouldBeInstanceOf<PermitMutationResult.Released<PermitHandle>>()
                .remainingPermits shouldBeEqualTo 3
            blocking.release(first.handle) shouldBeEqualTo PermitMutationResult.AlreadyReleased
            blocking.tryAcquire(owner, request, 2) shouldBeEqualTo PermitAcquireResult.Unavailable
            blocking.availablePermits() shouldBeEqualTo 3
        } finally {
            suspending.close()
            blocking.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `capacity and contention are explicit`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-${randomName().substringAfter(':')}"
        val keys = deriveSemaphoreKeys(name, SemaphoreConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettuceDistributedSemaphore.create(connection, name)
        try {
            semaphore.trySetPermits(1).shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            semaphore.trySetPermits(2).shouldBeInstanceOf<SemaphoreInitializationResult.AlreadyInitialized>()
            semaphore.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), 2) shouldBeEqualTo
                PermitAcquireResult.CapacityExceeded
            semaphore.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), 1)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
            semaphore.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), 1) shouldBeEqualTo
                PermitAcquireResult.Unavailable
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `async and suspend lifecycle surfaces reconcile released requests and close fail closed`() = runSuspendIO {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "semaphore-lifecycle-${randomName().substringAfter(':')}"
        val keys = deriveSemaphoreKeys(name, SemaphoreConfig(), StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val blocking = LettuceDistributedSemaphore.create(connection, name)
        val suspending = LettuceSuspendDistributedSemaphore.create(connection, name)
        try {
            blocking.trySetPermitsAsync(2).get()
                .shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            blocking.availablePermitsAsync().get() shouldBeEqualTo 2

            val owner = SemaphoreOwnerId.from("async-owner")
            val request = SemaphoreRequestId.from("async-request")
            val handle = blocking.tryAcquireAsync(owner, request, 1).get()
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
                .handle
            blocking.inspectAsync(handle).get()
                .shouldBeInstanceOf<PermitInspectResult.Owned<PermitHandle>>()
            blocking.reconcileAsync(owner, request).get()
                .shouldBeInstanceOf<PermitReconcileResult.Owned<PermitHandle>>()
            blocking.releaseAsync(handle).get()
                .shouldBeInstanceOf<PermitMutationResult.Released<PermitHandle>>()

            suspending.reconcile(owner, request) shouldBeEqualTo PermitReconcileResult.Released
            val suspendOwner = SemaphoreOwnerId.from("suspend-owner")
            val suspendRequest = SemaphoreRequestId.from("suspend-request")
            val suspendHandle = suspending.tryAcquire(suspendOwner, suspendRequest, 1)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
                .handle
            suspending.inspect(suspendHandle)
                .shouldBeInstanceOf<PermitInspectResult.Owned<PermitHandle>>()
            suspending.reconcile(suspendOwner, suspendRequest)
                .shouldBeInstanceOf<PermitReconcileResult.Owned<PermitHandle>>()
            suspending.release(suspendHandle)
                .shouldBeInstanceOf<PermitMutationResult.Released<PermitHandle>>()

            blocking.close()
            blocking.availablePermitsAsync().get() shouldBeEqualTo -1
            blocking.trySetPermitsAsync(1).get() shouldBeEqualTo SemaphoreInitializationResult.Closed
        } finally {
            suspending.close()
            blocking.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }
}
