package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceSuspendCuckooFilterTest: AbstractLettuceTest() {

    private lateinit var cuckooFilter: LettuceSuspendCuckooFilter

    @BeforeEach
    fun setup() = runTest {
        cuckooFilter = LettuceSuspendCuckooFilter(
            LettuceClients.connect(client, StringCodec.UTF8),
            "scf-${randomName()}",
            CuckooFilterOptions(capacity = 1000L, bucketSize = 4),
        )
        cuckooFilter.tryInit()
    }

    @AfterEach
    fun teardown() = cuckooFilter.close()

    @Test
    fun `insert - contains true`() = runTest {
        cuckooFilter.insert("hello").shouldBeTrue()
        cuckooFilter.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() = runTest {
        cuckooFilter.contains("ghost").shouldBeFalse()
    }

    @Test
    fun `delete - 삭제 후 false`() = runTest {
        cuckooFilter.insert("world")
        cuckooFilter.delete("world").shouldBeTrue()
        cuckooFilter.contains("world").shouldBeFalse()
    }

    @Test
    fun `delete - 없는 원소 삭제 시 false`() = runTest {
        cuckooFilter.delete("none").shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() = runTest {
        cuckooFilter.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() = runTest {
        val other = LettuceSuspendCuckooFilter(
            LettuceClients.connect(client, StringCodec.UTF8),
            cuckooFilter.filterName,
            CuckooFilterOptions(capacity = 2048L, bucketSize = 8),
        )
        other.use {
            assertThrows<IllegalStateException> { it.tryInit() }
        }
    }
}
