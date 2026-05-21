# Module bluetape4k-science

[English](./README.md) | 한국어

GIS 좌표 변환, Shapefile 처리, JTS 도형 연산, PostGIS 데이터베이스 파이프라인, NetCDF 메타데이터 카탈로그를 통합 제공하는 Kotlin 모듈입니다.

## 개요

`bluetape4k-science`는 다섯 가지 도메인을 다룹니다:

| # | 도메인 | 핵심 라이브러리 | 상태 |
|---|--------|---------------|------|
| 1 | **GIS 좌표 변환** | Proj4J, proj4j-epsg | ✅ 구현 완료 |
| 2 | **Shapefile 처리** | GeoTools (LGPL) | ✅ 구현 완료 |
| 3 | **JTS 도형 연산** | JTS Core | ✅ 구현 완료 |
| 4 | **PostGIS 데이터 파이프라인** | Exposed + PostGIS | ✅ 구현 완료 |
| 5 | **NetCDF 메타데이터 카탈로그** | UCAR netCDF-Java (스키마 + 저장소만) | ⚠️ 부분 구현 |

> **NetCDF 현황**: `NetCdfFileTable`, `NetCdfGridValueTable`, `NetCdfFileRepository`, 모든 모델 클래스는
> 완전히 구현되어 테스트까지 통과했습니다. `NetCdfCatalogService`(실제 `.nc` 파일 읽기)는
> `edu.ucar:netcdfAll` 의존성 문제로 Phase 5에 구현 예정입니다.

---

## 아키텍처

### 통합 모듈 구조

![science Architecture diagram](../../docs/images/readme-diagrams/utils-science-diagram-01.png)

### 좌표 변환 흐름

![Coordinate Transformation Flow diagram](../../docs/images/readme-diagrams/utils-science-diagram-02.png)

### PostGIS + NetCDF 데이터베이스 스키마

![PostGIS + NetCDF diagram](../../docs/images/readme-diagrams/utils-science-diagram-03.png)

---

## 모듈 레이아웃

### 패키지 구조

```
io.bluetape4k.science/
├── coords/                          — 좌표 기본 타입
│   ├── GeoLocation.kt              — WGS84 위경도, Haversine 거리
│   ├── BoundingBox.kt              — 사각형 경계 영역, contains/intersects
│   ├── BoundingBoxRelation.kt      — 경계 관계 계산
│   ├── DM.kt / DMS.kt              — 도분 / 도분초 표기법
│   ├── Vector.kt                   — 2D/3D 벡터
│   ├── UtmZone.kt                  — UTM Zone 데이터 클래스
│   ├── UtmZoneSupport.kt           — utmZoneOf(), boundingBox()
│   └── CoordConverters.kt          — 십진도 ↔ DM/DMS 변환 유틸리티
│
├── projection/                      — 좌표계 변환 (Proj4J)
│   ├── CrsRegistry.kt              — EPSG/Proj4 CRS 레지스트리 (인스턴스 캐싱)
│   └── Projections.kt              — wgs84ToUtm(), utmToWgs84(), transform()
│
├── shapefile/                       — Shapefile I/O (GeoTools)
│   ├── ShapeModels.kt              — Shape, ShapeRecord, ShapeHeader (GeoTools 미노출 공개 API)
│   ├── ShapefileReader.kt          — 동기 읽기
│   └── ShapefileExtensions.kt      — loadShape(), loadShapeAsync()
│
├── geometry/                        — 공간 기하학 (JTS)
│   ├── GeometryOperations.kt       — intersection, union, buffer, simplify, distance
│   └── PolygonExtensions.kt        — 면적, 둘레
│
└── exposed/                         — 데이터베이스 파이프라인
    ├── model/
    │   ├── SpatialModels.kt        — SpatialLayerRecord, SpatialFeatureRecord
    │   └── NetCdfModels.kt         — NetCdfFileRecord, NetCdfVariableInfo, NetCdfDimensionInfo
    ├── schema/
    │   ├── SpatialTables.kt        — SpatialLayerTable, SpatialFeatureTable
    │   ├── PoiTable.kt             — 관심 지점(POI) 테이블
    │   └── NetCdfTables.kt         — NetCdfFileTable, NetCdfGridValueTable
    ├── repository/
    │   ├── SpatialFeatureRepository.kt — 공간 피처 CRUD + bbox 검색
    │   └── NetCdfFileRepository.kt — NetCDF 파일 메타데이터 CRUD
    └── service/
        ├── ShapefileImportService.kt — Virtual Thread 배치 임포트
        └── NetCdfCatalogService.kt  — ⚠️ Phase 5 — 플레이스홀더만 존재
```

