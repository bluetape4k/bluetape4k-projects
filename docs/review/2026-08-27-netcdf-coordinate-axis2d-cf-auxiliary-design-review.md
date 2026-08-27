# #1352 NetCDF CoordinateAxis2D·CF auxiliary 설계 통합 검토

## 검토 범위와 기준

- **검토 일자**: 2026-08-27
- **이슈/에픽**: [#1352](https://github.com/bluetape4k/bluetape4k-projects/issues/1352) /
  [#1421](https://github.com/bluetape4k/bluetape4k-projects/issues/1421)
- **검토 대상**: `docs/superpowers/specs/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-design.md`
- **기준 branch/SHA**: `feat/1352-coordinate-axis2d-cf-grid` /
  `origin/develop@45260871f58433a78f2d633c235010f661d22c6e`
- **선행 train**: PR #1512가 #1343 문서 계약을 `develop`에 rebase merge한 상태
- **검토 방식**: main-session 설계 통합과 Performance, Stability/Ops, Security,
  Developer/API, User/caller 여섯 독립 관점의 최신 명세 재검토
- **변경 경계**: 이 문서는 읽기 전용 관점 검토 기록이며 코드·스키마·workflow·PR·merge를
  변경하지 않는다.

## 통합 판정

| 우선순위 | 관점 | 최신 근거 | 판정/조치 | 재검토 |
|---|---|---|---|---|
| P0 | 전체 | 승인된 목표, 저장 경계, lease fence, 중복 정책과 실패 중지가 명세에 연결됨 | 0건 | 불필요 |
| P1 | 전체 | 여섯 관점 최신 재검토가 모두 P1=0 | 0건 | 불필요 |
| P2 | 전체 | 각 관점에서 발견된 미완 계약을 명세에 반영함; Ops의 metric label allowlist·lease TTL/runbook·artifact retention은 후속 운영 문서 보강으로 기록 | 0건 | 불필요 |
| P3 | 전체 | 최신 재검토에서 별도 잔여 사항 없음 | 0건 | 불필요 |

**최종 verdict: PASS (P0=0, P1=0, P2=0, P3=0).** 후속 계획 검토에서 확인된 API·호출자
보완까지 명세에 반영했으며, 구현 착수 전 2026-08-27 사용자 승인을 수신했다.

## 여섯 관점별 확인

### Performance

- bounded tile read(`<=65,536` cells), JDBC batch(`<=1,000` rows), coordinate/cache
  64 MiB, owned working-set 128 MiB와 checked `Long` 산식을 고정했다.
- spatial slice를 preflight와 write의 두 pass로 분리해 tile 경계 duplicate를 놓치지 않고,
  preflight 이전 DB insert를 금지한다.
- `CoordinateReader.read1D/read2D`가 모든 축/auxiliary 접근을 bounded window로 통일하고,
  `read2D`의 row-major flat layout과 `index = localRow * columnCount + localColumn`을
  명시했다. full-grid materialization과 직접 `getCoordValue` 우회는 금지된다.
- 1D 동일 cell-count baseline과 2D+auxiliary report-only benchmark, clean DB·동일 JVM/
  PostGIS·warm-up/median 절차가 있어 성능 주장을 과장하지 않는다.

### Stability/Ops

- `lease_expires_at`을 acquire마다 새로 발급하는 opaque token으로 사용하고, tile 시작·commit·
  `markFailed` 모두 status/token/`> CURRENT_TIMESTAMP` CAS와 affected-row=1을 요구한다.
- progress row lock을 tile commit까지 유지하며, 중간 tile은 checkpoint를 전진시키지 않고
  마지막 tile만 `renewLease(lastSliceIdx=...)`를 같은 transaction에 포함한다.
- `PENDING/FAILED`, `IN_PROGRESS`, `COMPLETED` invariant, null lease repair, checkpoint 범위,
  fingerprint와 `CorruptProgress`를 명시했다. cancellation/interrupt는 `FAILED`로 바꾸지 않고
  원래 예외와 interrupt 상태를 보존한다.
- DB UTC를 단일 시계로 사용하고, metric label allowlist·lease TTL/runbook·artifact retention은
  구현 후 운영 문서 보강 항목으로 남긴다. 현재 P0/P1 차단 사유는 아니다.

### Security

- trusted-admin 경계, regular-file·NUL/control·URI·symlink 거부, 구성요소별 `NOFOLLOW_LINKS`,
  open 전후 identity 재검증으로 path/TOCTOU 경계를 고정했다.
- file size, metadata·dimension·variable·cell·slice·cache·working-set hard limit과 caller
  deadline을 명시해 입력 metadata가 자원을 무한히 소비하지 않도록 했다.
- EPSG ASCII/integral grammar, 충돌 attribute 거부, whitelist와 final lon/lat bounds를
  명시하고 WGS84 묵시 fallback을 금지했다.
- JSONB는 NFC strict UTF-8 key, control/surrogate/예약 접두사 거부, 8,192-byte cap과 typed
  placeholder를 사용한다. batch의 설명되지 않은 결과는 bounded 검증 후 rollback한다.

### Developer/API

- 공개 blocking `registerFile`/`importGridValues` 시그니처와 기존 `location`/`attrs` schema를
  유지한다. 새 다섯 subtype(`UnsupportedCoordinateAxis`, `DuplicateCoordinate`,
  `ResourceLimitExceeded`, `FileChanged`, `CorruptProgress`)는 2.0.0 major 경계의 sealed
  subtype임을 명시하고, base catch/`else` migration과 compile fixture를 계획했다.
  nullable `timeDim`/`levelDim`은 조건부 full-rank assignment로 처리하고, rank 1
  `TileRow`의 nullable location·typed SQL NULL invariant와 기존 custom `leaseTtl`을 보존한다.
- `VariableReader`는 data variable, `CoordinateReader`는 모든 axis/auxiliary, `CoordinateSampler`
  는 caller-owned mutable target, `TileBatchWriter`는 active Exposed transaction의 동일
  `java.sql.Connection`만 받는 것으로 역할을 분리했다.
- writer는 새 DataSource/transaction/connection close를 하지 않으며, `PreparedStatement`만
  닫는다. identity·rollback 원자성·batch status를 recording seam으로 검증한다.
- auxiliary map lifetime은 동기 직렬화 및 다음 셀 전 clear로 고정하고, row-major `read2D` layout과
  reversed-axis fixture를 수용 기준에 연결했다.

### User/caller

- trusted-admin 환경의 blocking Kotlin 호출을 virtual-thread `Future`에서 실행하고,
  caller-owned deadline/interrupt, cooperative cancel, bounded `shutdownNow()`/
  `awaitTermination`, timeout 후 progress·lease·partial-row side-effect 재확인,
  `location`/`attrs` read-back 예시를 명세와 README/KDoc 갱신 항목에 포함했다.
- 다섯 sealed subtype 모두의 migration branch/기본 `else`를 기록하고,
  `UnsupportedCoordinateAxis`/`UnsupportedProjection`/`ResourceLimitExceeded`,
  `FileOpen`/`FileChanged`/`MissingCoordinate`/`VariableNotFound`/`UnsupportedVariable`,
  `DuplicateCoordinate`는 수정 후 항상 새 등록·새 `fileId`를 사용한다. `CorruptProgress`는
  `SELECT ... FOR UPDATE` 기준 상태 기록과 audit/quarantine, 기존 column 기반 `FAILED` 격리
  또는 `COMPLETED` 차단, partial-row 보존·삭제 후 새 import 절차를 구체화했다.
  `ImportAlreadyRunning`/`ImportLeaseLost`는 3회·1/2/4초 backoff로 제한하고 무한 재시도는
  허용하지 않는다.
- 기존 `location` 기반 소비자는 auxiliary key를 해석하지 않아도 동작하며, schema migration·
  release note·workflow 변경은 이번 child 범위 밖으로 고정했다.

## 핵심 보완 이력

초기 설계 승인 후 Step 2-R에서 확인된 P1/P2를 다음처럼 보완했다.

1. 대용량 전체 배열 read, 배치·cache·working-set 상한, duplicate preflight와 고정 benchmark를
   추가했다.
2. lease token CAS, row lock, checkpoint 순서, DB UTC clock, malformed progress와 fingerprint를
   명시했다.
3. schema migration 없이 duplicate coordinate 유실을 막도록 slice-wide exact key 검사를 추가했다.
4. strict CRS grammar/bounds, trusted path·TOCTOU, JSONB key/payload와 resource limits를
   고정했다.
5. sealed exception의 다섯 subtype 2.0.0 migration, `CoordinateReader` bounded seam,
   nullable rank index와 동일 Exposed connection/TTL compatibility, rollback 원자성,
   auxiliary lifetime, caller 복구 예시를 추가했다.

## 수용 기준·writer gate

| 항목 | 판정 | 근거 |
|---|---|---|
| #1352 수용 기준이 fixture/read-back/CRS/regression/performance로 매핑됨 | PASS | 설계 §9 표와 §6 테스트 목록 |
| Type A 여섯 관점 최신 재검토 | PASS | 각 관점 P0/P1/P2/P3=0 |
| 공개 API·schema·migration 경계 | PASS | 설계 §2.2, §4.4, §5, §7 |
| rollback·lease·cancellation·resource 중지 조건 | PASS | 설계 §2.3, §4.3, §5, §8 |
| SPW-01 audience/purpose/evidence | PASS | 이슈·선행 PR·기준 SHA·저장소 근거를 명시 |
| SPW-02 artifact contract | PASS | 문제·대안·내부 계약·오류·테스트·DoD 포함 |
| SPW-03 Korean technical register | PASS | 사용자/검토자용 한국어, 코드/API/명령 토큰 보존 |
| SPW-04 technical traceability | PASS | source anchor·schema·lease·수용 기준 연결 |
| SPW-05 read-back | PASS | heading/table/code fence·결정·범위·잔여 P2를 재독 |

## 결론과 다음 게이트

설계는 구현에 전달할 수 있는 상태이며 최신 독립 검토의 P0/P1 차단 결함이 없다. 다만 Step
2-R에서 공개 API 호환성·트랜잭션 경계·호출자 복구 절차가 보완되었으므로, 이전 설계 승인이
이 보완까지 자동으로 승인한 것으로 간주하지 않는다. 사용자의 **명세 재승인**을 2026-08-27에
수신했으며, 다음 게이트는 `writing-plans`와 Step 3-R 계획 검토이고 코드는 그 이후에만 수정한다. PR 생성·rebase
merge·release/dispatch는 이 문서 범위가 아니며 exact-head와 fresh approval을 별도로 요구한다.
