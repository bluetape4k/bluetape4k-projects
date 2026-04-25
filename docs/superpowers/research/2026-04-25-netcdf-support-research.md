# NetCdfCatalogService UCAR netCDF-Java 통합 — Pre-Spec Research

- **일자**: 2026-04-25 (v2 refresh)
- **이슈**: #107 (utils/science — NetCdf 지원 완성)
- **대상**: `NetCdfCatalogService.registerFile()` / `importGridValues()`
- **저자**: Explorer agent (pre-spec research)
- **supersedes**: v1 (5.4.x 권장) — Unidata Nexus maven-metadata 직접 조회로 5.9.1 실존 확증
- **⚠️ 주의**: 버전 선택에 관한 최종 결정은 **design spec v3** (`docs/superpowers/specs/2026-04-25-netcdf-support-design.md`) 를 우선한다. 본 research 는 참고용 배경 자료이며, spec 과 버전 불일치 발견 시 spec 이 우선.

---

## 0. Executive summary

- **아티팩트 좌표 오류 수정**: 기존 `Libs.kt:1226`의 `edu.ucar:netcdfAll:5.6.0`은 **Maven에 존재하지 않는 좌표** — Unidata Nexus maven-metadata.xml 직접 조회(2026-04-25) 결과 해당 버전 미존재 확인.
- **실제 최신 release (2026-04-25 Unidata Nexus 직접 조회)**:
  - `edu.ucar:cdm-core` 최신 = **5.9.1** (2025-09-09, lastUpdated=20250909221454)
  - `edu.ucar:netcdf4` 최신 = **5.9.1** (2025-09-09, lastUpdated=20250909221354, HDF5 IOSP)
  - `edu.ucar:netcdfAll` Maven 최신 = **5.5.2** (2022-06-30, lastUpdated=20220630214319) — **주의**: 5.9.x netcdfAll jar는 UCAR 공식 다운로드 페이지/GitHub release에는 존재하나, Maven 배포는 2022년에 멈춤. 즉 "netcdfAll 배포 중단"이 아니라 "Maven 배포만 2022에 멈췄고, 다운로드/GitHub 경로는 계속됨"
- **권장 구성**: `cdm-core:5.9.1` (필수) + `netcdf4:5.9.1` (HDF5/NetCDF-4 지원) — 두 의존성 모두 `compileOnly`
- **저장소**: `build.gradle.kts:65` `allprojects { repositories { maven(unidata-all) } }` 이미 선언됨 — **추가 작업 없음**
- **Virtual Thread**: `registerFile`/`importGridValues` blocking 시그니처 유지 권장. UCAR의 `NetcdfFile.open()` 내부 `synchronized` 블록 → VT pinning 경고 가능하나 허용 범위 내.

---

## 1. 아티팩트 좌표 결정표 (5.9.1 기준 — 2026-04-25 검증)

| 선택 | 좌표 | 용도 | Maven 가용 |
|------|------|------|-----------|
| **필수** | `edu.ucar:cdm-core:5.9.1` | CDM API + NetCDF-3 읽기 | ✅ Unidata Nexus |
| **권장** | `edu.ucar:netcdf4:5.9.1` | HDF5 / NetCDF-4 IOSP | ✅ Unidata Nexus |
| 선택(별도 설치) | `edu.ucar:netcdfAll:*` | OPeNDAP/GRIB/BUFR fat jar | Maven 5.5.2까지. 5.9.x는 GitHub/다운로드 페이지만 |
| ❌ 삭제 | `edu.ucar:netcdfAll:5.6.0` | 없음 (좌표 오류) | 좌표 자체 미존재 |

### 1.1 Maven 저장소 선언

`build.gradle.kts:65` `allprojects { repositories { maven(unidata-all) } }`가 이미 루트에 선언되어 있음. **모듈별 추가 repositories {} 선언 불필요**.

### 1.2 의존성 선언 (compileOnly)

```kotlin
// utils/science/build.gradle.kts
dependencies {
    // NetCDF (UCAR) — compileOnly (BSD-3-Clause)
    // cdm-core: NetCDF-3 + CDM API 기본 (필수)
    // netcdf4: HDF5 / NetCDF-4 IOSP (권장)
    compileOnly(Libs.ucar_cdm_core)
    compileOnly(Libs.ucar_netcdf4)

    testImplementation(Libs.ucar_cdm_core)
    testImplementation(Libs.ucar_netcdf4)
    testRuntimeOnly(Libs.slf4j_jdk14)
}
```

