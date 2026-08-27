# NetCDF `CoordinateAxis2D`·CF auxiliary coordinate 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `bluetape4k-science`의 blocking NetCDF importer가 `CoordinateAxis1D`와 `CoordinateAxis2D`, CF auxiliary numeric coordinate, 비표준 dimension 순서와 지원 CRS를 bounded 메모리·원자적 lease·기존 schema 안에서 정확히 저장하도록 확장한다.

**Architecture:** 공개 `NetCdfCatalogService.registerFile`/`importGridValues` API와 기존 `NetCdfGridValueTable`을 유지한다. 내부 `VariableAxisMap`과 bounded `CoordinateReader`/`CoordinateSampler`가 축과 CF `coordinates`를 해석하고, slice-wide duplicate preflight 후 deterministic tile planner가 data/coordinate를 읽어 동일 Exposed transaction의 JDBC writer로 저장한다. 파일 fingerprint, typed exception, resource budget, progress CAS fence는 각각 작은 internal component로 분리한다.

**Tech Stack:** Kotlin 2.4, JVM 25, Gradle 9.7.0, NetCDF-Java 5.9.1, JetBrains Exposed v1 JDBC, PostgreSQL/PostGIS, Jackson JSONB, Proj4J, Micrometer, JUnit 5, MockK/Kluent-style bluetape4k assertions, Testcontainers.

---

## 0. 실행 전 고정 조건

- 기준: `origin/develop@45260871f58433a78f2d633c235010f661d22c6e`
- worktree: `.worktrees/feat/1352-coordinate-axis2d-cf-grid`
- branch: `feat/1352-coordinate-axis2d-cf-grid`
- 선행 train: merged PR #1512 → Issue #1343 문서 계약
- 승인 설계: `docs/superpowers/specs/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-design.md`
- 변경하지 않을 것: 새 dependency, 새 table/column, schema migration, settings/BOM/catalog 등록, workflow, `CHANGELOG.md`, release note
- heavy Testcontainers 검증은 한 번에 하나의 Gradle invocation으로 순차 실행한다.
- 모든 commit은 Lore intent/trailer 형식을 사용하고, PR/문서/KDoc/commit 메시지는 한국어로 작성한다.

### 0.1 Stacked PR train 순서

Epic #1421의 child train은 다음 순서로 고정한다.

1. **T0 문서 계약** — #1343의 문서 PR #1512가 `develop`에 rebase merge되어
   기준 SHA에 이미 반영되었다.
2. **T1 구현 child** — 현재 branch/worktree의 #1352 구현만 `develop`을 base로 올리고,
   PR body에서 `#1352`와 `#1421`, 선행 PR #1512를 연결한다. 코드·fixture·문서·benchmark는
   이 child의 범위로 제한한다.

T1의 exact head CI·review·metadata를 새로 확인한 뒤 fresh merge approval을 받아야
`gh pr merge --rebase --delete-branch`를 실행한다. merge 후 canonical `develop`은
fast-forward 동기화하고, Epic #1421은 #1343/#1352의 live 상태와 DoD를 다시 읽은 뒤에만
닫는다. auto-merge, squash/merge commit, branch 삭제의 독자적 선택은 사용하지 않는다.

## 1. 파일·소유권 지도

| 책임 | 파일 | 변경 유형 |
|---|---|---|
| 공개 오류와 major migration | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt` | 수정 |
| import hard limit·checked arithmetic·duplicate key | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfImportLimits.kt` | 생성 |
| trusted path·TOCTOU·fingerprint | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuard.kt` | 생성 |
| 축 binding·CF coordinate 탐지 | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/VariableAxisMap.kt` | 수정 |
| bounded variable/coordinate window와 sampler | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSampler.kt` | 생성 |
| whitelist CRS와 tile-local reprojection | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/CoordinateReprojector.kt` | 수정 |
| row/column tile shape와 full-rank index | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTilePlanner.kt` | 생성 |
| numeric auxiliary JSONB·NFC/UTF-8 cap | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfAuxiliarySerializer.kt` | 생성 |
| same-connection JDBC batch writer | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTileBatchWriter.kt` | 생성 |
| rank 1–4 orchestration·metrics·KDoc | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt` | 수정 |
| acquire/renew/assert/fail CAS와 UTC clock | `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/repository/NetCdfImportProgressRepository.kt` | 수정 |
| fixture 생성 | `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt` | 수정 |
| service/PostGIS/Testcontainers contract | `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt` | 수정 |
| internal pure contract tests | `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSamplerTest.kt`, `NetCdfImportLimitsTest.kt`, `NetCdfAuxiliarySerializerTest.kt`, `NetCdfTilePlannerTest.kt` | 생성 |
| API compile/error tests | `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/NetCdfExceptionApiCompatibilityTest.kt` | 생성 |
| public docs | `utils/science/README.md`, `utils/science/README.ko.md` | 수정 |
| follow-up status | `docs/followup-issues/utils-science-readme-rewrite.md` | 수정 |
| fixed benchmark result | `docs/benchmarks/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary.md` | 실행 후 생성 |
| implementation lesson | `docs/lessons/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary.md` | PR 전 생성 |
| 통합 계획 검토 | `docs/review/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-plan-review.md` | Step 3-R/3-P gate 기록 |

테이블 정의인 `NetCdfTables.kt`, `NetCdfModels.kt`, 모듈 등록·BOM·CI workflow는 schema/API 데이터 모델을 그대로 유지하므로 변경하지 않는다. `attrs`와 `NetCdfFileTable.globalAttrs`의 기존 JSONB를 재사용한다.

## 2. Task 1 — RED fixture와 pure contract 테스트 고정

**Files:**

- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`
- Create: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfImportLimitsTest.kt`
- Create: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTilePlannerTest.kt`
- Create: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSamplerTest.kt`
- Create: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfAuxiliarySerializerTest.kt`
- Create: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/NetCdfExceptionApiCompatibilityTest.kt`

- [ ] **Step 1.1: fixture API와 실패 테스트를 먼저 작성한다.**

`NetCdfSampleWriter`에 다음 호출 표면과 deterministic expected-value 상수를
먼저 선언한다. 함수 본문은 기존 `writeSample`과 같은 NetCDF-3 builder를 사용해
완성할 때까지 컴파일 RED가 유지되며, 구현 단계에서 각 fixture의 차원·속성·값을
명시적으로 채운다. `writeLargeContractSample`은 동일한 deterministic generator로
`temperature_1d`(1D)와 `temperature_2d`(2D+one auxiliary)를 함께 만들 수 있어
baseline과 feature가 같은 cell-value workload를 사용한다.

```kotlin
fun writeCurvilinearSample(path: Path, dataOrder: List<String> = listOf("time", "y", "x")): Path
fun writeCfAuxiliarySample(path: Path): Path
fun writeProjected2DSample(path: Path, sourceCrs: String): Path
fun writeDuplicateCoordinateSample(path: Path, duplicateAcrossTiles: Boolean): Path
fun writeLargeContractSample(path: Path, rows: Int = 1_024, columns: Int = 1_024): Path

const val CURVILINEAR_ROWS: Int = 3
const val CURVILINEAR_COLUMNS: Int = 4
val CURVILINEAR_VALUES: DoubleArray = doubleArrayOf(
    0.0, 1.0, 2.0, 3.0,
    4.0, 5.0, 6.0, 7.0,
    8.0, 9.0, 10.0, 11.0,
)
```

`writeProjected2DSample`은 `sourceCrs`를 `.toInt()`로 변환하지 않고 `EPSG:` 접두사
뒤 원문을 NetCDF `epsg_code` string attribute로 기록한다. 따라서 `EPSG:+4326`,
`EPSG:4326.0`, overflow와 충돌 값을 파일 생성 단계에서 삼키지 않고 service의 strict
parser가 `UnsupportedProjection`으로 판정할 수 있다.

