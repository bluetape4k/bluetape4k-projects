package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendHyperLogLogTest : AbstractLettuceTest() {

    private lateinit var hll: LettuceSuspendHyperLogLog<String>

    @BeforeEach
    fun setup() {
        hll = LettuceSuspendHyperLogLog(client.connect(StringCodec.UTF8), "shll-${randomName()}")
    }

    @AfterEach
    fun teardown() = hll.close()

    @Test
    fun `add - count`() = runTest {
        hll.add("x", "y", "z").shouldBeTrue()
        // HLL 근사: ±1 허용
        hll.count() shouldBeInRange 2L..4L
    }

    @Test
    fun `countWith 두 HLL 합산`() = runTest {
        val hll2 = LettuceSuspendHyperLogLog(client.connect(StringCodec.UTF8), "shll2-${randomName()}")
        hll2.use {
            hll.add("a", "b")
            it.add("c", "d")
            hll.countWith(it) shouldBeInRange 3L..5L
        }
    }
}