> **주의**: `cdm-core`만으로 로컬 NetCDF-3 파일 읽기는 가능. `netcdf4`는 HDF5/NetCDF-4 포맷 지원 IOSP 추가. 이슈 #107 범위 "NetCDF-3/4 파일 메타데이터/격자 값 읽기"는 두 의존성으로 충분.

### 1.3 Libs.kt 수정 방향

```kotlin
// Science / GIS
// UCAR netCDF-Java: Unidata Nexus 저장소 (루트 build.gradle.kts:65에 이미 선언됨)
//   https://artifacts.unidata.ucar.edu/repository/unidata-all/
const val ucar_cdm_core = "edu.ucar:cdm-core:5.9.1"   // CDM API + NetCDF-3 (필수)
const val ucar_netcdf4  = "edu.ucar:netcdf4:5.9.1"    // HDF5 / NetCDF-4 IOSP (권장)
// 기존 ucar_netcdf = "edu.ucar:netcdfAll:5.6.0" 는 좌표 오류 — 삭제
```

---

## 2. 권장 API 서피스 (CDM 5.x)

> 공식 문서: <https://docs.unidata.ucar.edu/netcdf-java/current/userguide/reading_cdm.html>

| 용도 | API 호출 | 근거 |
|------|---------|------|
| 파일 열기 | `NetcdfFiles.open(path)` (정적 팩터리, `NetcdfFile extends AutoCloseable`) | 문서 명시 "Use the static `NetcdfFiles.open` methods to open a netCDF file" |
| 리소스 해제 | `try-with-resources` / Kotlin `use { }` | AutoCloseable 구현 |
| 전역 속성 | `ncfile.globalAttributes()` → `Iterable<Attribute>` → `attr.getName()` / `attr.getStringValue()` | 표준 CDM API |
| 차원 | `ncfile.dimensions()` → `Iterable<Dimension>`; `.getLength()`, `.isUnlimited()` | NetCdfDimensionInfo에 매핑 가능 |
| 변수 | `ncfile.variables()` → `Iterable<Variable>`; `.getFullName()`, `.getDataType().name`, `.getShape() (int[])`, `.attributes()` | NetCdfVariableInfo에 매핑 가능 |
| 격자 값 | `variable.read()` → `ucar.ma2.Array` + `IndexIterator`/`getIndexIterator()` | 공식 문서 "Manipulating data in Arrays" 섹션 |
| 좌표축 lat/lon | `NetcdfDatasets.openDataset(path)` + `CoordinateAxis1D` (`findCoordinateAxis("lat")`/`"lon"`) | 좌표 해석이 필요하면 enhanced 모드 |
| Feature 접근 (옵션) | `GridDatasets.open(path)` → `GridDataset` → `Grid.readData()` | 고수준 API, 후속 단계 |

### 2.1 registerFile 의사코드

```kotlin
fun registerFile(filePath: String): Long {
    val record = NetcdfFiles.open(filePath).use { nc ->
        val variables = nc.variables.map { v ->
            NetCdfVariableInfo(
                name = v.fullName,
                dataType = v.dataType.name,
                shape = v.shape.toList(),
                attributes = v.attributes.associate { it.name to (it.stringValue ?: it.numericValue?.toString() ?: "") }
            )
        }
        val dimensions = nc.dimensions.associate { it.name to it.length }
        val globalAttrs = nc.globalAttributes.associate { it.name to (it.stringValue ?: it.numericValue?.toString() ?: "") }

        NetCdfFileRecord(
            filename = java.nio.file.Paths.get(filePath).fileName.toString(),
            filePath = filePath,
            fileSize = java.nio.file.Files.size(java.nio.file.Paths.get(filePath)),
            variables = variables,
            dimensions = dimensions,
            globalAttrs = globalAttrs,
        )
    }
    // Virtual Thread + JDBC 트랜잭션 (bluetape4k 관용)
    return suspendTransactionAsync(Dispatchers.IO /* VT pool */) {
        fileRepo.save(record).id
    }.get()  // 또는 blocking transaction { }
}
```

### 2.2 importGridValues 의사코드

