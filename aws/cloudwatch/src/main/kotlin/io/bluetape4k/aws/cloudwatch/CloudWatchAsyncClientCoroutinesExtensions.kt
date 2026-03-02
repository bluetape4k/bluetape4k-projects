package io.bluetape4k.aws.cloudwatch

import kotlinx.coroutines.future.await
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse

/**
 * [namespace]에 [metricData] 목록을 CloudWatch에 코루틴으로 게시합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [putMetricDataAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricData = listOf(metricDatum)
 * )
 * ```
 */
suspend fun CloudWatchAsyncClient.putMetricData(
    namespace: String,
    metricData: List<MetricDatum>,
): PutMetricDataResponse =
    putMetricDataAsync(namespace, metricData).await()

/**
 * [namespace]에 단일 [metricDatum]을 CloudWatch에 코루틴으로 게시합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [putMetricDataAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricDatum = MetricDatum.builder().metricName("Latency").value(100.0).build()
 * )
 * ```
 */
suspend fun CloudWatchAsyncClient.putMetricData(
    namespace: String,
    metricDatum: MetricDatum,
): PutMetricDataResponse =
    putMetricDataAsync(namespace, metricDatum).await()

/**
 * [namespace]의 메트릭 목록을 코루틴으로 조회합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [listMetricsAsync]를 호출한 뒤 `await()`로 완료를 기다린다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.listMetrics(namespace = "MyApp/Performance")
 * response.metrics().forEach { metric -> println(metric.metricName()) }
 * ```
 */
suspend fun CloudWatchAsyncClient.listMetrics(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
): ListMetricsResponse =
    listMetricsAsync(namespace, metricName, dimensions).await()
