package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test

class LettuceDistributedSemaphoreTest: AbstractLettuceTest() {

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
}
