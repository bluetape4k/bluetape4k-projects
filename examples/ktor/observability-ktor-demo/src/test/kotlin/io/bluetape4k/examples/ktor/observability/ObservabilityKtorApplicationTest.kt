package io.bluetape4k.examples.ktor.observability

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.ktor.testing.decodeJsonBody
import io.bluetape4k.ktor.testing.shouldHaveStatus
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ObservabilityKtorApplicationTest {

    @Test
    fun `application publishes event telemetry and propagates request id`() = testApplication {
        application {
            observabilityKtorModule()
        }

        val response = client.post("/orders/order-1/events") {
            header(HttpHeaders.XRequestId, "REQ_123")
        }

        response shouldHaveStatus HttpStatusCode.OK
        response.headers[HttpHeaders.XRequestId] shouldBeEqualTo "REQ_123"

        val body = response.decodeJsonBody<OrderEventResponse>()
        body.orderId shouldBeEqualTo "order-1"
        body.eventType shouldBeEqualTo "order.accepted"
        body.correlationPresent shouldBeEqualTo true
        body.status shouldBeEqualTo "accepted"
    }

    @Test
    fun `metrics route exposes Ktor and event observation metrics`() = testApplication {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

        application {
            observabilityKtorModule(meterRegistry = registry, openTelemetry = null)
        }

        client.post("/orders/order-2/events") {
            header(HttpHeaders.XRequestId, "REQ_456")
        } shouldHaveStatus HttpStatusCode.OK

        val metrics = client.get("/metrics").bodyAsText()

        metrics.contains("ktor_http_server_requests").shouldBeTrue()
        metrics.contains("event_publish").shouldBeTrue()
        metrics.contains("event_consume").shouldBeTrue()
    }

    @Test
    fun `optional OpenTelemetry tracing records server span without external collector`() = testTracing { tracing ->
        testApplication {
            application {
                observabilityKtorModule(openTelemetry = tracing.openTelemetry)
            }

            client.post("/orders/order-3/events") {
                header(HttpHeaders.XRequestId, "  REQ_789 Injected: raw  ")
            } shouldHaveStatus HttpStatusCode.OK

            tracing.flush()

            val spans = tracing.spanExporter.finishedSpanItems
            spans shouldHaveSize 1
            spans[0].kind shouldBeEqualTo SpanKind.SERVER
            spans[0].attributes[CORRELATION_PRESENT_KEY] shouldBeEqualTo true
            spans[0].attributes[CORRELATION_ID_KEY] shouldBeEqualTo "REQ_789Injectedraw"
        }
    }

    private class TestTracing: AutoCloseable {
        val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()
        private val tracerProvider: SdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
            .build()
        val openTelemetry: OpenTelemetrySdk = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build()

        fun flush() {
            tracerProvider.forceFlush().join(1, TimeUnit.SECONDS)
        }

        override fun close() {
            tracerProvider.close()
        }
    }

    private inline fun testTracing(block: (TestTracing) -> Unit) {
        TestTracing().use(block)
    }

    companion object {
        private val CORRELATION_PRESENT_KEY: AttributeKey<Boolean> = AttributeKey.booleanKey("correlation.present")
        private val CORRELATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("correlation.id")
    }
}
