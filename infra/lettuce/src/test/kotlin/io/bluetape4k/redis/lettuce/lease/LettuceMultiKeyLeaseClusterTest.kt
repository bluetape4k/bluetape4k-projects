package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletionException

internal class LettuceMultiKeyLeaseClusterTest {

    @Test
    fun `cluster preserves the lease contract for sync future and suspend callers`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val commands = connection.sync()
                clusterAdapters(connection).forEach { adapter ->
                    val tag = "${adapter.name}-${LettuceTestUtils.randomName()}"
                    val keys = listOf("lease:{$tag}:one", "lease:{$tag}:two")
                    val token = "owner-$tag"
                    try {
                        adapter.acquire(keys, token, FIVE_SECONDS) shouldBeEqualTo MultiKeyAcquireResult.Acquired
                        (adapter.inspect(keys, token) as MultiKeyInspectResult.Owned)
                            .minimumPttlMillis shouldBeGreaterOrEqualTo 1L
                        val beforeReplay = keys.map { commands.pttl(it) }
                        (adapter.acquire(keys, token, TEN_SECONDS) is MultiKeyAcquireResult.AlreadyOwned).shouldBeTrue()
                        keys.zip(beforeReplay).forEach { (key, previousPttl) ->
                            commands.pttl(key) shouldBeLessOrEqualTo previousPttl
                        }
                        adapter.renew(keys, token, TEN_SECONDS) shouldBeEqualTo MultiKeyRenewResult.Renewed
                        adapter.release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released

                        assertFailsWith<MultiKeyLeaseCrossSlotException> {
                            adapter.inspect(
                                listOf("lease:{slot-one}:one", "lease:{slot-two}:two"),
                                token,
                            )
                        }

                        commands.set(keys[0], token)
                        commands.psetex(keys[1], 5_000, token)
                        assertFailsWith<MultiKeyLeaseIntegrityException> {
                            adapter.acquire(keys, token, FIVE_SECONDS)
                        }.operation shouldBeEqualTo MultiKeyLeaseOperation.ACQUIRE
                        assertFailsWith<MultiKeyLeaseIntegrityException> {
                            adapter.inspect(keys, token)
                        }.operation shouldBeEqualTo MultiKeyLeaseOperation.INSPECT
                        assertFailsWith<MultiKeyLeaseIntegrityException> {
                            adapter.renew(keys, token, TEN_SECONDS)
                        }.operation shouldBeEqualTo MultiKeyLeaseOperation.RENEW
                        adapter.release(keys, token) shouldBeEqualTo MultiKeyReleaseResult.Released
                        commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L
                    } finally {
                        commands.del(*keys.toTypedArray())
                    }
                }
            }
        }
    }

    private fun clusterAdapters(
        connection: StatefulRedisClusterConnection<String, String>,
    ): List<MultiKeyLeaseAdapter> {
        val lease = LettuceMultiKeyLease(connection)
        val suspendLease = LettuceSuspendMultiKeyLease(connection)
        return listOf(
            object : MultiKeyLeaseAdapter {
                override val name: String = "cluster-sync"
                override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    lease.acquire(keys, ownerToken, leaseTime)
                override suspend fun inspect(keys: Collection<String>, ownerToken: String) =
                    lease.inspect(keys, ownerToken)
                override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    lease.renew(keys, ownerToken, leaseTime)
                override suspend fun release(keys: Collection<String>, ownerToken: String) =
                    lease.release(keys, ownerToken)
            },
            object : MultiKeyLeaseAdapter {
                override val name: String = "cluster-future"
                override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    lease.acquireAsync(keys, ownerToken, leaseTime).joinUnwrapped()
                override suspend fun inspect(keys: Collection<String>, ownerToken: String) =
                    lease.inspectAsync(keys, ownerToken).joinUnwrapped()
                override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    lease.renewAsync(keys, ownerToken, leaseTime).joinUnwrapped()
                override suspend fun release(keys: Collection<String>, ownerToken: String) =
                    lease.releaseAsync(keys, ownerToken).joinUnwrapped()
            },
            object : MultiKeyLeaseAdapter {
                override val name: String = "cluster-suspend"
                override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    suspendLease.acquire(keys, ownerToken, leaseTime)
                override suspend fun inspect(keys: Collection<String>, ownerToken: String) =
                    suspendLease.inspect(keys, ownerToken)
                override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                    suspendLease.renew(keys, ownerToken, leaseTime)
                override suspend fun release(keys: Collection<String>, ownerToken: String) =
                    suspendLease.release(keys, ownerToken)
            },
        )
    }

    private fun <T> java.util.concurrent.CompletableFuture<T>.joinUnwrapped(): T = try {
        join()
    } catch (failure: CompletionException) {
        throw failure.cause ?: failure
    }

    private companion object {
        val FIVE_SECONDS: Duration = Duration.ofSeconds(5)
        val TEN_SECONDS: Duration = Duration.ofSeconds(10)
    }
}
