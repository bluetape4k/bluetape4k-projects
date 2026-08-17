package io.bluetape4k.cache.nearcache.jcache;

import io.bluetape4k.cache.nearcache.jcache.management.NearJCacheConfigurationMXBean;

/**
 * 기존 consumer가 컴파일한 configuration MXBean 구현체를 흉내 내는 fixture입니다.
 *
 * 새 getter를 구현하지 않아도 Kotlin JVM default method가 기본 토큰을 제공해야 합니다.
 */
public final class LegacyNearJCacheConfigurationMXBean implements NearJCacheConfigurationMXBean {

    @Override
    public String getKeyType() {
        return String.class.getName();
    }

    @Override
    public String getValueType() {
        return String.class.getName();
    }

    @Override
    public boolean isReadThrough() {
        return false;
    }

    @Override
    public boolean isWriteThrough() {
        return false;
    }

    @Override
    public boolean isStoreByValue() {
        return true;
    }

    @Override
    public boolean isStatisticsEnabled() {
        return false;
    }

    @Override
    public boolean isManagementEnabled() {
        return false;
    }

    @Override
    public String getTypeResolutionSource() {
        return "legacy-fixture";
    }

    @Override
    public boolean isTypeResolutionExact() {
        return false;
    }

    @Override
    public String getBulkFrontPopulationPolicy() {
        return "DISABLED";
    }

    @Override
    public int getBulkFrontPopulationMaximumEntryCount() {
        return 0;
    }
}
