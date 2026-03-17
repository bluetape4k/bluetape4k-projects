# Test Spec: cache-ignite2 / redisson 안정화

## 검증 대상

- `infra/cache-ignite2`
- `infra/redisson`

## 검증 방법

1. 대상 테스트를 먼저 재현 실행한다.
2. 실패 원인을 코드/테스트 레벨에서 수정한다.
3. 영향 범위를 고려해 모듈 단위 테스트를 재실행한다.
4. 수정 파일에 대한 진단 또는 컴파일 오류를 확인한다.

## 기대 결과

- Ignite2 관련 테스트에서 hang 없이 완료되거나 의도된 예외만 검증된다.
- Redisson near cache 테스트의 `untilSuspending` 대기 조건이 불필요한 timeout 회귀 없이 통과한다.
- 회귀를 막는 테스트가 유지된다.
