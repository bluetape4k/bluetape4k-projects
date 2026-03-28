package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendHyperLogLogTest: AbstractLettuceTest() {

    private lateinit var hyperLogLog: LettuceSuspendHyperLogLog<String>

    @BeforeEach
    fun setup() {
        hyperLogLog = LettuceSuspendHyperLogLog(
            LettuceClients.connect(client, StringCodec.UTF8),
            "shll-${randomName()}",
        )
    }

    @AfterEach
    fun teardown() = hyperLogLog.close()

    @Test
    fun `add - count`() = runTest {
        hyperLogLog.add("x", "y", "z").shouldBeTrue()
        hyperLogLog.count() shouldBeInRange 2L..4L
    }

    @Test
    fun `countWith 두 HLL 합산`() = runTest {
        val other = LettuceSuspendHyperLogLog(
            LettuceClients.connect(client, StringCodec.UTF8),
            "shll2-${randomName()}",
        )
        other.use {
            hyperLogLog.add("a", "b")
            it.add("c", "d")
            hyperLogLog.countWith(it) shouldBeInRange 3L..5L
        }
    }
}
