# 교훈: 이슈 #827 Ktor correlation tracing

## 배경

`bluetape4k-ktor-observability`는 OpenTelemetry Ktor extractor의 `onStart`
callback에서 `correlation.present`와 optional `correlation.id`를 기록했다.
OpenTelemetry plugin은 Ktor `ApplicationCallPipeline.Setup`보다 먼저 시작하고,
Ktor `CallId`는 `Setup` 중 call id를 sanitize하거나 생성한다.

## 교훈

Tracing attribute가 call pipeline에서 더 늦게 실행되는 framework plugin에 의존하면,
그 값이 sampler decision에 참여해야 하는 경우가 아니라면 span end에서 캡처한다.
Ktor server span에서는 생성된 `CallId` 값이 OpenTelemetry extractor `onEnd` callback
시점에 사용 가능하다.

## 결과

Tracing helper는 이제 `onEnd`에서 correlation attribute를 기록한다. 따라서 header가
없는 request도 HTTP response와 server span에 같은 generated `X-Request-Id`를 남긴다.

## 향후 방지책

Ktor OpenTelemetry extractor timing, CallId installation order, correlation
header policy를 변경할 때는 generated correlation ID regression coverage를 유지한다.

## 검증

- Regression test failed before the fix with `correlation.present=false`.
- Targeted regression test passed after the fix.
- Full `:bluetape4k-ktor-observability` compile/test/Kover validation passed.
- `git diff --check` passed.
