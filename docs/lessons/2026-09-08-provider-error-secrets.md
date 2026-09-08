# provider 예외의 전체 체인을 비밀값 경계로 취급한다

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1695

## 실패한 가정과 근거

상위 메시지만 정리해도 원래 cause와 provider SQLException에서 비밀값이 노출됐다.

## 결정

provider 실행 경계에서 안전한 SQLException을 새로 만든다. null 결과 메시지와 JDBC driver 예외 경계는 유지한다.

## 재발 방지

비밀값 비노출 테스트는 message뿐 아니라 stackTraceToString으로 cause와 suppressed까지 확인한다. 디버깅 편의로 원문 cause를 다시 연결하지 않는다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.
