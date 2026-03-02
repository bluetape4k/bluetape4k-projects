package io.bluetape4k.bucket4j

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConfigurationBuilder

/**
 * DSL 블록으로 Bucket4j [BucketConfiguration]을 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `BucketConfiguration.builder()`를 생성해 [builder]를 적용합니다.
 * - [builder]에서 추가한 bandwidth 제한이 최종 구성에 포함됩니다.
 * - 호출마다 새 [BucketConfiguration] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val cfg = bucketConfiguration {
 *   addBandwidth { Bandwidth.simple(10, java.time.Duration.ofSeconds(1)) }
 * }
 * // cfg.bandwidths.size == 1
 * ```
 */
inline fun bucketConfiguration(builder: ConfigurationBuilder.() -> Unit): BucketConfiguration =
    BucketConfiguration.builder().apply(builder).build()


/**
 * [ConfigurationBuilder]에 bandwidth 제한을 하나 추가합니다.
 *
 * ## 동작/계약
 * - [supplier]를 호출해 생성한 [Bandwidth]를 `addLimit`으로 등록합니다.
 * - 빌더 자체를 반환해 체이닝을 지원합니다.
 * - supplier 예외는 그대로 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val cfg = bucketConfiguration {
 *   addBandwidth { Bandwidth.simple(100, java.time.Duration.ofMinutes(1)) }
 * }
 * // cfg.bandwidths.first().capacity == 100
 * ```
 */
inline fun ConfigurationBuilder.addBandwidth(supplier: () -> Bandwidth) = apply {
    addLimit(supplier())
}
