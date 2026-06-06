# Issue #698 HC5 interceptor ordering review

## Scope

- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/examples/ClientInterceptors.kt`
- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/examples/AsyncClientInterceptors.kt`

## Findings

- P0: 0
- P1: 0
- P2: 0

## Review Notes

- The examples now assert the documented HC5 ordering instead of leaving FIXME comments.
- The custom exec interceptor records that `request-id` is missing before the request protocol interceptor runs.
- The request protocol interceptor records `request-id` only after execution reaches the transport path.
- The 13th request short-circuit is asserted for both classic and async clients.
- The async example now uses the local httpbin prefix `/httpbin/get`, matching the repository test server contract.

## Verification Evidence

- `./gradlew :bluetape4k-http:test --tests "io.bluetape4k.http.hc5.examples.ClientInterceptors" --tests "io.bluetape4k.http.hc5.examples.AsyncClientInterceptors"`: PASS, 2 tests passing.
- `git diff --check`: PASS.
- `rg -n "FIXME|why does this run|ExecInterceptorAfter runs before request interceptor" ...`: PASS, no matches in touched example files.

## Residual Risk

- Full `:bluetape4k-http:test` was not run because the change is limited to two example tests and the targeted test task compiled the module test source and executed both touched classes.
