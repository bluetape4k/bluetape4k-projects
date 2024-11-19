package io.bluetape4k.nats.service

import io.nats.service.Endpoint

inline fun endpoint(initializer: Endpoint.Builder.() -> Unit): Endpoint {
    return Endpoint.builder().apply(initializer).build()
}

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