`writeDuplicateCoordinateSample(..., duplicateAcrossTiles = true)`는 최소
`257×257` spatial grid를 만들고 planner가 분리하는 row tile의 경계 양쪽(예: row 0과
row 255, 같은 column)에 동일 canonical point를 배치한다. `false`는 작은 단일-tile
fixture로 유지해 duplicate key set의 정상 경로도 검증한다.

`NetCdfCatalogServiceTest`에는 다음 이름의 테스트를 먼저 추가한다. 기존 rank 1–4 테스트는 유지한다.

```kotlin
@Test
fun `30a - curvilinear 2D axes preserve every cell coordinate and value`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeCurvilinearSample(dir.resolve("curvilinear.nc"))
    val fileId = service.registerFile(path.absolutePathString())
    service.importGridValues(fileId, "temperature")
    val readBack = readSpatialTuples(fileId)
    readBack shouldBe NetCdfSampleWriter.CURVILINEAR_TUPLES
}

@Test
fun `31 - CF coordinates stores altitude in attrs and excludes time axis`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeCfAuxiliarySample(dir.resolve("cf-aux.nc"))
    val fileId = service.registerFile(path.absolutePathString())
    service.importGridValues(fileId, "temperature")
    val attrs = transaction(db) {
        NetCdfGridValueTable.selectAll()
            .where { NetCdfGridValueTable.fileId eq fileId }
            .mapNotNull { it[NetCdfGridValueTable.attrs] }
    }
    attrs.all { "altitude" in it && "time" !in it } shouldBeEqualTo true
}

@Test
fun `32 - non standard data dimension order uses full rank index`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeCurvilinearSample(
        dir.resolve("order.nc"), dataOrder = listOf("time", "x", "y"),
    )
    val fileId = service.registerFile(path.absolutePathString())
    service.importGridValues(fileId, "temperature")
    val readBack = readSpatialTuples(fileId)
    readBack shouldBe NetCdfSampleWriter.CURVILINEAR_TUPLES
}

@Test
fun `33 - duplicate canonical coordinate is rejected before any insert`(@TempDir dir: Path) {
    val path = NetCdfSampleWriter.writeDuplicateCoordinateSample(
        dir.resolve("duplicate.nc"), duplicateAcrossTiles = true,
    )
    val fileId = service.registerFile(path.absolutePathString())
    assertFailsWith<NetCdfException.DuplicateCoordinate> {
        service.importGridValues(fileId, "temperature")
    }
    transaction(db) {
        NetCdfGridValueTable.selectAll().where { NetCdfGridValueTable.fileId eq fileId }.count()
    } shouldBeEqualTo 0L
}

@Test
fun `34 - unsupported grid mapping and malformed EPSG are typed failures`(@TempDir dir: Path) {
    val projected = NetCdfSampleWriter.writeProjected2DSample(dir.resolve("bad-crs.nc"), "EPSG:9999999")
    val projectedId = service.registerFile(projected.absolutePathString())
    assertFailsWith<NetCdfException.UnsupportedProjection> {
        service.importGridValues(projectedId, "temperature")
    }
    val malformed = NetCdfSampleWriter.writeProjected2DSample(dir.resolve("malformed-crs.nc"), "EPSG:+4326")
    val malformedId = service.registerFile(malformed.absolutePathString())
    assertFailsWith<NetCdfException.UnsupportedProjection> {
        service.importGridValues(malformedId, "temperature")
    }
}
```

테스트 본문은 `service.registerFile(path.absolutePathString())`, `service.importGridValues(fileId, "temperature")`, PostGIS `ST_X/ST_Y`, JSONB `attrs`, `assertFailsWith<NetCdfException.*>`를 사용한다.

`readSpatialTuples(fileId)`는 raw SQL로 `time_idx`, `level_idx`, `ST_X(location)`,
`ST_Y(location)`, `value`를 조회하고 fixture가 정의한 `(timeIdx, levelIdx, row,
column)` 순서로 정렬한 tuple을 반환한다. 30a와 32는 이 tuple 전체를
`CURVILINEAR_TUPLES`와 exact 비교한다. 값 집합 크기만 비교하거나 `timeIdx/value`만
비교하지 않으며, 전치·행/열 반전이 set 비교를 우회하지 못하도록 모든 좌표와 값이
고유한 fixture를 사용한다.

- [ ] **Step 1.2: pure contract의 최소 RED를 실행한다.**

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.internal.*' \
  --tests 'io.bluetape4k.science.exposed.NetCdfExceptionApiCompatibilityTest' \
  --no-configuration-cache --console=plain
```

Expected: `FAILED` because the new fixture/internal types and behavior are not yet
implemented. 이 실패는 구현 전 RED 증거이며, Testcontainers suite를 이 단계에서
병렬 실행하지 않는다.

## 3. Task 2 — typed exception, resource limit, path/fingerprint 경계

**Files:**

- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt`
- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfImportLimits.kt`
- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuard.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfImportLimitsTest.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/NetCdfExceptionApiCompatibilityTest.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 2.1: exception hierarchy와 API migration을 구현한다.**

`NetCdfException`의 sealed base에 다음 public subtype과 구조화된 필드를 추가한다. 메시지는 변수·축·원인을 포함하고, 다섯 subtype 모두를 `2.0.0` major migration으로 문서화한다. 기존 소비자가 구체 subtype exhaustive `when`을 사용하면 다섯 branch를 추가해야 하며, base-type catch에는 `else`/기본 오류 경로를 둔다.

```kotlin
class UnsupportedCoordinateAxis(
    val variableName: String,
    val coordinateName: String?,
    val reason: String,
) : NetCdfException("Unsupported coordinate axis: variable=$variableName coordinate=$coordinateName reason=$reason")

class DuplicateCoordinate(
    val fileId: Long,
    val variableName: String,
    val timeIdx: Int,
    val levelIdx: Int,
    val longitude: Double,
    val latitude: Double,
) : NetCdfException("Duplicate coordinate: fileId=$fileId variable=$variableName time=$timeIdx level=$levelIdx lon=$longitude lat=$latitude")

class ResourceLimitExceeded(val resource: String, val limit: Long, val actual: Long) :
    NetCdfException("Resource limit exceeded: $resource limit=$limit actual=$actual")

class FileChanged(val fileId: Long, val expectedFingerprint: String, val actualFingerprint: String) :
    NetCdfException("NetCDF file changed while import was resumable: fileId=$fileId")

class CorruptProgress(val progressId: Long, val detail: String) :
    NetCdfException("Corrupt NetCDF import progress: progressId=$progressId detail=$detail")
```

`NetCdfExceptionApiCompatibilityTest`는 다섯 새 subtype(`UnsupportedCoordinateAxis`,
`DuplicateCoordinate`, `ResourceLimitExceeded`, `FileChanged`, `CorruptProgress`)의
known branch와 `else`를 가진 base-type `when` fixture를 컴파일하고, 각 구조화 필드·stable
reason 보존을 검증한다.

- [ ] **Step 2.2: checked limits와 duplicate key set을 구현한다.**

`NetCdfImportLimits.kt`에 설계의 token 32, auxiliary 16, 이름 128 bytes, variables 1,024, group dimensions 256, metadata 1 MiB, cells 100,000,000, slices 1,000,000, tile 65,536, batch 1,000, JSONB 8,192, coordinate cache 64 MiB, duplicate set 32 MiB, owned working-set 128 MiB 상수를 둔다. `Math.multiplyExact`/`addExact` 기반 `Long` 산식으로 overflow를 `ResourceLimitExceeded`로 바꾸고 `Int` API 호출 전에 검사한다.

```kotlin
internal fun checkedProduct(vararg factors: Long): Long =
    factors.fold(1L) { acc, factor -> Math.multiplyExact(acc, factor) }

