package io.bluetape4k.ktor.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
}
