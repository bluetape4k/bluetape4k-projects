package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveFairLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveFencedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveMultiLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveReadWriteLockKeys
import io.bluetape4k.testcontainers.storage.RedisClusterServer
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration

@Timeout(45)
internal class LockClusterTest {

    @Test
    fun `standalone locks keep every derived key on one encoded Redis slot`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            topologyCases().forEach { case ->
                val keys = case.keys(connection.codec)
                try {
                    connection.sync().del(*keys)
                    keys.assertOneWireSlot(connection.codec)
                    case.exerciseStandalone(connection)
                } finally {
                    connection.sync().del(*keys)
                }
            }
        }
    }

    @Test
    fun `redis cluster accepts same-slot lock families`() = runSuspendIO {
        val server = RedisClusterServer.Launcher.redisCluster
        RedisClusterServer.Launcher.LettuceLib.getClusterClient(server).use { client ->
            client.connect().use { connection ->
                topologyCases().forEach { case ->
                    val keys = case.keys(connection.codec)
                    try {
                        connection.sync().del(*keys)
                        keys.assertOneWireSlot(connection.codec)
                        case.exerciseCluster(connection)
                    } finally {
                        connection.sync().del(*keys)
                    }
                }
            }
        }
    }

    @Test
    fun `split wire slots fail public cluster facades before Redis dispatch`() {
        val syncCommands = mockk<RedisAdvancedClusterCommands<String, String>>(relaxed = true)
        val asyncCommands = mockk<RedisAdvancedClusterAsyncCommands<String, String>>(relaxed = true)
        val connection = mockk<StatefulRedisClusterConnection<String, String>>()
        every { connection.sync() } returns syncCommands
        every { connection.async() } returns asyncCommands
        every { connection.codec } returns SplitSlotWireCodec

        crossSlotCases().forEach { case ->
            case.factories.forEach { create ->
                val failure = assertFailsWith<IllegalArgumentException> { create(connection) }
                failure.message shouldBeEqualTo case.message
                failure.message.orEmpty() shouldNotContain SECRET_NAME
                failure.message.orEmpty() shouldNotContain SECRET_HASH_TAG
            }
        }

        verify { syncCommands wasNot Called }
        verify { asyncCommands wasNot Called }
    }

    private fun topologyCases(): List<LockTopologyCase> {
        val lockConfig = LockConfig(hashTag = "topology-${System.nanoTime()}")
        val fairConfig = FairLockConfig(lockConfig)
        val fencedConfig = FencedLockConfig(lockConfig, epoch = 101)
        val readWriteConfig = ReadWriteLockConfig(lockConfig)
        val spinConfig = SpinLockConfig(lock = lockConfig, jitterRatio = 0.0)
        val multiConfig = MultiLockConfig(lock = lockConfig)
        return listOf(
            LockTopologyCase(
                keys = { codec -> deriveDistributedLockKeys("distributed-topology", lockConfig, codec).all },
                exerciseStandalone = { connection ->
                    LettuceDistributedLock.create(connection, "distributed-topology", lockConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.DISTRIBUTED)
                    }
                },
                exerciseCluster = { connection ->
                    LettuceDistributedLock.create(connection, "distributed-topology", lockConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.DISTRIBUTED)
                    }
                },
            ),
            LockTopologyCase(
                keys = { codec -> deriveFairLockKeys("fair-topology", fairConfig, codec).all },
                exerciseStandalone = { connection ->
                    LettuceFairLock.create(connection, "fair-topology", fairConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.FAIR)
                    }
                },
                exerciseCluster = { connection ->
                    LettuceFairLock.create(connection, "fair-topology", fairConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.FAIR)
                    }
                },
            ),
            LockTopologyCase(
                keys = { codec -> deriveFencedLockKeys("fenced-topology", fencedConfig, codec).all },
                exerciseStandalone = { connection ->
                    LettuceFencedLock.create(connection, "fenced-topology", fencedConfig).use { lock ->
                        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                        lock.acquireAndReleaseFenced()
                    }
                },
                exerciseCluster = { connection ->
                    LettuceFencedLock.create(connection, "fenced-topology", fencedConfig).use { lock ->
                        lock.bootstrapFencing() shouldBeEqualTo FencedBootstrapResult.Initialized
                        lock.acquireAndReleaseFenced()
                    }
                },
            ),
            LockTopologyCase(
                keys = { codec -> deriveReadWriteLockKeys("read-write-topology", readWriteConfig, codec).all },
                exerciseStandalone = { connection ->
                    LettuceReadWriteLock.create(connection, "read-write-topology", readWriteConfig).use { lock ->
                        lock.acquireAndReleaseWrite()
                    }
                },
                exerciseCluster = { connection ->
                    LettuceReadWriteLock.create(connection, "read-write-topology", readWriteConfig).use { lock ->
                        lock.acquireAndReleaseWrite()
                    }
                },
            ),
            LockTopologyCase(
                keys = { codec -> deriveDistributedLockKeys("spin-topology", spinConfig.lock, codec).all },
                exerciseStandalone = { connection ->
                    LettuceSpinLock.create(connection, "spin-topology", spinConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.DISTRIBUTED)
                    }
                },
                exerciseCluster = { connection ->
                    LettuceSpinLock.create(connection, "spin-topology", spinConfig).use { lock ->
                        lock.acquireAndRelease(LockKind.DISTRIBUTED)
                    }
                },
            ),
            LockTopologyCase(
                keys = { codec -> deriveMultiLockKeys(MULTI_NAMES, multiConfig, codec).all.toTypedArray() },
                exerciseStandalone = { connection ->
                    LettuceMultiLock.create(connection, MULTI_NAMES, multiConfig).use { lock ->
                        lock.acquireAndReleaseMulti()
                    }
                },
                exerciseCluster = { connection ->
                    LettuceMultiLock.create(connection, MULTI_NAMES, multiConfig).use { lock ->
                        lock.acquireAndReleaseMulti()
                    }
                },
            ),
        )
    }

    private fun crossSlotCases(): List<CrossSlotCase> {
        val lockConfig = LockConfig(hashTag = SECRET_HASH_TAG)
        return listOf(
            CrossSlotCase(
                "Derived distributed lock keys must share one Redis Cluster slot.",
                { connection -> LettuceDistributedLock.create(connection, SECRET_NAME, lockConfig) },
                { connection -> LettuceSuspendDistributedLock.create(connection, SECRET_NAME, lockConfig) },
            ),
            CrossSlotCase(
                "Derived fair lock keys must share one Redis Cluster slot.",
                { connection -> LettuceFairLock.create(connection, SECRET_NAME, FairLockConfig(lockConfig)) },
                { connection -> LettuceSuspendFairLock.create(connection, SECRET_NAME, FairLockConfig(lockConfig)) },
            ),
            CrossSlotCase(
                "Derived fenced lock keys must share one Redis Cluster slot.",
                { connection ->
                    LettuceFencedLock.create(connection, SECRET_NAME, FencedLockConfig(lockConfig, epoch = 102))
                },
                { connection ->
                    LettuceSuspendFencedLock.create(
                        connection,
                        SECRET_NAME,
                        FencedLockConfig(lockConfig, epoch = 102),
                    )
                },
            ),
            CrossSlotCase(
                "Derived read/write lock keys must share one Redis Cluster slot.",
                { connection ->
                    LettuceReadWriteLock.create(connection, SECRET_NAME, ReadWriteLockConfig(lockConfig))
                },
                { connection ->
                    LettuceSuspendReadWriteLock.create(connection, SECRET_NAME, ReadWriteLockConfig(lockConfig))
                },
            ),
            CrossSlotCase(
                "Derived distributed lock keys must share one Redis Cluster slot.",
                { connection ->
                    LettuceSpinLock.create(connection, SECRET_NAME, SpinLockConfig(lock = lockConfig, jitterRatio = 0.0))
                },
                { connection ->
                    LettuceSuspendSpinLock.create(
                        connection,
                        SECRET_NAME,
                        SpinLockConfig(lock = lockConfig, jitterRatio = 0.0),
                    )
                },
            ),
            CrossSlotCase(
                "Derived multi-lock keys must share one Redis Cluster slot.",
                { connection ->
                    LettuceMultiLock.create(
                        connection,
                        listOf(SECRET_NAME, "peer-secret-resource"),
                        MultiLockConfig(lock = lockConfig),
                    )
                },
                { connection ->
                    LettuceSuspendMultiLock.create(
                        connection,
                        listOf(SECRET_NAME, "peer-secret-resource"),
                        MultiLockConfig(lock = lockConfig),
                    )
                },
            ),
        )
    }

    private data class LockTopologyCase(
        val keys: (RedisCodec<String, String>) -> Array<String>,
        val exerciseStandalone: (StatefulRedisConnection<String, String>) -> Unit,
        val exerciseCluster: (StatefulRedisClusterConnection<String, String>) -> Unit,
    )

    private class CrossSlotCase(
        val message: String,
        vararg val factories: (StatefulRedisClusterConnection<String, String>) -> Unit,
    )

    private object SplitSlotWireCodec : RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer {
            val slot = if (key.endsWith(":state") || key.contains(":$SECRET_NAME:state")) {
                "one"
            } else {
                "two"
            }
            return encode("wire:{$slot}:$key")
        }

        override fun encodeValue(value: String): ByteBuffer = encode(value)

        private fun encode(value: String): ByteBuffer =
            ByteBuffer.wrap(value.toByteArray(StandardCharsets.UTF_8))

        private fun decode(bytes: ByteBuffer): String =
            bytes.duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }
                .toString(StandardCharsets.UTF_8)
    }

    companion object {
        const val SECRET_NAME = "secret-resource"
        const val SECRET_HASH_TAG = "secret-hash-tag"
        val LEASE: LeasePolicy = LeasePolicy.Fixed(Duration.ofSeconds(3))
        val OWNER: LockOwnerId = LockOwnerId.from("topology-owner")
        val REQUEST: LockRequestId = LockRequestId.from("topology-request")
        val MULTI_NAMES: List<String> = listOf("multi-topology-one", "multi-topology-two")
    }
}

