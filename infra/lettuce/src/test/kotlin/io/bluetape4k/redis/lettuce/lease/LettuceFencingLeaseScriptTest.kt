package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBePositive
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceFencingLeaseScriptTest : AbstractLettuceTest() {

    private lateinit var commands: RedisCommands<String, String>
    private lateinit var config: LettuceFencingLeaseConfig
    private lateinit var keys: FencingLeaseKeys

    private val owner = FencingOwnerId.from("owner-primary")
    private val contender = FencingOwnerId.from("owner-contender")

    @BeforeEach
    fun setUp() {
        commands = connection.sync()
        config = LettuceFencingLeaseConfig("test", "lease-${randomName().substringAfter(':')}", 7)
        keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        commands.del(keys.lease, keys.counter)
    }

    @AfterEach
    fun tearDown() {
        commands.del(keys.lease, keys.counter)
    }

    @Test
    fun `bootstrap acquire inspect renew and release preserve one ordered state machine`() {
        bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
        bootstrap() shouldBeEqualTo FencingBootstrapResult.AlreadyInitialized

        val acquired = acquire(owner).shouldBeInstanceOf<FencingAcquireResult.Acquired>()
        acquired.token shouldBeEqualTo FencingToken(7, 1)

        val beforeReplayTtl = commands.pttl(keys.lease)
        val replay = acquire(owner).shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
        val afterReplayTtl = commands.pttl(keys.lease)
        replay.token shouldBeEqualTo acquired.token
        replay.remainingTtlMillis.shouldBePositive()
        replay.remainingTtlMillis shouldBeLessOrEqualTo beforeReplayTtl
        afterReplayTtl shouldBeInRange (beforeReplayTtl - TTL_TOLERANCE_MILLIS)..beforeReplayTtl

        val owned = inspect(owner).shouldBeInstanceOf<FencingInspectResult.Owned>()
        owned.token shouldBeEqualTo acquired.token
        owned.remainingTtlMillis.shouldBePositive()
        inspect(contender).shouldBeInstanceOf<FencingInspectResult.Contended>()

        renew(contender, acquired.token) shouldBeEqualTo FencingRenewResult.OwnershipMismatch
        renew(owner, FencingToken(7, 2)) shouldBeEqualTo FencingRenewResult.OwnershipMismatch
        renew(owner, acquired.token) shouldBeEqualTo FencingRenewResult.Renewed

        release(contender, acquired.token) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
        release(owner, FencingToken(7, 2)) shouldBeEqualTo FencingReleaseResult.OwnershipMismatch
        release(owner, acquired.token) shouldBeEqualTo FencingReleaseResult.Released
        inspect(owner) shouldBeEqualTo FencingInspectResult.Lost

        val reacquired = acquire(contender).shouldBeInstanceOf<FencingAcquireResult.Acquired>()
        reacquired.token shouldBeEqualTo FencingToken(7, 2)
        commands.get(keys.counter) shouldBeEqualTo "2"
    }

    @Test
    fun `absent and exhausted counter branches do not create a lease`() {
        acquire(owner) shouldBeEqualTo FencingAcquireResult.CounterUnavailable
        inspect(owner) shouldBeEqualTo FencingInspectResult.Lost
        renew(owner, FencingToken(7, 1)) shouldBeEqualTo FencingRenewResult.Lost
        release(owner, FencingToken(7, 1)) shouldBeEqualTo FencingReleaseResult.Lost

        commands.set(keys.counter, Long.MAX_VALUE.toString())
        acquire(owner) shouldBeEqualTo FencingAcquireResult.SequenceExhausted
        commands.exists(keys.lease).shouldBeZero()
        commands.get(keys.counter) shouldBeEqualTo Long.MAX_VALUE.toString()
    }

    @Test
    fun `lease structural corruption is malformed and remains unchanged`() {
        val corruptions = listOf<() -> Unit>(
            { commands.set(keys.counter, "1"); commands.set(keys.lease, "wrong-type") },
            { commands.set(keys.counter, "1"); writeLease(fields = mapOf("owner" to owner.value, "epoch" to "7")) },
            {
                commands.set(keys.counter, "1")
                writeLease(
                    fields = validLeaseFields() + ("extra" to "field"),
                )
            },
            { commands.set(keys.counter, "1"); writeLease(ownerText = "x".repeat(257)) },
            { commands.set(keys.counter, "1"); writeLease(epoch = "1".repeat(20)) },
            { commands.set(keys.counter, "1"); writeLease(sequence = "1".repeat(20)) },
        )

        corruptions.forEach { corrupt ->
            commands.del(keys.lease, keys.counter)
            corrupt()
            assertIntegrityWithoutMutation(FencingIntegrityFailureKind.MALFORMED_LEASE)
        }
    }

    @Test
    fun `lease decimals reject signs leading zero whitespace non-digits and range overflow`() {
        val invalidDecimals = listOf("+7", "-7", "07", " 7", "7 ", "7x", "9223372036854775808")

        invalidDecimals.forEach { invalid ->
            commands.del(keys.lease, keys.counter)
            commands.set(keys.counter, "1")
            writeLease(epoch = invalid)
            assertIntegrityWithoutMutation(FencingIntegrityFailureKind.MALFORMED_LEASE)

            commands.del(keys.lease, keys.counter)
            commands.set(keys.counter, "1")
            writeLease(sequence = invalid)
            assertIntegrityWithoutMutation(FencingIntegrityFailureKind.MALFORMED_LEASE)
        }
    }

    @Test
    fun `counter corruption is invalid and remains unchanged`() {
        val corruptions = buildList<() -> Unit> {
            add { commands.lpush(keys.counter, "wrong-type") }
            listOf("+1", "-1", "01", " 1", "1 ", "1x", "9223372036854775808", "1".repeat(20)).forEach { invalid ->
                add { commands.set(keys.counter, invalid) }
            }
            add { commands.set(keys.counter, "1"); commands.pexpire(keys.counter, LEASE_MILLIS) }
        }

        corruptions.forEach { corrupt ->
            commands.del(keys.lease, keys.counter)
            corrupt()
            val beforeBootstrap = redisState()
            bootstrap() shouldBeEqualTo
                FencingBootstrapResult.IntegrityFailure(
                    FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.INVALID_COUNTER),
                )
            val afterBootstrap = redisState()
            afterBootstrap.counter.type shouldBeEqualTo beforeBootstrap.counter.type
            afterBootstrap.counter.value shouldBeEqualTo beforeBootstrap.counter.value
            afterBootstrap.counter.list shouldBeEqualTo beforeBootstrap.counter.list
            assertTtlNotMutated(beforeBootstrap.counter.ttlMillis, afterBootstrap.counter.ttlMillis)
            assertIntegrityWithoutMutation(FencingIntegrityFailureKind.INVALID_COUNTER)
        }
    }

    @Test
    fun `active lease rejects missing or behind counter without mutation`() {
        writeLease(sequence = "2")
        assertIntegrityWithoutMutation(FencingIntegrityFailureKind.INVALID_COUNTER)

        commands.set(keys.counter, "1")
        assertIntegrityWithoutMutation(FencingIntegrityFailureKind.COUNTER_BEHIND_LEASE)
    }

    @Test
    fun `active counter corruption fails closed across every operation`() {
        writeLease()
        val expected = FencingLeaseIntegrityFailure(FencingIntegrityFailureKind.INVALID_COUNTER)
        val token = FencingToken(7, 1)
        val before = redisState()

        bootstrap() shouldBeEqualTo FencingBootstrapResult.IntegrityFailure(expected)
        acquire(owner) shouldBeEqualTo FencingAcquireResult.IntegrityFailure(expected)
        inspect(owner) shouldBeEqualTo FencingInspectResult.IntegrityFailure(expected)
        renew(owner, token) shouldBeEqualTo FencingRenewResult.IntegrityFailure(expected)
        release(owner, token) shouldBeEqualTo FencingReleaseResult.IntegrityFailure(expected)

        val after = redisState()
        after.lease.type shouldBeEqualTo before.lease.type
        after.lease.hash shouldBeEqualTo before.lease.hash
        after.counter.type shouldBeEqualTo before.counter.type
        assertTtlNotMutated(before.lease.ttlMillis, after.lease.ttlMillis)
    }

    @Test
    fun `persistent active lease fails closed as malformed`() {
        commands.set(keys.counter, "1")
        writeLease()
        commands.persist(keys.lease)

        assertIntegrityWithoutMutation(FencingIntegrityFailureKind.MALFORMED_LEASE)
    }

    @Test
    fun `script flush uses source fallback without changing results`() {
        bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
        commands.scriptFlush()

        acquire(owner) shouldBeEqualTo FencingAcquireResult.Acquired(FencingToken(7, 1))
        inspect(owner).shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo FencingToken(7, 1)
    }

    @Test
    fun `maximum exact Redis lease time keeps canonical ttl replies`() {
        bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

        runFencingAcquire(
            commands,
            keys,
            config,
            owner,
            MAX_EXACT_REDIS_LEASE_TIME_MILLIS,
        ) shouldBeEqualTo FencingAcquireResult.Acquired(FencingToken(7, 1))

        val replay = runFencingAcquire(
            commands,
            keys,
            config,
            owner,
            MAX_EXACT_REDIS_LEASE_TIME_MILLIS,
        ).shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
        replay.remainingTtlMillis.shouldBePositive()
        inspect(owner).shouldBeInstanceOf<FencingInspectResult.Owned>().remainingTtlMillis.shouldBePositive()
    }

    @Test
    fun `operation wrapper dispatches evalsha once and eval only for noscript`() {
        val scripting = mockk<RedisScriptingCommands<String, String>>()
        val scriptKeys = arrayOf(keys.lease, keys.counter)
        val arguments = arrayOf(owner.value, config.epoch.toString(), LEASE_MILLIS.toString())
        val counterUnavailable = listOf("COUNTER_UNAVAILABLE", "0", "0", "-1")

        every {
            scripting.evalsha<List<String>>(
                FencingLeaseScripts.ACQUIRE.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
        } returns counterUnavailable

        runFencingAcquire(scripting, keys, config, owner, LEASE_MILLIS) shouldBeEqualTo
            FencingAcquireResult.CounterUnavailable

        verify(exactly = 1) {
            scripting.evalsha<List<String>>(
                FencingLeaseScripts.ACQUIRE.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
        }
        confirmVerified(scripting)

        val fallback = mockk<RedisScriptingCommands<String, String>>()
        every {
            fallback.evalsha<List<String>>(
                FencingLeaseScripts.ACQUIRE.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
        } throws RedisNoScriptException("NOSCRIPT")
        every {
            fallback.eval<List<String>>(
                FencingLeaseScripts.ACQUIRE.source,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
        } returns counterUnavailable

        runFencingAcquire(fallback, keys, config, owner, LEASE_MILLIS) shouldBeEqualTo
            FencingAcquireResult.CounterUnavailable

        verify(exactly = 1) {
            fallback.evalsha<List<String>>(
                FencingLeaseScripts.ACQUIRE.sha1,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
            fallback.eval<List<String>>(
                FencingLeaseScripts.ACQUIRE.source,
                ScriptOutputType.MULTI,
                scriptKeys,
                *arguments,
            )
        }
        confirmVerified(fallback)
    }

    @Test
    fun `operation wrapper rejects epoch and ttl validation before dispatch`() {
        val scripting = mockk<RedisScriptingCommands<String, String>>()

        assertFailsWith<IllegalArgumentException> {
            runFencingAcquire(scripting, keys, config, owner, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            runFencingAcquire(scripting, keys, config, owner, MAX_EXACT_REDIS_LEASE_TIME_MILLIS + 1)
        }
        assertFailsWith<IllegalArgumentException> {
            runFencingRenew(scripting, keys, config, owner, FencingToken(8, 1), LEASE_MILLIS)
        }

        confirmVerified(scripting)
    }

    @Test
    fun `runtime failure after increment leaves a safe sequence gap`() {
        bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

        assertFailsWith<RedisCommandExecutionException> {
            RedisScriptRunner.run<Any>(
                commands,
                failAfterIncrementScript,
                ScriptOutputType.MULTI,
                arrayOf(keys.lease, keys.counter),
            )
        }

        commands.get(keys.counter) shouldBeEqualTo "1"
        acquire(owner) shouldBeEqualTo FencingAcquireResult.Acquired(FencingToken(7, 2))
    }

    @Test
    fun `runtime failure after lease write leaves a partial lease that fails closed`() {
        bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

        assertFailsWith<RedisCommandExecutionException> {
            RedisScriptRunner.run<Any>(
                commands,
                failAfterLeaseWriteScript,
                ScriptOutputType.MULTI,
                arrayOf(keys.lease, keys.counter),
                owner.value,
                config.epoch.toString(),
                "1",
            )
        }

        commands.type(keys.lease) shouldBeEqualTo "hash"
        commands.pttl(keys.lease) shouldBeEqualTo -1L
        acquire(owner) shouldBeEqualTo integrityAcquire(FencingIntegrityFailureKind.MALFORMED_LEASE)
    }

    @Test
    fun `production scripts use bounded fixed-shape command inventories`() {
        val expectedMaximums = mapOf(
            FencingLeaseScripts.BOOTSTRAP to 12,
            FencingLeaseScripts.ACQUIRE to 15,
            FencingLeaseScripts.INSPECT to 11,
            FencingLeaseScripts.RENEW to 12,
            FencingLeaseScripts.RELEASE to 12,
        )

        expectedMaximums.forEach { (script, maximum) ->
            val source = script.source
            source shouldNotContain "redis.call('KEYS'"
            source shouldNotContain "redis.call('SCAN'"
            source shouldNotContain "redis.call('HGETALL'"
            source shouldNotContain "pairs("
            source shouldNotContain "ipairs("
            source shouldNotContain "tonumber("
            source shouldNotContain "error("
            source shouldContain "redis.call('HLEN', leaseKey) ~= 3"
            Regex("redis\\.call\\('[A-Z]+'").findAll(source).count() shouldBeEqualTo maximum
        }

        val acquire = FencingLeaseScripts.ACQUIRE.source
        val mutationOffsets = listOf(
            acquire.indexOf("redis.call('INCR'"),
            acquire.lastIndexOf("redis.call('GET'"),
            acquire.indexOf("redis.call('HSET'"),
            acquire.indexOf("redis.call('PEXPIRE'"),
        )
        mutationOffsets.forEach { offset -> offset shouldBeGreaterOrEqualTo 0 }
        mutationOffsets shouldBeEqualTo mutationOffsets.sorted()
    }

    private fun bootstrap(): FencingBootstrapResult = runFencingBootstrap(commands, keys, config)

    private fun acquire(targetOwner: FencingOwnerId): FencingAcquireResult =
        runFencingAcquire(commands, keys, config, targetOwner, LEASE_MILLIS)

    private fun inspect(targetOwner: FencingOwnerId): FencingInspectResult =
        runFencingInspect(commands, keys, config, targetOwner)

    private fun renew(targetOwner: FencingOwnerId, token: FencingToken): FencingRenewResult =
        runFencingRenew(commands, keys, config, targetOwner, token, LEASE_MILLIS)

    private fun release(targetOwner: FencingOwnerId, token: FencingToken): FencingReleaseResult =
        runFencingRelease(commands, keys, config, targetOwner, token)

    private fun writeLease(
        ownerText: String = owner.value,
        epoch: String = config.epoch.toString(),
        sequence: String = "1",
        fields: Map<String, String> = mapOf(
            "owner" to ownerText,
            "epoch" to epoch,
            "sequence" to sequence,
        ),
    ) {
        commands.hset(keys.lease, fields)
        commands.pexpire(keys.lease, LEASE_MILLIS)
    }

    private fun validLeaseFields(): Map<String, String> = mapOf(
        "owner" to owner.value,
        "epoch" to config.epoch.toString(),
        "sequence" to "1",
    )

    private fun assertIntegrityWithoutMutation(kind: FencingIntegrityFailureKind) {
        val before = redisState()
        acquire(owner) shouldBeEqualTo integrityAcquire(kind)
        val after = redisState()

        after.lease.type shouldBeEqualTo before.lease.type
        after.lease.value shouldBeEqualTo before.lease.value
        after.lease.hash shouldBeEqualTo before.lease.hash
        after.counter.type shouldBeEqualTo before.counter.type
        after.counter.value shouldBeEqualTo before.counter.value
        after.counter.list shouldBeEqualTo before.counter.list
        assertTtlNotMutated(before.lease.ttlMillis, after.lease.ttlMillis)
        assertTtlNotMutated(before.counter.ttlMillis, after.counter.ttlMillis)
    }

    private fun assertTtlNotMutated(before: Long, after: Long) {
        if (before >= 0) {
            after shouldBeInRange (before - TTL_TOLERANCE_MILLIS)..before
        } else {
            after shouldBeEqualTo before
        }
    }

    private fun redisState(): RedisState = RedisState(readKey(keys.lease), readKey(keys.counter))

    private fun readKey(key: String): RedisKeyState {
        val type = commands.type(key)
        return RedisKeyState(
            type = type,
            value = if (type == "string") commands.get(key) else null,
            hash = if (type == "hash") commands.hgetall(key).toSortedMap() else emptyMap(),
            list = if (type == "list") commands.lrange(key, 0, -1) else emptyList(),
            ttlMillis = commands.pttl(key),
        )
    }

    private data class RedisState(
        val lease: RedisKeyState,
        val counter: RedisKeyState,
    )

    private data class RedisKeyState(
        val type: String,
        val value: String?,
        val hash: Map<String, String>,
        val list: List<String>,
        val ttlMillis: Long,
    )

    private companion object {
        const val LEASE_MILLIS = 30_000L
        const val TTL_TOLERANCE_MILLIS = 2_000L

        val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }

        val failAfterIncrementScript = RedisScript(
            """
            redis.call('INCR', KEYS[2])
            redis.call('LPUSH', KEYS[2], 'force-wrong-type')
            return {'UNREACHABLE', '0', '0', '-1'}
            """.trimIndent(),
        )

        val failAfterLeaseWriteScript = RedisScript(
            """
            redis.call('HSET', KEYS[1],
              'owner', ARGV[1],
              'epoch', ARGV[2],
              'sequence', ARGV[3])
            redis.call('LPUSH', KEYS[1], 'force-wrong-type')
            return {'UNREACHABLE', '0', '0', '-1'}
            """.trimIndent(),
        )

        fun integrityAcquire(kind: FencingIntegrityFailureKind): FencingAcquireResult =
            FencingAcquireResult.IntegrityFailure(FencingLeaseIntegrityFailure(kind))
    }
}
