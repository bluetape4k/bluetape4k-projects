# 변환 실패 로그는 원문 예외도 제외한다

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1699

## 실패한 가정과 근거

JSON 역직렬화 실패 로그에 payload-secret이 그대로 기록됐다. 예외 메시지에도 원문이 포함될 수 있다.

## 결정

운영 로그는 변환 방향만 기록한다. 호출자에게 전달하는 ConversionFailedException의 원래 cause 계약은 유지한다.

## 재발 방지

로그 캡처에서 formattedMessage와 throwableProxy를 모두 확인한다. 로그 비밀값 제거와 호출자 예외 타입/원인 변경을 별개 계약으로 검토한다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.
