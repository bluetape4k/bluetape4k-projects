# Issue 833 - Spring WebClient README examples

## Context

`bluetape4k-spring-boot-core` documents `WebClient.httpGet` and
`WebClient.httpPost` helpers in both English and Korean READMEs.

Those helpers already call `retrieve()` internally and return
`WebClient.ResponseSpec`, but the README snippets chained another `retrieve()`.
The documented examples therefore failed to compile even though the extension
functions themselves worked.

## Decision

Keep the public helper contract unchanged and fix the examples to consume the
returned `ResponseSpec` directly:

- `httpGet("/users").bodyToFlux(User::class.java).asFlow()`
- `httpPost("/users", newUser).bodyToMono(User::class.java).awaitSingle()`

Add `WebClientReadmeExamplesTest` so README drift is caught by tests:

- the README files must not chain `.retrieve()` after `httpGet` or `httpPost`
- the documented `bodyToFlux` and `bodyToMono` chains compile against the real
  extension functions

## Follow-up Guard

When WebClient helper return types change, update `README.md`,
`README.ko.md`, and `WebClientReadmeExamplesTest` together. Public snippets
should demonstrate the exact next call available on the returned type.
