---
title: Error responses and observations
description: Explains API exception status mapping, message exposure, Micrometer Observation scope, and cardinality.
manualId: bluetape4k-spring-boot-core
chapterId: error-responses-and-observability
---

# Error responses and observations

## Keep a stable error-body shape

`ApiErrorBody` contains an `errorCode`, creation `timestamp`, and `message`. `apiErrorResponseEntityOf` combines a status and body in a `ResponseEntity`.

```json
{
  "errorCode": "USER_NOT_FOUND",
  "timestamp": "2026-07-14T01:23:45Z",
  "message": "User not found"
}
```

The actual JSON representation depends on the application's Jackson 3 configuration. This module does not configure the `Instant` serializer or naming policy.

## Know the handled exception set

`ApiExceptionHandler` maps these statuses explicitly:

| Exception | Status |
| --- | --- |
| `ApiBadRequestException`, `HttpMessageNotReadableException` | 400 |
| `ApiUnauthorizedException` | 401 |
| `ApiForbiddenException` | 403 |
| `ApiEntityNotFoundException` | 404 |
| `ApiTooManyRequestsException` | 429 |
| `ApiInternalServerErrorException` | 500 |
| `ApiServiceUnavailableException` | 503 |

There is no catch-all `Throwable` handler. Map validation, authentication, and other framework exceptions in application advice when they need the same response shape.

## Treat messages as an external contract

The handler logs stack traces and omits them from the body, but it copies `exception.message` into the response. SQL text, internal hosts, credentials, or nested-cause details can therefore leak.

Separate public messages from internal diagnostics. Use `errorCode` as the stable client decision field, and keep detailed causes in logs and traces.

## Observe with the application registry

```kotlin
val order = observationRegistry.observeSpring(
    name = "order.service.load",
    keyValues = SpringObservationKeyValues(
        lowCardinality = KeyValues.of("component", "order-service"),
    ),
) { context ->
    context.addLowCardinalityKeyValue(KeyValue.of("outcome", "success"))
    repository.load(id)
}
```

The helper creates and starts an observation, opens its scope, and stops it on every path. It records ordinary exceptions as observation errors and rethrows them. It rethrows `CancellationException` without recording an error.

## Propagate scope through suspension

`observeSpringSuspending` binds the current Observation into Reactor Context and coroutine thread context. The current observation remains readable after suspension and the scope is cleaned after completion.

The helper does not install an exporter, Prometheus endpoint, or OpenTelemetry SDK. The application owns Spring Boot Actuator and Micrometer Tracing configuration.

## Control cardinality

Use bounded fields such as service, operation, and outcome as low-cardinality values. Do not turn user IDs, request IDs, or raw URIs into metrics labels. Put sanitized values in high-cardinality trace fields only when needed.

## Source and tests

- [`ApiErrorResponse.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiErrorResponse.kt)
- [`ApiExceptions.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptions.kt)
- [`ApiExceptionHandler.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandler.kt)
- [`SpringObservationSupport.kt`](../../../../../spring-boot/core/src/main/kotlin/io/bluetape4k/spring/observability/SpringObservationSupport.kt)
- [`ApiExceptionHandlerTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/rest/exceptions/ApiExceptionHandlerTest.kt)
- [`SpringObservationSupportTest.kt`](../../../../../spring-boot/core/src/test/kotlin/io/bluetape4k/spring/observability/SpringObservationSupportTest.kt)

## Next chapter

[Execution resources and lifecycle](./execution-resources-and-lifecycle.md) covers ownership and shutdown of HTTP loops, coroutine scopes, and executors.
