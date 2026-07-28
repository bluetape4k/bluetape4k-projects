# Review - Issue #792 HTTP logging header redaction (2026-06-27)

## Scope

- Issue: #792, `P1: HTTP logging can expose sensitive headers`
- Modules: `:bluetape4k-http`, `:bluetape4k-retrofit2`
- Changed paths:
  - `io/http/src/main/kotlin/io/bluetape4k/http/okhttp3/HttpHeaderRedaction.kt`
  - `io/http/src/main/kotlin/io/bluetape4k/http/okhttp3/LoggingInterceptor.kt`
  - `io/http/src/test/kotlin/io/bluetape4k/http/okhttp3/LoggingInterceptorTest.kt`
  - `io/retrofit2/src/main/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3Support.kt`
  - `io/retrofit2/src/test/kotlin/io/bluetape4k/retrofit2/clients/hc5/Hc5OkHttp3SupportTest.kt`
  - `io/http/README.md`
  - `io/http/README.ko.md`

## 발견 사항

- P0: 0
- P1: 0
- P2: 0
- P3: 1

### P3 - Redaction helper normalizes custom header names per lookup

`isSensitiveHttpHeaderName` normalizes `additionalSensitiveHeaderNames` for each call. The current call sites pass small sets and HTTP header counts are small, so this is not a release blocker. If future logging paths apply large policy sets or hot-path structured logging, pre-normalize the policy once at the call boundary.

## Review Notes

- `LoggingInterceptor` no longer delegates header formatting to OkHttp `Headers.toString()`. That OkHttp path redacts some built-in sensitive names but does not cover project/API-key headers required by #792.
- `Hc5OkHttp3Support.toSimpleHttpRequest()` redacts only the trace log value. The generated `SimpleHttpRequest` still receives the original raw header values.
- The public factory `LoggingInterceptor(logger)` remains available, and the new `LoggingInterceptor(logger, additionalSensitiveHeaderNames)` overload gives callers a source-compatible way to add project-specific sensitive names.
- README locale pair was updated because the public logging behavior changed.
- Concurrency helper gate: not applicable. The change formats immutable request/response header snapshots and does not add shared mutable state, coroutines, virtual threads, structured concurrency, or stress-sensitive lifecycle behavior. No `MultithreadingTester`, `SuspendedJobTester`, or `StructuredTaskScopeTester` was needed.

## Verification Evidence

- RED: `./gradlew :bluetape4k-http:test --tests "io.bluetape4k.http.okhttp3.LoggingInterceptorTest.LoggingInterceptor - sensitive request and response headers are redacted" --no-daemon --no-configuration-cache` failed before the fix because sensitive response `X-Api-Key` remained visible and OkHttp built-in redaction used a different marker.
- RED: `./gradlew :bluetape4k-retrofit2:test --tests "io.bluetape4k.retrofit2.clients.hc5.Hc5OkHttp3SupportTest" --no-daemon --no-configuration-cache` failed before the fix because HC5 trace logs contained raw `Authorization` and `X-Api-Key` values.
- GREEN targeted: `./gradlew :bluetape4k-http:test --tests "io.bluetape4k.http.okhttp3.LoggingInterceptorTest" :bluetape4k-retrofit2:test --tests "io.bluetape4k.retrofit2.clients.hc5.Hc5OkHttp3SupportTest" --no-daemon --no-configuration-cache` passed.
- GREEN compile: `./gradlew :bluetape4k-http:compileTestKotlin :bluetape4k-retrofit2:compileTestKotlin --warning-mode all --rerun-tasks --no-daemon --no-configuration-cache` passed. Remaining warnings were existing Gradle Kotlin DSL deprecations outside the touched files.
- GREEN module tests: `./gradlew :bluetape4k-http:test --no-daemon --no-configuration-cache` passed with 446 passing and 3 pending tests.
- GREEN module tests: `./gradlew :bluetape4k-retrofit2:test --no-daemon --no-configuration-cache` passed with 293 passing and 1 pending test.
- Static diff check: `git diff --check` passed with no output.
- Source scan: searched HTTP and Retrofit production/test sources for raw header diagnostic paths; changed paths route sensitive diagnostic values through `toRedactedString` or `redactHttpHeaderValue`.
