# 교훈: 이슈 806 OpenTelemetry redacted exception telemetry (2026-06-27)

이슈: #806
모듈: `infra/opentelemetry`

## 배경

OpenTelemetry span helper는 기본적으로 exported span status와 exception event에
exception message를 기록했다. Exception message에는 SQL fragment, URL, token,
user input이 들어갈 수 있으므로 helper-managed failure path는 raw message를
기본적으로 sensitive하게 다뤄야 한다.

## 결정

Helper-managed failure는 이제 redacted `exception` event를 내보내고 exported
message로 `"unspecified error"`를 설정해 `StatusCode.ERROR`를 기록한다. 원래
exception은 호출자에게 그대로 rethrow한다. 전체 OpenTelemetry `recordException`은
raw exception message export가 허용된 경계에서 명시적으로 opt-in할 때만 사용한다.

## Test helper 판단

이 변경은 coroutine과 Flow failure path에 영향을 주므로 `SuspendedJobTester`가 맞는
bluetape4k helper다. Coroutine/Flow redaction stress test에 이 helper를 사용했다.

`MultithreadingTester`와 `StructuredTaskScopeTester`는 이 수정이 shared mutable
production state, thread contention, virtual-thread behavior, `StructuredTaskScope`
semantics를 추가하지 않으므로 사용하지 않았다.

## 향후 방지책

Telemetry helper는 status description과 event attribute를 모두 테스트한다.
`recordException(error)`는 `exception.message`를 export할 수 있으므로 status-only
assertion만으로는 secret-leak regression을 잡기에 충분하지 않다.
