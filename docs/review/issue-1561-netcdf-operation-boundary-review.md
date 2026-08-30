# Issue #1561 NetCDF 작업 경계 검토

## 검토 기준

- 이슈: `#1561 [science][api] NetCDF timeout progress·trusted path·sealed exception 2.0 경계 명시`
- 기준 commit: `9831a513f9b81e53f505fadd2e4546b8ea8cf6a8`
- 검토 시점의 `origin/develop`: `d59f7002469bc6596e0f487768468594e7bc08dc`
- 구현 delta SHA-256: `2c5aae4d0d5f94049d1060da793b01187aa9b8d402b8d56363fc67f3409afb07`
- 구현 범위: `utils/science` production 1개, service/guard test 2개, 외부 caller fixture 1개,
  EN/KO README 2개.
- 중단 조건: inline 검토와 독립 6관점 검토에서 `P0=0`, `P1=0`이고 전체 science
  test, compile, Detekt, JVM descriptor, 문서 parity 검증이 통과할 것.

구현 delta hash는 `utils/science`의 binary diff와 untracked consumer fixture의 경로·내용을
결합해 계산했다. 최신 `origin/develop`과의 two-dot 역차이는 feature 변경으로 간주하지 않았다.

## 최종 판정

최종 통합 판정은 **APPROVE**이다. inline 검토와 독립 6관점 재검토의 최종 합계는
`P0=0`, `P1=0`, `P2=0`, `P3=0`이다.

| 관점 | 최종 판정 | 근거 |
|---|---|---|
| Performance/resource | CLEAR | progress lookup은 indexed read transaction과 bounded `status` tag만 사용한다. `correlation_id`는 metric tag에서 제거하고 구조화 로그·trace 필드로 이동했다. |
| Stability/cancellation | APPROVE | post-lease interruption이 transaction을 rollback하고 worker 종료 뒤에만 retry한다. POSIX permission test는 non-root 전제를 명시하며 모든 latch/join은 bounded이다. |
| Security/trust boundary | CLEAR | `fileId`를 권한 토큰으로 취급하지 않고 tenant/job/path를 매 operation 재검증한다. checkpoint type과 4-arg constructor는 JVM public surface가 아니다. |
| Operator/Ops | CLEAR | timeout, cancellation, stuck worker, progress, alert, zero-auto-retry, recovery 순서가 EN/KO에서 일치한다. workflow 변경 경보는 merge-base 대조로 역차이 오탐임을 확인했다. |
| Developer/API | APPROVE | 기존 public 3-arg constructor와 method descriptor를 유지하고 `findImportProgress()`만 additive하게 추가했다. private typed checkpoint가 deterministic test seam을 제공한다. |
| User/caller | APPROVE | timeout 전에 `fileId`를 보존하고 worker termination과 progress를 함께 분류한다. fingerprint·sealed subtype·caller DTO·authoritative binding 한계가 명시됐다. |

## 해결된 주요 발견

1. **P1 — 고카디널리티 metric tag**
   `correlation_id`를 caller metric tag 예시와 fixture에서 제거했다. metric은 bounded
   `operation`/`outcome`만 사용하고 correlation ID는 구조화 로그나 trace에 둔다.
2. **P1 — fingerprint 보장 범위 누락**
   fingerprint가 `fileKey|size|lastModifiedTime` 휴리스틱이며 content hash나 TOCTOU
   증명이 아니라는 점을 public KDoc와 EN/KO README에 추가했다. 동일 metadata를 보존한
   hostile change는 탐지하지 못할 수 있으므로 immutable quarantine 책임을 유지한다.
3. **P2 — JVM public test seam**
   Kotlin `internal` 4-arg constructor가 JVM에서 public으로 노출되는 문제를 확인했다.
   checkpoint를 private typed interface와 private primary constructor로 축소하고, test만
   dynamic proxy로 private constructor를 호출한다. public non-synthetic constructor는 3-arg
   하나다.
4. **P2 — cancellation과 unknown failure 분류**
   README 예제가 `CancellationException`과 `awaitTermination()` interruption을 처리하고,
   종료 후 progress와 worker failure를 실제 outcome으로 매핑하도록 수정했다. 예상하지 못한
   worker failure는 `RECOVERY_REQUIRED`로 fail-closed한다.
