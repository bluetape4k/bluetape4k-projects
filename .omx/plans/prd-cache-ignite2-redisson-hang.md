# PRD: cache-ignite2 / redisson 테스트 안정화

## 목표

- `infra/cache-ignite2` 테스트에서 발생하는 hang 또는 예외를 제거한다.
- `infra/redisson` 테스트에서 `untilSuspending` timeout 회귀를 제거한다.

## 사용자 스토리

1. 라이브러리 유지보수자로서, 비동기 캐시/near-cache 테스트가 CI와 로컬에서 안정적으로 통과하길 원한다.
2. 라이브러리 유지보수자로서, suspend polling helper 변경이 기존 테스트 의미를 깨지 않길 원한다.

## 수용 기준

- `:infra:cache-ignite2:test` 또는 해당 모듈의 동등 테스트 태스크가 통과한다.
- `:infra:redisson:test` 또는 해당 모듈의 동등 테스트 태스크가 통과한다.
- 변경된 테스트는 flaky timeout 의존성을 줄이고, hang 방지 로직이 실제 실패 모드에 대응한다.
- 영향을 받은 소스/테스트 파일에 컴파일 오류가 없다.

## 범위

- 테스트 코드 및 필요한 최소한의 테스트 지원 코드 수정
- 재현과 검증에 필요한 targeted test 실행

## 비범위

- 관련 없는 모듈 전체 리팩터링
- API 동작 자체의 대규모 변경
