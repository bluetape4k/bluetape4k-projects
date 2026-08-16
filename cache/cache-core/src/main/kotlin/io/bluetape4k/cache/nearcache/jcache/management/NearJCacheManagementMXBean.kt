package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.cache.nearcache.jcache.NearJCache

/**
 * [NearJCache] 생성 시점의 immutable configuration을 노출합니다.
 *
 * bean은 cache/front/back reference를 보유하지 않으며, runtime configuration 변경은
 * 이미 생성된 bean의 값에 반영되지 않습니다.
 */
class NearJCacheManagementMXBean private constructor(
    private val snapshot: NearJCacheConfigurationSnapshot,
): NearJCacheConfigurationMXBean {

    constructor(cache: NearJCache<*, *>) : this(cache.configurationSnapshot)

    companion object {
        @JvmSynthetic
        internal fun fromSnapshot(snapshot: NearJCacheConfigurationSnapshot): NearJCacheManagementMXBean =
            NearJCacheManagementMXBean(snapshot)
    }

    override fun getKeyType(): String = snapshot.keyType

    override fun getValueType(): String = snapshot.valueType

    override fun getTypeResolutionSource(): String = snapshot.typeResolutionSource.name

    override fun isTypeResolutionExact(): Boolean = snapshot.typeResolutionExact

    /** bulk `getAll` 결과의 front population 정책을 stable token으로 반환합니다. */
    override fun getBulkFrontPopulationPolicy(): String = snapshot.bulkFrontPopulationPolicy

    /** bulk front population에 허용되는 최대 entry 수를 반환하며, 적용 불가이면 `0`입니다. */
    override fun getBulkFrontPopulationMaximumEntryCount(): Int =
        snapshot.bulkFrontPopulationMaximumEntryCount

    override fun isReadThrough(): Boolean = snapshot.readThrough

    override fun isWriteThrough(): Boolean = snapshot.writeThrough

    override fun isStoreByValue(): Boolean = snapshot.storeByValue

    override fun isStatisticsEnabled(): Boolean = snapshot.statisticsEnabled

    override fun isManagementEnabled(): Boolean = snapshot.managementEnabled
}