5. **P2 — caller binding fixture의 자기신고 값 의존**
   fixture가 authoritative `fileId → tenant/job/path` map을 조회하도록 변경했다. non-register
   operation은 caller가 보낸 path를 신뢰하지 않으며 cross-tenant, cross-job, unauthorized,
   outside-root를 register/import/progress/retry 각각에서 service invocation 전에 거부한다.
6. **P2 — POSIX permission test의 root 환경**
   permission 변경 뒤 `Files.isReadable()`을 확인하고 root처럼 denial을 재현할 수 없는 환경은
   명시적으로 abort한다. 현재 검증 환경은 uid `501`이며 최종 전체 suite의 skipped는 0이다.
7. **오탐 — release workflow rollback**
   최신 `origin/develop`과 working tree의 two-dot 비교에서 upstream 변경이 역차이로 보였다.
   merge-base `9831a513` 기준 `.github/workflows`와 release policy script delta 및 status는
   모두 비어 있어 Issue #1561 범위 변경이 아님을 확인했다.

## 공개 계약과 운영 경계

- `registerFile()`은 import deadline 밖에서 완료해 caller가 `fileId`를 보존한다.
- `findImportProgress(fileId, variableName)`는 진행 row를 조회할 뿐 lease나 cursor를 변경하지
  않는다. `fileId` 자체는 authorization 근거가 아니다.
- timeout은 cooperative cancellation 요청이다. worker termination을 bounded하게 확인하지
  못하면 `RECOVERY_REQUIRED`이며 자동 retry는 0회다.
- application host clock으로 `leaseExpiresAt`을 판정하지 않는다. 활성 lease 여부와 재획득은
  DB 결과와 `ImportAlreadyRunning`을 기준으로 한다.
- raw progress model을 HTTP/RPC 응답으로 직접 직렬화하지 않는다. caller DTO는 status,
  last committed slice, coarse outcome만 allowlist한다.
- sealed `NetCdfException` 소비자는 integration 경계의 `when`에 `else` fallback을 둔다.

## 검증 근거

- `./gradlew :bluetape4k-science:test`: `275` passed, failures `0`, errors `0`, skipped `0`.
- consumer fixture: `30` passed, failures `0`, errors `0`, skipped `0`.
- post-lease interruption, private constructor ABI, checkpoint cardinality, POSIX unreadable targeted
  test: PASS.
- `./gradlew :bluetape4k-science:detekt :bluetape4k-science:compileKotlin
  :bluetape4k-science:compileTestKotlin`: BUILD SUCCESSFUL.
- Detekt는 기존 `NetCdfCatalogService` complexity/size와 test `LargeClass` 경고를 계속 보고하지만,
  이번에 추가한 private checkpoint와 consumer fixture의 신규 경고는 없다.
- `javap -public -s`: 기존 3-arg constructor descriptor와 세 public method를 확인했다.
- `javap -private -s`: typed 4-arg constructor가 private임을 확인했다.
- EN/KO `netcdf-timeout-example` marker byte parity: PASS.
- Korean terminology audit: findings `0`.
- `git diff --check`: PASS.
- `.github/workflows`, dependency, catalog, schema, module registration delta: 없음.

## 잔여 공백과 후속 gate

- 실제 HTTP/RPC adapter의 tenant/job authorization과 DTO redaction은 caller 구현 책임이며 이
  repository에는 해당 adapter가 없다. consumer fixture는 source/decision-table 계약이지 실제
  endpoint integration proof가 아니다.
- fingerprint는 hostile writer를 막지 않는다. 운영 환경은 immutable quarantine과 별도 access
  control을 제공해야 한다.
- remote exact-head CI와 GitHub review thread 상태는 PR 생성 뒤 확인해야 한다.
- 병합, release, publication, branch/worktree 정리는 별도 승인 gate다.

최종 결론은 `P0=0`, `P1=0`이다. 현재 구현 범위에서 해결되지 않은 차단 finding은 없다.
