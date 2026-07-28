# 이슈 #828 Ktor cancellation span

## 배경

Observability 계약은 intentional coroutine cancellation이 error metric이나 `ERROR`
span status가 되면 안 된다고 말한다. `ktor/observability`는 Ktor server span lifecycle을
OpenTelemetry의 Ktor instrumentation에 위임했지만, `CancellationException`을 던지는
route에 대한 regression test가 없었다.

## 결정

Route에서 `CancellationException`을 던지고 exported span이 `StatusCode.ERROR`를 절대
사용하지 않음을 검증하는 Ktor OpenTelemetry regression test를 추가한다. 나중에
instrumentation이 cancellation span을 export하더라도 이 테스트는 status가 `UNSET`으로
남아야 함을 요구한다.

## 결과

현재 OpenTelemetry Ktor 동작은 Ktor test host에서 취소된 route의 span을 export하지
않는다. 이는 cancellation에 대해 "no ERROR span"을 이미 만족하며, 새 테스트는 실제
500 response가 계속 `ERROR`를 기록한다는 기존 coverage를 유지하면서 이 계약을 잠근다.

## 검증

- Targeted cancellation and real-error tracing tests
- Full `:bluetape4k-ktor-observability` compile/test command
- `git diff --check`

## 향후 방지책

Framework instrumentation을 감쌀 때는 cancellation을 일반 handler failure와 분리해서
테스트한다. Framework의 error response 형태가 tracing status contract와 같다고
가정하지 않는다.
