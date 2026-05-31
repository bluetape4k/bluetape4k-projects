# NetCdf 지원 완성 (utils/science) — 구현 Plan v2 (Step 3)

- **일자**: 2026-04-25
- **이슈**: #107 (utils/science — NetCdf 지원 완성)
- **브랜치**: `feat/science-netcdf` (worktree: `.worktrees/feat-science-netcdf`)
- **상위 Spec**: `docs/superpowers/specs/2026-04-25-netcdf-support-design.md` (v3.2)
- **상위 Research**: `docs/superpowers/research/2026-04-25-netcdf-support-research.md`
- **개정 이력**: v1 (초안) → v2 (Critic H1~H3 + M1~M5 + L1~L4 + Missing 반영) → **v2.1 (Codex Plan v2 Critical/High/Medium 5건 + Plan v2.1 Medium 1 + Low 2 반영)**
- **총 Task 수**: **25** (T0 / T1 / T2 / T3 / T3b / T3c / T4a / T5 / T5a / T5b / T6a / T6b / T6c / T7 / T8-pre / T8 / T9 / T10 / T11 / T12 / T13 / T14 / T15 / T16 / T17)
- **복잡도 분포**: **high 3 · medium 7 · low 15** (재집계)
  - high: T3c, T6b, T8
  - medium: T5, T5a, T5b, T6a, T6c, T7, T9
  - low: T0, T1, T2, T3, T3b, T4a, T8-pre, T10, T11, T12, T13, T14, T15, T16, T17

---

## 0. Plan v1 → v2 변경 요약

