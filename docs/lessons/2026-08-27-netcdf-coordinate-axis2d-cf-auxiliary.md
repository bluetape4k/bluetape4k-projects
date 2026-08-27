# NetCDF `CoordinateAxis2D`·CF auxiliary 구현 교훈

## 관찰된 핵심 교훈

### tile-local coordinate window가 필요하다

2D 축을 셀마다 `read1D`/`read2D`로 다시 읽으면 NetCDF 접근 호출 수가 셀 수에
비례해 증가한다. `CoordinateAxis2D`와 CF auxiliary 값은 현재 tile의 row/column
window를 한 번만 읽고, tile-local index로 재사용해야 bounded 메모리와 실제
throughput을 함께 유지할 수 있다. projected CRS에서도 coordinate source와
reprojection point provider를 tile 경계 안에 두어야 한다.

### 1D에서는 read window와 JDBC batch를 분리한다

1D reference run에서는 최대 `65,536`개를 한 번에 읽되, JDBC pending rows는
`1,000`개에서 flush하는 방식이 효과적이었다. 이 경계를 적용한 pre-final
feature 측정은 baseline 대비 `+0.598%`(`160,075 ms` 대 `159,123 ms`)로
`20%` reference gate를 통과했다. 최종 hardening에서는 rank 1 read도
`1,000` values로 낮췄으므로 위 수치는 final exact benchmark가 아니다. 큰
NetCDF read window와 작은 DB batch를 같은 수치로 묶으면 importer가 불필요하게
느려지므로 두 cap을 독립적으로 유지한다.

### 마지막 checkpoint는 renew 후 complete해야 한다

마지막 row write와 lease fence 뒤에 같은 transaction에서 `renewLease`로
`lastSliceIdx`를 먼저 기록하고 `markCompleted`를 호출해야 한다. 이 순서를
지키면 `COMPLETED` 상태의 terminal checkpoint, `completedAt`, null lease가
동시에 보장되고, 재시작 직전의 유효한 lease도 만료되지 않는다.

### malformed progress는 덮어쓰지 말고 격리한다

`IN_PROGRESS`인데 `lease_expires_at`이 null인 progress row는 정상 owner로
취급해 덮어쓰면 안 된다. acquire 조건에서 제외하고 row lock 아래
`CORRUPT_PROGRESS:<progressId>`로 격리한 뒤 재시도해야 한다. 반대로
`COMPLETED`의 invariant 오류는 terminal 결과를 바꾸지 않고 typed
`CorruptProgress`로 중단한다.

### duplicate와 schema 경계는 기존 구조를 재사용한다

중복 canonical coordinate는 두 번째 pass에서 insert하기 전에 slice 전체를
검사해야 부분 row가 남지 않는다. 기존 `location`은 `(longitude, latitude)`
PostGIS point로, 기존 `attrs` JSONB는 numeric CF auxiliary로 재사용하면 새
table/column이나 migration 없이 기존 consumer와 호환된다.

### 벤치마크 환경과 통계 한계를 분리해 기록한다

이번 실행은 macOS Colima arm64에서 `postgis/postgis:16-3.5` amd64 image를
emulation했다. 1D와 2D 실행은 Testcontainers를 순차 실행하고 성공 출력만
기록했지만, 각 실행 시간이 길어 계획한 `1 warm-up + 3 measured median`은
완료하지 못했다. 그러므로 수치는 단일 성공 실행 결과이며, median·분산·통계적
유의성을 주장하지 않는다. 후속 성능 비교에서는 같은 fixture SHA와 같은
container/runtime 조건을 먼저 고정한다.

## 재사용할 검증 순서

1. fixture generator와 SHA-256을 baseline/feature 양쪽에서 확인한다.
2. tile read, coordinate window, pending JDBC rows, serializer/duplicate
   working-set counter를 cap과 대조한다.
3. rank 1 throughput reference와 rank 2+ 보고용 결과를 별도 판정한다.
4. Testcontainers hiccup는 runtime 상태를 확인하고 동일 selector를 재실행한
   뒤, 실패와 성공을 문서에서 분리한다.

## DoD Status

- tile-local coordinate window·1D read/batch 분리·checkpoint 순서·progress 격리:
  **구현 및 회귀 테스트 PASS**.
- 기존 schema 재사용과 duplicate preflight: **구현 및 계약 테스트 PASS**.
- benchmark 환경/단일 실행 제한 기록: **완료**.
- 반복 median benchmark: **PENDING** — 별도 후속 실행이 필요하다.
