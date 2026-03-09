@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.nats.service

import io.bluetape4k.support.requireNotBlank
import io.nats.service.Endpoint

/**
 * NATS [Endpoint]를 코틀린 DSL로 생성합니다.
 */
inline fun endpoint(
    @BuilderInference builder: Endpoint.Builder.() -> Unit,
): Endpoint =
    Endpoint.builder().apply(builder).build()

/**
 * 기존 [Endpoint]를 기반으로 복사본을 생성합니다.
 */
inline fun endpointOf(endpoint: Endpoint): Endpoint = endpoint { endpoint(endpoint) }

/**
 * 이름과 subject 중심의 기본 설정으로 [Endpoint]를 생성합니다.
 *
 * ## 동작/계약
 * - [name], [subject]는 비어 있을 수 없습니다.
 * - [metadata]를 먼저 적용한 뒤 [builder]를 실행합니다.
 */
inline fun endpointOf(
    name: String,
    subject: String,
    metadata: Map<String, String> = emptyMap(),
    @BuilderInference builder: Endpoint.Builder.() -> Unit = {},
): Endpoint {
    name.requireNotBlank("name")
    subject.requireNotBlank("subject")

    return endpoint {
        name(name)
        subject(subject)
        metadata(metadata)

        builder()
    }
}