| 구분 | 항목 | v1 → v2 |
|---|---|---|
| H1 | CI 통합 누락 | `ci.yml:test-utils` (219~292 라인) 과 `nightly-tests.yml` 에 science 모듈 **미등록** 실측 확인 → T9 전면 재작성 (science step/job 신설) |
| H2 | T3c `acquireLease` 시그니처 spec 위반 | v1 "SELECT FOR UPDATE + INSERT RETURNING" 폐기 → spec §3.8 원문 `SELECT (read-only, COMPLETED 분기) → INSERT ... ON CONFLICT ... DO UPDATE ... WHERE ... RETURNING` 채택. 반환 **non-null `NetCdfImportProgress`** 로 통일 (COMPLETED 는 status 로 전달) |
| H3 | T8 테스트 수/번호 불일치 | spec §9.1 은 실제 **30종** (#1~#29 + #17b). T8 을 spec 번호 1:1 로 재작성. DoD "기본 29건 pass + slow-netcdf tag 1건 pass (#29)" |
| M1 | Unidata 샘플 라이선스 | T13 — BSD-3-Clause 가정 금지. Unidata 페이지 라이선스 확인 → 원문 체크인 또는 `ATTRIBUTION.md` (출처 URL + 접근 일자) |
| M2 | proj4j-epsg 누락 방지 | T10 — §11.3 dependency block 전체 (cdm-core / netcdf4 / proj4j / proj4j-epsg / micrometer-core + unidata-all repo) 원문 복사 |
| M3 | T3 DDL 인덱스 이름/컬럼 | spec §4.1 명칭 `uk_netcdf_grid_values_full` / `uk_netcdf_grid_values_nulloc` + `MD5(ST_AsBinary(location))` 해시로 통일 |
| M4 | T6 upsert 전략 | `upsert` / `batchInsert` **금지**. raw `INSERT ... ON CONFLICT DO NOTHING` prepared statement batch 또는 `insertIgnore { }` 중 실측 후 택1 |
| M5 | PR Task 누락 | **신규 T17** (PR 생성 + CodeRabbit review) 추가 |
| L1 | T6 분할 | T6 → **T6a** (rank 1/2 · medium) + **T6b** (rank 3/4 + sliceIdx 선형화 · high) + **T6c** (heartbeat + NaN + Micrometer + 예외 래퍼 · medium). 원본 T6 id 제거 |
| L2 | testlog 포맷 | T15 — "29 passing / 1 slow-tagged / 0 failing" 포맷 |
| L3 | T16 CLAUDE.md | "NetCdf 3개 테이블 + 일반 LongIdTable 사용 이유 (user context 불필요)" 한 줄 |
| L4 | property 파싱 | T2 — `.split(",").map { it.trim() }.filter { it.isNotBlank() }` 명시 |
| Missing | T0 신규 | Repo 사전 검증 (`geoPointOf`, `Libs.proj4j_epsg`, `NetCdfFileRecord`, `NetcdfDataset.enhance(...)` 등) |
| Missing | T8-pre 신규 | `NetCdfTableTest.kt` blast radius 분리 (생성자 갱신 + NotImplementedError 2건 삭제 + SchemaUtils 3테이블 + `@BeforeAll` raw DDL) |
| Missing | enhance() | T6c — `NetcdfDatasets.openDataset()` → `.enhance(NetcdfDataset.getDefaultEnhanceMode())` 명시 |
| Missing | `_FillValue` | T6c — `v.findAttribute("_FillValue")?.numericValue` 를 NaN 과 동일 skip, `netcdf.import.nan.skipped` counter 공유 |
| Missing | Path validation | T5 — null-byte / relative 검증은 호출자 책임 KDoc만 (Spec R11) |

---

## 1. Dependency Graph (v2)

```
T0 (Repo 사전 검증) ── 모든 Task 선행 ──▶

Phase 1 (T0 후 병렬)
 ├─ T1   Libs.kt 상수                                    [low]
 ├─ T4a  NetCdfException sealed 7종                      [low]
 └─ T14  후속 Issue draft                                [low, 독립]

Phase 2 (T1 후)
 ├─ T2   build.gradle.kts 의존성 활성화                  [low,    needs T1]
 ├─ T3   NetCdfGridValueTable nullable + partial unique  [low,    needs T2]
 └─ T3b  NetCdfImportProgressTable + enum + model        [low,    needs T2]

Phase 3 (스키마/모델 후 병렬)
 ├─ T3c  NetCdfImportProgressRepository                  [high,   needs T3b+T4a]
 ├─ T5b  VariableAxisMap + buildAxisMap                  [medium, needs T2]
 ├─ T5a  CoordinateReprojector sealed                    [medium, needs T2+T4a+T5b]
 └─ T7   NetCdfSampleWriter 테스트 헬퍼                  [medium, needs T2]

Phase 4 (서비스 구현)
 ├─ T5   registerFile + Micrometer Timer                 [medium, needs T2+T3c+T4a+T5b]
 ├─ T6a  importRank1/2                                   [medium, needs T5b+T5a]
 ├─ T6b  importRank3/4 + sliceIdx 선형화                 [high,   needs T6a+T5b+T5a]
 └─ T6c  heartbeat + NaN + Micrometer + 예외 래퍼        [medium, needs T6a+T6b+T3c+T4a]

Phase 5 (검증)
 ├─ T8-pre  NetCdfTableTest.kt blast radius              [low,    needs T3+T3b+T5+T6c]
 ├─ T13     Unidata 샘플 + LICENSE/ATTRIBUTION           [low,    needs T7]   ← Codex Plan v2 High#2: T8 #29 의 선행
 ├─ T8      단위 테스트 30종                             [high,   needs T8-pre+T7+T13]
 └─ T9      CI science 등록 + nightly job                [medium, needs T8]

Phase 6 (문서/마무리)
 ├─ T10  README NetCdf 챕터                              [low,    needs T5+T6c]
 ├─ T16  루트 CLAUDE.md 갱신                             [low,    needs T3+T3b]
 ├─ T15  docs/testlogs/2026-04.md                        [low,    needs T8+T9]
 ├─ T11  superpowers INDEX 월별 파일                     [low,    needs T10+T15]
 ├─ T12  /wiki-update                                    [low,    needs T11]
 └─ T17  PR 생성 + CodeRabbit review                     [low,    needs T8~T16 전부]
```

**핵심**: T0 → 모든 것 선행 · T17 → 최종

---

## Phase 0 — 사전 검증

### T0. Repo 현재 상태 검증 — `low`

- **선행 조건**: 없음 (최선행)
- **구체 작업**: 다음 항목을 `ide_find_definition` / `ide_find_class` / `ide_search_text` / `rg` 로 조사
  1. `geoPointOf(lon, lat)` 헬퍼 — `data/exposed-postgresql` 혹은 `utils/geo` 위치
  2. `Libs.proj4j` / `Libs.proj4j_epsg` 상수 — 누락 시 T1 에 추가
  3. `NetCdfFileRecord`, `NetCdfVariableInfo` 도메인 모델 (Spec §5.5) — 부재 시 `model` 패키지에 신규 (T5 에 편입)
  4. `NetCdfFileRepository` 시그니처 (`findByIdOrNull`, `save`) — Spec §5.5 와 일치 여부
  5. `NetcdfDatasets.openDataset()` / `NetcdfDataset.enhance(...)` / `getDefaultEnhanceMode()` — cdm-core 5.9.1 API (T2 컴파일 후 재확인)
  6. `ci.yml:test-utils` (236~275 라인), `nightly-tests.yml` 구조 — **v2 실측: 둘 다 science 미등록 확정**
  7. `NetCdfTableTest.kt:30/137/146` 실제 내용 (Spec §4.4)
  8. `utils/science/src/test/resources/junit-platform.properties`, `logback-test.xml` 존재 (Memory feedback_test_resources_required)
  9. **Codex Plan v2 Medium#5 + v2.1 Medium#1** — Exposed v1 `insertIgnore {}` 가 실제로 conflict target 없는 `INSERT ... ON CONFLICT DO NOTHING` SQL 을 생성하는지 확인. 방법: **Testcontainers PostgreSQL** (`bluetape4k-testcontainers` `PostgreSQLServer.Launcher` 또는 `postgis/postgis:16-3.4` 이미지) 환경에서 임시 테이블 생성 + `addLogger(StdOutSqlLogger)` 로 실제 발행 SQL 출력 + DO NOTHING 동작 검증. **H2 in-memory 금지** — Postgres partial expression unique index 와 ON CONFLICT 동작이 dialect 별로 다를 수 있음. 검증 실패 시 T6a/T6c 는 raw SQL 경로만 채택.
- 결과는 본 plan §21 "사전 검증 결과 로그" 에 기록
- **검증 방법**: 항목별 path/결과 명시 · 누락 symbol 은 관련 Task 에 blocking 플래그
- **완료 기준 (DoD)**: 9개 항목 조사 완료 + 누락 symbol 담당 Task 배정 + `insertIgnore` SQL 출력 검증 결과 기록

---

## Phase 1 — 기반 (T0 후 병렬)

### T1. Libs.kt 상수 추가/유령 제거 — `low`

- **선행 조건**: T0
- **구체 작업**:
  - 파일: `buildSrc/src/main/kotlin/Libs.kt`
  - 1222~1226 라인 `ucar_netcdf = "edu.ucar:netcdfAll:5.6.0"` (유령) **제거**
  - 신규 상수:
    - `const val ucar_cdm_core = "edu.ucar:cdm-core:5.9.1"`
    - `const val ucar_netcdf4 = "edu.ucar:netcdf4:5.9.1"`
  - T0 결과 기준 `Libs.proj4j_epsg` 누락 시 추가 (`"org.locationtech.proj4j:proj4j-epsg:<Libs.proj4j 와 동일 버전>"`)
- **검증 방법**:
  - `./gradlew buildSrc:compileKotlin`
  - `rg "ucar_cdm_core|ucar_netcdf4|proj4j_epsg" buildSrc/`
  - `rg "netcdfAll" buildSrc/` → 0건
- **완료 기준 (DoD)**: buildSrc 컴파일 + 상수 존재

### T4a. NetCdfException sealed 7종 — `low`

- **선행 조건**: T0
- **구체 작업**:
  - 신규 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt`
  - Spec §5.1 원문: `FileOpen(path, cause)` / `FileRecordNotFound(fileId)` / `VariableNotFound(fileId, variableName)` / `UnsupportedVariable(variableName, rank: Int)` / `MissingCoordinate(axisName: String)` / `UnsupportedProjection(srcCrs, cause? = null)` / `ImportAlreadyRunning(fileId, variableName)`
  - 각 서브클래스 한국어 KDoc
- **검증 방법**: `./gradlew :bluetape4k-science:compileKotlin`
- **완료 기준 (DoD)**: 7개 `RuntimeException` 서브클래스

### T14. 후속 Issue draft — `low` (독립)

- **선행 조건**: T0
- **구체 작업**:
  - 신규 파일: `.worktrees/feat-science-netcdf/docs/followup-issue-readme-rewrite-draft.md`
  - 제목 "utils/science README 전체 재작성", 머지 후 생성 예정, 범위, 관련 PR 자리
- **검증 방법**: 파일 존재
- **완료 기준 (DoD)**: draft 존재

---

## Phase 2 — 스키마 & 빌드

### T2. utils/science/build.gradle.kts 의존성 활성화 — `low`

- **선행 조건**: T1
- **구체 작업**:
  - 파일: `utils/science/build.gradle.kts`
  - 의존성: `compileOnly(Libs.ucar_cdm_core)` / `compileOnly(Libs.ucar_netcdf4)` / `compileOnly(Libs.micrometer_core)` + testImplementation 대응. T0 결과에 따라 `compileOnly(Libs.proj4j_epsg)` 포함
  - 저장소 추가 **없음** (Spec X1 — 루트 이미 선언됨)
  - 태그 필터 (L4 + Codex Plan v2 Critical#1 — include/exclude 충돌 방지):
    ```kotlin
    tasks.test {
      useJUnitPlatform {
        val include = (project.findProperty("includeTags") as String?)
          ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val excludeProp = (project.findProperty("excludeTags") as String?)
          ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
        // include 가 명시되면 default exclude 적용 안 함 (충돌 방지)
        // excludeTags 가 명시되면 그 값 사용, 명시 없고 include 도 없으면 slow-netcdf 기본 제외
        val exclude = when {
          excludeProp != null -> excludeProp
          include.isNotEmpty() -> emptyList()
          else -> listOf("slow-netcdf")
        }
        include.forEach { includeTags(it) }
        exclude.forEach { excludeTags(it) }
      }
    }
    ```
- **검증 방법**:
  - `./gradlew :bluetape4k-science:dependencies --configuration compileClasspath` → 3개 해결
  - 기본 test → slow-netcdf 제외, `-PincludeTags=slow-netcdf` → 해당 태그만 실행
- **완료 기준 (DoD)**: classpath 해결 + 양방향 태그 필터 동작

### T3. NetCdfGridValueTable.location nullable + partial unique DDL — `low`

- **선행 조건**: T2
- **구체 작업** (M3 수정):
  - 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTables.kt`
  - 기존 `val location = geoPoint("location")` → `.nullable()`
  - DDL 상수 (spec §4.1 원문 · 이름/컬럼 1:1):
    ```kotlin
    const val DDL_UK_NETCDF_GRID_VALUES_FULL = """
      CREATE UNIQUE INDEX IF NOT EXISTS uk_netcdf_grid_values_full
        ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx, MD5(ST_AsBinary(location)))
        WHERE location IS NOT NULL
    """
    const val DDL_UK_NETCDF_GRID_VALUES_NULLOC = """
      CREATE UNIQUE INDEX IF NOT EXISTS uk_netcdf_grid_values_nulloc
        ON netcdf_grid_values (file_id, variable_name, time_idx, level_idx)
        WHERE location IS NULL
    """
    ```
  - Exposed DSL 로 partial+expression index 생성 불가 → 테스트 `@BeforeAll` / 운영 마이그레이션에서 raw `exec` 로 수행
  - KDoc: PostGIS geometry B-tree 미지원 우회 근거 인용
- **검증 방법**:
  - `./gradlew :bluetape4k-science:compileKotlin`
  - Testcontainers PostGIS 에서 `SchemaUtils.create(NetCdfGridValueTable)` + 2 DDL 성공
  - `\d netcdf_grid_values` 출력에 `location NULL 허용` + 두 unique index 존재
- **완료 기준 (DoD)**: location nullable · 인덱스 이름이 spec §4.1 과 1:1 일치

### T3b. NetCdfImportProgressTable + enum + data class — `low`

- **선행 조건**: T2
- **구체 작업** (Spec §4.3, §5.2):
  - 파일 1 — `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/schema/NetCdfImportProgressTable.kt`:
    - `object NetCdfImportProgressTable : LongIdTable("netcdf_import_progress")` (일반 LongIdTable, M1)
    - 컬럼: `fileId` ref, `variableName` varchar(255), `status` `enumerationByName("status", 20, NetCdfImportStatus::class)`, `lastSliceIdx` long nullable, `leaseExpiresAt` timestamp nullable, `errorMessage` text nullable, `startedAt` timestamp, `completedAt` timestamp nullable, `updatedAt` timestamp
    - `init { uniqueIndex("ux_netcdf_import_progress_file_var", fileId, variableName) }`
  - 파일 2 — `model/NetCdfImportStatus.kt`: `enum class NetCdfImportStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED }`
  - 파일 3 — `model/NetCdfImportProgress.kt`: Spec §5.2 data class (Serializable + companion KLogging + `serialVersionUID = 1L`)
- **검증 방법**:
  - `./gradlew :bluetape4k-science:compileKotlin`
  - `SchemaUtils.create(NetCdfImportProgressTable)` 성공
- **완료 기준 (DoD)**: 3 파일 + bluetape4k data class 컨벤션 준수

---

## Phase 3 — Repository / 서비스 헬퍼 (병렬)

### T3c. NetCdfImportProgressRepository — `high`

- **선행 조건**: T3b + T4a
- **구체 작업** (H2 수정 — Spec §3.8 SQL + §5.3 시그니처):
  - 신규 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/repository/NetCdfImportProgressRepository.kt`
  - T0 결과 기준 기존 JDBC repository base (`LongJdbcRepository<NetCdfImportProgress>`) 따름
    - `override val table = NetCdfImportProgressTable`
    - `override fun extractId(entity) = entity.id`
    - `override fun ResultRow.toEntity(): NetCdfImportProgress = ...`
  - `companion object : KLogging()`
  - 메서드 시그니처 (Spec §5.3 그대로 — **반환 non-null**):
    1. `fun findByFileAndVariable(fileId: Long, variableName: String): NetCdfImportProgress?`
    2. `fun acquireLease(fileId: Long, variableName: String, ttl: Duration): NetCdfImportProgress` — **non-null**
    3. `fun renewLease(progressId: Long, lastSliceIdx: Long, ttl: Duration)`
    4. `fun markCompleted(progressId: Long)`
    5. `fun markFailed(progressId: Long, errorMessage: String)` — 길이 1024 clamp
  - `acquireLease` — **spec §3.8 2단계 SQL 원문 그대로**:
    - **1단계 (read-only SELECT, COMPLETED 분기)** — `TransactionManager.current().connection.prepareStatement(...)`:
      ```sql
      SELECT id, status, last_slice_idx, lease_expires_at
        FROM netcdf_import_progress
        WHERE file_id = ? AND variable_name = ?
      ```
      - row 존재 & `status = 'COMPLETED'` → `toEntity()` 매핑 후 **그대로 반환** (호출자가 `status` 로 no-op 판단; spec §5.3 KDoc 계약)
    - **2단계 (조건부 upsert RETURNING)**:
      ```sql
      INSERT INTO netcdf_import_progress
        (file_id, variable_name, status, last_slice_idx, lease_expires_at, started_at, updated_at)
      VALUES (?, ?, 'IN_PROGRESS', NULL, ?, ?, ?)
      ON CONFLICT (file_id, variable_name)
      DO UPDATE SET
        status = 'IN_PROGRESS',
        lease_expires_at = EXCLUDED.lease_expires_at,
        started_at = EXCLUDED.started_at,
        updated_at = EXCLUDED.updated_at,
        error_message = NULL
      WHERE
        netcdf_import_progress.status IN ('PENDING', 'FAILED')
        OR (netcdf_import_progress.status = 'IN_PROGRESS'
            AND netcdf_import_progress.lease_expires_at < EXCLUDED.started_at)
      RETURNING id, status, last_slice_idx, lease_expires_at, error_message,
                started_at, completed_at, updated_at, file_id, variable_name
      ```
    - RETURNING row 존재 → `NetCdfImportProgress` 매핑 반환
    - RETURNING 0 row (= WHERE false → 유효 lease 존재) → `throw NetCdfException.ImportAlreadyRunning(fileId, variableName)`
    - **COMPLETED 는 2단계 WHERE 에서 제외** (spec §3.8 Codex #1) — 1단계 분기로 이미 처리
  - `renewLease`:
    ```sql
    UPDATE netcdf_import_progress
      SET last_slice_idx = ?, lease_expires_at = ?, updated_at = ?
      WHERE id = ?
    ```
  - `markCompleted`:
    ```sql
    UPDATE netcdf_import_progress
      SET status='COMPLETED', completed_at=?, lease_expires_at=NULL, updated_at=?
      WHERE id=?
    ```
  - `markFailed`:
    ```sql
    UPDATE netcdf_import_progress
      SET status='FAILED', lease_expires_at=NULL, error_message=?, updated_at=?
      WHERE id=?
    ```
  - 구현: prepared statement + ResultSet 수동 매핑 · `Timestamp.from(Instant)`
  - KDoc — COMPLETED 반환 시 호출자가 `status` 로 no-op 판단한다는 계약 명시
- **검증 방법**:
  - `./gradlew :bluetape4k-science:compileKotlin`
  - T8 #21 (COMPLETED no-op) / #22 (concurrent) / #23 (expired lease)
- **완료 기준 (DoD)**:
  - `acquireLease` **non-null** 반환 (COMPLETED 포함)
  - 2단계 SQL 이 spec §3.8 과 **문자 단위 일치**
  - RETURNING 0 row → `ImportAlreadyRunning` throw

### T5b. VariableAxisMap + buildAxisMap — `medium`

- **선행 조건**: T2
- **구체 작업**:
  - 신규 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/VariableAxisMap.kt`
  - `internal data class VariableAxisMap(val timeDim: Int?, val levelDim: Int?, val latDim: Int?, val lonDim: Int?)` (spec §5.6)
  - `internal fun buildAxisMap(dataset: NetcdfDataset, variable: Variable): VariableAxisMap`:
    - `variable.dimensions` 순회 → `dataset.findCoordinateAxis(dim.name)`
    - `axis?.axisType` 로 `Time / Lat / Lon / Pressure|Height|GeoZ` 판별
    - axisType null → 이름 fallback (case-insensitive): `LEVEL_AXIS_NAME_FALLBACKS / LAT_AXIS_NAME_FALLBACKS / LON_AXIS_NAME_FALLBACKS / TIME_AXIS_NAME_FALLBACKS` (spec §5.4)
    - rank 별 필수 축 검증: rank 2 = lat+lon, rank 3 = time+lat+lon, rank 4 = time+level+lat+lon — 누락 시 `throw MissingCoordinate(dimName)`
  - internal 헬퍼:
    - `fun VariableAxisMap.buildOrigin(variable: Variable, timeIdx: Int, levelIdx: Int): IntArray`
    - `fun VariableAxisMap.buildShape(variable: Variable): IntArray` — time/level dim=1, lat/lon dim=원본 shape
- **검증 방법**: T8 #11, #12, #13, #26
- **완료 기준 (DoD)**: 임의 dim 순서에서도 origin/shape 정확

### T5a. CoordinateReprojector sealed — `medium`

- **선행 조건**: T2 + T4a + T5b
- **구체 작업** (Spec §3.5, §5.7):
  - 신규 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/CoordinateReprojector.kt`
  - `internal sealed class CoordinateReprojector { abstract fun pointAt(latIdx: Int, lonIdx: Int): Pair<Double, Double> }`
  - 서브클래스:
    - `class Geographic(private val lonValues: DoubleArray, private val latValues: DoubleArray, val sourceCrs: String)` — `pointAt(latIdx, lonIdx) = lonValues[lonIdx] to latValues[latIdx]`
    - `class Projected(private val projectedFlat: DoubleArray, private val lonCount: Int, val sourceCrs: String)` — 2D pair flat: `projectedFlat[(latIdx * lonCount + lonIdx) * 2]` = lon, `[... + 1]` = lat
  - companion:
    - `SUPPORTED_CRS: Set<String>` — `EPSG:4326`, `EPSG:4269`, `EPSG:3857`, `EPSG:3031`, `EPSG:3413`, **`EPSG:32601..32660` (UTM 북반구), `EPSG:32701..32760` (UTM 남반구)** (Codex Plan v2 High#3 — spec §3.5 화이트리스트와 일치)
    - `fun from(ncd, variable, axisMap): CoordinateReprojector`:
      - `detectSourceCrs`: `variable.findAttribute("grid_mapping")` → 참조 변수의 `grid_mapping_name` + EPSG; 없으면 axis type 기반 `EPSG:4326`
      - `isGeographic`: `EPSG:4326` / `EPSG:4269` 만 true
      - Geographic → `CoordinateAxis1D.coordValues` 복사
      - Projected → proj4j `CoordinateTransformFactory` 로 source→`EPSG:4326` 변환자, 모든 (y, x) 쌍 변환 → `DoubleArray(latN * lonN * 2)`
      - `CoordinateAxis2D` (curvilinear) 또는 1D 아닌 lat/lon → `throw MissingCoordinate(...)` (Spec §1.3 비목표)
      - 화이트리스트 외 CRS / proj4j 예외 → `throw UnsupportedProjection(srcCrs, cause)`
  - KDoc — 2D pair 캐싱 근거 (R15, C4)
- **검증 방법**: T8 #16, #17, #17b, #18, #27
- **완료 기준 (DoD)**: Geographic O(1), Projected 파일당 1회 flat 캐싱, 화이트리스트 외 `UnsupportedProjection`

### T7. NetCdfSampleWriter 테스트 헬퍼 — `medium`

- **선행 조건**: T2
- **구체 작업** (Spec §9.4):
  - 신규 파일: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt`
  - ```kotlin
    internal object NetCdfSampleWriter {
      fun writeSample(
        path: java.nio.file.Path,
        rank: Int,
        withLatAxis: Boolean = true,
        withLevelAxisByName: Boolean = false,
        withFillValue: Boolean = false,
        sourceCrs: String = "EPSG:4326",
        nonStandardDimOrder: Boolean = false,
      ): java.nio.file.Path
    }
    ```
  - `ucar.nc2.write.NetcdfFormatWriter.builder().setLocation(path.toString()).setFormat(NetcdfFileFormat.NETCDF4)` 사용
  - 기본 grid: timeN=2, levelN=2, latN=3, lonN=4
  - 축: `lat=[0.0, 45.5, 89.9]`, `lon=[-180.0, -90.0, 0.0, 90.0]`, `time=[0, 3600]`, `level=[1000.0, 500.0]`
  - 플래그 효과:
    - `withLevelAxisByName=true` → level 축 이름 `lev` (axisType 속성 미부여)
    - `withFillValue=true` → `_FillValue=9999.0` + 일부 셀 fill 값
    - `sourceCrs != EPSG:4326` → `grid_mapping` 변수 + `grid_mapping_name` / EPSG 속성
    - `nonStandardDimOrder=true` → `(lat, lon, time, level)` 비표준 순서
- **검증 방법**: 각 플래그 후 `NetcdfFiles.open(path)` 재오픈 smoke + T8 전 케이스
- **완료 기준 (DoD)**: 6 플래그 조합 전부 유효 `.nc` 생성

---

## Phase 4 — 서비스 구현

### T5. registerFile + Micrometer Timer — `medium`

- **선행 조건**: T2 + T3c + T4a + T5b
- **구체 작업** (Spec §5.5 의사코드):
  - 파일: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
  - 생성자 확장:
    ```kotlin
    class NetCdfCatalogService(
      private val fileRepo: NetCdfFileRepository,
      private val progressRepo: NetCdfImportProgressRepository,
      private val meterRegistry: io.micrometer.core.instrument.MeterRegistry? = null,
    )
    ```
  - companion (Spec §5.4): `LEVEL_AXIS_NAME_FALLBACKS / LAT_AXIS_NAME_FALLBACKS / LON_AXIS_NAME_FALLBACKS / TIME_AXIS_NAME_FALLBACKS`, `LEASE_TTL = Duration.ofMinutes(5)`, `HEARTBEAT_EVERY_SLICES = 10`, metric 이름 상수
  - `registerFile(filePath: String): Long`:
    - `filePath.requireNotBlank("filePath")`
    - `Timer.start(meterRegistry)` → finally `sample?.stop(meterRegistry!!.timer("netcdf.register.duration", "status", if (success) "success" else "failure"))`
    - `NetcdfFiles.open(filePath).use { nc -> NetCdfFileRecord(...) }` (variables/dimensions/globalAttrs)
    - `IOException` → `throw NetCdfException.FileOpen(filePath, e)`
    - `transaction { fileRepo.save(record).id }` 반환
  - Path validation: **없음** — 호출자 책임 KDoc 명시 (null byte / relative — Spec R11)
  - 한국어 KDoc
- **검증 방법**: `./gradlew :bluetape4k-science:compileKotlin` + T8 #1~#4
- **완료 기준 (DoD)**: `TODO()` 제거 · timer `meterRegistry != null` 일 때만 · `FileOpen` 타입화

### T6a. importGridValues — rank 1 / rank 2 — `medium`

- **선행 조건**: T5b + T5a
- **구체 작업**:
  - `NetCdfCatalogService.kt` 내부 private:
    - `importRank1(v, axisMap, fileId, variableName): ImportSliceResult` — location=null, levelIdx=0 (Spec §4.2 D3); `t=0..timeN-1` 단일 루프 → insert
    - `importRank2(v, axisMap, reprojector, fileId, variableName): ImportSliceResult` — 단일 슬라이스 (timeIdx=0, levelIdx=0) × (latN × lonN)
  - `ImportSliceResult(inserted: Int, skipped: Int)` 값 클래스
  - 삽입 전략 (Spec §4.1 M4 + Codex Plan v2.1 Low#3): `upsert` / `batchInsert` **금지**. **T0 §21 사전 검증 결과를 본 task 시작 전에 확인하고 단일 전략으로 확정**:
    - T0 검증 결과 `insertIgnore` 가 conflict target 없는 ON CONFLICT DO NOTHING 을 생성하면 → `insertIgnore { }` 채택
    - T0 검증 결과 `insertIgnore` 가 부적합 (target 자동 매칭 실패 / dialect 차이) → raw `INSERT ... ON CONFLICT DO NOTHING` prepared statement batch (`TransactionManager.current().connection`) 채택
    - **두 전략 동시 사용 금지** — 본 plan §21 에 채택 결과 기록
- **검증 방법**: `./gradlew :bluetape4k-science:compileKotlin` + T8 #5, #6, #15, #25
- **완료 기준 (DoD)**: rank 1/2 DB 삽입 정상 · upsert/batchInsert 미사용

### T6b. importGridValues — rank 3 / rank 4 + sliceIdx 선형화 — `high`

- **선행 조건**: T6a + T5b + T5a
- **구체 작업**:
  - `importRank3(...)`: time × (lat × lon), 각 time 슬라이스가 1 tx
  - `importRank4(...)`: time × level × (lat × lon), 각 (time, level) 슬라이스가 1 tx
  - `decomposeSliceIdx(sliceIdx: Long, levelN: Int): Pair<Int, Int>` (Spec §5.6): `val t = (sliceIdx / levelN).toInt(); val l = (sliceIdx % levelN).toInt()`
  - `totalSlices = if (rank == 1) 1L else timeN.toLong() * levelN.toLong()` (rank 3 levelN=1)
  - `origin = axisMap.buildOrigin(v, timeIdx, levelIdx)`, `shape = axisMap.buildShape(v)`
  - `v.read(origin, shape)` → lat/lon 2차원 iteration + `reprojector.pointAt(latIdx, lonIdx)` → POINT 생성 (`geoPointOf(lon, lat)` — T0 결과 기반)
- **검증 방법**: T8 #7, #8, #19, #20, #24, #26
- **완료 기준 (DoD)**: sliceIdx 선형 cursor 로 4D 재개 정확 · 슬라이스별 독립 tx

### T6c. heartbeat + NaN skip + Micrometer + 예외 래퍼 — `medium`

- **선행 조건**: T6a + T6b + T3c + T4a
- **구체 작업** (Spec §5.6, §7, §10.2):
  - Public entry point 재조립:
    ```kotlin
    fun importGridValues(fileId: Long, variableName: String) {
      variableName.requireNotBlank("variableName")
      // Codex Plan v2 Medium#4 — progress/lease 획득 전 실패는 import.status 미기록.
      // FileRecordNotFound 는 lease 미획득 상태이므로 markFailed/counter 호출 없이 raise.
      val record = transaction { fileRepo.findByIdOrNull(fileId) }
        ?: throw NetCdfException.FileRecordNotFound(fileId)

      val progress = transaction { progressRepo.acquireLease(fileId, variableName, LEASE_TTL) }
      if (progress.status == NetCdfImportStatus.COMPLETED) {
        log.info { "already completed: fileId=$fileId var=$variableName" }
        return
      }
      val startSliceIdx: Long = (progress.lastSliceIdx ?: -1L) + 1L
      if (progress.lastSliceIdx != null) {
        meterRegistry?.counter("netcdf.import.status", "status", "resumed")?.increment()
        log.info { "resuming from sliceIdx=$startSliceIdx" }
      }

      val dataset = runCatching { NetcdfDatasets.openDataset(record.filePath) }
        .getOrElse {
          transaction { progressRepo.markFailed(progress.id, it.message.orEmpty()) }
          meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
          throw NetCdfException.FileOpen(record.filePath, it)
        }

      dataset.use { ncd ->
        ncd.enhance(NetcdfDataset.getDefaultEnhanceMode())   // scale/offset/missing 자동 처리 (Spec §2.3)
        try {
          val v = ncd.findVariable(variableName)
            ?: throw NetCdfException.VariableNotFound(fileId, variableName)
          if (v.rank !in 1..4) throw NetCdfException.UnsupportedVariable(variableName, v.rank)
          val axisMap = buildAxisMap(ncd, v)
          val reprojector = if (v.rank >= 2) CoordinateReprojector.from(ncd, v, axisMap) else null

          runImport(v, axisMap, reprojector, progress, fileId, variableName, startSliceIdx)

          transaction { progressRepo.markCompleted(progress.id) }
          meterRegistry?.counter("netcdf.import.status", "status", "success")?.increment()
        } catch (e: NetCdfException.ImportAlreadyRunning) {
          throw e  // no markFailed, no failure counter (Spec §7, M4)
        } catch (e: Throwable) {
          transaction { progressRepo.markFailed(progress.id, e.message.orEmpty()) }
          meterRegistry?.counter("netcdf.import.status", "status", "failure")?.increment()
          throw e
        }
      }
    }
    ```
  - `runImport` 내부 — 슬라이스별 tx:
    - insert (T6a/T6b 위임)
    - **NaN / fillValue skip**:
      - `val fillValue = v.findAttribute("_FillValue")?.numericValue?.toDouble()`
      - `if (value.isNaN() || (fillValue != null && value == fillValue))` → `meterRegistry?.counter("netcdf.import.nan.skipped")?.increment()` + `log.debug` + continue
    - **Heartbeat** (Spec §3.8):
      - `if (sliceIdx == startSliceIdx || sliceIdx % HEARTBEAT_EVERY_SLICES == 0L || (Instant.now() isAfter lastHeartbeat + Duration.ofSeconds(30)))` → `progressRepo.renewLease(progress.id, sliceIdx, LEASE_TTL)` + `lastHeartbeat = Instant.now()`
    - 슬라이스 종료 시에도 `renewLease(progress.id, sliceIdx, LEASE_TTL)` 로 cursor 갱신 (Spec §5.6)
    - `meterRegistry?.counter("netcdf.import.variable.records", "variable", variableName)?.increment(inserted.toDouble())`
    - slice timer: `Timer.start(..).stop(meterRegistry!!.timer("netcdf.import.slice.duration"))`
  - Metric 이름 companion 집약: `netcdf.register.duration`, `netcdf.import.slice.duration`, `netcdf.import.variable.records`, `netcdf.import.nan.skipped`, `netcdf.import.status` (tags: `success|failure|resumed`)
- **검증 방법**:
  - `./gradlew :bluetape4k-science:compileKotlin` + `ide_diagnostics` 경고 0
  - T8 #14, #19, #20, #21, #22, #23, #24, #25, #26, #27
- **완료 기준 (DoD)**:
  - `TODO()` 제거 · heartbeat 10 slices OR 30s · `_FillValue`+NaN 공용 counter
  - `ImportAlreadyRunning` 은 markFailed 없이 re-throw (Spec §7)
  - `.enhance(getDefaultEnhanceMode())` 호출 존재

---

## Phase 5 — 검증

### T8-pre. NetCdfTableTest.kt blast radius 수정 — `low`

- **선행 조건**: T3 + T3b + T5 + T6c
- **구체 작업** (Spec §4.4):
  - 파일: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/schema/NetCdfTableTest.kt`
  - 라인 30: `NetCdfCatalogService(fileRepo)` → `NetCdfCatalogService(fileRepo, progressRepo, meterRegistry = null)`
  - 라인 137 `NotImplementedError 발생 (registerFile)` 테스트 **삭제**
  - 라인 146 `NotImplementedError 발생 (importGridValues)` 테스트 **삭제**
  - `SchemaUtils.create(NetCdfFileTable, NetCdfGridValueTable)` → `SchemaUtils.create(NetCdfFileTable, NetCdfGridValueTable, NetCdfImportProgressTable)`
  - `@BeforeAll` (컨테이너 초기화 직후) raw DDL:
    - `exec(DDL_UK_NETCDF_GRID_VALUES_FULL)`
    - `exec(DDL_UK_NETCDF_GRID_VALUES_NULLOC)`
- **검증 방법**: `./gradlew :bluetape4k-science:test --tests "*NetCdfTableTest*"` pass
- **완료 기준 (DoD)**: 기존 NotImplementedError 2건 제거 · 3 테이블 생성 · 2 DDL 실행

### T8. 단위 테스트 30종 — `high`

- **선행 조건**: T8-pre + T7 + T13
- **구체 작업** (Spec §9.1 30행 1:1):
  - 파일: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt` (신규)
  - 테스트 (spec §9.1 원문 제목 1:1):
    - #1 `registerFile returns metadata`
    - #2 `registerFile throws FileOpen on missing path`
    - #3 `registerFile blank path throws IAE`
    - #4 `registerFile records Micrometer timer` — `netcdf.register.duration{status=success}` count=1
    - #5 `importGridValues 1D (time series) — location=null`
    - #6 `importGridValues 2D single slice`
    - #7 `importGridValues 3D per time slice`
    - #8 `importGridValues 4D per (time,level) slice`
    - #9 `importGridValues throws VariableNotFound`
    - #10 `importGridValues throws UnsupportedVariable rank=5`
    - #11 `importGridValues throws MissingCoordinate (lat)`
    - #12 `importGridValues throws MissingCoordinate for 4D level`
    - #13 `importGridValues level axis fallback by name (lev)`
    - #14 `importGridValues skips NaN cells + counter`
    - #15 `importGridValues preserves POINT lon/lat order`
    - #16 `importGridValues EPSG:4326 (Geographic path — 1D axis)`
    - #17 `importGridValues reprojects from EPSG:3857 Web Mercator (Projected 2D pair)` — tolerance 1e-6
    - **#17b** `importGridValues reprojects from EPSG:32633 UTM (Projected 2D pair)`
    - #18 `importGridValues throws UnsupportedProjection`
    - #19 `importGridValues resume — 3D failure mid-way then resume`
    - #20 `importGridValues resume — 4D sliceIdx linearization`
    - #21 `importGridValues no-op on COMPLETED progress row`
    - #22 `importGridValues throws ImportAlreadyRunning on concurrent call` — `CountDownLatch(2)` + 2 스레드, failure counter 불변 (M4)
    - #23 `importGridValues recovers from expired lease` — `UPDATE netcdf_import_progress SET lease_expires_at = now() - interval '10 min' WHERE id = ?` raw SQL (Thread.sleep 금지)
    - #24 `importGridValues commits per slice independently`
    - #25 `importGridValues irregular lat axis` — `lat=[0.0, 45.5, 89.9]`
    - #26 `importGridValues non-standard dim order (lat, lon, time) — 3D & 4D` (두 케이스)
    - #27 `CoordinateReprojector caches per file` — 두 번 호출 시 변환 1회
    - #28 `upsert DO NOTHING prevents duplicate — both partial indexes` — (a) location NOT NULL → `uk_netcdf_grid_values_full`, (b) location NULL → `uk_netcdf_grid_values_nulloc`, DDL 생성 성공 @BeforeAll 확인
    - **#29** `Unidata CF-1.x sample sresa1b_ncar_ccsm3 import` — `@Tag("slow-netcdf")`
  - MockK `MeterRegistry` mocking → counter.increment() verify
  - 모든 동적 `.nc` 샘플은 `NetCdfSampleWriter` + `@TempDir`
- **검증 방법**:
  - `./gradlew :bluetape4k-science:test` (기본, slow-netcdf 자동 제외) → **29건 pass**
  - `./gradlew :bluetape4k-science:test -PincludeTags=slow-netcdf` → **1건 pass (#29)**
- **완료 기준 (DoD)**: 기본 29/29 pass + nightly 1/1 pass · NotImplementedError 재출현 0건

### T9. CI science 등록 + nightly job — `medium`

- **선행 조건**: T2 + T8 + T0 실측
- **구체 작업** (H1 수정):
  - **실측 사실**: `ci.yml:219-292` `test-utils` job 에 `:bluetape4k-science:test` 미등록; `nightly-tests.yml` 전체에 science 관련 step 0건
  - 수정 1 — `ci.yml` `test-utils` `Test utils modules (pure JVM)` step (현재 라인 236~252) 의 `./gradlew \ ...` 블록에 **`:bluetape4k-science:test -PexcludeTags=slow-netcdf \`** 한 줄 추가 (alphabetical 위치)
  - 수정 2 — 같은 파일 `Generate Kover XML report` step (라인 258~275) 에 **`:bluetape4k-science:koverXmlReport \`** 추가
  - 수정 3 — `nightly-tests.yml` 에 신규 job `science-slow-netcdf`:
    ```yaml
    science-slow-netcdf:
      name: Science / slow-netcdf (nightly)
      runs-on: ubuntu-latest
      timeout-minutes: 30
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            java-version: ${{ env.JAVA_VERSION }}
            distribution: ${{ env.JAVA_DISTRIBUTION }}
        - uses: gradle/actions/setup-gradle@v4
          with:
            gradle-version: wrapper
            cache-read-only: true
        - name: Run slow NetCDF regression tests
          run: ./gradlew :bluetape4k-science:test -Pnetcdf.slow=true -PincludeTags=slow-netcdf
        - name: Upload test results
          if: always()
          uses: actions/upload-artifact@v4
          with:
            name: test-results-science-slow
            path: '**/build/test-results/test/*.xml'
            retention-days: 30
    ```
  - Memory feedback_ci_nightly_sync 준수 — 두 YAML 동시 수정
- **검증 방법**:
  - `gh workflow view ci.yml` / `nightly-tests.yml` 로 YAML 파싱 성공
  - PR 생성 후 Actions 탭 `Test / Utils` 로그에 `:bluetape4k-science:test` 실행
  - nightly 첫 야간 실행 후 `science-slow-netcdf` job pass
- **완료 기준 (DoD)**:
  - `ci.yml:test-utils` gradle test + kover 블록 양쪽에 science 포함
  - `nightly-tests.yml` 에 science job 존재
  - PR Actions 로그에 science job 실행 증거

---

## Phase 6 — 문서/마무리

### T13. Unidata 샘플 + LICENSE/ATTRIBUTION — `low`

- **선행 조건**: T7
- **구체 작업** (M1 수정):
  - 디렉터리: `utils/science/src/test/resources/data/netcdf/`
  - **라이선스 확정 절차**:
    1. <https://www.unidata.ucar.edu/software/netcdf/examples/files.html> 페이지에서 `sresa1b_ncar_ccsm3-example.nc` 의 **라이선스 명시 여부 직접 확인** (WebFetch / 수동 열람)
    2a. 명시 존재 → `LICENSE` 에 원문 그대로 복사 + 파일별 출처 주석 (filename, source URL, access date)
    2b. 명시 부재 → `ATTRIBUTION.md` 작성 (출처 URL + access date + "public sample files used for testing purposes; reuse subject to Unidata's example file policy")
    - **BSD-3-Clause 가정 금지**
  - 파일 체크인: `sresa1b_ncar_ccsm3-example.nc` 를 `src/test/resources/data/netcdf/` 에 추가
  - `junit-platform.properties`, `logback-test.xml` 존재 확인 (T0)
- **검증 방법**:
  - `eza utils/science/src/test/resources/data/netcdf/`
  - T8 #29 pass
- **완료 기준 (DoD)**: 샘플 `.nc` + LICENSE/ATTRIBUTION 실제 출처 기반

### T10. README NetCdf 챕터 — `low`

- **선행 조건**: T5 + T6c
- **구체 작업** (M2 수정):
  - 파일 1: `utils/science/README.md` — `[한국어](./README.ko.md) | English` 링크 유지
  - 파일 2: `utils/science/README.ko.md` — `한국어 | [English](./README.md)` 링크 유지
  - NetCdf 챕터 — 순서 엄수 (Memory feedback_readme_maintenance): **Architecture → UML(Mermaid) → Features → Examples → Dependencies**
  - Architecture: NetCdfCatalogService / NetCdfFileRepository / NetCdfImportProgressRepository / CoordinateReprojector / ucar.nc2 / proj4j / PostgreSQL+PostGIS
  - UML: Mermaid `classDiagram` (Vega-Lite 금지 — Memory feedback_no_vegalite_in_readme)
  - Features: `registerFile`, `importGridValues` rank 1~4, heartbeat lease resume, CRS whitelist, Micrometer 5 지표
  - Examples (Kotlin): register → importGridValues / MeterRegistry 주입 / resume 케이스
  - Dependencies — **Spec §11.3 전체 블록 복사** (M2: proj4j-epsg 포함):
    ```kotlin
    dependencies {
      implementation("io.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
      implementation("edu.ucar:cdm-core:5.9.1")
      implementation("edu.ucar:netcdf4:5.9.1")
      implementation("org.locationtech.proj4j:proj4j:${proj4jVersion}")
      implementation("org.locationtech.proj4j:proj4j-epsg:${proj4jVersion}")
      implementation("io.micrometer:micrometer-core:${micrometerVersion}")
    }
    repositories {
      mavenCentral()
      maven("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
    }
    ```
- **검증 방법**: `glow utils/science/README.md` + Mermaid 렌더링
- **완료 기준 (DoD)**: 두 파일 동기 · 순서 준수 · proj4j-epsg 포함 · Vega-Lite 미사용

### T16. 루트 CLAUDE.md 갱신 — `low`

- **선행 조건**: T3 + T3b
- **구체 작업** (L3 수정):
  - 파일: `CLAUDE.md` (worktree 루트)
  - "Key Design Patterns" 섹션에 한 줄 추가:
    > **NetCdf**: `utils/science` 에 `NetCdfFileTable`, `NetCdfGridValueTable`, `NetCdfImportProgressTable` 3개 테이블 사용. `NetCdfImportProgressTable` 은 **일반 `LongIdTable`** (`AuditableLongIdTable` 아님) — 시스템 임포트 상태라 user context 불필요.
  - "Module Groups" `utils/` 행에 `science` 누락 시 보완 (T0 확인 기반)
- **검증 방법**: `rg "NetCdfImportProgressTable" CLAUDE.md`
- **완료 기준 (DoD)**: 3 테이블 명 + 일반 LongIdTable 사용 이유 반영

### T15. docs/testlogs/2026-04.md 기록 — `low`

- **선행 조건**: T8 + T9
- **구체 작업** (L2 수정):
  - 파일: `docs/testlogs/2026-04.md`
  - **맨 위 행 추가** (Memory feedback_testlog_read_first):
    `| 2026-04-25 | NetCdf Issue #107 구현 | bluetape4k-science | 29 passing / 1 slow-tagged / 0 failing | ✅ | XX min | Testcontainers PostGIS, cdm-core 5.9.1, nightly slow-netcdf job 신설 |`
  - 버그 수정 이력은 별도 행 추가 (Memory feedback_testlog_bugfix_history)
- **검증 방법**: 파일 맨 위 diff
- **완료 기준 (DoD)**: 최상단 행 · "29 passing / 1 slow-tagged / 0 failing" 포맷

### T11. superpowers INDEX 월별 파일 — `low`

- **선행 조건**: T10 + T15
- **구체 작업**:
  - 파일 1: `docs/superpowers/index/2026-04.md` — 맨 위 행 추가 (spec/plan/research 링크 + 한 줄 요약)
  - 파일 2: `docs/superpowers/INDEX.md` — 총 카운트 +1 (월별 섹션 일치)
- **검증 방법**: 두 파일 diff
- **완료 기준 (DoD)**: 카운트 + 엔트리 일치

### T12. /wiki-update — `low`

- **선행 조건**: T11
- **구체 작업**:
  - Skill 호출: `oh-my-claudecode:wiki-update`
  - 대상: spec/plan/research 3건
  - Obsidian `claude-code/` + `~/.claude/wiki/` 양쪽 반영 (Memory feedback_prefer_obsidian_for_wiki)
- **검증 방법**: `gno query "NetCdf Issue #107" -c wiki`
- **완료 기준 (DoD)**: wiki 페이지 신설/갱신 + GNO 재인덱싱

### T17. PR 생성 + CodeRabbit review — `low`

- **선행 조건**: T8 ~ T16 전부
- **구체 작업** (M5 신규):
  1. 사전 점검: `./bin/repo-status` / `./gradlew :bluetape4k-science:build` 그린
  2. PR 생성: `gh pr create --base develop --head feat/science-netcdf --title "feat(science): NetCdf Issue #107 완성" --body-file <description>`
  3. PR description (한국어):
     - 배경 / 변경 범위 / Spec §16 DoD 체크리스트 13개
     - 테스트 결과: `./gradlew :bluetape4k-science:test` (기본 29/29), `-PincludeTags=slow-netcdf` (1/1)
     - 주요 위험 R1~R18 요약 + 완화책
     - `ci.yml` / `nightly-tests.yml` 변경 내용
     - §11.3 런타임 가이드 인용
  4. CodeRabbit review: `Skill(skill="oh-my-claudecode:coderabbit:review")` 또는 `/coderabbit:review`
  5. 피드백 반영 → 재푸시 · merge-ready 유지
- **검증 방법**: `gh pr view --json reviews,checks` 로 CI/리뷰 상태; Actions 탭 `Test / Utils` + `Science / slow-netcdf (nightly)` pass
- **완료 기준 (DoD)**: PR 생성 · CI 기본 그린 · nightly 예약 · CodeRabbit 승인 · description 에 DoD/테스트/위험 전부 포함

---

## §18. 병렬 실행 전략

- **Phase 1**: T1 / T4a / T14 병렬
- **Phase 3**: T3c / T5a / T5b / T7 병렬 (T3b·T4a·T2 완료 전제)
- **Phase 4**: T6a → T6b → T6c 순차, T5 와 T6* 병렬
- **Phase 6 문서**: T10 / T15 / T16 병렬

`superpowers:subagent-driven-development` / `oh-my-claudecode:ultrawork` 활용.

---

## §19. 최종 검증 게이트 (T17 직전)

- [ ] `./gradlew :bluetape4k-science:compileKotlin` 성공
- [ ] `./gradlew :bluetape4k-science:test` (기본) 29건 통과
- [ ] `./gradlew :bluetape4k-science:test -PincludeTags=slow-netcdf` 1건 통과
- [ ] `./gradlew :bluetape4k-science:detekt` 경고 0
- [ ] `README.md` / `README.ko.md` 동기 (Architecture→UML→Features→Examples→Dependencies, proj4j-epsg 포함)
- [ ] `docs/testlogs/2026-04.md` "29 passing / 1 slow-tagged / 0 failing" 기록
- [ ] `docs/superpowers/index/2026-04.md` + `INDEX.md` 갱신
- [ ] 루트 `CLAUDE.md` 3 테이블 + 일반 LongIdTable 이유 반영
- [ ] 후속 Issue draft 파일 존재
- [ ] `ci.yml:test-utils` 에 `:bluetape4k-science:test` + `:bluetape4k-science:koverXmlReport` 포함
- [ ] `nightly-tests.yml` 에 `science-slow-netcdf` job 존재
- [ ] `/wiki-update` 반영 완료

---

## §20. Spec §16 DoD 매핑 (v2)

| DoD 항목 | 담당 Task |
|---|---|
| `compileKotlin` 성공 | T1, T2, T5, T6a, T6b, T6c |
| 기본 `test` 통과 (29건) | T8, T9 |
| nightly `includeTags=slow-netcdf` 통과 (1건) | T8 #29, T9, T13 |
| `registerFile` / `importGridValues` 실 구현 (TODO 제거) | T5, T6a, T6b, T6c |
| `location` nullable + partial unique 2종 | T3 |
| `NetCdfImportProgressTable` DDL | T3b |
| `NotImplementedError` 테스트 2건 삭제 + 생성자 갱신 | T8-pre |
| `detekt` 경고 0 | 전 Task |
| README 동기 (Arch→UML→Features→Examples) | T10 |
| 후속 Issue 생성 | T14 |
| `docs/testlogs/2026-04.md` | T15 |
| 루트 `CLAUDE.md` | T16 |
| PR 설명 / CodeRabbit | T17 |

---

## §21. 사전 검증 결과 로그 (T0 실행 시 채움)

| 항목 | 결과 | 비고 |
|---|---|---|
| `geoPointOf(lon, lat)` 위치 | _(T0 실행 시 기록)_ | |
| `Libs.proj4j` / `Libs.proj4j_epsg` | _(T0 실행 시 기록)_ | |
| `NetCdfFileRecord` / `NetCdfVariableInfo` | _(T0 실행 시 기록)_ | |
| `NetCdfFileRepository` 시그니처 | _(T0 실행 시 기록)_ | |
| `NetcdfDataset.enhance(...)` 유효 | _(T0 실행 시 기록)_ | |
| `ci.yml:test-utils` 현황 | **science 미등록** (236~275 라인) — T9 작업 필요 | v2 실측 확인 |
| `nightly-tests.yml` science 현황 | **science 관련 job 0건** — T9 신규 job 필요 | v2 실측 확인 |
| `NetCdfTableTest.kt:30/137/146` | spec §4.4 인용 기준 — T8-pre 재확인 | |
| `junit-platform.properties` / `logback-test.xml` | _(T0 실행 시 기록)_ | |

---

## §22. 참고

- Spec v3.2: [`specs/2026-04-25-netcdf-support-design.md`](../specs/2026-04-25-netcdf-support-design.md)
- Research: [`research/2026-04-25-netcdf-support-research.md`](../research/2026-04-25-netcdf-support-research.md)
- CDM 5.9.1 userguide: <https://docs.unidata.ucar.edu/netcdf-java/current/userguide/using_netcdf_java_artifacts.html>
- proj4j: <https://github.com/locationtech/proj4j>
- Unidata sample files: <https://www.unidata.ucar.edu/software/netcdf/examples/files.html>
