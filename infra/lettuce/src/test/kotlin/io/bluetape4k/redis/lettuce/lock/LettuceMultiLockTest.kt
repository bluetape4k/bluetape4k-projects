package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

internal class LettuceMultiLockTest {

    @Test
    fun `all keys acquire reenter renew and release under one generation`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceMultiLock.create(connection, NAMES, config("lifecycle"))
            try {
                val first = lock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                val second = lock.tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Reentered<MultiLockHandle>>()
                    .handle
                first.lock.generation shouldBeEqualTo second.lock.generation
                first.constituentCount shouldBeEqualTo NAMES.size

                lock.renew(second, Duration.ofSeconds(4))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<MultiLockHandle>>()
                lock.release(second) shouldBeEqualTo LockMutationResult.Released(1)
                lock.release(first) shouldBeEqualTo LockMutationResult.Released(0)
                lock.release(first) shouldBeEqualTo LockMutationResult.AlreadyReleased
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `conflict is atomic timeout is bounded and expiry permits takeover`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceMultiLock.create(connection, NAMES, config("contention"))
            try {
                val first = lock.tryAcquire(OWNER_1, REQUEST_1, LeasePolicy.Fixed(Duration.ofMillis(150)))
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                lock.acquire(OWNER_2, REQUEST_2, Duration.ofMillis(35), LEASE) shouldBeEqualTo
                    LockAcquireResult.TimedOut

                Thread.sleep(180)
                val takeover = lock.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                takeover.lock.generation.value shouldBeEqualTo first.lock.generation.value + 1L
                lock.release(takeover) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `future and suspend views preserve request bound multi handles`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val future = LettuceMultiLock.create(connection, NAMES, config("future"))
            val suspending = LettuceSuspendMultiLock.create(connection, NAMES, config("suspend"))
            try {
                val futureHandle = future.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                future.releaseAsync(futureHandle).await() shouldBeEqualTo LockMutationResult.Released(0)

                val suspendHandle = suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                suspending.release(suspendHandle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                future.close()
                suspending.close()
            }
        }
    }

    @Test
    fun `future and suspend lifecycle views preserve multi-lock ownership`() = runSuspendIO {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val config = config("parity")
            val future = LettuceMultiLock.create(connection, NAMES, config)
            val suspending = LettuceSuspendMultiLock.create(connection, NAMES, config)
            try {
                val futureHandle = future.tryAcquireAsync(OWNER_1, REQUEST_1, LEASE).await()
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                future.inspectAsync(futureHandle).await()
                    .shouldBeInstanceOf<LockInspectResult.Owned<MultiLockHandle>>()
                future.reconcileAsync(OWNER_1, REQUEST_1).await()
                    .shouldBeInstanceOf<LockReconcileResult.Owned<MultiLockHandle>>()
                future.renewAsync(futureHandle, Duration.ofSeconds(1)).await()
                    .shouldBeInstanceOf<LockMutationResult.Renewed<MultiLockHandle>>()
                future.releaseAsync(futureHandle).await() shouldBeEqualTo LockMutationResult.Released(0)

                val suspendHandle = suspending.tryAcquire(OWNER_2, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                suspending.inspect(suspendHandle)
                    .shouldBeInstanceOf<LockInspectResult.Owned<MultiLockHandle>>()
                suspending.reconcile(OWNER_2, REQUEST_2)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<MultiLockHandle>>()
                suspending.renew(suspendHandle, Duration.ofSeconds(1))
                    .shouldBeInstanceOf<LockMutationResult.Renewed<MultiLockHandle>>()
                suspending.release(suspendHandle) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                future.close()
                suspending.close()
            }
        }
    }

    @Test
    fun `reconcile replays the request bound multi handle`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceMultiLock.create(connection, NAMES, config("reconcile"))
            try {
                val acquired = lock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle
                val reconciled = lock.reconcile(OWNER_1, REQUEST_1)
                    .shouldBeInstanceOf<LockReconcileResult.Owned<MultiLockHandle>>()
                    .handle

                reconciled shouldBeEqualTo acquired
                lock.release(acquired) shouldBeEqualTo LockMutationResult.Released(0)
            } finally {
                lock.close()
            }
        }
    }

    @Test
    fun `watchdog renews every constituent and close completes pending acquisition`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val lock = LettuceMultiLock.create(connection, NAMES, config("watchdog"))
            val watchdog = LeasePolicy.Watchdog(
                ttl = Duration.ofSeconds(3),
                renewalInterval = Duration.ofMillis(900),
                maxLifetime = Duration.ofSeconds(10),
            )
            val handle = lock.tryAcquire(OWNER_1, REQUEST_1, watchdog)
                .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                .handle
            Thread.sleep(3_200)
            lock.inspect(handle).shouldBeInstanceOf<LockInspectResult.Owned<MultiLockHandle>>()

            val pending = lock.acquireAsync(OWNER_2, REQUEST_2, Duration.ofSeconds(5), LEASE)
            lock.close()
            pending.get(1, TimeUnit.SECONDS) shouldBeEqualTo LockAcquireResult.Closed
        }
    }

    private fun config(suffix: String): MultiLockConfig =
        MultiLockConfig(lock = LockConfig(hashTag = "multi-$suffix-${System.nanoTime()}"))

    private companion object {
        val NAMES = listOf("account", "inventory")
        val OWNER_1 = LockOwnerId.from("multi-owner-1")
        val OWNER_2 = LockOwnerId.from("multi-owner-2")
        val REQUEST_1 = LockRequestId.from("multi-request-1")
        val REQUEST_2 = LockRequestId.from("multi-request-2")
        val LEASE = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}
