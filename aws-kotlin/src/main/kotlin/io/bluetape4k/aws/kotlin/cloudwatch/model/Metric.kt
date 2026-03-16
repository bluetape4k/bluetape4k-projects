package io.bluetape4k.aws.kotlin.cloudwatch.model

import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.model.StandardUnit

/**
 * DSL 블록으로 [MetricDatum]을 빌드합니다.
 *
 * ```kotlin
 * val datum = metricDatum {
 *     metricName = "Latency"
 *     value = 100.0
 *     unit = StandardUnit.Milliseconds
 * }
 * ```
 */
inline fun metricDatum(
    @BuilderInference crossinline builder: MetricDatum.Builder.() -> Unit,
): MetricDatum =
    MetricDatum { builder() }

/**
 * 메트릭 이름과 값으로 [MetricDatum]을 생성합니다.
 *
 * ```kotlin
 * val datum = metricDatumOf(
 *     metricName = "Latency",
 *     value = 100.0,
 *     unit = StandardUnit.Milliseconds
 * )
 * ```
 *
 * @param metricName 메트릭 이름
 * @param value 메트릭 값
 * @param unit 메트릭 단위. 기본값은 [StandardUnit.None]입니다.
 * @param builder [MetricDatum.Builder]에 대한 추가 설정 람다
 * @return [MetricDatum] 인스턴스
 */
inline fun metricDatumOf(
    metricName: String,
    value: Double,
    unit: StandardUnit = StandardUnit.None,
    @BuilderInference crossinline builder: MetricDatum.Builder.() -> Unit = {},
): MetricDatum = metricDatum {
    this.metricName = metricName
    this.value = value
    this.unit = unit
    builder()
}
