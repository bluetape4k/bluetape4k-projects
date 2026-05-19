# Module bluetape4k-science

English | [한국어](./README.ko.md)

An integrated Kotlin module for scientific and geospatial data processing: GIS coordinate conversion,
Shapefile I/O, JTS geometry operations, PostGIS database pipelines, and NetCDF metadata cataloging.

## Overview

`bluetape4k-science` covers five domains:

| # | Domain | Key Libraries | Status |
|---|--------|---------------|--------|
| 1 | **GIS Coordinate Conversion** | Proj4J, proj4j-epsg | ✅ Implemented |
| 2 | **Shapefile Processing** | GeoTools (LGPL) | ✅ Implemented |
| 3 | **JTS Geometry Operations** | JTS Core | ✅ Implemented |
| 4 | **PostGIS Data Pipeline** | Exposed + PostGIS | ✅ Implemented |
| 5 | **NetCDF Metadata Catalog** | UCAR netCDF-Java (schema + repo only) | ⚠️ Partial |

> **NetCDF status**: `NetCdfFileTable`, `NetCdfGridValueTable`, `NetCdfFileRepository`, and all model
> classes are fully implemented and tested. `NetCdfCatalogService` (actual `.nc` file reading) requires
> `edu.ucar:netcdfAll` and is planned for Phase 5.

---

## Architecture

### Integrated Module Overview

![Integrated Module Overview 1](../../docs/images/readme-diagrams/utils-science-diagram-01.svg)

### Coordinate Transformation Flow

![Coordinate Transformation Flow 2](../../docs/images/readme-diagrams/utils-science-diagram-02.svg)

### PostGIS + NetCDF Database Schema

![PostGIS + NetCDF Database Schema 3](../../docs/images/readme-diagrams/utils-science-diagram-03.svg)

---

## Module Layout

```
io.bluetape4k.science/
├── coords/                          — Coordinate primitives
│   ├── GeoLocation.kt              — WGS84 lat/lon, Haversine distance
│   ├── BoundingBox.kt              — Rectangular boundary, contains/intersects
│   ├── BoundingBoxRelation.kt      — Relationship computation
│   ├── DM.kt / DMS.kt              — Degree-minute / degree-minute-second notation
│   ├── Vector.kt                   — 2D/3D vector
│   ├── UtmZone.kt                  — UTM zone data class
│   ├── UtmZoneSupport.kt           — utmZoneOf(), boundingBox()
│   └── CoordConverters.kt          — Decimal ↔ DM/DMS utilities
│
├── projection/                      — CRS transforms (Proj4J)
│   ├── CrsRegistry.kt              — EPSG/Proj4 registry with instance caching
│   └── Projections.kt              — wgs84ToUtm(), utmToWgs84(), transform()
│
├── shapefile/                       — Shapefile I/O (GeoTools)
│   ├── ShapeModels.kt              — Shape, ShapeRecord, ShapeHeader (GeoTools-free public API)
│   ├── ShapefileReader.kt          — Synchronous reader
│   └── ShapefileExtensions.kt      — loadShape(), loadShapeAsync()
│
├── geometry/                        — Spatial geometry (JTS)
│   ├── GeometryOperations.kt       — intersection, union, buffer, simplify, distance
│   └── PolygonExtensions.kt        — area, perimeter
│
└── exposed/                         — Database pipelines
    ├── model/
    │   ├── SpatialModels.kt        — SpatialLayerRecord, SpatialFeatureRecord
    │   └── NetCdfModels.kt         — NetCdfFileRecord, NetCdfVariableInfo, NetCdfDimensionInfo
    ├── schema/
    │   ├── SpatialTables.kt        — SpatialLayerTable, SpatialFeatureTable
    │   ├── PoiTable.kt             — Point of Interest table
    │   └── NetCdfTables.kt         — NetCdfFileTable, NetCdfGridValueTable
    ├── repository/
    │   ├── SpatialFeatureRepository.kt — Spatial feature CRUD + bbox search
    │   └── NetCdfFileRepository.kt — NetCDF file metadata CRUD
    └── service/
        ├── ShapefileImportService.kt — Virtual Thread batch importer
        └── NetCdfCatalogService.kt  — ⚠️ Phase 5 — placeholder only
```

