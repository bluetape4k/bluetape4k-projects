package io.bluetape4k.nats.service

import io.nats.service.Endpoint
import io.nats.service.Group
import io.nats.service.ServiceEndpoint

/**
 * NATS [ServiceEndpoint]를 코틀린 DSL로 생성합니다.
 */
inline fun serviceEndpoint(
    @BuilderInference builder: ServiceEndpoint.Builder.() -> Unit,
): ServiceEndpoint =
    ServiceEndpoint.builder().apply(builder).build()

/**
 * Group/Endpoint 조합을 빠르게 지정하여 [ServiceEndpoint]를 생성합니다.
 *
 * ## 동작/계약
 * - [group], [endpoint]는 전달된 경우에만 builder에 적용합니다.
 * - `builder`만으로 endpoint 속성을 채우는 사용법도 지원합니다.
 */
fun serviceEndpointOf(
    group: Group? = null,
    endpoint: Endpoint? = null,
    @BuilderInference builder: ServiceEndpoint.Builder.() -> Unit = {},
): ServiceEndpoint = serviceEndpoint {
    group?.let { group(it) }
    endpoint?.let { endpoint(it) }
    builder()
}
