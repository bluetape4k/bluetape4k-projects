package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Demonstrates durable epoch cutover and downstream tuple rejection outside the Redis primitive. */
internal class LettuceFencingLeaseRecoveryTest {

    @Test
    fun `downstream accepts only a newer tuple and keeps business idempotency separate`() {
        val store = DownstreamStore()
        val resourceId = "invoice-42"
        val first = FencingToken(7, 1)
        val next = FencingToken(7, 2)

        store.write(resourceId, first, "payment-1") shouldBeEqualTo 1
        store.write(resourceId, first, "payment-2") shouldBeEqualTo 0
        store.write(resourceId, FencingToken(6, Long.MAX_VALUE), "payment-3") shouldBeEqualTo 0
        store.write(resourceId, next, "payment-1") shouldBeEqualTo 1

        store.row(resourceId) shouldBeEqualTo GuardedRow(resourceId, next, "payment-1")
    }

    @Test
    fun `durable CAS has one winner and non durable sources cannot allocate epochs`() {
        val authority = DurableEpochAuthority(11)
        val barrier = CyclicBarrier(3)
        val results = ConcurrentLinkedQueue<Boolean>()
        val executor = Executors.newFixedThreadPool(3)
        try {
            val futures = List(3) {
                executor.submit {
                    barrier.await()
                    results += authority.compareAndSet(11, 12)
                }
            }
            futures.forEach { it.get(5, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS).shouldBeTrue()
        }

        results.count { it } shouldBeEqualTo 1
        authority.currentEpoch shouldBeEqualTo 12
        val localConfig = LocalConfigEpochSource(12)
        val redisCounter = RedisCounterEpochSource(12)
        localConfig.observedEpoch shouldBeEqualTo 12
        redisCounter.observedEpoch shouldBeEqualTo 12
        localConfig.allocateNextEpoch().shouldBeFalse()
        redisCounter.allocateNextEpoch().shouldBeFalse()
    }

    @Test
    fun `control plane follows the exact cutover order and aborts on mixed epochs`() {
        val authority = DurableEpochAuthority(20)
        val successful = RecoveryControlPlane(authority).cutover(
            observedEpochs = setOf(20),
            requestedEpoch = 21,
            readiness = Readiness(counterReady = true, downstreamTupleGuardEnabled = true),
        )

        successful.shouldBeTrue()
        successful.events shouldBeEqualTo listOf(
            RecoveryEvent.PAUSE,
            RecoveryEvent.BLOCK_OLD_ACQUIRE,
            RecoveryEvent.DRAIN,
            RecoveryEvent.CAS_BUMP,
            RecoveryEvent.BOOTSTRAP,
            RecoveryEvent.READINESS,
            RecoveryEvent.ROLLOUT,
            RecoveryEvent.CONFIRM_OLD_ABSENCE,
            RecoveryEvent.RESUME,
        )

        val mixed = RecoveryControlPlane(authority).cutover(
            observedEpochs = setOf(20, 21),
            requestedEpoch = 22,
            readiness = Readiness(counterReady = true, downstreamTupleGuardEnabled = true),
        )
        mixed.shouldBeFalse()
        mixed.events.count { it == RecoveryEvent.ROLLOUT } shouldBeEqualTo 0
        mixed.events.count { it == RecoveryEvent.RESUME } shouldBeEqualTo 0

        val rollback = RecoveryControlPlane(authority).cutover(
            observedEpochs = setOf(21),
            requestedEpoch = 20,
            readiness = Readiness(counterReady = true, downstreamTupleGuardEnabled = true),
        )
        rollback.shouldBeFalse()
        rollback.events shouldBeEqualTo listOf(RecoveryEvent.PAUSE, RecoveryEvent.ROLLBACK_REJECTED)
        authority.currentEpoch shouldBeEqualTo 21
    }

    @Test
    fun `bootstrap readiness requires exact durable counter invariants and tuple guard`() {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName().substringAfter(':')
            val config = LettuceFencingLeaseConfig("recovery", tag, 41)
            val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
            val commands = connection.sync()
            val lease = LettuceFencingLease(connection, config)
            try {
                lease.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
                isReady(commands, keys, downstreamTupleGuardEnabled = true).shouldBeTrue()
                isReady(commands, keys, downstreamTupleGuardEnabled = false).shouldBeFalse()

                commands.pexpire(keys.counter, 10_000)
                isReady(commands, keys, downstreamTupleGuardEnabled = true).shouldBeFalse()
                commands.persist(keys.counter)
                commands.set(keys.counter, "01")
                isReady(commands, keys, downstreamTupleGuardEnabled = true).shouldBeFalse()
                commands.set(keys.counter, "1\u0661")
                isReady(commands, keys, downstreamTupleGuardEnabled = true).shouldBeFalse()
                commands.set(keys.counter, "9223372036854775808")
                isReady(commands, keys, downstreamTupleGuardEnabled = true).shouldBeFalse()
            } finally {
                commands.del(keys.lease, keys.counter)
            }
        }
    }

