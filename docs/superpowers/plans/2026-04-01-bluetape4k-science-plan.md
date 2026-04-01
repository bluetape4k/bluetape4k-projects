# bluetape4k-science 모듈 구현 계획

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** debop4k-science의 GIS 좌표계, Shapefile, NetCDF 처리 기능을 bluetape4k `utils/science` 모듈로 마이그레이션. 완전 Kotlin화 + 최신 라이브러리 + Exposed/PostGIS DB 적재 파이프라인 제공.

**Architecture:** `utils/science`는 순수 GIS/과학 데이터 라이브러리(DB 무관). DB 적재 기능은 `exposed` 서브패키지에서 `compileOnly`로 exposed-postgresql/exposed-jackson3 연동. Phase 0에서 exposed-postgresql에 `GeoGeometryColumnType` 선행 추가.

**Tech Stack:** Kotlin 2.3, Java 21, JTS 1.20.0, Proj4J 1.3.0, GeoTools 31.6 (LGPL, compileOnly), UCAR netcdfAll 5.6.0, Exposed 1.0+, PostGIS, JUnit 5 + Kluent

**Spec:** `docs/superpowers/specs/2026-04-01-bluetape4k-science-design.md`

---

## 주요 구현 주의사항

1. **GeoTools LGPL 라이선스**: `compileOnly`로만 선언. Shapefile 처리 없이 좌표계/Geometry 연산만 사용하면 GeoTools 불필요.
2. **GeoTools Maven 저장소**: Maven Central에 없음. 루트 `build.gradle.kts`의 `allprojects { repositories }` 블록에 `maven("https://repo.osgeo.org/repository/release/")` 추가 필수.
3. **UCAR 5.x API 변경**: `NetcdfFile.open()` → `NetcdfFiles.open()`, `NetcdfDatasets.openDataset()` 추가.
4. **JTS 패키지 변경**: `com.vividsolutions.jts` → `org.locationtech.jts` (debop4k 소스에서 일괄 치환).
5. **Exposed JSON 컬럼**: `jsonb<T>()` API 없음. `bluetape4k-exposed-jackson3` 모듈의 `jacksonb<T>()` 사용 (JSONB + GIN 인덱스 지원).
6. **Auditable 테이블**: createdAt/updatedAt 필요한 테이블은 `AuditableLongIdTable` 상속 (exposed-core 모듈).
7. **H2 fallback**: PostGIS 전용 기능(GeoGeometryColumnType)은 H2에서 동작 불가. 테스트는 Testcontainers PostgreSQL + PostGIS 사용.
8. **기존 utils/geo와 독립**: geo = geocode/geohash/geoip 서비스, science = 공간분석/과학데이터 처리. 중복 없음.

---

## Phase 0 — 선행 작업: exposed-postgresql GeoGeometry 확장

### T0.1 GeoGeometryColumnType 추가 [complexity: high]

**입력**: `data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoColumnTypes.kt` (기존 GeoPointColumnType, GeoPolygonColumnType 패턴 참고)

**출력**: `data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoColumnTypes.kt` (수정)

**구현 지시사항**:
- [ ] `GeoGeometryColumnType` 클래스 추가 — `ColumnType<net.postgis.jdbc.geometry.Geometry>()` 상속
- [ ] `sqlType()`: PostgreSQL dialect 체크 후 `"GEOMETRY(GEOMETRY, 4326)"` 반환
- [ ] `notNullValueToDB()`: SRID 미설정 시 4326으로 설정 후 `PGgeometry(value)` 래핑
- [ ] `valueFromDB()`: `PGgeometry` → `geometry`, `String` → `PGgeometry` 파싱, 그 외 error
- [ ] KDoc 한국어 주석 (모든 PostGIS geometry 타입을 수용하는 generic 컬럼 타입임을 명시)
- [ ] 기존 `GeoPointColumnType`, `GeoPolygonColumnType`과 동일한 패턴 유지

### T0.2 geoGeometry() 확장함수 + ST_* 오버로드 추가 [complexity: medium]

**입력**: T0.1 완료, `data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoExtensions.kt`

**출력**: `data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoExtensions.kt` (수정)

**구현 지시사항**:
- [ ] `Table.geoGeometry(name: String): Column<Geometry>` 확장함수 추가
- [ ] `Column<Geometry>.stDistance(other)`, `stDWithin(other, distance)` 오버로드 추가
- [ ] `Column<Geometry>.stIntersects(other)`, `stContains(other)` 오버로드 추가 (Geometry ↔ Geometry)
- [ ] `Column<Geometry>.stWithin(other)` 오버로드 추가
- [ ] 기존 Point/Polygon 전용 함수는 그대로 유지 (하위 호환)
- [ ] H2 fallback 불가 → dialect 체크 일관 적용

### T0.3 GeoGeometryColumnType 통합 테스트 [complexity: medium]

**입력**: T0.1, T0.2 완료

**출력**: `data/exposed-postgresql/src/test/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoGeometryColumnTypeTest.kt` (신규)