```kotlin
fun importGridValues(fileId: Long, variableName: String) {
    val record = transaction { fileRepo.findByIdOrNull(fileId) } ?: error("file not found: $fileId")
    NetcdfDatasets.openDataset(record.filePath).use { ncd ->
        val v = ncd.findVariable(variableName) ?: error("variable not found")
        val latAxis = ncd.findCoordinateAxis("lat") as? CoordinateAxis1D
        val lonAxis = ncd.findCoordinateAxis("lon") as? CoordinateAxis1D
        // 대용량 대비 — time 축 한 슬라이스씩 읽어 batchInsert
        val shape = v.shape
        (0 until shape[0]).forEach { timeIdx ->
            val slice = v.read(intArrayOf(timeIdx, 0, 0), intArrayOf(1, shape[1], shape[2]))
            transaction {
                NetCdfGridValueTable.batchInsert(slice.chunkedIndices()) { (i, j, value) ->
                    this[NetCdfGridValueTable.fileId] = fileId
                    this[NetCdfGridValueTable.variableName] = variableName
                    this[NetCdfGridValueTable.location] = geoPointOf(lonAxis!!.getCoordValue(j), latAxis!!.getCoordValue(i))
                    this[NetCdfGridValueTable.timeIdx] = timeIdx
                    this[NetCdfGridValueTable.levelIdx] = 0
                    this[NetCdfGridValueTable.value] = value
                }
            }
        }
    }
}
```

---

## 3. bluetape4k 재사용 컴포넌트

| 경로 | 심볼 | 판정 | 근거 |
|------|------|------|------|
| `data/exposed-postgresql/…/postgis/GeoPoint.kt` | `geoPoint`, `geoPolygon` column helpers | **채택(as-is)** | `NetCdfTables.kt`가 이미 사용 중 |
| `data/exposed-postgresql/…/postgis/` | `ST_MakePoint` / `geoPointOf(lon, lat)` | **채택(as-is)** | 격자 셀당 Point 삽입용 |
| `data/exposed-jdbc/…` | `batchInsert` (Exposed v1 API) | **채택(as-is)** | `importGridValues` 대량 insert 필수 |
| `data/exposed-jdbc/…/repository/LongJdbcRepository` | 기존 `NetCdfFileRepository` 상속 | **채택(as-is)** | save/findAll/findByIdOrNull 기구현 |
| `bluetape4k-core` | `KLogging`, `Serializable` 패턴 | **채택(as-is)** | 모델 3개 이미 규약 준수 (serialVersionUID 1L, companion object) |
| `virtualthread/api`, `virtualthread/jdk21|25` | VT dispatcher | **부분 차용** | `transaction {}` 자체가 VT 풀 위에서 돌도록 테스트 컨텍스트에서만 보장 필요 |
| `utils/science` 기존 | `bluetape4k-exposed-postgresql` / `bluetape4k-exposed-jackson3` | **채택(as-is)** | testImplementation 체인 기존대로 |

### 3.1 compileOnly + optional 패턴 선례

`data/exposed-tink/build.gradle.kts`, `utils/science/build.gradle.kts` (proj4j/geotools), `io/protobuf`, `data/exposed-jdbc-redisson` 등이 동일 패턴 사용.
**특징**: `compileOnly(...)` + `testImplementation(...)` 이중 선언. 런타임 classpath에 없으면 `ClassNotFoundException`이 자연스럽게 발생.
별도의 `Class.forName` 체크 계층은 **없음** — 사용자가 의존성을 직접 추가하지 않으면 UCAR API 사용 메서드 호출 시점에 예외 발생하는 게 기존 bluetape4k 관용.

### 3.2 Virtual Thread 래핑 패턴

- 프로젝트 관용: `data/exposed-jdbc`는 blocking `transaction { }` 그대로 사용 → Spring Boot 3/4의 VT thread pool이 픽업.
- 이슈 #107 요구사항 "Virtual Thread에서 JDBC 트랜잭션 실행" → `transaction { }` 블록을 그대로 유지하고, 호출자가 VT executor에서 호출하도록 설계. `suspend` 변환은 불필요.

---

## 4. 테스트 전략

### 4.1 현재 상태

- `utils/science/src/test/resources/data/` — Shapefile(`.shp`, `.dbf` 등) 다수, **NetCDF `.nc` 샘플 없음**.
- 기존 테스트 `NetCdfTableTest.kt` — DDL/CRUD만 검증, UCAR 미사용.

### 4.2 권장 전략: 런타임 생성 + 체크인 결합

| 방법 | 사용 시점 | 근거 |
|------|----------|------|
| **ucar.nc2.write.NetcdfFormatWriter** (cdm-core 포함) 로 테스트에서 동적 생성 | 단위 테스트 — tiny 3D grid (`time=2, lat=3, lon=4`) | <100KB, 외부 파일 의존 X, 라이선스 이슈 X |
| Unidata `cdmUnitTest` 공식 샘플 일부 (BSD-3-Clause) | 통합 테스트만 | 실제 CF 준수 파일 필요 시 |
| 실제 ERA5 subset 파일 (`era5_2023_tiny.nc` ~50KB) | 회귀 테스트 | 라이선스 Copernicus License (재배포 가능 조건 확인 필요) |

