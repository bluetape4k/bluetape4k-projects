package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.RedisFuture
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.codec.StringCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

internal class LettuceMultiKeyLeaseTest : MultiKeyLeaseContract() {

    private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    override val commands: RedisCommands<String, String> by lazy { connection.sync() }

    private val lease by lazy { LettuceMultiKeyLease(connection) }

    override val adapters: List<MultiKeyLeaseAdapter> by lazy {
        listOf(syncAdapter(lease), asyncAdapter(lease))
    }

    @Test
    fun `standalone and cluster constructors reject corrupted config before command dispatch`() {
        val corrupted = corruptedConfig()
        val standaloneSync = mockk<RedisCommands<String, String>>(relaxed = true)
        val standaloneAsync = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.sync() } returns standaloneSync
        every { standalone.async() } returns standaloneAsync
        every { standalone.codec } returns StringCodec.UTF8

        assertFailsWith<IllegalArgumentException> { LettuceMultiKeyLease(standalone, corrupted) }
        verify { standaloneSync wasNot Called }
        verify { standaloneAsync wasNot Called }

        val clusterSync = mockk<RedisAdvancedClusterCommands<String, String>>(relaxed = true)
        val clusterAsync = mockk<RedisAdvancedClusterAsyncCommands<String, String>>(relaxed = true)
        val cluster = mockk<StatefulRedisClusterConnection<String, String>>()
        every { cluster.sync() } returns clusterSync
        every { cluster.async() } returns clusterAsync
        every { cluster.codec } returns StringCodec.UTF8

        assertFailsWith<IllegalArgumentException> { LettuceMultiKeyLease(cluster, corrupted) }
        verify { clusterSync wasNot Called }
        verify { clusterAsync wasNot Called }
    }

    @Test
    fun `public constructors validate before sync or async dispatch`() {
        val sync = mockk<RedisCommands<String, String>>(relaxed = true)
        val async = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.sync() } returns sync
        every { standalone.async() } returns async
        every { standalone.codec } returns StringCodec.UTF8
        val lease = LettuceMultiKeyLease(standalone)

        assertFailsWith<IllegalArgumentException> { lease.inspect(emptyList(), "owner") }
        assertFailsWith<IllegalArgumentException> {
            lease.acquire(listOf("a:{x}"), "owner", Duration.ZERO)
        }
        assertFailsWith<IllegalArgumentException> { lease.inspectAsync(listOf("a:{x}"), " ") }
        assertFailsWith<MultiKeyLeaseCrossSlotException> {
            lease.releaseAsync(listOf("a:{one}", "b:{two}"), "owner")
        }

        verify { sync wasNot Called }
        verify { async wasNot Called }
    }

    @Test
    fun `future methods throw validation failures before returning a future`() {
        val async = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.sync() } returns mockk(relaxed = true)
        every { standalone.async() } returns async
        every { standalone.codec } returns StringCodec.UTF8
        val lease = LettuceMultiKeyLease(standalone)

        assertFailsWith<IllegalArgumentException> {
            lease.acquireAsync(listOf("lease:{validation}:one"), "owner", Duration.ZERO)
        }

        verify { async wasNot Called }
    }

    @Test
    fun `future methods report post-dispatch failures through exceptional completion`() {
        val failure = IllegalStateException("redis failed after dispatch")
        val pending = TestRedisFuture<List<Long>>().apply { completeExceptionally(failure) }
        val async = mockk<RedisAsyncCommands<String, String>>()
        val standalone = mockk<StatefulRedisConnection<String, String>>()
        every { standalone.sync() } returns mockk(relaxed = true)
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
        val lease = LettuceMultiKeyLease(standalone)

        val returned = lease.inspectAsync(listOf("lease:{dispatch}:one"), "owner")

        assertFailsWith<CompletionException> { returned.join() }.cause shouldBeEqualTo failure
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

    private fun syncAdapter(target: LettuceMultiKeyLease): MultiKeyLeaseAdapter = object : MultiKeyLeaseAdapter {
        override val name: String = "sync"
        override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
            target.acquire(keys, ownerToken, leaseTime)
        override suspend fun inspect(keys: Collection<String>, ownerToken: String) = target.inspect(keys, ownerToken)
        override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
            target.renew(keys, ownerToken, leaseTime)
        override suspend fun release(keys: Collection<String>, ownerToken: String) = target.release(keys, ownerToken)
    }

    private fun asyncAdapter(target: LettuceMultiKeyLease): MultiKeyLeaseAdapter = object : MultiKeyLeaseAdapter {
        override val name: String = "future"
        override suspend fun acquire(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
            target.acquireAsync(keys, ownerToken, leaseTime).joinUnwrapped()
        override suspend fun inspect(keys: Collection<String>, ownerToken: String) =
            target.inspectAsync(keys, ownerToken).joinUnwrapped()
        override suspend fun renew(keys: Collection<String>, ownerToken: String, leaseTime: Duration) =
            target.renewAsync(keys, ownerToken, leaseTime).joinUnwrapped()
        override suspend fun release(keys: Collection<String>, ownerToken: String) =
            target.releaseAsync(keys, ownerToken).joinUnwrapped()
    }

    private fun <T> java.util.concurrent.CompletableFuture<T>.joinUnwrapped(): T = try {
        join()
    } catch (failure: CompletionException) {
        throw failure.cause ?: failure
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
