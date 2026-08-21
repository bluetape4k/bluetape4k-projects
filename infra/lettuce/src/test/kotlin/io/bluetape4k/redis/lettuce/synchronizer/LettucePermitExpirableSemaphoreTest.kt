package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration

class LettucePermitExpirableSemaphoreTest: AbstractLettuceTest() {

    @Test
    fun `three unit leases expire and restore exactly three permits`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofMillis(150))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettucePermitExpirableSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermits(3).shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            val acquired = semaphore.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random(), 3)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()

            acquired.handle.leases.size shouldBeEqualTo 3
            acquired.handle.leases.map { it.permitId }.distinct().size shouldBeEqualTo 3
            semaphore.availablePermits() shouldBeEqualTo 0
            Thread.sleep(220)
            semaphore.availablePermits() shouldBeEqualTo 3
            semaphore.release(acquired.handle) shouldBeEqualTo PermitMutationResult.Expired
            semaphore.tryAcquire(
                acquired.handle.permit.ownerId,
                acquired.handle.permit.requestId,
                3,
            ) shouldBeEqualTo PermitAcquireResult.Unavailable
            semaphore.availablePermits() shouldBeEqualTo 3
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `cleanup batch limit restores expired allocations in bounded passes`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-batch-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(
            leaseTime = Duration.ofMillis(150),
            cleanupBatchLimit = 2,
        )
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettucePermitExpirableSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermits(4)
            repeat(4) {
                semaphore.tryAcquire(
                    SemaphoreOwnerId.from("owner-$it"),
                    SemaphoreRequestId.from("request-$it"),
                ).shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()
            }
            Thread.sleep(220)
            semaphore.availablePermits() shouldBeEqualTo 2
            semaphore.availablePermits() shouldBeEqualTo 4
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `renew distinguishes explicit release from ownership loss`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-renew-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(5))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettucePermitExpirableSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermits(2)
            val released = semaphore.tryAcquire(
                SemaphoreOwnerId.from("released-owner"),
                SemaphoreRequestId.from("released-request"),
            ).shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>().handle
            semaphore.release(released).shouldBeInstanceOf<PermitMutationResult.Released<ExpirablePermitHandle>>()
            semaphore.renew(released, Duration.ofSeconds(1)) shouldBeEqualTo PermitRenewResult.Released

            val lost = semaphore.tryAcquire(
                SemaphoreOwnerId.from("lost-owner"),
                SemaphoreRequestId.from("lost-request"),
            ).shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>().handle
            connection.sync().hdel(keys.allocations, lost.permit.token)
            connection.sync().hset(
                keys.requests,
                "lost-owner|lost-request",
                "replacement-allocation",
            )
            semaphore.renew(lost, Duration.ofSeconds(1)) shouldBeEqualTo PermitRenewResult.OwnershipLost
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `async and suspend modes preserve acquire renew inspect and release contracts`() = runTest {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-modes-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(5))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val future = LettucePermitExpirableSemaphore.create(connection, name, config)
        val suspending = LettuceSuspendPermitExpirableSemaphore.create(connection, name, config)
        try {
            future.trySetPermitsAsync(2).get()
                .shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()

            val futureHandle = future.tryAcquireAsync(
                SemaphoreOwnerId.from("future-owner"),
                SemaphoreRequestId.from("future-request"),
            ).get().shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>().handle
            future.inspectAsync(futureHandle).get()
                .shouldBeInstanceOf<PermitInspectResult.Owned<ExpirablePermitHandle>>()
            val renewedFutureHandle = future.renewAsync(futureHandle, Duration.ofSeconds(1)).get()
                .shouldBeInstanceOf<PermitRenewResult.Renewed<ExpirablePermitHandle>>().handle
            future.releaseAsync(renewedFutureHandle).get()
                .shouldBeInstanceOf<PermitMutationResult.Released<ExpirablePermitHandle>>()

            val suspendHandle = suspending.tryAcquire(
                SemaphoreOwnerId.from("suspend-owner"),
                SemaphoreRequestId.from("suspend-request"),
            ).shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>().handle
            suspending.inspect(suspendHandle)
                .shouldBeInstanceOf<PermitInspectResult.Owned<ExpirablePermitHandle>>()
            val renewedSuspendHandle = suspending.renew(suspendHandle, Duration.ofSeconds(1))
                .shouldBeInstanceOf<PermitRenewResult.Renewed<ExpirablePermitHandle>>().handle
            suspending.release(renewedSuspendHandle)
                .shouldBeInstanceOf<PermitMutationResult.Released<ExpirablePermitHandle>>()
            suspending.availablePermits() shouldBeEqualTo 2
        } finally {
            future.close()
            suspending.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `async reconcile and closed surfaces preserve expirable allocation ownership`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-reconcile-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(5))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettucePermitExpirableSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermitsAsync(2).get()
                .shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
            semaphore.availablePermitsAsync().get() shouldBeEqualTo 2

            val owner = SemaphoreOwnerId.from("reconcile-owner")
            val request = SemaphoreRequestId.from("reconcile-request")
            val handle = semaphore.tryAcquireAsync(owner, request).get()
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()
                .handle
            semaphore.reconcileAsync(owner, request).get()
                .shouldBeInstanceOf<PermitReconcileResult.Owned<ExpirablePermitHandle>>()
            semaphore.releaseAsync(handle).get()
                .shouldBeInstanceOf<PermitMutationResult.Released<ExpirablePermitHandle>>()
            semaphore.reconcile(owner, request) shouldBeEqualTo PermitReconcileResult.Released
            semaphore.reconcileAsync(
                SemaphoreOwnerId.from("missing-owner"),
                SemaphoreRequestId.from("missing-request"),
            ).get() shouldBeEqualTo PermitReconcileResult.NotFound

            semaphore.close()
            semaphore.availablePermitsAsync().get() shouldBeEqualTo -1
            semaphore.tryAcquireAsync(owner, SemaphoreRequestId.from("after-close")).get() shouldBeEqualTo
                PermitAcquireResult.Closed
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `replay and stale generations preserve allocation contract`() {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-replay-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(5))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val semaphore = LettucePermitExpirableSemaphore.create(connection, name, config)
        try {
            semaphore.trySetPermits(3)
            val owner = SemaphoreOwnerId.from("replay-owner")
            val request = SemaphoreRequestId.from("replay-request")
            val first = semaphore.tryAcquire(owner, request, 2)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()
                .handle
            val replay = semaphore.tryAcquire(owner, request, 2)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()
                .handle
            replay shouldBeEqualTo first
            semaphore.availablePermits() shouldBeEqualTo 1

            val stale = first.copy(permit = first.permit.copy(generation = first.permit.generation + 1))
            semaphore.inspect(stale) shouldBeEqualTo PermitInspectResult.StaleGeneration
            semaphore.release(stale) shouldBeEqualTo PermitMutationResult.StaleGeneration
            semaphore.renew(stale, Duration.ofSeconds(1)) shouldBeEqualTo PermitRenewResult.StaleGeneration
            semaphore.reconcile(owner, request)
                .shouldBeInstanceOf<PermitReconcileResult.Owned<ExpirablePermitHandle>>()
            semaphore.release(first) shouldBeEqualTo PermitMutationResult.Released(first, 3)
            semaphore.tryAcquire(owner, request, 2) shouldBeEqualTo PermitAcquireResult.Unavailable
        } finally {
            semaphore.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }

    @Test
    fun `invalid bounds and closed adapters remain explicit`() = runTest {
        val connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        val name = "expirable-boundary-${randomName().substringAfter(':')}"
        val config = ExpirableSemaphoreConfig(leaseTime = Duration.ofSeconds(5))
        val keys = deriveSemaphoreKeys(name, config.semaphore, StringCodec.UTF8)
        connection.sync().del(*keys.all.toTypedArray())
        val future = LettucePermitExpirableSemaphore.create(connection, name, config)
        val suspending = LettuceSuspendPermitExpirableSemaphore.create(connection, name, config)
        try {
            future.trySetPermits(1)
            val owner = SemaphoreOwnerId.from("boundary-owner")
            val request = SemaphoreRequestId.from("boundary-request")
            assertFailsWith<IllegalArgumentException> { future.tryAcquire(owner, request, 0) }
            assertFailsWith<IllegalArgumentException> {
                future.acquire(owner, request, 1, Duration.ZERO)
            }
            assertFailsWith<IllegalArgumentException> {
                future.acquire(owner, request, 1, Duration.ofHours(24).plusMillis(1))
            }

            val held = future.tryAcquire(owner, request, 1)
                .shouldBeInstanceOf<PermitAcquireResult.Acquired<ExpirablePermitHandle>>()
                .handle
            assertFailsWith<IllegalArgumentException> {
                future.renew(held, Duration.ofMillis(99))
            }
            future.acquire(
                SemaphoreOwnerId.from("sync-timeout"),
                SemaphoreRequestId.from("sync-timeout"),
                1,
                Duration.ofMillis(35),
            ) shouldBeEqualTo PermitAcquireResult.TimedOut
            future.acquireAsync(
                SemaphoreOwnerId.from("async-timeout"),
                SemaphoreRequestId.from("async-timeout"),
                1,
                Duration.ofMillis(35),
            ).get() shouldBeEqualTo PermitAcquireResult.TimedOut
            suspending.acquire(
                SemaphoreOwnerId.from("suspend-timeout"),
                SemaphoreRequestId.from("suspend-timeout"),
                1,
                Duration.ofMillis(35),
            ) shouldBeEqualTo PermitAcquireResult.TimedOut

            future.close()
            future.availablePermitsAsync().get() shouldBeEqualTo -1
            future.inspectAsync(held).get() shouldBeEqualTo PermitInspectResult.Closed
            future.releaseAsync(held).get() shouldBeEqualTo PermitMutationResult.Closed
            future.renewAsync(held, Duration.ofSeconds(1)).get() shouldBeEqualTo PermitRenewResult.Closed
            future.reconcileAsync(owner, request).get() shouldBeEqualTo PermitReconcileResult.Closed
        } finally {
            future.close()
            suspending.close()
            connection.sync().del(*keys.all.toTypedArray())
            connection.close()
        }
    }
}
