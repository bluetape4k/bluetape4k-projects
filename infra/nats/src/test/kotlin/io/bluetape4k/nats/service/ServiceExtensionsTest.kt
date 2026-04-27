package io.bluetape4k.nats.service

import io.mockk.mockk
import io.nats.client.Connection
import io.nats.service.ServiceMessageHandler
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ServiceExtensionsTest {

    private lateinit var nc: Connection

    @BeforeEach
    fun setUp() {
        nc = mockk<Connection>(relaxed = true)
    }

    @Test
    fun `natsService with minimal config creates Service`() {
        val service = natsService {
            connection(nc)
            name("test-service")
            version("1.0.0")
        }

        service.shouldNotBeNull()
        service.name shouldBeEqualTo "test-service"
        service.version shouldBeEqualTo "1.0.0"
    }

    @Test
    fun `natsServiceOf with connection name and version creates Service`() {
        val service = natsServiceOf(nc, "my-service", "2.0.0")

        service.shouldNotBeNull()
        service.name shouldBeEqualTo "my-service"
        service.version shouldBeEqualTo "2.0.0"
    }

    @Test
    fun `natsServiceOf with endpoints registers endpoints`() {
        val handler = mockk<ServiceMessageHandler>(relaxed = true)
        val endpoint = serviceEndpointOf {
            endpointName("echo")
            endpointSubject("service.echo")
            handler(handler)
        }

        val service = natsServiceOf(nc, "echo-service", "1.0.0", endpoint)

        service.shouldNotBeNull()
        service.name shouldBeEqualTo "echo-service"
    }

    @Test
    fun `natsServiceOf with builder block applies additional config`() {
        val service = natsServiceOf(nc, "svc", "1.0.0") {
            description("A test service")
        }

        service.shouldNotBeNull()
        service.name shouldBeEqualTo "svc"
    }
}