---

## Features

| Domain | Feature | API |
|--------|---------|-----|
| **Coordinates** | WGS84 lat/lon with Haversine distance | `GeoLocation.distanceTo()` |
| | Rectangular bounding box | `BoundingBox.contains()`, `.intersects()` |
| | Degree-minute-second notation | `DMS.parse()`, `.toDecimal()` |
| | UTM zone detection | `utmZoneOf(lat, lon)` |
| | 2D/3D vector math | `Vector(x, y, z?)` |
| **Projection** | WGS84 ↔ UTM conversion | `wgs84ToUtm()`, `utmToWgs84()` |
| | Arbitrary EPSG conversion | `transform(x, y, srcEpsg, tgtEpsg)` |
| | CRS instance caching | `CrsRegistry` |
| **Shapefile** | Synchronous Shapefile reading | `loadShape(file)` |
| | Coroutine-based async reading | `loadShapeAsync(file)` |
| | Type-safe models (no GeoTools leakage) | `Shape`, `ShapeRecord` |
| **Geometry** | JTS intersection / union / difference | `GeometryOperations.intersection()` |
| | Buffer zone creation | `GeometryOperations.buffer()` |
| | Douglas-Peucker simplification | `GeometryOperations.simplify()` |
| | Distance calculation | `GeometryOperations.distance()` |
| **Database** | Spatial layer + feature CRUD | `SpatialLayerRepository`, `SpatialFeatureRepository` |
| | Virtual Thread batch Shapefile import | `ShapefileImportService` |
| | NetCDF file metadata catalog | `NetCdfFileRepository` ✅ |
| | NetCDF grid value storage schema | `NetCdfGridValueTable` ✅ |
| | `.nc` file import service | `NetCdfCatalogService` ⚠️ Phase 5 |

---

## Quick Start

### 5.1 GIS Coordinate Conversion

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

// Haversine distance
val distanceKm = seoul.distanceTo(tokyo) / 1000.0
println("Seoul ↔ Tokyo: $distanceKm km")

// Bounding box
val seoulArea = BoundingBox(minLat = 37.4, maxLat = 37.6, minLon = 126.8, maxLon = 127.0)
println("Contains Seoul City Hall: ${seoulArea.contains(seoul)}")
println("Center: ${seoulArea.center}, Width: ${seoulArea.widthKm} km")

// Degree-minute-second
val dms = DMS.parse("37°33'59.4\"N")
println("DMS → decimal: ${dms.toDecimal()}")  // 37.5665

// UTM zone
val zone = utmZoneOf(37.5665, 126.9780)
println("Seoul UTM Zone: ${zone.longitudeZone}${zone.hemisphere}")  // 52S

// WGS84 ↔ UTM roundtrip
val (easting, northing) = wgs84ToUtm(seoul)
val restored = utmToWgs84(easting, northing, zone)
println("Roundtrip: $restored")

// Arbitrary EPSG transform (WGS84 → Korea 2000 Central Belt)
val (kx, ky) = transform(x = 126.9780, y = 37.5665, sourceEpsg = 4326, targetEpsg = 5179)
println("EPSG:4326 → EPSG:5179: ($kx, $ky)")
```

### 5.2 Shapefile Processing

```kotlin
import io.bluetape4k.science.shapefile.loadShape
import io.bluetape4k.science.shapefile.loadShapeAsync
import java.io.File

// Synchronous
val shapeFile = File("/data/provinces.shp")
val shape = loadShape(shapeFile, charset = Charsets.UTF_8)
println("Type: ${shape.shapeType}, Records: ${shape.recordCount}")

shape.records.forEach { record ->
    println("Geometry: ${record.geometry.geometryType}")
    println("Attributes: ${record.attributes}")
}

