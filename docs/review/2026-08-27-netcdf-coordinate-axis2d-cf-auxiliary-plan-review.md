# #1352 NetCDF CoordinateAxis2D·CF auxiliary 구현 계획 통합 검토

## 검토 범위와 기준

- **검토 일자**: 2026-08-27
- **이슈/에픽**: [#1352](https://github.com/bluetape4k/bluetape4k-projects/issues/1352) /
  [#1421](https://github.com/bluetape4k/bluetape4k-projects/issues/1421)
- **검토 대상**: 승인 명세 `docs/superpowers/specs/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-design.md`,
  구현 계획 `docs/superpowers/plans/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-plan.md`
- **기준 branch/SHA**: `feat/1352-coordinate-axis2d-cf-grid` /
  `origin/develop@45260871f58433a78f2d633c235010f661d22c6e`
- **선행 train**: PR #1512가 Issue #1343 문서 계약을 `develop`에 rebase merge
- **검토 방식**: main-session architecture 통합과 Performance, Security,
  Stability/Ops, Developer/API, User/caller 독립 관점의 최신 계획 재검토
- **변경 경계**: Step 3-R 산출물 검토 기록이다. 구현 코드·스키마·workflow·PR·merge는
  이 단계에서 변경하지 않는다.

## 통합 판정

| 우선순위 | 관점 | 최신 판정 | 근거/처분 |
|---|---|---|---|
| P0 | 전체 | 0건 | 명세 요구가 파일·task·테스트·중지 조건과 연결되고, schema/dependency/workflow 경계가 고정됨 |
| P1 | 전체 | 0건 | API의 nullable dimension/null location·custom TTL·exact tuple과 호출자의 executor/migration/CorruptProgress recovery 보완 후 독립 재검토 PASS |
| P2 | 전체 | 0건 | 동일 보완 사항의 retry/backoff·timeout side-effect recheck·compatibility fixture 보완 후 독립 재검토 PASS |
| P3 | 전체 | 0건 | 별도 잔여 경미 사항 없음 |

**최종 verdict: PASS (P0=0, P1=0, P2=0, P3=0).** API, Stability/Ops, User/caller 독립
재검토와 보완 후 계획·명세 정합성 확인을 통과했다. Step 3-R/3-P를 통과했으며 다음
게이트는 승인 산출물 commit 후 TDD 구현이다. hosted CI·실행 테스트·benchmark는 구현
이후 증거로 남긴다.

## 관점별 확인

### Architecture / main session

- 공개 blocking `registerFile`/`importGridValues`, 기존 `location`/`attrs`, 기존 table/index와
  Exposed 연결 경계를 유지한다. 새 table/column/schema migration, dependency, settings/BOM,
  module registration, workflow 변경은 N/A 이유와 함께 범위 밖으로 고정했다.
- Epic #1421 train은 선행 PR #1512(T0, #1343 문서)를 `develop`에 반영한 뒤 현재 #1352
  child(T1)를 `develop` base로 올리는 순서로 고정하고, T1은 exact-head fresh approval 후
  rebase merge한다. Epic close는 두 child의 live DoD를 재확인한 뒤로 유보한다.
- `VariableAxisMap` → bounded reader/sampler → deterministic tile planner → serializer/writer →
  service/progress repository 순서가 파일 소유권과 task 순서로 분리되어 역순 의존이 없다.
- 선행 PR #1512의 문서 계약과 기준 SHA를 plan/benchmark/rollback에 고정하고, child #1352만
  구현 train으로 유지한다.

### Performance

- `CoordinateReader.read1D/read2D`, rank-1 window와 rank 2–4 tile read가 full-array를 만들지
  않으며 tile cell은 65,536 이하, JDBC batch/pending rows는 1,000 이하이다.
- `MAX_FIXED_ROW_BYTES=256L`, checked `batchPayloadBytes`,
  `serializerScratchBytes=max(rawScratchBytes,batchPayloadBytes)`, coordinate/duplicate 64 MiB,
  owned working-set 128 MiB를 첫 tile 전에 계산한다.
- 두 pass duplicate preflight, row-major `read2D`, same generator 1D baseline/2D report-only
  benchmark, exact baseline SHA와 `MemoryMXBean`/counter 기록으로 성능 주장을 제한한다.

### Security

- trusted-admin regular-file·NUL/control·URI·symlink·size guard와 open 전후 identity 검증을
  명시하고, 불일치 시 dataset close·DB progress 불변·`FileChanged`를 보장한다.
- verified dataset 뒤 bounded non-recursive stack/queue metadata scan을 수행하고
  `MAX_GROUP_COUNT=256`, `MAX_GROUP_DEPTH=32`, 1 MiB metadata cap에서 map materialization 전
  조기 중지한다.
- strict EPSG grammar/whitelist, source/final coordinate bounds, JSONB NFC/UTF-8/control/
  reserved-prefix/8,192-byte cap, typed placeholder와 bounded batch result 처리를 고정했다.

### Stability/Ops

- 최초 missing-row upsert, unique `(file_id, variable_name)` race, malformed
  `IN_PROGRESS`+null lease repair, 미래 expiry `ImportAlreadyRunning`, stale fence
  `ImportLeaseLost`를 구분한다. 비교 시계는 DB UTC wall clock이며 lease
  fence에는 `clock_timestamp()`를 사용하고, audit timestamp에는
  `CURRENT_TIMESTAMP`를 유지한다.
- 각 tile transaction은 시작·read/write·commit fence를 검사하고 현재 tile만 rollback한다.
  이전에 commit된 tile/slice는 보존하며, duplicate는 두 번째 pass 전에 slice 전체가 0 rows다.
- 마지막 row write/fence 뒤 DB commit 전에 같은 transaction에서 `markCompleted`를 호출해
  terminal status/completedAt/lease clear/checkpoint invariant를 원자적으로 보장한다.
- cancellation/interrupt는 `FAILED` 전환 없이 원래 예외와 interrupt를 보존하고,
  `Future.cancel(true)`·`shutdownNow()`·bounded `awaitTermination` 뒤 connection=0/추가 renew
  없음과 현재 tile rollback을 확인한다. 운영 runbook은 5분 TTL·stale progress·rollback·
  allowlisted rejection metric과 alert를 포함한다.

### Developer/API

- sealed `NetCdfException` 다섯 새 subtype과 2.0.0 migration/`else` catch fixture,
  구조화된 오류 필드를 계획했다. nullable `timeDim`/`levelDim`은 조건부 full-rank
  assignment로 처리하고, rank 1 `TileRow`의 nullable location과 typed SQL NULL
  binding invariant를 고정한다. 기존 custom `leaseTtl`은 DB interval parameter로
  보존하며, 30a/32는 raw `ST_X/ST_Y/value` exact tuple을 검증한다. `CoordinateSampler`는
  caller-owned `MutableCoordinateSample`와 즉시 `readOnlyCopy()` lifetime을 사용하고
  target/map을 보유하지 않는다.
- `TileBatchWriter.write(connection, rows)`는 active Exposed transaction의 동일 JDBC
  connection만 받아 새 transaction/DataSource/close를 금지하고, prepared statement만 닫는다.
- full-rank `Index.set`, dimension-order fixture, CRS/auxiliary/duplicate/error tests와
  기존 rank 1–4 회귀를 Task 1–7에 매핑했다.

### User/caller

- blocking API를 virtual-thread executor에서 실행하고, timeout/interrupt/cancellation/
  execution failure을 처리한 뒤 `Future.cancel(true)`, `shutdownNow()`/30초
  `awaitTermination`을 수행하는 EN·KO 예시를 계획했다. cooperative cancel 경고와
  timeout 후 progress/lease/partial-row side-effect 재확인도 포함했다.
- `location`은 `(lon,lat)`이고 numeric CF auxiliary는 `attrs`로 간다는 read-back, supported
  CRS/hard limits, trusted-admin/authN/authZ/tenant/root 책임을 문서화한다.
- 다섯 새 sealed subtype 모두의 exhaustive `when` 대응과, 입력·metadata·경로·축·변수
  변경 시 항상 새 등록·새 `fileId`를 사용하는 규칙을 문서화한다. `CorruptProgress`는
  `SELECT ... FOR UPDATE` 기준 상태 기록/audit/quarantine, 기존 column 기반 `FAILED` 격리 또는
  `COMPLETED` 차단, partial-row retention/delete 후 새 import의 concrete 절차를 갖는다.
  `ImportAlreadyRunning`/`ImportLeaseLost`는 3회·1/2/4초 backoff로 제한하고 소진 시
  typed error/alert를 표면화하며, 무한 retry·health endpoint 책임 전가·schema migration을
  허용하지 않는다.

## 초기 발견과 반영 이력

1. Performance: rank-1/full-array·batch payload 산식 모호성을 window, `MAX_FIXED_ROW_BYTES`,
   checked budget, fixed benchmark로 보완했다.
2. Security: metadata map materialization 순서와 group depth/count, open TOCTOU를 verified
   open → bounded stack/queue scan → materialization 순서로 보완했다.
3. Stability/Ops: terminal `markCompleted`, cancellation lifecycle, null lease/initial upsert,
   active lease 분기, tile별 rollback 의미를 명시했다.
4. Developer/API: mutable sample·same connection·full-rank index·strict EPSG·cross-tile exact
   tuple과 compile fixture를 구체화했다.
5. User/caller: register/import 모두 blocking이라는 점, bounded executor 종료, 예외 recovery와
   2.0.0 migration 책임을 EN·KO 문서 task에 연결했다.
6. 최신 독립 재검토의 API P1/P2는 nullable dimension assignment, rank-1 null location,
   custom lease TTL, exact tuple read-back을 보완하는 patch로 반영했다. User/caller P1/P2는
   명세·계획 예시 parity, 다섯 subtype migration, concrete `CorruptProgress` 격리,
   changed-file 새 `fileId`, 유한 retry/backoff, cooperative cancellation과 side-effect
   재확인을 보완하는 patch로 반영했고, API·Stability/Ops·User/caller 최종 재검토가
   모두 P0/P1/P2/P3=0임을 확인했다.

## Step 3-P 위험 예측과 rollback

| 위험 신호 | 완화/검증 | rollback |
|---|---|---|
| dimension order 전치 | `[time,y,x]`, `[time,x,y]`, `[y,x,time]` exact `ST_X/ST_Y/value` read-back | Task 5부터 수정·재실행 |
| stale owner 혼합 | token/status/DB-UTC fence와 stale renew/complete/fail tests | 현재 기능 commit revert, progress row 격리 |
| duplicate 유실 | slice-wide first pass exact key set, cross-tile rows=0 | tile engine 수정 후 service suite 재실행 |
| heap/batch 초과 | tile/batch/JSONB/working-set counters와 fixed benchmark | limits/planner/writer commit revert |
| 취소 partial write | first-write/flush cancel, current tile rollback, prior tile 보존, await termination | cancellation seam 수정·재실행 |
| corrupt COMPLETED no-op | verified dataset totalSlices/checkpoint invariant | progress 검증 수정 후 재실행 |
| path/metadata TOCTOU | open 전후 identity와 map 전 bounded scan | guard commit revert, 새 fileId 등록 |
| observability 오염 | stable reason·control sanitation·allowlist labels/rejection metric | metric 변경 revert |
| NetCDF/Testcontainers 오탐 | heavy invocation 순차, Colima/context/info 진단 | 환경 수정 후 동일 selector 재실행 |

schema migration이 없으므로 DB rollback은 각 transaction rollback과 fixture cleanup으로 한정하고,
구현 실패 시 branch-local commit만 `git revert`한다. canonical `develop`과 다른 worktree는
건드리지 않는다.

## Step 3-R 구현 증거 (2026-08-27)

승인된 계획의 구현 경계를 실제 source·fixture·테스트·benchmark 결과로 재확인했다.

| 증거 | 결과 | 비고 |
|---|---|---|
| typed exception·checked limits·path fingerprint | **PASS** | `NetCdfException`, `NetCdfImportLimits`, `NetCdfFileGuard`와 pure contract test가 구조화된 오류·overflow·file identity를 검증 |
| 1D/2D axis·CF auxiliary·dimension order | **PASS** | `VariableAxisMap`, tile-local sampler, curvilinear/CF/order fixture와 PostGIS exact tuple/attrs read-back |
| CRS·duplicate·JSONB·same-connection writer | **PASS** | strict EPSG whitelist, slice-wide duplicate preflight, NFC/UTF-8 cap, bounded JDBC batch 계약 |
| lease/progress/cancellation/resource budget | **PASS** | DB UTC CAS, malformed progress quarantine, fence-before-read/write, terminal renew→complete, fixed working-set accounting |
| regression suite | **PASS** | `:bluetape4k-science:test`: `SUCCESS: Executed 233 tests` |
| 1D throughput reference gate | **PASS (pre-final reference)** | baseline 159,123 ms, feature 160,075 ms, `+0.598% < 20%` |
| 2D + auxiliary bounded report | **PASS (pre-final reference, 보고용)** | 1,048,576 cells, 260,697 ms, fixed tile/batch/working-set caps |

소스·문서 범위에는 schema/table/column migration, dependency, module catalog,
workflow 변경이 없다. 기존 rank 1–4 회귀와 새 pure/service 계약을 함께 실행했고,
feature의 임시 benchmark harness는 결과를 기록한 뒤 제거했다.

### 구현 단계의 보류 증거

계획에 있던 `1회 warm-up + 3회 측정 median`은 macOS arm64에서 amd64 PostGIS
image를 emulation하는 환경과 2.5–4.5분의 Testcontainers 실행 시간 때문에 단일
성공한 단일 실행 결과로 제한했다. 따라서 benchmark 문서는 median·표준편차를 주장하지
않으며, 반복 측정과 최종 rank 1 read cap 기준 재측정은 후속 성능 작업으로
남긴다. 이 제한은 구현 correctness와 bounded memory 판정과 분리하며, 1D
`<20%` 수치는 pre-final reference gate로만 기록한다.

**Step 3-R 구현 verdict: PASS with benchmark WATCH.** 계획의 기능·안전·운영
계약은 구현과 fresh local evidence로 통과했으며, 반복 median만 미실행이다. 다음
게이트는 변경 범위 audit, Lore implementation commit, PR exact-head CI/review,
fresh merge approval이다.

## Writer gate와 Step 3-R 종료

| 항목 | 판정 | 근거 |
|---|---|---|
| SPW-01 audience/purpose/evidence | PASS | #1352/#1421, 선행 #1512, 기준 SHA, source anchor, stop condition |
| SPW-02 artifact contract | PASS | goal/architecture/file ownership/task/test/docs/rollback |
| SPW-03 Korean technical register | PASS | 설명·결정은 한국어, code/API/command/path token 보존 |
| SPW-04 technical traceability | PASS | 명세 §1–§10 요구를 Task 1–9와 gate/rollback에 연결 |
| SPW-05 read-back | PASS | plan/spec heading·checkbox·code fence·selector·expected result 재독 및 placeholder/terminology/diff check |

Step 3-R 구현 종료 조건을 모두 충족했다. 승인 산출물(spec·design review·plan·본 plan review)을
먼저 Lore commit한 뒤, `$test-driven-development`와 `$bluetape-kotlin-patterns`를 다시 읽고
RED/GREEN 구현과 local verification을 완료했다. 다음 변경은 implementation Lore commit,
PR exact-head CI/review, fresh merge approval, rebase merge 순서로 진행하며 schema·workflow와
canonical `develop`은 merge gate 전까지 변경하지 않는다.
