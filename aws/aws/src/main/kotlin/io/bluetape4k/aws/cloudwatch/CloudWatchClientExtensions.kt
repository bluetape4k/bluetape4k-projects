package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse

/**
 * [namespace]에 [metricData] 목록을 CloudWatch에 게시합니다.
 *
 * ## 동작/계약
 * - [namespace]가 blank이면 `IllegalArgumentException`을 던진다.
 * - [metricData]가 비어있으면 아무 작업도 수행하지 않는다.
 *
 * ```kotlin
 * val response = cloudWatchClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricData = listOf(metricDatum)
 * )
 * response.sdkHttpResponse().statusCode() == 200
 * ```
 */
fun CloudWatchClient.putMetricData(
    namespace: String,
    metricData: List<MetricDatum>,
): PutMetricDataResponse {
    namespace.requireNotBlank("namespace")
    return putMetricData {
        it.namespace(namespace)
        it.metricData(metricData)
    }
}

/**
 * [namespace]에 단일 [metricDatum]을 CloudWatch에 게시합니다.
 *
 * ## 동작/계약
 * - [namespace]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricDatum = MetricDatum.builder().metricName("Latency").value(100.0).build()
 * )
 * ```
 */
fun CloudWatchClient.putMetricData(
    namespace: String,
    metricDatum: MetricDatum,
): PutMetricDataResponse {
    namespace.requireNotBlank("namespace")
    return putMetricData(namespace, listOf(metricDatum))
}

/**
 * [namespace]의 메트릭 목록을 조회합니다.
 *
 * ## 동작/계약
 * - [namespace]가 null이 아닌 경우 해당 namespace의 메트릭만 조회합니다.
 * - [metricName]이 null이 아닌 경우 해당 이름의 메트릭만 조회합니다.
 * - [dimensions]가 null이 아닌 경우 해당 dimensions를 기준으로 필터링합니다.
 *
 * ```kotlin
 * val response = cloudWatchClient.listMetrics(namespace = "MyApp/Performance")
 * response.metrics().forEach { metric -> println(metric.metricName()) }
 * ```
 */
fun CloudWatchClient.listMetrics(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
): ListMetricsResponse =
    listMetrics {
        namespace?.let { ns -> it.namespace(ns) }
        metricName?.let { mn -> it.metricName(mn) }
        dimensions?.let { dims -> it.dimensions(dims) }
    }
