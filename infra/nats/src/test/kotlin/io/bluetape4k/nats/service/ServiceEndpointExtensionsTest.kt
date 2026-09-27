package io.bluetape4k.nats.service

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import io.bluetape4k.nats.AbstractNatsTest
import io.mockk.mockk
import io.nats.service.ServiceMessageHandler
import org.junit.jupiter.api.Test

class ServiceEndpointExtensionsTest: AbstractNatsTest() {

    companion object: KLogging()

    @Test
    fun `serviceEndpointOf supports builder-only endpoint definition`() {
        val handler = mockk<ServiceMessageHandler>(relaxed = true)

        val serviceEndpoint = serviceEndpointOf {
            endpointName("echo")
            endpointSubject("service.echo")
            handler(handler)
        }

        serviceEndpoint.name shouldBeEqualTo "echo"
        serviceEndpoint.subject shouldBeEqualTo "service.echo"
    }
}
