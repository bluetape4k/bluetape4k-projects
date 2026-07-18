package io.bluetape4k.junit5.observability

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.junit5.output.InMemoryLogbackAppender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpOperationObservabilityConformanceTest {

    @Test
    fun `safe HTTP observation satisfies the conformance contract and logs only bounded metadata`() {
        val observation = successObservation()
        val expectation = successExpectation()

        InMemoryLogbackAppender(HTTP_OPERATION_OBSERVABILITY_LOGGER_NAME).use { appender ->
            assertHttpOperationObservability(observation, expectation)

            appender.size shouldBeEqualTo 1
            appender.lastMessage shouldBeEqualTo
                    "HTTP observability conformance verified: " +
                    "classification=SUCCESS, statusCode=200, metricAttributeCount=4"
            appender.messages.forEach { message ->
                message shouldNotContain observation.operationName
                message shouldNotContain observation.routeTemplate
                message shouldNotContain observation.correlation.inbound.orEmpty()
                sensitiveValueSet.forEach { sensitiveValue ->
                    message shouldNotContain sensitiveValue
                }
            }
        }
    }

    @Test
    fun `all required HTTP result classifications satisfy the same contract`() {
        val cases = listOf(
            HttpOperationClassification.SUCCESS to 200,
            HttpOperationClassification.CLIENT_ERROR to 404,
            HttpOperationClassification.TIMEOUT_OR_CANCELLATION to null,
            HttpOperationClassification.DEPENDENCY_FAILURE to 503,
        )

        cases.forEach { (classification, statusCode) ->
            val observation = successObservation().copy(
                statusCode = statusCode,
                classification = classification,
                metricAttributes = successObservation().metricAttributes.withStatusCode(statusCode),
            )
            val expectation = successExpectation().copy(
                statusCode = statusCode,
                classification = classification,
            )

            assertHttpOperationObservability(observation, expectation)
        }
    }

    @Test
    fun `semantically invalid status and classification pairs fail the conformance contract`() {
        val invalidCases = listOf(
            HttpOperationClassification.SUCCESS to 503,
            HttpOperationClassification.CLIENT_ERROR to 200,
            HttpOperationClassification.TIMEOUT_OR_CANCELLATION to 200,
            HttpOperationClassification.DEPENDENCY_FAILURE to 404,
        )

        invalidCases.forEach { (classification, statusCode) ->
            val observation = successObservation().copy(
                statusCode = statusCode,
                classification = classification,
                metricAttributes = successObservation().metricAttributes.withStatusCode(statusCode),
            )
            val expectation = successExpectation().copy(
                statusCode = statusCode,
                classification = classification,
            )

            assertFailsWith<AssertionError> {
                assertHttpOperationObservability(observation, expectation)
            }
        }
    }

    @Test
    fun `metric status must match the observed HTTP status`() {
        val mismatchedStatus = successObservation().copy(
            metricAttributes = successObservation().metricAttributes + ("http.response.status_code" to "503"),
        )

        assertFailsWith<AssertionError> {
            assertHttpOperationObservability(mismatchedStatus, successExpectation())
        }
    }

    @Test
    fun `standard body size metric keys remain low-cardinality safe`() {
        val bodySizes = successObservation().copy(
            metricAttributes = successObservation().metricAttributes + mapOf(
                "http.request.body.size" to "14",
                "http.response.body.size" to "2",
            ),
        )

        assertHttpOperationObservability(bodySizes, successExpectation())
    }

    @Test
    fun `raw route fails the conformance contract`() {
        val rawRoute = "/sales/sale-123"
        val rawRouteObservation = successObservation().copy(
            routeTemplate = rawRoute,
            metricAttributes = successObservation().metricAttributes + ("http.route" to rawRoute),
        )
        val rawRouteExpectation = successExpectation().copy(routeTemplate = rawRoute)

        val failure = assertFailsWith<AssertionError> {
            assertHttpOperationObservability(rawRouteObservation, rawRouteExpectation)
        }
        failure.message.orEmpty() shouldContain "values redacted"
        sensitiveValueSet.forEach { sensitiveValue ->
            failure.message.orEmpty() shouldNotContain sensitiveValue
        }
    }

    @Test
    fun `sensitive metric keys independently fail the conformance contract`() {
        val unsafeAttributes = mapOf(
            "url.full" to "https://example.test/sales/sale-123",
            "url.query" to "user=user-456",
            "client.address" to "203.0.113.7",
            "user.id" to "user-456",
            "sale.id" to "sale-123",
            "request.payload" to "payload-secret",
            "client_ip" to "203.0.113.7",
            "userId" to "user-456",
            "sale-id" to "sale-123",
            "requestPayload" to "payload-secret",
        )

        unsafeAttributes.forEach { (key, value) ->
            val unsafeObservation = successObservation().copy(
                metricAttributes = successObservation().metricAttributes + (key to value),
            )

            assertFailsWith<AssertionError> {
                assertHttpOperationObservability(unsafeObservation, successExpectation())
            }
        }
    }

    @Test
    fun `sensitive metric value under an unrecognized key fails the conformance contract`() {
        val unsafeObservation = successObservation().copy(
            metricAttributes = successObservation().metricAttributes + ("custom.label" to "sale-123"),
        )

        assertFailsWith<AssertionError> {
            assertHttpOperationObservability(unsafeObservation, successExpectation())
        }
    }

    @Test
    fun `blank sensitive input coverage fails the conformance contract`() {
        val incompleteExpectation = successExpectation().copy(
            sensitiveValues = sensitiveValues().copy(requestPayload = ""),
        )

        assertFailsWith<AssertionError> {
            assertHttpOperationObservability(successObservation(), incompleteExpectation)
        }
    }

    @Test
    fun `correlation mismatch fails the conformance contract`() {
        val mismatched = successObservation().copy(
            correlation = HttpOperationCorrelation(
                inbound = "request-123",
                outbound = "different-456",
                mode = HttpOperationCorrelationMode.PROPAGATED,
            ),
        )

        assertFailsWith<AssertionError> {
            assertHttpOperationObservability(mismatched, successExpectation())
        }
    }

    @Test
    fun `generated correlation satisfies the explicit generated contract`() {
        val generated = successObservation().copy(
            correlation = HttpOperationCorrelation(
                inbound = null,
                outbound = "generated-123",
                mode = HttpOperationCorrelationMode.GENERATED,
            ),
        )

        assertHttpOperationObservability(generated, successExpectation())
    }

    @Test
    fun `absent correlation satisfies the explicit absent contract`() {
        val absent = successObservation().copy(
            correlation = HttpOperationCorrelation(
                inbound = null,
                outbound = null,
                mode = HttpOperationCorrelationMode.ABSENT,
            ),
            metricAttributes = successObservation().metricAttributes + ("correlation.present" to "false"),
        )

        assertHttpOperationObservability(absent, successExpectation())
    }

    private fun successObservation(): HttpOperationObservation =
        HttpOperationObservation(
            operationName = "http.server.requests",
            routeTemplate = "/sales/{saleId}",
            statusCode = 200,
            classification = HttpOperationClassification.SUCCESS,
            correlation = HttpOperationCorrelation(
                inbound = "request-123",
                outbound = "request-123",
                mode = HttpOperationCorrelationMode.PROPAGATED,
            ),
            metricAttributes = mapOf(
                "http.request.method" to "GET",
                "http.route" to "/sales/{saleId}",
                "http.response.status_code" to "200",
                "correlation.present" to "true",
            ),
        )

    private fun successExpectation(): HttpOperationExpectation =
        HttpOperationExpectation(
            operationName = "http.server.requests",
            routeTemplate = "/sales/{saleId}",
            statusCode = 200,
            classification = HttpOperationClassification.SUCCESS,
            sensitiveValues = sensitiveValues(),
        )

    private companion object {
        fun Map<String, String>.withStatusCode(statusCode: Int?): Map<String, String> =
            if (statusCode == null) {
                this - "http.response.status_code"
            } else {
                this + ("http.response.status_code" to statusCode.toString())
            }

        val sensitiveValueSet = setOf(
            "https://example.test/sales/sale-123?user=user-456",
            "user=user-456",
            "203.0.113.7",
            "user-456",
            "sale-123",
            "payload-secret",
        )

        fun sensitiveValues(): HttpOperationSensitiveValues =
            HttpOperationSensitiveValues(
                rawUrl = "https://example.test/sales/sale-123?user=user-456",
                query = "user=user-456",
                clientIp = "203.0.113.7",
                userId = "user-456",
                saleId = "sale-123",
                requestPayload = "payload-secret",
            )
    }
}
