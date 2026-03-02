package io.bluetape4k.aws.cloudwatch.model

import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest

/**
 * DSL 블록으로 [ListMetricsRequest]를 빌드합니다.
 *
 * ```kotlin
 * val request = listMetricsRequest {
 *     namespace("MyApp/Performance")
 * }
 * ```
 */
inline fun listMetricsRequest(
    @BuilderInference builder: ListMetricsRequest.Builder.() -> Unit,
): ListMetricsRequest =
    ListMetricsRequest.builder().apply(builder).build()

/**
 * namespace와 선택적 필터로 [ListMetricsRequest]를 생성합니다.
 *
 * ```kotlin
 * val request = listMetricsRequestOf(
 *     namespace = "MyApp/Performance",
 *     metricName = "Latency"
 * )
 * ```
 */
inline fun listMetricsRequestOf(
    namespace: String? = null,
    metricName: String? = null,
    dimensions: List<DimensionFilter>? = null,
    @BuilderInference builder: ListMetricsRequest.Builder.() -> Unit = {},
): ListMetricsRequest = listMetricsRequest {
    namespace?.let { namespace(it) }
    metricName?.let { metricName(it) }
    dimensions?.let { dimensions(it) }
    builder()
}
