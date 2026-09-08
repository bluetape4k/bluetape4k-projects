# timeout 소유권과 취소 전파

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1692

## 실패한 가정과 근거

상위 timeout과 핸들러 내부 timeout이 정책 TimeoutException으로 변환됐다. 두 회귀 테스트가 잘못된 예외 타입으로 실패했다.

## 결정

withTimeoutOrNull이 소유한 timeout만 정책 실패로 변환한다. 다른 CancellationException은 그대로 전파한다.

## 재발 방지

timeout 테스트는 정책 자체, 상위 요청, 내부 작업의 세 가지 소유자를 각각 검증하고 정책 이벤트 수도 확인한다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.