internal data class MemoryBudget(
    val tileBufferBytes: Long,
    val coordinateBytes: Long,
    val serializerScratchBytes: Long,
    val duplicateSetBytes: Long,
) {
    val ownedWorkingSetBytes: Long
        get() = Math.addExact(
            Math.addExact(tileBufferBytes, coordinateBytes),
            Math.addExact(serializerScratchBytes, duplicateSetBytes),
        )
}
```

`MAX_GROUP_COUNT = 256L`, `MAX_GROUP_DEPTH = 32L`도 metadata preflight hard limit으로
고정한다. root/group 순회는 재귀 호출 대신 명시적 stack/queue를 사용하고, group을
방문할 때마다 count/depth를 checked 검증한다. 깊은 빈 nested group과 count 초과
fixture는 variable/attribute map을 materialize하기 전에 `ResourceLimitExceeded`로
조기 중지되는지 확인한다. `MAX_FIXED_ROW_BYTES = 256L`을 고정된 보수 상수로 두고, writer가 소유하는
`batchPayloadBytes`를 `checkedProduct(MAX_BATCH_ROWS, MAX_FIXED_ROW_BYTES + MAX_AUXILIARY_JSONB_BYTES)`로
계산한다. `serializerScratchBytes`는 raw serializer scratch와 이 batch payload 중 큰
값으로 설정한다(`max(MAX_AUXILIARY_JSONB_BYTES, batchPayloadBytes)`). 따라서 JSON
문자열·고정 column payload·pending row list를 같은 checked 상한으로 계측하고,
overflow나 `ownedWorkingSetBytes > MAX_OWNED_WORKING_SET_BYTES`는 첫 tile 전에
`ResourceLimitExceeded`로 중지한다. benchmark counter에도 `batchPayloadBytes`와
실제 `pendingRows`를 함께 기록해 이 보수 상한이 실행 중에도 유지되는지 확인한다.

open-addressed `LongArray` pair key set은 `-0.0/+0.0`을 `+0.0`으로 정규화하고, 32 MiB budget을 넘으면 insert 없이 실패시킨다. 등록 전에는
NetCDF root/group를 bounded recursive scan하여 variable·dimension 수, 이름의 UTF-8
합계, attribute/global metadata의 UTF-8 합계를 먼저 계산한다. 이 preflight는
`variables`/`attributes`/`globalAttrs` map을 materialize하거나 DB에 저장하기 전에
실행하고, 초과 시 `ResourceLimitExceeded`를 반환한다. nested-group·deep/empty-group
및 1 MiB 경계 fixture로 명시적 stack 순서와 조기 중지를 검증한다. writer의
`batchPayloadBytes`는
`MAX_BATCH_ROWS * (MAX_FIXED_ROW_BYTES + MAX_AUXILIARY_JSONB_BYTES)`의 checked 상한으로
계산해 `serializerScratchBytes = max(rawScratchBytes, batchPayloadBytes)`에 포함하고,
`coordinateBytes + duplicateSetBytes`
64 MiB 및 전체 `ownedWorkingSetBytes` 128 MiB를 넘으면 tile을 시작하지 않는다.

- [ ] **Step 2.3: trusted file guard를 구현한다.**

`NetCdfFileGuard.validateForRegister`와 `verifyForResume`는 NUL/control/URI scheme/원격 URL/non-regular path/component symlink를 거부하고, canonical component를 `NOFOLLOW_LINKS`로 확인한다. `fileKey|size|lastModifiedTime.epochNanos`를 ASCII fingerprint로 생성하며 open 전·후 stat을 비교한다. fingerprint가 누락되거나 불일치하면 `FileChanged`로 중단하고 항상 새 등록·새 `fileId`를 사용한다. old `fileId`의 progress/partial rows를 resume에 재사용하지 않는다. `MAX_FILE_BYTES=64L * 1024 * 1024 * 1024`를 초과하면 `ResourceLimitExceeded`, 불일치는 `FileChanged`다. `openVerifiedDataset`는
resume fingerprint 검증 직후 identity를 캡처하고 dataset을 연 다음 즉시 다시
stat한다. 두 identity가 다르면 dataset을 닫고 DB progress 변경 없이
`FileChanged`를 발생시킨다. validation/open 사이 rename·symlink 교체와 대형
metadata fixture는 등록·resume 조기 거부를 검증한다.

Run after implementation:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.*path*' \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfImportLimitsTest' \
  --no-configuration-cache --console=plain
```

Expected: path/resource/exception tests PASS; any remaining axis/tile tests stay RED.

## 4. Task 3 — progress lease fence와 상태 복구

**Files:**

- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/repository/NetCdfImportProgressRepository.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 3.1: repository에 DB UTC와 opaque lease token을 고정한다.**

기존 public `acquireLease(fileId, variableName, leaseTtl: Duration)` 시그니처와 custom TTL을
보존한다. `leaseTtl`은 양의 whole-second로 검증하고 기본값만 5분으로 둔다. SQL에는
`leaseTtl.toSeconds()`를 parameter로 바인딩하며 DB UTC 기준의
`CURRENT_TIMESTAMP + (? * INTERVAL '1 second')`만 사용한다. `acquireLease`는 매번 DB 기준
expiry를 새로 발급하고 기존 token을 재사용하지 않는다.
단일 row CAS는 최초 row가 없으면 `IN_PROGRESS` row를 생성하는 upsert로 처리하고,
기존 `PENDING/FAILED` 또는 만료된 `IN_PROGRESS`만 takeover하며, 새 expiry 값을 DB에서
반환해 opaque token으로 사용한다. acquire 직전 같은 transaction에서
`SELECT ... FOR UPDATE`로 기존 row를 잠그고, `status=IN_PROGRESS`인데
`lease_expires_at IS NULL`인 malformed row는 `FAILED`로 repair한 뒤 새 lease를
획득한다. `status=IN_PROGRESS`이고 expiry가 미래인 row는 `ImportAlreadyRunning`으로
분리한다. 따라서 최초 동시 acquire는 unique key 아래 정확히 한 owner만 만들고,
acquire의 affected-row=0은 active lease 경쟁일 때만 `ImportAlreadyRunning`이다.
`COMPLETED` race는 row를 다시 읽어 검증된 no-op으로 분기하고, 이미 획득한
owner의 후속 fence 실패만 `ImportLeaseLost`다.

```sql
INSERT INTO netcdf_import_progress
    (file_id, variable_name, status, last_slice_idx, lease_expires_at, started_at, updated_at)
VALUES (?, ?, 'IN_PROGRESS', NULL, CURRENT_TIMESTAMP + (? * INTERVAL '1 second'), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (file_id, variable_name) DO UPDATE
SET status = 'IN_PROGRESS', lease_expires_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'),
    error_message = NULL, updated_at = CURRENT_TIMESTAMP
WHERE netcdf_import_progress.status IN ('PENDING', 'FAILED')
   OR (netcdf_import_progress.status = 'IN_PROGRESS'
       AND netcdf_import_progress.lease_expires_at <= CURRENT_TIMESTAMP)
RETURNING id, lease_expires_at
```

insert/update의 두 TTL parameter는 같은 검증된 `leaseTtl.toSeconds()` 값으로
바인딩한다. custom TTL(예: 30초/10분)과 기본 5분을 각각 호출하는 compatibility
test에서 expiry가 DB `CURRENT_TIMESTAMP` 기준으로 계산되고, 기존 public repository
호출자가 시그니처 변경 없이 동작하는지 확인한다.

