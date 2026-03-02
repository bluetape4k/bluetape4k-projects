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
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.http.engine.HttpClientEngine
import aws.smithy.kotlin.runtime.net.url.Url
import io.bluetape4k.aws.kotlin.http.HttpClientEngineProvider
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.utils.ShutdownQueue

/**
 * AWS Kotlin SDK [CloudWatchClient] 인스턴스를 생성합니다.
 *
 * ```kotlin
 * val client = cloudWatchClientOf(
 *     endpointUrl = Url.parse("http://localhost:4566"),
 *     region = "us-east-1",
 *     credentialsProvider = credentialsProvider
 * )
 * ```
 *
 * @param endpointUrl CloudWatch 서비스 엔드포인트 URL. null이면 기본 AWS 엔드포인트를 사용합니다.
 * @param region AWS 리전. null이면 환경 설정에서 자동으로 감지합니다.
 * @param credentialsProvider AWS 인증 정보 제공자. null이면 기본 자격 증명 체인을 사용합니다.
 * @param httpClient HTTP 클라이언트 엔진. 기본값은 [HttpClientEngineProvider.defaultHttpEngine]입니다.
 * @param builder [CloudWatchClient.Config.Builder]에 대한 추가 설정 람다.
 * @return 설정된 [CloudWatchClient] 인스턴스.
 */
inline fun cloudWatchClientOf(
    endpointUrl: Url? = null,
    region: String? = null,
    credentialsProvider: CredentialsProvider? = null,
    httpClient: HttpClientEngine = HttpClientEngineProvider.defaultHttpEngine,
    @BuilderInference crossinline builder: CloudWatchClient.Config.Builder.() -> Unit = {},
): CloudWatchClient =
    CloudWatchClient {
        endpointUrl?.let { this.endpointUrl = it }
        region?.let { this.region = it }
        credentialsProvider?.let { this.credentialsProvider = it }
        this.httpClient = httpClient

        builder()
    }.apply {
        ShutdownQueue.register(this)
    }

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
    @BuilderInference crossinline builder: PutMetricDataRequest.Builder.() -> Unit = {},
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
    @BuilderInference crossinline builder: PutMetricDataRequest.Builder.() -> Unit = {},
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
    @BuilderInference crossinline builder: ListMetricsRequest.Builder.() -> Unit = {},
): ListMetricsResponse =
    listMetrics {
        namespace?.let { this.namespace = it }
        metricName?.let { this.metricName = it }
        dimensions?.let { this.dimensions = it }
        builder()
    }
