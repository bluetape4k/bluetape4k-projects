package io.bluetape4k.nats.service

import io.nats.client.Connection
import io.nats.service.Service
import io.nats.service.ServiceBuilder
import io.nats.service.ServiceEndpoint

inline fun natsService(
    @BuilderInference builder: ServiceBuilder.() -> Unit,
): Service =
    ServiceBuilder().apply(builder).build()

inline fun natsServiceOf(
    nc: Connection,
    name: String,
    version: String,
    vararg serviceEndpoints: ServiceEndpoint,
    @BuilderInference builder: ServiceBuilder.() -> Unit = {},
): Service = natsService {
    connection(nc)
    name(name)
    version(version)

    serviceEndpoints.forEach { serviceEndpoint ->
        addServiceEndpoint(serviceEndpoint)
    }

    builder()
}