**1차 권장**: `NetcdfFormatWriter`로 `@TempDir`에 생성 → register → importGridValues → assertion. 리소스 체크인 불필요.

### 4.3 선택적 활성화 (optional classpath gating)

bluetape4k 관용상 `@EnabledIfSystemProperty` / `Class.forName` 게이트는 **사용하지 않음**. 테스트가 `testImplementation(Libs.ucar_cdm_core)` + `testImplementation(Libs.ucar_netcdf4)`에 의존하므로, UCAR jar 미존재 시 테스트 컴파일 자체가 실패 → 사용자가 저장소 설정을 안 하면 테스트가 돌지 않음.

**대안(옵션)**: 저장소 접근 실패 대비 CI-only 프로파일로 격리 — `./gradlew :bluetape4k-science:test -PskipUcarTests` 플래그 지원.

---

## 5. Opt-in 설계 (cdm-core + netcdf4)

| 레이어 | 선택 |
|-------|------|
| **bluetape4k 코드** | `ucar.nc2.NetcdfFiles`, `ucar.nc2.Variable`, `ucar.ma2.Array`, `ucar.nc2.dataset.NetcdfDatasets`, `ucar.nc2.dataset.CoordinateAxis1D` **만** 사용 — 이 모든 심볼은 `cdm-core` 포함 |
| **기본 의존성** | `compileOnly(Libs.ucar_cdm_core)` + `compileOnly(Libs.ucar_netcdf4)` |
| **사용자 Full 전환** | 애플리케이션 build.gradle.kts에 `implementation("edu.ucar:netcdfAll:5.5.2")` (Maven 최신) 또는 GitHub에서 직접 5.9.x jar 취득 → cdm-core API 동일하게 동작 + 추가 IOSP 자동 등록 |
| **테스트** | `testImplementation(Libs.ucar_cdm_core)` + `testImplementation(Libs.ucar_netcdf4)` + `testRuntimeOnly(Libs.slf4j_jdk14)` |

**결정 포인트**: 우리 코드가 CDM 5.x surface만 쓰면 lite/full이 binary-compatible (fat jar는 같은 `ucar.nc2.*` 패키지 제공). 사용자가 원하는 포맷 지원 범위를 직접 택.

---

## 6. 위험 / 실패 모드 (구체)

1. **Unidata Nexus 접속 불가 / 느림** — CI에서 첫 빌드 시 다운로드 타임아웃 가능. 완화: `maven-publish` 미러 캐시 또는 Gradle `dependency-verification`.
2. **Virtual Thread pinning**: CDM 5.x의 `NetcdfFile.open` 내부에서 `synchronized` 블록 사용 (RandomAccessFile 캐싱). JDK 21 VT에서 carrier thread pinning 로그 발생 가능. 완화: `registerFile` 호출은 소수 · 짧음 → 허용 가능. 대용량 `importGridValues`는 `Dispatchers.IO` (platform thread)로 폴백 고려.
3. **cdm-core 5.9.1 / netcdf4 5.9.1 transitive dep CVE 검토 필요**: 5.9.1 transitive 의존성 중 protobuf-java, gson 등 CVE 가능성 있음. 프로덕션 배포 시 `dependencyResolutionManagement`로 강제 버전 업 필요.
4. **netcdfAll Maven 5.5.2 vs 5.9.x 불일치**: `netcdfAll:5.5.2`는 Maven에서 사용 가능하나 cdm-core 5.9.1과 버전 불일치. 가능하면 `netcdf4:5.9.1`로 HDF5 지원을 확보하고 netcdfAll 의존은 최소화.
5. **대용량 변수 OOM**: 3D 변수 `time=8760 × lat=721 × lon=1440 × 4byte ≈ 36GB`. 단일 `variable.read()` 금지. time 축 한 슬라이스씩 `Section`으로 청크 읽기 필수.
6. **POINT WKT 좌표 순서**: PostGIS `POINT(lon lat)`는 lon 먼저. `NetCdfGridValueTable.location` 삽입 시 반드시 `geoPointOf(lon, lat)` — lat/lon 순서 뒤집으면 bounds 쿼리 틀림.
7. **좌표축 없는 파일**: `findCoordinateAxis("lat")`가 null 반환 가능 (IOSP가 좌표 추론 실패). 이 경우 `location`을 nullable로 처리하거나 스킵 정책 필요 (스키마 재검토).

