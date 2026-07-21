package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.api.sync.RedisCommands
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.time.Duration

internal interface MultiKeyLeaseAdapter {
    val name: String

    fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration): MultiKeyAcquireResult
    fun inspect(keys: Collection<String>, ownerToken: String): MultiKeyInspectResult
    fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration): MultiKeyRenewResult
    fun release(keys: Collection<String>, ownerToken: String): MultiKeyReleaseResult
}

internal abstract class MultiKeyLeaseContract : AbstractLettuceTest() {

    protected abstract val commands: RedisCommands<String, String>
    protected abstract val adapters: List<MultiKeyLeaseAdapter>

    private val touchedKeys = mutableSetOf<String>()

    @AfterEach
    fun cleanLeaseContractKeys() {
        if (touchedKeys.isNotEmpty()) {
            commands.del(*touchedKeys.toTypedArray())
            touchedKeys.clear()
        }
    }

    @TestFactory
    fun `all adapters share the multi-key lease contract`(): List<DynamicTest> =
        adapters.flatMap { adapter ->
            scenarios.map { scenario ->
                DynamicTest.dynamicTest("${adapter.name}: ${scenario.name}") {
                    val fixture = fixture(adapter.name, scenario.name)
                    touchedKeys += fixture.keys
                    commands.del(*fixture.keys.toTypedArray())
                    scenario.run(adapter, fixture)
                }
            }
        }

    private fun fixture(adapterName: String, scenarioName: String): LeaseFixture {
        val tag = "${adapterName.hashCode().toUInt()}-${scenarioName.hashCode().toUInt()}-${System.nanoTime()}"
        return LeaseFixture(
            keys = listOf("lease:{$tag}:one", "lease:{$tag}:two"),
            token = "owner-$tag",
        )
    }

    private data class LeaseFixture(
        val keys: List<String>,
        val token: String,
    )

    private class Scenario(
        val name: String,
        val run: (MultiKeyLeaseAdapter, LeaseFixture) -> Unit,
    )

