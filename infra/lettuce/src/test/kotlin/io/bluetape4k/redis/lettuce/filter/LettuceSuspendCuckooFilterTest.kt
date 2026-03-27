package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceSuspendCuckooFilterTest : AbstractLettuceTest() {

    private lateinit var cf: LettuceSuspendCuckooFilter

    @BeforeEach
    fun setup() = runTest {
        cf = LettuceSuspendCuckooFilter(
            client.connect(StringCodec.UTF8),
            "scf-${randomName()}",
            CuckooFilterOptions(capacity = 1000L, bucketSize = 4),
        )
        cf.tryInit()
    }

    @AfterEach
    fun teardown() = cf.close()

    @Test
    fun `insert - contains true`() = runTest {
        cf.insert("hello").shouldBeTrue()
        cf.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() = runTest {
        cf.contains("ghost").shouldBeFalse()
    }

    @Test
    fun `delete - 삭제 후 false`() = runTest {
        cf.insert("world")
        cf.delete("world").shouldBeTrue()
        cf.contains("world").shouldBeFalse()
    }

    @Test
    fun `delete - 없는 원소 삭제 시 false`() = runTest {
        cf.delete("none").shouldBeFalse()
    }
}
