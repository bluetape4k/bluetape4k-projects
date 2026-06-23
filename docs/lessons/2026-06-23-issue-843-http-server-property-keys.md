# Issue 843 - BluetapeHttpServer property keys

## Context

`PropertyExportingServer` defines exported system property keys as lowercase
kebab-case under `testcontainers.{propertyNamespace}.{key}`.

`BluetapeWebfluxServer` already followed this contract with `httpbin-url`,
`jsonplaceholder-url`, and `web-url`, but `BluetapeHttpServer` exposed the
same values as `httpbinUrl`, `jsonplaceholderUrl`, and `webUrl`.

## Decision

Make `BluetapeHttpServer.propertyKeys()` return only the canonical kebab-case
keys used by the shared export contract.

`properties()` now writes canonical kebab-case entries and uses
`withCompatKeys` to keep the previous camelCase keys as compatibility aliases
for existing downstream tests.

## Follow-up Guard

Keep public README placeholders aligned with `PropertyExportingServer`.
`PropertyExportingServerContractTest` now includes both mock HTTP server
wrappers so future divergence between `BluetapeHttpServer` and
`BluetapeWebfluxServer` fails without starting Docker.
