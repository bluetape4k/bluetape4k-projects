# NetCDF `CoordinateAxis2D`·CF auxiliary bounded 임포트 벤치마크

## 목적과 범위

이 기록은 Epic #1421의 child #1352에서 `CoordinateAxis2D`와 numeric CF
auxiliary coordinate를 기존 `NetCdfGridValueTable`에 bounded 하게 임포트하는
경로의 성능·메모리 계약을 검증한다. schema migration, 새 dependency,
workflow 변경은 포함하지 않는다.

비교 기준은 `origin/develop`의 기준 SHA이고, feature 측정은 동일한 fixture와
동일한 Testcontainers 경로를 사용한 구현 branch 실행 기준 데이터다. 1D 경로는
기준 대비 throughput reference gate를 적용하고, 2D+auxiliary 경로는 회귀 기준선이
없는 보고용 결과로 보존한다.

## 고정 workload

- 생성기: `NetCdfSampleWriter.writeLargeContractSample(path, 1024, 1024)`와
  동일한 deterministic generator.
- 셀 수: `1,048,576` (`1024 × 1024`).
- 값/좌표: row-major 순서의 고유한 `(longitude, latitude, value)` 조합과
  하나의 numeric auxiliary coordinate를 사용한다. 1D 비교는 같은 cell-value
  workload를 `temperature_1d`로 읽고, 2D 보고값은 `temperature_2d`와 auxiliary를
  함께 읽는다.
- fixture SHA-256:
  `2de7ae11c2279bf2f0b30148f485c7932572cad45ed91274906ca22ba631e6a5`
- 타일/배치 계약: tile read 최대 `65,536` cells, JDBC pending rows 최대 `1,000`.
  아래 feature 1D reference run은 당시 `65,536` values read와 `1,000` rows
  flush를 사용했다. 이후 최종 hardening에서 rank 1 read도 `1,000` values로
  낮췄으므로, 아래 수치는 final exact benchmark가 아니다. spatial 경로는 tile
  read와 JDBC flush 상한을 각각 적용한다.

## 재현 명령

기준과 feature 모두 다음 selector를 사용했다.

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.benchmark - large1d*' \
  -PincludeTags=slow-netcdf --no-configuration-cache --console=plain
```

2D+auxiliary 보고값은 다음 selector로 실행했다.

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.benchmark - large2d*' \
  -PincludeTags=slow-netcdf --no-configuration-cache --console=plain
```

각 실행은 Testcontainers를 포함하므로 동시에 실행하지 않았다. 테스트 결과와
Gradle 출력은 모두 `BUILD SUCCESSFUL`이었다.

## 실행 환경과 기준 데이터

- 기준 branch/SHA: detached baseline worktree,
  `origin/develop@45260871f58433a78f2d633c235010f661d22c6e`.
- feature 실행 기준 데이터: `feat/1352-coordinate-axis2d-cf-grid`의 구현 diff가 있는
  상태에서 측정했다. 이후 duplicate backing-accounting, strict CRS/axis 검증,
  경계 예외 처리를 보완했으므로 아래 수치는 최종 코드의 exact benchmark가
  아니라 pre-final reference로 보존한다. 최종 코드의 correctness와 cap은
  fresh test evidence로 다시 검증했다.
- OS/runtime: macOS, Colima Virtualization Framework, Docker context `default`.
- Docker: `29.2.1` (`aarch64`), `postgis/postgis:16-3.5` (`amd64` image를 arm64
  환경에서 emulation).
- JVM: GraalVM JDK 25 (`/Library/Java/JavaVirtualMachines/graalvm-jdk-25`).
- Gradle: `9.7.0` wrapper.

## 결과

