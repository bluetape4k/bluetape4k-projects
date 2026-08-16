package io.bluetape4k.cache.nearcache.jcache.compat.issue1369

import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig

/** Pre-#1369 Kotlin consumer: old six-field default dispatch and copy$default. */
object NearJCacheConfigConsumerPre1369 {
    @JvmStatic
    fun constructWithDefaults(): NearJCacheConfig<String, String> =
        NearJCacheConfig(cacheName = "pre-1369", syncRemoteRetryCount = 2)

    @JvmStatic
    fun copyWithDefaults(source: NearJCacheConfig<String, String>): NearJCacheConfig<String, String> =
        source.copy(syncRemoteRetryCount = 2)
}
