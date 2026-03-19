package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.bloomfilter.AbstractBloomFilterTest
import io.bluetape4k.bloomfilter.DEFAULT_ERROR_RATE
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class InMemoryMutableBloomFilterEdgeCaseTest: AbstractBloomFilterTest() {

    companion object: KLogging()

    private val bloomFilter = InMemoryMutableBloomFilter(
        2_000_000L,
        DEFAULT_ERROR_RATE
    )

    @BeforeEach
    fun beforeEach() {
        bloomFilter.clear()
    }

    @Test
    fun `빈 MutableBloomFilter 검증`() {
        bloomFilter.isEmpty.shouldBeTrue()
        bloomFilter.contains("any-value").shouldBeFalse()
        bloomFilter.approximateCount("any-value") shouldBeEqualTo 0
    }

    @Test
    fun `clear 후 MutableBloomFilter 검증`() {
        val values = List(100) { Base58.randomString(16) }.distinct()
        values.forEach { bloomFilter.add(it) }

        bloomFilter.isEmpty.shouldBeFalse()

        bloomFilter.clear()

        bloomFilter.isEmpty.shouldBeTrue()
        values.all { bloomFilter.contains(it) }.shouldBeFalse()
    }

    @Test
    fun `동일 원소 여러 번 추가 시 approximateCount 증가`() {
        val value = Base58.randomString(16)

        bloomFilter.add(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 1

        bloomFilter.add(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 2

        bloomFilter.add(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 3
    }

    @Test
    fun `존재하지 않는 원소 remove 시 아무 동작 안 함`() {
        bloomFilter.remove("non-existent")
        bloomFilter.isEmpty.shouldBeTrue()
    }

    @Test
    fun `remove 후 approximateCount 감소`() {
        val value = Base58.randomString(16)

        bloomFilter.add(value)
        bloomFilter.add(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 2

        bloomFilter.remove(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 1

        bloomFilter.remove(value)
        bloomFilter.approximateCount(value) shouldBeEqualTo 0
        bloomFilter.contains(value).shouldBeFalse()
    }

    @Test
    fun `blank 값에 대한 예외 검증`() {
        assertFailsWith<IllegalArgumentException> {
            bloomFilter.remove("")
        }
        assertFailsWith<IllegalArgumentException> {
            bloomFilter.remove("   ")
        }
        assertFailsWith<IllegalArgumentException> {
            bloomFilter.approximateCount("")
        }
        assertFailsWith<IllegalArgumentException> {
            bloomFilter.approximateCount("   ")
        }
    }

    @Test
    fun `toString 출력이 공백 구분 버킷값 형식`() {
        // 작은 필터로 toString 검증 (큰 m은 OOM 유발)
        val smallFilter = InMemoryMutableBloomFilter(100L, 0.01)
        smallFilter.add("hello")

        val result = smallFilter.toString()
        val parts = result.split(" ")
        parts.all { it.toLongOrNull() != null }.shouldBeTrue()
        parts.all { it.toLong() >= 0 }.shouldBeTrue()
        // 추가된 원소가 있으므로 일부 버킷은 0이 아님
        parts.any { it.toLong() > 0 }.shouldBeTrue()
    }

    @Test
    fun `count 메서드는 m 값을 반환`() {
        bloomFilter.count() shouldBeEqualTo bloomFilter.m.toLong()
    }
}
