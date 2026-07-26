package io.bluetape4k.examples.ktor.observability

import io.bluetape4k.ktor.core.installBluetape4kKtorCore
import io.bluetape4k.ktor.core.requiredPathParameter
import io.bluetape4k.ktor.observability.Bluetape4kKtorObservabilityConfig
import io.bluetape4k.ktor.observability.KtorOpenTelemetryTracingConfig
import io.bluetape4k.ktor.observability.installBluetape4kKtorObservability
import io.bluetape4k.ktor.observability.prometheusScrapeRoute
import io.bluetape4k.micrometer.observation.events.EventCorrelation
import io.bluetape4k.micrometer.observation.events.EventDestination
import io.bluetape4k.micrometer.observation.events.EventTelemetry
import io.bluetape4k.micrometer.observation.events.observeEventConsumeSuspending
import io.bluetape4k.micrometer.observation.events.observeEventPublishSuspending
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

/**
 * Configures the Ktor observability example application.
 *
 * ## Contract
 * - The application owns the Prometheus registry and exposes it at `/metrics`.
 * - OpenTelemetry tracing is installed only when an [OpenTelemetry] instance is supplied.
 * - Application event telemetry is recorded with bounded low-cardinality dimensions.
 */
internal fun Application.observabilityKtorModule(
    meterRegistry: PrometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
    openTelemetry: OpenTelemetry? = null,
    service: OrderEventTelemetryService = OrderEventTelemetryService(observationRegistryFor(meterRegistry)),
) {
    installBluetape4kKtorCore()
    installBluetape4kKtorObservability(
        Bluetape4kKtorObservabilityConfig(
            meterRegistry = meterRegistry,
            tracing = openTelemetry?.let {
                KtorOpenTelemetryTracingConfig(
                    openTelemetry = it,
                    captureSanitizedCorrelationId = true,
                )
            },
        )
    )

    routing {
        observabilityRoutes(service, meterRegistry)
    }
}

fun main() {
    embeddedServer(CIO, host = "0.0.0.0", port = 8080) {
        observabilityKtorModule()
    }.start(wait = true)
}

internal fun Routing.observabilityRoutes(
    service: OrderEventTelemetryService,
    meterRegistry: PrometheusMeterRegistry,
) {
    prometheusScrapeRoute(meterRegistry)

    get("/health") {
        call.respond(HealthResponse())
    }

    post("/orders/{orderId}/events") {
        call.respond(
            service.publishOrderEvent(
                orderId = call.requiredPathParameter("orderId"),
                rawCorrelationId = call.request.header(HttpHeaders.XRequestId),
            )
        )
    }
}

internal class OrderEventTelemetryService(
    private val observationRegistry: ObservationRegistry,
) {

    suspend fun publishOrderEvent(
        orderId: String,
        rawCorrelationId: String?,
    ): OrderEventResponse {
        val correlation = EventCorrelation.sanitized(rawCorrelationId)
        val telemetry = EventTelemetry(
            destination = EventDestination("ktor", "order-events"),
            eventType = EVENT_TYPE,
            correlation = correlation,
            batchMessageCount = 1,
        )
        val event = OrderEvent(orderId = orderId, type = EVENT_TYPE)

        observationRegistry.observeEventPublishSuspending(telemetry) {
            publishToLocalChannel(event)
        }
        observationRegistry.observeEventConsumeSuspending(telemetry) {
            consumeFromLocalChannel(event)
        }

        return OrderEventResponse(
            orderId = orderId,
            eventType = event.type,
            correlationPresent = correlation.present,
            status = "accepted",
        )
    }

    private suspend fun publishToLocalChannel(event: OrderEvent): OrderEvent =
        event

    private suspend fun consumeFromLocalChannel(event: OrderEvent): OrderEvent =
        event

    companion object {
        private const val EVENT_TYPE = "order.accepted"
    }
}

internal fun observationRegistryFor(meterRegistry: MeterRegistry): ObservationRegistry =
    ObservationRegistry.create().apply {
        observationConfig().observationHandler(DefaultMeterObservationHandler(meterRegistry))
    }

@Serializable
internal data class OrderEvent(
    val orderId: String,
    val type: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1155546218142106024L
    }
}

@Serializable
internal data class OrderEventResponse(
    val orderId: String,
    val eventType: String,
    val correlationPresent: Boolean,
    val status: String,
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 2044338399999869276L
    }
}

@Serializable
internal data class HealthResponse(
    val status: String = "UP",
): JavaSerializable {
    companion object {
        private const val serialVersionUID: Long = 1624425969250105208L
    }
}
