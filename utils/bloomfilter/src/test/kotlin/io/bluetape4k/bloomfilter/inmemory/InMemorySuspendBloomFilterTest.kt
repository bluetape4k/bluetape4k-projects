package io.bluetape4k.bloomfilter.inmemory

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class InMemorySuspendBloomFilterTest {

    companion object: KLoggingChannel() {
        private const val REPEAT_SIZE = 5
        private const val ITEM_COUNT = 100
    }

    private val bloomFilter = InMemorySuspendBloomFilter<String>()

    @BeforeEach
    fun beforeEach() {
        runBlocking {
            bloomFilter.clear()
        }
    }

    @Test
    fun `get bit size of bloom filter`() = runTest {
        log.debug { "maximum size=${bloomFilter.m}, hash function count=${bloomFilter.k}" }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `verify not exists`() = runTest {
        val values = List(ITEM_COUNT) { Fakers.fixedString(256) }
            .onEach { bloomFilter.add(it) }

        values.all { bloomFilter.contains(it) }.shouldBeTrue()

        bloomFilter.contains("not-exists").shouldBeFalse()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `verify not exists random string`() = runTest {
        val values = List(10 * ITEM_COUNT) { Fakers.fixedString(256) }
        val testValues = List(ITEM_COUNT) { Fakers.fixedString(256) }

        values.forEach { bloomFilter.add(it) }
        values.all { bloomFilter.contains(it) }.shouldBeTrue()

        testValues.filterNot { values.contains(it) }
            .any { bloomFilter.contains(it) }
            .shouldBeFalse()
    }
}