`assertLeaseOwner`/`touchLease`/`renewLease`/`markCompleted`/`markFailed`는 모두 다음 predicate를 사용한다.

```sql
WHERE id = ?
  AND status = 'IN_PROGRESS'
  AND lease_expires_at = ?
  AND lease_expires_at > CURRENT_TIMESTAMP
```

acquire와 위 fence의 affected row가 1이 아니면 즉시 해당 경로의 typed exception을
던지고, progress row lock은 같은 transaction commit까지 유지한다. SQL 비교에
`Instant.now()`를 쓰지 않으며, 테스트에는 deterministic DB-clock seam을 주입한다. 두 importer가 만료
경계에서 동시에 takeover를 시도하는 concurrency fixture는 정확히 한 쪽만 새 token과
affected row=1을 얻는지 검증한다.

- [ ] **Step 3.2: invariant repair와 cancellation/failure 경로를 테스트 우선으로 완성한다.**

`PENDING/FAILED`, `IN_PROGRESS`, `COMPLETED` invariant, null lease repair, `-1..totalSlices-1` checkpoint, COMPLETED 마지막 slice 불일치 `CorruptProgress`, stale importer rollback, cancellation/interrupt의 lease expiry 위임을 테스트한다. `COMPLETED` no-op은 dataset을 verified-open하여 실제 `totalSlices`와 checkpoint를 먼저 검증한 뒤에만 허용하고, 불일치면 `CorruptProgress`로 종료한다. 마지막 `totalSlices - 1` slice의 row write/lease fence 뒤 DB commit 전에 같은 transaction에서 `markCompleted`를 호출하고, fence 또는 `markCompleted` 실패 시 전체 현재 transaction을 rollback한다. `status=COMPLETED`, `completedAt != null`, `lease_expires_at = null`, 마지막 checkpoint 불변식을 검증한다. `markFailed` 실패는 원래 exception에 suppressed로 추가하고, cancellation은 `FAILED`로 전환하지 않는다.

`CorruptProgress`는 library 내부에서 자동 reset하거나 재시도하지 않는다. application-owned
admin recovery는 먼저 `SELECT ... FOR UPDATE`로 progress row를 잠그고 status, checkpoint,
lease, error의 immutable 기준 상태 기록과 `progressId`를 audit/quarantine에 기록한다. 기존
column만 사용할 때 `PENDING/IN_PROGRESS/FAILED` row는 기준 상태 기록 후 stable
`CORRUPT_PROGRESS:<progressId>`를 `error_message`에 기록하고 `FAILED`, `lease=null`로
격리하며, `COMPLETED` row는 변경하지 않고 old `fileId`를 차단한다. partial rows는 old
`fileId`에 묶어 retention 정책에 따라 보존하거나 삭제하고, 수정·검증한 파일은 반드시
새 등록·새 `fileId`로 import한다. in-place reset API가 없다는 것을 runbook과 테스트에
명시하고, 격리 전후에 old progress가 재사용되지 않는지 검증한다.

`IN_PROGRESS` + `lease_expires_at IS NULL` repair, 미래 expiry의
`ImportAlreadyRunning`(rejection metric 비증가), 만료 경계의 단일 takeover,
후속 owner fence 실패의 `ImportLeaseLost`를 각각 독립 테스트로 고정한다.

`ImportAlreadyRunning`과 `ImportLeaseLost`의 caller retry policy는
`MAX_LEASE_RETRIES = 3`, backoff `1초 → 2초 → 4초`로 고정한다. 각 시도 전 DB에서
현재 lease/status를 다시 읽고, 세 번 소진하면 원래 typed exception을 표면화하고
alert를 발생시킨다. `FileChanged`, `DuplicateCoordinate`, `CorruptProgress`에는 이
retry policy를 적용하지 않으며, 무한 loop를 만들지 않는다.

```kotlin
assertFailsWith<NetCdfException.ImportLeaseLost> {
    transaction(db) {
        progressRepo.renewLease(stale.id, stale.leaseExpiresAt!!, lastSliceIdx = 99L)
    }
}
```

- [ ] **Step 3.3: progress 테스트를 순차 실행한다.**

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.23*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.*progress*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.*cancellation*' \
  --no-configuration-cache --console=plain
```

Expected: initial upsert/unique-key race, stale lease/takeover/null repair/checkpoint/terminal-transition tests PASS. Testcontainers는 이 invocation만 실행한다.

## 5. Task 4 — axis map, bounded readers, sampler, CRS

**Files:**

- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/VariableAxisMap.kt`
- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSampler.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/CoordinateReprojector.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSamplerTest.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 4.1: `VariableAxisMap`에 CF 역할·dimension binding을 구현한다.**

dimension axis → CF `coordinates` token → `AxisType`/`standard_name`/units/name fallback 순서로 후보를 수집한다. full/short name ambiguity, time/level 재분류, rank/shape/dimension mismatch, 빈 spatial dimension을 typed exception으로 거부한다. `latBinding`/`lonBinding`은 1D 또는 2D axis와 axis dimension 순서를 보존하고, auxiliary는 numeric rank 1/2이며 grid dimension 부분집합이어야 한다.

- [ ] **Step 4.2: `CoordinateReader`와 sampler를 bounded window로 구현한다.**

```kotlin
internal interface VariableReader {
    fun read(origin: IntArray, shape: IntArray): UcarArray
}

internal interface CoordinateReader {
    fun read1D(axisName: String, origin: Int, length: Int): DoubleArray
    fun read2D(
        axisName: String,
        rowOrigin: Int,
        columnOrigin: Int,
        rowCount: Int,
        columnCount: Int,
    ): DoubleArray
}

internal interface CoordinateSampler {
    fun sample(globalRow: Int, globalColumn: Int, target: MutableCoordinateSample)
}

internal class MutableCoordinateSample {
    var longitude: Double = 0.0
    var latitude: Double = 0.0
    val auxiliary: MutableMap<String, Double> = LinkedHashMap()

    fun clear() {
        longitude = 0.0
        latitude = 0.0
        auxiliary.clear()
    }

    fun readOnlyCopy(): CoordinateSample = CoordinateSample(longitude, latitude, auxiliary.toMap())
}

internal data class CoordinateSample(
    val longitude: Double,
    val latitude: Double,
    val auxiliary: Map<String, Double>,
)
```

`read2D`는 row-major flat 배열(`localRow * columnCount + localColumn`)을 반환한다. `CoordinateSampler.sample`은 호출자가 소유한 `MutableCoordinateSample`을 매 셀 clear한 뒤 채우고, sampler는 target·`CoordinateSample`·auxiliary map을 보유하지 않는다. service는 같은 셀에서 즉시 `readOnlyCopy()`를 만들고 JSONB를 직렬화한 뒤 다음 셀을 요청한다. `CoordinateSample` 복사본과 그 map은 tile 밖으로 전달하지 않는다. direct `getCoordValue` 우회는 금지한다. spatial non-finite와 최종 lon/lat 범위를 검증하고 non-spatial non-finite auxiliary는 생략한다. 이 읽기 전용 복사본 경로의 transient allocation과 GC/peak-heap은 Task 8.2 benchmark에서 관찰하고 owned working-set에는 bounded payload 상한을 포함한다.

- [ ] **Step 4.3: CRS whitelist와 reprojection을 연결한다.**

