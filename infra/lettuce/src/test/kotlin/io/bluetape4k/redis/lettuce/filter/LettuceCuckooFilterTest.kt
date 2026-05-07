package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.codec.StringCodec
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class LettuceCuckooFilterTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var cuckooFilter: LettuceCuckooFilter

    @BeforeEach
    fun setup() {
        cuckooFilter = LettuceCuckooFilter(
            connection,
            "cf-${randomName()}",
            CuckooFilterOptions(capacity = 1000L, bucketSize = 4),
        )
        cuckooFilter.tryInit()
    }

    @Test
    fun `CuckooFilterOptions - 잘못된 bucketSize 예외`() {
        assertFailsWith<IllegalArgumentException> { CuckooFilterOptions(bucketSize = 0) }
        assertFailsWith<IllegalArgumentException> { CuckooFilterOptions(bucketSize = 9) }
    }

    @Test
    fun `insert - contains true`() {
        cuckooFilter.insert("hello").shouldBeTrue()
        cuckooFilter.contains("hello").shouldBeTrue()
    }

    @Test
    fun `contains - 없는 원소 false`() {
        cuckooFilter.contains("not-inserted").shouldBeFalse()
    }

    @Test
    fun `delete - 삽입 후 삭제, 이후 contains false`() {
        cuckooFilter.insert("world")
        cuckooFilter.delete("world").shouldBeTrue()
        cuckooFilter.contains("world").shouldBeFalse()
    }

    @Test
    fun `delete - 없는 원소 삭제 시 false`() {
        cuckooFilter.delete("ghost").shouldBeFalse()
    }

    @Test
    fun `tryInit - 이미 초기화된 경우 false`() {
        cuckooFilter.tryInit().shouldBeFalse()
    }

    @Test
    fun `tryInit - 다른 파라미터로 재초기화 시 예외`() {
        val other = LettuceCuckooFilter(
            connection,
            cuckooFilter.filterName,
            CuckooFilterOptions(capacity = 2048L, bucketSize = 8),
        )
        assertFailsWith<IllegalStateException> { other.tryInit() }
    }

    @Test
    fun `count - 삽입 및 삭제에 따른 카운트`() {
        cuckooFilter.insert("a")
        cuckooFilter.insert("b")
        cuckooFilter.insert("a")
        cuckooFilter.count() shouldBeGreaterOrEqualTo 2L

        cuckooFilter.delete("a")
        cuckooFilter.count() shouldBeGreaterOrEqualTo 1L
    }

    @Test
    fun `insert 실패 시 기존 원소 유실 없음`() {
        val smallFilter = LettuceCuckooFilter(
            connection,
            "cf-small-${randomName()}",
            CuckooFilterOptions(capacity = 10L, bucketSize = 2, maxIterations = 5),
        )
        smallFilter.tryInit()
        val inserted = (1..8).filter { index -> smallFilter.insert("item-$index") }
        inserted.forEach { index -> smallFilter.contains("item-$index").shouldBeTrue() }
    }
}