---

## 핵심 기능

| 도메인 | 기능 | API |
|--------|------|-----|
| **좌표 기본 타입** | WGS84 위경도 + Haversine 거리 | `GeoLocation.distanceTo()` |
| | 사각형 경계 영역 | `BoundingBox.contains()`, `.intersects()` |
| | 도분초 표기법 | `DMS.parse()`, `.toDecimal()` |
| | UTM Zone 자동 판정 | `utmZoneOf(lat, lon)` |
| | 2D/3D 벡터 연산 | `Vector(x, y, z?)` |
| **좌표계 변환** | WGS84 ↔ UTM 변환 | `wgs84ToUtm()`, `utmToWgs84()` |
| | 임의 EPSG 간 변환 | `transform(x, y, srcEpsg, tgtEpsg)` |
| | CRS 인스턴스 캐싱 | `CrsRegistry` |
| **Shapefile** | 동기 Shapefile 읽기 | `loadShape(file)` |
| | 코루틴 기반 비동기 읽기 | `loadShapeAsync(file)` |
| | 타입 안전 모델 (GeoTools 미노출) | `Shape`, `ShapeRecord` |
| **도형 연산** | JTS 교집합 / 합집합 / 차집합 | `GeometryOperations.intersection()` |
| | 버퍼 영역 생성 | `GeometryOperations.buffer()` |
| | Douglas-Peucker 단순화 | `GeometryOperations.simplify()` |
| | 거리 계산 | `GeometryOperations.distance()` |
| **데이터베이스** | 공간 레이어 + 피처 CRUD | `SpatialLayerRepository`, `SpatialFeatureRepository` |
| | Virtual Thread 배치 Shapefile 임포트 | `ShapefileImportService` |
| | NetCDF 파일 메타데이터 카탈로그 | `NetCdfFileRepository` ✅ |
| | NetCDF 격자 값 저장 스키마 | `NetCdfGridValueTable` ✅ |
| | `.nc` 파일 임포트 서비스 | `NetCdfCatalogService` ⚠️ Phase 5 |

---

## 빠른 시작 (Quick Start)

### 5.1 GIS 좌표 변환

```kotlin
import io.bluetape4k.science.coords.GeoLocation
import io.bluetape4k.science.coords.BoundingBox
import io.bluetape4k.science.coords.DMS
import io.bluetape4k.science.coords.utmZoneOf
import io.bluetape4k.science.projection.wgs84ToUtm
import io.bluetape4k.science.projection.utmToWgs84
import io.bluetape4k.science.projection.transform

val seoul = GeoLocation(latitude = 37.5665, longitude = 126.9780)
val tokyo = GeoLocation(latitude = 35.6762, longitude = 139.6503)

// Haversine 거리 계산
val distanceKm = seoul.distanceTo(tokyo) / 1000.0
println("서울 ↔ 도쿄: $distanceKm km")

// 경계 영역 확인
val seoulArea = BoundingBox(minLat = 37.4, maxLat = 37.6, minLon = 126.8, maxLon = 127.0)
println("서울 시청 포함 여부: ${seoulArea.contains(seoul)}")
println("중심: ${seoulArea.center}, 너비: ${seoulArea.widthKm} km")

// 도분초 파싱
val dms = DMS.parse("37°33'59.4\"N")
println("도분초 → 십진도: ${dms.toDecimal()}")  // 37.5665

// UTM Zone 자동 판정
val zone = utmZoneOf(37.5665, 126.9780)
println("서울 UTM Zone: ${zone.longitudeZone}${zone.hemisphere}")  // 52S

// WGS84 ↔ UTM 왕복 변환
val (easting, northing) = wgs84ToUtm(seoul)
val restored = utmToWgs84(easting, northing, zone)
println("UTM 복원 좌표: $restored")

// 임의 EPSG 변환 (WGS84 → 한국 2000 중부원점)
val (kx, ky) = transform(x = 126.9780, y = 37.5665, sourceEpsg = 4326, targetEpsg = 5179)
println("EPSG:4326 → EPSG:5179: ($kx, $ky)")
```

### 5.2 Shapefile 처리

```kotlin
import io.bluetape4k.science.shapefile.loadShape
import io.bluetape4k.science.shapefile.loadShapeAsync
import java.io.File

// 동기 읽기
val shapeFile = File("/data/provinces.shp")
val shape = loadShape(shapeFile, charset = Charsets.UTF_8)
println("파일 타입: ${shape.shapeType}, 레코드 수: ${shape.recordCount}")

shape.records.forEach { record ->
    println("도형 타입: ${record.geometry.geometryType}")
    println("속성: ${record.attributes}")
}

// 비동기 읽기 (Dispatchers.IO 사용)
suspend fun processAsync() {
    val large = loadShapeAsync(File("/data/large_dataset.shp"))
    println("로드 완료: ${large.recordCount} 레코드")
}
```