EPSG 4326/4269/3857/32601–32660/32701–32760/3413/3031 및 `latitude_longitude`만 허용한다. `grid_mapping`이 있으면 mapping variable을 반드시 해석하고, ASCII digits/integral numeric exact parse·충돌·overflow·공백·부호·소수형을 거부한다. source/final finite와 bounds를 모두 검사한다.

- [ ] **Step 4.4: axis reader/sampler pure 테스트를 GREEN으로 만든다.**

2D row/column orientation, reversed axis, 1D fallback, CF `coordinates="time lat lon altitude"`, ambiguous/missing/unsupported rank/shape, non-finite, projected CRS/unsupported mapping을 `assertFailsWith`와 exact sample 비교로 검증한다. `MutableCoordinateSample`이 매 셀 clear되고 `readOnlyCopy()`가 다음 셀 변이에 영향을 받지 않는지, sampler가 target/map을 보유하지 않는지 recording seam으로 검증한다.

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfCoordinateSamplerTest' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.30a*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.31*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.34*' \
  --no-configuration-cache --console=plain
```

Expected: axis/CRS tests PASS; tile writer integration remains RED until Task 5–6.

## 6. Task 5 — deterministic tile planner와 rank 1–4 data mapping

**Files:**

- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTilePlanner.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTilePlannerTest.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 5.1: tile planner와 full-rank index를 구현한다.**

`tileCols=min(lonN,65_536)`, `tileRows=min(latN,max(1,65_536/tileCols))`에서 시작하고 checked product가 65,536 이하가 되도록 축소한다. rank 2–4 `variable.read(tileOrigin,tileShape)`는 time/level=1, row/column=tile shape, 나머지=1이다. `Index`와 `IntArray`는 tile 안에서 재사용한다. rank 1 time-only도 `origin=[offset]`, `shape=[min(remaining,1_000)]` window로 읽고 매 window마다 최대 1,000행을 flush한다. rank 1 window 경계에서도 checked cell limit, heartbeat, cancellation/deadline을 검사하고 `RecordingVariableReader`로 1,001개 이상 fixture의 full-array read와 batch 초과를 금지한다.

각 tile loop는 `val sampleTarget = MutableCoordinateSample()` 하나만 생성하고, 모든
셀에서 `CoordinateSampler.sample(..., sampleTarget)` → `readOnlyCopy()` → serializer
순서로 사용한다.

```kotlin
timeDim?.let { indices[it] = 0 }
levelDim?.let { indices[it] = 0 }
indices[gridRowDim] = localRow
indices[gridColumnDim] = localColumn
val raw = tileData.getDouble(tileIndex.set(indices))
sampler.sample(tileRowOrigin + localRow, tileColumnOrigin + localColumn, sampleTarget)
val sample = sampleTarget.readOnlyCopy()
```

`timeDim`/`levelDim`은 rank에 따라 `Int?`이며, 없는 축에는 assignment를 수행하지
않는다. local time/level은 존재할 때만 항상 0이고 global 값은 `tileOrigin`에만 둔다.
`sampleTarget`은
tile loop가 소유하고 매 셀 clear하며, `sample`은 같은 셀에서 serializer로 즉시
소비한 뒤 버린다. tile data/axis window/serializer scratch는 transaction `finally`에서
폐기한다. rank 1 time-only 경로도 동일한 cancel/deadline·heartbeat와 batch cap을 유지한다.

- [ ] **Step 5.2: slice-wide two-pass와 duplicate 정책을 구현한다.**

첫 pass는 tile read/coordinate validation/key set만 수행하고 매 tile 경계와 tile read 직후에 heartbeat fence·cancellation/deadline을 확인한다. 첫 pass 성공 전에는 DB insert가 없어야 한다. 두 번째 pass는 tile별 transaction으로 실행하며 tile 경계·write 직전·commit 직전에 같은 control seam을 검사한다. `CancellationException`/interrupt는 원래 예외와 interrupt 상태를 보존하며 `FAILED` 전환 없이 현재 tile transaction만 rollback한다. 이미 commit된 이전 tile과 slice는 보존할 수 있고, 취소된 현재 tile의 rows만 tile 시작 시점으로 복귀해야 한다. timeout caller는 `Future.cancel(true)`를 사용할 수 있다. duplicate면 첫 pass에서 `DuplicateCoordinate`로 slice 전체를 거부하므로 두 번째 pass의 어떤 tile도 insert되지 않아 해당 slice rows-written=0이어야 한다. 첫 write 직전과 flush 중 취소를 각각 주입하고, `*cancellation*` selector로 두 경로를 순차 검증한다.

- [ ] **Step 5.3: data-order/large shape 테스트를 GREEN으로 만든다.**

`[time,y,x]`, `[time,x,y]`, `[y,x,time]`, rank 2–4, tile 경계 offset, empty/overflow dimensions, `1024×1024` tile read cell cap을 검증한다. `RecordingVariableReader`에서 full-array read가 없고 각 read shape product가 65,536 이하임을 확인한다.

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfTilePlannerTest' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.6*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.7*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.8*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.32*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.33*' \
  --no-configuration-cache --console=plain
```

## 7. Task 6 — auxiliary JSONB serializer와 same-connection JDBC writer

**Files:**

- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfAuxiliarySerializer.kt`
- Create: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTileBatchWriter.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfAuxiliarySerializerTest.kt`
- Test: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`

- [ ] **Step 6.1: serializer와 row payload를 구현한다.**

NFC strict UTF-8 key, invalid surrogate/control/length/canonical collision/`__bluetape4k_` 거부, numeric-only map, escaped UTF-8 8,192-byte cap, empty map `NULL`을 구현한다. JSONB는 Jackson serializer를 재사용하고 `PGobject` 또는 typed `jsonb` placeholder로만 bind한다.

```kotlin
internal data class TileRow(
    val fileId: Long,
    val variableName: String,
    val longitude: Double?,
    val latitude: Double?,
    val timeIdx: Int,
    val levelIdx: Int,
    val value: Double,
    val attrsJson: String?,
)

internal data class BatchWriteResult(val inserted: Int, val conflicts: Int)
```

serializer는 `MutableCoordinateSample.readOnlyCopy()`가 만든 현재 셀의
`CoordinateSample`을 `TileRow`로 변환하고, `TileBatchWriter`가 행을 보유하지 않는
동기 lifetime 계약을 지키며 다음 sample 전에 즉시 직렬화한다. service는 tile 전체 행을
materialize하지 않고 `pendingRows.size <= 1_000`인 bounded chunk만 유지한다.
encoded attrs와 고정 row payload byte는 `MemoryBudget`의 checked batch payload
상한에 포함하고, 다음 chunk를 만들기 전에 이전 list·문자열 참조를 폐기한다.
rank 1 time-only row는 `longitude == null && latitude == null`, rank 2–4 spatial row는
두 값 모두 non-null이어야 하며, mixed-null row는 writer에 전달하기 전에 내부 invariant
위반으로 거부한다. writer는 두 값이 모두 null이면 기존 `location` column에 typed SQL
`NULL`을 바인딩하고, 둘 다 값이 있으면 `(longitude, latitude)` 순서로 PostGIS point를
바인딩한다. rank별 null-binding과 connection recording을 함께 검증한다.

- [ ] **Step 6.2: writer의 transaction/connection 경계를 구현한다.**

```kotlin
internal interface TileBatchWriter {
    fun write(connection: java.sql.Connection, rows: List<TileRow>): BatchWriteResult
}
```

writer는 `TransactionManager.current().connection.connection`과 동일한 connection만 받고, `DataSource.getConnection`, 새 Exposed/JDBC transaction, connection close를 하지 않는다. 호출 입력 `rows.size`가 1,000을 초과하면 즉시 `ResourceLimitExceeded`로 거부해 tile-wide list를 차단한다. `PreparedStatement`만 닫고, batch 행 수를 1,000 이하로 flush한다. 각 flush 뒤 payload 참조를 해제하고 control seam을 검사한다. `1`/`0`만 정상이며 `SUCCESS_NO_INFO`/기타 값은 bounded canonical-key 조회 후 설명되지 않으면 rollback한다.

- [ ] **Step 6.3: DB writer와 JSONB 테스트를 GREEN으로 만든다.**

recording writer에서 connection identity, `rows.size <= 1_000`, pending rows/encoded
payload 상한, flush/commit 순서, 두 번째 pass 첫 write 직전·flush 중 취소 후 현재
tile rollback을 검증한다. 이전에 commit된 tile/slice rows는 보존되는지 함께 확인한다.
active connection=0 및 추가 lease renew 없음은 bounded
`awaitTermination` 뒤 recording seam으로 확인한다. adjacent cell auxiliary 값 오염, quote/injection-like key,
UTF-8 8,192/8,193 boundary, empty attrs와 idempotent conflict를 검증한다.

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.internal.NetCdfAuxiliarySerializerTest' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.31*' \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.33*' \
  --no-configuration-cache --console=plain
```

