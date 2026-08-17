package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import org.junit.jupiter.api.Test

class NearJCacheManagementMXBeanTest {

    private val snapshot = NearJCacheConfigurationSnapshot(
        keyType = "java.lang.String",
        valueType = "java.lang.Long",
        typeResolutionSource = NearJCacheTypeResolutionSource.SUPPLIED_FRONT,
        typeResolutionExact = false,
        readThrough = true,
        writeThrough = true,
        storeByValue = false,
        statisticsEnabled = true,
        managementEnabled = true,
        bulkFrontPopulationPolicy = "POPULATE_IF_AT_MOST",
        bulkFrontPopulationMaximumEntryCount = 17,
    )

    @Test
    fun `snapshot의 표준 configuration 속성을 노출한다`() {
        val bean = NearJCacheManagementMXBean.fromSnapshot(snapshot)

        bean.keyType shouldBeEqualTo "java.lang.String"
        bean.valueType shouldBeEqualTo "java.lang.Long"
        bean.isReadThrough.shouldBeTrue()
        bean.isWriteThrough.shouldBeTrue()
        bean.isStoreByValue.shouldBeFalse()
        bean.isStatisticsEnabled.shouldBeTrue()
        bean.isManagementEnabled.shouldBeTrue()
    }

    @Test
    fun `type resolution source와 exact 여부를 노출한다`() {
        val bean = NearJCacheManagementMXBean.fromSnapshot(snapshot)

        bean.getTypeResolutionSource() shouldBeEqualTo "SUPPLIED_FRONT"
        bean.isTypeResolutionExact().shouldBeFalse()
    }

    @Test
    fun `bulk front population 정책의 stable metadata를 노출한다`() {
        val bean = NearJCacheManagementMXBean.fromSnapshot(snapshot)

        bean.getBulkFrontPopulationPolicy() shouldBeEqualTo "POPULATE_IF_AT_MOST"
        bean.getBulkFrontPopulationMaximumEntryCount() shouldBeEqualTo 17
    }

    @Test
    fun `기존 NearJCache public constructor 하나만 유지한다`() {
        val publicConstructors = NearJCacheManagementMXBean::class.java.constructors
            .filterNot { it.isSynthetic }

        publicConstructors.size shouldBeEqualTo 1
        publicConstructors.single().parameterTypes.toList() shouldBeEqualTo listOf(NearJCache::class.java)
    }

    @Test
    fun `snapshot factory는 JVM public surface에 노출하지 않는다`() {
        val factory = NearJCacheManagementMXBean.Companion::class.java.declaredMethods
            .single { it.name.startsWith("fromSnapshot") }

        factory.isSynthetic.shouldBeTrue()
    }
}
