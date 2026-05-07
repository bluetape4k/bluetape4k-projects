package io.bluetape4k.cache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class SuspendJCacheEntryTest {

    companion object: KLogging()

    @Test
    fun `key와 value를 올바르게 반환`() {
        val entry = SuspendJCacheEntry("user:1", 42)
        entry.getKey() shouldBeEqualTo "user:1"
        entry.getValue() shouldBeEqualTo 42
    }

    @Test
    fun `unwrap - 호환 타입이면 인스턴스 반환`() {
        val entry = SuspendJCacheEntry("k", "v")
        entry.unwrap(SuspendJCacheEntry::class.java).shouldNotBeNull()
    }

    @Test
    fun `unwrap - 비호환 타입이면 null 반환`() {
        val entry = SuspendJCacheEntry("k", "v")
        entry.unwrap(String::class.java).shouldBeNull()
    }

    @Test
    fun `data class equals와 copy`() {
        val e1 = SuspendJCacheEntry("k", 1)
        val e2 = SuspendJCacheEntry("k", 1)
        val e3 = e1.copy(entryValue = 2)

        (e1 == e2).shouldBeEqualTo(true)
        (e1 == e3).shouldBeEqualTo(false)
        e3.getValue() shouldBeEqualTo 2
    }
}
