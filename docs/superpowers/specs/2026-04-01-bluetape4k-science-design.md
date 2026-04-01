# bluetape4k-science 모듈 설계 스펙

> **작성일**: 2026-04-01
> **상태**: Draft
> **소스**: debop4k-science (`~/work/debop/debop4k/debop4k-science`)
> **대상**: bluetape4k-projects `utils/science`

---

## 1. 개요

### 목적

debop4k-science 모듈의 GIS 좌표계, NetCDF 과학 데이터, Shapefile 처리 기능을 bluetape4k 생태계로 마이그레이션한다. 기존 Java 코드를 완전 Kotlin화하고, 라이브러리를 2025년 기준 최신 안정 버전으로 업그레이드하며, Exposed/PostGIS 연동을 통해 공간 데이터 DB 적재 파이프라인까지 제공한다.

### 범위

| 범위 | 포함 여부 |
|------|-----------|
| GIS 좌표계 (BoundingBox, GeoLocation, DM/DMS, UTM) | O |
| Geometry 연산 (각도, 거리, 면적, 교차점) | O |
| 좌표 프로젝션 (UTM <-> WGS84) | O |
| Shapefile 읽기/파싱 | O |
| NetCDF 파일 읽기 | O |
| DB 적재 파이프라인 (PostGIS) | O (Phase 2) |
| 기존 utils/geo 모듈 변경 | X (독립 유지) |

---

## 2. 모듈 이름 및 위치

### 후보 분석

| 후보 | 장점 | 단점 | 결정 |
|------|------|------|------|
| `bluetape4k-science` | 원본 이름 유지, NetCDF 포함 | "science"가 너무 넓은 의미 | **채택** |
| `bluetape4k-gis` | GIS 도메인 명확 | NetCDF는 GIS에 국한되지 않음 | 후보 2 |
| `bluetape4k-geospatial` | 정확한 도메인 표현 | 이름이 김, 기상 데이터 포함 어색 | 탈락 |
| `bluetape4k-spatial` | 짧고 범용적 | PostGIS spatial과 혼동 가능 | 탈락 |

### 결정: `bluetape4k-science`

**근거**:
1. NetCDF는 기상, 해양, 대기과학 등 과학 데이터 포맷으로 GIS에 국한되지 않음
2. 원본 debop4k-science의 이름과 일관성 유지
3. 향후 과학 계산 기능(수치 해석, 통계 등) 확장 여지
4. utils/geo와 역할이 명확히 구분됨 (geo = geocode/geohash/geoip, science = 공간분석/과학데이터)

### 위치

```
utils/
  geo/          # 기존 유지 (geocode, geohash, geoip2)
  science/      # 신규 (GIS 좌표계, Shapefile, NetCDF, 프로젝션)
```

`settings.gradle.kts`에 의해 `bluetape4k-science`로 자동 등록된다.

### UTM 구현 전략

```
UTM 구현 전략:
- Zone/Band 판정 (longitudeZone, latitudeZone 계산): 자체 구현 (수학 공식 직접)
- 실제 좌표 변환 (UTM ↔ WGS84 수치 계산): Proj4J 위임
```

- `coords/UtmZoneSupport.kt`: Zone/Band 판정 + BoundingBox 계산 담당 (자체 수학 공식)
- `projection/Projections.kt`: 실제 `wgs84ToUtm()` / `utmToWgs84()` 좌표 변환 담당 (Proj4J API 사용)

---

## 3. 의존 라이브러리

### 신규 추가 (Libs.kt)

```kotlin
// === Science / GIS ===

// UCAR NetCDF-Java (CDM 라이브러리)
// https://docs.unidata.ucar.edu/netcdf-java/current/
// 2024년 기준 5.6.x → netcdf-java 리브랜딩, netcdfAll 단일 jar 제공
const val ucar_netcdf = "edu.ucar:netcdfAll:5.6.0"

// GeoTools - OGC 표준 지리정보 처리
// https://geotools.org/ (LGPL 2.1)
// 31.x 시리즈: Java 17+ 지원, JTS 1.20 호환
const val geotools_version = "31.6"
const val geotools_shapefile = "org.geotools:gt-shapefile:$geotools_version"
const val geotools_referencing = "org.geotools:gt-referencing:$geotools_version"
const val geotools_epsg_hsql = "org.geotools:gt-epsg-hsql:$geotools_version"

// Proj4J - 좌표 프로젝션 변환
// https://github.com/locationtech/proj4j
// 기존 com.jhlabs:javaproj:1.0.6 → LocationTech Proj4J로 대체
const val proj4j = "org.locationtech.proj4j:proj4j:1.3.0"
const val proj4j_epsg = "org.locationtech.proj4j:proj4j-epsg:1.3.0"

// ESRI Geometry API (선택적, Geometry 연산 가속)
// https://github.com/Esri/geometry-api-java
const val esri_geometry_api = "com.esri.geometry:esri-geometry-api:2.2.4"
```

### 기존 Libs.kt 활용

```kotlin
// 이미 존재
const val jts_core = "org.locationtech.jts:jts-core:1.20.0"
const val postgis_jdbc = "net.postgis:postgis-jdbc:2024.1.0"
```

### 제거 대상 (debop4k에서 사용했으나 불필요)

| 기존 | 대체 | 이유 |
|------|------|------|
| `com.jhlabs:javaproj:1.0.6` | `org.locationtech.proj4j:proj4j:1.3.0` | LocationTech 공식 후속 프로젝트 |
| `org.jscience:jscience:4.3.1` | 제거 | UTM 좌표 변환은 직접 구현으로 충분 |
| `org.jblas:jblas:1.2.4` | 제거 | 행렬 연산 미사용 (필요 시 commons-math3) |
| `org.eclipse.collections` | `kotlin.collections` | Kotlin stdlib로 대체 |
| `com.vividsolutions:jts` | `org.locationtech.jts:jts-core` | LocationTech JTS가 공식 후속 |

### 라이브러리 버전 상세

