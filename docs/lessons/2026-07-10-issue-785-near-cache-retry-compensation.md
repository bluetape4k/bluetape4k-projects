# 이슈 #785: Near-cache retry failure compensation

## 배경

Bounded queue overflow와 close draining은 이미 처리되어 있었지만, public write-behind
operation이 front cache를 갱신한 뒤 backend write가 모든 retry를 소진할 수 있었다.
그 결과 uncommitted front value, 영구 tombstone, 영구적으로 켜진 `clearPending` flag가
남았다.

## 결정

- 기존 write-behind API를 유지하고 terminal backend failure를 compensation한다.
- 실패한 `Put` front entry를 invalidate해서 이후 read가 back cache에서 다시 채우게 한다.
- 실패한 `Remove` tombstone과 실패한 `ClearBack` read blocking을 해제한다.
- Consumer가 local state 설치 전에 command를 완료할 수 없도록 queue acceptance와 optimistic front-state mutation을 직렬화한다.
- Accepted command마다 ownership token을 연결한다. Stale completion은 같은 key 또는 clear에 대해 새 command가 설치한 state를 compensate하면 안 된다.
- Enqueue state installation에 쓰는 같은 lock/mutex 아래에서 token completion과 compensation을 적용하고, caller-owned bulk collection을 snapshot한다.
- Backend read가 새 mutation보다 먼저 시작되었더라도 더 새 front state를 덮어쓰지 못하도록 monotonic state version으로 read-through population을 guard한다.
- Synchronous replace를 pending write state와 직렬화하고 같은 version을 전진시켜 delayed read가 replaced value를 복원하지 못하게 한다.
- `putIfAbsent`가 in-flight Put value를 반환하거나 in-flight remove/clear 뒤 reordering 없이 enqueue할 수 있도록 mutation token에 logical value를 보존한다.
- `putIfAbsent` 평가 시 더 새 clear가 더 오래된 pending Put을 supersede하도록 mutation sequence와 clear command sequence를 비교한다.

## 결과

Blocking/suspend 구현은 retry exhaustion 뒤 영구적으로 divergent local view를 유지하지
않고 observable backend state로 수렴한다. Asynchronous write-behind call은 반환된 뒤
retroactive하게 실패할 수 없으므로 terminal failure는 log로 남긴다.

## 검증

- RED: six retry-exhaustion tests failed with `ConditionTimeoutException` before the fix.
- GREEN: the same six tests passed after compensation was implemented.
- RED: mutable `PutAll` inputs terminated both consumers before snapshotting was added.
- `MultithreadingTester` and `SuspendedJobTester` verified mixed same-key
  put/remove/clear completion against the final backend state.
- Deterministic blocked-read tests verified stale read-through results are not
  repopulated after a concurrent put or replace.
- Queued-remove tests verified replace cannot bypass a pending tombstone.
- Queued remove/clear tests verified `putIfAbsent` preserves mutation order.
- Pending Put then Clear tests verified the newer clear wins before `putIfAbsent`.
- Both resilient near-cache test classes: 68 tests passed.
- `:bluetape4k-cache-core:test`: 502 tests passed.

## 향후 방지책

Optimistic state가 queued asynchronous work와 연결되어 있다면 completion과
compensation은 command ownership에 조건부여야 한다. 더 새로 accepted된 state를
덮어쓸 수 있는 unversioned `remove`나 boolean reset을 사용하지 않는다.
