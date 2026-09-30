# OpenFGA 로그에 tuple·cursor와 예외 체인을 남기지 않는다

관련 이슈: [#1799](https://github.com/bluetape4k/bluetape4k-projects/issues/1799)

관련 PR: [#1807](https://github.com/bluetape4k/bluetape4k-projects/pull/1807)

## 맥락과 놓친 점

OpenFGA Read 응답 객체를 그대로 debug 로그에 넘기면 tuple 값과 continuation token이 함께 출력된다. 실패 경계에서 `logger.warn(Throwable)`을 사용하면 예외 메시지뿐 아니라 cause와 suppressed 예외도 로그의 throwable proxy에 포함된다. 권한 관계 값과 cursor가 운영 로그에 남을 수 있는 경로였다.

## 결정

Read 로그는 page 번호, tuple 개수, continuation token 존재 여부만 기록한다. 실패 로그에는 operation과 실패 상태만 남기고 `Throwable`을 전달하지 않는다. 호출자에게는 원래 예외 객체를 다시 던지고 `CancellationException`도 그대로 전파한다. 이 방향은 [provider 예외 체인 교훈](2026-09-08-provider-error-secrets.md)과 [변환 실패 로그 교훈](2026-09-08-safe-conversion-logs.md)을 OpenFGA 경계에 적용한 것이다.

## 결과

SDK 응답·요청 객체와 예외 원문을 로그에서 제외하면서 page 진행과 실패 operation은 확인할 수 있다. 비동기 로깅 회귀 테스트는 현재 호출의 고유한 tuple 개수 이벤트를 latch로 기다린 뒤 로그 메시지, `throwableProxy`, operation과 예외 전파를 확인한다. 이전 테스트의 같은 operation/status 로그가 현재 테스트의 신호로 오인되지 않도록 별도 metadata 이벤트를 먼저 drain한다.

## 검증

회귀 테스트에서 수정 전 tuple/cursor canary와 Throwable/cause/suppressed canary 검증이 실패했고, 수정 후 `OpenFgaCoroutinesTest` 21개가 통과했다. 해당 테스트 클래스는 최종 상태에서 3회 연속 통과했다. `:bluetape4k-openfga:test :bluetape4k-openfga:detekt --rerun-tasks`는 Testcontainers 통합 테스트를 포함해 22개 테스트 통과, 실패·오류·건너뜀 0건으로 완료됐다.

## 향후 지침

- SDK request/response 객체와 `Throwable`을 운영 로그에 직접 넘기지 않는다. 허용할 operation과 필요한 메타데이터만 명시적으로 구성한다.
- 비밀값 비노출 테스트는 message만 보지 말고 `formattedMessage`, `throwableProxy`, cause와 suppressed canary를 함께 확인한다. 호출자 예외 계약과 로그 비밀값 제거는 별도 검증한다.
- 비동기 로그 테스트는 임의의 짧은 대기 대신 latch를 사용하고, predicate에 테스트마다 다른 metadata를 넣는다. 같은 logger·operation의 이전 이벤트가 남을 수 있으면 고유 이벤트를 먼저 drain한 뒤 개수와 내용을 단언한다.
