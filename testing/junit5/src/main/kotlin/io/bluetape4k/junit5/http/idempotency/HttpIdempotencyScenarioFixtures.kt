package io.bluetape4k.junit5.http.idempotency

import java.time.Duration

internal fun request(
    authenticationProfile: String = "tenant-a-principal",
    operation: String = "create-widget",
    resourceIdentity: String = "widget-1",
    idempotencyKeys: List<String> = listOf("fixture-key"),
    requestBody: String = "{\"name\":\"sample\"}",
): HttpIdempotencyRequest = HttpIdempotencyRequest(
    authenticationProfile = authenticationProfile,
    operation = operation,
    resourceIdentity = resourceIdentity,
    idempotencyKeys = idempotencyKeys,
    requestBody = requestBody,
)

internal fun createdResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 201,
    body = "{\"id\":\"widget-1\"}",
    headers = mapOf("content-type" to listOf("application/json")),
)

internal fun deterministicFailureResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 422,
    body = "{\"code\":\"validation_failed\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "validation_failed",
)

internal fun transientFailureResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 503,
    body = "{\"code\":\"temporarily_unavailable\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "temporarily_unavailable",
)

internal fun idempotencyConflictResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 409,
    body = "{\"code\":\"idempotency_key_reused\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "idempotency_key_reused",
)

internal fun unauthenticatedResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 401,
    body = "{\"code\":\"authentication_required\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "authentication_required",
)

internal fun unauthorizedResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 403,
    body = "{\"code\":\"forbidden\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "forbidden",
)

internal fun invalidIdempotencyRequestResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 400,
    body = "{\"code\":\"invalid_idempotency_request\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "invalid_idempotency_request",
)

internal fun oversizedRequestResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 413,
    body = "{\"code\":\"idempotency_request_too_large\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "idempotency_request_too_large",
)

internal fun unsafeReplaySnapshotResponse(): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 500,
    body = "{\"code\":\"idempotency_snapshot_rejected\"}",
    headers = mapOf("content-type" to listOf("application/problem+json")),
    problemCode = "idempotency_snapshot_rejected",
)

internal fun inFlightTimeoutResponse(
    config: BoundedWaitHttpIdempotencyConformanceConfig,
): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 409,
    body = "{\"code\":\"idempotency_in_flight\"}",
    headers = mapOf(
        "content-type" to listOf("application/problem+json"),
        "retry-after" to listOf(config.inFlightRetryAfter.seconds.toString()),
    ),
    problemCode = "idempotency_in_flight",
)

internal fun waiterOverflowResponse(
    config: BoundedWaitHttpIdempotencyConformanceConfig,
): HttpIdempotencyResponse = HttpIdempotencyResponse(
    statusCode = 429,
    body = "{\"code\":\"idempotency_waiters_exceeded\"}",
    headers = mapOf(
        "content-type" to listOf("application/problem+json"),
        "retry-after" to listOf(config.overflowRetryAfter.seconds.toString()),
    ),
    problemCode = "idempotency_waiters_exceeded",
)

internal fun HttpIdempotencyResponse.withReplayFlag(replayed: Boolean): HttpIdempotencyResponse = copy(
    headers = headers + ("idempotency-replayed" to listOf(replayed.toString())),
)

internal fun representativeNormalRequests(): List<HttpIdempotencyRequest> = listOf(
    request(),
    request(
        resourceIdentity = "widget-fan-in",
        idempotencyKeys = listOf("fan-in-" + "x".repeat(57)),
        requestBody = "{\"name\":\"fan-in\"}",
    ),
)

internal fun representativeTerminalResponses(): List<HttpIdempotencyResponse> = listOf(
    createdResponse(),
    deterministicFailureResponse(),
    transientFailureResponse(),
)

internal fun config(
    waitTimeout: Duration = Duration.ofSeconds(2),
    scenarioTimeout: Duration = Duration.ofSeconds(15),
    maxWaitersPerKey: Int = 2,
    retention: Duration = Duration.ofHours(1),
    inFlightRetryAfter: Duration = Duration.ofSeconds(1),
    overflowRetryAfter: Duration = Duration.ofSeconds(2),
    maxIdempotencyKeyBytes: Int = 255,
    maxRequestBodyBytes: Int = 64 * 1024,
    maxReplayBodyBytes: Int = 64 * 1024,
    maxReplayHeaderNames: Int = 8,
    maxReplayValuesPerHeader: Int = 4,
    maxReplayHeaderValueBytes: Int = 4 * 1024,
    maxReplayHeaderBytes: Int = 16 * 1024,
    replayHeaderAllowlist: Set<String> = emptySet(),
): BoundedWaitHttpIdempotencyConformanceConfig = BoundedWaitHttpIdempotencyConformanceConfig(
    waitTimeout = waitTimeout,
    scenarioTimeout = scenarioTimeout,
    maxWaitersPerKey = maxWaitersPerKey,
    retention = retention,
    inFlightRetryAfter = inFlightRetryAfter,
    overflowRetryAfter = overflowRetryAfter,
    maxIdempotencyKeyBytes = maxIdempotencyKeyBytes,
    maxRequestBodyBytes = maxRequestBodyBytes,
    maxReplayBodyBytes = maxReplayBodyBytes,
    maxReplayHeaderNames = maxReplayHeaderNames,
    maxReplayValuesPerHeader = maxReplayValuesPerHeader,
    maxReplayHeaderValueBytes = maxReplayHeaderValueBytes,
    maxReplayHeaderBytes = maxReplayHeaderBytes,
    replayHeaderAllowlist = replayHeaderAllowlist,
)