| 경로 | cells | elapsed | cells/sec | peak heap | 판정 |
|---|---:|---:|---:|---:|---|
| baseline 1D | 1,048,576 | 159,123 ms | 6,589.72 | 504,616,768 B (약 481.1 MiB) | 비교 기준 |
| feature 1D (pre-final reference) | 1,048,576 | 160,075 ms | 6,550.53 | 182,604,160 B (약 174.1 MiB) | baseline 대비 +0.598%, reference gate 통과 |
| feature 2D + CF auxiliary (pre-final reference) | 1,048,576 | 260,697 ms | 4,022.20 | 308,149,776 B (약 293.9 MiB) | 기준선이 없는 보고용 |

1D feature reference elapsed는 `160,075 ms`, baseline은 `159,123 ms`로 측정되어
`(160075 / 159123 - 1) × 100 = +0.598%`이다. 이는 최종 rank 1 read cap
hardening 전 측정값이다. JVM peak heap은 NetCDF,
PostgreSQL/Testcontainers, Gradle와 공유되므로 bounded 계약의 판정값으로
사용하지 않고 참고값으로만 기록한다.

## 구현 내부 working-set 회계

고정 상수로 계산한 import-owned working set은 JVM peak와 분리해 검증했다.

| 항목 | 1D | 2D + auxiliary |
|---|---:|---:|
| tile buffer (`65,536 × 8`) | 524,288 B | 524,288 B |
| coordinate window (`65,536 × (2 + 1) × 8`) | 0 B | 1,572,864 B |
| serializer/batch scratch (`1,000 × (256 + 8,192)`) | 8,448,000 B | 8,448,000 B |
| duplicate key set (보수치 `cells × 32`) | 0 B | 33,554,432 B (약 32.00 MiB) |
| 총 owned working set | 8,972,288 B (약 8.56 MiB) | 44,099,584 B (약 42.06 MiB) |

2D의 `coordinateBytes + duplicateSetBytes`는 약 33.5 MiB로 `64 MiB`
working-set cap 이하이고, 총 owned working set은 약 42.1 MiB로 `128 MiB`
cap 이하이다. duplicate key set은 승인된 `cells × 32` 보수 회계를
`MemoryBudget`에 사용하며, 실제 backing table의 capacity·slot 회계는
별도 구현 counter(`capacity × 25 B`)로 관찰한다.
coordinate cache는 `64 MiB`, JSONB auxiliary는 `8,192` UTF-8 bytes,
tile은 `65,536` cells, JDBC batch는 `1,000` rows cap을 사용한다.

## 해석과 제한

- 1D reference throughput gate는 통과했다. 당시 65,536-value read와
  1,000-row flush를 사용해 bounded read와 batch 계약을 동시에 유지했으며,
  최종 코드는 rank 1 read를 1,000 values로 더 보수적으로 제한한다. 최종
  hardening 이후 동일 조건의 재측정은 후속 작업이다.
- 2D+auxiliary 결과는 tile-local coordinate window가 per-cell coordinate
  read보다 안정적이라는 구현 경향을 확인하는 pre-final reference이며,
  기존 baseline이 없으므로 회귀 pass/fail 기준으로 해석하지 않는다.
- 계획된 `1회 warm-up + 3회 측정 median`은 실행하지 않았다. arm64에서
  `amd64` PostGIS image를 emulation하고 각 Testcontainers 실행이 약
  2.5–4.5분 걸려 단일 성공 실행 결과만 남겼다. 따라서 median,
  표준편차, 통계적 유의성은 주장하지 않는다.
- feature 1D 첫 시도에서 PostgreSQL EOF가 발생했으나 Colima/docker 상태를
  확인한 뒤 동일 selector를 재실행해 성공했다. 실패 실행은 환경 hiccup으로
  분리했고 성공 실행만 수치에 사용했다.

## DoD Status

- 상태: bounded import 및 1D throughput reference **PASS**.
- 메모리/타일/배치 cap: **PASS** (고정 counter 회계).
- 2D+CF auxiliary: **PASS (pre-final reference, 보고용)**.
- warm-up + 3회 median: **PENDING** — emulation 환경에서 단일 실행 결과만 확보.
- schema/dependency/workflow 변경: **없음**.
