package io.bluetape4k.nats.service

import io.nats.service.Endpoint

inline fun endpoint(
    @BuilderInference builder: Endpoint.Builder.() -> Unit,
): Endpoint =
    Endpoint.builder().apply(builder).build()

fun endpointOf(endpoint: Endpoint): Endpoint = endpoint { endpoint(endpoint) }

fun endpointOf(
    name: String,
    subject: String,
    metadata: Map<String, String> = emptyMap(),
): Endpoint = endpoint {
    name(name)
    subject(subject)
    metadata(metadata)
}
