package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@OptIn(ExperimentalCoroutinesApi::class)
internal class LettuceSuspendMultiKeyLeaseTest : MultiKeyLeaseContract() {

    private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    override val commands: RedisCommands<String, String> by lazy { connection.sync() }

    private val lease by lazy { LettuceSuspendMultiKeyLease(connection) }

    override val adapters: List<MultiKeyLeaseAdapter> by lazy {
        listOf(suspendAdapter(lease))
    }

    @Test
    fun `standalone and cluster constructors reject corrupted config before command dispatch`() {
        val corrupted = corruptedConfig()
        val standaloneAsync = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.async() } returns standaloneAsync
        every { standalone.codec } returns StringCodec.UTF8

        assertFailsWith<IllegalArgumentException> { LettuceSuspendMultiKeyLease(standalone, corrupted) }
        verify { standaloneAsync wasNot Called }

        val clusterAsync = mockk<RedisAdvancedClusterAsyncCommands<String, String>>(relaxed = true)
        val cluster = mockk<StatefulRedisClusterConnection<String, String>>()
        every { cluster.async() } returns clusterAsync
        every { cluster.codec } returns StringCodec.UTF8

        assertFailsWith<IllegalArgumentException> { LettuceSuspendMultiKeyLease(cluster, corrupted) }
        verify { clusterAsync wasNot Called }
    }

    @Test
    fun `suspend methods validate before command dispatch`() = runTest {
        val async = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.async() } returns async
        every { standalone.codec } returns StringCodec.UTF8
        val target = LettuceSuspendMultiKeyLease(standalone)

        assertFailsWith<IllegalArgumentException> {
            target.acquire(listOf("lease:{validation}:one"), "owner", Duration.ZERO)
        }
        assertFailsWith<MultiKeyLeaseCrossSlotException> {
            target.inspect(listOf("lease:{one}:a", "lease:{two}:b"), "owner")
        }

        verify { async wasNot Called }
    }

    @Test
    fun `suspend cancellation cancels the pending RedisFuture`() = runTest {
        val pending = TestRedisFuture<List<Long>>()
        val async = mockk<RedisAsyncCommands<String, String>>()
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.async() } returns async
        every { standalone.codec } returns StringCodec.UTF8
        every {
            async.evalsha<List<Long>>(
                any<String>(),
                any<ScriptOutputType>(),
                any<Array<String>>(),
                *anyVararg<String>(),
            )
        } returns pending
        val target = LettuceSuspendMultiKeyLease(standalone)

        val job = launch {
            target.acquire(listOf("lease:{cancel}:one"), "owner", Duration.ofSeconds(5))
        }
        runCurrent()
        job.cancelAndJoin()

        pending.isCancelled.shouldBeTrue()
        verify(exactly = 1) {
            async.evalsha<List<Long>>(
                any<String>(),
                any<ScriptOutputType>(),
                any<Array<String>>(),
                *anyVararg<String>(),
            )
        }
    }

    private fun corruptedConfig(): LettuceMultiKeyLeaseConfig = LettuceMultiKeyLeaseConfig().also { config ->
        LettuceMultiKeyLeaseConfig::class.java.getDeclaredField("maxKeys").apply {
            isAccessible = true
            setInt(config, 0)
        }
    }

    private fun suspendAdapter(target: LettuceSuspendMultiKeyLease): MultiKeyLeaseAdapter =
        object : MultiKeyLeaseAdapter {
            override val name: String = "suspend"
            override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                target.acquire(keys, ownerToken, leaseTime)
            override suspend fun inspect(keys: Collection<String>, ownerToken: String) = target.inspect(keys, ownerToken)
            override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
                target.renew(keys, ownerToken, leaseTime)
            override suspend fun release(keys: Collection<String>, ownerToken: String) = target.release(keys, ownerToken)
        }

    private class TestRedisFuture<T>: CompletableFuture<T>(), RedisFuture<T> {
        override fun getError(): String? = if (isCompletedExceptionally) "completed exceptionally" else null

        override fun await(timeout: Long, unit: TimeUnit): Boolean = try {
            get(timeout, unit)
            true
        } catch (_: TimeoutException) {
            false
        }
    }
}
