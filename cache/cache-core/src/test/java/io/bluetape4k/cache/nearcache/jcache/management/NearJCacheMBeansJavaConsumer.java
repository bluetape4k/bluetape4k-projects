package io.bluetape4k.cache.nearcache.jcache.management;

import io.bluetape4k.cache.nearcache.jcache.NearJCache;

import javax.management.MBeanServer;

final class NearJCacheMBeansJavaConsumer {
    private NearJCacheMBeansJavaConsumer() {
    }

    static NearJCacheMBeanRegistration register(
            NearJCache<?, ?> cache,
            MBeanServer server,
            String managerId,
            String cacheId
    ) {
        return NearJCacheMBeans.registerMBeans(cache, server, managerId, cacheId);
    }
}
