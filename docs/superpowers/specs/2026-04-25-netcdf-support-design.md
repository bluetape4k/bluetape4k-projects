# NetCdf 지원 완성 (utils/science) — 설계 스펙

- **일자**: 2026-04-25
- **이슈**: #107 (utils/science — NetCdf 지원 완성)
- **브랜치**: `feat/science-netcdf` (worktree: `.worktrees/feat-science-netcdf`)
- **관련 연구**: `.worktrees/feat-science-netcdf/docs/superpowers/research/2026-04-25-netcdf-support-research.md`
- **대상 심볼**: `io.bluetape4k.science.exposed.service.NetCdfCatalogService`
- **관련 모듈**: `bluetape4k-science` (utils/science)
- **개정 이력**:
  - v1 (초안)
  - v2 (사용자 확정 결정 9건 + 추가 5건)
  - **v3 (Codex+Critic 종합 피드백: 동시성 가드, sliceIdx 선형화, projected CRS 2D 변환, axis-dim 매핑, unique index 등)**
- **미해결 항목**: **0 (모두 확정)**

---

## 1. 개요

### 1.1 문제

`NetCdfCatalogService.registerFile()` / `importGridValues()` 두 메서드가 Phase 4
(UCAR 아티팩트 좌표 미확정)로 인해 `TODO()` 상태. `Libs.kt:1226` 의
`edu.ucar:netcdfAll:5.6.0` 은 **Maven 에 존재하지 않는 좌표(유령)**.
Maven 배포 최신은 `edu.ucar:netcdfAll:5.5.2` (2022-06-30). 한편 **UCAR 공식 다운로드
페이지 및 GitHub release 에는 `netcdfAll` 5.9.x jar 가 여전히 배포**되고 있어,
필요 시 사용자는 이 경로로 직접 설치 가능하다.

