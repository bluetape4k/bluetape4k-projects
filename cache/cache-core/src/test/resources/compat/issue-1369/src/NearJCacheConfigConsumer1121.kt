package io.bluetape4k.cache.nearcache.jcache.compat.issue1369

import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig

/** 1.12.1 Kotlin consumer: old five-field default dispatch and copy$default. */
object NearJCacheConfigConsumer1121 {
    @JvmStatic
    fun constructWithDefaults(): NearJCacheConfig<String, String> =
        NearJCacheConfig(cacheName = "legacy-kotlin")

    @JvmStatic
    fun copyWithDefaults(source: NearJCacheConfig<String, String>): NearJCacheConfig<String, String> =
        source.copy(cacheName = "legacy-kotlin-copy")
}
