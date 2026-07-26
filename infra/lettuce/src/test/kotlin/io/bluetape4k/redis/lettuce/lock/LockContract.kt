package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal interface DistributedLockAdapter {
    suspend fun tryAcquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle>

    suspend fun acquire(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
        waitTime: Duration,
        leasePolicy: LeasePolicy,
    ): LockAcquireResult<LockHandle>

    suspend fun inspect(handle: LockHandle): LockInspectResult<LockHandle>

    suspend fun reconcile(
        ownerId: LockOwnerId,
        requestId: LockRequestId,
    ): LockReconcileResult<LockHandle>

    suspend fun renew(
        handle: LockHandle,
        extension: Duration,
    ): LockMutationResult<LockHandle>

    suspend fun release(handle: LockHandle): LockMutationResult<LockHandle>

    fun close()
}

internal abstract class LockContract : AbstractLettuceTest() {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var commands: RedisCommands<String, String>
    private lateinit var adapter: DistributedLockAdapter
    private lateinit var keys: DistributedLockKeys

    protected abstract fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        name: String,
        config: LockConfig,
    ): DistributedLockAdapter

    private val owner = LockOwnerId.from("contract-owner")
    private val contender = LockOwnerId.from("contract-contender")
    private val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

    @BeforeEach
    fun setUpContract() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        commands = connection.sync()
        val name = "lock-${randomName().substringAfter(':')}"
        val config = LockConfig()
        keys = deriveDistributedLockKeys(name, config, StringCodec.UTF8)
        deleteKeys()
        adapter = createAdapter(connection, name, config)
    }

    @AfterEach
    fun tearDownContract() {
        try {
            adapter.close()
            deleteKeys()
        } finally {
            connection.close()
        }
    }

    @Test
    fun `request replay and owner reentry preserve one generation`() = runSuspendIO {
        val outerRequest = LockRequestId.from("outer-request")
        val first = adapter.tryAcquire(owner, outerRequest, lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        val replay = adapter.tryAcquire(owner, outerRequest, lease)
        val inner = adapter.tryAcquire(owner, LockRequestId.from("inner-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
        val lateOuterReplay = adapter.tryAcquire(owner, outerRequest, lease)

        replay.shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle shouldBeEqualTo first.handle
        lateOuterReplay.shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle shouldBeEqualTo first.handle
        inner.handle.generation shouldBeEqualTo first.handle.generation
        inner.holdCount shouldBeEqualTo 2
        adapter.inspect(inner.handle)
            .shouldBeInstanceOf<LockInspectResult.Owned<LockHandle>>()
            .holdCount shouldBeEqualTo 2
        adapter.reconcile(owner, outerRequest)
            .shouldBeInstanceOf<LockReconcileResult.Owned<LockHandle>>()
            .handle shouldBeEqualTo first.handle
    }

    @Test
    fun `contention is immediate and bounded acquisition times out`() = runSuspendIO {
        adapter.tryAcquire(owner, LockRequestId.from("owner-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()

        adapter.tryAcquire(contender, LockRequestId.from("contender-now"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Contended>()
            .remainingTtlMillis shouldBeGreaterThan 0L
        adapter.acquire(
            contender,
            LockRequestId.from("contender-wait"),
            Duration.ofMillis(100),
            lease,
        ) shouldBeEqualTo LockAcquireResult.TimedOut
    }

    @Test
    fun `invalid wait time fails before acquisition dispatch`() = runSuspendIO {
        val zeroRequestId = LockRequestId.from("zero-wait-request")

        assertFailsWith<IllegalArgumentException> {
            adapter.acquire(owner, zeroRequestId, Duration.ZERO, lease)
        }
        adapter.reconcile(owner, zeroRequestId) shouldBeEqualTo LockReconcileResult.NotFound

        val overMaximumRequestId = LockRequestId.from("over-maximum-wait-request")
        assertFailsWith<IllegalArgumentException> {
            adapter.acquire(
                owner,
                overMaximumRequestId,
                Duration.ofHours(24).plusNanos(1),
                lease,
            )
        }
        adapter.reconcile(owner, overMaximumRequestId) shouldBeEqualTo LockReconcileResult.NotFound
    }

    @Test
    fun `each request-bound handle releases once in caller order`() = runSuspendIO {
        val outer = adapter.tryAcquire(owner, LockRequestId.from("outer-release"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        val inner = adapter.tryAcquire(owner, LockRequestId.from("inner-release"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
            .handle

        adapter.release(inner) shouldBeEqualTo LockMutationResult.Released(1)
        adapter.release(inner) shouldBeEqualTo LockMutationResult.AlreadyReleased
        adapter.inspect(outer)
            .shouldBeInstanceOf<LockInspectResult.Owned<LockHandle>>()
            .holdCount shouldBeEqualTo 1
        adapter.release(outer) shouldBeEqualTo LockMutationResult.Released(0)
        adapter.release(outer) shouldBeEqualTo LockMutationResult.AlreadyReleased
        adapter.inspect(outer) shouldBeEqualTo LockInspectResult.Released
    }

    @Test
    fun `releasing outer twice never consumes the inner hold`() = runSuspendIO {
        val outer = adapter.tryAcquire(owner, LockRequestId.from("outer-first"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        val inner = adapter.tryAcquire(owner, LockRequestId.from("inner-second"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<LockHandle>>()
            .handle

        adapter.release(outer) shouldBeEqualTo LockMutationResult.Released(1)
        adapter.release(outer) shouldBeEqualTo LockMutationResult.AlreadyReleased
        adapter.inspect(inner)
            .shouldBeInstanceOf<LockInspectResult.Owned<LockHandle>>()
            .holdCount shouldBeEqualTo 1
        adapter.release(inner) shouldBeEqualTo LockMutationResult.Released(0)
    }

    @Test
    fun `missing terminal evidence distinguishes expiry from release replay`() = runSuspendIO {
        val handle = adapter.tryAcquire(owner, LockRequestId.from("terminal-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle

        adapter.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
        adapter.release(handle) shouldBeEqualTo LockMutationResult.AlreadyReleased
        commands.del(keys.terminal)
        adapter.release(handle) shouldBeEqualTo LockMutationResult.Expired
    }

    @Test
    fun `renew replaces the ttl instead of cumulatively adding it`() = runSuspendIO {
        val handle = adapter.tryAcquire(owner, LockRequestId.from("renew-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle
        val extension = Duration.ofMillis(700)

        adapter.renew(handle, extension)
            .shouldBeInstanceOf<LockMutationResult.Renewed<LockHandle>>()
        val beforeReplay = withTimeout(Duration.ofSeconds(2).toMillis()) {
            var remainingTtl = commands.pttl(keys.state)
            while (remainingTtl > extension.toMillis() - RENEW_REPLAY_DECAY_MILLIS) {
                delay(20)
                remainingTtl = commands.pttl(keys.state)
            }
            remainingTtl
        }
        adapter.renew(handle, extension)
            .shouldBeInstanceOf<LockMutationResult.Renewed<LockHandle>>()
        val afterReplay = commands.pttl(keys.state)

        afterReplay shouldBeGreaterOrEqualTo beforeReplay
        afterReplay shouldBeLessOrEqualTo extension.toMillis()
    }

    @Test
    fun `expiry permits takeover only with a greater generation`() = runSuspendIO {
        val shortLease = LeasePolicy.Fixed(Duration.ofMillis(100))
        val expired = adapter.tryAcquire(owner, LockRequestId.from("expiring-request"), shortLease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle

        withTimeout(Duration.ofSeconds(5).toMillis()) {
            while (adapter.inspect(expired) != LockInspectResult.Expired) {
                delay(20)
            }
        }
        val replacement = adapter.tryAcquire(contender, LockRequestId.from("takeover-request"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
            .handle

        replacement.generation shouldBeGreaterThan expired.generation
        adapter.release(expired) shouldBeEqualTo LockMutationResult.StaleGeneration
    }

    private fun deleteKeys() {
        commands.del(keys.state, keys.generation, keys.holds, keys.terminal)
    }
}

private const val RENEW_REPLAY_DECAY_MILLIS = 200L