    @Test
    fun `read only diagnostic classifies bounded states and repair preserves the counter`() {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = LettuceTestUtils.randomName().substringAfter(':')
            val config = LettuceFencingLeaseConfig("diagnostic", tag, 51)
            val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
            val commands = connection.sync()
            try {
                commands.set(keys.counter, "4")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("CLEAN", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()

                commands.del(keys.counter)
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("COUNTER_MISSING", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()

                commands.set(keys.counter, "4")
                commands.set(keys.lease, "not-a-hash")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_MALFORMED", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.exists(keys.lease) shouldBeEqualTo 1L

                commands.del(keys.lease)
                commands.hset(
                    keys.lease,
                    mapOf("owner" to "owner", "epoch" to "51", "sequence" to "4"),
                )
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_NO_TTL", "1")

                commands.hset(keys.lease, "sequence", "5")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("COUNTER_BEHIND_LEASE", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.hset(keys.lease, "sequence", "9223372036854775808")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_MALFORMED", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.hset(keys.lease, "sequence", "4")
                commands.set(keys.counter, "9223372036854775808")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("COUNTER_INVALID", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.set(keys.counter, "4")

                commands.hset(keys.lease, "epoch", "50")
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_MALFORMED", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.hset(keys.lease, "epoch", "51")
                commands.hset(keys.lease, "owner", "x".repeat(257))
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_MALFORMED", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.hset(keys.lease, "owner", "owner")
                commands.pexpire(keys.lease, 30_000)
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("ACTIVE", "0")
                repairLeaseOnly(commands, keys, config.epoch, FULL_REPAIR_ELIGIBILITY).shouldBeFalse()
                commands.persist(keys.lease)
                diagnostic(commands, keys, config.epoch) shouldBeEqualTo listOf("LEASE_NO_TTL", "1")

                repairCases().dropLast(1).forEach { eligibility ->
                    repairLeaseOnly(commands, keys, config.epoch, eligibility).shouldBeFalse()
                    commands.exists(keys.lease) shouldBeEqualTo 1L
                }

                val counterBefore = commands.get(keys.counter)
                val counterTtlBefore = commands.pttl(keys.counter)
                repairLeaseOnly(commands, keys, config.epoch, repairCases().last()).shouldBeTrue()
                commands.exists(keys.lease) shouldBeEqualTo 0L
                commands.get(keys.counter) shouldBeEqualTo counterBefore
                commands.pttl(keys.counter) shouldBeEqualTo counterTtlBefore
                commands.pttl(keys.counter) shouldBeEqualTo -1L
            } finally {
                commands.del(keys.lease, keys.counter)
            }
        }
    }

    @Test
    fun `higher epoch downstream state rejects Redis only rollback tokens`() {
        val store = DownstreamStore()
        val resourceId = "restored-resource"
        val current = FencingToken(62, 1)
        val restoredRedisToken = FencingToken(61, 99)

        store.write(resourceId, current, "business-current") shouldBeEqualTo 1
        RedisCounterEpochSource(restoredRedisToken.epoch).allocateNextEpoch().shouldBeFalse()
        store.write(resourceId, restoredRedisToken, "business-old") shouldBeEqualTo 0
        requireNotNull(store.row(resourceId)).token shouldBeGreaterThan restoredRedisToken
    }

    private fun diagnostic(
        commands: RedisCommands<String, String>,
        keys: FencingLeaseKeys,
        expectedEpoch: Long,
    ): List<String> = commands.evalReadOnly(
        DIAGNOSTIC_LUA,
        ScriptOutputType.MULTI,
        arrayOf(keys.lease, keys.counter),
        expectedEpoch.toString(),
    )

    private fun isReady(
        commands: RedisCommands<String, String>,
        keys: FencingLeaseKeys,
        downstreamTupleGuardEnabled: Boolean,
    ): Boolean {
        if (!downstreamTupleGuardEnabled) return false
        if (commands.type(keys.counter) != "string") return false
        if (commands.pttl(keys.counter) != -1L) return false
        return commands.get(keys.counter)?.let { counter ->
            runCatching { requireCanonicalFencingDecimal(counter) }.isSuccess
        } == true
    }

    private fun repairLeaseOnly(
        commands: RedisCommands<String, String>,
        keys: FencingLeaseKeys,
        expectedEpoch: Long,
        eligibility: RepairEligibility,
    ): Boolean {
        val diagnosis = diagnostic(commands, keys, expectedEpoch)
        if (diagnosis != listOf("LEASE_NO_TTL", "1") || !eligibility.mayDeleteLease) return false
        return commands.del(keys.lease) == 1L
    }

    private fun repairCases(): List<RepairEligibility> = listOf(
        RepairEligibility(false, true, true, true),
        RepairEligibility(true, false, true, true),
        RepairEligibility(true, true, false, true),
        RepairEligibility(true, true, true, false),
        RepairEligibility(true, true, true, true),
    )

    private class DurableEpochAuthority(initialEpoch: Long) {
        private val epoch = AtomicLong(initialEpoch)

        val currentEpoch: Long get() = epoch.get()

        fun compareAndSet(expected: Long, update: Long): Boolean =
            update > expected && epoch.compareAndSet(expected, update)
    }

    private class LocalConfigEpochSource(val observedEpoch: Long) {
        fun allocateNextEpoch(): Boolean = false
    }

    private class RedisCounterEpochSource(val observedEpoch: Long) {
        fun allocateNextEpoch(): Boolean = false
    }

    private class DownstreamStore {
        private val rows = mutableMapOf<String, GuardedRow>()

        @Synchronized
        fun write(resourceId: String, token: FencingToken, businessIdempotencyKey: String): Int {
            val current = rows[resourceId]
            if (current != null && token <= current.token) return 0
            rows[resourceId] = GuardedRow(resourceId, token, businessIdempotencyKey)
            return 1
        }

        @Synchronized
        fun row(resourceId: String): GuardedRow? = rows[resourceId]
    }

    private class RecoveryControlPlane(private val authority: DurableEpochAuthority) {
        fun cutover(
            observedEpochs: Set<Long>,
            requestedEpoch: Long,
            readiness: Readiness,
        ): RecoveryOutcome {
            val events = mutableListOf(RecoveryEvent.PAUSE)
            if (observedEpochs.size != 1) return RecoveryOutcome(false, events + RecoveryEvent.MIXED_EPOCH_ABORT)
            if (requestedEpoch <= authority.currentEpoch) {
                return RecoveryOutcome(false, events + RecoveryEvent.ROLLBACK_REJECTED)
            }

            events += RecoveryEvent.BLOCK_OLD_ACQUIRE
            events += RecoveryEvent.DRAIN
            if (!authority.compareAndSet(observedEpochs.single(), requestedEpoch)) {
                return RecoveryOutcome(false, events + RecoveryEvent.CAS_REJECTED)
            }
            events += RecoveryEvent.CAS_BUMP
            events += RecoveryEvent.BOOTSTRAP
            events += RecoveryEvent.READINESS
            if (!readiness.counterReady || !readiness.downstreamTupleGuardEnabled) {
                return RecoveryOutcome(false, events + RecoveryEvent.READINESS_REJECTED)
            }
            events += RecoveryEvent.ROLLOUT
            events += RecoveryEvent.CONFIRM_OLD_ABSENCE
            events += RecoveryEvent.RESUME
            return RecoveryOutcome(true, events)
        }
    }

    private data class GuardedRow(
        val resourceId: String,
        val token: FencingToken,
        val businessIdempotencyKey: String,
    )

    private data class Readiness(
        val counterReady: Boolean,
        val downstreamTupleGuardEnabled: Boolean,
    )

    private data class RecoveryOutcome(
        val succeeded: Boolean,
        val events: List<RecoveryEvent>,
    ) {
        fun shouldBeTrue() = succeeded.shouldBeTrue()
        fun shouldBeFalse() = succeeded.shouldBeFalse()
    }

    private enum class RecoveryEvent {
        PAUSE,
        BLOCK_OLD_ACQUIRE,
        DRAIN,
        CAS_BUMP,
        BOOTSTRAP,
        READINESS,
        ROLLOUT,
        CONFIRM_OLD_ABSENCE,
        RESUME,
        MIXED_EPOCH_ABORT,
        ROLLBACK_REJECTED,
        CAS_REJECTED,
        READINESS_REJECTED,
    }

    private data class RepairEligibility(
        val incidentPaused: Boolean,
        val downstreamDrained: Boolean,
        val counterValidAndPersistent: Boolean,
        val leaseConfirmedAnomalous: Boolean,
    ) {
        val mayDeleteLease: Boolean
            get() = incidentPaused && downstreamDrained && counterValidAndPersistent && leaseConfirmedAnomalous
    }

    private companion object {
        val FULL_REPAIR_ELIGIBILITY = RepairEligibility(true, true, true, true)
        val DIAGNOSTIC_LUA: String =
            """
            local counter_type = redis.call('TYPE', KEYS[2])['ok']
            if counter_type == 'none' then return {'COUNTER_MISSING', '0'} end
            if counter_type ~= 'string' then return {'COUNTER_INVALID', '0'} end
            if redis.call('PTTL', KEYS[2]) ~= -1 then return {'COUNTER_INVALID', '0'} end
            local counter = redis.call('GET', KEYS[2])
            local counter_valid = counter == '0' or string.match(counter, '^[1-9][0-9]*$')
            counter_valid = counter_valid and (#counter < 19 or (#counter == 19 and counter <= '9223372036854775807'))
            if not counter_valid then return {'COUNTER_INVALID', '0'} end

            local lease_type = redis.call('TYPE', KEYS[1])['ok']
            if lease_type == 'none' then return {'CLEAN', '0'} end
            if lease_type ~= 'hash' then return {'LEASE_MALFORMED', '0'} end
            if redis.call('HLEN', KEYS[1]) ~= 3 then return {'LEASE_MALFORMED', '0'} end
            local owner_length = redis.call('HSTRLEN', KEYS[1], 'owner')
            local epoch_length = redis.call('HSTRLEN', KEYS[1], 'epoch')
            local sequence_length = redis.call('HSTRLEN', KEYS[1], 'sequence')
            if owner_length < 1 or owner_length > 256 or epoch_length < 1 or epoch_length > 19 or
               sequence_length < 1 or sequence_length > 19 then
                return {'LEASE_MALFORMED', '0'}
            end
            local fields = redis.call('HMGET', KEYS[1], 'owner', 'epoch', 'sequence')
            local epoch = fields[2]
            local sequence = fields[3]
            if not string.match(epoch, '^[1-9][0-9]*$') then return {'LEASE_MALFORMED', '0'} end
            if not string.match(sequence, '^[1-9][0-9]*$') then return {'LEASE_MALFORMED', '0'} end
            if epoch ~= ARGV[1] then return {'LEASE_MALFORMED', '0'} end
            if #epoch > 19 or (#epoch == 19 and epoch > '9223372036854775807') then
                return {'LEASE_MALFORMED', '0'}
            end
            if #sequence > 19 or (#sequence == 19 and sequence > '9223372036854775807') then
                return {'LEASE_MALFORMED', '0'}
            end
            if #counter < #sequence or (#counter == #sequence and counter < sequence) then
                return {'COUNTER_BEHIND_LEASE', '0'}
            end
            if redis.call('PTTL', KEYS[1]) == -1 then return {'LEASE_NO_TTL', '1'} end
            return {'ACTIVE', '0'}
            """.trimIndent()
    }
}
