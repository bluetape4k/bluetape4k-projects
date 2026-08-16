package io.bluetape4k.cache.nearcache.jcache.management

import javax.cache.Cache
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration

internal enum class NearJCacheTypeResolutionSource {
    ACTUAL_FRONT,
    SUPPLIED_FRONT,
    ACTUAL_BACK,
    UNRESOLVED_OBJECT,
}

/** NearJCache 생성 시점의 immutable management configuration입니다. */
internal data class NearJCacheConfigurationSnapshot(
    val keyType: String,
    val valueType: String,
    val typeResolutionSource: NearJCacheTypeResolutionSource,
    val typeResolutionExact: Boolean,
    val readThrough: Boolean,
    val writeThrough: Boolean,
    val storeByValue: Boolean,
    val statisticsEnabled: Boolean,
    val managementEnabled: Boolean,
)

/**
 * 실제 front, caller가 공급한 front, 실제 back 순으로 하나의 온전한 타입 pair를 선택합니다.
 *
 * provider가 requested configuration class를 지원하지 않는다는 [IllegalArgumentException]만
 * unavailable로 취급하며, provider lifecycle/security failure는 호출자에게 그대로 전파합니다.
 */
internal fun nearJCacheConfigurationSnapshot(
    actualFront: Cache<*, *>,
    suppliedFront: Configuration<*, *>,
    actualBack: Cache<*, *>,
): NearJCacheConfigurationSnapshot {
    val actualFrontConfiguration = actualFront.configurationOrNull()
    val (typePair, source, exact) = sequenceOf(
        TypeCandidate(actualFrontConfiguration, NearJCacheTypeResolutionSource.ACTUAL_FRONT, true),
        TypeCandidate(suppliedFront, NearJCacheTypeResolutionSource.SUPPLIED_FRONT, false),
        TypeCandidate(actualBack.configurationOrNull(), NearJCacheTypeResolutionSource.ACTUAL_BACK, false),
    ).mapNotNull { candidate ->
        candidate.configuration?.concreteTypePair()?.let { Triple(it, candidate.source, candidate.exact) }
    }.firstOrNull()
        ?: Triple(
            TypePair(Any::class.java, Any::class.java),
            NearJCacheTypeResolutionSource.UNRESOLVED_OBJECT,
            false,
        )

    val completeConfiguration = actualFront.completeConfigurationOrNull()
    return NearJCacheConfigurationSnapshot(
        keyType = typePair.keyType.name,
        valueType = typePair.valueType.name,
        typeResolutionSource = source,
        typeResolutionExact = exact,
        readThrough = completeConfiguration?.isReadThrough ?: false,
        writeThrough = completeConfiguration?.isWriteThrough ?: false,
        storeByValue = false,
        statisticsEnabled = completeConfiguration?.isStatisticsEnabled ?: false,
        managementEnabled = completeConfiguration?.isManagementEnabled ?: false,
    )
}

private data class TypeCandidate(
    val configuration: Configuration<*, *>?,
    val source: NearJCacheTypeResolutionSource,
    val exact: Boolean,
)

private data class TypePair(
    val keyType: Class<*>,
    val valueType: Class<*>,
)

private fun Configuration<*, *>.concreteTypePair(): TypePair? =
    TypePair(keyType, valueType).takeIf { pair ->
        pair.keyType != Any::class.java && pair.valueType != Any::class.java
    }

@Suppress("UNCHECKED_CAST")
private fun Cache<*, *>.configurationOrNull(): Configuration<*, *>? = try {
    (this as Cache<Any, Any>).getConfiguration(
        Configuration::class.java as Class<Configuration<Any, Any>>,
    )
} catch (_: IllegalArgumentException) {
    null
}

@Suppress("UNCHECKED_CAST")
private fun Cache<*, *>.completeConfigurationOrNull(): CompleteConfiguration<*, *>? = try {
    val requested = (this as Cache<Any, Any>).getConfiguration(
        CompleteConfiguration::class.java as Class<Configuration<Any, Any>>,
    )
    requested as? CompleteConfiguration<*, *>
} catch (_: IllegalArgumentException) {
    null
}