**구현 지시사항**:
- [ ] Testcontainers PostgreSQL + PostGIS 컨테이너 사용
- [ ] Point, Polygon, LineString, MultiPolygon 타입 저장/조회 테스트
- [ ] `stIntersects()`, `stContains()`, `stDistance()` 쿼리 테스트
- [ ] 기존 `GeoColumnTypeTest.kt` 패턴 참고
- [ ] JUnit 5 + Kluent assertion

---

## Phase 1 — 모듈 기반 구조

### T1.1 buildSrc/Libs.kt에 science 관련 상수 추가 [complexity: low]

**입력**: `buildSrc/src/main/kotlin/Libs.kt`

**출력**: `buildSrc/src/main/kotlin/Libs.kt` (수정)

**구현 지시사항**:
- [ ] `// === Science / GIS ===` 섹션 추가 (기존 `jts_core` 근처)
- [ ] 추가할 상수:
  ```kotlin
  const val ucar_netcdf = "edu.ucar:netcdfAll:5.6.0"
  const val geotools_version = "31.6"
  const val geotools_shapefile = "org.geotools:gt-shapefile:$geotools_version"
  const val geotools_referencing = "org.geotools:gt-referencing:$geotools_version"
  const val geotools_epsg_hsql = "org.geotools:gt-epsg-hsql:$geotools_version"
  const val proj4j = "org.locationtech.proj4j:proj4j:1.3.0"
  const val proj4j_epsg = "org.locationtech.proj4j:proj4j-epsg:1.3.0"
  const val esri_geometry_api = "com.esri.geometry:esri-geometry-api:2.2.4"
  ```
- [ ] 기존 `jts_core`, `postgis_jdbc` 상수는 그대로 유지

### T1.2 루트 build.gradle.kts에 GeoTools Maven 저장소 추가 [complexity: low]

**입력**: `build.gradle.kts` (루트)

**출력**: `build.gradle.kts` (수정)

**구현 지시사항**:
- [ ] `allprojects { repositories { } }` 블록에 `maven("https://repo.osgeo.org/repository/release/")` 추가
- [ ] 기존 `mavenCentral()`, `google()` 뒤에 추가
- [ ] 주석으로 `// GeoTools (LGPL) — Shapefile 처리 시 필요` 명시

### T1.3 utils/science/build.gradle.kts 생성 [complexity: low]

**입력**: T1.1 완료, `utils/geo/build.gradle.kts` (패턴 참고)

**출력**: `utils/science/build.gradle.kts` (신규)

**구현 지시사항**:
- [ ] `configurations { testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get()) }` 패턴 적용
- [ ] `api` 의존성: `bluetape4k-core`, `bluetape4k-logging`, `jts_core`
- [ ] `compileOnly` 의존성: `proj4j`, `proj4j_epsg`, `geotools_shapefile`, `geotools_referencing`, `geotools_epsg_hsql`, `ucar_netcdf`, `esri_geometry_api`
- [ ] `compileOnly` 의존성: `bluetape4k-coroutines`, `kotlinx_coroutines_core`
- [ ] `compileOnly` 의존성 (DB): `bluetape4k-exposed-jdbc`, `bluetape4k-exposed-postgresql`, `bluetape4k-exposed-jackson3`
- [ ] `testImplementation`: `bluetape4k-junit5`, `kotlinx_coroutines_test`

### T1.4 settings.gradle.kts 등록 확인 [complexity: low]

**입력**: `settings.gradle.kts`

**출력**: 확인만 (자동 등록 시 변경 불필요)

**구현 지시사항**:
- [ ] `settings.gradle.kts`의 `includeModules` 자동 등록 로직 확인 — `utils/science` 디렉토리가 `bluetape4k-science`로 자동 등록되는지 검증
- [ ] 자동 등록이 안 되면 수동 등록 추가

---

## Phase 2 — 핵심 GIS 좌표 모듈

### T2.1 GeoLocation.kt [complexity: medium]

**입력**: debop4k-science `gis/coords/GeoLocation.kt` + `.java`

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/coords/GeoLocation.kt` (신규)

**구현 지시사항**:
- [ ] `data class GeoLocation(val latitude: Double, val longitude: Double)` 설계
- [ ] `init` 블록에서 위경도 범위 검증 (`require` 사용): latitude ∈ [-90, 90], longitude ∈ [-180, 180]
- [ ] `ZERO`, `ORIGIN` 등 companion object 상수
- [ ] `distanceTo(other)` 메서드 (Haversine 공식)
- [ ] `AbstractValueObject` 상속 제거 → data class 자동 equals/hashCode
- [ ] `@JvmStatic`, `@JvmOverloads` 제거

### T2.2 BoundingBox.kt + BoundingBoxRelation.kt [complexity: medium]

**입력**: debop4k-science `gis/coords/BoundingBox.kt` + `.java`, `gis/BoundingBoxRelation.kt`

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBox.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/BoundingBoxRelation.kt` (신규)

**구현 지시사항**:
- [ ] `data class BoundingBox(val minLat: Double, val minLon: Double, val maxLat: Double, val maxLon: Double)`
- [ ] `contains(location)`, `intersects(other)`, `union(other)` 메서드
- [ ] `BoundingBoxRelation` enum: `DISJOINT`, `INTERSECTS`, `CONTAINS`, `WITHIN`
- [ ] `relationTo(other: BoundingBox): BoundingBoxRelation` 메서드
- [ ] JTS `Envelope` 변환: `toEnvelope()`, `fromEnvelope()` 확장함수
- [ ] `debop4k.core.utils.min`/`max` → `kotlin.coerceIn()` / `minOf()` / `maxOf()`
- [ ] `Eclipse Collections FastList` → `kotlin.collections.MutableList`

