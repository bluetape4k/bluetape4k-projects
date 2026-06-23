# Issue 841 - mock-webflux README routes

## Context

`testing/mock-webflux-server` README files listed several httpbin/admin
endpoints that were not implemented by the WebFlux controllers. Consumers using
the README as an integration-test contract could call those routes and receive
404 responses.

## Decision

Keep the implemented route surface unchanged and remove README-only endpoint
rows from both locales.

The removed rows were `/admin/info`, `/httpbin/stream-bytes/{n}`,
`/httpbin/drip`, `/httpbin/sse`, `/httpbin/brotli`, `/httpbin/html`,
`/httpbin/xml`, `/httpbin/json`, `/httpbin/robots.txt`, and `/httpbin/deny`.

## Follow-up Guard

`ReadmeRouteContractTest` now blocks those stale routes from returning to the
README endpoint tables unless matching WebFlux controller mappings are added.
