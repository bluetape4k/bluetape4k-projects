package io.bluetape4k.aws.cloudwatch

import io.bluetape4k.support.requireNotBlank
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse
import java.util.concurrent.CompletableFuture

/**
 * [namespace]에 [metricData] 목록을 CloudWatch에 비동기로 게시합니다.
 *
 * ## 동작/계약
 * - [namespace]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.putMetricDataAsync(
 *     namespace = "MyApp/Performance",
 *     metricData = listOf(metricDatum)
 * ).join()
 * ```
 */
fun CloudWatchAsyncClient.putMetricDataAsync(
    namespace: String,
    metricData: List<MetricDatum>,
): CompletableFuture<PutMetricDataResponse> {
    namespace.requireNotBlank("namespace")
    return putMetricData {
        it.namespace(namespace)
        it.metricData(metricData)
    }
}

/**
 * [namespace]에 단일 [metricDatum]을 CloudWatch에 비동기로 게시합니다.
 *
 * ## 동작/계약
 * - [namespace]가 blank이면 `IllegalArgumentException`을 던진다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.putMetricDataAsync(
 *     namespace = "MyApp/Performance",
 *     metricDatum = MetricDatum.builder().metricName("Latency").value(100.0).build()
 * ).join()
 * ```
 */
fun CloudWatchAsyncClient.putMetricDataAsync(
    namespace: String,
    metricDatum: MetricDatum,
): CompletableFuture<PutMetricDataResponse> {
    namespace.requireNotBlank("namespace")
    return putMetricDataAsync(namespace, listOf(metricDatum))
}

/**
 * [namespace]의 메트릭 목록을 비동기로 조회합니다.
 *
 * ```kotlin
 * val response = cloudWatchAsyncClient.listMetricsAsync(namespace = "MyApp/Performance").join()
 * response.metrics().forEach { metric -> println(metric.metricName()) }
 * ```
 */
fun CloudWatchAsyncClient.listMetricsAsync(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
): CompletableFuture<ListMetricsResponse> =
    listMetrics {
        namespace?.let { ns -> it.namespace(ns) }
        metricName?.let { mn -> it.metricName(mn) }
        dimensions?.let { dims -> it.dimensions(dims) }
    }