### T2.3 DMS.kt, DM.kt, UtmZone.kt, Vector.kt [complexity: medium]

**입력**: debop4k-science `gis/coords/DM.kt`, `DMS.kt`, `UtmZone.kt`, `Vector.kt` (+ .java 파일)

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/DM.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/DMS.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZone.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/Vector.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/coords/CoordConverters.kt` (신규)

**구현 지시사항**:
- [ ] `data class DM(val degree: Int, val minute: Double)` — 도·분 좌표
- [ ] `data class DMS(val degree: Int, val minute: Int, val second: Double)` — 도·분·초 좌표
- [ ] `data class UtmZone(val zone: Int, val band: Char, val easting: Double, val northing: Double)`
- [ ] `data class Vector(val degree: Double, val distance: Double)` — 방향+거리
- [ ] `CoordConverters.kt`: `Double.toDM()`, `Double.toDMS()`, `DM.toDegree()`, `DMS.toDegree()` 확장함수
- [ ] 모든 Java 클래스 제거, `@Deprecated` 함수 제거

### T2.4 UtmZoneSupport.kt — UTM ↔ WGS84 변환 [complexity: high]

**입력**: debop4k-science `gis/coords/UtmZonex.kt` + `.java`

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/coords/UtmZoneSupport.kt` (신규)

**구현 지시사항**:
- [ ] `utmZoneOf(latitude, longitude)` 팩토리: WGS84 좌표 → UTM Zone 결정
- [ ] `utmZoneOf(zoneString)` 팩토리: 문자열 파싱 ("52N" 형태)
- [ ] `UtmZone.boundingBox(): BoundingBox` — UTM Zone의 위경도 영역 계산
- [ ] `UtmZone.cellBbox(row, col): BoundingBox` — Zone 내 셀 단위 BoundingBox
- [ ] UTM 전략: Zone/Band 판정은 자체 구현, 실제 수치 변환(UTM↔WGS84)은 Proj4J에 위임
- [ ] T2.6 (Projections.kt)에서 Proj4J API를 사용하므로 T2.4는 수치 변환 코드를 직접 구현하지 않음
- [ ] T2.4: Zone/Band 판정 + BoundingBox 계산 담당
- [ ] T2.6: 실제 wgs84ToUtm/utmToWgs84 좌표 변환 담당 (Proj4J)
- [ ] `@Deprecated` 함수 전부 제거, `@JvmStatic` 제거
- [ ] 정확도 단위 테스트: 알려진 좌표(서울: 37.5665°N, 126.9780°E → Zone 52N) 검증

### T2.5 GeometryOperations.kt — 각도/거리/교차점/면적/무게중심 [complexity: high]

**입력**: debop4k-science `gis/coords/Geometryx.kt` + `.java`

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/geometry/GeometryOperations.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/geometry/PolygonExtensions.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/geometry/RotationSupport.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/geometry/BoundingBoxCheck.kt` (신규)

**구현 지시사항**:
- [ ] `angleOf(p1: Point2D, p2: Point2D): Double` — 두 점 사이 각도
- [ ] `distanceOf(p1: Point2D, p2: Point2D): Double` — 유클리드 거리
- [ ] `vectorOf(degree, distance): Vector` — 벡터 생성
- [ ] `Point2D.endPoint(vector): Point2D` — 벡터 끝점 확장함수
- [ ] `Polygon.area(): Double`, `Polygon.centerOfGravity(): Point2D` — JTS Polygon 확장함수
- [ ] `getIntersectPoint(l1, l2): Point2D?` — 두 선분 교차점 (nullable)
- [ ] `getLineNPoints(line, n): List<Point2D>` — 선분 N등분
- [ ] `rotateXYPoint(point, degree, base): Point2D` — 회전 변환
- [ ] `checkPositionPoint(point, bbox): Point2D` — BoundingBox 내 보정
- [ ] `Double.isValidLatitude()`, `Double.isValidLongitude()` — 위경도 유효성 검증
- [ ] `bboxOf(bboxStr): BoundingBox` — 문자열 파싱
- [ ] 기존 Geometryx.kt의 단일 파일을 기능별 4개 파일로 분리

### T2.6 Projections.kt — Proj4J 기반 좌표계 변환 [complexity: medium]

**입력**: debop4k-science `gis/projections/Projections.kt`

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/projection/Projections.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/projection/CrsRegistry.kt` (신규)

**구현 지시사항**:
- [ ] `com.jhlabs:javaproj` → `org.locationtech.proj4j:proj4j:1.3.0` 전환
- [ ] `utmToWgs84(easting, northing, zone): GeoLocation` — UTM → WGS84
- [ ] `wgs84ToUtm(latitude, longitude): UtmZone` — WGS84 → UTM
- [ ] `transform(sourceCrs: String, targetCrs: String, coord: ProjCoordinate): ProjCoordinate` — 임의 CRS 변환
- [ ] `CrsRegistry`: EPSG 코드 기반 CRS 캐시 (`ConcurrentHashMap`)
- [ ] Proj4J `CRSFactory`, `CoordinateTransformFactory` 활용

