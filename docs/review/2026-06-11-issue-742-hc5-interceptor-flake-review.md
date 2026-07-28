# Issue #742 HC5 async interceptor ordering review

## Scope

- `io/http/src/test/kotlin/io/bluetape4k/http/hc5/examples/AsyncClientInterceptors.kt`

## 발견 사항

- P0: 0
- P1: 0
- P2: 0

## Review Notes

- The production HC5 async client code is unchanged.
- The flaky global event-list assertion was replaced with per-execution event assertions.
- Each request still verifies the exec-before, request interceptor, and exec-after ordering for the same execution id.
- The short-circuited 13th request still verifies exec-before then exec-short-circuit, without requiring unrelated async requests to serialize around it.

## Verification Evidence

- `./gradlew :bluetape4k-http:test --tests 'io.bluetape4k.http.hc5.examples.AsyncClientInterceptors' --no-daemon --no-configuration-cache --no-build-cache`: PASS, 1 test passing.
- `./gradlew :bluetape4k-http:test --no-daemon --no-configuration-cache --no-build-cache`: PASS, 444 tests passing, 3 pending.

## Residual Risk

- The original CI failure was an async interleaving flake, so local reruns cannot prove every scheduler interleaving. The assertion now removes the invalid cross-request ordering contract while retaining same-request ordering coverage.
