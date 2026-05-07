package io.bluetape4k.cache.ehcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import org.ehcache.config.units.EntryUnit
import org.junit.jupiter.api.Test

class EhcacheSupportTest {

    companion object: KLogging()

    private val ehcacheManager = ehcacheManager {}

    @Test
    fun `default cache`() {
        val cache = ehcacheManager.getOrCreateCache<String, String>("default") {
            this.heap(10_000, EntryUnit.ENTRIES)
        }
        cache.containsKey("key").shouldBeFalse()
        cache.get("key").shouldBeNull()

        cache.putIfAbsent("key", "value").shouldBeNull()  // 기존 값을 반환한다
        cache.get("key") shouldBeEqualTo "value"

        cache.clear()
    }
}