### T2.7 GIS 좌표 통합 단위 테스트 [complexity: medium]

**입력**: T2.1 ~ T2.6 완료

**출력**:
- `utils/science/src/test/kotlin/io/bluetape4k/science/coords/GeoLocationTest.kt` (신규)
- `utils/science/src/test/kotlin/io/bluetape4k/science/coords/BoundingBoxTest.kt` (신규)
- `utils/science/src/test/kotlin/io/bluetape4k/science/coords/CoordConvertersTest.kt` (신규)
- `utils/science/src/test/kotlin/io/bluetape4k/science/coords/UtmZoneSupportTest.kt` (신규)
- `utils/science/src/test/kotlin/io/bluetape4k/science/geometry/GeometryOperationsTest.kt` (신규)
- `utils/science/src/test/kotlin/io/bluetape4k/science/projection/ProjectionsTest.kt` (신규)

**구현 지시사항**:
- [ ] 서울(37.5665, 126.978), 뉴욕(40.7128, -74.006), 런던(51.5074, -0.1278) 등 알려진 좌표 사용
- [ ] DM ↔ DMS ↔ Degree 왕복 변환 정확도 검증 (epsilon = 1e-6)
- [ ] UTM Zone 결정 정확도 검증
- [ ] Geometry 연산(면적, 교차점, 거리) 수치 검증
- [ ] Proj4J 좌표 변환 왕복 정확도 (UTM → WGS84 → UTM)
- [ ] JUnit 5 + Kluent assertion, `@ParameterizedTest` 활용

---

## Phase 3 — Shapefile 처리

### T3.1 ShapeModels.kt — Kotlin data class 설계 [complexity: medium]

**입력**: debop4k-science `gis/shapefiles/ShapeModels.kt` + Java 클래스들 (`Shape.java`, `ShapeAttribute.java`, `ShapeRecord.java`, `ShapeHeader.java`)

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/shapefile/ShapeModels.kt` (신규)

**구현 지시사항**:
- [ ] `data class ShapeHeader(val fileCode: Int, val fileLength: Int, val shapeType: Int, val bbox: BoundingBox)`
- [ ] `data class ShapeAttribute(val name: String, val type: Char, val length: Int, val decimal: Int)`
- [ ] `data class ShapeRecord(val recordNumber: Int, val shapeType: Int, val bbox: BoundingBox?, val geometry: org.locationtech.jts.geom.Geometry)`
- [ ] `data class Shape(val header: ShapeHeader, val records: List<ShapeRecord>, val attributes: List<ShapeAttribute>)`
- [ ] `FastList` → `List` / `MutableList`
- [ ] `com.vividsolutions.jts` → `org.locationtech.jts` 패키지 변경
- [ ] Java 클래스 모두 Kotlin data class로 통합

### T3.2 ShapefileReader.kt — GeoTools 31.6 마이그레이션 [complexity: high]

**입력**: debop4k-science `gis/shapefiles/ShapeFilex.kt`, T3.1 완료

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/shapefile/ShapefileReader.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/shapefile/ShapefileExtensions.kt` (신규)

**구현 지시사항**:
- [ ] `loadShape(file: File): Shape` — GeoTools ShapefileDataStore 기반 Shapefile 읽기
- [ ] GeoTools `gt-shapefile:15.0` → `31.6` API 변경 대응
  - `ShapefileDataStore` 생성 방식 확인
  - `FeatureSource.getFeatures()` → `FeatureIterator` 순회
- [ ] `com.vividsolutions.jts.geom` → `org.locationtech.jts.geom` 패키지 변경
- [ ] `suspend fun loadShapeAsync(file: File): Shape` — `withContext(Dispatchers.IO)` 래핑
- [ ] 리소스 정리: `try-finally`로 DataStore, FeatureIterator 반드시 close
- [ ] `ShapefileExtensions.kt`: `Shape.toGeoLocations()`, `ShapeRecord.toJtsGeometry()` 등 편의 확장

### T3.3 Shapefile 테스트 [complexity: medium]

**입력**: T3.1, T3.2 완료, T3.4 (테스트 데이터 복사) 완료 후 진행

**출력**: `utils/science/src/test/kotlin/io/bluetape4k/science/shapefile/ShapefileReaderTest.kt` (신규)

**구현 지시사항**:
- [ ] `data/shp_v5/harbors/` Shapefile 읽기 → 레코드 수, 속성 필드 검증
- [ ] `data/shp_v5/oceans/` Shapefile 읽기 → Polygon geometry 타입 검증
- [ ] ShapeHeader 정보 검증 (shapeType, bbox)
- [ ] 파일 없는 경우 예외 처리 검증

### T3.4 테스트 데이터 복사 [complexity: low]

**입력**: debop4k-science `data/shp_v5/`

**출력**: `utils/science/src/test/resources/data/shp_v5/` (복사)

