package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.FencedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal class FencedLockScriptTest : AbstractLettuceTest() {

    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var commands: RedisCommands<String, String>
    private lateinit var lock: LettuceFencedLock
    private lateinit var keys: FencedLockKeys
    private lateinit var lockName: String

    private val owner = LockOwnerId.from("fenced-script-owner")
    private val lease = LeasePolicy.Fixed(Duration.ofSeconds(3))

    @BeforeEach
    fun setUp() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        commands = connection.sync()
        val name = "fenced-script-${randomName().substringAfter(':')}"
        lockName = name
        val config = FencedLockConfig(epoch = 7)
        keys = deriveFencedLockKeys(name, config, StringCodec.UTF8)
        deleteKeys()
        lock = LettuceFencedLock.create(connection, name, config)
    }

    @AfterEach
    fun tearDown() {
        try {
            lock.close()
            deleteKeys()
        } finally {
            connection.close()
        }
    }

    @Test
    fun `explicit bootstrap initializes the counter before acquisition can mint a token`() {
        lock.tryAcquire(owner, LockRequestId.from("missing-counter"), lease)
            .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
            .failure.kind shouldBeEqualTo LockIntegrityFailureKind.COUNTER_REGRESSION
        commands.exists(keys.state, keys.holds).shouldBeZero()

        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val first = lock.tryAcquire(owner, LockRequestId.from("first"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
            .handle

        first.epoch shouldBeEqualTo 7L
        first.fencingToken shouldBeEqualTo 1L
        commands.get(keys.counter) shouldBeEqualTo "1"
    }

    @Test
    fun `reentry keeps the original token and takeover mints a strictly newer token`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val first = lock.tryAcquire(owner, LockRequestId.from("first"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        val reentered = lock.tryAcquire(owner, LockRequestId.from("reentered"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Reentered<FencedLockHandle>>().handle
        reentered.fencingToken shouldBeEqualTo first.fencingToken

        lock.release(reentered) shouldBeEqualTo LockMutationResult.Released(1)
        lock.release(first) shouldBeEqualTo LockMutationResult.Released(0)
        val takeover = lock.tryAcquire(
            LockOwnerId.from("takeover-owner"),
            LockRequestId.from("takeover"),
            lease,
        ).shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle

        (takeover.fencingToken > first.fencingToken) shouldBeEqualTo true
    }

    @Test
    fun `expired and stale handles cannot release a newer generation`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val first = lock.tryAcquire(owner, LockRequestId.from("first"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle

        commands.del(keys.state, keys.holds)
        lock.release(first) shouldBeEqualTo LockMutationResult.Expired

        val takeover = lock.tryAcquire(
            LockOwnerId.from("takeover-owner"),
            LockRequestId.from("takeover"),
            lease,
        ).shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        lock.release(first) shouldBeEqualTo LockMutationResult.StaleGeneration
        lock.inspect(takeover).shouldBeInstanceOf<LockInspectResult.Owned<FencedLockHandle>>()
    }

    @Test
    fun `same generation token mismatch loses ownership without mutation`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val active = lock.tryAcquire(owner, LockRequestId.from("active"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        val forged = active.copy(fencingToken = active.fencingToken + 1)

        lock.release(forged) shouldBeEqualTo LockMutationResult.OwnershipLost
        lock.inspect(active).shouldBeInstanceOf<LockInspectResult.Owned<FencedLockHandle>>()
    }

    @Test
    fun `counter regression behind active token fails closed`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val active = lock.tryAcquire(owner, LockRequestId.from("active"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        commands.set(keys.counter, "0")

        lock.inspect(active)
            .shouldBeInstanceOf<LockInspectResult.IntegrityFailure>()
            .failure.kind shouldBeEqualTo LockIntegrityFailureKind.COUNTER_REGRESSION
    }

    @Test
    fun `counter regression behind released terminal token cannot mint a duplicate`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val released = lock.tryAcquire(owner, LockRequestId.from("released"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        lock.release(released) shouldBeEqualTo LockMutationResult.Released(0)
        commands.set(keys.counter, "0")

        lock.tryAcquire(
            LockOwnerId.from("next-owner"),
            LockRequestId.from("next-request"),
            lease,
        ).shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
            .failure.kind shouldBeEqualTo LockIntegrityFailureKind.COUNTER_REGRESSION
        commands.exists(keys.state, keys.holds).shouldBeZero()
    }

    @Test
    fun `malformed released terminal evidence returns integrity failure`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val released = lock.tryAcquire(owner, LockRequestId.from("released"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
        lock.release(released) shouldBeEqualTo LockMutationResult.Released(0)
        commands.hdel(keys.terminal, "generation")

        lock.bootstrapFencing()
            .shouldBeInstanceOf<FencedBootstrapResult.IntegrityFailure>()
            .failure.kind shouldBeEqualTo LockIntegrityFailureKind.INVALID_STATE
        lock.tryAcquire(
            LockOwnerId.from("next-owner"),
            LockRequestId.from("next-request"),
            lease,
        ).shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
            .failure.kind shouldBeEqualTo LockIntegrityFailureKind.INVALID_STATE
    }

    @Test
    fun `bootstrap renew reconcile and release expose replay-safe terminal outcomes`() {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.AlreadyInitialized

        val handle = lock.tryAcquire(owner, LockRequestId.from("lifecycle"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
            .handle
        lock.renew(handle, Duration.ofSeconds(2))
            .shouldBeInstanceOf<LockMutationResult.Renewed<FencedLockHandle>>()
        lock.inspect(handle).shouldBeInstanceOf<LockInspectResult.Owned<FencedLockHandle>>()
        lock.reconcile(owner, LockRequestId.from("lifecycle"))
            .shouldBeInstanceOf<LockReconcileResult.Owned<FencedLockHandle>>()
        lock.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
        lock.release(handle) shouldBeEqualTo LockMutationResult.AlreadyReleased
        lock.inspect(handle) shouldBeEqualTo LockInspectResult.Released
        lock.renew(handle, Duration.ofSeconds(1)) shouldBeEqualTo LockMutationResult.AlreadyReleased

        lock.bootstrapFencingAsync().get() shouldBeEqualTo FencedBootstrapResult.AlreadyInitialized
        lock.tryAcquireAsync(owner, LockRequestId.from("async"), lease).get()
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
            .handle.let(lock::release)
    }

    @Test
    fun `bounded acquire adapters time out and reject foreign handles`() = runSuspendIO {
        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
        val holder = lock.tryAcquire(owner, LockRequestId.from("bounded-holder"), lease)
            .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
            .handle

        val foreignName = "fenced-foreign-${randomName().substringAfter(':')}"
        val foreign = LettuceFencedLock.create(connection, foreignName, FencedLockConfig(epoch = 7))
        val foreignKeys = deriveFencedLockKeys(foreignName, FencedLockConfig(epoch = 7), connection.codec)
        try {
            foreign.bootstrapFencing()
            val foreignHandle = foreign.tryAcquire(
                LockOwnerId.from("foreign-owner"),
                LockRequestId.from("foreign-request"),
                lease,
            ).shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>().handle
            assertFailsWith<IllegalArgumentException> { lock.inspect(foreignHandle) }
            assertFailsWith<IllegalArgumentException> { lock.release(foreignHandle) }

            lock.acquire(
                LockOwnerId.from("sync-timeout"),
                LockRequestId.from("sync-timeout"),
                Duration.ofMillis(35),
                lease,
            ) shouldBeEqualTo LockAcquireResult.TimedOut
            lock.acquireAsync(
                LockOwnerId.from("async-timeout"),
                LockRequestId.from("async-timeout"),
                Duration.ofMillis(35),
                lease,
            ).get(2, java.util.concurrent.TimeUnit.SECONDS) shouldBeEqualTo LockAcquireResult.TimedOut

            val suspendLock = LettuceSuspendFencedLock.create(
                connection,
                lockName,
                FencedLockConfig(epoch = 7),
            )
            try {
                suspendLock.bootstrapFencing()
                suspendLock.acquire(
                    LockOwnerId.from("suspend-timeout"),
                    LockRequestId.from("suspend-timeout"),
                    Duration.ofMillis(35),
                    lease,
                ) shouldBeEqualTo LockAcquireResult.TimedOut
            } finally {
                suspendLock.close()
            }

            assertFailsWith<IllegalArgumentException> {
                lock.acquire(
                    LockOwnerId.from("too-long"),
                    LockRequestId.from("too-long"),
                    Duration.ofHours(24).plusMillis(1),
                    lease,
                )
            }
            assertFailsWith<IllegalArgumentException> { lock.inspect(holder.copy(epoch = 8)) }
            foreign.release(foreignHandle) shouldBeEqualTo LockMutationResult.Released(0)
        } finally {
            connection.sync().del(*foreignKeys.all)
            foreign.close()
        }

        lock.release(holder) shouldBeEqualTo LockMutationResult.Released(0)
    }

    private fun deleteKeys() {
        commands.del(*keys.all)
    }
}
