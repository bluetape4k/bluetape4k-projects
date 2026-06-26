# Lessons Learned - Issue #792 HTTP logging header redaction (2026-06-27)

## Context

Issue #792 showed that HTTP diagnostic logs could expose credential-bearing headers. The affected paths were OkHttp request/response logging and Retrofit HC5 request conversion trace logging.

## Decision

Use one shared HTTP header redaction helper before formatting diagnostic log values. The helper redacts well-known credential headers, API-key names, token-like names, and caller-provided project-specific header names.

## Outcome

- `LoggingInterceptor` now logs redacted request and response headers.
- `Hc5OkHttp3Support.toSimpleHttpRequest()` now redacts sensitive header values only in trace logs while preserving the real outgoing request headers.
- `io/http` README locale pair documents the default redaction policy and custom extension path.

## Verification

- RED tests first reproduced raw sensitive header leakage in `LoggingInterceptor` and `Hc5OkHttp3Support`.
- Targeted tests passed for `LoggingInterceptorTest` and `Hc5OkHttp3SupportTest`.
- Full module tests passed for `:bluetape4k-http:test` and `:bluetape4k-retrofit2:test`.
- `:bluetape4k-http:compileTestKotlin` and `:bluetape4k-retrofit2:compileTestKotlin` passed with `--warning-mode all --rerun-tasks`; remaining warnings were existing Gradle Kotlin DSL deprecations outside the touched code.
- `git diff --check` passed.

## Future Guard

Do not log HTTP header values directly. New HTTP diagnostic paths should call `redactHttpHeaderValue` for individual name/value logs or `Headers.toRedactedString` for OkHttp header blocks. If the change introduces concurrency, coroutine, virtual-thread, or structured-concurrency behavior, use the matching bluetape4k concurrency helper and record the helper evidence in the PR DoD.
