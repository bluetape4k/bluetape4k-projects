package io.bluetape4k.bloomfilter.redis

import io.bluetape4k.LibraryName
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class LettuceAsyncBloomFilterTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
        private const val ITEM_COUNT = 100
    }

    private val bloomFilter: LettuceAsyncBloomFilter<String> by lazy {
        LettuceAsyncBloomFilter(connection, "$LibraryName:lettuce:async-bloomfilter:test")
    }

    @BeforeEach
    fun beforeEach() {
        bloomFilter.clearAsync().get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `get bit size of bloom filter`() {
        log.debug { "maximum size=${bloomFilter.m}, hash function count=${bloomFilter.k}" }

        bloomFilter.m shouldBeEqualTo Int.MAX_VALUE
        bloomFilter.k shouldBeEqualTo 1
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `비동기로 원소를 추가하고 포함 여부를 검증한다`() {
        val values = List(ITEM_COUNT) { Base58.randomString(256) }

        // 비동기 추가 후 모두 완료 대기
        val addFutures = values.map { bloomFilter.addAsync(it) }
        addFutures.forEach { it.get(10, TimeUnit.SECONDS) }

        // 비동기 포함 여부 검사
        values.all {
            bloomFilter.containsAsync(it).get(10, TimeUnit.SECONDS) == 1L
        }.shouldBeTrue()

        // 존재하지 않는 값 검사
        (bloomFilter.containsAsync("not-exists").get(10, TimeUnit.SECONDS) == 1L).shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `비동기로 존재하지 않는 원소 검증`() {
        val values = List(10 * ITEM_COUNT) { Base58.randomString(256) }
        val testValues = List(ITEM_COUNT) { Base58.randomString(256) }

        // 비동기 추가
        val addFutures = values.map { bloomFilter.addAsync(it) }
        addFutures.forEach { it.get(10, TimeUnit.SECONDS) }

        // 추가된 원소 검증
        values.all {
            bloomFilter.containsAsync(it).get(10, TimeUnit.SECONDS) == 1L
        }.shouldBeTrue()

        // 추가되지 않은 원소 검증
        testValues.filterNot { values.contains(it) }
            .any { bloomFilter.containsAsync(it).get(10, TimeUnit.SECONDS) == 1L }
            .shouldBeFalse()
    }

    @Test
    fun `비동기로 clear 후 비어있는지 검증`() {
        bloomFilter.addAsync("test-item").get(10, TimeUnit.SECONDS)
        bloomFilter.isEmpty.shouldBeFalse()

        bloomFilter.clearAsync().get(10, TimeUnit.SECONDS)
        bloomFilter.isEmpty.shouldBeTrue()
    }

    @Test
    fun `비동기로 bitcount 조회`() {
        bloomFilter.addAsync("item1").get(10, TimeUnit.SECONDS)
        bloomFilter.addAsync("item2").get(10, TimeUnit.SECONDS)

        val count = bloomFilter.countAsync().get(10, TimeUnit.SECONDS)
        (count > 0).shouldBeTrue()
    }
}