**구현 지시사항**:
- [ ] `data/shp_v5/harbors/` 전체 복사 (.shp, .shx, .dbf, .prj 등)
- [ ] `data/shp_v5/oceans/` 전체 복사
- [ ] 파일 크기 확인 — 1MB 이하면 리포지토리에 포함, 초과 시 `.gitignore` + 다운로드 스크립트

---

## Phase 4 — NetCDF 처리

### T4.1 NetCdfReader.kt — UCAR 5.6.0 마이그레이션 [complexity: high]

**입력**: debop4k-science `netcdf/NetCdfReader.kt`

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/netcdf/NetCdfReader.kt` (신규)

**구현 지시사항**:
- [ ] `NetcdfFile.open()` → `NetcdfFiles.open()` 변경 (UCAR 5.x breaking change)
- [ ] `open(path: String): NetcdfFile` — 파일 열기
- [ ] `openInMemory(path: String): NetcdfFile` — 메모리 로딩 (`NetcdfFiles.openInMemory()`)
- [ ] `canRead(file: File): Boolean` — 파일 읽기 가능 여부
- [ ] object 메서드 → top-level 함수 또는 확장 함수 패턴
- [ ] `Eclipse Collections IntArrayList` → `IntArray`, `DoubleArray` (Kotlin 원시 배열)
- [ ] 리소스 정리: `use {}` 또는 `Closeable` 패턴

### T4.2 NetCdfExtensions.kt — Variable 확장함수 + Flow 스트리밍 [complexity: medium]

**입력**: debop4k-science `netcdf/NetCdfReader.kt` (Variable 관련 부분), T4.1 완료

**출력**:
- `utils/science/src/main/kotlin/io/bluetape4k/science/netcdf/NetCdfExtensions.kt` (신규)
- `utils/science/src/main/kotlin/io/bluetape4k/science/netcdf/NetCdfModels.kt` (신규)

**구현 지시사항**:
- [ ] `Variable.readIntArray(): IntArray` — 정수 배열 읽기 확장함수
- [ ] `Variable.readDoubleArray(): DoubleArray` — 실수 배열 읽기 확장함수
- [ ] `Variable.readNDArray(): ucar.ma2.Array` — N차원 배열 읽기
- [ ] `NetcdfFile.getInformation(): String` — 파일 정보 문자열
- [ ] `NetcdfFile.variableFlow(): Flow<Variable>` — Kotlin Flow 기반 스트리밍
- [ ] `data class NetCdfVariableInfo(val name: String, val dataType: String, val shape: List<Int>, val attributes: Map<String, String>)`
- [ ] `data class NetCdfDimensionInfo(val name: String, val length: Int, val isUnlimited: Boolean)`
- [ ] `NetcdfFile.toVariableInfos(): List<NetCdfVariableInfo>` 확장함수
- [ ] `NetcdfFile.toDimensionInfos(): List<NetCdfDimensionInfo>` 확장함수

### T4.3 NetCDF 테스트 [complexity: medium]

**입력**: T4.1, T4.2 완료, T4.4 (테스트 데이터 복사) 완료 후 진행

**출력**: `utils/science/src/test/kotlin/io/bluetape4k/science/netcdf/NetCdfReaderTest.kt` (신규)

**구현 지시사항**:
- [ ] `data/netcdf/woa05_temp.nc` 파일 읽기 → 변수 목록, 차원 정보 검증
- [ ] Variable 배열 읽기 테스트 (intArray, doubleArray)
- [ ] Flow 기반 Variable 순회 테스트
- [ ] `openInMemory()` 테스트
- [ ] 파일 없는 경우 예외 처리 검증

### T4.4 테스트 데이터 복사 [complexity: low]

**입력**: debop4k-science `data/netcdf/`

**출력**: `utils/science/src/test/resources/data/netcdf/` (복사)

**구현 지시사항**:
- [ ] `woa05_temp.nc` 복사 (소형 데이터)
- [ ] GRIB2 파일은 크기 확인 후 결정
- [ ] 대형 파일은 `.gitignore`에 등록 + README에 다운로드 방법 기재

---

## Phase 5 — DB 적재 파이프라인

> **전제조건**: Phase 0 (GeoGeometryColumnType) 완료 필수

### T5.1 SpatialLayerTable + SpatialFeatureTable [complexity: high]

**입력**: Phase 0 완료, 스펙 Section 8.2

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/SpatialTables.kt` (신규)

**구현 지시사항**:
- [ ] `object SpatialLayerTable : AuditableLongIdTable("spatial_layers")` — 스펙 8.2의 컬럼 정의
  - `name`, `description`, `sourceFile`, `srid`, `geometryType`, `bboxMinX/Y`, `bboxMaxX/Y`, `recordCount`
  - `createdAt`, `updatedAt`, `createdBy`, `updatedBy` → `AuditableLongIdTable` 상속으로 자동 제공
- [ ] `object SpatialFeatureTable : AuditableLongIdTable("spatial_features")` — 대량 import 시에도 감사 추적 필요 (UserContext로 import 사용자 추적)
  - `layerId = reference("layer_id", SpatialLayerTable)`
  - `featureType = varchar("feature_type", 50)`
  - `geom = geoGeometry("geom")` (Phase 0에서 추가한 확장함수)
  - `properties = jacksonb<Map<String, Any?>>("properties")` (`bluetape4k-exposed-jackson3`, JSONB)
  - `name = varchar("name", 255).nullable()`
  - `createdAt/updatedAt/createdBy/updatedBy` → `AuditableLongIdTable` 상속으로 자동 제공
