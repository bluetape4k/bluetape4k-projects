package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.AbstractBloomFilterTest
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.Serializable

class InMemoryBloomFilterEdgeCaseTest: AbstractBloomFilterTest() {
    companion object: KLogging() {
        private const val ITEM_COUNT = 100
    }

    private lateinit var bloomFilter: InMemoryBloomFilter<String>

    @BeforeEach
    fun beforeEach() {
        bloomFilter = InMemoryBloomFilter()
        bloomFilter.clear()
    }

    @Test
    fun `빈 BloomFilter 검증`() {
        bloomFilter.isEmpty.shouldBeTrue()
        bloomFilter.contains("any-value").shouldBeFalse()
    }

    @Test
    fun `clear 후 BloomFilter 검증`() {
        val values = List(ITEM_COUNT) { Base58.randomString(32) }
        values.forEach { bloomFilter.add(it) }

        bloomFilter.isEmpty.shouldBeFalse()

        bloomFilter.clear()

        bloomFilter.isEmpty.shouldBeTrue()
        values.all { bloomFilter.contains(it) }.shouldBeFalse()
    }

    @Test
    fun `동일한 값 여러 번 추가`() {
        val value = Base58.randomString(32)

        repeat(10) {
            bloomFilter.add(value)
        }

        bloomFilter.contains(value).shouldBeTrue()
        // BloomFilter는 중복 추가를 감지하지 못함 (정상 동작)
    }

    @Test
    fun `매우 긴 문자열 추가`() {
        val longValue = Base58.randomString(10000)

        bloomFilter.add(longValue)

        bloomFilter.contains(longValue).shouldBeTrue()
    }

    @Test
    fun `특수 문자 포함 문자열`() {
        val specialValues =
            listOf(
                "!@#$%^&*()",
                "<script>alert('xss')</script>",
                "   ",
                "\t\n\r",
                "한글테스트",
                "🎉🎊🎁",
            )

        specialValues.forEach { bloomFilter.add(it) }

        specialValues.all { bloomFilter.contains(it) }.shouldBeTrue()
    }

    @Test
    fun `다양한 타입의 값`() {
        val intFilter = InMemoryBloomFilter<Int>()
        val longFilter = InMemoryBloomFilter<Long>()

        val intValues = (1..100).toList()
        val longValues = (1L..100L).toList()

        intValues.forEach { intFilter.add(it) }
        longValues.forEach { longFilter.add(it) }

        intValues.all { intFilter.contains(it) }.shouldBeTrue()
        longValues.all { longFilter.contains(it) }.shouldBeTrue()
    }

    @Test
    fun `count 메서드 검증`() {
        // count()는 m 값을 반환 (bitset 크기)
        bloomFilter.count() shouldBeEqualTo bloomFilter.m.toLong()
    }

    @Test
    fun `확률 계산 검증`() {
        val n = 1000

        // bit가 0일 확률은 0과 1 사이
        val zeroProb = bloomFilter.getBitZeroProbability(n)
        (zeroProb in 0.0..1.0).shouldBeTrue()

        // false positive 확률은 0과 1 사이
        val fpProb = bloomFilter.getFalsePositiveProbability(n)
        (fpProb in 0.0..1.0).shouldBeTrue()

        // 원소당 bit 수는 양수
        val bitsPerElement = bloomFilter.getBitsPerElement(n)
        (bitsPerElement > 0).shouldBeTrue()
    }

    @Test
    fun `m과 k 값 검증`() {
        // m은 양수
        (bloomFilter.m > 0).shouldBeTrue()

        // k는 양수
        (bloomFilter.k > 0).shouldBeTrue()
    }

    data class TestData(
        val id: Int,
        val name: String,
    ): Serializable

    @Test
    fun `Serializable 객체 사용`() {
        val objectFilter = InMemoryBloomFilter<TestData>()

        val objects = (1..100).map { TestData(it, Base58.randomString(16)) }
        objects.forEach { objectFilter.add(it) }

        objects.all { objectFilter.contains(it) }.shouldBeTrue()
    }
}
