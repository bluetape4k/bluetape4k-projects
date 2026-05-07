package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceHyperLogLogTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var hyperLogLog: LettuceHyperLogLog<String>

    @BeforeEach
    fun setup() {
        hyperLogLog = LettuceHyperLogLog(
            connection,
            "hll-${randomName()}",
        )
    }

    @Test
    fun `add - 새 원소 추가 시 true`() {
        hyperLogLog.add("a", "b", "c").shouldBeTrue()
    }

    @Test
    fun `count - 추가한 원소 수 근사값 반환`() {
        hyperLogLog.add("a", "b", "c", "a")
        hyperLogLog.count() shouldBeInRange 2L..4L
    }

    @Test
    fun `countWith - 두 HLL 합산 카운트`() {
        val other = LettuceHyperLogLog(
            connection,
            "hll2-${randomName()}",
        )
        hyperLogLog.add("a", "b")
        other.add("c", "d")
        hyperLogLog.countWith(other) shouldBeInRange 3L..5L
    }

    @Test
    fun `mergeWith - dest에 병합`() {
        val other = LettuceHyperLogLog(
            connection,
            "hll2-${randomName()}",
        )
        val destination = "merged-${randomName()}"

        hyperLogLog.add("a", "b")
        other.add("c", "d")
        hyperLogLog.mergeWith(destination, other)

        val merged = LettuceHyperLogLog(
            connection,
            destination,
        )
        merged.count() shouldBeGreaterOrEqualTo 1L
    }
}