- [ ] `import io.bluetape4k.exposed.postgresql.postgis.geoGeometry` 확인
- [ ] `import io.bluetape4k.exposed.jackson3.jacksonb` 확인

### T5.2 NetCdfFileTable + NetCdfGridValueTable [complexity: high]

**입력**: Phase 0 완료, 스펙 Section 8.2, T4.2 (NetCdfModels)

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfTables.kt` (신규)

**구현 지시사항**:
- [ ] `object NetCdfFileTable : AuditableLongIdTable("netcdf_files")`
  - `filename`, `filePath`, `fileSize`
  - `variables = jacksonb<List<NetCdfVariableInfo>>()` (JSONB)
  - `dimensions = jacksonb<Map<String, Int>>()` (JSONB)
  - `globalAttrs = jacksonb<Map<String, String>>()` (JSONB)
  - `bbox = geoPolygon("bbox").nullable()`, `timeStart`, `timeEnd`
- [ ] `object NetCdfGridValueTable : LongIdTable("netcdf_grid_values")`
  - `fileId = reference("file_id", NetCdfFileTable)`, `variableName`, `location = geoPoint("location")`, `timeIdx`, `value`, `levelIdx`
  - `attrs = jacksonb<Map<String, Any?>>("attrs").nullable()` (JSONB)
- [ ] NetCdfVariableInfo, NetCdfDimensionInfo는 T4.2에서 정의한 data class 재사용

### T5.3 PoiTable [complexity: medium]

**입력**: 스펙 Section 8.2

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/PoiTable.kt` (신규)

**구현 지시사항**:
- [ ] `object PoiTable : AuditableLongIdTable("poi")`
  - `name`, `category`, `location = geoPoint("location")`, `properties = jacksonb<Map<String, Any?>>()` (JSONB)
- [ ] POI는 항상 POINT이므로 `geoPoint()` 사용 (geoGeometry 아님)
- [ ] `PoiRepository : AbstractLongJdbcRepository<PoiRecord, PoiTable>()`

### T5.4 SpatialFeatureRepository — Shapefile → DB 적재 [complexity: high]

