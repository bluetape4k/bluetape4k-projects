package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier

/**
 * Proves bounded fencing-token uniqueness for one hot resource serialized by one Redis slot.
 *
 * These tests verify ordering and duplicate absence; they intentionally do not define a latency SLA.
 */
internal class LettuceFencingLeaseConcurrencyTest : AbstractLettuceTest() {

    @Test
    @Timeout(30)
    fun `sync callers issue one unique generation per bounded contention round`() {
        val config = newConfig("sync")
        val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        val lease = LettuceFencingLease(connection, config)
        val tokens = mutableListOf<FencingToken>()
        try {
            lease.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

            repeat(ROUNDS) { round ->
                val barrier = CyclicBarrier(CALLERS)
                val attempts = ConcurrentLinkedQueue<Attempt>()
                val tasks = List(CALLERS) { caller ->
                    val ownerId = owner(round, caller)
                    val task: () -> Unit = {
                        barrier.await()
                        attempts += Attempt(ownerId, lease.acquire(ownerId, LEASE_TIME))
                    }
                    task
                }

                MultithreadingTester()
                    .workers(CALLERS)
                    .rounds(1)
                    .addAll(tasks)
                    .run()

                val winner = verifyRound(attempts)
                val replay = lease.acquire(winner.ownerId, LONGER_LEASE)
                    .shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
                replay.token shouldBeEqualTo winner.token
                commands.get(keys.counter) shouldBeEqualTo winner.token.sequence.toString()
                lease.release(winner.ownerId, winner.token) shouldBeEqualTo FencingReleaseResult.Released
                tokens += winner.token
            }

            verifyTokens(tokens, ROUNDS)
        } finally {
            commands.del(keys.lease, keys.counter)
        }
    }

    @Test
    @Timeout(30)
    fun `suspend callers issue one unique generation per bounded contention round`() = runSuspendIO {
        val config = newConfig("suspend")
        val keys = deriveFencingLeaseKeys(config, StringCodec.UTF8)
        val lease = LettuceSuspendFencingLease(connection, config)
        val tokens = mutableListOf<FencingToken>()
        try {
            lease.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

            repeat(ROUNDS) { round ->
                val barrier = CyclicBarrier(CALLERS)
                val attempts = ConcurrentLinkedQueue<Attempt>()
                val tasks = List<suspend () -> Unit>(CALLERS) { caller ->
                    val ownerId = owner(round, caller)
                    val task: suspend () -> Unit = {
                        barrier.await()
                        attempts += Attempt(ownerId, lease.acquire(ownerId, LEASE_TIME))
                    }
                    task
                }

                SuspendedJobTester()
                    .workers(CALLERS)
                    .rounds(1)
                    .addAll(tasks)
                    .run()

                val winner = verifyRound(attempts)
                val replay = lease.acquire(winner.ownerId, LONGER_LEASE)
                    .shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
                replay.token shouldBeEqualTo winner.token
                commands.get(keys.counter) shouldBeEqualTo winner.token.sequence.toString()
                lease.release(winner.ownerId, winner.token) shouldBeEqualTo FencingReleaseResult.Released
                tokens += winner.token
            }

            verifyTokens(tokens, ROUNDS)
        } finally {
            commands.del(keys.lease, keys.counter)
        }
    }

    private fun verifyRound(attempts: Collection<Attempt>): Winner {
        attempts.size shouldBeEqualTo CALLERS
        val acquired = attempts.filter { it.result is FencingAcquireResult.Acquired }
        val contended = attempts.filter { it.result is FencingAcquireResult.Contended }
        acquired.size shouldBeEqualTo 1
        contended.size shouldBeEqualTo CALLERS - 1
        return acquired.single().let { attempt ->
            Winner(
                attempt.ownerId,
                attempt.result.shouldBeInstanceOf<FencingAcquireResult.Acquired>().token,
            )
        }
    }

    private fun verifyTokens(tokens: List<FencingToken>, expected: Int) {
        tokens.size shouldBeEqualTo expected
        tokens.toSet().size shouldBeEqualTo expected
        tokens.zipWithNext().forEach { (previous, next) -> next shouldBeGreaterThan previous }
    }

    private fun newConfig(style: String): LettuceFencingLeaseConfig =
        LettuceFencingLeaseConfig("contention", "$style-${randomName().substringAfter(':')}", 23)

    private fun owner(round: Int, caller: Int): FencingOwnerId =
        FencingOwnerId.from("owner-$round-$caller")

    private data class Attempt(
        val ownerId: FencingOwnerId,
        val result: FencingAcquireResult,
    )

    private data class Winner(
        val ownerId: FencingOwnerId,
        val token: FencingToken,
    )

    private companion object {
        const val CALLERS: Int = 16
        const val ROUNDS: Int = 25
        val LEASE_TIME: Duration = Duration.ofSeconds(10)
        val LONGER_LEASE: Duration = Duration.ofSeconds(30)

        val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
        val commands by lazy { connection.sync() }

        @BeforeAll
        @JvmStatic
        fun warmStandaloneRedis() {
            connection.sync().ping() shouldBeEqualTo "PONG"
        }
    }
}
