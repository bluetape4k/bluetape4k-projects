# Issue 645 Ktor OpenAPI Design

## Context

Issue #645 asks for optional Ktor OpenAPI/documentation support that adds
reusable value without replacing Ktor routing. The route behavior must stay
independent from documentation generation.

## Dependency Evaluation

Ktor 3.5.0 provides official OpenAPI and Swagger UI support:

- `ktor-server-openapi` serves OpenAPI documentation from an existing YAML/JSON
  file or from runtime metadata.
- `ktor-server-routing-openapi` provides runtime route annotation APIs such as
  `.describe {}` and can be combined with compiler-generated metadata.
- `ktor-server-swagger` serves Swagger UI.
- `swagger-codegen-generators` is optional and only needed for custom renderer
  generation.

References:

- https://ktor.io/docs/server-openapi.html
- https://ktor.io/docs/openapi-spec-generation.html

The repository catalog already governs the Ktor version, so the new aliases use
`version.ref = "ktor"` and do not introduce a second compatibility line.
`swagger-codegen-generators` is intentionally not added because the module only
needs route documentation endpoints, not custom code generation.

## Decision

Publish `:bluetape4k-ktor-openapi` as a thin wrapper over the official Ktor
OpenAPI and Swagger UI plugins.

The module provides:

- `bluetape4kOpenApi()` with default path `openapi` and default spec
  `openapi/documentation.yaml`.
- `bluetape4kSwaggerUi()` with default path `swagger` and the same default spec.

Applications remain responsible for:

- static OpenAPI YAML/JSON documents,
- Ktor compiler OpenAPI extension configuration,
- route-level `.describe {}` metadata,
- schema/security/server details.

## Risks

- Ktor's runtime annotation APIs are marked experimental by upstream; this
  module documents them but does not wrap their unstable types.
- Static specs can drift from actual route behavior. Keep route metadata close
  to route declarations or use compiler/runtime metadata where practical.
- Static specs should include `components.schemas`, even when empty, because the
  Ktor OpenAPI HTML renderer delegates to Swagger Codegen internals that expect
  the schema map to exist.
- Swagger UI rendering behavior belongs to Ktor; bluetape4k should not fork or
  proxy the UI assets.

## Done

- The module compiles against Ktor 3.5.0.
- Static OpenAPI and Swagger UI endpoints are tested.
- README and README.ko describe opt-in usage and explicit route metadata.
