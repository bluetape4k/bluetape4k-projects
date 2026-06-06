package io.bluetape4k.examples.spring.observability

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

private const val REQUEST_ID_HEADER = "X-Request-Id"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "management.tracing.enabled=false",
    ],
)
class ObservabilitySpringBootDemoApplicationTest {

    @LocalServerPort
    private val port: Int = 0

    private val client: WebTestClient by lazy {
        WebTestClient
            .bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `application publishes event telemetry and returns correlation state`() {
        val response = client
            .post()
            .uri("/orders/order-1/events")
            .header(REQUEST_ID_HEADER, "REQ_123")
            .exchange()
            .expectStatus().isOk
            .expectBody<OrderEventResponse>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        response.orderId shouldBeEqualTo "order-1"
        response.eventType shouldBeEqualTo "order.accepted"
        response.correlationPresent shouldBeEqualTo true
        response.status shouldBeEqualTo "accepted"
    }

    @Test
    fun `actuator prometheus endpoint exposes http and event observation metrics`() {
        client
            .post()
            .uri("/orders/order-2/events")
            .header(REQUEST_ID_HEADER, "REQ_456")
            .exchange()
            .expectStatus().isOk

        val metrics = client
            .get()
            .uri("/actuator/prometheus")
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .returnResult()
            .responseBody
            .shouldNotBeNull()

        metrics.contains("http_server_requests").shouldBeTrue()
        metrics.contains("event_publish").shouldBeTrue()
        metrics.contains("event_consume").shouldBeTrue()
    }

    @Test
    fun `actuator health remains available with observability configuration`() {
        client
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
    }
}