    private val scenarios: List<Scenario> = listOf(
        Scenario("acquire Acquired") { adapter, fixture ->
            adapter.acquire(fixture.keys, fixture.token, FIVE_SECONDS) shouldBeEqualTo MultiKeyAcquireResult.Acquired
        },
        Scenario("acquire AlreadyOwned does not extend ttl") { adapter, fixture ->
            adapter.acquire(fixture.keys, fixture.token, FIVE_SECONDS)
            val before = (adapter.inspect(fixture.keys, fixture.token) as MultiKeyInspectResult.Owned).minimumPttlMillis
            val replay = adapter.acquire(fixture.keys, fixture.token, TEN_SECONDS) as MultiKeyAcquireResult.AlreadyOwned
            val after = (adapter.inspect(fixture.keys, fixture.token) as MultiKeyInspectResult.Owned).minimumPttlMillis
            replay.minimumPttlMillis shouldBeLessOrEqualTo before
            after shouldBeLessOrEqualTo before
            after shouldBeGreaterOrEqualTo before - TTL_TOLERANCE_MILLIS
        },
        Scenario("acquire PartialOwnership") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            adapter.acquire(fixture.keys, fixture.token, FIVE_SECONDS) shouldBeEqualTo
                MultiKeyAcquireResult.PartialOwnership(counts(2, 1, 1, 0))
            commands.get(fixture.keys[1]).shouldBeNull()
        },
        Scenario("acquire Conflicted") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, OTHER_OWNER)
            adapter.acquire(fixture.keys, fixture.token, FIVE_SECONDS) shouldBeEqualTo
                MultiKeyAcquireResult.Conflicted(counts(2, 0, 1, 1))
            commands.get(fixture.keys[1]).shouldBeNull()
        },
        Scenario("inspect Owned") { adapter, fixture ->
            fixture.keys.forEach { commands.psetex(it, 5_000, fixture.token) }
            (adapter.inspect(fixture.keys, fixture.token) as MultiKeyInspectResult.Owned)
                .minimumPttlMillis shouldBeGreaterOrEqualTo 1L
        },
        Scenario("inspect Lost") { adapter, fixture ->
            adapter.inspect(fixture.keys, fixture.token) shouldBeEqualTo MultiKeyInspectResult.Lost
        },
        Scenario("inspect PartialOwnership") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            adapter.inspect(fixture.keys, fixture.token) shouldBeEqualTo
                MultiKeyInspectResult.PartialOwnership(counts(2, 1, 1, 0))
        },
        Scenario("inspect Conflicted") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            commands.psetex(fixture.keys[1], 5_000, OTHER_OWNER)
            adapter.inspect(fixture.keys, fixture.token) shouldBeEqualTo
                MultiKeyInspectResult.Conflicted(counts(2, 1, 0, 1))
        },
        Scenario("renew Renewed") { adapter, fixture ->
            fixture.keys.forEach { commands.psetex(it, 5_000, fixture.token) }
            adapter.renew(fixture.keys, fixture.token, TEN_SECONDS) shouldBeEqualTo MultiKeyRenewResult.Renewed
        },
        Scenario("renew PartialLoss") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            adapter.renew(fixture.keys, fixture.token, TEN_SECONDS) shouldBeEqualTo
                MultiKeyRenewResult.PartialLoss(counts(2, 1, 1, 0))
        },
        Scenario("renew Lost") { adapter, fixture ->
            adapter.renew(fixture.keys, fixture.token, TEN_SECONDS) shouldBeEqualTo MultiKeyRenewResult.Lost
        },
        Scenario("renew OwnershipMismatch") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            commands.psetex(fixture.keys[1], 5_000, OTHER_OWNER)
            adapter.renew(fixture.keys, fixture.token, TEN_SECONDS) shouldBeEqualTo
                MultiKeyRenewResult.OwnershipMismatch(counts(2, 1, 0, 1))
            commands.get(fixture.keys[1]) shouldBeEqualTo OTHER_OWNER
        },
        Scenario("release Released") { adapter, fixture ->
            fixture.keys.forEach { commands.psetex(it, 5_000, fixture.token) }
            adapter.release(fixture.keys, fixture.token) shouldBeEqualTo MultiKeyReleaseResult.Released
        },
        Scenario("release PartialRelease") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            adapter.release(fixture.keys, fixture.token) shouldBeEqualTo
                MultiKeyReleaseResult.PartialRelease(counts(2, 1, 1, 0))
        },
        Scenario("release Lost") { adapter, fixture ->
            adapter.release(fixture.keys, fixture.token) shouldBeEqualTo MultiKeyReleaseResult.Lost
        },
        Scenario("release OwnershipMismatch") { adapter, fixture ->
            commands.psetex(fixture.keys[0], 5_000, fixture.token)
            commands.psetex(fixture.keys[1], 5_000, OTHER_OWNER)
            adapter.release(fixture.keys, fixture.token) shouldBeEqualTo
                MultiKeyReleaseResult.OwnershipMismatch(counts(2, 1, 0, 1))
            commands.get(fixture.keys[1]) shouldBeEqualTo OTHER_OWNER
        },
        Scenario("invalid inputs fail before dispatch") { adapter, fixture ->
            val invalidCalls = listOf<() -> Unit>(
                { adapter.inspect(emptyList(), fixture.token) },
                { adapter.inspect(listOf(" "), fixture.token) },
                { adapter.inspect(listOf(fixture.keys[0], fixture.keys[0]), fixture.token) },
                { adapter.inspect(fixture.keys, " ") },
                { adapter.acquire(fixture.keys, fixture.token, Duration.ZERO) },
                { adapter.renew(fixture.keys, fixture.token, Duration.ofMillis(-1)) },
                { adapter.acquire(fixture.keys, fixture.token, Duration.ofNanos(1)) },
            )
            invalidCalls.forEach { call -> assertFailsWith<IllegalArgumentException> { call() } }
            assertFailsWith<ArithmeticException> {
                adapter.acquire(fixture.keys, fixture.token, Duration.ofSeconds(Long.MAX_VALUE))
            }
            commands.exists(*fixture.keys.toTypedArray()) shouldBeEqualTo 0L
        },
        Scenario("persistent same-token integrity and release recovery") { adapter, fixture ->
            commands.set(fixture.keys[0], fixture.token)
            commands.psetex(fixture.keys[1], 5_000, fixture.token)
            assertFailsWith<MultiKeyLeaseIntegrityException> {
                adapter.acquire(fixture.keys, fixture.token, FIVE_SECONDS)
            }.operation shouldBeEqualTo MultiKeyLeaseOperation.ACQUIRE
            assertFailsWith<MultiKeyLeaseIntegrityException> {
                adapter.inspect(fixture.keys, fixture.token)
            }.operation shouldBeEqualTo MultiKeyLeaseOperation.INSPECT
            assertFailsWith<MultiKeyLeaseIntegrityException> {
                adapter.renew(fixture.keys, fixture.token, TEN_SECONDS)
            }.operation shouldBeEqualTo MultiKeyLeaseOperation.RENEW
            adapter.release(fixture.keys, fixture.token) shouldBeEqualTo MultiKeyReleaseResult.Released
            commands.exists(*fixture.keys.toTypedArray()) shouldBeEqualTo 0L
        },
        Scenario("cross-slot input fails before dispatch") { adapter, fixture ->
            val crossSlot = listOf("lease:{slot-one}:one", "lease:{slot-two}:two")
            assertFailsWith<MultiKeyLeaseCrossSlotException> {
                adapter.inspect(crossSlot, fixture.token)
            }
            commands.exists(*fixture.keys.toTypedArray()) shouldBeEqualTo 0L
        },
    )

    private fun counts(requested: Int, owned: Int, missing: Int, mismatched: Int) =
        MultiKeyLeaseCounts(requested, owned, missing, mismatched)

    private companion object {
        const val OTHER_OWNER = "other-owner"
        const val TTL_TOLERANCE_MILLIS = 1_000L
        val FIVE_SECONDS: Duration = Duration.ofSeconds(5)
        val TEN_SECONDS: Duration = Duration.ofSeconds(10)
    }
}
