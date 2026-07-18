package io.bluetape4k.spring.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.observability.HttpOperationClassification
import io.bluetape4k.junit5.observability.HttpOperationCorrelation
import io.bluetape4k.junit5.observability.HttpOperationCorrelationMode
import io.bluetape4k.junit5.observability.HttpOperationExpectation
import io.bluetape4k.junit5.observability.HttpOperationObservation
import io.bluetape4k.junit5.observability.HttpOperationSensitiveValues
import io.bluetape4k.junit5.observability.assertHttpOperationObservability
import io.micrometer.common.KeyValue
import io.micrometer.common.KeyValues
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import io.micrometer.observation.ObservationRegistry
import io.micrometer.observation.tck.ObservationRegistryAssert
import io.micrometer.observation.tck.TestObservationRegistry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.filter.ServerHttpObservationFilter

class SpringObservationSupportTest {

    private fun registry(handler: RecordingObservationHandler): ObservationRegistry =
        ObservationRegistry.create().apply {
            observationConfig().observationHandler(handler)
        }

    private class RecordingObservationHandler: ObservationHandler<Observation.Context> {
        var started = 0
        var stopped = 0
        var errors = 0
        var lastContext: Observation.Context? = null

        override fun onStart(context: Observation.Context) {
            started++
            lastContext = context
        }

        override fun onStop(context: Observation.Context) {
            stopped++
            lastContext = context
        }

        override fun onError(context: Observation.Context) {
            errors++
            lastContext = context
        }

        override fun supportsContext(context: Observation.Context): Boolean = true
    }

    @Test
    fun `observeSpring starts stops and applies key values`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeSpring(
                name = "spring.service.load",
                keyValues = SpringObservationKeyValues(
                    lowCardinality = KeyValues.of(KeyValue.of("component", "order-service")),
                    highCardinality = KeyValues.of(KeyValue.of("correlation.id", "safe-id")),
                ),
            ) { context ->
                context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
                "ok"
            }

        result shouldBeEqualTo "ok"
        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.lastContext?.getLowCardinalityKeyValue("component")?.value shouldBeEqualTo "order-service"
        handler.lastContext?.getLowCardinalityKeyValue("outcome")?.value shouldBeEqualTo "success"
        handler.lastContext?.getHighCardinalityKeyValue("correlation.id")?.value shouldBeEqualTo "safe-id"
    }

    @Test
    fun `observeSpring records exception and stops observation`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<IllegalStateException> {
            registry.observeSpring("spring.service.error") {
                throw IllegalStateException("boom")
            }
        }

        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
    }

    @Test
    fun `observeSpring rethrows cancellation without recording error`() {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        assertFailsWith<CancellationException> {
            registry.observeSpring("spring.service.cancel") {
                throw CancellationException("cancel")
            }
        }

        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1
    }

    @Test
    fun `observeSpringSuspending binds and cleans current observation`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)

        val result =
            registry.observeSpringSuspending("spring.handler.suspend") { context ->
                registry.currentObservation?.context?.name shouldBeEqualTo "spring.handler.suspend"
                context.name
            }

        result shouldBeEqualTo "spring.handler.suspend"
        yield()

        handler.started shouldBeEqualTo 1
        handler.stopped shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        ObservationRegistryAssert.assertThat(registry)
            .doesNotHaveAnyRemainingCurrentObservation()
    }

    @Test
    fun `observeSpringSuspending rethrows cancellation without recording error`() = runTest {
        val handler = RecordingObservationHandler()
        val registry = registry(handler)
        val cancellation = CancellationException("cancel")

        val thrown =
            assertFailsWith<CancellationException> {
                registry.observeSpringSuspending("spring.handler.cancel") {
                    throw cancellation
                }
            }

        thrown.message shouldBeEqualTo "cancel"
        handler.started shouldBeEqualTo 1
        handler.errors shouldBeEqualTo 0
        handler.stopped shouldBeEqualTo 1
        registry.currentObservation.shouldBeNull()
    }

    @Test
    fun `Spring HTTP observation satisfies the shared HTTP observability conformance fixture`() {
        val handler = RecordingObservationHandler()
        val registry = TestObservationRegistry.create().apply {
            observationConfig().observationHandler(handler)
        }
        val correlationId = "request-123"
        val mockMvcBuilder = MockMvcBuilders.standaloneSetup(HttpConformanceController())
        mockMvcBuilder.addFilters<StandaloneMockMvcBuilder>(
            ServerHttpObservationFilter(registry),
            CorrelationObservationFilter(registry),
        )
        val mockMvc = mockMvcBuilder.build()

        val result = mockMvc
            .perform(
                post("/sales/sale-123")
                    .queryParam("user", "user-456")
                    .header("X-Request-Id", correlationId)
                    .header("X-Forwarded-For", "203.0.113.7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("payload-secret")
            )
            .andExpect(status().isOk)
            .andReturn()

        val context = handler.lastContext.shouldNotBeNull()
        val metricAttributes = context.lowCardinalityKeyValues.associate { keyValue ->
            val key = when (keyValue.key) {
                "method" -> "http.request.method"
                "uri"    -> "http.route"
                "status" -> "http.response.status_code"
                else     -> keyValue.key
            }
            key to keyValue.value
        }
        val statusCode = result.response.status

        assertHttpOperationObservability(
            observation = HttpOperationObservation(
                operationName = context.name.shouldNotBeNull(),
                routeTemplate = metricAttributes.getValue("http.route"),
                statusCode = statusCode,
                classification = statusCode.toHttpOperationClassification(),
                correlation = HttpOperationCorrelation(
                    inbound = correlationId,
                    outbound = result.response.getHeader("X-Request-Id"),
                    mode = HttpOperationCorrelationMode.PROPAGATED,
                ),
                metricAttributes = metricAttributes,
            ),
            expectation = HttpOperationExpectation(
                operationName = "http.server.requests",
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
    }

    private class CorrelationObservationFilter(
        private val registry: ObservationRegistry,
    ): OncePerRequestFilter() {
        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            val correlationId = request.getHeader("X-Request-Id")
            registry.currentObservation?.lowCardinalityKeyValue(
                "correlation.present",
                (correlationId != null).toString(),
            )
            correlationId?.let { response.setHeader("X-Request-Id", it) }
            filterChain.doFilter(request, response)
        }
    }

    @RestController
    private class HttpConformanceController {
        @PostMapping("/sales/{saleId}")
        fun createSale(
            @PathVariable saleId: String,
            @RequestBody payload: String,
        ): ResponseEntity<String> =
            ResponseEntity.ok("$saleId:${payload.length}")
    }

    private fun Int.toHttpOperationClassification(): HttpOperationClassification =
        when (this) {
            in 100..399 -> HttpOperationClassification.SUCCESS
            in 400..499 -> HttpOperationClassification.CLIENT_ERROR
            else        -> HttpOperationClassification.DEPENDENCY_FAILURE
        }
}
