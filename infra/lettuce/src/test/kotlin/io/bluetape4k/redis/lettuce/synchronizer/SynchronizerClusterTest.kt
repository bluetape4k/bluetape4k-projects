package io.bluetape4k.redis.lettuce.synchronizer

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveLatchKeys
import io.bluetape4k.redis.lettuce.synchronizer.internal.deriveSemaphoreKeys
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.codec.RedisCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class SynchronizerClusterTest {

    @Test
    fun `custom codec split slots fail every public family before dispatch`() {
        val sync = mockk<RedisAdvancedClusterCommands<String, String>>(relaxed = true)
        val async = mockk<RedisAdvancedClusterAsyncCommands<String, String>>(relaxed = true)
        val connection = mockk<StatefulRedisClusterConnection<String, String>>()
        every { connection.codec } returns SplitSlotCodec
        every { connection.sync() } returns sync
        every { connection.async() } returns async

        listOf<(StatefulRedisClusterConnection<String, String>) -> Unit>(
            { LettuceDistributedSemaphore.create(it, SECRET) },
            { LettucePermitExpirableSemaphore.create(it, SECRET) },
            { LettuceCountDownLatch.create(it, SECRET) },
        ).forEach { factory ->
            val failure = assertFailsWith<IllegalArgumentException> { factory(connection) }
            failure.message.orEmpty() shouldNotContain SECRET
        }

        verify { sync wasNot Called }
        verify { async wasNot Called }
    }

    @Test
    fun `real cluster accepts same slot synchronizer scripts`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                val suffix = System.nanoTime().toString()
                val semaphoreName = "cluster-semaphore-$suffix"
                val latchName = "cluster-latch-$suffix"
                val semaphoreKeys = deriveSemaphoreKeys(semaphoreName, SemaphoreConfig(), connection.codec)
                val latchKeys = deriveLatchKeys(latchName, LatchConfig(), connection.codec)
                connection.sync().del(*(semaphoreKeys.all + latchKeys.all).toTypedArray())
                try {
                    LettuceDistributedSemaphore.create(connection, semaphoreName).use { semaphore ->
                        semaphore.trySetPermits(1)
                            .shouldBeInstanceOf<SemaphoreInitializationResult.Initialized>()
                        semaphore.tryAcquire(SemaphoreOwnerId.random(), SemaphoreRequestId.random())
                            .shouldBeInstanceOf<PermitAcquireResult.Acquired<PermitHandle>>()
                    }
                    LettuceCountDownLatch.create(connection, latchName).use { latch ->
                        latch.trySetCount(1, LatchRequestId.random())
                            .shouldBeInstanceOf<LatchSetCountResult.Created>()
                    }
                } finally {
                    connection.sync().del(*(semaphoreKeys.all + latchKeys.all).toTypedArray())
                }
            }
        }
    }

    private object SplitSlotCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer {
            val slot = if (key.endsWith(":available") || key.endsWith(":count")) "one" else "two"
            return encode("wire:{$slot}:$key")
        }
        override fun encodeValue(value: String): ByteBuffer = encode(value)
        private fun encode(value: String) = ByteBuffer.wrap(value.toByteArray(StandardCharsets.UTF_8))
        private fun decode(bytes: ByteBuffer): String =
            bytes.duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }
                .toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val SECRET = "secret-synchronizer"
    }
}
