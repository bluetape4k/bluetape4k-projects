package io.bluetape4k.junit5.observability

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.io.Serializable

/** Logger name used by the HTTP operation observability conformance fixture. */
const val HTTP_OPERATION_OBSERVABILITY_LOGGER_NAME: String =
    "io.bluetape4k.junit5.observability.HttpOperationObservabilityConformance"

/**
 * Stable classifications shared by HTTP operation observability tests.
 */
enum class HttpOperationClassification {
    SUCCESS,
    CLIENT_ERROR,
    TIMEOUT_OR_CANCELLATION,
    DEPENDENCY_FAILURE,
}

/**
 * Describes how the outbound correlation value relates to the inbound request.
 */
enum class HttpOperationCorrelationMode {
    PROPAGATED,
    GENERATED,
    ABSENT,
}

/**
 * Captures inbound and outbound correlation values for propagation assertions.
 *
 * The fixture compares these values but never writes them to its log.
 */
data class HttpOperationCorrelation(
    val inbound: String?,
    val outbound: String?,
    val mode: HttpOperationCorrelationMode,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Representative sensitive inputs that must be absent from metric attributes,
 * operation names, route templates, and fixture logs.
 */
data class HttpOperationSensitiveValues(
    val rawUrl: String,
    val query: String,
    val clientIp: String,
    val userId: String,
    val saleId: String,
    val requestPayload: String,
): Serializable {
    internal fun asList(): List<String> =
        listOf(rawUrl, query, clientIp, userId, saleId, requestPayload)

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Framework-neutral snapshot of an observed HTTP operation.
 *
 * Spring Boot and Ktor tests adapt their fake registry or tracer output into
 * this value without introducing a shared production routing API.
 */
data class HttpOperationObservation(
    val operationName: String,
    val routeTemplate: String,
    val statusCode: Int?,
    val classification: HttpOperationClassification,
    val correlation: HttpOperationCorrelation,
    val metricAttributes: Map<String, String>,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Expected stable values and known sensitive inputs for one HTTP operation.
 *
 * [sensitiveValues] contains representative raw identifiers, addresses, query
 * values, and payload fragments supplied to the framework integration.
 */
data class HttpOperationExpectation(
    val operationName: String,
    val routeTemplate: String,
    val statusCode: Int?,
    val classification: HttpOperationClassification,
    val sensitiveValues: HttpOperationSensitiveValues,
): Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Verifies the shared HTTP operation observability contract.
 *
 * The assertion checks stable operation and route names, status/failure
 * classification, correlation propagation, and low-cardinality metric
 * attributes. It rejects common raw URL, query, address, identity, and payload
 * attribute keys as well as caller-supplied sensitive values.
 *
 * A successful check emits one bounded summary log containing only the
 * classification, status code, and attribute count. Raw telemetry values are
 * never logged by this fixture. The caller owns and closes every registry,
 * tracer, exporter, and SDK; this function only inspects the supplied snapshot.
 */
fun assertHttpOperationObservability(
    observation: HttpOperationObservation,
    expectation: HttpOperationExpectation,
) {
    assertRedacted(observation.operationName == expectation.operationName, "operation name")
    assertRedacted(observation.routeTemplate == expectation.routeTemplate, "route template")
    assertRedacted(observation.statusCode == expectation.statusCode, "status code")
    assertRedacted(observation.classification == expectation.classification, "classification")
    assertRedacted(isClassificationCompatible(observation), "classification semantics")

    assertRedacted(!observation.routeTemplate.contains('?'), "route cardinality")
    assertRedacted(!observation.routeTemplate.contains("://"), "route cardinality")
    assertRedacted(
        observation.metricAttributes["http.route"] == expectation.routeTemplate,
        "metric route template",
    )
    assertRedacted(
        observation.metricAttributes["http.response.status_code"] == observation.statusCode?.toString(),
        "metric status code",
    )

    assertCorrelation(observation.correlation)
    val correlationPresent = observation.correlation.inbound != null || observation.correlation.outbound != null
    assertRedacted(
        observation.metricAttributes["correlation.present"] == correlationPresent.toString(),
        "metric correlation presence",
    )

    assertRedacted(
        observation.metricAttributes.keys.none(::isForbiddenMetricAttributeKey),
        "metric attribute key safety",
    )
    assertRedacted(
        expectation.sensitiveValues.asList().all(String::isNotBlank),
        "sensitive input coverage",
    )

    val telemetryText = buildString {
        appendLine(observation.operationName)
        appendLine(observation.routeTemplate)
        observation.metricAttributes.forEach { (key, value) ->
            appendLine("$key=$value")
        }
    }
    assertRedacted(
        expectation.sensitiveValues.asList().none(telemetryText::contains),
        "sensitive telemetry exclusion",
    )

    HttpOperationObservabilityConformance.logVerified(observation)
}

private fun assertCorrelation(correlation: HttpOperationCorrelation) {
    val matchesMode = when (correlation.mode) {
        HttpOperationCorrelationMode.PROPAGATED ->
            correlation.inbound != null && correlation.outbound == correlation.inbound

        HttpOperationCorrelationMode.GENERATED ->
            correlation.inbound == null && correlation.outbound != null

        HttpOperationCorrelationMode.ABSENT ->
            correlation.inbound == null && correlation.outbound == null
    }
    assertRedacted(matchesMode, "correlation contract")
}

private fun isClassificationCompatible(observation: HttpOperationObservation): Boolean =
    when (observation.classification) {
        HttpOperationClassification.SUCCESS ->
            observation.statusCode != null && observation.statusCode in 100..399

        HttpOperationClassification.CLIENT_ERROR ->
            observation.statusCode != null && observation.statusCode in 400..499

        HttpOperationClassification.TIMEOUT_OR_CANCELLATION ->
            observation.statusCode == null || observation.statusCode in setOf(408, 499, 504)

        HttpOperationClassification.DEPENDENCY_FAILURE ->
            observation.statusCode == null || observation.statusCode >= 500
    }

private fun assertRedacted(condition: Boolean, field: String) {
    if (!condition) {
        throw AssertionError("HTTP observability conformance failed for $field; values redacted")
    }
}

private object HttpOperationObservabilityConformance: KLogging() {
    fun logVerified(observation: HttpOperationObservation) {
        log.info {
            "HTTP observability conformance verified: " +
                    "classification=${observation.classification}, " +
                    "statusCode=${observation.statusCode}, " +
                    "metricAttributeCount=${observation.metricAttributes.size}"
        }
    }
}

private fun isForbiddenMetricAttributeKey(key: String): Boolean {
    val normalized = key.lowercase().replace('_', '.').replace('-', '.')
    val compact = normalized.replace(".", "")
    return normalized in FORBIDDEN_METRIC_ATTRIBUTE_KEYS ||
            FORBIDDEN_METRIC_ATTRIBUTE_FRAGMENTS.any(normalized::contains) ||
            FORBIDDEN_METRIC_ATTRIBUTE_COMPACT_FRAGMENTS.any(compact::contains)
}

private val FORBIDDEN_METRIC_ATTRIBUTE_KEYS = setOf(
    "url.full",
    "url.query",
    "http.url",
    "http.query",
    "client.address",
    "network.peer.address",
    "remote.address",
    "ip.address",
    "request.body",
    "response.body",
    "http.request.body",
    "http.response.body",
)

private val FORBIDDEN_METRIC_ATTRIBUTE_FRAGMENTS = setOf(
    "user.id",
    "sale.id",
    "request.payload",
    "response.payload",
)

private val FORBIDDEN_METRIC_ATTRIBUTE_COMPACT_FRAGMENTS = setOf(
    "clientip",
    "userid",
    "saleid",
    "requestpayload",
    "responsepayload",
)
