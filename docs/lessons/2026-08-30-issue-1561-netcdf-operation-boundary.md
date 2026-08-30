# Issue #1561 NetCDF 작업 경계 보강 교훈

## 맥락

기존 NetCDF import는 blocking API였지만 timeout 뒤 caller가 progress를 조회할 `fileId`를
보존하는 흐름, worker 종료와 retry의 순서, trusted path와 tenant/job 책임, fingerprint 한계,
sealed exception migration 계약이 한 흐름으로 연결되지 않았다.

## 결정

- `registerFile()`을 import deadline 밖에서 완료하고 worker에는 `importGridValues()`만 제출한다.
- read-only progress API를 추가하되 `fileId`를 권한 토큰으로 취급하지 않는다.
- timeout 뒤 `cancel(true)`, `shutdownNow()`, bounded `awaitTermination()`, progress/failure 분류,
  운영 검토 순서를 고정하고 자동 retry는 0회로 둔다.
- active lease는 caller clock이 아니라 DB 결과와 `ImportAlreadyRunning`으로 판정한다.
- private typed checkpoint를 첫 spatial preflight의 `touchLease()` 직후에 한 번 호출한다.
  production 기본값은 no-op이며 test만 private constructor를 dynamic proxy로 호출한다.
- caller fixture는 authoritative `fileId → tenant/job/path` binding을 사용하고 raw progress를
  3-field allowlist DTO로 변환한다.
- fingerprint를 content integrity로 표현하지 않고 `fileKey|size|lastModifiedTime` 휴리스틱으로
  한정한다.

## 구현 중 놓쳤던 점

### Kotlin `internal`은 JVM private이 아니다

초기 설계는 `internal ImportCheckpoint`와 internal 4-arg constructor를 사용했다. Kotlin
caller에게는 감춰지지만 `javap`에서는 public constructor와 interface로 보였다. 테스트 편의를
위해 새 accidental ABI를 만드는 셈이었다.

public 3-arg constructor는 유지하고 typed 4-arg constructor를 private으로 바꿨다. test가
reflection을 쓰는 불편보다 production artifact의 공개 표면을 좁히는 편이 안전했다. private
interface method에는 `@Throws(InterruptedException::class)`를 붙여 dynamic proxy가 checked
interruption을 `UndeclaredThrowableException`으로 감싸지 않도록 했다.

### correlation ID는 bounded metric label이 아니다

초기 문서와 fixture는 `correlation_id`를 metric tag 예시로 넣었다. 요청이나 job마다 달라지는
값은 시계열 cardinality와 관측 비용을 폭증시킬 수 있다. metric은 `operation`과 `outcome`만
사용하고 correlation ID는 구조화 로그나 trace 필드로 옮겼다.

### timeout 예제는 모든 종료 경로를 합류시켜야 한다

처음 예제는 `ExecutionException`만 보완했고 `CancellationException`, `awaitTermination()`의
interruption, 실제 outcome 재분류를 빠뜨렸다. unchecked cancellation도 bounded termination과
progress 조회 경로로 합류시켰고, 예상하지 못한 worker failure는 fail-closed 처리했다.

### caller가 보낸 path로 caller authorization을 증명할 수 없다

초기 fixture는 요청의 tenant/path를 서로 비교했다. 이는 `fileId`가 가리키는 authoritative
record와 다른 값을 caller가 함께 위조하는 경우를 막지 못한다. non-register operation은
caller path를 무시하고 file binding에서 tenant/job/path를 읽도록 바꿨다.

### 파일 permission test에는 실행 사용자 전제가 있다

POSIX read bit를 제거해도 root는 파일을 읽을 수 있다. permission 변경 직후
`Files.isReadable()`을 확인해 재현 가능한 환경에서만 denial을 검증하고 permissions는
`finally`에서 복원했다. 현재 로컬 uid `501`과 hosted runner에서는 skipped 없이 실행된다.

### 최신 base와의 단순 diff는 역차이를 만들 수 있다

reviewer가 최신 `origin/develop`의 workflow hardening을 feature branch가 되돌렸다고 판단했지만,
이는 two-dot 비교에서 upstream-only 변경을 역차이로 본 결과였다. feature delta 판정은
`git merge-base origin/develop HEAD`를 기준으로 하고 status를 함께 확인해야 한다.

## 왜 JDK latch와 executor를 직접 사용했는가

검증해야 할 지점은 Exposed transaction 안에서 `touchLease()`가 성공한 직후, NetCDF tile read
전이다. 기존 test helper에는 이 정확한 경계에서 worker를 멈추는 수단이 없었다. 따라서
`CountDownLatch`, single-thread executor, private checkpoint를 사용했다. 모든 wait와 join은
5초로 제한하고 `Thread.sleep()`은 사용하지 않았다. 테스트는 interruption 뒤 worker 종료,
transaction rollback, 초기 lease 보존, grid row 0개, active retry 거부, DB lease 강제 만료 뒤
완료까지 검증한다.

## 결과와 검증

- 전체 science test `275`개가 failures/errors/skipped 없이 통과했다.
- consumer fixture `30`개가 public API, sealed fallback, authoritative authorization,
  fail-closed lifecycle, bounded metric/log field를 검증한다.
- public 3-arg JVM descriptor는 유지되고 typed 4-arg constructor는 private이다.
- EN/KO timeout marker는 byte-identical하며 Korean terminology findings는 0건이다.
- 독립 6관점 재검토와 main inline 검토의 최종 결과는 `P0=0`, `P1=0`, `P2=0`, `P3=0`이다.

## 앞으로의 guard

- timeout/retry 문서를 바꾸면 `CancellationException`, caller interruption 복구, bounded worker
  termination, progress/failure 재분류, zero-auto-retry가 모두 남아 있는지 확인한다.
- operation handle이나 progress DTO를 외부 endpoint에 연결할 때 authorization과 redaction을
  실제 adapter integration test로 승격한다.
- fingerprint를 강화하려면 기존 heuristic을 content hash라고 재명명하지 말고 비용·TOCTOU·
  immutable storage 계약을 별도 설계한다.
- test seam 변경은 Kotlin visibility와 JVM descriptor를 함께 검증한다.
