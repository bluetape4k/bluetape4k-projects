# 교훈: 이슈 #792 HTTP logging header redaction (2026-06-27)

## 배경

이슈 #792는 HTTP diagnostic log가 credential-bearing header를 노출할 수 있음을
보여주었다. 영향을 받은 경로는 OkHttp request/response logging과 Retrofit HC5
request conversion trace logging이었다.

## 결정

Diagnostic log 값을 formatting하기 전에 하나의 공유 HTTP header redaction helper를
사용한다. 이 helper는 잘 알려진 credential header, API-key 이름, token-like 이름,
호출자가 지정한 project-specific header 이름을 redaction한다.

## 결과

- `LoggingInterceptor`는 이제 redacted request/response header를 log에 남긴다.
- `Hc5OkHttp3Support.toSimpleHttpRequest()`는 실제 outgoing request header는 유지하면서 trace log에서만 sensitive header value를 redaction한다.
- `io/http` README locale pair는 기본 redaction policy와 custom extension 경로를 문서화한다.

## 검증

- RED test가 먼저 `LoggingInterceptor`와 `Hc5OkHttp3Support`의 raw sensitive header leakage를 재현했다.
- `LoggingInterceptorTest`와 `Hc5OkHttp3SupportTest` targeted test가 통과했다.
- `:bluetape4k-http:test`와 `:bluetape4k-retrofit2:test` 전체 module test가 통과했다.
- `:bluetape4k-http:compileTestKotlin`과 `:bluetape4k-retrofit2:compileTestKotlin`은 `--warning-mode all --rerun-tasks`로 통과했다. 남은 warning은 touched code 밖의 기존 Gradle Kotlin DSL deprecation이었다.
- `git diff --check`가 통과했다.

## 향후 방지책

HTTP header 값을 직접 log하지 않는다. 새 HTTP diagnostic path는 개별 name/value
log에 `redactHttpHeaderValue`를 호출하거나 OkHttp header block에
`Headers.toRedactedString`을 사용해야 한다. 변경이 동시성, coroutine,
virtual-thread, structured-concurrency 동작을 도입한다면 맞는 bluetape4k concurrency
helper를 사용하고 PR DoD에 helper evidence를 기록한다.
