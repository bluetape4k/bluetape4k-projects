package io.bluetape4k.nats.service

import io.nats.client.Connection
import io.nats.service.Service
import io.nats.service.ServiceBuilder
import io.nats.service.ServiceEndpoint

/**
 * NATS [Service]를 코틀린 DSL로 생성합니다.
 *
 * ## 동작/계약
 * - 호출마다 새 [Service] 인스턴스를 생성합니다.
 * - [builder] 내부 예외는 그대로 전파합니다.
 */
inline fun natsService(
    builder: ServiceBuilder.() -> Unit,
): Service =
    ServiceBuilder().apply(builder).build()

/**
 * 연결/이름/버전과 엔드포인트를 한 번에 지정하여 [Service]를 생성합니다.
 *
 * ## 동작/계약
 * - [serviceEndpoints]는 전달된 순서대로 등록됩니다.
 * - 기본 필수 값 설정 후 [builder]가 추가로 실행되어 세부 옵션을 덮어쓸 수 있습니다.
 */
inline fun natsServiceOf(
    nc: Connection,
    name: String,
    version: String,
    vararg serviceEndpoints: ServiceEndpoint,
    builder: ServiceBuilder.() -> Unit = {},
): Service = natsService {
    connection(nc)
    name(name)
    version(version)

    serviceEndpoints.forEach { serviceEndpoint ->
        addServiceEndpoint(serviceEndpoint)
    }

    builder()
}