### 5.3 JTS 도형 연산

```kotlin
import io.bluetape4k.science.geometry.GeometryOperations
import org.locationtech.jts.io.WKTReader

val wkt = WKTReader()
val poly1 = wkt.read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
val poly2 = wkt.read("POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))")

val intersection = GeometryOperations.intersection(poly1, poly2)   // 교집합
val union        = GeometryOperations.union(poly1, poly2)          // 합집합
val buffered     = GeometryOperations.buffer(poly1, 100.0)         // 100m 버퍼
val simplified   = GeometryOperations.simplify(poly1, 1.0)         // Douglas-Peucker
val distance     = GeometryOperations.distance(poly1, poly2)
println("거리: $distance m")
```

### 5.4 PostGIS 데이터 파이프라인

```kotlin
import io.bluetape4k.science.exposed.service.ShapefileImportService
import io.bluetape4k.science.exposed.repository.SpatialLayerRepository
import io.bluetape4k.science.exposed.repository.SpatialFeatureRepository
import org.jetbrains.exposed.sql.Database
import java.io.File

val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/gis_db",
    driver = "org.postgresql.Driver",
    user = "postgres",
    password = "password"
)

val service = ShapefileImportService(SpatialLayerRepository(), SpatialFeatureRepository())
val importedCount = service.importShapefile(
    file = File("/data/harbors.shp"),
    layerName = "harbors-2024"
)
println("임포트 완료: $importedCount 레코드")
```

### 5.5 NetCDF 메타데이터 카탈로그

DB 스키마와 저장소가 완성되어 있습니다. `NetCdfFileRepository`로 파일 메타데이터를 직접 등록하세요.

```kotlin
import io.bluetape4k.science.exposed.model.NetCdfFileRecord
import io.bluetape4k.science.exposed.model.NetCdfVariableInfo
import io.bluetape4k.science.exposed.repository.NetCdfFileRepository
import org.jetbrains.exposed.sql.transactions.transaction

val repo = NetCdfFileRepository()

transaction {
    val record = NetCdfFileRecord(
        filename = "ERA5_2024_01.nc",
        filePath = "/data/era5/ERA5_2024_01.nc",
        fileSize = 104_857_600L,
        variables = listOf(
            NetCdfVariableInfo(
                name = "temperature",
                dataType = "float",
                shape = listOf(24, 181, 360),
                attributes = mapOf("units" to "K", "long_name" to "Air Temperature")
            ),
            NetCdfVariableInfo(
                name = "precipitation",
                dataType = "float",
                shape = listOf(24, 181, 360),
                attributes = mapOf("units" to "mm", "long_name" to "Total Precipitation")
            )
        ),
        dimensions = mapOf("time" to 24, "lat" to 181, "lon" to 360),
        globalAttrs = mapOf("Conventions" to "CF-1.8", "institution" to "ECMWF")
    )

    val saved = repo.save(record)
    println("저장 완료: id=${saved.id}, filename=${saved.filename}")

    val found = repo.findByIdOrNull(saved.id)
    println("변수 목록: ${found?.variables?.map { it.name }}")  // [temperature, precipitation]
    println("시간 스텝: ${found?.dimensions?.get("time")}")      // 24
}
```

---

## API 가이드

### coords

| 클래스 / 함수 | 설명 |
|--------------|------|
| `GeoLocation(lat, lon)` | WGS84 좌표; `.distanceTo()` — Haversine 거리 (미터) |
| `BoundingBox(minLat, maxLat, minLon, maxLon)` | 사각형 경계; `.contains()`, `.intersects()` |
| `DMS.parse(str)` / `DM.parse(str)` | 도분초 / 도분 문자열 파싱 |
| `UtmZone(zone, hemisphere)` | UTM Zone 데이터 클래스 |
| `utmZoneOf(lat, lon)` | WGS84 좌표로 UTM Zone 자동 판정 |
| `Vector(x, y, z?)` | 2D/3D 벡터 + 산술 연산 |

### projection

| 함수 | 설명 |
|------|------|
| `wgs84ToUtm(geoLocation)` | WGS84 → UTM (easting, northing) |
| `utmToWgs84(e, n, zone)` | UTM → WGS84 |
| `transform(x, y, srcEpsg, tgtEpsg)` | 임의 EPSG 간 좌표 변환 |
| `CrsRegistry` | EPSG 코드별 CRS 인스턴스 캐시 (스레드 안전) |