**입력**: T5.1 완료, T3.2 (ShapefileReader)

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/SpatialFeatureRepository.kt` (신규)

**구현 지시사항**:
- [ ] `class ShapefileImportService(layerRepo, featureRepo)`
- [ ] `suspend fun importShapefile(file: File, layerName: String, batchSize: Int = 1000): Int`
  1. 파일 파싱: `withContext(Dispatchers.IO) { loadShape(file) }` — 블로킹 I/O 감싸기
  2. `SpatialLayerTable`에 레이어 메타데이터 insert (name, srid, geometryType, bbox, recordCount)
  3. `SpatialFeatureTable`에 각 ShapeRecord를 배치 insert (layerId, geom, properties as JSONB)
  4. 배치 insert 사용 (`BatchInsertStatement` 또는 Exposed `batchInsert {}`)
  5. 배치 크기: 기본 1000건/트랜잭션 (configurable)
- [ ] 트랜잭션 경계: `newSuspendedTransaction(Dispatchers.IO) { ... }` — transaction-per-batch (부분 실패 시 해당 배치만 롤백)
- [ ] 취소 처리: 배치 루프마다 `ensureActive()` 호출
- [ ] 중복 방지: `SpatialLayerTable.name` UNIQUE 제약 → 동일 이름 import 시 `IllegalArgumentException`
- [ ] 재시도: DB 쓰기 실패 시 Resilience4j Retry (3회, 1s 백오프)
- [ ] postgis-jdbc `Geometry` → Exposed column 연동 확인
- [ ] `SpatialLayerRepository : AbstractLongJdbcRepository<SpatialLayerRecord, SpatialLayerTable>()`
- [ ] `SpatialFeatureRepository : AbstractLongJdbcRepository<SpatialFeatureRecord, SpatialFeatureTable>()`
- [ ] `override val table = SpatialLayerTable` / `SpatialFeatureTable`
- [ ] `override fun extractId(entity) = entity.id`

### T5.5 NetCdfRepository — NetCDF → DB 적재 [complexity: high]

**입력**: T5.2 완료, T4.1/T4.2 (NetCdfReader/Extensions)

**출력**: `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/NetCdfRepository.kt` (신규)

**구현 지시사항**:
- [ ] `class NetCdfCatalogService(fileRepo)`
- [ ] `suspend fun registerFile(filePath: String): Long`
  1. `NetCdfReader.open(filePath)` 호출
  2. 변수/차원/글로벌 속성 추출 → `NetCdfVariableInfo`, `NetCdfDimensionInfo`
  3. 공간 범위(bbox) 추출: lat/lon 변수에서 min/max 계산 → Polygon 생성
  4. 시간 범위 추출: time 변수에서 start/end 계산
  5. `NetCdfFileTable`에 insert, 생성된 ID 반환
- [ ] `suspend fun importGridValues(fileId: Long, variableName: String, batchSize: Int = 1000, limit: Int? = null): Int`
  1. 격자 데이터를 `NetCdfGridValueTable`에 배치 insert (기본 1000건/배치)
  2. Flow 기반으로 대용량 데이터 처리
  3. 배치 루프마다 `ensureActive()` 호출 (취소 처리)
  4. `withContext(Dispatchers.IO)` 로 NetCDF 파일 I/O 감싸기
- [ ] `NetCdfFileRepository : AbstractLongJdbcRepository<NetCdfFileRecord, NetCdfFileTable>()`
- [ ] `NetCdfGridValueRepository : AbstractLongJdbcRepository<NetCdfGridValueRecord, NetCdfGridValueTable>()`

### T5.6 DB 파이프라인 통합 테스트 [complexity: medium]

**입력**: T5.1 ~ T5.5 완료

**출력**: `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/SpatialFeatureRepositoryTest.kt` (신규), `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/NetCdfRepositoryTest.kt` (신규)

**구현 지시사항**:
- [ ] Testcontainers PostgreSQL + PostGIS 사용 (`postgis/postgis:16-3.4` 이미지)
- [ ] Shapefile 적재 → `ST_Contains`, `ST_Intersects` 공간 쿼리 검증
- [ ] NetCDF 파일 등록 → 메타데이터 조회 검증
- [ ] POI 삽입 → 반경 검색(`ST_DWithin`) 검증
- [ ] `@Testcontainers`, `@Container` 어노테이션 사용
- [ ] 테스트 데이터: Phase 3/4에서 복사한 Shapefile/NetCDF 사용

---

## Phase 6 — 문서화 및 검증

### T6.1 README.md 작성 [complexity: low]

**입력**: 전체 Phase 완료

**출력**: `utils/science/README.md` (신규)

**구현 지시사항**:
- [ ] 모듈 개요 (GIS 좌표계, Shapefile, NetCDF, DB 적재)
- [ ] 의존성 매트릭스 (스펙 Section 8.5 표 포함)
- [ ] 패키지 구조 설명
- [ ] 사용 예시 (좌표 변환, Shapefile 읽기, NetCDF 읽기, DB 적재)
- [ ] GeoTools LGPL 주의사항
- [ ] GeoTools Maven 저장소 설정 안내

### T6.2 KDoc 한국어 주석 보완 [complexity: low]

**입력**: 전체 Phase 완료

**출력**: 모든 public API 파일 (기존 파일 수정)

**구현 지시사항**:
- [ ] 모든 public class, interface, extension function에 KDoc 한국어 작성
- [ ] `@param`, `@return`, `@throws` 태그 포함
- [ ] 사용 예시 코드 블록 포함 (주요 API)
- [ ] `@see` 태그로 관련 API 상호 참조

### T6.3 bluetape4k-patterns 체크리스트 검증 [complexity: medium]

**입력**: 전체 Phase 완료

**출력**: 검증 결과 (수정 필요 시 해당 파일 수정)

**구현 지시사항**:
- [ ] Companion object → `KLogging()` 패턴 확인
- [ ] `compileOnly` 의존성이 올바르게 선언되었는지 확인
- [ ] `configurations { testImplementation.extendsFrom(compileOnly, runtimeOnly) }` 패턴 확인
- [ ] data class에 `toString()` 커스텀 필요 여부 검토
- [ ] 확장 함수명이 bluetape4k 네이밍 컨벤션(`*Of`, `*Support`, `*Extensions`)과 일치하는지 확인
- [ ] detekt 정적 분석 통과 확인

### T6.4 CLAUDE.md Architecture 섹션 업데이트 [complexity: low]

**입력**: 전체 Phase 완료

**출력**: `CLAUDE.md` (루트, 수정)

**구현 지시사항**:
- [ ] Architecture > Utilities 테이블에 `science` 모듈 추가
  ```
  | `science` | GIS 좌표계 (BoundingBox/UTM/DMS), Shapefile (GeoTools), NetCDF (UCAR), PostGIS DB 적재 |
  ```
- [ ] 의존 라이브러리 특이사항 (GeoTools LGPL, UCAR BSD-3) 주석 추가

---

### T6.5 최종 검증 체크리스트 [complexity: medium]

**입력**: T6.1 ~ T6.4 완료

**출력**: 검증 통과 확인 (수정 필요 시 즉시 수정)

> 이 체크리스트를 모두 통과해야 구현 완료로 간주한다.

#### 빌드 및 컴파일

- [ ] `./gradlew :bluetape4k-science:compileKotlin` 오류 없음
- [ ] `./gradlew :bluetape4k-science:compileTestKotlin` 오류 없음
- [ ] `./gradlew :bluetape4k-science:detekt` 경고/오류 없음
- [ ] IntelliJ `ide_diagnostics` 실행 → import 오류, @Deprecated 경고 없음

#### 테스트

- [ ] `./gradlew :bluetape4k-science:test` 전체 통과
- [ ] GIS 좌표계 단위 테스트 (T2.7): BoundingBox, UTM 변환, Geometry 연산
- [ ] Shapefile 테스트 (T3.3): data/shp_v5 데이터로 로딩 확인
- [ ] NetCDF 테스트 (T4.3): data/netcdf 데이터로 변수 읽기 확인
- [ ] DB 파이프라인 테스트 (T5.6): Testcontainers PostgreSQL+PostGIS로 import → 조회 왕복 확인

#### 공개 API 타입 규칙 (compileOnly 계약)

- [ ] 모든 public 함수의 파라미터/반환 타입에 GeoTools 타입(`SimpleFeature`, `DataStore` 등) 없음
- [ ] 모든 public 함수의 파라미터/반환 타입에 UCAR 타입(`NetcdfFile`, `Variable` 등) 없음
- [ ] 모든 public 함수의 파라미터/반환 타입에 Proj4J 타입(`CoordinateReferenceSystem` 등) 없음
- [ ] 모든 public 함수의 파라미터/반환 타입에 Exposed 테이블/컬럼 타입 없음
  → 위반 시: 해당 의존성을 `implementation` 또는 `api`로 승격하고 스펙 의존성 매트릭스 업데이트

#### Exposed 테이블 설계

- [ ] 모든 테이블이 `AuditableLongIdTable` 또는 `LongIdTable` 상속 (직접 `Table` 상속 없음)
- [ ] `SpatialFeatureTable.geom`이 `geoGeometry()` 사용 (Phase 0 선행 완료 필요)
- [ ] JSON 컬럼 전부 `jacksonb<T>()` 사용 (`jackson<T>()` 아님)
- [ ] `SpatialLayerTable.name`에 `uniqueIndex()` 적용
- [ ] `SpatialFeatureTable.layerId`가 `reference("layer_id", SpatialLayerTable)` 패턴
- [ ] `NetCdfGridValueTable.fileId`가 `reference("file_id", NetCdfFileTable)` 패턴
- [ ] SQL DDL(`SchemaUtils.create()` 생성 결과)과 스펙 Section 7 SQL 초안 컬럼 수 일치 확인

#### Repository 패턴

- [ ] 모든 Repository가 `AbstractLongJdbcRepository<Record, Table>` 상속
- [ ] `override val table` 와 `override fun extractId()` 구현 확인
- [ ] `newSuspendedTransaction(Dispatchers.IO) { ... }` 패턴 일관 사용

#### Import 서비스

- [ ] `importShapefile()`: 파일 파싱이 `withContext(Dispatchers.IO)` 안에 있음
- [ ] `importShapefile()`: 배치 루프에 `ensureActive()` 호출 있음
- [ ] `importShapefile()`: 동일 `layerName` 중복 import 시 `IllegalArgumentException` 발생
- [ ] `importGridValues()`: `batchSize` 파라미터 기본값 1000, Flow 기반 스트리밍
- [ ] 배치 경계: 하나의 트랜잭션이 배치 단위로 commit (단일 거대 트랜잭션 없음)

#### UTM 구현

- [ ] Zone/Band 판정 로직이 `UtmZoneSupport.kt`에 자체 구현
- [ ] 실제 수치 좌표 변환은 `Projections.kt`에서 Proj4J API 위임
- [ ] `UtmZoneSupport`와 `Projections`가 서로 중복 구현 없음

#### 문서화

- [ ] `utils/science/README.md` 존재 및 의존성 매트릭스 포함
- [ ] 모든 public class/interface/extension fun에 KDoc (한국어) 있음
- [ ] 루트 `CLAUDE.md`에 `science` 모듈 추가됨

---

## 병렬 실행 가이드

```
Phase 0 ─────────────────────────────────────────────┐
                                                      │