본 모듈의 기본 방향은 **모듈식(modular) 좌표**인 `cdm-core:5.9.1` + `netcdf4:5.9.1`
경로를 사용하는 것. (출처:
<https://docs.unidata.ucar.edu/netcdf-java/current/userguide/using_netcdf_java_artifacts.html>,
<https://www.unidata.ucar.edu/news/netcdf-java-version-591-released>)

### 1.2 목표

- 정확한 의존성 좌표 확정: `edu.ucar:cdm-core:5.9.1` + `edu.ucar:netcdf4:5.9.1`
- `NetCdfCatalogService` 두 메서드 실제 구현 (blocking 시그니처 유지)
- **axis-to-dimension 매핑**(CF 비표준 shape 지원) 기반 rank 1/2/3/4 모두 지원
- **CRS 재투영**: Geographic 1D caching + Projected 2D pair caching (proj4j)
- **Micrometer 계측**: `MeterRegistry?` 선택 주입, 지표 5종
- **재개(resume) 지원**: `NetCdfImportProgressTable` (heartbeat lease) + `lastSliceIdx` 단일 선형 cursor
- **NetcdfFormatWriter 동적 생성 + Unidata CF-1.x 공개 샘플** 병용 테스트
- 좌표축 누락 / 미지원 CRS / 동시 실행 시 타입 예외
- 슬로우 회귀 테스트는 `@Tag("slow-netcdf")`로 분리, nightly 프로파일에서만 실행

### 1.3 비목표

- GRIB / BUFR / OPeNDAP 포맷 지원 (후속 이슈)
- `cdm-core 6.0.0-beta1` (`ucar.array.*` 신규 API) 마이그레이션 — 5.9.x 유지
- `suspend fun` 변환 (이슈 #107 요구사항에 따라 blocking 유지)
- 대용량 파일용 스트리밍 Flow API — 현재 범위 외
- WMS/WCS 서비스 노출 — 범위 외
- `utils/science/README.md` 전체 재작성 — **NetCdf 챕터 신설/보강 한정** (후속 Issue)
- **Curvilinear / 2D auxiliary coordinates 비지원 (Codex #4)** — `CoordinateAxis2D` (예: rotated pole, tripolar ocean grid) 와 CF `coordinates` 속성 기반 auxiliary coordinate 는 본 PR 스코프 외. 파일에 `CoordinateAxis1D` 가 아닌 lat/lon 이 있으면 `MissingCoordinate` throw. 향후 이슈로 분리.

---

## 2. 배경 및 제약 (연구 요약 + 사용자 확정)

### 2.1 사용자 확정 결정 요약 (v2 → v3 보강)

| # | 항목 | 확정 |
|---|------|------|
| D1 | CRS 정책 | proj4j 재투영 — **Geographic 1D + Projected 2D pair** 캐싱; 화이트리스트 외 → `UnsupportedProjection` |
| D2 | NaN / missing | `log.debug` skip + `netcdf.import.nan.skipped` counter |
| D3 | rank=1 허용 | 시계열 저장 — `location=null`, `levelIdx=0` |
| D4 | level 축 fallback | `AxisType` 우선 → 이름 기반(`level`, `lev`, `plev`, `pressure`, `depth`, `z`, `height`) fallback |
| D5 | Micrometer | `MeterRegistry?` 선택 주입, 5개 지표 |
| D6 | 재개 지원 | `NetCdfImportProgressTable` + **heartbeat lease** + **`lastSliceIdx` 단일 선형 cursor** |
| D7 | 슬로우 테스트 | `@Tag("slow-netcdf")` + nightly CI 분리; 기준: 단일 테스트 > 5초 또는 샘플 > 1MB |
| D8 | README 범위 | NetCdf 챕터 신설/보강만 (전체 재작성은 후속 Issue) |
| D9 | 테스트 샘플 | `NetcdfFormatWriter` 동적 생성 + Unidata 공개 CF-1.x 샘플 체크인 |

### 2.2 연구 결과에서 확정된 사항

1. **의존성 좌표**
   - `edu.ucar:cdm-core:5.9.1` — CDM API + NetCDF-3 IOSP
   - `edu.ucar:netcdf4:5.9.1` — HDF5 / NetCDF-4 IOSP (순수 Java, JNI 불필요)
   - `edu.ucar:netcdfAll:5.6.0` **제거** (유령 좌표)
2. **저장소**: **루트 `build.gradle.kts` 에 이미 선언됨** —
   `https://artifacts.unidata.ucar.edu/repository/unidata-all/`.
   **모듈별 `utils/science/build.gradle.kts` 에 저장소 추가 task 불필요** (X1).
3. **메서드 시그니처**: blocking `fun registerFile(filePath: String): Long` /
   `fun importGridValues(fileId: Long, variableName: String)` 유지
4. **트랜잭션**: Exposed v1 JDBC `transaction { }` (blocking)
5. **스키마 변경**: `NetCdfGridValueTable.location` nullable + unique index;
   `NetCdfImportProgressTable` 신규

### 2.3 API 서피스 (CDM 5.x)

- `ucar.nc2.NetcdfFiles.open(path)` — 정적 팩터리, `AutoCloseable`
- `ucar.nc2.dataset.NetcdfDatasets.openDataset(path)` — enhanced 모드
- `ucar.nc2.dataset.NetcdfDataset.getDefaultEnhanceMode()` — 자동 scale/offset/missing 처리
- `ucar.nc2.dataset.CoordinateAxis` · `CoordinateAxis1D` (1D 만 지원 — `CoordinateAxis2D` 는 비목표, §1.3 참조)
- `ucar.nc2.constants.AxisType` — `Time / Lat / Lon / Pressure / Height / GeoZ`
- `ucar.nc2.Variable.read(origin, shape)` — 섹션 읽기 (전체 read 금지)
- proj4j `CoordinateReferenceSystem` · `CoordinateTransformFactory` · `ProjCoordinate`

### 2.4 위험 요약 (v1 + v2 + v3)

| # | 출처 | 위험 | 완화 |
|---|------|------|------|
| R1 | 연구 | 유령 아티팩트 5.6.0 | 5.9.1로 교체 |
| R2 | 연구 | VT pinning (`synchronized`) | import 루프는 platform thread pool 권장 문서화 |
| R3 | 연구 | 대용량 OOM | 슬라이스별 `read(origin, shape)` 강제 |
| R4 | 연구 | POINT lon/lat 순서 | `geoPointOf(lon, lat)` 단일 진입점 |
| R5 | 연구 | CVE (protobuf / gson) | 사용자 BOM 버전 강제 README 안내 |
| R6 | v1 | NaN / missing | 자동 언팩 + skip + debug log + counter |
| R7 | v1 | rank 오인식 | 1/2/3/4 허용, 5+ 는 `UnsupportedVariable` |
| R8 | v1 | SRID 불일치 | proj4j 재투영, 화이트리스트 외 `UnsupportedProjection` |
| R9 | v1 | 축 방향 뒤집힘 | `getCoordValue(i)` 그대로 사용 |
| R10 | v1 | 트랜잭션 경계 | 슬라이스별 커밋 |
| R11 | v1 | Path traversal | KDoc 로 호출자 책임 명시 |
| R12 | v2 | proj4j 변환 비용 | 파일당 1회 계산 + 캐싱 (`CoordinateReprojector`) |
| R13 | v2 | resume 동시성 | **heartbeat lease + raw UPDATE WHERE 조건** (C2) |
| R14 | v2 | Unidata 샘플 라이선스 | BSD-3-Clause 만 체크인 + `LICENSE` 동봉 |
| R15 | v3 | Projected CRS 1D 캐싱 오류 | **2D pair 캐싱** (C4) — UTM/Polar Stereographic 정확성 |
| R16 | v3 | Dimension order 다양성 | **`VariableAxisMap`** 으로 dim→AxisType 매핑 (C5) |
| R17 | v3 | 4D cursor 독립 계산 오류 | **`lastSliceIdx` 단일 선형 cursor** (C3) |
| R18 | v3 | batchInsert 중복 입력 | **unique partial index + `upsert` DO NOTHING** (M2) |

---

## 3. 설계 결정 (확정 — v3)

### 3.1 (a) 읽기 전략 — Single slice per sliceIdx

- sliceIdx 선형 cursor (C3 참조). 슬라이스 shape 은 `VariableAxisMap` 으로부터 동적 구성.
- 2D 변수 (lat, lon): 단일 슬라이스, sliceIdx=0
- 3D 변수: sliceIdx = timeIdx (levelN=1 기본)
- 4D 변수: sliceIdx = timeIdx × levelN + levelIdx (row-major)
- 1D 시계열: sliceIdx=0 (단일 슬라이스, 전체 시계열 한 번에 읽고 행별 timeIdx 기록)

### 3.2 (b) 스키마 마이그레이션

- dev/test: `SchemaUtils.create` drop-recreate (기존 `NetCdfTableTest.kt` 수정 필요 — §4.4)
- prod: ALTER + CREATE 스크립트 (§4.4)

### 3.3 (c) 에러 정책 — sealed `NetCdfException` (7종)

타입 안전 계층 + NaN은 skip + debug.

### 3.4 (d) level 축 탐지 — AxisType → 이름 fallback

- 1차: `findCoordinateAxis(AxisType.Height | Pressure | GeoZ)`
- 2차: 컴패니언 상수 `LEVEL_AXIS_NAME_FALLBACKS = listOf("level", "lev", "plev", "pressure", "depth", "z", "height")`
- 실패 시 `NetCdfException.MissingCoordinate`

### 3.5 (e) CRS 정책 — Geographic 1D + Projected 2D pair caching (C4)

파일 진입 시 `CoordinateReprojector` 생성:

- **Geographic CRS** (axis.axisType == Lat 또는 Lon):
  독립 1D 배열 캐싱 — `lonValues: DoubleArray`, `latValues: DoubleArray`.
- **Projected CRS** (UTM / Polar Stereographic 등):
  **2D pair 캐싱** — shape `[latN][lonN][2]={lon, lat}`.
  셀당 `(x[lonIdx], y[latIdx])` → proj4j `CoordinateTransform` → `(lon, lat)` 쌍.
  메모리: 1440×721 grid ≈ 16 MB (8byte × 2).
- **지원 CRS 화이트리스트** (README 에 명시):
  - `EPSG:4326` (WGS84)
  - `EPSG:3857` (Web Mercator)
  - `EPSG:32601`..`32660` / `32701`..`32760` (UTM Northern/Southern)
  - `EPSG:3413` (North Polar Stereographic)
  - `EPSG:3031` (South Polar Stereographic)
- 화이트리스트 외 → `UnsupportedProjection(srcCrs)`
- **Thread-safety**: reprojector 는 `importGridValues` 호출 수명 내 단일 스레드 사용.
  `CoordinateTransform` 객체는 `from()` factory 내부에서만 사용 후 폐기.
  최종 `DoubleArray` / 2D pair 버퍼는 immutable snapshot.

### 3.6 (f) axis-to-dimension 매핑 (C5)

rank 기반 고정 가정 제거. `variable.dimensions` 순회 → `AxisType` 매핑:

1. `NetcdfDatasets.openDataset(path).enhance(getDefaultEnhanceMode())` 로 enhance
2. 각 `Dimension` 에 `dataset.findCoordinateAxis(dimensionName)` 로 축 조회
3. `axis.axisType` 로 `Time / Pressure|Height|GeoZ / Lat / Lon` 판별
4. 판별 실패 시 이름 fallback:
   - Lat: `lat`, `latitude`, `nlat`, `y`
   - Lon: `lon`, `longitude`, `x`
   - Time: `time`, `t`
   - Level: `LEVEL_AXIS_NAME_FALLBACKS`
5. 결과: 내부 private data class `VariableAxisMap(timeDim: Int?, levelDim: Int?, latDim: Int?, lonDim: Int?)`
6. lat/lon 매핑 실패 시 `MissingCoordinate`. 1D 시계열은 time dim 만 필요.
   4D 인데 level 매핑 실패 시 `MissingCoordinate("level/...")`.

**결과**: `shape=[lat, lon, time]` 같은 비표준 variable 도 정상 처리.

### 3.7 (g) Micrometer 계측 — 시점 명시 (M3)

`MeterRegistry?` 선택 주입 (null → no-op). 지표 5종:

| 지표 | 타입 | 태그 | 증가 시점 |
|------|------|------|-----------|
| `netcdf.register.duration` | Timer | `status=success\|failure` | `registerFile` 전체 래핑 |
| `netcdf.import.variable.records` | Counter | `variable=<name>` | 슬라이스 commit 직후 `increment(insertedCount)` |
| `netcdf.import.slice.duration` | Timer | — | 슬라이스 단위 measure |
| `netcdf.import.nan.skipped` | Counter | — | NaN skip 발생 시 1씩 |
| `netcdf.import.status` | Counter | `status=success\|failure\|resumed` | 최종 상태. `resumed` 는 진입 시 기존 progress row(lastSliceIdx != null) 감지 시 1회 |

**`ImportAlreadyRunning`** 은 `status=failure` 로 **count 하지 않음** (다른 프로세스 소유이며 호출자가 재시도). M4 참조.

### 3.8 (h) 재개(resume) — heartbeat lease 방식 (C2, C3)

**문제점 (v2 FOR UPDATE NOWAIT)**: tx 커밋 시 락 해제 → 동시성 방지 불가.

**v3 설계**:

- `NetCdfImportProgressTable` 에 `leaseExpiresAt: timestamp?` 컬럼 + `lastSliceIdx: long?` 컬럼
- **진입 시 원자적 upsert (raw SQL)** — 2단계:

  **1단계 — 선조회 (COMPLETED 분기)**:
  ```sql
  SELECT id, status, last_slice_idx, lease_expires_at
    FROM netcdf_import_progress
    WHERE file_id = :fileId AND variable_name = :varName
  ```
  - `status = 'COMPLETED'` → 즉시 no-op return (lease 획득 skip)

  **2단계 — 조건부 upsert (PENDING / FAILED / stale IN_PROGRESS 만 허용)**:
  ```sql
  INSERT INTO netcdf_import_progress
    (file_id, variable_name, status, last_slice_idx, lease_expires_at, started_at, updated_at)
  VALUES (:fileId, :varName, 'IN_PROGRESS', NULL, :leaseExp, :now, :now)
  ON CONFLICT (file_id, variable_name)
  DO UPDATE SET
    status = 'IN_PROGRESS',
    lease_expires_at = :leaseExp,
    started_at = :now,
    updated_at = :now,
    error_message = NULL
  WHERE
    netcdf_import_progress.status IN ('PENDING', 'FAILED')
    OR (netcdf_import_progress.status = 'IN_PROGRESS'
        AND netcdf_import_progress.lease_expires_at < :now)
  RETURNING id, status, last_slice_idx
  ```
  - 갱신 0 row + 기존 status=IN_PROGRESS + lease 유효 → **`ImportAlreadyRunning`** throw
  - 갱신 성공 (INSERT 또는 UPDATE) → 기존 `lastSliceIdx` 로부터 재개
  - **COMPLETED 는 WHERE 에서 제외** → 갱신 불가, 1단계에서 이미 분기됨 (Codex #1)
- **Lease TTL**: 5분 (`LEASE_TTL = Duration.ofMinutes(5)`)
- **Heartbeat**: 슬라이스 10개마다 **또는** 30초 경과 시 `leaseExpiresAt = now() + 5min` 갱신 (슬라이스 commit tx 안에서 함께 수행)
- **완료**: `status=COMPLETED, leaseExpiresAt=null, completedAt=now()`
- **실패**: `status=FAILED, leaseExpiresAt=null, errorMessage=<msg>`
- **`COMPLETED` 재호출**: 즉시 no-op + info 로그만 (`netcdf.import.status` counter 증가 없음)

---

## 4. 스키마 변경

### 4.1 `NetCdfGridValueTable` — nullable location + unique partial indexes (M2)

```kotlin
object NetCdfGridValueTable: LongIdTable("netcdf_grid_values") {
    val fileId = reference("file_id", NetCdfFileTable)
    val variableName = varchar("variable_name", 255)
    val location = geoPoint("location").nullable()   // CHANGED
    val timeIdx = integer("time_idx").default(0)
    val levelIdx = integer("level_idx").default(0)
    val value = double("value")
    val attrs = jacksonb<Map<String, Any?>>("attrs").nullable()

    // Unique partial indexes — Postgres 전용
    // SchemaUtils.create 이후 raw DDL 로 생성 (테스트 @BeforeAll / prod 마이그레이션)
}
```

**unique partial index DDL**:

PostGIS `geometry` 컬럼은 기본 b-tree operator class 가 없다. unique index 에는 `location` 대신 `ST_AsBinary(location)` 또는 `MD5(ST_AsBinary(location))` 해시를 사용한다 (또는 PostgreSQL 14+ 에서 `geometry_ops_nd` operator class 로 GIST unique 인덱스 가능하지만 단순성·이식성 우선 해시 채택).

```sql
-- 해시 컬럼 기반 unique partial index (PostGIS geometry B-tree 미지원 우회)
CREATE UNIQUE INDEX uk_netcdf_grid_values_full
  ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx, MD5(ST_AsBinary(location)))
  WHERE location IS NOT NULL;

CREATE UNIQUE INDEX uk_netcdf_grid_values_nulloc
  ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx)
  WHERE location IS NULL;
```

**주의**: `MD5(ST_AsBinary(location))` 는 STABLE 함수 조합 → expression index 로 유효. 동일 grid cell 은 동일 WKB 바이트 → 동일 해시 보장. 테스트 #28 에서 DDL 성공 자체도 검증해야 한다.

**중복 방지 구현 (Codex #2)**: Exposed `upsert` 는 conflict target 으로 **컬럼만** 지정 가능하며, expression index (`MD5(ST_AsBinary(location))`) 와 partial predicate (`WHERE location IS NOT NULL`) 에는 대응 불가. 따라서 슬라이스 insert 는 다음 중 하나를 사용:

- **권장**: raw SQL `INSERT INTO netcdf_grid_values (...) VALUES (...) ON CONFLICT DO NOTHING` — conflict target 생략 시 모든 unique 제약이 자동 적용됨 (Postgres). `TransactionManager.current()` 의 prepared statement API 로 배치 실행.
- **대안**: `insertIgnore { }` (Exposed v1) — conflict 발생 시 skip. conflict target 미지정이라 partial expression index 와 자동 매칭됨.

`batchInsert { }` 는 conflict 발생 시 전체 롤백되므로 **사용 금지**. T6 구현 시 `insertIgnore` 또는 raw `ON CONFLICT DO NOTHING` 둘 중 구현자가 실측 후 선택.

### 4.2 `NetCdfGridValueTable` — rank별 컬럼 의미 (M5)

| rank | `timeIdx` | `levelIdx` | `location` |
|------|-----------|------------|------------|
| 1D (time) | 실제 값 | 0 (default) | **null** |
| 2D (lat, lon) | 0 | 0 | 실제 Point |
| 3D (time, lat, lon) | 실제 값 | 0 | 실제 Point |
| 4D (time, level, lat, lon) | 실제 값 | 실제 값 | 실제 Point |

### 4.3 `NetCdfImportProgressTable` — 일반 `LongIdTable` (M1, C2, C3)

```kotlin
object NetCdfImportProgressTable: LongIdTable("netcdf_import_progress") {
    val fileId           = reference("file_id", NetCdfFileTable)
    val variableName     = varchar("variable_name", 255)
    val status           = enumerationByName("status", 20, NetCdfImportStatus::class)
    val lastSliceIdx     = long("last_slice_idx").nullable()   // 선형 cursor (C3)
    val leaseExpiresAt   = timestamp("lease_expires_at").nullable()  // heartbeat (C2)
    val errorMessage     = text("error_message").nullable()
    val startedAt        = timestamp("started_at")
    val completedAt      = timestamp("completed_at").nullable()
    val updatedAt        = timestamp("updated_at")

    init {
        uniqueIndex("ux_netcdf_import_progress_file_var", fileId, variableName)
    }
}
```

**변경점 vs v2**:
- `AuditableLongIdTable` → **일반 `LongIdTable`** (M1: 시스템 임포트 상태에 user context 불필요)
- `lastTimeIdx`, `lastLevelIdx` 제거 → **`lastSliceIdx: long` 단일** (C3)
- `leaseExpiresAt` 신규 (C2)
- `completedAt` 신규, `updatedAt` 수동 관리

### 4.4 마이그레이션 + blast radius (C6)

#### DDL

```sql
-- dev / test: drop-recreate (SchemaUtils 자동) + raw DDL 추가

-- prod:
ALTER TABLE netcdf_grid_values
    ALTER COLUMN location DROP NOT NULL;

-- PostGIS geometry b-tree 미지원 → MD5(ST_AsBinary(location)) 해시 사용
CREATE UNIQUE INDEX uk_netcdf_grid_values_full
  ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx, MD5(ST_AsBinary(location)))
  WHERE location IS NOT NULL;

CREATE UNIQUE INDEX uk_netcdf_grid_values_nulloc
  ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx)
  WHERE location IS NULL;

CREATE TABLE netcdf_import_progress (
    id                 BIGSERIAL PRIMARY KEY,
    file_id            BIGINT       NOT NULL REFERENCES netcdf_files(id),
    variable_name      VARCHAR(255) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    last_slice_idx     BIGINT,
    lease_expires_at   TIMESTAMP,
    error_message      TEXT,
    started_at         TIMESTAMP    NOT NULL,
    completed_at       TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL
);

CREATE UNIQUE INDEX ux_netcdf_import_progress_file_var
    ON netcdf_import_progress (file_id, variable_name);
```

#### 기존 테스트 / 코드 blast radius (확인 필요)

- **`utils/science/src/test/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTableTest.kt:30`**
  - 기존: `private val catalogService = NetCdfCatalogService(fileRepo)`
  - 수정: `NetCdfCatalogService(fileRepo, progressRepo, meterRegistry = null)`
- **`NetCdfTableTest.kt:137`** — `NetCdfCatalogService - registerFile 호출 시 NotImplementedError 발생`
  - **삭제** (실 구현으로 대체)
- **`NetCdfTableTest.kt:146`** — `NetCdfCatalogService - importGridValues 호출 시 NotImplementedError 발생`
  - **삭제**
- `SchemaUtils.create(NetCdfFileTable, NetCdfGridValueTable)`
  → `SchemaUtils.create(NetCdfFileTable, NetCdfGridValueTable, NetCdfImportProgressTable)` 로 확장
  + 테스트 `@BeforeAll` 에 partial index 생성 raw SQL 실행
- DDL rollback:
  - 개발/테스트: drop-and-recreate
  - 프로덕션: ALTER + DROP TABLE (역순)
  - null cleanup (사용자 판단): `UPDATE netcdf_grid_values SET location = NULL WHERE ...`

---

## 5. API 설계

### 5.1 sealed `NetCdfException` (7종)

```kotlin
sealed class NetCdfException(message: String, cause: Throwable? = null): RuntimeException(message, cause) {
    class FileOpen(path: String, cause: Throwable):
        NetCdfException("Failed to open NetCDF file: $path", cause)
    class FileRecordNotFound(fileId: Long):
        NetCdfException("NetCDF file record not found (fileId=$fileId)")
    class VariableNotFound(fileId: Long, variableName: String):
        NetCdfException("Variable '$variableName' not found in file (id=$fileId)")
    class UnsupportedVariable(variableName: String, rank: Int):
        NetCdfException("Variable '$variableName' has unsupported rank=$rank (must be 1, 2, 3, or 4)")
    class MissingCoordinate(axisName: String):
        NetCdfException("Required coordinate axis '$axisName' is missing or cannot be mapped")
    /**
     * proj4j 로 재투영 불가한 CRS 일 때. 화이트리스트 외 CRS / 좌표축 해석 실패 /
     * proj4j 변환 예외 세 가지 모두 이 예외로 통합 (L1 — v3 에서 UnsupportedCoordinateSystem 제거).
     */
    class UnsupportedProjection(srcCrs: String, cause: Throwable? = null):
        NetCdfException("Unsupported projection: '$srcCrs' (cannot reproject to EPSG:4326)", cause)
    class ImportAlreadyRunning(fileId: Long, variableName: String):
        NetCdfException("Import already running: fileId=$fileId var=$variableName")
}
```

**변경 (L1)**: 기존 `UnsupportedCoordinateSystem` 과 `UnsupportedProjection` 역할 중복으로 전자 제거. CRS 관련 실패는 `UnsupportedProjection` 으로 통일.

### 5.2 Import 상태 enum · 모델

```kotlin
enum class NetCdfImportStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }

data class NetCdfImportProgress(
    val id: Long = 0L,
    val fileId: Long,
    val variableName: String,
    val status: NetCdfImportStatus = NetCdfImportStatus.PENDING,
    val lastSliceIdx: Long? = null,
    val leaseExpiresAt: Instant? = null,
    val errorMessage: String? = null,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
    val updatedAt: Instant = Instant.now(),
): Serializable {
    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}
```

### 5.3 `NetCdfImportProgressRepository` (재설계)

```kotlin
class NetCdfImportProgressRepository: LongJdbcRepository<NetCdfImportProgress> {
    override val table = NetCdfImportProgressTable
    override fun extractId(entity: NetCdfImportProgress): Long = entity.id
    override fun ResultRow.toEntity(): NetCdfImportProgress = ...

    /**
     * 원자적 lease 획득. 구현: raw INSERT ... ON CONFLICT ... DO UPDATE ... WHERE ... RETURNING.
     * - COMPLETED row → 그대로 반환 (호출자가 no-op 판단)
     * - IN_PROGRESS + lease 유효 → [NetCdfException.ImportAlreadyRunning]
     * - 그 외 (PENDING, FAILED, IN_PROGRESS with expired lease) → lease 재획득 성공
     */
    fun acquireLease(fileId: Long, variableName: String, ttl: Duration): NetCdfImportProgress

    /** 슬라이스 commit tx 내부 호출 — lastSliceIdx 갱신 + lease 연장. */
    fun renewLease(progressId: Long, lastSliceIdx: Long, ttl: Duration)

    /** 정상 완료. */
    fun markCompleted(progressId: Long)

    /** 실패. */
    fun markFailed(progressId: Long, errorMessage: String)

    /** 조회 전용. */
    fun findByFileAndVariable(fileId: Long, variableName: String): NetCdfImportProgress?
}
```

### 5.4 `NetCdfCatalogService` 생성자

```kotlin
class NetCdfCatalogService(
    private val fileRepo: NetCdfFileRepository,
    private val progressRepo: NetCdfImportProgressRepository,
    private val meterRegistry: MeterRegistry? = null,
) {
    companion object: KLogging() {
        val LEVEL_AXIS_NAME_FALLBACKS = listOf("level", "lev", "plev", "pressure", "depth", "z", "height")
        val LAT_AXIS_NAME_FALLBACKS   = listOf("lat", "latitude", "nlat", "y")
        val LON_AXIS_NAME_FALLBACKS   = listOf("lon", "longitude", "x")
        val TIME_AXIS_NAME_FALLBACKS  = listOf("time", "t")
        val LEASE_TTL = Duration.ofMinutes(5)
        const val HEARTBEAT_EVERY_SLICES = 10
    }
}
```

### 5.5 `registerFile` 의사코드

> **L2 주석**: 현재 API 는 blocking — `CancellationException` 발생 경로 없음. 향후 `suspend` 변환 시 `runCatching` 대신 `try/catch` + `if (e is CancellationException) throw e` rethrow 로 교체 필요 (coding-style 규칙 준수).

```kotlin
fun registerFile(filePath: String): Long {
    filePath.requireNotBlank("filePath")

    val sample = meterRegistry?.let { Timer.start(it) }
    var success = false
    try {
        val record = runCatching {
            NetcdfFiles.open(filePath).use { nc ->
                NetCdfFileRecord(
                    filename = Paths.get(filePath).fileName.toString(),
                    filePath = filePath,
                    fileSize = Files.size(Paths.get(filePath)),
                    variables = nc.variables.map { v -> /* NetCdfVariableInfo */ },
                    dimensions = nc.dimensions.associate { it.name to it.length },
                    globalAttrs = nc.globalAttributes.associate { /* k to v */ },
                )
            }
        }.getOrElse { throw NetCdfException.FileOpen(filePath, it) }

        val id = transaction { fileRepo.save(record).id }
        success = true
        return id
    } finally {
        sample?.stop(meterRegistry!!.timer("netcdf.register.duration",
            "status", if (success) "success" else "failure"))
    }
}
```

### 5.6 `importGridValues` 의사코드 (v3 통합)

```kotlin
fun importGridValues(fileId: Long, variableName: String) {
    variableName.requireNotBlank("variableName")

    val record = transaction { fileRepo.findByIdOrNull(fileId) }
        ?: throw NetCdfException.FileRecordNotFound(fileId)

    // 1) Lease 획득 + resume 판단 (ImportAlreadyRunning 은 여기서 throw; progress 변경 없음)
    val progress = transaction { progressRepo.acquireLease(fileId, variableName, LEASE_TTL) }
    if (progress.status == NetCdfImportStatus.COMPLETED) {
        log.info { "already completed: fileId=$fileId var=$variableName" }
        return
    }
    if (progress.lastSliceIdx != null) {
        meterRegistry?.counter("netcdf.import.status", "status", "resumed")?.increment()
        log.info { "resuming: fileId=$fileId var=$variableName from sliceIdx=${progress.lastSliceIdx + 1}" }
    }
    val startSliceIdx: Long = (progress.lastSliceIdx ?: -1L) + 1L

    val dataset = runCatching { NetcdfDatasets.openDataset(record.filePath) }
        .getOrElse {
            transaction { progressRepo.markFailed(progress.id, it.message.orEmpty()) }
            meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
            throw NetCdfException.FileOpen(record.filePath, it)
        }

    dataset.use { ncd ->
        try {
            val v = ncd.findVariable(variableName)
                ?: throw NetCdfException.VariableNotFound(fileId, variableName)
            if (v.rank !in 1..4) throw NetCdfException.UnsupportedVariable(variableName, v.rank)

            // 2) axis-to-dimension 매핑 (C5)
            val axisMap = buildAxisMap(ncd, v)

            // 3) Reprojector (C4) — 1D 또는 2D pair 중 선택
            val reprojector = CoordinateReprojector.from(ncd, axisMap)

            // 4) 슬라이스 개수 계산
            val timeN = axisMap.timeDim?.let { v.shape[it] } ?: 1
            val levelN = axisMap.levelDim?.let { v.shape[it] } ?: 1
            val totalSlices = if (v.rank == 1) 1L else timeN.toLong() * levelN.toLong()

            // 5) 슬라이스 루프 — startSliceIdx 부터
            var heartbeatCounter = 0
            for (sliceIdx in startSliceIdx until totalSlices) {
                val (timeIdx, levelIdx) = decomposeSliceIdx(sliceIdx, levelN)
                val sliceSample = meterRegistry?.let { Timer.start(it) }

                transaction {
                    val inserted = readAndUpsertSlice(v, axisMap, reprojector, fileId, variableName, timeIdx, levelIdx)
                    progressRepo.renewLease(progress.id, sliceIdx, LEASE_TTL)
                    meterRegistry?.counter("netcdf.import.variable.records",
                        "variable", variableName)?.increment(inserted.toDouble())
                }

                sliceSample?.stop(meterRegistry!!.timer("netcdf.import.slice.duration"))
                heartbeatCounter++
            }

            transaction { progressRepo.markCompleted(progress.id) }
            meterRegistry?.counter("netcdf.import.status", "status", "success")?.increment()
        } catch (e: NetCdfException.ImportAlreadyRunning) {
            // M4 — no progress mutation, no failure counter
            throw e
        } catch (e: Throwable) {
            transaction { progressRepo.markFailed(progress.id, e.message.orEmpty()) }
            meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
            throw e
        }
    }
}

private fun decomposeSliceIdx(sliceIdx: Long, levelN: Int): Pair<Int, Int> {
    val t = (sliceIdx / levelN).toInt()
    val l = (sliceIdx % levelN).toInt()
    return t to l
}

private fun buildAxisMap(ncd: NetcdfDataset, v: Variable): VariableAxisMap {
    // 각 dimension 의 name 을 조회해 AxisType 탐지 → 이름 fallback 순.
    // rank 1: timeDim 권장 (없으면 single-slice 처리)
    // rank 2: latDim + lonDim 필수
    // rank 3: timeDim + latDim + lonDim 필수
    // rank 4: timeDim + levelDim + latDim + lonDim 필수
    // 실패 시 MissingCoordinate
}

private data class VariableAxisMap(
    val timeDim: Int?,
    val levelDim: Int?,
    val latDim: Int?,
    val lonDim: Int?,
)
```

### 5.7 `CoordinateReprojector` — sealed Geographic/Projected (C4)

```kotlin
internal sealed class CoordinateReprojector {

    /** 셀 (latIdx, lonIdx) 의 WGS84 (lon, lat) 반환. */
    abstract fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double>

    /** Geographic CRS — 1D 독립 배열 캐싱. */
    class Geographic(
        private val lonValues: DoubleArray,
        private val latValues: DoubleArray,
        val sourceCrs: String,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int) = lonValues[lonIdx] to latValues[latIdx]
    }

    /** Projected CRS — 2D pair 캐싱 shape [latN*lonN*2]={lon,lat}. */
    class Projected(
        private val projected: DoubleArray,   // flat [latN * lonN * 2]
        private val lonN: Int,
        val sourceCrs: String,
    ): CoordinateReprojector() {
        override fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> {
            val base = (latIdx * lonN + lonIdx) * 2
            return projected[base] to projected[base + 1]
        }
    }

    companion object: KLogging() {
        val SUPPORTED_CRS: Set<String> = buildSet {
            add("EPSG:4326"); add("EPSG:3857")
            (32601..32660).forEach { add("EPSG:$it") }
            (32701..32760).forEach { add("EPSG:$it") }
            add("EPSG:3413"); add("EPSG:3031")
        }

        fun from(ncd: NetcdfDataset, axisMap: VariableAxisMap): CoordinateReprojector {
            val srcCrs = detectSourceCrs(ncd)  // grid_mapping / AxisType 기반
            if (srcCrs !in SUPPORTED_CRS) throw NetCdfException.UnsupportedProjection(srcCrs)

            return if (isGeographic(srcCrs)) {
                val lat = findLatAxis(ncd, axisMap).coordValues.copyOf()
                val lon = findLonAxis(ncd, axisMap).coordValues.copyOf()
                Geographic(lon, lat, srcCrs)
            } else {
                val xAxis = findXAxis(ncd, axisMap)
                val yAxis = findYAxis(ncd, axisMap)
                val projected = reprojectToWgs84Pairs(yAxis, xAxis, srcCrs)
                Projected(projected, xAxis.size, srcCrs)
            }
        }
    }
}
```

---

## 6. 읽기 전략 (chunked section read + sliceIdx)

| rank | slice shape (dim 순서는 `axisMap` 기반) | sliceIdx 범위 | sliceIdx → (timeIdx, levelIdx) |
|------|------------------------------------------|--------------|--------------------------------|
| 1D (time) | `[timeN]` 단일 (루프 내부에서 행별 timeIdx 기록) | `0..0` | `(-, 0)` |
| 2D (lat, lon) | `[latN, lonN]` 단일 | `0..0` | `(0, 0)` |
| 3D (time, lat, lon) | `[1, latN, lonN]` per time | `0..timeN-1` | `(sliceIdx.toInt(), 0)` |
| 4D (time, level, lat, lon) | `[1, 1, latN, lonN]` per (t,l) | `0..timeN*levelN-1` | `(sliceIdx/levelN, sliceIdx%levelN)` |

- 각 슬라이스는 `transaction { upsertRows; progressRepo.renewLease(id, sliceIdx, ttl) }` 한 커밋
- row-major 순서: 4D 에서 `sliceIdx = timeIdx × levelN + levelIdx`
- **dim 순서가 다른 경우** (예: shape=`[lat, lon, time]`) — `axisMap` 으로 `origin[]` / `shape[]` 를 올바른 dim 위치에 배치

---

## 7. 에러 / 경계 상황 처리

| 상황 | 정책 | progress 변경 | `netcdf.import.status` counter |
|------|------|----------------|--------------------------------|
| 파일 열기 실패 | `FileOpen` | → FAILED | `failure` |
| 파일 레코드 없음 | `FileRecordNotFound` | 생성 안 함 | **증가 없음** — progress/lease 획득 전 조기 throw (Codex #5) |
| 변수 없음 | `VariableNotFound` | → FAILED | `failure` |
| rank < 1 or > 4 | `UnsupportedVariable` | → FAILED | `failure` |
| axis 매핑 실패 (lat/lon/level) | `MissingCoordinate` | → FAILED | `failure` |
| 미지원 CRS (화이트리스트 외) | `UnsupportedProjection` | → FAILED | `failure` |
| proj4j 변환 실패 | `UnsupportedProjection` | → FAILED | `failure` |
| 동일 (fileId, var) lease 보유 중 | **`ImportAlreadyRunning`** | **변경 없음** (다른 프로세스 소유) | **증가 없음** (M4) |
| NaN / _FillValue | skip + debug log | — | — (`nan.skipped` counter 만 증가) |
| 슬라이스 tx 실패 | 해당 슬라이스 롤백, 전체는 FAILED | → FAILED | `failure` |
| 이미 COMPLETED 호출 | 즉시 no-op + info 로그 | 변경 없음 | 증가 없음 |
| Lease 만료 후 재호출 | 정상 재개 (lease 재획득) | → IN_PROGRESS | 진입 시 `resumed` 증가 |

---

## 8. bluetape4k 컨벤션 준수 체크리스트

- [x] `requireNotBlank` — `filePath`, `variableName`
- [x] `companion object : KLogging()` — `NetCdfCatalogService`, `CoordinateReprojector`,
      `NetCdfImportProgress`, `NetCdfException` 개별
- [x] `java.io.Serializable` + `serialVersionUID = 1L` — 기존 모델 3개 + `NetCdfImportProgress`
- [x] 예외 sealed class — `NetCdfException` (7종 하위 클래스, L1: `UnsupportedCoordinateSystem` 제거)
- [x] enum — `NetCdfImportStatus`
- [x] `atomicfu` — 사용 불필요 (상태 없는 서비스; progress 는 DB 관리)
- [x] `transaction { }` — Exposed v1 JDBC blocking
- [x] `upsert` (ON CONFLICT DO NOTHING) — 슬라이스 insert 중복 방지 (M2)
- [x] `LongIdTable` — `NetCdfImportProgressTable` (AuditableLongIdTable 아님, M1)
- [x] `uniqueIndex` — `(fileId, variableName)` + grid 부분 인덱스 2종 (M2)
- [x] extension / DSL — `geoPointOf(lon, lat)` 기존 헬퍼 사용
- [x] Micrometer — `MeterRegistry?` 선택 주입 (infra/micrometer 관용)
- [x] Korean KDoc 허용, 공개 API 는 KDoc 필수
- [x] IntelliJ 포맷 + `.editorconfig`, no ktlint
- [x] Companion 상수 (`LEVEL_AXIS_NAME_FALLBACKS`, `SUPPORTED_CRS`, `LEASE_TTL`, `HEARTBEAT_EVERY_SLICES`) — magic literal 제거
- [x] `VariableAxisMap` private data class — rank 가정 제거 (C5)
- [x] `CoordinateReprojector` sealed — Geographic/Projected 분기 (C4)
- [x] **README Architecture → UML(Mermaid) → Features → Examples 순서**
- [x] **`[한국어](./README.ko.md) | English` / `한국어 | [English](./README.md)` 언어 링크**
- [x] `docs/testlogs/2026-04.md` 테스트/버그 이력 기록

---

## 9. 테스트 전략

### 9.1 단위 테스트 (Testcontainers PostgreSQL + PostGIS) — 29종

모든 `.nc` 파일은 `NetcdfFormatWriter` + `@TempDir` 로 동적 생성 (체크인 바이너리 금지).
단, CF-1.x 실제 파일 회귀 검증용 Unidata 공개 샘플 1개 예외.

| # | 테스트 | 검증 |
|---|--------|------|
| 1 | `registerFile returns metadata` | 반환 ID + DB row variables/dimensions/globalAttrs |
| 2 | `registerFile throws FileOpen on missing path` | `NetCdfException.FileOpen` |
| 3 | `registerFile blank path throws IAE` | `IllegalArgumentException` |
| 4 | `registerFile records Micrometer timer` | `netcdf.register.duration{status=success}` count=1 |
| 5 | `importGridValues 1D (time series) — location=null` | row count=timeN, location=null, levelIdx=0 |
| 6 | `importGridValues 2D single slice` | row count=latN*lonN, timeIdx=0 |
| 7 | `importGridValues 3D per time slice` | row count=timeN*latN*lonN |
| 8 | `importGridValues 4D per (time,level) slice` | row count=timeN*levelN*latN*lonN |
| 9 | `importGridValues throws VariableNotFound` | typed 예외 |
| 10 | `importGridValues throws UnsupportedVariable rank=5` | typed 예외 |
| 11 | `importGridValues throws MissingCoordinate (lat)` | lat 축 없는 파일 |
| 12 | `importGridValues throws MissingCoordinate for 4D level` | level 매핑 실패 |
| 13 | `importGridValues level axis fallback by name (lev)` | AxisType 없지만 `lev` 이름 → 성공 |
| 14 | `importGridValues skips NaN cells + counter` | skipped=N, `nan.skipped` counter=N |
| 15 | `importGridValues preserves POINT lon/lat order` | `ST_X=lon`, `ST_Y=lat` |
| 16 | `importGridValues EPSG:4326 (Geographic path — 1D axis)` | 재투영 없이 lat/lon 1D axis 직접 사용 |
| 17 | `importGridValues reprojects from EPSG:3857 Web Mercator (Projected 2D pair)` | 2D pair 경유 재투영, tolerance 1e-6 |
| 17b | `importGridValues reprojects from EPSG:32633 UTM (Projected 2D pair)` | 2D pair 경유 재투영 |
| 18 | `importGridValues throws UnsupportedProjection` | 화이트리스트 외 CRS |
| 19 | `importGridValues resume — 3D failure mid-way then resume` | 첫 호출 sliceIdx=k 에서 중단 → 두번째 호출 sliceIdx=k+1 부터 |
| 20 | `importGridValues resume — 4D sliceIdx linearization` | `sliceIdx = t*levelN + l` 올바른 재개 |
| 21 | `importGridValues no-op on COMPLETED progress row` | 재호출 시 row 증가 없음, counter 증가 없음 |
| 22 | `importGridValues throws ImportAlreadyRunning on concurrent call` | 동시 호출 (`CountDownLatch`), progress 불변, failure counter 증가 없음 (M4) |
| 23 | `importGridValues recovers from expired lease` | 첫 호출 진입 후 `@BeforeEach` 에서 `UPDATE netcdf_import_progress SET lease_expires_at = now() - interval '10 min' WHERE id=...` raw SQL 로 만료 시뮬레이션 → 재호출 → 정상 재개. `Thread.sleep` 금지. |
| 24 | `importGridValues commits per slice independently` | 강제 예외 시 앞선 슬라이스 보존 |
| 25 | `importGridValues irregular lat axis` | 불규칙 `lat=[0.0, 45.5, 89.9]` |
| 26 | `importGridValues non-standard dim order (lat, lon, time) — 3D & 4D` | axisMap 기반 처리 검증 (C5). 3D `[lat,lon,time]` + 4D `[lat,lon,time,level]` 두 케이스 |
| 27 | `CoordinateReprojector caches per file` | 동일 파일 두 번 호출 시 재투영 계산 1회 |
| 28 | `upsert DO NOTHING prevents duplicate — both partial indexes` | 재개 전체 재실행 시 row 수 동일. (a) 3D location=NOT NULL → `uk_netcdf_grid_values_full` (b) 1D location=NULL → `uk_netcdf_grid_values_nulloc` 두 부분 인덱스 모두 검증 (M2). DDL 생성 성공 자체도 `@BeforeAll` 에서 확인 |
| 29 | `Unidata CF-1.x sample sresa1b_ncar_ccsm3 import` | `@Tag("slow-netcdf")` — 실 샘플 회귀 |

합계: **29종** (최소 18종 요구 초과 달성).

### 9.2 슬로우 테스트 기준 (D7)

- `@Tag("slow-netcdf")` 로 분리
- 기준: 단일 테스트 실행 시간 > 5초 **또는** 합성 파일 크기 > 1MB
- Gradle `test` 기본 task 는 태그 제외, nightly CI 에서 활성화

### 9.3 샘플 파일 전략

- `src/test/resources/data/netcdf/`
  - `LICENSE` — Unidata BSD-3-Clause 전문 + 파일별 출처 주석
  - `sresa1b_ncar_ccsm3-example.nc` (~680 KB, CF-1.x)

### 9.3.1 test/resources 필수 파일 (L5)

- `src/test/resources/junit-platform.properties` — **태그 필터는 `build.gradle.kts` 에서만 처리**. 기존 프로젝트 설정 유지, 신규 태그 관련 항목 추가 금지
- `src/test/resources/logback-test.xml` — 기존 유지
- 두 파일 부재 시 `utils/science` 모듈 테스트가 불일관하게 동작하므로 Task 수행 전 존재 여부 확인 필수

### 9.4 Test helper

`src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt`:

```kotlin
fun writeSample(
    path: Path,
    rank: Int,                            // 1/2/3/4
    withLatAxis: Boolean = true,
    withLevelAxisByName: Boolean = false, // AxisType 제거, 이름만 'lev'
    withFillValue: Boolean = false,
    sourceCrs: String = "EPSG:4326",
    nonStandardDimOrder: Boolean = false, // C5 테스트용
): Path
```

---

## 10. 관찰 가능성

### 10.1 로깅 (`io.bluetape4k.logging.KLogging`)

- `registerFile`:
  - `debug`: 파일 열기, 변수/차원 추출 완료
  - `info`: 등록 성공 (`fileId=$id path=$path vars=${record.variables.size}`)
  - `error`: 파일 열기 실패 / DB 예외
- `importGridValues`:
  - `debug`: NaN skip 개별 건
  - `info`: lease 획득 / 재개 시작 (`startSliceIdx=X`)
  - `info`: 슬라이스 커밋 완료 (`fileId=$id var=$variableName sliceIdx=$sliceIdx inserted=$n skipped=$m`)
  - `info`: 완료 요약 (`totalInserted`, `totalSkipped`, `elapsedMs`, `status=COMPLETED`)
  - `warn`: COMPLETED row 재호출 시
  - `warn`: `ImportAlreadyRunning` — 재시도 유도
  - `error`: 실패 시 원인 + progress=FAILED

### 10.2 Micrometer 지표 — 시점 명시 (M3)

§3.7 표 참조.

---

## 11. 의존성 변경

### 11.1 `buildSrc/src/main/kotlin/Libs.kt` diff

```diff
-    // === Science / GIS ===
-    // TODO: UCAR CDM 5.x+는 Maven Central에 없음. Unidata 저장소 재구성으로 5.6.0 아티팩트 미존재.
-    // Phase 4 구현 전 정확한 좌표 확인 필요. 현재 사용 불가.
-    // 대안 후보: edu.ucar:cdm-core (CDM6 모듈식), edu.ucar:netcdfAll:5.5.3 (구 Nexus)
-    const val ucar_netcdf = "edu.ucar:netcdfAll:5.6.0"                 // 미사용 — 저장소 이슈
+    // === Science / GIS ===
+    // UCAR netCDF-Java — Unidata Nexus 저장소는 루트 build.gradle.kts 에 이미 선언됨
+    const val ucar_cdm_core = "edu.ucar:cdm-core:5.9.1"   // https://mvnrepository.com/artifact/edu.ucar/cdm-core
+    const val ucar_netcdf4  = "edu.ucar:netcdf4:5.9.1"    // https://mvnrepository.com/artifact/edu.ucar/netcdf4
```

### 11.2 `utils/science/build.gradle.kts` diff

```diff
     // NetCDF (UCAR — compileOnly)
-    // TODO: edu.ucar:netcdfAll 아티팩트 좌표 확인 필요 (Unidata Maven 저장소 재구성됨)
-    // Phase 4 (NetCDF 구현) 시작 전에 정확한 버전/저장소 확인
-    // compileOnly(Libs.ucar_netcdf)
+    compileOnly(Libs.ucar_cdm_core)
+    compileOnly(Libs.ucar_netcdf4)
+
+    // Micrometer — 선택 주입 (MeterRegistry 인터페이스만 필요)
+    compileOnly(Libs.micrometer_core)
     ...
     testImplementation(project(":bluetape4k-exposed-jackson3"))
+    testImplementation(Libs.ucar_cdm_core)
+    testImplementation(Libs.ucar_netcdf4)
+    testImplementation(Libs.micrometer_core)
     testImplementation(Libs.kotlinx_coroutines_test)
```

**참고**: 저장소 추가 task 없음 (X1 — 루트 선언 재사용). proj4j 는 `compileOnly` → `testImplementation` 로 확장됨 (루트 `configurations` 설정).

### 11.3 런타임 사용자 가이드 (README 챕터)

```kotlin
dependencies {
    implementation("io.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
    implementation("edu.ucar:cdm-core:5.9.1")
    implementation("edu.ucar:netcdf4:5.9.1")                 // HDF5 / NetCDF-4
    implementation("org.locationtech.proj4j:proj4j:${proj4jVersion}")
    implementation("org.locationtech.proj4j:proj4j-epsg:${proj4jVersion}")  // EPSG:32633 UTM 등 투영 해석용
    implementation("io.micrometer:micrometer-core:${micrometerVersion}")    // 선택 — Libs.micrometer_core 와 동일 버전 사용
}
repositories {
    mavenCentral()
    maven("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
}
```

**버전 정책**: bluetape4k `Libs.kt` 가 관리하는 버전 (`Libs.proj4j`, `Libs.micrometer_core`) 과 일치시킬 것. 하드코딩 피하고 `gradle.properties` 또는 BOM 사용 권장.

---

## 12. CI / nightly 설정

### 12.1 `ci.yml` 기본 `test` job

```yaml
- name: Run tests (exclude slow-netcdf)
  run: ./gradlew :bluetape4k-science:test -PexcludeTags=slow-netcdf
```

### 12.2 `nightly-tests.yml` — `slow-netcdf` 활성화

```yaml
- name: Run slow NetCDF regression tests
  run: ./gradlew :bluetape4k-science:test -Pnetcdf.slow=true -PincludeTags=slow-netcdf
```

### 12.3 `utils/science/build.gradle.kts` 태그 필터

```kotlin
tasks.test {
    useJUnitPlatform {
        val exclude = (project.findProperty("excludeTags") as String?)?.split(",") ?: emptyList()
        val include = (project.findProperty("includeTags") as String?)?.split(",") ?: emptyList()
        if (exclude.isNotEmpty()) excludeTags(*exclude.toTypedArray())
        if (include.isNotEmpty()) includeTags(*include.toTypedArray())
    }
}
```

---

## 13. 위험 / 롤백

§2.4 표 참조 (R1~R18). 롤백 계획:

- 구현 실패 시 두 메서드를 `TODO()` 로 되돌림 + 스키마 변경 유지 (nullable 은 하위 호환)
- `NetCdfImportProgressTable` 롤백: `DROP TABLE netcdf_import_progress`
- Unique partial index 롤백: `DROP INDEX uk_netcdf_grid_values_full; DROP INDEX uk_netcdf_grid_values_nulloc;`
- `Libs.kt` 의 `ucar_netcdf` 상수 삭제는 참조 없음 → 영향 없음

---

## 14. 미해결 / 사용자 결정 필요

**모두 확정 (0건).** v1·v2·v3 의 설계 선택이 전부 §3~§12 및 §15 task list 에 반영됨.

---

## 15. 구현 복잡도 태깅 (Step 3 plan preview)

| Task | 복잡도 | 비고 |
|------|--------|------|
| T1. `Libs.kt` — `ucar_cdm_core`, `ucar_netcdf4` 추가, 유령 상수 제거 | low | 단일 파일 |
| T2. `utils/science/build.gradle.kts` — ucar + micrometer compileOnly/test + 태그 필터. **저장소 추가 없음** (X1) | low | build 파일 |
| T3. `NetCdfGridValueTable.location.nullable()` + 부분 unique index raw DDL (M2) | low | 테이블 수정 + `@BeforeAll` raw SQL |
| T3b. `NetCdfImportProgressTable` (일반 `LongIdTable`) + `NetCdfImportStatus` enum + `NetCdfImportProgress` data class (Serializable) | low | §4.3 스키마 |
| T3c. `NetCdfImportProgressRepository` — `acquireLease` (raw INSERT ON CONFLICT WHERE RETURNING), `renewLease`, `markCompleted`, `markFailed`, `findByFileAndVariable` | **high** | Postgres 방언 raw SQL + JDBC ResultSet 수동 매핑 + ImportAlreadyRunning 분기 — 평균 Repository 대비 복잡 (L3) |
| T4a. `NetCdfException` sealed 7종 (`UnsupportedProjection`, `ImportAlreadyRunning` 포함) | low | 파일 1개 |
| T5. `registerFile` 구현 + Micrometer Timer | medium | CDM API + timer |
| T5a. `CoordinateReprojector` sealed (Geographic / Projected 2D pair) + proj4j + 캐싱 + 화이트리스트 상수 (C4) | medium | 2개 서브클래스 |
| T5b. `VariableAxisMap` + `buildAxisMap` — AxisType → 이름 fallback (C5) | medium | axis-dim 매핑 |
| T6. `importGridValues` — rank 1/2/3/4 + resume(sliceIdx) + heartbeat lease + NaN skip + reprojection + Micrometer (C2, C3) | **high** | 핵심 복잡도 |
| T7. `NetCdfSampleWriter` 테스트 헬퍼 (`NetcdfFormatWriter`) — rank/withLatAxis/withLevelAxisByName/withFillValue/sourceCrs/nonStandardDimOrder | medium | 샘플 매트릭스 |
| T8. 단위 테스트 29종 (§9.1) — 재개(선형 cursor)·재투영(Projected 2D)·1D·fallback·동시성·lease 만료·upsert 중복·CF-1.x 샘플 포함 | **high** | Testcontainers PostGIS 기반 |
| T9. `@Tag("slow-netcdf")` 분리 + `ci.yml` / `nightly-tests.yml` 동기화 | medium | CI YAML 2개 |
| T10. `README.md` / `README.ko.md` **NetCdf 챕터 신설/보강** — Architecture → UML Mermaid → Features → Examples 순서, `[한국어](./README.ko.md) \| English` 링크 유지 | low | 두 파일 동기화 |
| T11. `docs/superpowers/index/2026-04.md` 엔트리 + `INDEX.md` 카운트 | low | 기존 관행 |
| T12. `/wiki-update` 스킬 실행 | low | 사후 처리 |
| T13. Unidata 공개 샘플 체크인 (`sresa1b_ncar_ccsm3-example.nc` + `LICENSE`) | low | `src/test/resources/data/netcdf/` |
| T14. 후속 Issue draft — "utils/science README 전체 재작성" (docs only) | low | GH issue 초안 |
| T15. `docs/testlogs/2026-04.md` 테스트 실행 / 버그 수정 이력 기록 | low | testlog |
| T16. 루트 `CLAUDE.md` — Exposed 모듈 표 및 신규 테이블 반영 | low | docs |

**요약**: high 3 (T3c, T6, T8) · medium 5 (T5, T5a, T5b, T7, T9) · low 10 (T1, T2, T3, T3b, T4a, T10~T16).
총 **18개 task** (T1~T16 + 하위분할 T3b·T3c·T4a·T5a·T5b).

---

## 16. 성공 기준 (Definition of Done)

- [ ] `./gradlew :bluetape4k-science:compileKotlin` 성공 (cdm-core 5.9.1 의존성 해결)
- [ ] `./gradlew :bluetape4k-science:test` 기본 — `@Tag("slow-netcdf")` 제외 29종 중 해당분 pass
- [ ] `./gradlew :bluetape4k-science:test -Pnetcdf.slow=true -PincludeTags=slow-netcdf` nightly pass
- [ ] `NetCdfCatalogService.registerFile` / `importGridValues` 실 구현 (TODO 제거)
- [ ] `NetCdfGridValueTable.location` nullable + 부분 unique index 2종 반영
- [ ] `NetCdfImportProgressTable` DDL 반영 (heartbeat lease + `lastSliceIdx`)
- [ ] 기존 `NetCdfTableTest.kt` 의 `NotImplementedError` 테스트 2건 삭제 + 생성자 시그니처 업데이트
- [ ] `detekt` 경고 0
- [ ] `README.md` / `README.ko.md` NetCdf 챕터 동기 업데이트 (Architecture→UML→Features→Examples)
- [ ] 후속 Issue "utils/science README 전체 재작성" 생성
- [ ] `docs/testlogs/2026-04.md` 기록
- [ ] 루트 `CLAUDE.md` 업데이트
- [ ] PR 설명에 테스트 결과, 위험 요약, §11.3 가이드, `ci.yml` / `nightly-tests.yml` 변경 포함
- [ ] CodeRabbit review (`/coderabbit:review`) 통과
