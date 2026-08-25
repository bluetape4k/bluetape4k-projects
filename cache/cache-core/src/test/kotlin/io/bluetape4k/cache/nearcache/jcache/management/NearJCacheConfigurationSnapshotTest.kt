package io.bluetape4k.cache.nearcache.jcache.management

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.nearcache.jcache.BulkFrontPopulationPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.cache.CacheException
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import javax.cache.Cache as JCache

class NearJCacheConfigurationSnapshotTest {

    @Test
    fun `actual front의 concrete pair를 exact type으로 사용한다`() {
        val actualFront = configuration<String, Long>(readThrough = true, writeThrough = true)
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(actualFront),
            suppliedFront = configuration<UUID, Double>(),
            actualBack = cacheOf(configuration<Int, Boolean>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.keyType shouldBeEqualTo "java.lang.String"
        snapshot.valueType shouldBeEqualTo "java.lang.Long"
        snapshot.typeResolutionSource shouldBeEqualTo NearJCacheTypeResolutionSource.ACTUAL_FRONT
        snapshot.typeResolutionExact.shouldBeTrue()
        snapshot.readThrough.shouldBeTrue()
        snapshot.writeThrough.shouldBeTrue()
        snapshot.storeByValue.shouldBeFalse()
        snapshot.statisticsEnabled.shouldBeTrue()
        snapshot.managementEnabled.shouldBeTrue()
    }

    @Test
    fun `Object pair인 actual front는 supplied front pair로 fallback한다`() {
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(configuration<Any, Any>()),
            suppliedFront = configuration<String, Long>(),
            actualBack = cacheOf(configuration<Int, Boolean>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.keyType shouldBeEqualTo "java.lang.String"
        snapshot.valueType shouldBeEqualTo "java.lang.Long"
        snapshot.typeResolutionSource shouldBeEqualTo NearJCacheTypeResolutionSource.SUPPLIED_FRONT
        snapshot.typeResolutionExact.shouldBeFalse()
    }

    @Test
    fun `Object pair인 supplied front는 actual back pair로 fallback한다`() {
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(configuration<Any, Any>()),
            suppliedFront = configuration<Any, Any>(),
            actualBack = cacheOf(configuration<UUID, Long>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.keyType shouldBeEqualTo "java.util.UUID"
        snapshot.valueType shouldBeEqualTo "java.lang.Long"
        snapshot.typeResolutionSource shouldBeEqualTo NearJCacheTypeResolutionSource.ACTUAL_BACK
        snapshot.typeResolutionExact.shouldBeFalse()
    }

    @Test
    fun `모든 source가 Object pair이면 unresolved Object pair를 사용한다`() {
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(configuration<Any, Any>()),
            suppliedFront = configuration<Any, Any>(),
            actualBack = cacheOf(configuration<Any, Any>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.keyType shouldBeEqualTo "java.lang.Object"
        snapshot.valueType shouldBeEqualTo "java.lang.Object"
        snapshot.typeResolutionSource shouldBeEqualTo NearJCacheTypeResolutionSource.UNRESOLVED_OBJECT
        snapshot.typeResolutionExact.shouldBeFalse()
    }

    @Test
    fun `서로 다른 source의 partial type을 pair로 혼합하지 않는다`() {
        val actualFront = partialConfiguration(String::class.java, Any::class.java)
        val suppliedFront = partialConfiguration(Any::class.java, Long::class.javaObjectType)
        val actualBack = partialConfiguration(UUID::class.java, Any::class.java)

        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(actualFront),
            suppliedFront = suppliedFront,
            actualBack = cacheOf(actualBack),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.keyType shouldBeEqualTo "java.lang.Object"
        snapshot.valueType shouldBeEqualTo "java.lang.Object"
        snapshot.typeResolutionSource shouldBeEqualTo NearJCacheTypeResolutionSource.UNRESOLVED_OBJECT
    }

    @Test
    fun `actual front가 CompleteConfiguration을 지원하지 않으면 flag를 false로 고정한다`() {
        val actualFront = cacheOf(
            configuration = configuration<String, Long>(),
            completeConfigurationFailure = IllegalArgumentException("unsupported"),
        )

        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = actualFront,
            suppliedFront = configuration<String, Long>(),
            actualBack = cacheOf(configuration<String, Long>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.readThrough.shouldBeFalse()
        snapshot.writeThrough.shouldBeFalse()
        snapshot.statisticsEnabled.shouldBeFalse()
        snapshot.managementEnabled.shouldBeFalse()
        snapshot.storeByValue.shouldBeFalse()
    }

    @Test
    fun `requested configuration class 이외의 runtime failure는 원인 그대로 전파한다`() {
        val failures = listOf(
            IllegalStateException("closed"),
            CacheException("provider failure"),
            SecurityException("denied"),
            UnsupportedOperationException("unexpected"),
        )

        failures.forEach { failure ->
            val error = assertFailsWith<RuntimeException> {
                nearJCacheConfigurationSnapshot(
                    actualFront = cacheOf(
                        configuration = configuration<String, Long>(),
                        completeConfigurationFailure = failure,
                    ),
                    suppliedFront = configuration<String, Long>(),
                    actualBack = cacheOf(configuration<String, Long>()),
                    bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
                )
            }

            (error === failure).shouldBeTrue()
        }
    }

    @Test
    fun `snapshot은 construction 이후 mutable configuration flag 변경의 영향을 받지 않는다`() {
        val actualFront = configuration<String, Long>(readThrough = true, writeThrough = true)
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(actualFront),
            suppliedFront = configuration<UUID, Double>(),
            actualBack = cacheOf(configuration<Int, Boolean>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        actualFront
            .setReadThrough(false)
            .setWriteThrough(false)
            .setStatisticsEnabled(false)
            .setManagementEnabled(false)

        snapshot.keyType shouldBeEqualTo "java.lang.String"
        snapshot.valueType shouldBeEqualTo "java.lang.Long"
        snapshot.readThrough.shouldBeTrue()
        snapshot.writeThrough.shouldBeTrue()
        snapshot.statisticsEnabled.shouldBeTrue()
        snapshot.managementEnabled.shouldBeTrue()
    }

    @Test
    fun `BypassFront 정책은 stable token과 적용 불가 limit로 snapshot한다`() {
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(configuration<String, Long>()),
            suppliedFront = configuration<String, Long>(),
            actualBack = cacheOf(configuration<String, Long>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.BypassFront,
        )

        snapshot.bulkFrontPopulationPolicy shouldBeEqualTo "BYPASS_FRONT"
        snapshot.bulkFrontPopulationMaximumEntryCount shouldBeEqualTo 0
    }

    @Test
    fun `PopulateIfAtMost 정책은 stable token과 양수 limit로 snapshot한다`() {
        val snapshot = nearJCacheConfigurationSnapshot(
            actualFront = cacheOf(configuration<String, Long>()),
            suppliedFront = configuration<String, Long>(),
            actualBack = cacheOf(configuration<String, Long>()),
            bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(17),
        )

        snapshot.bulkFrontPopulationPolicy shouldBeEqualTo "POPULATE_IF_AT_MOST"
        snapshot.bulkFrontPopulationMaximumEntryCount shouldBeEqualTo 17
    }

    @Suppress("UNCHECKED_CAST")
    private fun partialConfiguration(
        keyType: Class<*>,
        valueType: Class<*>,
    ): MutableConfiguration<Any, Any> = configuration<Any, Any>()
        .setTypes(keyType as Class<Any>, valueType as Class<Any>)

    private inline fun <reified K: Any, reified V: Any> configuration(
        readThrough: Boolean = false,
        writeThrough: Boolean = false,
    ): MutableConfiguration<K, V> = MutableConfiguration<K, V>()
        .setTypes(K::class.java, V::class.java)
        .setStoreByValue(false)
        .setReadThrough(readThrough)
        .setWriteThrough(writeThrough)
        .setStatisticsEnabled(true)
        .setManagementEnabled(true)

    @Suppress("UNCHECKED_CAST")
    private fun <K: Any, V: Any> cacheOf(
        configuration: Configuration<K, V>,
        completeConfigurationFailure: RuntimeException? = null,
    ): JCache<K, V> {
        val cache = mockk<JCache<K, V>>()
        val configurationClass = Configuration::class.java as Class<Configuration<K, V>>
        val completeConfigurationClass =
            CompleteConfiguration::class.java as Class<CompleteConfiguration<K, V>>

        every { cache.getConfiguration(configurationClass) } returns configuration
        if (completeConfigurationFailure == null) {
            every { cache.getConfiguration(completeConfigurationClass) } returns
                    configuration as CompleteConfiguration<K, V>
        } else {
            every { cache.getConfiguration(completeConfigurationClass) } throws completeConfigurationFailure
        }
        return cache
    }
}
