package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class CoordinationKeyspaceTest {

    private companion object: KLogging() {
        fun encode(value: String): ByteBuffer =
            ByteBuffer.wrap(value.toUtf8Bytes())

        fun decode(bytes: ByteBuffer): String =
            bytes.toByteArray().toUtf8String()

        fun ByteBuffer.toByteArray(): ByteArray =
            duplicate().let { copy -> ByteArray(copy.remaining()).also(copy::get) }
    }

    @Test
    fun `derived keys share the codec wire-byte Redis slot`() {
        val keyspace = CoordinationKeyspace(
            objectKind = "lock",
            name = "inventory",
            codec = StringCodec.UTF8,
        )
        val keys = listOf(
            keyspace.stateKey,
            keyspace.key("generation"),
            keyspace.key("holds"),
            keyspace.key("terminal"),
        )

        keyspace.requireSameSlot(keys) shouldBeEqualTo keyspace.slot
        keys.all { SlotHash.getSlot(StringCodec.UTF8.encodeKey(it)) == keyspace.slot }.shouldBeTrue()
    }

    @Test
    fun `slot proof uses encoded key bytes instead of source text`() {
        val keyspace = CoordinationKeyspace(
            objectKind = "lock",
            name = "wire-sensitive",
            codec = RewritingWireCodec,
        )
        val encoded = RewritingWireCodec.encodeKey(keyspace.stateKey).toByteArray()

        encoded shouldNotBeEqualTo keyspace.stateKey.toUtf8Bytes()
        keyspace.slot shouldBeEqualTo SlotHash.getSlot(RewritingWireCodec.encodeKey(keyspace.stateKey))
        keyspace.requireSameSlot(listOf(keyspace.stateKey, keyspace.key("generation"))) shouldBeEqualTo keyspace.slot
    }

    @Test
    fun `split codec wire slots are rejected without exposing raw names`() {
        val keyspace = CoordinationKeyspace(
            objectKind = "lock",
            name = "secret-customer-lock",
            codec = SplitSlotWireCodec,
        )

        val failure = assertFailsWith<IllegalArgumentException> {
            keyspace.requireSameSlot(listOf(keyspace.stateKey, keyspace.key("generation")))
        }

        failure.message shouldNotContain "secret-customer-lock"
        failure.message shouldContain keyspace.fingerprint
    }

    @Test
    fun `keyspace validates version kind name and suffix`() {
        assertFailsWith<IllegalArgumentException> {
            CoordinationKeyspace("Lock", "inventory", StringCodec.UTF8)
        }
        assertFailsWith<IllegalArgumentException> {
            CoordinationKeyspace("lock", " ", StringCodec.UTF8)
        }
        assertFailsWith<IllegalArgumentException> {
            CoordinationKeyspace("lock", "inventory", StringCodec.UTF8, version = 0)
        }

        val keyspace = CoordinationKeyspace("lock", "inventory", StringCodec.UTF8)
        assertFailsWith<IllegalArgumentException> {
            keyspace.key("raw:{slot}")
        }
        keyspace.stateKey shouldBeEqualTo "bt4k:coord:v1:{inventory}:lock:inventory:state"
        keyspace.fingerprint shouldNotContain "inventory"
    }

    private object RewritingWireCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer =
            encode("wire:{encoded-coordination-slot}:$key")

        override fun encodeValue(value: String): ByteBuffer = encode(value)
    }

    private object SplitSlotWireCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = decode(bytes)
        override fun decodeValue(bytes: ByteBuffer): String = decode(bytes)
        override fun encodeKey(key: String): ByteBuffer =
            encode(if (key.endsWith(":generation")) "wire:{two}:$key" else "wire:{one}:$key")

        override fun encodeValue(value: String): ByteBuffer = encode(value)
    }
}
