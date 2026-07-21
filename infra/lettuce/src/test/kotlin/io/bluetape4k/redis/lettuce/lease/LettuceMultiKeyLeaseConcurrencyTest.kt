package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicReference

internal class LettuceMultiKeyLeaseConcurrencyTest {

    @Test
    fun `overlapping callers create only the winning key set`() {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { firstConnection ->
            LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { secondConnection ->
                val tag = UUID.randomUUID().toString()
                val shared = "lease:{$tag}:shared"
                val firstOnly = "lease:{$tag}:first"
                val secondOnly = "lease:{$tag}:second"
                val allKeys = arrayOf(shared, firstOnly, secondOnly)
                val commands = firstConnection.sync()
                val firstToken = "owner-first-$tag"
                val secondToken = "owner-second-$tag"
                val firstResult = AtomicReference<MultiKeyAcquireResult>()
                val secondResult = AtomicReference<MultiKeyAcquireResult>()
                val barrier = CyclicBarrier(2)
                try {
                    commands.del(*allKeys)
                    val firstLease = LettuceMultiKeyLease(firstConnection)
                    val secondLease = LettuceMultiKeyLease(secondConnection)
                    MultithreadingTester()
                        .workers(2)
                        .rounds(1)
                        .addAll(
                            {
                                barrier.await()
                                firstResult.set(
                                    firstLease.acquire(listOf(shared, firstOnly), firstToken, FIVE_SECONDS)
                                )
                            },
                            {
                                barrier.await()
                                secondResult.set(
                                    secondLease.acquire(listOf(shared, secondOnly), secondToken, FIVE_SECONDS)
                                )
                            },
                        )
                        .run()

                    val firstWon = firstResult.get() == MultiKeyAcquireResult.Acquired
                    val secondWon = secondResult.get() == MultiKeyAcquireResult.Acquired
                    (firstWon xor secondWon) shouldBeEqualTo true
                    if (firstWon) {
                        secondResult.get() shouldBeEqualTo
                            MultiKeyAcquireResult.Conflicted(MultiKeyLeaseCounts(2, 0, 1, 1))
                        commands.get(shared) shouldBeEqualTo firstToken
                        commands.get(firstOnly) shouldBeEqualTo firstToken
                        commands.get(secondOnly).shouldBeNull()
                    } else {
                        firstResult.get() shouldBeEqualTo
                            MultiKeyAcquireResult.Conflicted(MultiKeyLeaseCounts(2, 0, 1, 1))
                        commands.get(shared) shouldBeEqualTo secondToken
                        commands.get(secondOnly) shouldBeEqualTo secondToken
                        commands.get(firstOnly).shouldBeNull()
                    }
                } finally {
                    commands.del(*allKeys)
                }
            }
        }
    }

    @Test
    fun `hostile ownership changes preserve keys owned by replacement callers`() {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = UUID.randomUUID().toString()
            val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
            val owner = "owner-$tag"
            val stale = "stale-$tag"
            val replacement = "replacement-$tag"
            val commands = connection.sync()
            val lease = LettuceMultiKeyLease(connection)
            try {
                commands.del(*keys.toTypedArray())
                lease.acquire(keys, owner, FIVE_SECONDS) shouldBeEqualTo MultiKeyAcquireResult.Acquired

                lease.renew(keys, stale, TEN_SECONDS) shouldBeEqualTo
                    MultiKeyRenewResult.OwnershipMismatch(MultiKeyLeaseCounts(2, 0, 0, 2))
                lease.release(keys, stale) shouldBeEqualTo
                    MultiKeyReleaseResult.OwnershipMismatch(MultiKeyLeaseCounts(2, 0, 0, 2))
                keys.forEach { commands.get(it) shouldBeEqualTo owner }

                commands.del(keys[1])
                lease.renew(keys, owner, TEN_SECONDS) shouldBeEqualTo
                    MultiKeyRenewResult.PartialLoss(MultiKeyLeaseCounts(2, 1, 1, 0))
                commands.get(keys[0]) shouldBeEqualTo owner
                commands.get(keys[1]).shouldBeNull()

                commands.psetex(keys[1], 5_000, replacement)
                lease.release(keys, owner) shouldBeEqualTo
                    MultiKeyReleaseResult.OwnershipMismatch(MultiKeyLeaseCounts(2, 1, 0, 1))
                commands.get(keys[0]).shouldBeNull()
                commands.get(keys[1]) shouldBeEqualTo replacement
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    @Test
    fun `expired lease converges to full loss without exact sleeps`() {
        LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8).use { connection ->
            val tag = UUID.randomUUID().toString()
            val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
            val token = "owner-$tag"
            val commands = connection.sync()
            val lease = LettuceMultiKeyLease(connection)
            try {
                commands.del(*keys.toTypedArray())
                lease.acquire(keys, token, Duration.ofMillis(200)) shouldBeEqualTo MultiKeyAcquireResult.Acquired

                await()
                    .atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(20))
                    .untilAsserted {
                        lease.inspect(keys, token) shouldBeEqualTo MultiKeyInspectResult.Lost
                    }

                commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L
            } finally {
                commands.del(*keys.toTypedArray())
            }
        }
    }

    private companion object {
        val FIVE_SECONDS: Duration = Duration.ofSeconds(5)
        val TEN_SECONDS: Duration = Duration.ofSeconds(10)
    }
}
