# Issue 645 Ktor OpenAPI

## Context

Ktor OpenAPI support was a backlog follow-up after the core, testing,
observability, client, and resilience slices.

## Decision

Use Ktor's official OpenAPI and Swagger UI plugins through a small
`bluetape4k-ktor-openapi` module. Add only Ktor version-governed aliases and
avoid Swagger Codegen because this module does not own custom renderer
generation.

## Outcome

Applications get explicit `bluetape4kOpenApi()` and `bluetape4kSwaggerUi()`
route helpers while keeping specs and route metadata application-owned.

## Verification

Run module compile/test/Kover, workflow lint, and diff hygiene before merging.

## Future Guidance

Do not add generated route behavior here. If Ktor runtime annotation APIs change,
adjust documentation and wrappers only where the official API remains stable.