Phase 1 (T1.1 ~ T1.4) ──┐                            │
                         ├─ Phase 2 (T2.1 ~ T2.7) ─┐ │
                         │                          │ │
                         │  Phase 3 (T3.1 ~ T3.4) ─┤ │
                         │                          │ │
                         │  Phase 4 (T4.1 ~ T4.4) ─┤ │
                         │                          │ │
                         └──────────────────────────┘ │
                                                      │
                         Phase 5 (T5.1 ~ T5.6) ──────┘
                                   │
                         Phase 6 (T6.1 ~ T6.4)
```

- **Phase 1** 완료 후 **Phase 2/3/4**는 병렬 실행 가능 (독립적인 패키지)
- **Phase 5**는 Phase 0 + Phase 2 + Phase 3 + Phase 4 모두 완료 후 진행
- **Phase 6**은 모든 Phase 완료 후 진행
- Phase 0은 다른 Phase와 독립적으로 먼저 시작 가능 (exposed-postgresql 모듈 작업)

## 예상 소요 시간

| Phase | 예상 시간 | 비고 |
|-------|-----------|------|
| Phase 0 | 0.5일 | exposed-postgresql 기존 패턴 참고 |
| Phase 1 | 0.5일 | 빌드 설정 |
| Phase 2 | 1.5일 | 핵심 GIS 로직 + UTM 알고리즘 |
| Phase 3 | 1일 | GeoTools API 변경 대응 |
| Phase 4 | 1일 | UCAR API 변경 대응 |
| Phase 5 | 2일 | DB 적재 + 통합 테스트 |
| Phase 6 | 0.5일 | 문서화 |
| **합계** | **~7일** | |
