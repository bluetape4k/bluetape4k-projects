package io.bluetape4k.redis.lettuce.filter

import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LettuceCuckooFilterTest: AbstractLettuceTest() {

    private lateinit var cuckooFilter: LettuceCuckooFilter

    @BeforeEach
    fun setup() {
        cuckooFilter = LettuceCuckooFilter(
            LettuceClients.connect(client, StringCodec.UTF8),
            "cf-${randomName()}",
            CuckooFilterOptions(capacity = 1000L, bucketSize = 4),
        )
        cuckooFilter.tryInit()
    }

    @AfterEach
    fun teardown() = cuckooFilter.close()

    @Test
    fun `CuckooFilterOptions - 잘못된 bucketSize 예외`() {
        assertThrows<IllegalArgumentException> { CuckooFilterOptions(bucketSize = 0) }
        assertThrows<IllegalArgumentException> { CuckooFilterOptions(bucketSize = 9) }
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
            LettuceClients.connect(client, StringCodec.UTF8),
            cuckooFilter.filterName,
            CuckooFilterOptions(capacity = 2048L, bucketSize = 8),
        )
        other.use {
            assertThrows<IllegalStateException> { it.tryInit() }
        }
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
            LettuceClients.connect(client, StringCodec.UTF8),
            "cf-small-${randomName()}",
            CuckooFilterOptions(capacity = 10L, bucketSize = 2, maxIterations = 5),
        )
        smallFilter.use {
            it.tryInit()
            val inserted = (1..8).filter { index -> it.insert("item-$index") }
            inserted.forEach { index -> it.contains("item-$index").shouldBeTrue() }
        }
    }
}
