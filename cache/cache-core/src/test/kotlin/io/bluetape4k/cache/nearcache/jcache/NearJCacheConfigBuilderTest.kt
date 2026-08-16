package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeBlank
import org.junit.jupiter.api.Test

class NearJCacheConfigBuilderTest {

    companion object: KLogging()

    @Test
    fun `기본값으로 NearJCacheConfig 생성`() {
        val config = nearJCacheConfig<String, String> { }

        config.cacheName.shouldNotBeBlank()
        config.isSynchronous shouldBeEqualTo false
        config.syncRemoteTimeout shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT
        config.syncRemoteRetryCount shouldBeEqualTo NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT
        config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.BypassFront
    }

    @Test
    fun `DSL로 커스텀 NearJCacheConfig 생성`() {
        val config = nearJCacheConfig<String, String> {
            cacheName = "test-cache"
            isSynchronous = true
            syncRemoteTimeout = 1000L
            syncRemoteRetryCount = 2
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(3)
        }

        config.cacheName shouldBeEqualTo "test-cache"
        config.isSynchronous shouldBeEqualTo true
        config.syncRemoteTimeout shouldBeEqualTo 1000L
        config.syncRemoteRetryCount shouldBeEqualTo 2
        config.bulkFrontPopulationPolicy shouldBeEqualTo BulkFrontPopulationPolicy.PopulateIfAtMost(3)
    }
}