## 8. Task 7 — service 통합, metrics, 기존 회귀

**Files:**

- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt`

- [ ] **Step 7.1: register/import 진입점에 guard와 fingerprint를 연결한다.**

`registerFile`은 path guard/pre-stat → NetCDF open 전후 identity 확인 → verified
dataset의 bounded metadata preflight(명시적 stack/queue) → map materialization →
reserved fingerprint collision 확인 → DB 저장 순서를 지킨다. `importGridValues`는
다음 순서를 고정한다.

```text
record 조회(없으면 lease/metric 없이 FileRecordNotFound)
→ guard와 persisted fingerprint 확인
→ openVerifiedDataset(open 전후 identity 재검증)
→ variable/axis/CRS/limits와 실제 totalSlices 검증
→ progress 조회 및 invariant/checkpoint 검증
→ COMPLETED이면 검증된 checkpoint가 마지막 slice일 때만 no-op 반환
→ 그 밖에는 lease acquire(CAS) 후 동일 dataset identity를 재확인하고 import
```

검증 단계에서는 progress checkpoint와 metric을 변경하지 않으며, 검증을 통과한 뒤
별도 phase에서만 lease 필드 acquire를 수행한다. lease acquire가 경쟁으로
`COMPLETED`를 반환하면 같은 invariant를 다시 검증한다. TOCTOU는 dataset을 닫고
`FileChanged`로 종료한다. 실제 slice import 이후에만 checkpoint/metric을 전진시키고,
마지막 slice의 row write와 lease fence를 수행한 뒤 DB commit 전에 같은 transaction에서
`markCompleted`를 호출한다. 이 호출은
`status=COMPLETED`, `completedAt != null`, `lease_expires_at = null`,
`lastSliceIdx = totalSlices - 1`을 한 transaction에서 보장한다. 불일치는
`CorruptProgress`로 종료한다.

- [ ] **Step 7.2: rank 1–4를 공통 tile engine으로 전환한다.**

rank 1은 location/attrs null과 time index를 유지한다. rank 2–4는 `VariableAxisMap`/sampler/tile planner/writer를 사용하고, 마지막 tile만 checkpoint를 전진시킨다. 마지막 `totalSlices - 1` tile의 row write/lease fence를 수행한 뒤 DB commit 전에 `markCompleted`를 호출해 terminal status/lease clear/completedAt/checkpoint invariant를 같은 transaction에서 검증한다. fence 또는 `markCompleted` 실패 시 현재 transaction 전체를 rollback한다. success/failure/nan/records/slice timer metric 이름과 low-cardinality label을 기존 계약에 맞춰 유지한다. pre-lease 입력 거부는 `netcdf.import.rejected`의 고정 allowlist(`reason=resource|axis|crs|duplicate|path|progress`)만 증가시키고, `FileRecordNotFound`/`ImportAlreadyRunning`은 기존 비증가 계약을 유지한다. 예외·로그 detail은 stable reason code와 CR/LF/control 제거·길이 제한을 적용하고, raw variable/coordinate/path 이름은 metric label로 사용하지 않는 allowlist를 고정한다. reason sanitation·rejection counter 증가/비증가와 label cardinality 회귀를 검증한다.

- [ ] **Step 7.3: 기존 service suite를 순차 실행한다.**

Run:

```bash
./gradlew :bluetape4k-science:test \
  --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest' \
  --no-configuration-cache --console=plain
```

Expected: 기존 rank 1–4, NaN/FillValue, CRS, resume/no-op, lease takeover, metrics tests와 새 30a–34 tests가 모두 PASS. 실패하면 raw Testcontainers 로그를 읽고 원인 수정 후 이 단계부터 재실행한다.

## 9. Task 8 — fixture benchmark와 문서 계약

**Files:**

- Modify: `utils/science/README.md`
- Modify: `utils/science/README.ko.md`
- Modify: `docs/followup-issues/utils-science-readme-rewrite.md`
- Create: `docs/benchmarks/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary.md`
- Temporary (discard before commit): `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt` benchmark-only harness
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt` KDoc
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/VariableAxisMap.kt` KDoc
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/CoordinateReprojector.kt` KDoc

- [ ] **Step 8.1: public KDoc/README EN·KO를 실제 계약으로 갱신한다.**

blocking `registerFile`/`importGridValues` 호출을 모두 worker/virtual-thread executor에서 실행하는 예시, `location` lon-first와 `attrs["altitude"]`, 2D/CF coordinate, supported CRS, hard limits, `FileOpen`/`FileChanged`/`CorruptProgress`/`DuplicateCoordinate` 복구를 양 locale에 같은 구조로 기록한다. `registerFile`도 blocking이며 open 단계가 caller deadline을 선점할 수 있으므로 bounded executor와 명시적 shutdown을 사용한다. timeout 뒤 작업이 계속 lease/connection을 점유하지 않도록 등록된 `fileId`를 관찰할 수 있는 `Future`를 보존하고 `TimeoutException`에서 `cancel(true)`를 호출하며, 구현은 open 직후·tile 경계·write/commit 직전 취소를 관찰하고 `CancellationException`/interrupt 상태를 보존한다. `cancel(true)`는 협력적 취소일 뿐 완료나 side effect 부재를 보장하지 않는다. 호출자는 `cancel(true)` 뒤 `shutdownNow()`와 함께 최대 30초의 bounded `awaitTermination`으로 worker 종료를 기다리고, `awaitTermination` 자체가 interrupt되면 interrupt 상태를 복원한 뒤 실패를 표면화한다. worker가 30초 안에 종료되지 않으면 재시도하지 않고 운영자에게 escalation한다. timeout/interrupt 뒤 재시도하기 전에는 관찰된 `fileId`의 DB progress·lease·partial rows를 재조회해 active connection=0, 추가 renew 없음, 현재 tile rollback을 확인한다. 이 side-effect 재확인을 통과하지 못하면 재시도하지 않는다. recording connection/lease seam에서 이 조건을 검증한다. 첫 write 직전과 flush 중 취소 테스트는 이전에 commit된 tile/slice의 rows는 보존하고 현재 tile rows만 시작 시점으로 복귀하는지 검증한다. duplicate preflight 실패는 두 번째 pass 전에 slice rows-written=0으로 유지되는지 별도로 확인한다. trusted-admin 전용 경계와 caller가 authN/authZ·tenant ownership·허용 root allowlist를 보장해야 한다는 책임, identity-only fingerprint의 same inode/size/mtime 한계와 immutable 저장소 전제도 명시한다. README의 기존 “CoordinateAxis2D 비지원” 문구를 제거하고 migration/dependency 책임은 보존한다. 2.0.0 sealed-exception migration subsection에는 다섯 새 subtype(`UnsupportedCoordinateAxis`, `DuplicateCoordinate`, `ResourceLimitExceeded`, `FileChanged`, `CorruptProgress`) 각각의 exhaustive `when` branch와 base catch의 `else` 호환 패턴을 함께 기록한다. 입력·metadata·경로·축·변수 오류는 수정 후 항상 새 등록·새 `fileId`를 사용하고, fingerprint가 변경된 old progress/partial rows를 resume하지 않는다. `CorruptProgress`는 자동 retry하지 않고 progress row를 `SELECT ... FOR UPDATE`로 잠가 기준 상태 기록/audit/quarantine한 뒤 기존 column의 `FAILED`+`CORRUPT_PROGRESS:<progressId>` 또는 `COMPLETED` 차단으로 격리하고 partial rows를 retention 정책에 따라 보존·삭제한 다음 새 `fileId`로 import한다. `ImportAlreadyRunning`/`ImportLeaseLost`는 최대 3회(1초·2초·4초 backoff)만 시도하고 소진 시 원래 typed exception과 alert를 표면화한다. `FileRecordNotFound`/`VariableNotFound`/`UnsupportedVariable`는 record/variable을 보정한 뒤 기존 progress를 임의로 재사용하지 않고 새 호출·새 `fileId`로 처리한다. `NetCdfExceptionApiCompatibilityTest` compile fixture와 양 locale read-back을 연결한다. health/readiness endpoint는 이 library 범위가 아니므로 N/A로 기록하고, application operator가 DB 연결·PostGIS/NetCDF runtime·DB/host UTC 동기화를 사전 점검한다. README/follow-up runbook에는 기본 5분 lease TTL과 custom TTL compatibility, stale progress 소유권·격리·rollback 절차, `netcdf.import.rejected` allowlist와 기본 alert(자원 거부 1건 또는 5분 내 `ImportLeaseLost` 2건)를 명시하며, threshold 조정 권한은 배포 애플리케이션에 둔다.

