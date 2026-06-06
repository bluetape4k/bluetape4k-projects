package io.bluetape4k.ktor.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

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
            span.attributes[CORRELATION_PRESENT_KEY] shouldBeEqualTo true
            span.attributes[CORRELATION_ID_KEY] shouldBeEqualTo "REQ_123Injectedraw"
            span.attributes.asMap().keys.none { it.key.equals(HttpHeaders.Authorization, ignoreCase = true) }.shouldBeTrue()
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
        KtorCorrelationId.isValid(generated) shouldBeEqualTo true
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

        registry.meters.isNotEmpty() shouldBeEqualTo true
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
        response.bodyAsText().contains("demo_requests_total") shouldBeEqualTo true
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
