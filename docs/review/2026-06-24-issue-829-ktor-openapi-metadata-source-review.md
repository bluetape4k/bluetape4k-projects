# Issue 829 Ktor OpenAPI Metadata Source Review

## Scope

- `ktor/openapi/src/main/kotlin/io/bluetape4k/ktor/openapi/KtorOpenApiRoutes.kt`
- `ktor/openapi/src/test/kotlin/io/bluetape4k/ktor/openapi/KtorOpenApiRoutesTest.kt`
- `ktor/openapi/README.md`
- `ktor/openapi/README.ko.md`

## Verdict

APPROVE.

- P0: 0
- P1: 0

## Review Evidence

- Verified issue #829: existing wrappers called Ktor static-file overloads that
  run caller configuration and then overwrite `source` with
  `OpenApiDocSource.File(swaggerFile)`.
- Verified local Ktor 3.5.0 sources:
  - `Route.openAPI(path, swaggerFile, block)` delegates to `openAPI(path)` and
    then sets `source = OpenApiDocSource.File(swaggerFile)`.
  - `Route.swaggerUI(path, swaggerFile, block)` delegates to `swaggerUI(path)`
    and then sets `source = OpenApiDocSource.File(swaggerFile)` plus
    `remotePath`.
- Verified new regression coverage:
  - `openapi endpoint preserves caller owned document source`
  - `swagger ui endpoint preserves caller owned document source`
- Verified static file coverage still exists for both helpers.

## P2 Notes

The implementation detects Ktor's default source shape before applying the
static `swaggerFile` fallback. This is intentionally narrow for Ktor 3.5.0:
it preserves the existing default static-file behavior while allowing
caller-owned `OpenApiDocSource.Routing` or other explicit sources to survive.
If Ktor changes its default `OpenApiDocSource` shape in a future upgrade, this
helper should be revisited with that Ktor source change in hand.

## Validation

- RED: old implementation failed the two new source-preservation tests.
- GREEN: `./gradlew :bluetape4k-ktor-openapi:test --tests 'io.bluetape4k.ktor.openapi.KtorOpenApiRoutesTest' --no-build-cache`
  passed with 6 tests.
- Module gate: `./gradlew :bluetape4k-ktor-openapi:compileKotlin :bluetape4k-ktor-openapi:compileTestKotlin :bluetape4k-ktor-openapi:test --no-build-cache`
  passed with 6 `KtorOpenApiRoutesTest` cases.
- `git diff --check` passed.
