package io.bluetape4k.cache.nearcache.jcache;

import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;

/** prior-release 5-인자 NearJCacheConfig ABI를 컴파일하는 소비자 fixture입니다. */
public final class LegacyNearJCacheConfigConsumer {

    private LegacyNearJCacheConfigConsumer() {
    }

    public static NearJCacheConfig<String, String> construct(
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

    public static NearJCacheConfig<String, String> copy(
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
