package io.bluetape4k.cache.nearcache.jcache;

import javax.cache.CacheManager;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;

/** 현재 no-arg와 6/7-인자 NearJCacheConfig ABI, copy, policy getter를 검증하는 소비자 fixture입니다. */
public final class CurrentNearJCacheConfigConsumer {

    private CurrentNearJCacheConfigConsumer() {
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

    public static NearJCacheConfig<String, String> constructWithSevenArguments(
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout,
        int syncRemoteRetryCount,
        BulkFrontPopulationPolicy bulkFrontPopulationPolicy
    ) {
        return new NearJCacheConfig<>(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout,
            syncRemoteRetryCount,
            bulkFrontPopulationPolicy
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

    public static NearJCacheConfig<String, String> copyWithSevenArguments(
        NearJCacheConfig<String, String> source,
        Factory<CacheManager> cacheManagerFactory,
        String cacheName,
        MutableConfiguration<String, String> frontCacheConfiguration,
        boolean isSynchronous,
        long syncRemoteTimeout,
        int syncRemoteRetryCount,
        BulkFrontPopulationPolicy bulkFrontPopulationPolicy
    ) {
        return source.copy(
            cacheManagerFactory,
            cacheName,
            frontCacheConfiguration,
            isSynchronous,
            syncRemoteTimeout,
            syncRemoteRetryCount,
            bulkFrontPopulationPolicy
        );
    }

    public static BulkFrontPopulationPolicy policy(NearJCacheConfig<String, String> config) {
        return config.getBulkFrontPopulationPolicy();
    }
}
