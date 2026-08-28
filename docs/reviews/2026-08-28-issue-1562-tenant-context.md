# Issue #1562 TenantContext 구현 6관점 리뷰

## 판정

| 항목 | 결과 |
| --- | --- |
| P0 | 0 |
| source/local P1 | 0 |
| 외부 운영 P1 | 1 — #1565에서 차단 중 |
| fixed P2 | 10 |
| rationale-deferred P2 | 2 |
| publication 상태 | `PENDING` |

TenantContext source와 local workflow 계약에는 unresolved P0/P1이 없다. 다만 live
`maven-central-release` environment가 `protection_rules=[]`,
`deployment_branch_policy=null`, `can_admins_bypass=true`이고 publication/signing secret도
environment scope로 격리되지 않았다. 이 외부 P1은 #1565에 기록했으며 read-back으로 닫기 전에는
SNAPSHOT dispatch를 하지 않는다.

## 관점별 결과

| 관점 | 1차 finding | disposition | 근거 |
| --- | --- | --- | --- |
| Developer/API | P0=0, P1=0, P2=0 | 변경 없음 | `TenantContext`는 no-default lexical API만 노출하고 carrier raw state를 공개하지 않는다. |
| Architecture/docs | P2=1 | fixed | application DI singleton과 라이브러리 `static` 프로세스 전역 singleton을 구분했다(`bluetape4k/tenant/README.ko.md:63-76,115-116`). |
| Stability/retention | P1=3, P2=3 | P1/P2 5건 fixed, P2 1건 deferred | 동시 start gate, 예외 cleanup, 고유 virtual tenant 10,000개, exact sentinel 비교, 256 MiB heap의 retained GC pressure를 적용했다(`TenantContextRetentionStressTest.kt:22-145,198-204`). |
| SNAPSHOT integrity | P1=2, P2=3 | code finding fixed, environment P1 external | dispatch ref/SHA와 전체 Nightly job을 검증하고 transient exit `75`만 재시도하며 receipt identity를 모두 대조한다(`publish-snapshot.yml:54-151,251-295`). |
| Dependency/publication | P1=1, P2=1 | P2 fixed, P1 external | Nightly matrix group과 required job set의 drift test를 추가했다(`scripts/test_release_workflow_policy.py`). 세 artifact의 dependency/POM/module metadata/BOM constraint는 검증됐다. |
| Operations/security | P1=1, P2=5 | P2 4건 fixed, 1건 deferred; P1 external | issue write를 별도 `record-handoff` job으로 격리하고 #1562 identity, Maven snapshot identity, TLS 영구 실패를 fail-fast로 고정했다(`publish-snapshot.yml:336-393`, `create_snapshot_handoff.py:44-132`). |

## Finding disposition

### Fixed

- full Nightly의 matrix prefix 하나만 확인하던 검증을 모든 exact job name 검증으로 교체했다.
- Nightly matrix group과 SNAPSHOT required set의 drift를 policy test가 검출한다.
- dispatch ref가 `refs/heads/develop`이고 dispatch SHA, verified Nightly SHA, 현재 develop SHA가
  모두 같아야 publish job을 시작한다.
- `404/408/429/500/502/503/504`, timeout과 제한된 연결 오류만 transient exit `75`로 분류한다.
  TLS 인증 오류와 semantic receipt 오류는 즉시 영구 실패한다.
- Maven timestamp, build number, `lastUpdated`, timestamped snapshot version을 strict format과
  동일 identity로 검증한다.
- receipt와 issue 기록 job이 `handoff_issue_number=1562`를 다시 검증한다.
- `issues: write`는 publication secret을 사용하는 job과 분리된 `record-handoff` job에만 둔다.
- retention stress는 platform/virtual task의 동시 시작, 예외 경로 cleanup, 고유 tenant identity,
  exact sentinel equality와 bounded retained memory pressure를 검증한다.
- README는 application-scoped singleton과 process-global singleton의 경계를 구분한다.

### Rationale-deferred

- ThreadLocal/platform과 ScopedValue/virtual의 교차 조합 stress는 지원 carrier 선택 계약이 아니므로
  추가하지 않았다. 각 지원 조합의 isolation과 예외 cleanup은 검증하며 README가 비지원 전파 경계를
  명시한다.
- privileged workflow action의 immutable commit SHA pin은 저장소 전체가 version tag 관례이고 live
  `sha_pinning_required=false`이므로 이 PR에서 일부 action만 바꾸지 않는다. #1565에서 environment
  보호 및 secret scope와 함께 저장소 publication 정책으로 결정한다. 이 항목이 닫히기 전 publication은
  외부 P1에 의해 이미 차단된다.

## 검증 증거

- targeted JUnit: core 17, Reactor 6, Ktor 6; 합계 29, failures/errors/skipped=0
- retention JUnit: 2, failures/errors/skipped=0
- 세 module `check`, Kover XML, root Detekt, `checkDisabledTests`: 성공
- workflow/receipt policy: 30 tests, 모두 성공; `actionlint` 성공
- CI domain policy: 15 tests, 모두 성공
- module registration, manual inventory, buildSrc tests: 성공
- publication POM/module metadata: 79 files, failures=0; BOM constraint 세 artifact 각각 1회
- production logging/MDC/metric/Reactor global hook scan: 0 findings
- `checkPomFileForBluetape4kPublication`은 신규 tenant와 기존 core 모두 developer
  `organization`/`organizationUrl` 누락으로 동일 실패하며 #1565의 공통 blocker다.

## 최종 상태

구현과 local 검증은 `DONE`, SNAPSHOT publication은 외부 P1과 #1565 때문에 `PENDING`이다.
PR exact-head CI와 merge는 이후 별도 gate이며 merge에는 fresh 사용자 승인이 필요하다.
