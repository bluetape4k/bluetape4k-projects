# NearJCache 동기 write-through listener 재진입 교착

## 맥락

`NearJCache`의 동기 mutation은 호출자 스레드가 `mutationGate`를 잡은 상태에서
back provider write를 전용 virtual thread에 위임하고 완료를 기다립니다. Lettuce
JCache provider는 `isSynchronous=true` listener를 같은 write worker에서 inline으로
호출할 수 있고, 다른 provider는 synchronous callback thread를 사용할 수 있습니다.
listener가 front reconciliation을 위해 같은 `mutationGate`를 다시 획득하면 호출자는
worker를 기다리고 worker는 호출자를 기다리는 교착이 발생했습니다.

## 결정 또는 발견

- 동기 write-through의 self-event를 operation-scoped key/type/value 상관관계로 식별하고
  callback thread에서 front에 직접 반영하도록 context reconciliation 경계를 추가했습니다.
- inline callback은 thread-local fast path를 사용하고, 별도 synchronous callback thread는
  active operation context를 사용합니다. 한 operation에서 이미 소비한 key의 후속 event는
  다시 self-event로 처리하지 않습니다.
- 다른 wrapper 또는 외부 write의 listener event와 비동기 경로는 기존처럼
  `mutationGate`를 통해 직렬화합니다.
- provider 호출은 기존 `backWriteLock`과 bounded timeout을 그대로 사용해 timeout,
  cancellation, late completion의 순서 계약을 변경하지 않았습니다.
- 호출자 gate를 provider I/O 전에 무조건 해제하는 대안은 late completion과 후속
  write의 순서를 약화할 수 있어 채택하지 않았습니다.

## 결과

Lettuce의 동기 `put`, `putAll`, `putIfAbsent`, `remove`, `replace`가 inline listener
재진입으로 timeout되지 않고 완료되며, peer invalidation과 비동기 ordering 경계는
유지됩니다.

## 검증

- RED: 실제 Lettuce/Testcontainers 경로에서 기존 구현이 `TimeoutException`으로
  실패했습니다.
- GREEN: cache-core mock provider가 inline 및 별도 callback thread에서 CREATED/UPDATED/REMOVED
  event를 호출하는 다섯 CRUD 회귀 테스트가 통과했습니다.
- cache-core NearJCache 패키지 regression suite가 통과했습니다.
- 실제 Lettuce integration test도 다섯 CRUD 연산 모두 통과했습니다. 최초 실행은
  Colima의 Docker socket mount 오류로 중단되었고, `TESTCONTAINERS_RYUK_DISABLED=true`
  조건부 재실행에서 Redis 8.8.1 경로를 확인했습니다.
- timeout 뒤 provider가 interrupt를 무시하는 mock과 후속 write의 backWriteLock 순서를
  회귀 테스트로 고정했습니다.

## 향후 지침

새 synchronous listener-backed write 경로를 추가할 때는 호출자 lock을 잡은 채
provider callback을 기다리는 역방향 대기를 먼저 점검합니다. self-event와 외부 event의
경계를 분리하고, timeout 이후 late completion 및 후속 write 순서를 함께 회귀 테스트로
고정합니다.
