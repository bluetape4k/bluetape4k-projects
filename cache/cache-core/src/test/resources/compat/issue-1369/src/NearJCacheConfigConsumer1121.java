package io.bluetape4k.cache.nearcache.jcache.compat.issue1369;

import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;

/** 1.12.1 Java consumer: public no-arg, direct five-arg constructor, and copy. */
final class NearJCacheConfigConsumer1121Java {

    private NearJCacheConfigConsumer1121Java() {
    }

    public static NearJCacheConfig<String, String> constructWithNoArguments() {
        return new NearJCacheConfig<>();
    }

    public static NearJCacheConfig<String, String> constructWithFiveArguments(
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout
    ) {
        return new NearJCacheConfig<>(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout
        );
    }

    public static NearJCacheConfig<String, String> copyWithFiveArguments(
        NearJCacheConfig<String, String> source,
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout
    ) {
        return source.copy(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout
        );
    }
}