### shapefile

| 함수 | 설명 |
|------|------|
| `loadShape(file, charset?)` | 동기 Shapefile 읽기 |
| `loadShapeAsync(file, charset?)` | 코루틴 비동기 읽기 (`Dispatchers.IO`) |
| `Shape` | 파일 메타데이터 + 레코드 목록 |
| `ShapeRecord` | 도형 + 속성 맵 (GeoTools 타입 미노출) |

### geometry

| 함수 | 설명 |
|------|------|
| `GeometryOperations.intersection(a, b)` | 교집합 |
| `GeometryOperations.union(a, b)` | 합집합 |
| `GeometryOperations.buffer(g, dist)` | 지정 거리 버퍼 영역 생성 |
| `GeometryOperations.simplify(g, tol)` | Douglas-Peucker 단순화 |
| `GeometryOperations.distance(a, b)` | 도형 간 최소 거리 |
| `Polygon.area()` / `.perimeter()` | 면적 / 둘레 확장 함수 |

### exposed (PostGIS)

| 클래스 | 설명 |
|--------|------|
| `SpatialLayerRepository` | 레이어 CRUD (`save`, `findByName`) |
| `SpatialFeatureRepository` | 피처 CRUD + PostGIS bbox 검색 |
| `ShapefileImportService` | Virtual Thread 배치 Shapefile 임포트 |

### exposed (NetCDF)

| 클래스 | 상태 | 설명 |
|--------|------|------|
| `NetCdfFileRecord` | ✅ | 파일 메타데이터 모델 (filename, path, size, variables, dimensions) |
| `NetCdfVariableInfo` | ✅ | 변수 기술자 (name, dataType, shape, attributes) |
| `NetCdfDimensionInfo` | ✅ | 차원 기술자 (name, length, isUnlimited) |
| `NetCdfFileRepository` | ✅ | 파일 메타데이터 CRUD (`save`, `findByIdOrNull`, `findAll`, `deleteById`) |
| `NetCdfFileTable` | ✅ | JSONB 컬럼 + PostGIS bbox + 시간 범위 |
| `NetCdfGridValueTable` | ✅ | 격자 값 테이블 (location: PostGIS POINT, value, timeIdx, levelIdx) |
| `NetCdfCatalogService` | ⚠️ Phase 5 | 플레이스홀더 — `NotImplementedError` 발생 (`netcdfAll` 해결 후 구현) |

---

## 설치 및 의존성

`bluetape4k-science`는 기능별 선택 의존성을 `compileOnly`로 선언합니다.
**필요한 라이브러리만 런타임 의존성으로 추가하세요.**

### 기본 설치

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
}
```

### GIS 좌표 변환 (Proj4J)

```kotlin
implementation(Libs.proj4j)
implementation(Libs.proj4j_epsg)
```

### Shapefile 읽기 (GeoTools — LGPL)

```kotlin
repositories {
    maven(url = "https://repo.osgeo.org/repository/release/") { name = "OSGeo Release" }
}
dependencies {
    implementation(Libs.geotools_shapefile)
    implementation(Libs.geotools_referencing)
    implementation(Libs.geotools_epsg_hsql)
}
```

> **라이선스**: GeoTools는 LGPL입니다. `bluetape4k-science`는 `compileOnly`로 선언하므로
> 배포 시 포함되지 않습니다. GeoTools 클래스를 재배포하려면 LGPL을 준수해야 합니다.

### 공간 기하학 (JTS)

```kotlin
implementation(Libs.jts_core)
```

### PostGIS 데이터베이스

```kotlin
implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:${bluetape4kVersion}")
implementation(Libs.postgis_jdbc)
```

### 비동기 처리 (Coroutines)

```kotlin
implementation("io.github.bluetape4k:bluetape4k-coroutines:${bluetape4kVersion}")
implementation(Libs.kotlinx_coroutines_core)
```

### NetCDF (Phase 5 — 현재 불필요)

`NetCdfCatalogService` 구현 완료 시 필요:

```kotlin
repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/") { name = "Unidata" }
}
dependencies {
    implementation("edu.ucar:netcdfAll:5.6.0")
}
```

### 전체 의존성 예시

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
    implementation(Libs.proj4j)
    implementation(Libs.proj4j_epsg)
    implementation(Libs.geotools_shapefile)
    implementation(Libs.geotools_referencing)
    implementation(Libs.geotools_epsg_hsql)
    implementation(Libs.jts_core)
    implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:${bluetape4kVersion}")
    implementation(Libs.postgis_jdbc)
    implementation("io.github.bluetape4k:bluetape4k-coroutines:${bluetape4kVersion}")
    implementation(Libs.kotlinx_coroutines_core)
}
```

