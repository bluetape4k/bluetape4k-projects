# API contract failures should preserve caller context

## Context

Issues #951 and #954 found helper APIs where public contracts drifted from
caller expectations:

- Ktor Swagger UI collapsed nested specification paths to a basename.
- RestClient coroutine helpers used `!!`, turning empty bodies into raw
  `NullPointerException` failures.

## Decision

- Preserve caller-provided relative Swagger specification paths for both the
  file source and Swagger UI remote path.
- Replace RestClient coroutine `!!` assertions with an explicit non-null body
  contract error that includes HTTP method, URI, and target type.

## Verification

- `./gradlew :bluetape4k-ktor-openapi:test --tests 'io.bluetape4k.ktor.openapi.KtorOpenApiRoutesTest'`
- `./gradlew :bluetape4k-spring-boot-core:test --tests 'io.bluetape4k.spring.http.RestClientCoroutinesDslTest'`
- `git diff --check`

## Future guidance

Route helper defaults must keep documented path contracts source-verified.
Coroutine wrappers around blocking clients should expose explicit caller-facing
contract errors instead of relying on Kotlin null assertions.
