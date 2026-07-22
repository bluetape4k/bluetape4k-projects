package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBePositive
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration

internal interface FencingLeaseAdapter {
    suspend fun bootstrap(): FencingBootstrapResult
    suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult
    suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult
    suspend fun renew(ownerId: FencingOwnerId, token: FencingToken, leaseTime: Duration): FencingRenewResult
    suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult
}

internal abstract class FencingLeaseContract : AbstractLettuceTest() {

    private lateinit var config: LettuceFencingLeaseConfig
    private lateinit var keys: FencingLeaseKeys
    private lateinit var adapter: FencingLeaseAdapter
    private lateinit var connection: StatefulRedisConnection<String, String>
    private lateinit var commands: RedisCommands<String, String>

    private val owner = FencingOwnerId.from("contract-owner")
    private val contender = FencingOwnerId.from("contract-contender")

    protected abstract fun createAdapter(
        connection: StatefulRedisConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): FencingLeaseAdapter

    @BeforeEach
    fun setUpContract() {
        connection = LettuceTestUtils.client.connect(StringCodec.UTF8)
        commands = connection.sync()
        config = LettuceFencingLeaseConfig("contract", "lease-${randomName().substringAfter(':')}", 11)
        keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        commands.del(keys.lease, keys.counter)
        adapter = createAdapter(connection, config)
    }

    @AfterEach
    fun tearDownContract() {
        try {
            commands.del(keys.lease, keys.counter)
        } finally {
            connection.close()
        }
    }

    @Test
    fun `bootstrap replay and sequential generations are consistent`() = runSuspendIO {
        adapter.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
        adapter.bootstrap() shouldBeEqualTo FencingBootstrapResult.AlreadyInitialized

        val first = adapter.acquire(owner, DEFAULT_LEASE).shouldBeInstanceOf<FencingAcquireResult.Acquired>().token
        adapter.release(owner, first) shouldBeEqualTo FencingReleaseResult.Released
        val second = adapter.acquire(contender, DEFAULT_LEASE)
            .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token

        second shouldBeGreaterThan first
        commands.get(keys.counter) shouldBeEqualTo second.sequence.toString()
    }

    @Test
    fun `same owner replay keeps token and does not extend ttl`() = runSuspendIO {
        adapter.bootstrap()
        val acquired = adapter.acquire(owner, DEFAULT_LEASE).shouldBeInstanceOf<FencingAcquireResult.Acquired>()
        val before = commands.pttl(keys.lease)

        val replay = adapter.acquire(owner, LONGER_LEASE).shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
        val after = commands.pttl(keys.lease)

        replay.token shouldBeEqualTo acquired.token
        replay.remainingTtlMillis.shouldBePositive()
        replay.remainingTtlMillis shouldBeLessOrEqualTo before
        after shouldBeLessOrEqualTo before
    }

    @Test
    fun `inspect renew and release reject stale ownership without changing newer lease`() = runSuspendIO {
        adapter.bootstrap()
        val token = adapter.acquire(owner, DEFAULT_LEASE).shouldBeInstanceOf<FencingAcquireResult.Acquired>().token

        adapter.inspect(owner).shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo token
        adapter.inspect(contender).shouldBeInstanceOf<FencingInspectResult.Contended>()
        adapter.renew(contender, token, LONGER_LEASE) shouldBeEqualTo FencingRenewResult.OwnershipMismatch
        adapter.renew(owner, FencingToken(token.epoch, token.sequence + 1), LONGER_LEASE) shouldBeEqualTo
            FencingRenewResult.OwnershipMismatch
        adapter.release(contender, token) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
        adapter.release(owner, FencingToken(token.epoch, token.sequence + 1)) shouldBeEqualTo
            FencingReleaseResult.OwnershipMismatch

        adapter.inspect(owner).shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo token
        adapter.renew(owner, token, LONGER_LEASE) shouldBeEqualTo FencingRenewResult.Renewed
        commands.get(keys.counter) shouldBeEqualTo token.sequence.toString()
        adapter.release(owner, token) shouldBeEqualTo FencingReleaseResult.Released
        commands.get(keys.counter) shouldBeEqualTo token.sequence.toString()
        adapter.inspect(owner) shouldBeEqualTo FencingInspectResult.Lost
    }

    @Test
    fun `expiry permits takeover only with a greater token`() = runSuspendIO {
        adapter.bootstrap()
        val first = adapter.acquire(owner, Duration.ofMillis(100))
            .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token

        withTimeout(Duration.ofSeconds(5).toMillis()) {
            while (adapter.inspect(owner) != FencingInspectResult.Lost) {
                delay(20)
            }
        }
        val second = adapter.acquire(contender, DEFAULT_LEASE)
            .shouldBeInstanceOf<FencingAcquireResult.Acquired>().token

        second shouldBeGreaterThan first
    }

    @Test
    fun `cross epoch and invalid ttl fail before Redis dispatch`() = runSuspendIO {
        adapter.bootstrap()
        val before = commands.get(keys.counter)
        val otherEpoch = FencingToken(12, 1)

        assertFailsWith<IllegalArgumentException> {
            adapter.acquire(owner, Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> {
            adapter.acquire(owner, Duration.ofNanos(1))
        }
        assertFailsWith<IllegalArgumentException> {
            adapter.acquire(owner, Duration.ofMillis(MAX_EXACT_REDIS_LEASE_TIME_MILLIS + 1))
        }
        assertFailsWith<IllegalArgumentException> {
            adapter.renew(owner, otherEpoch, DEFAULT_LEASE)
        }
        assertFailsWith<IllegalArgumentException> {
            adapter.release(owner, otherEpoch)
        }

        commands.get(keys.counter) shouldBeEqualTo before
        commands.exists(keys.lease) shouldBeEqualTo 0L
    }

    private companion object {
        val DEFAULT_LEASE: Duration = Duration.ofSeconds(5)
        val LONGER_LEASE: Duration = Duration.ofSeconds(30)
    }
}