---

## 테스트 (Testcontainers + PostGIS)

통합 테스트는 Testcontainers 기반 PostgreSQL + PostGIS 컨테이너(`postgis/postgis:16-3.4`)로 실행됩니다.

### NetCDF 카탈로그 테스트

```kotlin
@Testcontainers
class NetCdfTableTest : AbstractPostgisTest() {

    private val repo = NetCdfFileRepository()

    @Test
    fun `NetCDF 파일 레코드 저장 및 조회`() {
        transaction(db) {
            val record = NetCdfFileRecord(
                filename = "ERA5_2024_01.nc",
                filePath = "/data/ERA5_2024_01.nc",
                fileSize = 1_024_000L,
                variables = listOf(
                    NetCdfVariableInfo("temperature", "float", listOf(24, 181, 360),
                        mapOf("units" to "K"))
                ),
                dimensions = mapOf("time" to 24, "lat" to 181, "lon" to 360),
                globalAttrs = mapOf("Conventions" to "CF-1.8", "institution" to "ECMWF")
            )
            val saved = repo.save(record)

            val found = repo.findByIdOrNull(saved.id)
            found shouldNotBeNull()
            found.filename shouldBeEqualTo "ERA5_2024_01.nc"
            found.variables.size shouldBeEqualTo 1
            found.dimensions["time"] shouldBeEqualTo 24
        }
    }

    @Test
    fun `NetCdfCatalogService - registerFile 호출 시 NotImplementedError 발생`() {
        assertThrows<NotImplementedError> {
            catalogService.registerFile("/data/test.nc")
        }
    }
}
```

### Shapefile 임포트 테스트

```kotlin
@Test
fun `Shapefile PostGIS 임포트`() {
    transaction(db) {
        SchemaUtils.create(SpatialLayerTable, SpatialFeatureTable)
        val service = ShapefileImportService(SpatialLayerRepository(), SpatialFeatureRepository())
        val count = service.importShapefile(
            file = File("src/test/resources/test-data/provinces.shp"),
            layerName = "provinces-test"
        )
        count shouldBeGreaterThan 0
    }
}
```

---

## 성능 / 운영 가이드

### 좌표 변환

- **CRS 캐싱**: `CrsRegistry`는 EPSG 코드별 CRS 인스턴스를 캐시 — 반복 변환 비용 거의 없음.
- **스레드 안전**: `CrsRegistry`는 ConcurrentMap 기반; 멀티스레드 환경에서 안전.

### Shapefile 처리

- 대용량 파일은 `loadShapeAsync()` 사용 — `Dispatchers.IO` 디스패치, 논블로킹 처리.
- 지연(lazy) 레코드 순회로 수 GB 파일도 메모리 효율적 처리 가능.

### PostGIS 데이터베이스

- **공간 인덱스**: `CREATE INDEX ON spatial_features USING GIST (geom)` — bbox 범위 검색 가속.
- **배치 처리**: `ShapefileImportService`는 설정 가능한 배치 크기(기본 1000행)로 Virtual Thread 처리.
- **연결 풀링**: HikariCP 또는 Exposed 내장 풀 사용 권장.

### JTS 도형

- 저장 전 `GeometryOperations.simplify()` 적용 → 좌표 수 감소, DB 저장 비용 절감.
- `GeometryFactory`에 일관된 `PrecisionModel` 설정 → 재현 가능한 계산 결과.

### NetCDF 카탈로그

- `NetCdfFileTable`은 `bbox`(PostGIS POLYGON) + `timeStart/timeEnd` 컬럼 제공 → 시공간 필터링 지원.
- `netcdf_files(time_start, time_end)` 인덱스 → 시간 범위 쿼리 가속.
- `netcdf_grid_values(file_id, variable_name)` 복합 인덱스 → 변수별 격자 조회 가속.

---

## 관련 모듈

| 모듈 | 용도 |
|------|------|
| `bluetape4k-core` | 기본 유틸리티 (압축, 어설션) |
| `bluetape4k-coroutines` | 코루틴 확장 (Flow, DeferredValue) |
| `bluetape4k-exposed-postgresql` | PostGIS 컬럼 타입 |
| `bluetape4k-exposed-jdbc` | Exposed JDBC 저장소 기반 클래스 |
| `bluetape4k-testing-testcontainers` | Testcontainers 헬퍼 |
