@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.nats.service

import io.bluetape4k.support.requireNotBlank
import io.nats.service.Endpoint

inline fun endpoint(
    @BuilderInference builder: Endpoint.Builder.() -> Unit,
): Endpoint =
    Endpoint.builder().apply(builder).build()

inline fun endpointOf(endpoint: Endpoint): Endpoint = endpoint { endpoint(endpoint) }

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
