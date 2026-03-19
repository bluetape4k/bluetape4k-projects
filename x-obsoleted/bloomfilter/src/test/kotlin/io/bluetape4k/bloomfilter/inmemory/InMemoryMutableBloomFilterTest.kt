package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.AbstractBloomFilterTest
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class InMemoryMutableBloomFilterTest: AbstractBloomFilterTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private const val ITEM_SIZE = 1000
    }

    private val bloomFilter = InMemoryMutableBloomFilter(
        2_000_000L,
        DEFAULT_ERROR_RATE
    )

    @BeforeEach
    fun beforeEach() {
        bloomFilter.clear()
    }

    @Test
    fun `get bit size of bloom filter`() {
        log.debug { "maximum size=${bloomFilter.m}, hash function count=${bloomFilter.k}" }
    }

    @Test
    fun `invalid argument checking`() {
        assertFailsWith<IllegalArgumentException> {
            bloomFilter.add("")
        }

        assertFailsWith<IllegalArgumentException> {
            bloomFilter.contains("")
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `요소가 존재하지 않음을 검증`() {
        val items = List(ITEM_SIZE) { Base58.randomString(16) }.distinct()
        items.forEach {
            bloomFilter.add(it)
        }

        // 기존 Item이 존재하는지 검증
        items.all { bloomFilter.contains(it) }.shouldBeTrue()

        // 기존 Item이 아닌 값이 존재하지 않는지 검증
        bloomFilter.contains("not-exists").shouldBeFalse()
    }


    @RepeatedTest(REPEAT_SIZE)
    fun `remove items`() {
        val expectedItem = Base58.randomString(16)
        val tailItems = List(ITEM_SIZE) { Base58.randomString(16) }.distinct()

        // 하나의 요소 등록 작업 
        bloomFilter.add(expectedItem)
        bloomFilter.contains(expectedItem).shouldBeTrue()
        bloomFilter.contains("not-exists").shouldBeFalse()

        // 추가된 요소들은 모두 존재하는지 검증
        tailItems.forEach { bloomFilter.add(it) }
        tailItems.all { bloomFilter.contains(it) }.shouldBeTrue()

        // 기존 요소 제거
        bloomFilter.remove(expectedItem)
        bloomFilter.contains(expectedItem).shouldBeFalse()

        bloomFilter.isEmpty.shouldBeFalse()

        // 모든 요소 제거 검증
        tailItems.forEach { bloomFilter.remove(it) }
        tailItems.any { bloomFilter.contains(it) }.shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `get approximate count`() {
        val expectedItem = Base58.randomString(16)
        val tailItems = List(ITEM_SIZE) { Base58.randomString(16) }.distinct()

        // 하나의 요소 등록 작업
        bloomFilter.add(expectedItem)
        bloomFilter.contains(expectedItem).shouldBeTrue()
        bloomFilter.contains("not-exists").shouldBeFalse()

        // 추가된 요소들은 모두 존재하는지 검증
        tailItems.forEach { bloomFilter.add(it) }
        tailItems.all { bloomFilter.contains(it) }.shouldBeTrue()

        // Approximate Count 검증
        bloomFilter.approximateCount(expectedItem) shouldBeEqualTo 1
        tailItems.all { bloomFilter.approximateCount(it) == 1 }.shouldBeTrue()

        bloomFilter.approximateCount("not-exists") shouldBeEqualTo 0
    }
}