| 라이브러리 | 버전 | 릴리즈 | 라이선스 | 비고 |
|------------|------|--------|----------|------|
| UCAR netcdfAll | 5.6.0 | 2024-08 | BSD-3 | 순수 Java, ARM64 호환 |
| GeoTools | 31.6 | 2025-01 | LGPL 2.1 | **주의: LGPL** |
| JTS Core | 1.20.0 | 2024-03 | EPL 2.0 + BSD | 기존 사용 중 |
| Proj4J | 1.3.0 | 2024-02 | Apache 2.0 | LocationTech 프로젝트 |
| ESRI Geometry API | 2.2.4 | 2022-09 | Apache 2.0 | 선택적 의존 |

---

## 4. 패키지 구조

```
io.bluetape4k.science/
  coords/
    BoundingBox.kt          # 위경도 사각 영역 (data class)
    BoundingBoxRelation.kt  # 영역 관계 enum
    GeoLocation.kt          # 위경도 좌표 (data class)
    DM.kt                   # 도·분 좌표 (data class)
    DMS.kt                  # 도·분·초 좌표 (data class)
    UtmZone.kt              # UTM Zone (data class)
    UtmZoneSupport.kt       # UTM 변환 확장 함수
    Vector.kt               # 벡터 (각도+길이)
    CoordConverters.kt      # DM/DMS/Degree 상호 변환

  geometry/
    GeometryOperations.kt   # 각도, 거리, 면적, 무게중심, 교차점
    PolygonExtensions.kt    # Polygon/Point2D 확장 함수
    RotationSupport.kt      # 회전 연산
    BoundingBoxCheck.kt     # BoundingBox 내 위치 보정

  projection/
    Projections.kt          # Proj4J 기반 UTM <-> WGS84 변환
    CrsRegistry.kt          # 좌표 참조 시스템 레지스트리

  shapefile/
    ShapeModels.kt          # ShapeRecord, ShapeHeader, ShapeAttribute, Shape
    ShapefileReader.kt      # Shapefile 읽기 (GeoTools 기반)
    ShapefileExtensions.kt  # 편의 확장 함수

  netcdf/
    NetCdfReader.kt         # NetCDF 파일 읽기 (UCAR CDM)
    NetCdfExtensions.kt     # Variable 확장 함수, Kotlin Flow 지원
    NetCdfModels.kt         # NetCDF 메타데이터 모델 (data class)
```

---

## 5. 핵심 기능

### 5.1 좌표계 (coords/)

| 클래스/함수 | 설명 | 소스 |
|-------------|------|------|
| `BoundingBox` | 위경도 사각 영역, `data class`로 재설계 | BoundingBox.kt (Java + Kotlin 통합) |
| `GeoLocation` | 위경도 좌표, `data class` 유지 | GeoLocation.kt |
| `DM` / `DMS` | 도분, 도분초 좌표 표현 | DM.kt, DMS.kt |
| `UtmZone` | UTM Zone 표현 | UtmZone.kt |
| `Double.toDM()` / `Double.toDMS()` | 10진도 -> DM/DMS 변환 | Geometryx.kt 분리 |
| `DM.toDegree()` / `DMS.toDegree()` | DM/DMS -> 10진도 변환 | Geometryx.kt 분리 |
| `utmZoneOf()` | UTM Zone 팩토리 (위경도, 문자열 파싱) | UtmZonex.kt |
| `UtmZone.boundingBox()` | UTM Zone -> BoundingBox 변환 | UtmZonex.kt |
| `UtmZone.cellBbox()` | UTM Zone 내 셀 BoundingBox | UtmZonex.kt |

**마이그레이션 변경사항**:
- `AbstractValueObject` 상속 제거 -> `data class` 전환 (BoundingBox)
- `debop4k.core.utils.hashOf` -> `Objects.hash()` 또는 data class 자동 생성
- `debop4k.core.utils.min`/`max` 확장 -> `kotlin.coerceIn()` / `minOf()` / `maxOf()`
- `@JvmStatic`, `@JvmOverloads` 제거 (Kotlin-first, Java 호환 불필요)
- `Eclipse Collections FastList` -> `kotlin.collections.MutableList`
- `@Deprecated` 함수 전부 제거

### 5.2 Geometry 연산 (geometry/)

| 함수 | 설명 |
|------|------|
| `angleOf(p1, p2)` | 두 점 사이 각도 (degree) |
| `distanceOf(p1, p2)` | 두 점 사이 거리 |
| `vectorOf(degree, distance)` | 벡터 생성 |
| `Point2D.endPoint(vector)` | 벡터 끝점 계산 |
| `Polygon.area()` | 다각형 면적 |
| `Polygon.centerOfGravity()` | 무게 중심 |
| `getIntersectPoint(l1, l2)` | 두 선분 교차점 |
| `getLineNPoints(line, n)` | 선분 N등분 |
| `rotateXYPoint(point, degree, base)` | 점 회전 |
| `checkPositionPoint(point, bbox)` | BoundingBox 내 위치 보정 |
| `Double.isValidLatitude()` / `isValidLongitude()` | 위경도 유효성 검증 |
| `bboxOf(bboxStr)` | BoundingBox 문자열 파싱 |

### 5.3 좌표 프로젝션 (projection/)

| 함수 | 설명 |
|------|------|
| `utmToWgs84(easting, northing, zone)` | UTM -> WGS84 변환 |
| `wgs84ToUtm(latitude, longitude)` | WGS84 -> UTM 변환 |
| `transform(source, target, coord)` | 임의 CRS 간 변환 |

**변경사항**: `com.jhlabs:javaproj` -> `org.locationtech.proj4j:proj4j:1.3.0` 전환. Proj4J는 EPSG 코드 기반 CRS 생성을 지원하므로 더 정확하고 다양한 프로젝션 지원.

### 5.4 Shapefile 처리 (shapefile/)

