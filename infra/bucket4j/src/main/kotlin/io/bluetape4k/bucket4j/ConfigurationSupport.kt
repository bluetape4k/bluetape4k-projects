package io.bluetape4k.bucket4j

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.ConfigurationBuilder

/**
 * Bucket4j의 [BucketConfiguration]을 생성합니다.
 *
 * ```
 * val configuration = bucketConfiguration {
 *    addBandwidth { Bandwidth.simple(10, Duration.ofSeconds(1)) }
 *    addBandwidth { Bandwidth.simple(100, Duration.ofMinutes(1)) }
 * }
 * ```
 *
 * @param builder [ConfigurationBuilder]를 이용한 초기화 람다
 * @return [BucketConfiguration] 인스턴스
 */
inline fun bucketConfiguration(builder: ConfigurationBuilder.() -> Unit): BucketConfiguration =
    BucketConfiguration.builder().apply(builder).build()


/**
 * Bucket4j 환경설정 빌더([ConfigurationBuilder])에 [Bandwidth]를 추가합니다.
 *
 * ```
 * val configuration = bucketConfiguration {
 *   addBandwidth { Bandwidth.simple(10, Duration.ofSeconds(1)) }
 *   addBandwidth { Bandwidth.simple(100, Duration.ofMinutes(1)) }
 *   addBandwidth { Bandwidth.simple(1000, Duration.ofHours(1)) }
 * }
 * ```
 *
 * @param supplier [Bandwidth] 인스턴스를 생성하는 람다
 * @return [ConfigurationBuilder] 인스턴스
 */
inline fun ConfigurationBuilder.addBandwidth(supplier: () -> Bandwidth) = apply {
    addLimit(supplier())
}
