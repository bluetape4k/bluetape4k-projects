package io.bluetape4k.aws.cloudwatch.model

import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit

/**
 * DSL 블록으로 [PutMetricDataRequest]를 빌드합니다.
 *
 * ```kotlin
 * val request = putMetricDataRequest {
 *     namespace("MyApp/Performance")
 *     metricData(listOf(metricDatum))
 * }
 * ```
 */
inline fun putMetricDataRequest(
    builder: PutMetricDataRequest.Builder.() -> Unit,
): PutMetricDataRequest =
    PutMetricDataRequest.builder().apply(builder).build()

/**
 * namespace와 metricData 목록으로 [PutMetricDataRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = putMetricDataRequestOf(
 *     namespace = "MyApp/Performance",
 *     metricData = listOf(metricDatum)
 * )
 * ```
 */
inline fun putMetricDataRequestOf(
    namespace: String,
    metricData: List<MetricDatum>,
    builder: PutMetricDataRequest.Builder.() -> Unit = {},
): PutMetricDataRequest = putMetricDataRequest {
    namespace(namespace)
    metricData(metricData)
    builder()
}

/**
 * DSL 블록으로 [MetricDatum]을 빌드합니다.
 *
 * ```kotlin
 * val datum = metricDatum {
 *     metricName("Latency")
 *     value(100.0)
 *     unit(StandardUnit.MILLISECONDS)
 * }
 * ```
 */
inline fun metricDatum(
    builder: MetricDatum.Builder.() -> Unit,
): MetricDatum =
    MetricDatum.builder().apply(builder).build()

/**
 * 메트릭 이름과 값으로 [MetricDatum]을 생성합니다.
 *
 * ```kotlin
 * val datum = metricDatumOf(
 *     metricName = "Latency",
 *     value = 100.0,
 *     unit = StandardUnit.MILLISECONDS
 * )
 * ```
 */
inline fun metricDatumOf(
    metricName: String,
    value: Double,
    unit: StandardUnit = StandardUnit.NONE,
    builder: MetricDatum.Builder.() -> Unit = {},
): MetricDatum = metricDatum {
    metricName(metricName)
    value(value)
    unit(unit)
    builder()
}
