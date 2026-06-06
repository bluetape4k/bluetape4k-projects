package io.bluetape4k.examples.spring.observability

import io.bluetape4k.micrometer.observation.events.EventCorrelation
import io.bluetape4k.micrometer.observation.events.EventDestination
import io.bluetape4k.micrometer.observation.events.EventTelemetry
import io.bluetape4k.micrometer.observation.events.observeEventConsume
import io.bluetape4k.micrometer.observation.events.observeEventPublish
import io.bluetape4k.spring.observability.SpringObservationKeyValues
import io.bluetape4k.spring.observability.observeSpring
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.observation.ObservationRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.stereotype.Service
import java.io.Serializable

private const val REQUEST_ID_HEADER = "X-Request-Id"

/**
 * Spring Boot observability example for Prometheus metrics and OTLP tracing configuration.
 *
 * ## Behavior
 * - Exposes application metrics through Spring Boot Actuator's Prometheus endpoint.
 * - Wraps HTTP service and application event paths with bluetape4k observation helpers.
 * - Keeps OTLP exporter configuration application-owned through Spring Boot properties.
 */
@SpringBootApplication(proxyBeanMethods = false)
class ObservabilitySpringBootDemoApplication

fun main(args: Array<String>) {
    runApplication<ObservabilitySpringBootDemoApplication>(*args)
}

@RestController
internal class OrderEventController(
    private val service: OrderEventService,
) {

    @GetMapping("/health")
    fun health(): HealthResponse =
        HealthResponse()

    @PostMapping("/orders/{orderId}/events")
    fun publishOrderEvent(
        @PathVariable orderId: String,
        @RequestHeader(REQUEST_ID_HEADER, required = false) correlationId: String?,
    ): OrderEventResponse =
        service.publishOrderEvent(orderId = orderId, rawCorrelationId = correlationId)
}

@Service
internal class OrderEventService(
    private val observationRegistry: ObservationRegistry,
) {

    fun publishOrderEvent(
        orderId: String,
        rawCorrelationId: String?,
    ): OrderEventResponse =
        observationRegistry.observeSpring(
            name = "orders.http.publish",
            keyValues = SpringObservationKeyValues(
                lowCardinality = KeyValues.of(
                    KeyValue.of("component", "observability-demo"),
                    KeyValue.of("http.route", "/orders/{orderId}/events"),
                ),
            ),
        ) { context ->
            context.addLowCardinalityKeyValue(KeyValue.of("order.event", EVENT_TYPE))

            val correlation = EventCorrelation.sanitized(rawCorrelationId)
            val telemetry = EventTelemetry(
                destination = EventDestination.spring("order-events"),
                eventType = EVENT_TYPE,
                correlation = correlation,
                batchMessageCount = 1,
            )
            val event = OrderEvent(orderId = orderId, type = EVENT_TYPE)

            observationRegistry.observeEventPublish(telemetry) {
                publishToLocalChannel(event)
            }
            observationRegistry.observeEventConsume(telemetry) {
                consumeFromLocalChannel(event)
            }

            OrderEventResponse(
                orderId = orderId,
                eventType = event.type,
                correlationPresent = correlation.present,
                status = "accepted",
            )
        }

    private fun publishToLocalChannel(event: OrderEvent): OrderEvent =
        event

    private fun consumeFromLocalChannel(event: OrderEvent): OrderEvent =
        event

    companion object {
        private const val EVENT_TYPE = "order.accepted"
    }
}

internal data class OrderEvent(
    val orderId: String,
    val type: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 7379384661824745069L
    }
}

internal data class OrderEventResponse(
    val orderId: String,
    val eventType: String,
    val correlationPresent: Boolean,
    val status: String,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = -9107738853997178628L
    }
}

internal data class HealthResponse(
    val status: String = "UP",
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 3030398709155074173L
    }
}