| 클래스/함수 | 설명 |
|-------------|------|
| `Shape` | Shapefile 전체 구조 (header + records + attributes) |
| `ShapeRecord` | 개별 레코드 (번호, 타입, BoundingBox, Geometry) |
| `ShapeHeader` | 파일 헤더 |
| `ShapeAttribute` | DBF 속성 |
| `loadShape(file)` | Shapefile 읽기 (shp + dbf + shx) |

**변경사항**:
- `com.vividsolutions.jts` -> `org.locationtech.jts` (패키지 변경만)
- GeoTools `gt-shapefile:15.0` -> `31.6` (API 변경 대응 필요)
- Java 클래스 (`Shape.java`, `ShapeAttribute.java` 등) -> Kotlin data class 통합
- `FastList` -> `List` / `MutableList`

### 5.5 NetCDF 처리 (netcdf/)

| 함수 | 설명 |
|------|------|
| `NetCdfReader.open(path)` | NetCDF 파일 열기 |
| `NetCdfReader.openInMemory(path)` | 메모리 로딩 |
| `NetCdfReader.canRead(file)` | 파일 읽기 가능 여부 |
| `Variable.readIntArray()` | 정수 배열 읽기 (확장 함수) |
| `Variable.readDoubleArray()` | 실수 배열 읽기 (확장 함수) |
| `Variable.readNDArray()` | N차원 배열 읽기 |
| `NetcdfFile.getInformation()` | 파일 정보 문자열 |
| `NetcdfFile.variableFlow()` | Variable을 Kotlin Flow로 스트리밍 |

**변경사항**:
- UCAR `4.x` -> `5.6.0` (API 대폭 변경: `NetcdfFile.open()` -> `NetcdfFiles.open()`)
- `Eclipse Collections IntArrayList` 등 -> `IntArray` / `DoubleArray` (Kotlin 원시 배열)
- object 메서드 -> 확장 함수 패턴으로 리팩토링
- Kotlin Flow 기반 대용량 데이터 스트리밍 추가

---

## 6. 테스트 데이터 전략

### 현재 데이터 (debop4k-science/data)

| 디렉토리 | 내용 | 크기 | 활용 |
|----------|------|------|------|
| `data/netcdf/woa05_temp.nc` | World Ocean Atlas 2005 수온 데이터 | ~수 MB | NetCDF 읽기 테스트 |
| `data/netcdf/MRMS_*.grib2` | 기상 레이더 GRIB2 데이터 | ~수 MB | GRIB2 읽기 테스트 |
| `data/netcdf/radar/` | 레이더 데이터 | 미확인 | 레이더 데이터 파싱 |
| `data/netcdf/shrt/` | 단파 복사 데이터 | 미확인 | 과학 데이터 파싱 |
| `data/shp_v5/harbors/` | 항만 Shapefile | 소량 | Shapefile 읽기 테스트 |
| `data/shp_v5/oceans/` | 해양 경계 Shapefile | 소량 | 다각형 Shapefile 테스트 |

### 최신 샘플 데이터 추가 방안