```kotlin
val executor = Executors.newVirtualThreadPerTaskExecutor()
val registeredFileId = AtomicLong(-1L)
val task: Future<Long> = executor.submit {
    val fileId = catalog.registerFile("/data/grid.nc") // blocking; trusted-admin path
    registeredFileId.set(fileId)
    catalog.importGridValues(fileId, "temperature") // blocking; tile-boundary cancel
    fileId
}
try {
    val fileId = task.get(30, TimeUnit.MINUTES)
    println("imported fileId=$fileId")
} catch (timeout: TimeoutException) {
    task.cancel(true) // caller owns cancellation and interrupt propagation
    throw timeout
} catch (interrupted: InterruptedException) {
    task.cancel(true)
    Thread.currentThread().interrupt()
    throw interrupted
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (failure: ExecutionException) {
    throw (failure.cause ?: failure)
} finally {
    executor.shutdownNow()
    try {
        check(executor.awaitTermination(30, TimeUnit.SECONDS)) { "import worker did not terminate" }
    } catch (interrupted: InterruptedException) {
        Thread.currentThread().interrupt()
        throw interrupted
    }
}
// timeout/interrupt 후에는 registeredFileId의 progress·lease·partial rows를 재조회하고
// side effect가 종료된 경우에만 caller retry policy(MAX_LEASE_RETRIES=3)를 적용한다.
```

- [ ] **Step 8.2: fixed benchmark를 실행하고 결과를 저장한다.**

`writeLargeContractSample(rows=1_024, columns=1_024)`가 같은 deterministic value generator로
`temperature_1d`와 `temperature_2d`+one auxiliary를 만든다. feature worktree에서 fixture를
한 번 생성해 SHA-256과 경로를 고정하고, baseline worktree에는 그 파일을 복사한다. baseline과
feature 각각에 동일한 임시 benchmark-only JUnit harness를 적용해(커밋하지 않음) 정확히 다음
순서로 `temperature_1d`를 측정한다: 새 baseline worktree를
`45260871f58433a78f2d633c235010f661d22c6e`로
만들고, clean DB/Testcontainers에서 1 warm-up 후 3회 순차 실행; feature에서도 새
fileId/clean DB로 같은 1+3회 실행. 각 실행은 fixture read 시작부터 마지막 commit까지
측정하고 생성·register 시간은 제외한다. harness 적용·제거 명령과 두 worktree의 commit SHA,
fixture SHA-256, 정확한 Gradle `--tests` selector를 artifact에 기록한다. 동일 JVM/Gradle/
PostGIS image, JVM `-Xmx`, Docker image, run durations/median, observed peak heap, owned
counters를 함께 남긴다. 1D throughput은 baseline median 대비 20% 이상 하락하지 않아야
하며 2D+auxiliary cells/sec는 report-only다. 두 경로 모두 `pendingRows<=1_000`, tile read
`<=65,536`, owned working-set `<=128 MiB`를 counter/assertion으로 증명한다.

임시 harness의 테스트 이름은 `large1d throughput uses fixed generator`와
`large2d auxiliary stays within bounded contract`로 고정하고, baseline/feature 모두 다음
selector를 사용한다.

```bash
git worktree add --detach .worktrees/chore/1352-coordinate-axis2d-cf-grid-baseline \
  45260871f58433a78f2d633c235010f661d22c6e
(cd .worktrees/chore/1352-coordinate-axis2d-cf-grid-baseline && \
  test "$(git rev-parse HEAD)" = "45260871f58433a78f2d633c235010f661d22c6e" && \
  ./gradlew :bluetape4k-science:test \
    --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.large1d*' \
    -PincludeTags=slow-netcdf --no-configuration-cache --console=plain)
(cd .worktrees/feat/1352-coordinate-axis2d-cf-grid && \
  ./gradlew :bluetape4k-science:test \
    --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.large1d*' \
    -PincludeTags=slow-netcdf --no-configuration-cache --console=plain)
```

2D+auxiliary는 workload가 달라 feature worktree에서만 report-only로 다음 selector를
별도 실행하고, baseline gate에는 포함하지 않는다.

```bash
(cd .worktrees/feat/1352-coordinate-axis2d-cf-grid && \
  ./gradlew :bluetape4k-science:test \
    --tests 'io.bluetape4k.science.exposed.service.NetCdfCatalogServiceTest.large2d*' \
    -PincludeTags=slow-netcdf --no-configuration-cache --console=plain)
```

임시 harness는 `ManagementFactory.getMemoryMXBean().heapMemoryUsage.used`를 fixture read
직전·각 tile/flush 경계·마지막 commit 직후에 기록하고, 최대 관측값을 peak heap으로
보고한다. `MemoryBudget` counters(`tileCells`, `pendingRows`, `batchPayloadBytes`,
`coordinateBytes`, `duplicateSetBytes`, `ownedWorkingSetBytes`)는 같은 시점에 JSON/CSV로
남기며, JFR은 선택적 진단 자료로만 첨부한다.

각 worktree에서 harness 적용 전후 `git status --short`가 원상 복귀하고, benchmark worktree는
결과 artifact를 고정한 뒤에만 제거한다.

Run the baseline `large1d*` commands and the feature `large2d*` report-only command
sequentially; never run the two Testcontainers invocations concurrently.

- [ ] **Step 8.3: 문서 audit를 실행한다.**

```bash
node /Users/debop/.codex/skills/bluetape-writer/scripts/audit-korean-terms.mjs \
  utils/science/README.ko.md \
  docs/followup-issues/utils-science-readme-rewrite.md
git diff --check
```

