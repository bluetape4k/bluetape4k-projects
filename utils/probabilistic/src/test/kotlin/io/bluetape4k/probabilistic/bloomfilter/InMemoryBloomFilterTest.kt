package io.bluetape4k.probabilistic.bloomfilter

import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.Serializable

class InMemoryBloomFilterTest {

    @Test
    fun `삽입한 원소는 항상 포함 가능 상태가 된다`() {
        val filter = bloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)

        val values = (1..1_000).map { "value-$it" }
        values.forEach { filter.put(it).shouldBeTrue() }

        values.all { filter.mightContain(it) }.shouldBeTrue()
        filter.bitCount shouldBeGreaterThan 0L
        filter.approximateElementCount() shouldBeGreaterThan 0L
    }

    @Test
    fun `clear 는 모든 bit 를 초기화한다`() {
        val filter = bloomFilter<String>(expectedInsertions = 100L, fpp = 0.01)
        filter.put("alpha")

        filter.isEmpty.shouldBeFalse()
        filter.clear()

        filter.isEmpty.shouldBeTrue()
        filter.mightContain("alpha").shouldBeFalse()
    }

    @Test
    fun `관측 오탐률은 설정값을 과도하게 넘지 않는다`() {
        val filter = bloomFilter<String>(expectedInsertions = 10_000L, fpp = 0.01)
        val inserted = (1..10_000).map { "inserted-$it" }
        inserted.forEach { filter.put(it) }

        val falsePositives = (1..20_000).count { filter.mightContain("missing-$it") }
        val observedFpp = falsePositives.toDouble() / 20_000.0

        observedFpp shouldBeLessThan 0.03
        filter.expectedFpp() shouldBeLessThan 0.03
    }

    @Test
    fun `putAll 은 호환 가능한 필터를 병합한다`() {
        val left = mutableBloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)
        val right = mutableBloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)

        left.put("left")
        right.put("right")

        left.putAll(right)

        left.mightContain("left").shouldBeTrue()
        left.mightContain("right").shouldBeTrue()
    }

    @Test
    fun `putAll 은 호환되지 않는 필터를 거부한다`() {
        val left = mutableBloomFilter<String>(expectedInsertions = 1_000L, fpp = 0.01)
        val right = mutableBloomFilter<String>(expectedInsertions = 2_000L, fpp = 0.01)

        assertThrows<IllegalArgumentException> {
            left.putAll(right)
        }
    }

    @Test
    fun `기본 hasher 는 다양한 타입을 지원한다`() {
        val intFilter = bloomFilter<Int>(expectedInsertions = 100L, fpp = 0.01)
        val longFilter = bloomFilter<Long>(expectedInsertions = 100L, fpp = 0.01)
        val bytesFilter = bloomFilter<ByteArray>(expectedInsertions = 100L, fpp = 0.01)
        val objectFilter = bloomFilter<TestPayload>(expectedInsertions = 100L, fpp = 0.01)

        intFilter.put(42)
        longFilter.put(42L)
        bytesFilter.put(byteArrayOf(1, 2, 3))
        objectFilter.put(TestPayload(1, "alpha"))

        intFilter.mightContain(42).shouldBeTrue()
        longFilter.mightContain(42L).shouldBeTrue()
        bytesFilter.mightContain(byteArrayOf(1, 2, 3)).shouldBeTrue()
        objectFilter.mightContain(TestPayload(1, "alpha")).shouldBeTrue()
    }

    data class TestPayload(
        val id: Int,
        val name: String,
    ): Serializable
}
