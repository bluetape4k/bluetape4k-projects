# Issue 829 Ktor OpenAPI Metadata Source

## Context

`bluetape4k-ktor-openapi` advertised static files, compiler-generated OpenAPI
metadata, and runtime `.describe {}` metadata as application-owned document
sources.

## Lesson

Ktor's `openAPI(path, swaggerFile, block)` and
`swaggerUI(path, swaggerFile, block)` overloads apply the caller block first and
then force `source = OpenApiDocSource.File(swaggerFile)`. Wrapper helpers that
accept a configuration block must delegate through Ktor's `block`-only overload
when caller-owned `OpenApiDocSource` values need to survive.

## Guard

When README or KDoc claims Ktor routing-tree OpenAPI metadata is supported,
tests must cover `OpenApiDocSource.Routing` through both the OpenAPI endpoint
and the Swagger UI specification endpoint. A static YAML happy path is not
enough because it does not prove the caller-owned `source` contract.

## Evidence

- RED: `KtorOpenApiRoutesTest` added routing metadata source tests; old wrapper
  failed 2 of 6 tests because the Ktor static-file overload overwrote source.
- GREEN: `./gradlew :bluetape4k-ktor-openapi:compileKotlin :bluetape4k-ktor-openapi:compileTestKotlin :bluetape4k-ktor-openapi:test --no-build-cache`
  passed with 6 `KtorOpenApiRoutesTest` cases.
- Official Ktor docs and local Ktor 3.5.0 sources both show runtime metadata
  via `OpenApiDocSource.Routing`.