// Asynchronous (Coroutines — dispatches to Dispatchers.IO)
suspend fun processAsync() {
    val large = loadShapeAsync(File("/data/large_dataset.shp"))
    println("Loaded ${large.recordCount} records")
}
```

### 5.3 JTS Geometry Operations

```kotlin
import io.bluetape4k.science.geometry.GeometryOperations
import org.locationtech.jts.io.WKTReader

val wkt = WKTReader()
val poly1 = wkt.read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
val poly2 = wkt.read("POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))")

val intersection = GeometryOperations.intersection(poly1, poly2)
val union        = GeometryOperations.union(poly1, poly2)
val buffered     = GeometryOperations.buffer(poly1, 100.0)
val simplified   = GeometryOperations.simplify(poly1, 1.0)
val distance     = GeometryOperations.distance(poly1, poly2)
println("Distance: $distance m")
```

### 5.4 PostGIS Data Pipeline

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
println("Imported: $importedCount records")
```

### 5.5 NetCDF Metadata Catalog

The DB schema and repository are ready. Register NetCDF file metadata directly using `NetCdfFileRepository`.

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
    println("Saved: id=${saved.id}, filename=${saved.filename}")

    val found = repo.findByIdOrNull(saved.id)
    println("Variables: ${found?.variables?.map { it.name }}")  // [temperature, precipitation]
    println("Time steps: ${found?.dimensions?.get("time")}")     // 24
}
```

---

## API Guide

### coords

| Class / Function | Description |
|------------------|-------------|
| `GeoLocation(lat, lon)` | WGS84 coordinate; `.distanceTo()` for Haversine distance |
| `BoundingBox(minLat, maxLat, minLon, maxLon)` | Rectangular boundary; `.contains()`, `.intersects()` |
| `DMS.parse(str)` / `DM.parse(str)` | Parse degree-minute-second / degree-minute strings |
| `UtmZone(zone, hemisphere)` | UTM zone data class |
| `utmZoneOf(lat, lon)` | Auto-detect UTM zone from WGS84 coordinates |
| `Vector(x, y, z?)` | 2D/3D vector with arithmetic operations |

### projection

| Function | Description |
|----------|-------------|
| `wgs84ToUtm(geoLocation)` | WGS84 → UTM (easting, northing) |
| `utmToWgs84(e, n, zone)` | UTM → WGS84 |
| `transform(x, y, srcEpsg, tgtEpsg)` | Arbitrary EPSG-to-EPSG conversion |
| `CrsRegistry` | Thread-safe CRS instance cache by EPSG code |

### shapefile

| Function | Description |
|----------|-------------|
| `loadShape(file, charset?)` | Synchronous Shapefile reading |
| `loadShapeAsync(file, charset?)` | Coroutine-based async reading (`Dispatchers.IO`) |
| `Shape` | File metadata + record list |
| `ShapeRecord` | Geometry + attribute map (GeoTools-free public API) |

### geometry

| Function | Description |
|----------|-------------|
| `GeometryOperations.intersection(a, b)` | Geometric intersection |
| `GeometryOperations.union(a, b)` | Geometric union |
| `GeometryOperations.buffer(g, dist)` | Buffer zone at given distance |
| `GeometryOperations.simplify(g, tol)` | Douglas-Peucker simplification |
| `GeometryOperations.distance(a, b)` | Minimum distance between geometries |
| `Polygon.area()` / `.perimeter()` | Area and perimeter extensions |

### exposed (PostGIS)

| Class | Description |
|-------|-------------|
| `SpatialLayerRepository` | Layer CRUD (`save`, `findByName`) |
| `SpatialFeatureRepository` | Feature CRUD + PostGIS bbox search |
| `ShapefileImportService` | Virtual Thread batch import from Shapefile |

### exposed (NetCDF)

| Class | Status | Description |
|-------|--------|-------------|
| `NetCdfFileRecord` | ✅ | File metadata model (filename, path, size, variables, dimensions) |
| `NetCdfVariableInfo` | ✅ | Variable descriptor (name, dataType, shape, attributes) |
| `NetCdfDimensionInfo` | ✅ | Dimension descriptor (name, length, isUnlimited) |
| `NetCdfFileRepository` | ✅ | File metadata CRUD (`save`, `findByIdOrNull`, `findAll`, `deleteById`) |
| `NetCdfFileTable` | ✅ | Exposed table — JSONB columns + PostGIS bbox + time range |
| `NetCdfGridValueTable` | ✅ | Grid value table (location: PostGIS POINT, value, timeIdx, levelIdx) |
| `NetCdfCatalogService` | ⚠️ Phase 5 | Placeholder — throws `NotImplementedError` until `netcdfAll` is resolved |

---

## Dependencies

`bluetape4k-science` declares optional feature-specific libraries as `compileOnly`.
Add only what your application uses at runtime.

### Base

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
}
```

