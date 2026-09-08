# 스트림 종료는 성공과 실패 경로에서 검증한다

관련 이슈: https://github.com/bluetape4k/bluetape4k-projects/issues/1693

## 실패한 가정과 근거

MVC와 WebFlux 로더에서 성공과 읽기 실패 모두 close 횟수가 0이었다.

## 결정

두 로더의 Reader를 use로 감싸고 기존 allowlist와 캐시 이름을 유지한다.

## 재발 방지

내용 반환 테스트만으로 자원 소유권을 증명하지 않는다. 추적 가능한 입력 스트림으로 정상/예외 경로 close를 모두 검증한다.

## 검증

이 PR의 회귀 테스트에서 수정 전 동작 실패를 확인했다. 수정 후 테스트와 관련 모듈 검증 결과는 PR의 `DoD Status`에 기록한다.