Expected: terminology audit passed, whitespace check has no output, EN/KO section and code example read-back matches source.

## 10. Task 9 — 표준 검증·위험 예측·rollback

**Files:**

- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfException.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogService.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfImportLimits.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfFileGuard.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/VariableAxisMap.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfCoordinateSampler.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/CoordinateReprojector.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTilePlanner.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfAuxiliarySerializer.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/internal/NetCdfTileBatchWriter.kt`
- Modify: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/repository/NetCdfImportProgressRepository.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/support/NetCdfSampleWriter.kt`
- Modify: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/service/NetCdfCatalogServiceTest.kt`
- Create: `docs/lessons/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary.md`

- [ ] **Step 9.1: triggered risk register를 실행 증거와 연결한다.**

| 위험 | 조기 신호 | 완화·재실행 |
|---|---|---|
| dimension order 전치 | `[time,x,y]` read-back 불일치 | Task 5 full-rank index 수정 후 Task 5.3/7.3 재실행 |
| stale owner가 새 owner를 덮음 | `ImportLeaseLost` 뒤 row 혼합 | Task 3 CAS/row-lock test와 Task 7.3 재실행 |
| duplicate가 unique index에서 유실 | cross-tile row count 감소 | Task 5 two-pass preflight 수정 후 Task 7.3 재실행 |
| tile/JSONB heap 증가 | owned counter 또는 batch cap 초과 | Task 2 budget/Task 5 planner/Task 6 serializer 수정 후 benchmark 재실행 |
| second pass 중 취소가 partial write를 남김 | `Future.cancel(true)` 뒤 rows-written 증가 또는 FAILED 전환 | Task 5.2/6.2 control seam·rollback 수정 후 cancellation suite 재실행 |
| 손상된 COMPLETED progress가 no-op으로 통과 | totalSlices/checkpoint 불일치가 감지되지 않음 | Task 3.2/7.1 verified-open invariant 수정 후 progress suite 재실행 |
| path TOCTOU/fingerprint 혼합 | same-size mutation resume 성공 | Task 2 guard 수정 후 path/fingerprint tests 재실행 |
| 등록 metadata가 map materialize 전에 heap을 소진 | nested/1 MiB fixture에서 늦은 실패 | Task 2.2 preflight scan 수정 후 metadata suite 재실행 |
| 입력 이름이 log/metric cardinality를 오염 | CR/LF/control 또는 raw variable label 관찰 | Task 7.2 sanitation/label allowlist 수정 후 observability suite 재실행 |
| NetCDF/Testcontainers 환경 오탐 | Connection refused 또는 mount error | `colima status`, `docker context show`, `docker info` 확인 후 재시도; healthy Colima 재시작 금지 |
| public sealed API source break | consumer exhaustive when compile error | 2.0.0 migration fixture/KDoc 수정 후 API test 재실행 |

- [ ] **Step 9.2: proportional module verification을 실행한다.**

```bash
./gradlew :bluetape4k-science:compileKotlin \
  --no-configuration-cache --console=plain
./gradlew :bluetape4k-science:test \
  --no-configuration-cache --console=plain
./gradlew :bluetape4k-science:test \
  -PincludeTags=slow-netcdf --no-configuration-cache --console=plain
./gradlew :bluetape4k-science:detekt \
  --no-configuration-cache --console=plain
git diff --check
```

Expected: compile/test/slow-netcdf/detekt PASS, `git diff --check` no output. 실패 시 실패 task의 raw output을 진단하고 해당 Task부터 RED/GREEN을 반복한다.

- [ ] **Step 9.3: spec-to-plan read-back과 rollback을 확인한다.**

`rg -n`으로 spec의 `CoordinateAxis2D`, CF auxiliary, tile/cache/batch/working-set, CRS, duplicate, lease/fingerprint, API migration, README/benchmark 각 요구가 이 plan의 Task 2–9에 매핑됐는지 확인한다. 구현 실패 시 branch-local commit을 `git revert <commit>`으로 되돌리고 canonical `develop`이나 다른 worktree는 건드리지 않는다. schema migration이 없으므로 DB rollback은 transaction rollback과 테스트 fixture cleanup으로 한정한다.

## Writer gate

| 항목 | 판정 | 근거 |
|---|---|---|
| SPW-01 audience/purpose/evidence | PASS | #1352/#1421, 선행 PR #1512, 기준 SHA, 현재 source anchor와 구현 stop condition을 명시했다. |
| SPW-02 artifact contract | PASS | goal·architecture·파일 소유권·ordered task·테스트·문서·rollback을 포함한다. |
| SPW-03 Korean technical register | PASS | 계획 설명은 한국어로 작성하고 코드·API·명령·경로·식별자 토큰은 보존했다. terminology audit가 통과했다. |
| SPW-04 technical traceability | PASS | 승인 명세 §1–§10 요구를 Task 1–9 및 Step 3-R/3-P/commit/rollback 증거에 연결했다. |
| SPW-05 read-back | PASS | heading·파일 경로·checkbox·code fence·기대 결과를 재독했고 placeholder scan과 `git diff --no-index --check`가 통과했다. |

계획 자체의 writer gate는 통합 Step 3-R review에서 다시 판정하며, P0/P1 발견 시 해당 항목을
수정하고 영향을 받은 렌즈를 재실행한다.

## 11. Commit 순서와 Lore 형식

- [ ] **Step 11.1: 승인 산출물을 먼저 commit한다.**

```bash
git add docs/superpowers/specs/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-design.md \
  docs/review/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-design-review.md \
  docs/superpowers/plans/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-plan.md \
  docs/review/2026-08-27-netcdf-coordinate-axis2d-cf-auxiliary-plan-review.md
git commit -m "좌표축 임포트 구현 범위를 실행 가능한 계약으로 고정"
```

Commit body는 다음 trailer를 포함한다.

```text
Constraint: 기존 NetCDF schema/API와 protected develop을 유지
Rejected: 좌표 전용 테이블과 schema migration | child 범위를 불필요하게 확장
Confidence: high
Scope-risk: broad
Directive: CoordinateReader와 동일 Exposed Connection 경계를 우회하지 말 것
Tested: specification/review/plan terminology audit와 diff check
Not-tested: implementation code and hosted CI
```

- [ ] **Step 11.2: implementation을 기능 경계별로 commit한다.**

권장 순서는 `예외·guard·limits` → `progress fence` → `axis/reader/CRS` → `tile engine` → `serializer/writer` → `service integration` → `fixtures/tests` → `docs/benchmark/lesson`이다. 각 commit마다 `git diff --check`와 해당 Task의 targeted Gradle test를 실행하고 Lore trailers를 포함한다.

## 12. Step 3-R 종료 조건

- 모든 설계 §1–§10 요구가 Task 1–11의 concrete file/task/command에 매핑된다.
- 선행 산출물(spec → plan → tests → implementation → docs)이 역순 의존 없이 실행된다.
- success/failure/edge/concurrency/cancellation/backend/Testcontainers/benchmark 경로가 명시된다.
- schema/settings/BOM/workflow/Kover/Nightly 변경은 N/A 이유가 기록된다. 기존 `slow-netcdf` nightly 실행은 Task 8–9에서 검증한다.
- Exposed deprecated import/receiver shadowing, Kotlin cancellation, resource close, same-connection JDBC 경계는 구현 후 source scan과 테스트로 확인한다.
- plan과 통합 review가 writer SPW-01..05 PASS, 최신 P0=0/P1=0일 때만 Step 3-R PASS로 닫는다.
- Step 3-R PASS와 plan commit 전에는 implementation code를 수정하지 않는다.
