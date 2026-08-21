# 이슈 #1459 Redis disconnect 테스트 fixture 정리 경계 (2026-08-21)

관련 이슈: #1459 · PR #1460 · Epic #1418 Slot 3
영향 module: `:bluetape4k-lettuce`

## 맥락

Lettuce multi lock과 read/write lock의 disconnect 회귀 테스트는 실제 핸들을
획득한 뒤 커넥션을 닫고 backend failure 결과를 확인한다. 테스트가 사용하는
공유 Redis fixture에서 lock state 키는 lease에 따라 만료되지만 generation 키는
Lua 스크립트가 증가시킨 뒤 별도 만료 없이 남을 수 있었다. 테스트 wrapper를
닫는 것만으로는 이 키를 정리하지 못한다.

## 결정 또는 발견

1. 테스트가 계산한 lock 이름과 config를 커넥션 종료 전에 보존한다. acquisition과
   disconnect 검증을 같은 `try/finally` 안에 두어 중간 setup 실패에도 정리 경계를
   유지한다.
2. `finally`에서 원래 커넥션을 닫은 뒤 독립 cleanup connection을 열고,
   `deriveMultiLockKeys` 또는 `deriveReadWriteLockKeys`가 반환한 전체 key set을
   `DEL`한다. 이어서 `EXISTS`가 `0`인지 확인해 fixture 격리를 증명한다.
3. 이 테스트는 production Lua의 generation TTL 정책을 바꾸지 않는다. 공유
   fixture의 소유권과 정리 책임을 테스트가 명시적으로 맡는다.
4. public failure 결과는 wrapper 타입뿐 아니라 nested `failure.kind`와
   `recoveryAction`까지 검증한다. `Ambiguous` 결과는 owner/request identity와
   `RECONCILE_REQUEST`를 함께 고정한다.

## 결과

disconnect 테스트는 setup 또는 검증 중 예외가 나도 독립 커넥션으로 모든 파생
키를 정리하고, backend/integrity 분류의 세 실행 모델 계약을 구체적인 payload로
확인한다. fault-injection은 fatal/cancellation 및 일반 예외에서도 모든 teardown을
실행하고 primary·suppressed 예외 우선순위를 보존하는지 검증한다. terminal protocol
tag characterization은 sync decoder 범위로 문서화해 실제 Lua/transport 보장과
혼동하지 않는다.

## 검증

`./gradlew :bluetape4k-lettuce:test --tests
'io.bluetape4k.redis.lettuce.lock.LettuceClosedLockCoverageTest'
--tests
'io.bluetape4k.redis.lettuce.lock.internal.FencedLockClientFailureCoverageTest'
--no-parallel --max-workers=1` 결과는 9개 테스트 통과다.
`:bluetape4k-lettuce:test --no-parallel --max-workers=1` 전체 모듈 검증은 912개
테스트가 통과했다. `git diff --check`와 Kotlin 컴파일도 같은 검증 흐름에서
통과했다.

## 향후 지침

- 공유 Testcontainers 또는 singleton Redis fixture에서 disconnect를 재현할 때는
  wrapper close를 resource cleanup으로 간주하지 말고 독립 cleanup connection과
  파생 key set을 함께 둔다.
- generation, sequence, counter처럼 Lua가 별도 TTL 없이 갱신하는 보조 키를
  추가하면 해당 테스트의 `DEL`/`EXISTS` 목록과 cleanup 회귀를 같은 stacked slot에
  갱신한다.
- failure coverage를 추가할 때는 public result 종류와 nested kind/action 또는
  identity를 동시에 검증해 분류 계약의 회귀를 놓치지 않는다.
