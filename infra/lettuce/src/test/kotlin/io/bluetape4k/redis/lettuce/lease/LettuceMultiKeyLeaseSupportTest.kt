package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContentEqual
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.assertions.shouldNotContain
import io.lettuce.core.cluster.SlotHash
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration

class LettuceMultiKeyLeaseSupportTest {

    @Test
    fun `validation preserves order and validates optional ttl`() {
        val keys = listOf("ticket:{sale}:ip:a", "ticket:{sale}:user:b")

        validateLeaseInput(keys, "owner", Duration.ofMillis(10), LettuceMultiKeyLeaseConfig(), StringCodec.UTF8)
            .shouldBeEqualTo(ValidatedLeaseInput(keys, 10L))
        validateLeaseInput(keys, "owner", null, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8)
            .shouldBeEqualTo(ValidatedLeaseInput(keys, null))
    }

    @Test
    fun `validation rejects invalid keys token and ttl`() {
        val valid = listOf("a:{x}")
        val invalidCalls = listOf<() -> Unit>(
            { validateLeaseInput(emptyList(), "owner", null, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(listOf(" "), "owner", null, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(listOf("a:{x}", "a:{x}"), "owner", null, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(valid, " ", null, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(valid, "owner", Duration.ZERO, LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(valid, "owner", Duration.ofMillis(-1), LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
            { validateLeaseInput(valid, "owner", Duration.ofNanos(1), LettuceMultiKeyLeaseConfig(), StringCodec.UTF8) },
        )

        invalidCalls.forEach { call -> assertFailsWith<IllegalArgumentException> { call() } }
        assertFailsWith<ArithmeticException> {
            validateLeaseInput(
                valid,
                "owner",
                Duration.ofSeconds(Long.MAX_VALUE),
                LettuceMultiKeyLeaseConfig(),
                StringCodec.UTF8,
            )
        }
    }

    @Test
    fun `bounded iteration ignores hostile collection size`() {
        val oversized = hostileCollection(listOf("a:{x}", "b:{x}", "c:{x}"))
        assertFailsWith<IllegalArgumentException> {
            validateLeaseInput(oversized, "owner", null, LettuceMultiKeyLeaseConfig(2), StringCodec.UTF8)
        }

        val small = hostileCollection(listOf("a:{x}", "b:{x}"))
        validateLeaseInput(small, "owner", null, LettuceMultiKeyLeaseConfig(Int.MAX_VALUE), StringCodec.UTF8).keys
            .shouldContentEqual(listOf("a:{x}", "b:{x}"))
    }

    @Test
    fun `validation uses encoded wire bytes for slots and preserves order`() {
        val keys = listOf("a:{one}", "b:{two}")
        val codec = NonUtf8SameSlotCodec

        SlotHash.getSlot(StringCodec.UTF8.encodeKey(keys[0])) shouldNotBeEqualTo
            SlotHash.getSlot(StringCodec.UTF8.encodeKey(keys[1]))
        SlotHash.getSlot(codec.encodeKey(keys[0])) shouldBeEqualTo SlotHash.getSlot(codec.encodeKey(keys[1]))
        validateLeaseInput(keys, "owner", null, LettuceMultiKeyLeaseConfig(), codec).keys shouldContentEqual keys
    }

    @Test
    fun `validation reports only the distinct cross slot count`() {
        val error = assertFailsWith<MultiKeyLeaseCrossSlotException> {
            validateLeaseInput(
                listOf("a:{one}", "b:{two}"),
                "owner-secret",
                null,
                LettuceMultiKeyLeaseConfig(),
                StringCodec.UTF8,
            )
        }
        error.distinctSlotCount shouldBeEqualTo 2
        error.message shouldNotContain "owner-secret"
        error.message shouldNotContain "a:{one}"
    }

    @Test
    fun `decoders map every valid status`() {
        decodeAcquire(vector(10, 2, 0, 2, 0)) shouldBeEqualTo MultiKeyAcquireResult.Acquired
        decodeAcquire(vector(11, 2, 2, 0, 0, minPttl = 9)) shouldBeEqualTo MultiKeyAcquireResult.AlreadyOwned(9)
        decodeAcquire(vector(12, 2, 1, 1, 0)) shouldBeEqualTo
            MultiKeyAcquireResult.PartialOwnership(counts(2, 1, 1, 0))
        decodeAcquire(vector(13, 2, 0, 1, 1)) shouldBeEqualTo
            MultiKeyAcquireResult.Conflicted(counts(2, 0, 1, 1))

        decodeInspect(vector(20, 2, 2, 0, 0, minPttl = 8)) shouldBeEqualTo MultiKeyInspectResult.Owned(8)
        decodeInspect(vector(21, 2, 0, 2, 0)) shouldBeEqualTo MultiKeyInspectResult.Lost
        decodeInspect(vector(22, 2, 1, 1, 0)) shouldBeEqualTo
            MultiKeyInspectResult.PartialOwnership(counts(2, 1, 1, 0))
        decodeInspect(vector(23, 2, 0, 1, 1)) shouldBeEqualTo
            MultiKeyInspectResult.Conflicted(counts(2, 0, 1, 1))

        decodeRenew(vector(40, 2, 2, 0, 0)) shouldBeEqualTo MultiKeyRenewResult.Renewed
        decodeRenew(vector(41, 2, 1, 1, 0)) shouldBeEqualTo MultiKeyRenewResult.PartialLoss(counts(2, 1, 1, 0))
        decodeRenew(vector(42, 2, 0, 2, 0)) shouldBeEqualTo MultiKeyRenewResult.Lost
        decodeRenew(vector(43, 3, 0, 1, 2)) shouldBeEqualTo
            MultiKeyRenewResult.OwnershipMismatch(counts(3, 0, 1, 2))

        decodeRelease(vector(50, 2, 2, 0, 0)) shouldBeEqualTo MultiKeyReleaseResult.Released
        decodeRelease(vector(51, 2, 1, 1, 0)) shouldBeEqualTo MultiKeyReleaseResult.PartialRelease(counts(2, 1, 1, 0))
        decodeRelease(vector(52, 2, 0, 2, 0)) shouldBeEqualTo MultiKeyReleaseResult.Lost
        decodeRelease(vector(53, 3, 0, 1, 2)) shouldBeEqualTo
            MultiKeyReleaseResult.OwnershipMismatch(counts(3, 0, 1, 2))
    }

    @Test
    fun `integrity status throws only for supported operations and positive invalid ttl`() {
        val acquire = assertFailsWith<MultiKeyLeaseIntegrityException> {
            decodeAcquire(vector(90, 2, 2, 0, 0, invalidTtl = 1))
        }
        acquire.operation shouldBeEqualTo MultiKeyLeaseOperation.ACQUIRE
        acquire.requestedKeyCount shouldBeEqualTo 2
        acquire.invalidLeaseKeyCount shouldBeEqualTo 1

        assertFailsWith<MultiKeyLeaseIntegrityException> {
            decodeInspect(vector(90, 2, 2, 0, 0, invalidTtl = 2))
        }.operation shouldBeEqualTo MultiKeyLeaseOperation.INSPECT
        assertFailsWith<MultiKeyLeaseIntegrityException> {
            decodeRenew(vector(90, 2, 2, 0, 0, invalidTtl = 1))
        }.operation shouldBeEqualTo MultiKeyLeaseOperation.RENEW

        assertFailsWith<IllegalStateException> { decodeAcquire(vector(90, 1, 1, 0, 0)) }
        assertFailsWith<IllegalStateException> { decodeRelease(vector(90, 1, 1, 0, 0, invalidTtl = 1)) }
    }

    @Test
    fun `decoders reject malformed vectors and invalid numeric fields`() {
        val invalid = listOf(
            emptyList(),
            listOf(40L, 1L, 1L, 0L, 0L, 0L),
            vector(999, 1, 1, 0, 0),
            vector(40, -1, 0, 0, 0),
            vector(40, 0, 0, 0, 0),
            listOf(40L, Int.MAX_VALUE.toLong() + 1, 0L, 0L, 0L, 0L, -1L),
            vector(40, 3, 1, 1, 0),
            vector(40, 2, 1, 1, 0, invalidTtl = 2),
        )
        invalid.forEach { value -> assertFailsWith<IllegalStateException> { decodeRenew(value) } }

        val overflowProne = listOf(40L, Int.MAX_VALUE.toLong(), Int.MAX_VALUE.toLong(), 1L, 0L, 0L, -1L)
        assertFailsWith<IllegalStateException> { decodeRenew(overflowProne) }

        (1..5).forEach { index ->
            val negative = vector(40, 1, 1, 0, 0).toMutableList().also { it[index] = -1L }
            val tooLarge = vector(40, 1, 1, 0, 0).toMutableList().also {
                it[index] = Int.MAX_VALUE.toLong() + 1
            }
            assertFailsWith<IllegalStateException> { decodeRenew(negative) }
            assertFailsWith<IllegalStateException> { decodeRenew(tooLarge) }
        }
    }

    @Test
    fun `decoders enforce ttl and status count shapes`() {
        val invalid = listOf<() -> Unit>(
            { decodeAcquire(vector(11, 1, 1, 0, 0, minPttl = 0)) },
            { decodeInspect(vector(20, 1, 1, 0, 0, minPttl = -1)) },
            { decodeRenew(vector(40, 1, 1, 0, 0, minPttl = 1)) },
            { decodeAcquire(vector(10, 1, 1, 0, 0)) },
            { decodeAcquire(vector(12, 2, 0, 2, 0)) },
            { decodeAcquire(vector(13, 2, 1, 1, 0)) },
            { decodeInspect(vector(21, 2, 1, 1, 0)) },
            { decodeInspect(vector(22, 2, 0, 2, 0)) },
            { decodeRenew(vector(41, 2, 0, 2, 0)) },
            { decodeRenew(vector(42, 2, 1, 1, 0)) },
            { decodeRenew(vector(43, 2, 1, 1, 0)) },
            { decodeRelease(vector(51, 2, 0, 2, 0)) },
            { decodeRelease(vector(52, 2, 1, 1, 0)) },
            { decodeRelease(vector(53, 2, 1, 1, 0)) },
        )
        invalid.forEach { call -> assertFailsWith<IllegalStateException> { call() } }
    }

    private fun hostileCollection(values: List<String>): Collection<String> = object: AbstractCollection<String>() {
        override val size: Int = Int.MAX_VALUE
        override fun iterator(): Iterator<String> = values.iterator()
    }

    private fun counts(requested: Int, owned: Int, missing: Int, mismatched: Int) =
        MultiKeyLeaseCounts(requested, owned, missing, mismatched)

    private fun vector(
        status: Int,
        requested: Int,
        owned: Int,
        missing: Int,
        mismatched: Int,
        invalidTtl: Int = 0,
        minPttl: Long = -1,
    ): List<Long> = listOf(
        status.toLong(), requested.toLong(), owned.toLong(), missing.toLong(), mismatched.toLong(),
        invalidTtl.toLong(), minPttl,
    )

    private object NonUtf8SameSlotCodec: RedisCodec<String, String> {
        override fun decodeKey(bytes: ByteBuffer): String = String(read(bytes), StandardCharsets.ISO_8859_1)
        override fun decodeValue(bytes: ByteBuffer): String = String(read(bytes), StandardCharsets.ISO_8859_1)
        override fun encodeKey(key: String): ByteBuffer =
            ByteBuffer.wrap("wire:{same}:$key".toByteArray(StandardCharsets.UTF_16BE))
        override fun encodeValue(value: String): ByteBuffer =
            ByteBuffer.wrap(value.toByteArray(StandardCharsets.ISO_8859_1))

        private fun read(source: ByteBuffer): ByteArray = source.duplicate().let { copy ->
            ByteArray(copy.remaining()).also(copy::get)
        }
    }
}
