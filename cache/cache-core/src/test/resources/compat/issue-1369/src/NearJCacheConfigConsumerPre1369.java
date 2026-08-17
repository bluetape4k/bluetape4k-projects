package io.bluetape4k.cache.nearcache.jcache.compat.issue1369;

import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig;
import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;

/** Pre-#1369 Java consumer: public no-arg, direct six-arg constructor, and copy. */
final class NearJCacheConfigConsumerPre1369Java {

    private NearJCacheConfigConsumerPre1369Java() {
    }

    public static NearJCacheConfig<String, String> constructWithNoArguments() {
        return new NearJCacheConfig<>();
    }

    public static NearJCacheConfig<String, String> constructWithSixArguments(
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout,
        int syncRemoteRetryCount
    ) {
        return new NearJCacheConfig<>(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout,
            syncRemoteRetryCount
        );
    }

    public static NearJCacheConfig<String, String> copyWithSixArguments(
        NearJCacheConfig<String, String> source,
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout,
        int syncRemoteRetryCount
    ) {
        return source.copy(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout,
            syncRemoteRetryCount
        );
    }
}
