package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.codec.RedisCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier

/** Verifies that one fencing domain remains on one Redis Cluster slot across every execution style. */
internal class LettuceFencingLeaseClusterTest {

    @Test
    @Timeout(30)
    fun `cluster issues unique generations under mixed sync future and suspend contention`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val config = newConfig("contention")
                val keys = deriveFencingLeaseKeys(config, connection.codec)
                val adapters = clusterAdapters(connection, config)
                val commands = connection.sync()
                val tokens = mutableListOf<FencingToken>()
                try {
                    adapters.first().bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized

                    repeat(CLUSTER_ROUNDS) { round ->
                        val barrier = CyclicBarrier(CLUSTER_CALLERS)
                        val attempts = ConcurrentLinkedQueue<ClusterAttempt>()
                        val tasks = List<suspend () -> Unit>(CLUSTER_CALLERS) { caller ->
                            val ownerId = FencingOwnerId.from("cluster-owner-$round-$caller")
                            val adapterIndex = caller % adapters.size
                            val task: suspend () -> Unit = {
                                barrier.await()
                                attempts += ClusterAttempt(
                                    ownerId,
                                    adapterIndex,
                                    adapters[adapterIndex].acquire(ownerId, LEASE_TIME),
                                )
                            }
                            task
                        }

                        SuspendedJobTester()
                            .workers(CLUSTER_CALLERS)
                            .rounds(1)
                            .addAll(tasks)
                            .run()

                        val winner = verifyClusterRound(attempts)
                        val winnerAdapter = adapters[winner.adapterIndex]
                        val replay = winnerAdapter.acquire(winner.ownerId, LONGER_LEASE)
                            .shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
                        replay.token shouldBeEqualTo winner.token
                        commands.get(keys.counter) shouldBeEqualTo winner.token.sequence.toString()
                        winnerAdapter.release(winner.ownerId, winner.token) shouldBeEqualTo
                            FencingReleaseResult.Released
                        tokens += winner.token
                    }

                    tokens.size shouldBeEqualTo CLUSTER_ROUNDS
                    tokens.toSet().size shouldBeEqualTo CLUSTER_ROUNDS
                    tokens.zipWithNext().forEach { (previous, next) -> next shouldBeGreaterThan previous }
                } finally {
                    commands.del(keys.lease, keys.counter)
                }
            }
        }
    }

    @Test
    @Timeout(30)
    fun `cluster preserves complete result parity for every execution style`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val commands = connection.sync()
                clusterAdapterFactories(connection).forEachIndexed { index, factory ->
                    val config = newConfig("parity-$index")
                    val keys = deriveFencingLeaseKeys(config, connection.codec)
                    val adapter = factory(config)
                    val ownerId = FencingOwnerId.from("cluster-parity-owner-$index")
                    try {
                        adapter.bootstrap() shouldBeEqualTo FencingBootstrapResult.Initialized
                        val acquired = adapter.acquire(ownerId, LEASE_TIME)
                            .shouldBeInstanceOf<FencingAcquireResult.Acquired>()
                        val replay = adapter.acquire(ownerId, LONGER_LEASE)
                            .shouldBeInstanceOf<FencingAcquireResult.AlreadyOwned>()
                        replay.token shouldBeEqualTo acquired.token
                        adapter.inspect(ownerId)
                            .shouldBeInstanceOf<FencingInspectResult.Owned>().token shouldBeEqualTo acquired.token
                        adapter.renew(ownerId, acquired.token, LONGER_LEASE) shouldBeEqualTo FencingRenewResult.Renewed
                        adapter.release(ownerId, acquired.token) shouldBeEqualTo FencingReleaseResult.Released
                        adapter.inspect(ownerId) shouldBeEqualTo FencingInspectResult.Lost
                    } finally {
                        commands.del(keys.lease, keys.counter)
                    }
                }
            }
        }
    }

    @Test
    fun `wire split codec rejects public cluster facades before Redis dispatch`() {
        val syncCommands = mockk<RedisAdvancedClusterCommands<String, String>>(relaxed = true)
        val asyncCommands = mockk<RedisAdvancedClusterAsyncCommands<String, String>>(relaxed = true)
        val connection = mockk<StatefulRedisClusterConnection<String, String>>()
        every { connection.sync() } returns syncCommands
        every { connection.async() } returns asyncCommands
        every { connection.codec } returns SplitSlotWireCodec
        val config = LettuceFencingLeaseConfig("cluster", "wire-split", 29)
        val keys = FencingLeaseKeys(
            "fence:{cluster:wire-split}:29:lease",
            "fence:{cluster:wire-split}:29:counter",
        )
        SlotHash.getSlot(SplitSlotWireCodec.encodeKey(keys.lease)) shouldNotBeEqualTo
            SlotHash.getSlot(SplitSlotWireCodec.encodeKey(keys.counter))
        assertFailsWith<IllegalArgumentException> { LettuceFencingLease(connection, config) }
        assertFailsWith<IllegalArgumentException> { LettuceSuspendFencingLease(connection, config) }

        verify { syncCommands wasNot Called }
        verify { asyncCommands wasNot Called }
    }

    private fun clusterAdapters(
        connection: StatefulRedisClusterConnection<String, String>,
        config: LettuceFencingLeaseConfig,
    ): List<FencingLeaseAdapter> = clusterAdapterFactories(connection).map { factory -> factory(config) }

    private fun clusterAdapterFactories(
        connection: StatefulRedisClusterConnection<String, String>,
    ): List<(LettuceFencingLeaseConfig) -> FencingLeaseAdapter> = listOf(
        { config ->
            val lease = LettuceFencingLease(connection, config)
            object: FencingLeaseAdapter {
                override suspend fun bootstrap(): FencingBootstrapResult = lease.bootstrap()
                override suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult =
                    lease.acquire(ownerId, leaseTime)
                override suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult = lease.inspect(ownerId)
                override suspend fun renew(
                    ownerId: FencingOwnerId,
                    token: FencingToken,
                    leaseTime: Duration,
                ): FencingRenewResult = lease.renew(ownerId, token, leaseTime)
                override suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult =
                    lease.release(ownerId, token)
            }
        },
        { config ->
            val lease = LettuceFencingLease(connection, config)
            object: FencingLeaseAdapter {
                override suspend fun bootstrap(): FencingBootstrapResult = lease.bootstrapAsync().await()
                override suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult =
                    lease.acquireAsync(ownerId, leaseTime).await()
                override suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult =
                    lease.inspectAsync(ownerId).await()
                override suspend fun renew(
                    ownerId: FencingOwnerId,
                    token: FencingToken,
                    leaseTime: Duration,
                ): FencingRenewResult = lease.renewAsync(ownerId, token, leaseTime).await()
                override suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult =
                    lease.releaseAsync(ownerId, token).await()
            }
        },
        { config ->
            val lease = LettuceSuspendFencingLease(connection, config)
            object: FencingLeaseAdapter {
                override suspend fun bootstrap(): FencingBootstrapResult = lease.bootstrap()
                override suspend fun acquire(ownerId: FencingOwnerId, leaseTime: Duration): FencingAcquireResult =
                    lease.acquire(ownerId, leaseTime)
                override suspend fun inspect(ownerId: FencingOwnerId): FencingInspectResult = lease.inspect(ownerId)
                override suspend fun renew(
                    ownerId: FencingOwnerId,
                    token: FencingToken,
                    leaseTime: Duration,
                ): FencingRenewResult = lease.renew(ownerId, token, leaseTime)
                override suspend fun release(ownerId: FencingOwnerId, token: FencingToken): FencingReleaseResult =
                    lease.release(ownerId, token)
            }
        },
    )

    private fun verifyClusterRound(attempts: Collection<ClusterAttempt>): ClusterWinner {
        attempts.size shouldBeEqualTo CLUSTER_CALLERS
        val acquired = attempts.filter { it.result is FencingAcquireResult.Acquired }
        attempts.count { it.result is FencingAcquireResult.Contended } shouldBeEqualTo CLUSTER_CALLERS - 1
        acquired.size shouldBeEqualTo 1
        return acquired.single().let { attempt ->
            ClusterWinner(
                attempt.ownerId,
                attempt.adapterIndex,
                attempt.result.shouldBeInstanceOf<FencingAcquireResult.Acquired>().token,
            )
        }
    }

    private fun newConfig(suffix: String): LettuceFencingLeaseConfig =
        LettuceFencingLeaseConfig(
            "cluster",
            "$suffix-${LettuceTestUtils.randomName().substringAfter(':')}",
            31,
        )

    private data class ClusterAttempt(
        val ownerId: FencingOwnerId,
        val adapterIndex: Int,
        val result: FencingAcquireResult,
    )

    private data class ClusterWinner(
        val ownerId: FencingOwnerId,
        val adapterIndex: Int,
        val token: FencingToken,
    )

    private object SplitSlotWireCodec : RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer =
            encode(if (key.endsWith(":lease")) "wire:{one}" else "wire:{two}")
        override fun encodeValue(value: String): ByteBuffer = encode(value)

        private fun encode(value: String): ByteBuffer =
            ByteBuffer.wrap(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(bytes: ByteBuffer): String = bytes.duplicate().let { copy ->
            ByteArray(copy.remaining()).also(copy::get).toString(StandardCharsets.UTF_8)
        }
    }

    private companion object {
        const val CLUSTER_CALLERS: Int = 8
        const val CLUSTER_ROUNDS: Int = 10
        val LEASE_TIME: Duration = Duration.ofSeconds(10)
        val LONGER_LEASE: Duration = Duration.ofSeconds(30)

        @BeforeAll
        @JvmStatic
        fun warmRedisCluster() {
            RedisClusterServer.Launcher.redisCluster.isRunning.shouldBeTrue()
        }
    }
}
