package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.redis.lettuce.lock.internal.deriveMultiLockKeys
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration

internal class MultiLockScriptTest {

    @Test
    fun `same slot distinct bounded names derive one stable ordered group identity`() {
        val config = MultiLockConfig(lock = LockConfig(hashTag = "multi-contract"), maxKeys = 3)
        val first = deriveMultiLockKeys(listOf("two", "one"), config, StringCodec.UTF8)
        val replay = deriveMultiLockKeys(listOf("one", "two"), config, StringCodec.UTF8)

        first.fingerprint shouldBeEqualTo replay.fingerprint
        first.states shouldBeEqualTo replay.states
        assertFailsWith<IllegalArgumentException> {
            deriveMultiLockKeys(listOf("one", "one"), config, StringCodec.UTF8)
        }
        assertFailsWith<IllegalArgumentException> {
            deriveMultiLockKeys(listOf("one", "two", "three", "four"), config, StringCodec.UTF8)
        }
        deriveMultiLockKeys(
            listOf("one", "two"),
            MultiLockConfig(lock = LockConfig(), maxKeys = 3),
            StringCodec.UTF8,
        ).states.size shouldBeEqualTo 2
        assertFailsWith<IllegalArgumentException> {
            deriveMultiLockKeys(listOf("one", "two"), config, CrossSlotWireCodec)
        }
    }

    @Test
    fun `partial state and changed constituent set fail closed without mutation`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val config = MultiLockConfig(lock = LockConfig(hashTag = "multi-partial-${System.nanoTime()}"))
            val firstKeys = deriveMultiLockKeys(listOf("one", "two"), config, connection.codec)
            val changedKeys = deriveMultiLockKeys(listOf("one", "three"), config, connection.codec)
            val first = LettuceMultiLock.create(connection, listOf("one", "two"), config)
            val changed = LettuceMultiLock.create(connection, listOf("one", "three"), config)
            try {
                connection.sync().del(*(firstKeys.all + changedKeys.all).distinct().toTypedArray())
                val handle = first.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                    .handle

                changed.tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
                connection.sync().exists(changedKeys.states[1]) shouldBeEqualTo 0L
                assertFailsWith<IllegalArgumentException> { changed.inspect(handle) }

                first.release(handle) shouldBeEqualTo LockMutationResult.Released(0)
                connection.sync().hset(firstKeys.states[0], "owner", OWNER_1.value)
                first.tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
                connection.sync().exists(firstKeys.states[1]) shouldBeEqualTo 0L
            } finally {
                first.close()
                changed.close()
                connection.sync().del(*(firstKeys.all + changedKeys.all).distinct().toTypedArray())
            }
        }
    }

    @Test
    fun `persistent complete same owner state fails closed`() {
        LettuceTestUtils.client.connect(StringCodec.UTF8).use { connection ->
            val config = MultiLockConfig(lock = LockConfig(hashTag = "multi-persistent-${System.nanoTime()}"))
            val keys = deriveMultiLockKeys(listOf("one", "two"), config, connection.codec)
            val lock = LettuceMultiLock.create(connection, listOf("one", "two"), config)
            try {
                connection.sync().del(*keys.all.toTypedArray())
                lock.tryAcquire(OWNER_1, REQUEST_1, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.Acquired<MultiLockHandle>>()
                keys.states.forEach(connection.sync()::persist)
                connection.sync().persist(keys.holds)

                lock.tryAcquire(OWNER_1, REQUEST_2, LEASE)
                    .shouldBeInstanceOf<LockAcquireResult.IntegrityFailure>()
            } finally {
                lock.close()
                connection.sync().del(*keys.all.toTypedArray())
            }
        }
    }

    private object CrossSlotWireCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = StringCodec.UTF8.decodeKey(bytes)

        override fun decodeValue(bytes: ByteBuffer): String = StringCodec.UTF8.decodeValue(bytes)

        override fun encodeKey(key: String): ByteBuffer {
            val rewritten = when {
                key.endsWith(":one:state") -> key.replace("{multi-contract}", "{slot-one}")
                key.endsWith(":two:state") -> key.replace("{multi-contract}", "{slot-two}")
                else -> key
            }
            return ByteBuffer.wrap(rewritten.toByteArray(StandardCharsets.UTF_8))
        }

        override fun encodeValue(value: String): ByteBuffer = StringCodec.UTF8.encodeValue(value)
    }

    private companion object {
        val OWNER_1 = LockOwnerId.from("multi-owner-1")
        val REQUEST_1 = LockRequestId.from("multi-request-1")
        val REQUEST_2 = LockRequestId.from("multi-request-2")
        val LEASE = LeasePolicy.Fixed(Duration.ofSeconds(3))
    }
}