### GIS Coordinate Conversion (Proj4J)

```kotlin
implementation(Libs.proj4j)
implementation(Libs.proj4j_epsg)
```

### Shapefile (GeoTools — LGPL)

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

> **License note**: GeoTools uses LGPL. `bluetape4k-science` declares it `compileOnly`.
> Add it explicitly only if your application redistributes GeoTools classes.

### JTS Geometry

```kotlin
implementation(Libs.jts_core)
```

### PostGIS Database

```kotlin
implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:${bluetape4kVersion}")
implementation(Libs.postgis_jdbc)
```

### Coroutines (async Shapefile reading)

```kotlin
implementation("io.github.bluetape4k:bluetape4k-coroutines:${bluetape4kVersion}")
implementation(Libs.kotlinx_coroutines_core)
```

### NetCDF (Phase 5 — not yet required)

When `NetCdfCatalogService` is complete, you will need:

```kotlin
repositories {
    maven(url = "https://artifacts.unidata.ucar.edu/repository/unidata-all/") { name = "Unidata" }
}
dependencies {
    implementation("edu.ucar:netcdfAll:5.6.0")
}
```

### Full Example

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

## Tests

Integration tests run against a Testcontainers-managed PostgreSQL + PostGIS container (`postgis/postgis:16-3.4`).

### NetCDF Catalog Test

```kotlin
@Testcontainers
class NetCdfTableTest : AbstractPostgisTest() {

    private val repo = NetCdfFileRepository()

    @Test
    fun `save and retrieve NetCDF file record`() {
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
}
```

### Shapefile Import Test

```kotlin
@Test
fun `import shapefile into PostGIS`() {
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

## Performance / Operations

### Coordinate Transforms

- **CRS caching**: `CrsRegistry` caches instances by EPSG code — repeated transforms have near-zero overhead.
- **Thread safety**: `CrsRegistry` uses a concurrent map; safe for multi-threaded use.

### Shapefile Processing

- Use `loadShapeAsync()` for large files — dispatches to `Dispatchers.IO`, non-blocking.
- Stream records lazily for memory-efficient processing of multi-GB Shapefiles.

### PostGIS Database

- **Spatial indexes**: `CREATE INDEX ON spatial_features USING GIST (geom)` for fast bbox queries.
- **Batch loading**: `ShapefileImportService` processes rows in configurable batches (default: 1000) via Virtual Threads.
- **Connection pooling**: Use HikariCP or Exposed's built-in pool.

### JTS Geometry

- Apply `GeometryOperations.simplify()` before persistence to reduce coordinate count.
- Set a consistent `PrecisionModel` in your `GeometryFactory` for reproducible results.

### NetCDF Catalog

- `NetCdfFileTable` stores `bbox` (PostGIS POLYGON) and `timeStart/timeEnd` for spatio-temporal filtering.
- Add index on `netcdf_files(time_start, time_end)` for time-range queries.
- Add composite index on `netcdf_grid_values(file_id, variable_name)` for per-variable lookups.

---

## Related Modules

| Module | Purpose |
|--------|---------|
| `bluetape4k-core` | Core utilities (compression, assertions) |
| `bluetape4k-coroutines` | Coroutine extensions (Flow, DeferredValue) |
| `bluetape4k-exposed-postgresql` | PostGIS column types |
| `bluetape4k-exposed-jdbc` | Exposed JDBC repository base |
| `bluetape4k-testing-testcontainers` | Testcontainers helpers |
