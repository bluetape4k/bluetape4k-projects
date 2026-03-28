package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceHyperLogLogTest: AbstractLettuceTest() {

    private lateinit var hyperLogLog: LettuceHyperLogLog<String>

    @BeforeEach
    fun setup() {
        hyperLogLog = LettuceHyperLogLog(
            LettuceClients.connect(client, StringCodec.UTF8),
            "hll-${randomName()}",
        )
    }

    @AfterEach
    fun teardown() = hyperLogLog.close()

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
            LettuceClients.connect(client, StringCodec.UTF8),
            "hll2-${randomName()}",
        )
        other.use {
            hyperLogLog.add("a", "b")
            it.add("c", "d")
            hyperLogLog.countWith(it) shouldBeInRange 3L..5L
        }
    }

    @Test
    fun `mergeWith - dest에 병합`() {
        val other = LettuceHyperLogLog(
            LettuceClients.connect(client, StringCodec.UTF8),
            "hll2-${randomName()}",
        )
        val destination = "merged-${randomName()}"

        other.use {
            hyperLogLog.add("a", "b")
            it.add("c", "d")
            hyperLogLog.mergeWith(destination, it)
        }

        val merged = LettuceHyperLogLog(
            LettuceClients.connect(client, StringCodec.UTF8),
            destination,
        )
        merged.use {
            it.count() shouldBeGreaterOrEqualTo 1L
        }
    }
}
