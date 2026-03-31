package io.bluetape4k.aws.kotlin.cloudwatch

import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.listMetrics
import aws.sdk.kotlin.services.cloudwatch.model.DimensionFilter
import aws.sdk.kotlin.services.cloudwatch.model.ListMetricsRequest
import aws.sdk.kotlin.services.cloudwatch.model.ListMetricsResponse
import aws.sdk.kotlin.services.cloudwatch.model.MetricDatum
import aws.sdk.kotlin.services.cloudwatch.model.PutMetricDataRequest
import aws.sdk.kotlin.services.cloudwatch.model.PutMetricDataResponse
import aws.sdk.kotlin.services.cloudwatch.putMetricData
import io.bluetape4k.support.requireNotBlank

/**
 * [namespace]에 [metricData] 목록을 CloudWatch에 게시합니다.
 *
 * ```kotlin
 * val response = cloudWatchClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricData = listOf(metricDatum)
 * )
 * ```
 *
 * @param namespace CloudWatch 네임스페이스
 * @param metricData 게시할 메트릭 데이터 목록
 * @param builder [PutMetricDataRequest.Builder]에 대한 추가 설정 람다
 * @return [PutMetricDataResponse] 인스턴스
 */
suspend inline fun CloudWatchClient.putMetricData(
    namespace: String,
    metricData: List<MetricDatum>,
    crossinline builder: PutMetricDataRequest.Builder.() -> Unit = {},
): PutMetricDataResponse {
    namespace.requireNotBlank("namespace")
    return putMetricData {
        this.namespace = namespace
        this.metricData = metricData
        builder()
    }
}

/**
 * [namespace]에 단일 [metricDatum]을 CloudWatch에 게시합니다.
 *
 * ```kotlin
 * val response = cloudWatchClient.putMetricData(
 *     namespace = "MyApp/Performance",
 *     metricDatum = MetricDatum { metricName = "Latency"; value = 100.0 }
 * )
 * ```
 *
 * @param namespace CloudWatch 네임스페이스
 * @param metricDatum 게시할 단일 메트릭 데이터
 * @param builder [PutMetricDataRequest.Builder]에 대한 추가 설정 람다
 * @return [PutMetricDataResponse] 인스턴스
 */
suspend inline fun CloudWatchClient.putMetricData(
    namespace: String,
    metricDatum: MetricDatum,
    crossinline builder: PutMetricDataRequest.Builder.() -> Unit = {},
): PutMetricDataResponse {
    namespace.requireNotBlank("namespace")
    return putMetricData(namespace, listOf(metricDatum), builder)
}

/**
 * 네임스페이스 및 필터 조건으로 CloudWatch 메트릭 목록을 조회합니다.
 *
 * ```kotlin
 * val response = cloudWatchClient.listMetrics(namespace = "MyApp/Performance")
 * response.metrics?.forEach { metric -> println(metric.metricName) }
 * ```
 *
 * @param namespace 조회할 네임스페이스. null이면 전체 조회합니다.
 * @param metricName 조회할 메트릭 이름. null이면 필터하지 않습니다.
 * @param dimensions 조회할 dimensions 필터. null이면 필터하지 않습니다.
 * @param builder [ListMetricsRequest.Builder]에 대한 추가 설정 람다
 * @return [ListMetricsResponse] 인스턴스
 */
suspend inline fun CloudWatchClient.listMetrics(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
    crossinline builder: ListMetricsRequest.Builder.() -> Unit = {},
): ListMetricsResponse =
    listMetrics {
        namespace?.let { this.namespace = it }
        metricName?.let { this.metricName = it }
        dimensions?.let { this.dimensions = it }
        builder()
    }
