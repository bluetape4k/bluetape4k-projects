package io.bluetape4k.nats.service

import io.nats.service.Endpoint
import io.nats.service.Group
import io.nats.service.ServiceEndpoint

inline fun serviceEndpoint(
    @BuilderInference builder: ServiceEndpoint.Builder.() -> Unit,
): ServiceEndpoint =
    ServiceEndpoint.builder().apply(builder).build()

fun serviceEndpointOf(
    group: Group? = null,
    endpoint: Endpoint? = null,
): ServiceEndpoint = serviceEndpoint {
    group(group)
    endpoint(endpoint)
}
