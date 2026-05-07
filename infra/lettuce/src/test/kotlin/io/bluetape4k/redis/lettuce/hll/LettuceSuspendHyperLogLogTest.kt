package io.bluetape4k.redis.lettuce.hll

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendHyperLogLogTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var hyperLogLog: LettuceSuspendHyperLogLog<String>

    @BeforeEach
    fun setup() {
        hyperLogLog = LettuceSuspendHyperLogLog(
            connection,
            "shll-${randomName()}",
        )
    }

    @Test
    fun `add - count`() = runTest {
        hyperLogLog.add("x", "y", "z").shouldBeTrue()
        hyperLogLog.count() shouldBeInRange 2L..4L
    }

    @Test
    fun `countWith 두 HLL 합산`() = runTest {
        val other = LettuceSuspendHyperLogLog(
            connection,
            "shll2-${randomName()}",
        )
        hyperLogLog.add("a", "b")
        other.add("c", "d")
        hyperLogLog.countWith(other) shouldBeInRange 3L..5L
    }
}
