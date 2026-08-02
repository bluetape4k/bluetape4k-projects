package io.bluetape4k.ktor.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.observability.HttpOperationClassification
import io.bluetape4k.junit5.observability.HttpOperationCorrelation
import io.bluetape4k.junit5.observability.HttpOperationCorrelationMode
import io.bluetape4k.junit5.observability.HttpOperationExpectation
import io.bluetape4k.junit5.observability.HttpOperationObservation
import io.bluetape4k.junit5.observability.HttpOperationSensitiveValues
import io.bluetape4k.junit5.observability.assertHttpOperationObservability
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Bluetape4kKtorObservabilityTest {

    @Test
    fun `sanitize correlation id removes unsafe characters and caps length`() {
        val sanitized = KtorCorrelationId.sanitize(
            rawValue = "  abcDEF-_.\r\nInjected: 한글 1234567890  ",
            maxLength = 12
        )

        sanitized shouldBeEqualTo "abcDEF-_.Inj"
    }

    @Test
    fun `observability installer propagates sanitized request correlation id`() = testApplication {
        application {
            installBluetape4kKtorObservability()
            routing {
                get("/ping") {
                    call.respondText("pong")
                }
            }
        }

        val response = client.get("/ping") {
            header(HttpHeaders.XRequestId, "REQ_123-ABC")
        }

        response.status shouldBeEqualTo HttpStatusCode.OK
        response.headers[HttpHeaders.XRequestId] shouldBeEqualTo "REQ_123-ABC"
    }

    @Test
    fun `observability installer creates server span when tracing is configured`() = testTracing { tracing ->
        testApplication {
            application {
                installBluetape4kKtorObservability(
                    Bluetape4kKtorObservabilityConfig(
                        tracing = KtorOpenTelemetryTracingConfig(
                            openTelemetry = tracing.openTelemetry,
                            captureSanitizedCorrelationId = true
                        )
                    )
                )
                routing {
                    get("/ping") {
                        call.respondText("pong")
                    }
                }
            }

            val response = client.get("/ping") {
                header(HttpHeaders.XRequestId, "  REQ_123 Injected: raw  ")
            }

            response.status shouldBeEqualTo HttpStatusCode.OK
            tracing.flush()

            val spans = tracing.spanExporter.finishedSpanItems
            spans shouldHaveSize 1
            val span = spans[0]
            span.kind shouldBeEqualTo SpanKind.SERVER
            span.attributes[CORRELATION_PRESENT_KEY].shouldBeTrue()
            span.attributes[CORRELATION_ID_KEY] shouldBeEqualTo "REQ_123Injectedraw"
            span.attributes.asMap().keys.none { it.key.equals(HttpHeaders.Authorization, ignoreCase = true) }.shouldBeTrue()
        }
    }

    @Test
    fun `observability installer records generated correlation id on server span`() = testTracing { tracing ->
        testApplication {
            application {
                installBluetape4kKtorObservability(
                    Bluetape4kKtorObservabilityConfig(
                        tracing = KtorOpenTelemetryTracingConfig(
                            openTelemetry = tracing.openTelemetry,
                            captureSanitizedCorrelationId = true
                        )
                    )
                )
                routing {
                    get("/ping") {
                        call.respondText("pong")
                    }
                }
            }

            val response = client.get("/ping")
            val generated = response.headers[HttpHeaders.XRequestId].shouldNotBeNull()

            response.status shouldBeEqualTo HttpStatusCode.OK
            generated.length shouldBeEqualTo KtorCorrelationId.DEFAULT_GENERATED_LENGTH
            KtorCorrelationId.isValid(generated).shouldBeTrue()
            tracing.flush()

            val spans = tracing.spanExporter.finishedSpanItems
            spans shouldHaveSize 1
            val span = spans[0]
            span.kind shouldBeEqualTo SpanKind.SERVER
            span.attributes[CORRELATION_PRESENT_KEY].shouldBeTrue()
            span.attributes[CORRELATION_ID_KEY] shouldBeEqualTo generated
        }
    }

    @Test
    fun `observability installer shares custom correlation policy with tracing`() = testTracing { tracing ->
        val correlationId = CorrelationIdSettings(
            requestHeaderName = "X-Correlation-ID",
            responseHeaderName = "X-Correlation-ID",
            maxLength = 8,
        )

        testApplication {
            application {
                installBluetape4kKtorObservability(
                    Bluetape4kKtorObservabilityConfig(
                        correlationId = correlationId,
                        tracing = KtorOpenTelemetryTracingConfig(
                            openTelemetry = tracing.openTelemetry,
                            captureSanitizedCorrelationId = true,
                        ),
                    )
                )
                routing {
                    get("/ping") {
                        call.respondText("pong")
                    }
                }
            }

            val response = client.get("/ping") {
                header(correlationId.requestHeaderName, "client-correlation-id")
            }

            response.status shouldBeEqualTo HttpStatusCode.OK
            response.headers[correlationId.responseHeaderName] shouldBeEqualTo "client-c"
            tracing.flush()

            val span = tracing.spanExporter.finishedSpanItems.single()
            span.attributes[CORRELATION_PRESENT_KEY].shouldBeTrue()
            span.attributes[CORRELATION_ID_KEY] shouldBeEqualTo "client-c"
        }
    }

    @Test
    fun `observability installer preserves explicit tracing correlation override`() = testTracing { tracing ->
        val correlationId = CorrelationIdSettings(
            requestHeaderName = "X-Correlation-ID",
            responseHeaderName = "X-Correlation-ID",
            maxLength = 8,
        )
        val tracingCorrelationId = CorrelationIdSettings(
            requestHeaderName = "X-Trace-ID",
            maxLength = 12,
        )

        testApplication {
            application {
                installBluetape4kKtorObservability(
                    Bluetape4kKtorObservabilityConfig(
                        correlationId = correlationId,
                        tracing = KtorOpenTelemetryTracingConfig(
                            openTelemetry = tracing.openTelemetry,
                            correlationId = tracingCorrelationId,
                            captureSanitizedCorrelationId = true,
                        ),
                    )
                )
                routing {
                    get("/ping") {
                        call.respondText("pong")
                    }
                }
            }

            val response = client.get("/ping") {
                header(correlationId.requestHeaderName, "application-correlation-id")
                header(tracingCorrelationId.requestHeaderName, "trace-correlation-id")
            }

            response.status shouldBeEqualTo HttpStatusCode.OK
            response.headers[correlationId.responseHeaderName] shouldBeEqualTo "applicat"
            tracing.flush()

            val span = tracing.spanExporter.finishedSpanItems.single()
            span.attributes[CORRELATION_PRESENT_KEY].shouldBeTrue()
            span.attributes[CORRELATION_ID_KEY] shouldBeEqualTo "trace-correl"
        }
    }

    @Test
    fun `observability installer does not create spans when tracing is disabled`() = testTracing { tracing ->
        testApplication {
            application {
                installBluetape4kKtorObservability()
                routing {
                    get("/ping") {
                        call.respondText("pong")
                    }
                }
            }

            client.get("/ping").status shouldBeEqualTo HttpStatusCode.OK
            tracing.flush()

            tracing.spanExporter.finishedSpanItems shouldHaveSize 0
        }
    }

    @Test
    fun `open telemetry tracing records error request spans`() = testTracing { tracing ->
        testApplication {
            application {
                installBluetape4kKtorOpenTelemetryTracing(
                    KtorOpenTelemetryTracingConfig(openTelemetry = tracing.openTelemetry)
                )
                routing {
                    get("/failed") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }

            client.get("/failed").status shouldBeEqualTo HttpStatusCode.InternalServerError
            tracing.flush()

            val spans = tracing.spanExporter.finishedSpanItems
            spans shouldHaveSize 1
            spans[0].kind shouldBeEqualTo SpanKind.SERVER
            spans[0].status.statusCode shouldBeEqualTo StatusCode.ERROR
        }
    }

    @Test
    fun `open telemetry tracing does not record error status for cancellation`() = testTracing { tracing ->
        testApplication {
            application {
                installBluetape4kKtorOpenTelemetryTracing(
                    KtorOpenTelemetryTracingConfig(openTelemetry = tracing.openTelemetry)
                )
                routing {
                    get("/cancelled") {
                        throw CancellationException("client disconnected")
                    }
                }
            }

            val response = client.get("/cancelled")
            response.status shouldBeEqualTo HttpStatusCode.InternalServerError
            tracing.flush()

            val spans = tracing.spanExporter.finishedSpanItems
            spans.none { it.status.statusCode == StatusCode.ERROR }.shouldBeTrue()
            spans.forEach { span ->
                span.kind shouldBeEqualTo SpanKind.SERVER
                span.status.statusCode shouldBeEqualTo StatusCode.UNSET
            }
        }
    }

    @Test
    fun `observability installer generates correlation id when request header is missing`() = testApplication {
        application {
            installBluetape4kKtorObservability()
            routing {
                get("/ping") {
                    call.respondText("pong")
                }
            }
        }

        val response = client.get("/ping")
        val generated = response.headers[HttpHeaders.XRequestId]

        response.status shouldBeEqualTo HttpStatusCode.OK
        generated.shouldNotBeNull()
        generated.length shouldBeEqualTo KtorCorrelationId.DEFAULT_GENERATED_LENGTH
        KtorCorrelationId.isValid(generated).shouldBeTrue()
    }

    @Test
    fun `micrometer metrics are installed when registry is provided`() = testApplication {
        val registry = SimpleMeterRegistry()

        application {
            installBluetape4kKtorObservability(
                Bluetape4kKtorObservabilityConfig(meterRegistry = registry)
            )
            routing {
                get("/ping") {
                    call.respondText("pong")
                }
            }
        }

        client.get("/ping").status shouldBeEqualTo HttpStatusCode.OK

        registry.meters.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `Ktor telemetry satisfies the shared HTTP observability conformance fixture`() = testTracing { tracing ->
        testApplication {
            val registry = SimpleMeterRegistry()
            val correlationId = "request-123"
            try {
                application {
                    installBluetape4kKtorObservability(
                        Bluetape4kKtorObservabilityConfig(
                            meterRegistry = registry,
                            tracing = KtorOpenTelemetryTracingConfig(
                                openTelemetry = tracing.openTelemetry,
                                captureSanitizedCorrelationId = true,
                            ),
                        )
                    )
                    routing {
                        post("/sales/{saleId}") {
                            call.respondText("ok")
                        }
                    }
                }

                val response = client.post("/sales/sale-123?user=user-456") {
                    header(HttpHeaders.XRequestId, correlationId)
                    header(HttpHeaders.XForwardedFor, "203.0.113.7")
                    contentType(ContentType.Application.Json)
                    setBody("payload-secret")
                }
                tracing.flush()

                val span = tracing.spanExporter.finishedSpanItems.single()
                val timer = registry.find("ktor.http.server.requests").timers().single()
                val metricAttributes = timer.id.tags.associate { tag ->
                    val key = when (tag.key) {
                        "method"      -> "http.request.method"
                        "route"       -> "http.route"
                        "status"      -> "http.response.status_code"
                        else          -> tag.key
                    }
                    key to tag.value
                } + ("correlation.present" to span.attributes[CORRELATION_PRESENT_KEY].toString())

                assertHttpOperationObservability(
                    observation = HttpOperationObservation(
                        operationName = timer.id.name,
                        routeTemplate = metricAttributes.getValue("http.route"),
                        statusCode = response.status.value,
                        classification = response.status.value.toHttpOperationClassification(),
                        correlation = HttpOperationCorrelation(
                            inbound = correlationId,
                            outbound = response.headers[HttpHeaders.XRequestId],
                            mode = HttpOperationCorrelationMode.PROPAGATED,
                        ),
                        metricAttributes = metricAttributes,
                    ),
                    expectation = HttpOperationExpectation(
                        operationName = "ktor.http.server.requests",
                        routeTemplate = "/sales/{saleId}",
                        statusCode = 200,
                        classification = HttpOperationClassification.SUCCESS,
                        sensitiveValues = HttpOperationSensitiveValues(
                            rawUrl = "http://localhost/sales/sale-123?user=user-456",
                            query = "user=user-456",
                            clientIp = "203.0.113.7",
                            userId = "user-456",
                            saleId = "sale-123",
                            requestPayload = "payload-secret",
                        ),
                    ),
                )
            } finally {
                registry.close()
            }
        }
    }

    @Test
    fun `observability config rejects micrometer installation without registry`() {
        assertFailsWith<IllegalArgumentException> {
            Bluetape4kKtorObservabilityConfig(installMicrometerMetrics = true)
        }
    }

    @Test
    fun `prometheus scrape route exposes registry content`() = testApplication {
        val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        registry.counter("demo.requests").increment()

        application {
            routing {
                prometheusScrapeRoute(registry)
            }
        }

        val response = client.get("/metrics")

        response.status shouldBeEqualTo HttpStatusCode.OK
        response.bodyAsText().contains("demo_requests_total").shouldBeTrue()
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
            openTelemetry.close()
        }
    }

    private inline fun testTracing(block: (TestTracing) -> Unit) {
        TestTracing().use(block)
    }

    private fun Int.toHttpOperationClassification(): HttpOperationClassification =
        when (this) {
            in 100..399 -> HttpOperationClassification.SUCCESS
            in 400..499 -> HttpOperationClassification.CLIENT_ERROR
            else        -> HttpOperationClassification.DEPENDENCY_FAILURE
        }

    companion object {
        private val CORRELATION_PRESENT_KEY: AttributeKey<Boolean> = AttributeKey.booleanKey("correlation.present")
        private val CORRELATION_ID_KEY: AttributeKey<String> = AttributeKey.stringKey("correlation.id")
    }
}
