package io.bluetape4k.workshop.redis.cache.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.workshop.redis.cache.AbstractRedisCacheTest
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager

class LettuceRedisCacheConfigurationTest(
    @Autowired private val cacheManager: CacheManager,
): AbstractRedisCacheTest() {

    companion object: KLogging()

    @Test
    fun `context loading`() {
        cacheManager.shouldNotBeNull()
    }

}