---

## 7. 미해결 / 사용자 결정 필요

1. **버전 선택**: `cdm-core:5.9.1` + `netcdf4:5.9.1` 기본값 확정. `Libs.kt` 상수 두 개 추가 권장.
2. **netcdfAll Maven 5.5.2 채택 여부**: GRIB/BUFR/OPeNDAP 지원 필요 시 `netcdfAll:5.5.2` 추가 or GitHub 5.9.x jar 직접 사용 — 이슈 #107 스코프 외.
3. **`NetCdfCatalogService` 시그니처**: blocking `fun registerFile` 유지 vs `suspend fun registerFile`? → 이슈 #107 `"Virtual Thread에서 JDBC 트랜잭션 실행"` 문구는 **blocking 유지 + 호출자가 VT executor 선택** 가능성이 더 높음. 사용자 확인 필요.
4. **테스트 데이터 전략**: `NetcdfFormatWriter` 동적 생성 (권장) vs 체크인된 tiny `.nc` 샘플? → 동적 생성이 더 가볍지만 체크인 샘플이 회귀 추적에 유리. 사용자 결정.
5. **좌표축 없는 파일 처리**: `location` nullable로 스키마 변경? 현재 `geoPoint NOT NULL`. → 스키마 재검토 필요.
6. **`NetCdfGridValueTable.levelIdx`**: 현재 1 (fixed). 4D 변수 (time, level, lat, lon) 지원하려면 level 축 반복 로직 추가 필요 — 이슈 #107 스코프 확인 요.
7. **optional logger**: cdm-core는 `slf4j-api`에만 의존. bluetape4k-logging 이미 `slf4j-api` 포함 → **별도 logger 바인딩 불필요**. 사용자 애플리케이션이 책임.

---

## 8. 참고 출처

- [UCAR netCDF-Java current — Using netCDF-Java Maven Artifacts](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/using_netcdf_java_artifacts.html)
- [Unidata News — netCDF-Java Version 5.9.1 Released](https://www.unidata.ucar.edu/news/netcdf-java-version-591-released)
- [UCAR netCDF-Java current — NetcdfFile reading CDM files](https://docs.unidata.ucar.edu/netcdf-java/current/userguide/reading_cdm.html)
- [GitHub — Unidata/netcdf-java releases](https://github.com/Unidata/netcdf-java/releases)
- Unidata Nexus maven-metadata.xml 직접 조회 결과 (2026-04-25, 본 세션 검증):
  - `cdm-core` latest=5.9.1, lastUpdated=20250909221454
  - `netcdf4` latest=5.9.1, lastUpdated=20250909221354
  - `netcdfAll` latest=5.5.2, lastUpdated=20220630214319 (Maven 배포만 멈춤)

### 코드 지식베이스 (재조회 가능)

- `batch:NetCdfCatalogService current state,NetCdfTables schema,NetCdfModels records` — 프로젝트 소스 스냅샷
- `UCAR netcdf-java current maven artifacts doc` — 저장소 URL + 모듈 구성 (5.9.1 기준)
- `UCAR netcdf-java reading CDM files NetcdfFile NetcdfFiles API` — 읽기 API
- `Unidata Nexus maven-metadata cdm-core netcdf4 netcdfAll version 5.9.1` — 직접 조회 결과

---

## 9. 구현 다음 단계 (요약)

1. `Libs.kt` — 기존 `ucar_netcdf = "edu.ucar:netcdfAll:5.6.0"` 삭제 (좌표 오류), `ucar_cdm_core = "edu.ucar:cdm-core:5.9.1"` + `ucar_netcdf4 = "edu.ucar:netcdf4:5.9.1"` 추가.
2. `utils/science/build.gradle.kts` — `compileOnly(Libs.ucar_cdm_core)` + `compileOnly(Libs.ucar_netcdf4)` + `testImplementation(...)` 활성화. **저장소 추가 불필요** (루트에 이미 선언됨).
3. `NetCdfCatalogService.registerFile` — `NetcdfFiles.open` + attr/dim/var 추출 + `fileRepo.save`.
4. `NetCdfCatalogService.importGridValues` — `NetcdfDatasets.openDataset` + `CoordinateAxis1D` + time slice chunked batchInsert.
5. 테스트 — `NetcdfFormatWriter` 기반 tiny NC 동적 생성, `@TempDir` 사용, DDL/CRUD 기존 테스트 유지.
6. CoordinateAxis/4D 처리 미지원 케이스 → 예외 vs 스킵 정책 문서화.