| 데이터 | 출처 | 용도 |
|--------|------|------|
| Natural Earth Admin Boundaries (1:110m) | [naturalearthdata.com](https://www.naturalearthdata.com/) | 국가/행정구역 경계 Shapefile |
| ERA5 Sample NetCDF | ECMWF Copernicus | 최신 기상 재분석 데이터 |
| 대한민국 행정동 경계 (SHP) | 국토정보플랫폼 | 한국 행정경계 테스트 |
| OpenStreetMap Korea Extract | Geofabrik | 도로/건물 공간 데이터 |

### 테스트 데이터 관리 정책

1. **소형 데이터 (< 1MB)**: `src/test/resources/data/` 에 포함
2. **중형 데이터 (1-50MB)**: `.gitignore`에 등록, `README.md`에 다운로드 스크립트 제공
3. **대형 데이터 (> 50MB)**: Testcontainers + 초기화 스크립트로 런타임 생성

---

## 7. DB 적재 Usecase

### 7.1 적재 대상 데이터 분류

| 데이터 | DB 적재 필요성 | 이유 |
|--------|----------------|------|
| Shapefile 지리 경계 | **높음** | 공간 쿼리 (ST_Contains, ST_Intersects) 필수 |
| Shapefile 속성 | **높음** | 경계와 함께 속성 검색 필수 |
| NetCDF 격자 데이터 | **중간** | 대용량 시계열 -> PostGIS Raster 또는 TimescaleDB |
| NetCDF 메타데이터 | **높음** | 파일/변수 카탈로그 관리 |
| GeoLocation/BoundingBox | **높음** | POI, 영역 검색 |

### 7.2 PostgreSQL + PostGIS 스키마 초안

```sql
-- Shapefile 데이터 적재용
CREATE EXTENSION IF NOT EXISTS postgis;

-- 1. 공간 레이어 메타데이터
CREATE TABLE spatial_layers (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL UNIQUE,
    description  TEXT,
    source_file  VARCHAR(512),
    srid         INT NOT NULL DEFAULT 4326,
    geometry_type VARCHAR(50) NOT NULL,  -- POINT, POLYGON, LINESTRING, MULTIPOLYGON
    bbox_min_x   DOUBLE PRECISION,
    bbox_min_y   DOUBLE PRECISION,
    bbox_max_x   DOUBLE PRECISION,
    bbox_max_y   DOUBLE PRECISION,
    record_count INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. 공간 피처 (Shapefile record + attributes)
CREATE TABLE spatial_features (
    id           BIGSERIAL PRIMARY KEY,
    layer_id     BIGINT NOT NULL REFERENCES spatial_layers(id),
    feature_type VARCHAR(50) NOT NULL,
    geom         GEOMETRY(GEOMETRY, 4326) NOT NULL,
    properties   JSONB,
    name         VARCHAR(255),
    -- AuditableLongIdTable 제공 컬럼
    created_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_spatial_features_geom ON spatial_features USING GIST (geom);
CREATE INDEX idx_spatial_features_layer ON spatial_features (layer_id);
CREATE INDEX idx_spatial_features_props ON spatial_features USING GIN (properties);

-- 3. NetCDF 파일 카탈로그
CREATE TABLE netcdf_files (
    id           BIGSERIAL PRIMARY KEY,
    filename     VARCHAR(512) NOT NULL,
    file_path    VARCHAR(1024),
    file_size    BIGINT,
    variables    JSONB NOT NULL DEFAULT '[]',
    dimensions   JSONB NOT NULL DEFAULT '[]',
    global_attrs JSONB NOT NULL DEFAULT '{}',
    bbox         GEOMETRY(POLYGON, 4326),  -- 공간 범위
    time_start   TIMESTAMPTZ,
    time_end     TIMESTAMPTZ,
    -- AuditableLongIdTable 제공 컬럼
    created_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_netcdf_files_bbox ON netcdf_files USING GIST (bbox);

-- 4. NetCDF 격자 데이터 (선택적, 소규모 데이터셋용)
CREATE TABLE netcdf_grid_values (
    id           BIGSERIAL PRIMARY KEY,
    file_id      BIGINT NOT NULL REFERENCES netcdf_files(id),
    variable_name VARCHAR(255) NOT NULL,
    location     GEOMETRY(POINT, 4326) NOT NULL,
    time_idx     TIMESTAMPTZ,
    value        DOUBLE PRECISION NOT NULL,
    level_idx    INT
);
CREATE INDEX idx_netcdf_grid_loc ON netcdf_grid_values USING GIST (location);
CREATE INDEX idx_netcdf_grid_time ON netcdf_grid_values (time_idx);

-- 5. POI (Point of Interest)
CREATE TABLE poi (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    location     GEOMETRY(POINT, 4326) NOT NULL,
    properties   JSONB NOT NULL DEFAULT '{}',
    -- AuditableLongIdTable 제공 컬럼
    created_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by   VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_poi_location ON poi USING GIST (location);
CREATE INDEX idx_poi_category ON poi (category);
```

### 7.3 쿼리 패턴

```sql
-- 특정 위치 반경 내 POI 검색 (meters 단위 — 양쪽 모두 geography 캐스팅 필수)
SELECT * FROM poi
WHERE ST_DWithin(
  location::geography,
  ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326)::geography,
  1000
);
-- 또는 geometry CRS 통일 방식 (미터 단위 투영 좌표계 사용):
-- WHERE ST_DWithin(
--   ST_Transform(location, 3857),
--   ST_Transform(ST_SetSRID(ST_MakePoint(126.9780, 37.5665), 4326), 3857),
--   1000
-- );

-- 행정구역 내 POI 검색
SELECT p.* FROM poi p
JOIN spatial_features f ON ST_Contains(f.geom, p.location)
WHERE f.layer_id = 1 AND f.properties->>'name' = '서울특별시';

-- BoundingBox 내 피처 검색
SELECT * FROM spatial_features
WHERE geom && ST_MakeEnvelope(126.0, 37.0, 127.5, 37.7, 4326);

-- NetCDF 파일에서 특정 영역의 격자값 조회
SELECT * FROM netcdf_grid_values
WHERE ST_Contains(
    ST_MakeEnvelope(126.0, 33.0, 130.0, 39.0, 4326),
    location
) AND variable_name = 'temperature';
```

---

## 8. Exposed 통합 전략

### 8.1 기존 exposed-postgresql 모듈과의 관계

기존 `exposed-postgresql` 모듈은 이미 PostGIS (`GeoPointColumnType`, `GeoPolygonColumnType`)와 pgvector를 지원한다. 새로운 science 모듈과의 관계:

```
bluetape4k-science (utils/science)
  ├─ 순수 GIS 좌표/연산 라이브러리 (DB 무관)
  └─ compileOnly: exposed-postgresql (DB 적재가 필요할 때만)

bluetape4k-exposed-postgresql (data/exposed-postgresql)
  ├─ PostGIS Column Types (기존)
  └─ 공간 쿼리 함수 (기존 ST_* 함수)
```

**원칙**: science 모듈 자체는 DB 의존성이 없다. DB 적재 기능은 별도 패키지(`io.bluetape4k.science.exposed`)에 `compileOnly`로 제공하거나, 사용자가 직접 exposed-postgresql과 조합한다.

### 8.2 Exposed 테이블 설계

> **Fix 1**: `SpatialFeatureTable.geom`은 `geoGeometry()`(Phase 0에서 신규 추가 예정)를 사용한다.
> Shapefile은 POINT 외에 POLYGON, LINESTRING, MULTIPOLYGON 등 다양한 Geometry 타입을 포함하므로
> `geoPoint()`(net.postgis.jdbc.geometry.Point 반환)로는 충분하지 않다.
>
> **Fix 2**: `jsonb<>()` API는 존재하지 않는다. `bluetape4k-exposed-jackson3` 모듈의
> `jacksonb<T>()` 확장함수를 사용한다 (JSONB + GIN 인덱스 지원).
>
> **Fix 3**: `CurrentTimestampWithTimeZone`은 Exposed에 존재하지 않는 API이다.
> `timestamp("...").clientDefault { Instant.now() }` 패턴으로 대체한다.
> createdAt/updatedAt이 필요한 테이블은 `AuditableLongIdTable` 상속을 우선 고려한다.

```kotlin
// --- spatial_layers ---
// AuditableLongIdTable 상속으로 createdAt/updatedAt 자동 제공
object SpatialLayerTable : AuditableLongIdTable("spatial_layers") {
    val name = varchar("name", 255).uniqueIndex()
    val description = text("description").nullable()
    val sourceFile = varchar("source_file", 512).nullable()
    val srid = integer("srid").default(4326)
    val geometryType = varchar("geometry_type", 50)
    val bboxMinX = double("bbox_min_x").nullable()
    val bboxMinY = double("bbox_min_y").nullable()
    val bboxMaxX = double("bbox_max_x").nullable()
    val bboxMaxY = double("bbox_max_y").nullable()
    val recordCount = integer("record_count").nullable()
    // createdAt, updatedAt, createdBy, updatedBy → AuditableLongIdTable 상속으로 자동 제공
}

// --- spatial_features ---
// Fix 1: geoPoint() → geoGeometry() 로 변경 (Phase 0 선행 작업에서 추가 예정)
// Fix 2: jsonb<>() → jacksonb<>() (bluetape4k-exposed-jackson3, JSONB + GIN 인덱스)
// Fix B: AuditableLongIdTable 상속 (대량 import 시에도 감사 추적 필요 — 누가 언제 import했는지 UserContext로 추적)
object SpatialFeatureTable : AuditableLongIdTable("spatial_features") {
    val layerId = reference("layer_id", SpatialLayerTable)  // Fix 1: long().references() → reference()
    val featureType = varchar("feature_type", 50)
    val geom = geoGeometry("geom")  // Fix 1: geoPoint() → geoGeometry() (신규, GEOMETRY(GEOMETRY,4326))
    val properties = jacksonb<Map<String, Any?>>("properties")  // Fix 2: jsonb → jacksonb (JSONB)
    val name = varchar("name", 255).nullable()
    // createdAt/updatedAt/createdBy/updatedBy는 AuditableLongIdTable에서 자동 제공
}

// --- netcdf_files ---
// Fix 2: jsonb<>() → jacksonb<>() (bluetape4k-exposed-jackson3, JSONB + GIN 인덱스)
// Fix 3: AuditableLongIdTable 상속
object NetCdfFileTable : AuditableLongIdTable("netcdf_files") {
    val filename = varchar("filename", 512)
    val filePath = varchar("file_path", 1024).nullable()
    val fileSize = long("file_size").nullable()
    val variables = jacksonb<List<NetCdfVariableInfo>>("variables")   // Fix 2: JSONB
    val dimensions = jacksonb<Map<String, Int>>("dimensions")         // Fix 2: JSONB
    val globalAttrs = jacksonb<Map<String, String>>("global_attrs")   // Fix 2: JSONB
    val bbox = geoPolygon("bbox").nullable()
    val timeStart = timestamp("time_start").nullable()
    val timeEnd = timestamp("time_end").nullable()
    // createdAt → AuditableLongIdTable 상속으로 자동 제공
}

// --- poi ---
// Fix 2: jsonb<>() → jacksonb<>() (bluetape4k-exposed-jackson3, JSONB + GIN 인덱스)
// Fix 3: AuditableLongIdTable 상속
object PoiTable : AuditableLongIdTable("poi") {
    val name = varchar("name", 255)
    val category = varchar("category", 100).nullable()
    val location = geoPoint("location")   // POI는 항상 POINT이므로 geoPoint() 유지
    val properties = jacksonb<Map<String, Any?>>("properties")  // Fix 2: JSONB
    // createdAt, updatedAt → AuditableLongIdTable 상속으로 자동 제공
}
```

### 8.3 Repository 설계

#### JDBC 전용 (R2DBC 미사용)

**결정**: `exposed-jdbc`만 사용한다. R2DBC는 사용하지 않는다.

**이유**:
- `bluetape4k-exposed-postgresql`의 PostGIS 컬럼 타입(`GeoPointColumnType`, `GeoGeometryColumnType` 등)은
  모두 `postgis-jdbc` 기반 JDBC 구현이다.
- R2DBC PostgreSQL 드라이버(`r2dbc-postgresql`)는 PostGIS geometry 타입 codec을 자동 제공하지 않으므로
  커스텀 codec 구현이 추가로 필요하다. 이번 scope에서는 다루지 않는다.
- `newSuspendedTransaction(Dispatchers.IO) { ... }` 패턴으로 JDBC 위에서 suspend 지원을 제공한다.

#### Repository 기반 클래스

`bluetape4k-exposed-jdbc`의 `AbstractJdbcRepository<ID, E>` 패턴을 따른다:

```kotlin
// build.gradle.kts
compileOnly(project(":bluetape4k-exposed-jdbc"))

// SpatialLayerRepository
class SpatialLayerRepository : AbstractLongJdbcRepository<SpatialLayerRecord, SpatialLayerTable>() {
    override val table = SpatialLayerTable
    override fun extractId(entity: SpatialLayerRecord) = entity.id

    /** 레이어 이름으로 조회 */
    suspend fun findByName(name: String): SpatialLayerRecord? = newSuspendedTransaction(Dispatchers.IO) {
        table.selectAll().where { table.name eq name }.singleOrNull()?.toRecord()
    }
}

// SpatialFeatureRepository
class SpatialFeatureRepository : AbstractLongJdbcRepository<SpatialFeatureRecord, SpatialFeatureTable>() {
    override val table = SpatialFeatureTable
    override fun extractId(entity: SpatialFeatureRecord) = entity.id

    /** 반경 내 피처 조회 (PostGIS ST_DWithin) */
    suspend fun findWithinRadius(lon: Double, lat: Double, radiusMeters: Double): List<SpatialFeatureRecord> =
        newSuspendedTransaction(Dispatchers.IO) { ... }
}

// NetCdfFileRepository
class NetCdfFileRepository : AbstractLongJdbcRepository<NetCdfFileRecord, NetCdfFileTable>() {
    override val table = NetCdfFileTable
    override fun extractId(entity: NetCdfFileRecord) = entity.id
}

// PoiRepository
class PoiRepository : AbstractLongJdbcRepository<PoiRecord, PoiTable>() {
    override val table = PoiTable
    override fun extractId(entity: PoiRecord) = entity.id
}
```

> **참고**: `AbstractLongJdbcRepository`는 `AbstractJdbcRepository<Long, E>`의 편의 타입 alias이다.
> CLAUDE.md Repository Generic Pattern 참조.

#### Import 서비스 (JDBC 기반)

```kotlin
/**
 * Shapefile을 읽어 PostGIS에 적재하는 서비스 (JDBC)
 */
class ShapefileImportService(
    private val layerRepo: SpatialLayerRepository,
    private val featureRepo: SpatialFeatureRepository,
) {
    /**
     * Shapefile을 읽어 DB에 적재한다.
     *
     * @param file Shapefile (.shp)
     * @param layerName 레이어 이름
     * @param batchSize 배치 크기 (기본 1000)
     * @return 적재된 피처 수
     */
    suspend fun importShapefile(file: File, layerName: String, batchSize: Int = 1000): Int {
        val shape = withContext(Dispatchers.IO) { loadShape(file) }
        // 배치 단위 transaction-per-batch (newSuspendedTransaction)
        // ensureActive() 호출로 취소 처리
    }
}

/**
 * NetCDF 파일 메타데이터를 DB에 적재하는 서비스 (JDBC)
 */
class NetCdfCatalogService(
    private val fileRepo: NetCdfFileRepository,
) {
    suspend fun registerFile(filePath: String): Long = newSuspendedTransaction(Dispatchers.IO) { ... }
}
```

#### Import 서비스 설계 상세

**블로킹 I/O 전략**:
- 파일 파싱 (GeoTools/UCAR): `withContext(Dispatchers.IO) { ... }` 감싸기
- DB 쓰기: `newSuspendedTransaction(Dispatchers.IO) { ... }` 사용

**트랜잭션 경계**:
- 배치 크기: 기본 1000건/트랜잭션 (configurable)
- 실패 격리: transaction-per-batch (부분 실패 시 해당 배치만 롤백)
- 중복 방지: `SpatialLayerTable.name` UNIQUE 제약 → 동일 이름 import 시 `IllegalArgumentException`

**취소 처리**:
- 배치 루프마다 `ensureActive()` 호출
- `CoroutineScope.isActive` 체크로 장시간 import 중단 가능

**재시도 정책**:
- 파일 파싱 실패: 재시도 없음 (파일 자체 문제)
- DB 쓰기 실패: Resilience4j Retry (3회, 1s 백오프)
```

### 8.4 exposed-postgresql 확장 필요 사항

현재 exposed-postgresql의 PostGIS 지원은 `Point`와 `Polygon`만 지원한다. Shapefile 적재를 위해 추가 필요:

| 필요 타입 | 현재 | 추가 필요 |
|-----------|------|-----------|
| POINT | O (`GeoPointColumnType`) | - |
| POLYGON | O (`GeoPolygonColumnType`) | - |
| LINESTRING | X | `GeoLineStringColumnType` |
| MULTIPOLYGON | X | `GeoMultiPolygonColumnType` |
| MULTILINESTRING | X | `GeoMultiLineStringColumnType` |
| MULTIPOINT | X | `GeoMultiPointColumnType` |
| GEOMETRY (generic) | **X** | **`GeoGeometryColumnType` (모든 타입 수용) — Phase 0 선행 필수** |

> **중요**: `GeoGeometryColumnType` + `geoGeometry()` 확장함수는 Phase 0 선행 작업으로
> exposed-postgresql 모듈에 먼저 구현해야 한다. Phase 3(Shapefile) 진입 전 완료 필수.

### 8.5 의존성 매트릭스

어떤 기능을 사용할 때 어떤 런타임 의존성이 필요한지 정리한다.

| 기능 | `jts-core` | `proj4j` | `geotools` (LGPL) | `ucar-netcdf` | `exposed-postgresql` | `exposed-jackson3` |
|------|:----------:|:--------:|:-----------------:|:-------------:|:--------------------:|:------------------:|
| 좌표계 (coords/) | O | - | - | - | - | - |
| Geometry 연산 (geometry/) | O | - | - | - | - | - |
| 좌표 프로젝션 (projection/) | O | **O** | - | - | - | - |
| Shapefile 읽기 (shapefile/) | O | - | **O** | - | - | - |
| NetCDF 읽기 (netcdf/) | - | - | - | **O** | - | - |
| DB 적재 — Shapefile | O | - | **O** | - | **O** | **O** |
| DB 적재 — NetCDF 메타데이터 | - | - | - | **O** | **O** | **O** |
| DB 적재 — POI | O | - | - | - | **O** | **O** |

- **O**: 필수 런타임 의존성
- **O** (굵게): 사용자가 런타임에 별도 추가 필요 (`compileOnly`로 선언됨)
- `-`: 불필요

#### 소비자 가이드 (단일 모듈 compileOnly 전략)

`bluetape4k-science` 소비자는 사용하는 기능에 따라 런타임 의존성을 직접 추가해야 한다:

| 기능 | 런타임 의존성 추가 필요 |
|------|----------------------|
| coords/geometry (기본) | `jts-core` (자동 전이, 추가 불필요) |
| Shapefile 처리 | `gt-shapefile`, `proj4j-core` |
| NetCDF 처리 | `netcdfAll` |
| DB 저장 | `bluetape4k-exposed-jackson3`, `bluetape4k-exposed-postgresql` |

### compileOnly 전략: 공개 API 타입 규칙 (CRITICAL)

bluetape4k-science의 모든 공개 API(fun, class)는 GeoTools/UCAR/Proj4J/Exposed 타입을 
**메서드 시그니처(파라미터, 반환 타입)에 절대 노출하지 않는다**.

- `loadShape(file: File): ShapeCollection`  ← bluetape4k 래핑 타입만 반환
- `NetCdfReader.open(path: String): NetCdfFile`  ← bluetape4k 래핑 타입 반환
- `transformCoord(from, to, point): GeoLocation`  ← bluepape4k 타입 반환

GeoTools `SimpleFeature`, UCAR `NetcdfFile`, `Proj4J CRS` 등 외부 라이브러리 타입이
공개 API 시그니처에 등장하면 해당 라이브러리가 compileOnly에서 implementation/api로 승격되어야 한다.

이 규칙 위반 시 소비자의 컴파일 classpath에 해당 라이브러리가 없어 컴파일 오류 발생.

---

## 9. 마이그레이션 전략

### Phase 0: 선행 작업 — exposed-postgresql GeoGeometryColumnType 추가 (0.5일)

**대상 모듈**: `data/exposed-postgresql` (기존 모듈 확장)

Phase 3(Shapefile DB 적재) 진입 전에 반드시 완료해야 한다. Shapefile은 POINT/POLYGON/LINESTRING/
MULTIPOLYGON 등 혼재된 Geometry 타입을 포함하므로, 단일 generic Geometry 컬럼 타입이 필요하다.

**구현 항목**:

1. `GeoGeometryColumnType` 추가
   - SQL 타입: `GEOMETRY(GEOMETRY, 4326)` (모든 PostGIS geometry 수용)
   - `net.postgis.jdbc.geometry.Geometry` 타입 (postgis-jdbc의 최상위 Geometry)
   - `notNullValueToDB()`: `PGgeometry(value)` 래핑
   - `valueFromDB()`: `PGgeometry` → `Geometry` 캐스팅

   ```kotlin
   // data/exposed-postgresql/src/main/kotlin/io/bluetape4k/exposed/postgresql/postgis/GeoColumnTypes.kt 에 추가
   class GeoGeometryColumnType : ColumnType<net.postgis.jdbc.geometry.Geometry>() {
       override fun sqlType(): String {
           check(currentDialect is PostgreSQLDialect) { "..." }
           return "GEOMETRY(GEOMETRY, 4326)"
       }
       override fun notNullValueToDB(value: net.postgis.jdbc.geometry.Geometry): Any {
           if (value.srid == net.postgis.jdbc.geometry.Geometry.UNKNOWN_SRID) value.srid = 4326
           return PGgeometry(value)
       }
       override fun valueFromDB(value: Any): net.postgis.jdbc.geometry.Geometry = when (value) {
           is PGgeometry -> value.geometry
           is String     -> PGgeometry(value).geometry
           else          -> error("Unsupported: ${value::class.java}")
       }
   }
   ```

2. `Table.geoGeometry(name)` 확장함수 추가
   - `GeoExtensions.kt`에 추가:
   ```kotlin
   fun Table.geoGeometry(name: String): Column<net.postgis.jdbc.geometry.Geometry> =
       registerColumn(name, GeoGeometryColumnType())
   ```

3. `Column<Geometry>.stIntersects()` 등 기존 ST_* 함수가 generic Geometry를 수용하도록 오버로드 확인

4. 단위 테스트: `GeoGeometryColumnTypeTest` 추가 (H2 fallback 불가 → Testcontainers PostgreSQL 필요)

### Phase 1: 코어 좌표계 + Geometry 연산 (1-2일)

1. `utils/science` 디렉토리 생성, `build.gradle.kts` 작성
2. `coords/` 패키지 마이그레이션
   - Java 클래스 제거, Kotlin data class로 통일
   - `debop4k.core` 의존 -> `io.bluetape4k.core` / Kotlin stdlib 대체
   - `@Deprecated` 함수 전체 제거
3. `geometry/` 패키지 마이그레이션
   - `Geometryx.kt` -> 기능별 파일 분리
4. 단위 테스트 작성 (JUnit 5 + Kluent)

### Phase 2: 좌표 프로젝션 (0.5일)

1. `projection/` 패키지 마이그레이션
2. `com.jhlabs:javaproj` -> `org.locationtech.proj4j` 전환
3. UTM <-> WGS84 변환 테스트

### Phase 3: Shapefile 처리 (1일)

1. `shapefile/` 패키지 마이그레이션
2. GeoTools `15.0` -> `31.6` 버전 업그레이드 대응
3. `com.vividsolutions.jts` -> `org.locationtech.jts` 패키지 변경
4. Java 클래스 -> Kotlin data class 변환
5. 테스트 데이터 (`data/shp_v5`) 복사 및 테스트

### Phase 4: NetCDF 처리 (1일)

1. `netcdf/` 패키지 마이그레이션
2. UCAR `4.x` -> `5.6.0` API 변경 대응
   - `NetcdfFile.open()` -> `NetcdfFiles.open()`
   - Variable API 변경 확인
3. Eclipse Collections -> Kotlin 원시 배열
4. Kotlin Flow 기반 스트리밍 API 추가
5. 테스트 데이터 (`data/netcdf`) 복사 및 테스트

### Phase 5: DB 적재 파이프라인 (2일)

> **전제조건**: Phase 0(GeoGeometryColumnType 추가)이 완료된 후 진행

1. exposed-postgresql 모듈 확장 완료 확인 (Phase 0)
2. Exposed 테이블/Repository 구현
3. `ShapefileImportService`, `NetCdfCatalogService` 구현
4. Testcontainers + PostGIS 통합 테스트

### Phase 6: 문서화 + 정리 (0.5일)

1. README.md 작성
2. KDoc 작성 (한국어)
3. CLAUDE.md Architecture 섹션 업데이트
4. Libs.kt 업데이트

---

## 10. 기술적 제약사항

### 10.1 GeoTools LGPL 라이선스

**문제**: GeoTools는 LGPL 2.1 라이선스이다. bluetape4k는 Apache 2.0.

**대응**:
- GeoTools는 `compileOnly` 의존으로 선언 (사용자가 런타임에 직접 추가)
- Shapefile 처리 없이 좌표계/Geometry 연산만 사용하는 경우 GeoTools 불필요
- 대안 검토: Apache SIS (Apache 2.0, OGC 표준) -- 단, Shapefile 지원 부족

### 10.2 ARM64 호환성

| 라이브러리 | ARM64 (Apple Silicon) | 비고 |
|------------|----------------------|------|
| UCAR netcdfAll 5.6.0 | **호환** | 순수 Java |
| GeoTools 31.6 | **호환** | 순수 Java (JAI 제거됨) |
| JTS Core 1.20 | **호환** | 순수 Java |
| Proj4J 1.3.0 | **호환** | 순수 Java |
| ESRI Geometry API | **호환** | 순수 Java |

> GeoTools 15.x에서 의존하던 `jai_core`는 31.x에서 제거되었으므로 ARM64 문제 없음.

### 10.3 UCAR netcdfAll 5.x API 변경

주요 breaking change:

```kotlin
// 4.x (기존)
NetcdfFile.open(path)
v.readScalarInt()

// 5.x (신규)
NetcdfFiles.open(path)           // 클래스명 변경
v.readScalarInt()                // 유지 (일부 시그니처 변경)
NetcdfDatasets.openDataset(path) // Dataset API 추가
```

### 10.4 GeoTools 31.x API 변경

```kotlin
// 15.x (기존)
import org.geotools.data.shapefile.shp.ShapefileReader
// 31.x 에서도 동일 패키지 유지, 단 일부 생성자 변경

// JTS 패키지 변경
// 15.x: com.vividsolutions.jts.geom.Geometry
// 31.x: org.locationtech.jts.geom.Geometry (17.x부터 변경)
```

### 10.5 기존 utils/geo와의 중복

| 항목 | utils/geo | utils/science | 비고 |
|------|-----------|---------------|------|
| `BoundingBox` | geohash 패키지 내부 | 독립 모델 | **다른 구현** (geo는 geohash 전용) |
| `GeoLocation` | geoip2 패키지 | 독립 모델 | **다른 구현** (geo는 IP 기반) |
| `WGS84Point` | geohash 패키지 | 미사용 | geo 전용 |
| 좌표 변환 | 없음 | DM/DMS/UTM | science 전용 |
| 공간 쿼리 | 없음 | Geometry 연산 | science 전용 |

**결론**: 두 모듈은 도메인이 다르므로 독립 유지한다. geo는 "서비스 기반 위치 조회" (geocode, geohash, geoip), science는 "과학/공간 데이터 처리"에 집중한다.

### 10.6 build.gradle.kts 초안

```kotlin
configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    api(project(":bluetape4k-core"))
    api(project(":bluetape4k-logging"))

    // JTS (좌표계, Geometry 연산)
    api(Libs.jts_core)

    // Proj4J (좌표 프로젝션)
    compileOnly(Libs.proj4j)
    compileOnly(Libs.proj4j_epsg)

    // GeoTools (Shapefile 처리) - LGPL, compileOnly
    compileOnly(Libs.geotools_shapefile)
    compileOnly(Libs.geotools_referencing)
    compileOnly(Libs.geotools_epsg_hsql)

    // UCAR NetCDF (NetCDF 파일 처리)
    compileOnly(Libs.ucar_netcdf)

    // ESRI Geometry API (선택적)
    compileOnly(Libs.esri_geometry_api)

    // Coroutines (Flow 기반 스트리밍)
    compileOnly(project(":bluetape4k-coroutines"))
    compileOnly(Libs.kotlinx_coroutines_core)

    // Exposed 연동 (선택적, DB 적재 시)
    compileOnly(project(":bluetape4k-exposed-jdbc"))
    compileOnly(project(":bluetape4k-exposed-postgresql"))

    // JSON 컬럼 타입 (jacksonb<T>() 확장함수 — JSONB + GIN 인덱스)
    compileOnly(project(":bluetape4k-exposed-jackson3"))

    // Testing
    testImplementation(project(":bluetape4k-junit5"))
    testImplementation(Libs.kotlinx_coroutines_test)
}
```

### 10.7 GeoTools Maven Repository

GeoTools는 Maven Central에 없으므로 **루트 `build.gradle.kts`의 `allprojects { repositories }` 블록**에
별도 저장소를 추가해야 한다 (모듈 자체의 `build.gradle.kts`가 아님).

```kotlin
// 루트 build.gradle.kts
allprojects {
    repositories {
        mavenCentral()
        maven("https://repo.osgeo.org/repository/release/")  // GeoTools 전용 저장소
    }
}
```

> GeoTools 의존성을 사용하는 모듈(science)의 `build.gradle.kts`에만 추가해도 동작하지만,
> Gradle 빌드 캐시 일관성을 위해 루트의 `allprojects` 블록에 추가하는 것을 권장한다.

---

## 부록: 소스 파일 매핑

| debop4k-science 소스 | bluetape4k-science 대상 | 변환 내용 |
|---------------------|------------------------|-----------|
| `gis/coords/BoundingBox.kt` + `.java` | `coords/BoundingBox.kt` | data class 전환, Java 제거 |
| `gis/coords/GeoLocation.kt` + `.java` | `coords/GeoLocation.kt` | Java 제거, hashOf 제거 |
| `gis/coords/DM.kt` + `.java` | `coords/DM.kt` | Java 제거 |
| `gis/coords/DMS.kt` + `.java` | `coords/DMS.kt` | Java 제거 |
| `gis/coords/UtmZone.kt` + `.java` | `coords/UtmZone.kt` | Java 제거 |
| `gis/coords/UtmZonex.kt` + `.java` | `coords/UtmZoneSupport.kt` | @Deprecated 제거, 리네임 |
| `gis/coords/Vector.kt` + `.java` | `coords/Vector.kt` | Java 제거 |
| `gis/coords/Geometryx.kt` + `.java` | `geometry/` 분할 | 기능별 파일 분리 |
| `gis/BoundingBoxRelation.kt` | `coords/BoundingBoxRelation.kt` | 그대로 이동 |
| `gis/projections/Projections.kt` | `projection/Projections.kt` | javaproj -> proj4j |
| `gis/shapefiles/ShapeFilex.kt` | `shapefile/ShapefileReader.kt` | GeoTools 31.x 대응 |
| `gis/shapefiles/ShapeModels.kt` | `shapefile/ShapeModels.kt` | JTS 패키지 변경, FastList 제거 |
| `gis/shapefiles/*.java` | 통합 | Kotlin data class로 통합 |
| `netcdf/NetCdfReader.kt` | `netcdf/NetCdfReader.kt` | UCAR 5.x, 확장 함수 패턴 |