private fun Array<String>.assertOneWireSlot(
    codec: RedisCodec<String, String>,
) {
    map { SlotHash.getSlot(codec.encodeKey(it)) }.toSet().size shouldBeEqualTo 1
}

private fun LettuceDistributedLock.acquireAndRelease(expectedKind: LockKind) {
    val handle = tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        .handle
    handle.kind shouldBeEqualTo expectedKind
    release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}

private fun LettuceFairLock.acquireAndRelease(expectedKind: LockKind) {
    val handle = tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        .handle
    handle.kind shouldBeEqualTo expectedKind
    release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}

private fun LettuceSpinLock.acquireAndRelease(expectedKind: LockKind) {
    val handle = tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>()
        .handle
    handle.kind shouldBeEqualTo expectedKind
    release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}

private fun LettuceFencedLock.acquireAndReleaseFenced() {
    val handle = tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<FencedLockHandle>>()
        .handle
    handle.lock.kind shouldBeEqualTo LockKind.FENCED
    release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}

private fun LettuceReadWriteLock.acquireAndReleaseWrite() {
    val handle = writeLock().tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<WriteLockHandle>>()
        .handle
    handle.lock.kind shouldBeEqualTo LockKind.WRITE
    writeLock().release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}

private fun LettuceMultiLock.acquireAndReleaseMulti() {
    val handle = tryAcquire(LockClusterTest.OWNER, LockClusterTest.REQUEST, LockClusterTest.LEASE)
        .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
        .handle
    handle.lock.kind shouldBeEqualTo LockKind.MULTI
    handle.constituentCount shouldBeEqualTo LockClusterTest.MULTI_NAMES.size
    release(handle) shouldBeEqualTo LockMutationResult.Released(0)
}
