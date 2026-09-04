# Issue #1618: 배치 재시도 상태는 entry별로 전이한다

## 맥락

`LettuceLoadedMap`과 `LettuceSuspendedLoadedMap`의 write-behind queue는 각 entry에
retry count를 보관한다. 그러나 writer batch가 실패하면 첫 entry의 retry count만 증가시켜
나머지 entry에도 적용했다. 이미 두 번 실패한 entry와 처음 실패한 entry가 한 batch에
섞이면 첫 entry 순서에 따라 재시도 상한이 늦어지거나 fresh entry가 조기 dead-letter됐다.

## 결정

- writer batch 실패 후 각 entry의 기존 retry count에서 `nextRetryCount`를 독립 계산한다.
- `nextRetryCount < MAX_DEAD_LETTER_RETRY`인 entry만 queue 또는 channel에 다시 넣는다.
- 재시도 상한에 도달했거나 queue/channel 포화로 다시 넣을 수 없는 entry만 dead-letter에
  기록한다.
- dead-letter의 복구 값 `HSET` 후 모니터링 키 `LPUSH` 순서와 suspend cancellation 전파는
  유지한다.
- `(retry=2, retry=0)` 혼합 batch를 직접 구성해 동기·suspend 구현이 같은 상태 전이를
  수행하는지 회귀 테스트로 고정한다.

## 결과

retry 2 entry는 다음 실패인 attempt 3에서 dead-letter되고, 같은 batch의 fresh entry는
retry 1 상태로 남는다. batch 내 순서가 다른 entry의 수명에 영향을 주지 않으며 기존
retry exhaustion과 write-behind map 동작도 유지된다. public API와 JVM descriptor 변경은
없다.

## 검증

- 수정 전 `LettuceWriteBehindRetryTest`: 2 tests 중 2 failures.
- 수정 후 `LettuceWriteBehindRetryTest`: 4 passing. 혼합 retry-count 2건과 queue/channel
  포화 시 dead-letter key·recovery value 보존 2건을 포함한다.
- Lettuce map 집중 회귀: 42 passing.
- `:bluetape4k-lettuce:check`: 기본 924 passing과 별도 topology/performance 4 passing,
  총 928 passing.
- compile, Detekt, Kover 및 `git diff --check`: 통과.

## 향후 지침

batch element가 retry, offset, sequence, deadline처럼 독립 상태를 보유하면 batch 대표값을
전체 상태 전이에 재사용하지 않는다. 실패한 I/O는 한 번이어도 후속 retry/dead-letter
결정은 element별 이전 상태에서 계산한다. 회귀 테스트는 서로 다른 상태의 element를 한
batch에 넣고 각 결과를 동시에 검증해야 한다.
