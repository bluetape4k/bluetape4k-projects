# Module bluetape4k-science

English | [한국어](./README.ko.md)

An integrated module for GIS coordinate conversion, Shapefile processing, JTS geometry operations, and PostGIS data-loading pipelines.

It includes coordinate transforms based on Proj4J, GeoTools-backed Shapefile parsing, JTS spatial geometry operations, and database pipelines built on Exposed + PostGIS.

## Architecture

### Module Overview

```mermaid
flowchart TD
    Science["bluetape4k-science"]

    subgraph Coords["coords — Coordinate Primitives"]
        GeoLocation["GeoLocation\nWGS84 lat/lon"]
        BoundingBox["BoundingBox\nbounding rectangle"]
        DMS["DM / DMS\ndegree-minute-second"]
        UtmZone["UtmZone\nUTM coordinate system"]
        Vector["Vector\n2D/3D"]
    end

    subgraph Projection["projection — CRS Transforms"]
        Projections["Projections\nwgs84ToUtm / utmToWgs84\ntransform(EPSG)"]
        CrsRegistry["CrsRegistry\nEPSG cache"]
    end

    subgraph Shapefile["shapefile — Shapefile I/O"]
        LoadShape["loadShape() sync"]
        LoadShapeAsync["loadShapeAsync() async"]
        ShapeModels["ShapeModels\nShape / ShapeRecord"]
    end

    subgraph Geometry["geometry — JTS Operations"]
        GeomOps["GeometryOperations\nintersection / union / buffer\nsimplify / distance"]
        PolyExt["PolygonExtensions\narea / perimeter"]
    end

    subgraph Exposed["exposed — PostGIS Pipeline"]
        Schema["schema/\nSpatialLayerTable\nSpatialFeatureTable"]
        Models["model/\nSpatialLayerRecord\nSpatialFeatureRecord"]
        Repos["repository/\nSpatialLayerRepository\nSpatialFeatureRepository"]
        Service["service/\nShapefileImportService"]
    end

    Science --> Coords
    Science --> Projection
    Science --> Shapefile
    Science --> Geometry
    Science --> Exposed

    Coords --> Projection
    Projection --> Shapefile
    Shapefile --> Exposed
    Geometry --> Exposed

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef utilStyle fill:#FFF3E0,stroke:#FFCC80,color:#E65100
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000
    classDef dslStyle fill:#E0F7FA,stroke:#80DEEA,color:#00695C

    class Science coreStyle
    class GeoLocation,BoundingBox,DMS,UtmZone,Vector dataStyle
    class Projections,CrsRegistry serviceStyle
    class LoadShape,LoadShapeAsync,ShapeModels utilStyle
    class GeomOps,PolyExt serviceStyle
    class Schema,Models,Repos,Service dslStyle
```

---

### Coordinate Transformation Flow

```mermaid
flowchart TD
    A[WGS84 GeoLocation\nlat, lon] -->|wgs84ToUtm| B[UTM Zone detection\nutmZoneOf]
    B --> C{Southern hemisphere?}
    C -->|latBand < N| D[proj4: +south]
    C -->|latBand >= N| E[proj4: northern]
    D --> F[BasicCoordinateTransform]
    E --> F
    F --> G[UTM coordinates\neasting, northing]

    G -->|utmToWgs84| H[UtmZone input]
    H --> I{latitudeZone < N?}
    I -->|Yes| J[proj4: +south]
    I -->|No| K[proj4: northern]
    J --> L[BasicCoordinateTransform]
    K --> L
    L --> M[WGS84 GeoLocation]

    classDef coreStyle fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32,font-weight:bold
    classDef serviceStyle fill:#E3F2FD,stroke:#90CAF9,color:#1565C0
    classDef dataStyle fill:#F57F17,stroke:#F57F17,color:#000000

    class A,M coreStyle
    class F,L serviceStyle
    class G,H dataStyle
```

---

### PostGIS Database Pipeline Class Diagram

```mermaid
classDiagram
    direction TB

    class SpatialLayerRecord {
        <<dataClass>>
        +id: Long
        +name: String
        +srid: Int
        +geometryType: String?
        +recordCount: Int
    }
    class SpatialFeatureRecord {
        <<dataClass>>
        +id: Long
        +layerId: Long
        +featureType: String
        +geom: Geometry
        +properties: Map
    }
    class NetCdfFileRecord {
        <<dataClass>>
        +id: Long
        +filename: String
        +variables: List~NetCdfVariableInfo~
    }

    class SpatialLayerTable {
        <<AuditableLongIdTable>>
        +name: Column~String~
        +srid: Column~Int~
        +geometryType: Column~String?~
    }
    class SpatialFeatureTable {
        <<AuditableLongIdTable>>
        +layerId: Column~EntityID~
        +geom: Column~PGGeometry~
        +properties: Column~Map~
    }
    class NetCdfFileTable {
        <<AuditableLongIdTable>>
        +filename: Column~String~
        +variables: Column~List~
    }

    class SpatialLayerRepository {
        <<LongJdbcRepository>>
        +save(SpatialLayerRecord): SpatialLayerRecord
        +findByName(String): SpatialLayerRecord?
    }
    class SpatialFeatureRepository {
        <<LongJdbcRepository>>
        +save(SpatialFeatureRecord): SpatialFeatureRecord
    }
    class NetCdfFileRepository {
        <<LongJdbcRepository>>
        +save(NetCdfFileRecord): NetCdfFileRecord
    }

    class ShapefileImportService {
        -layerRepo: SpatialLayerRepository
        -featureRepo: SpatialFeatureRepository
        +importShapefile(File, String, Int): Int
    }

    SpatialLayerRepository --> SpatialLayerTable : uses
    SpatialLayerRepository --> SpatialLayerRecord : maps
    SpatialFeatureRepository --> SpatialFeatureTable : uses
    SpatialFeatureRepository --> SpatialFeatureRecord : maps
    NetCdfFileRepository --> NetCdfFileTable : uses
    NetCdfFileRepository --> NetCdfFileRecord : maps
    ShapefileImportService --> SpatialLayerRepository : delegates
    ShapefileImportService --> SpatialFeatureRepository : delegates
    SpatialFeatureTable --> SpatialLayerTable : references

    style SpatialLayerRecord fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style SpatialFeatureRecord fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style NetCdfFileRecord fill:#FFFDE7,stroke:#FFF176,color:#F57F17
    style SpatialLayerTable fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style SpatialFeatureTable fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style NetCdfFileTable fill:#E0F7FA,stroke:#80DEEA,color:#00695C
    style SpatialLayerRepository fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style SpatialFeatureRepository fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style NetCdfFileRepository fill:#E0F2F1,stroke:#80CBC4,color:#00695C
    style ShapefileImportService fill:#E8F5E9,stroke:#A5D6A7,color:#2E7D32
```

---

### Shapefile Import Sequence

```mermaid
sequenceDiagram
    box "Consumer" #E8F5E9
    participant Caller
    end
    box "Service" #E3F2FD
    participant SIS as ShapefileImportService
    participant VT as VirtualThread
    end
    box "Repositories" #FFF3E0
    participant LR as SpatialLayerRepository
    participant FR as SpatialFeatureRepository
    end
    box "Database" #F3E5F5
    participant DB as PostgreSQL/PostGIS
    end

    Caller->>SIS: importShapefile(file, layerName, batchSize)
    SIS->>SIS: loadShape(file)

    SIS->>VT: newVirtualThreadJdbcTransaction
    VT->>LR: findByName(layerName)
    LR->>DB: SELECT
    DB-->>LR: null (no duplicate)
    VT->>LR: save(SpatialLayerRecord)
    LR->>DB: INSERT spatial_layers
    DB-->>LR: layerRecord (id)
    VT-->>SIS: layerRecord

    loop Per batch (batchSize=1000)
        SIS->>VT: newVirtualThreadJdbcTransaction
        VT->>DB: batchInsert spatial_features
        DB-->>VT: inserted rows
        VT-->>SIS: batch complete
    end

    SIS-->>Caller: totalInserted count
```

---

## Key Features

- **Coordinate Primitives**: `GeoLocation` (WGS84), `BoundingBox`, `DM/DMS`, `UtmZone`, `Vector`
- **Coordinate Transforms**: Proj4J-based WGS84 ↔ UTM and arbitrary EPSG code transformations via `CrsRegistry`
- **Shapefile I/O**: Synchronous and async Shapefile reading; type-safe `ShapeModels` without exposing GeoTools types
- **JTS Geometry**: `GeometryOperations` — intersection, union, difference, buffer, simplify, distance
- **PostGIS Pipeline**: `SpatialLayerTable/Repository` + `SpatialFeatureTable/Repository` +
  `ShapefileImportService` backed by Virtual Threads

## Usage Examples

### Coordinate Primitives

**GeoLocation — WGS84 latitude/longitude**

```kotlin
import io.bluetape4k.science.coords.GeoLocation

val seoul = GeoLocation(latitude = 37.5665, longitude = 126.9780)
val tokyo = GeoLocation(latitude = 35.6762, longitude = 139.6503)

// Haversine distance (meters)
val distanceMeters = seoul.distanceTo(tokyo)
val distanceKm = distanceMeters / 1000.0
println("Seoul↔Tokyo: $distanceKm km")

// Predefined locations
val newYork = GeoLocation.NEW_YORK
val london = GeoLocation.LONDON
```

**BoundingBox — rectangular boundary**

```kotlin
import io.bluetape4k.science.coords.BoundingBox

val seoulArea = BoundingBox(
    minLat = 37.4, maxLat = 37.6,
    minLon = 126.8, maxLon = 127.0
)

if (seoulArea.contains(seoul)) {
    println("Seoul City Hall is within the area")
}

println("Center: ${seoulArea.center}")
println("Width: ${seoulArea.widthKm} km")
println("Height: ${seoulArea.heightKm} km")
```

**DMS — degree-minute-second notation**

```kotlin
import io.bluetape4k.science.coords.DMS

val dms = DMS.parse("37°33'59.4\"N")
val decimal = dms.toDecimal()  // 37.5665
println("DMS → decimal: $decimal")

val dmsStr = DMS(degree = 37, minute = 33, second = 59.4, direction = 'N').toString()
println("Decimal → DMS: $dmsStr")
```

**UtmZone — UTM coordinate system**

```kotlin
import io.bluetape4k.science.coords.utmZoneOf

val zone = utmZoneOf(37.5665, 126.9780)
println("Seoul: UTM Zone ${zone.longitudeZone}${zone.hemisphere}")  // 52S

val bbox = zone.boundingBox()
println("Zone boundary: $bbox")
```

### Coordinate Transformation

**WGS84 ↔ UTM**

```kotlin
import io.bluetape4k.science.projection.wgs84ToUtm
import io.bluetape4k.science.projection.utmToWgs84
import io.bluetape4k.science.coords.UtmZone

val seoul = GeoLocation(37.5665, 126.9780)
val (easting, northing) = wgs84ToUtm(seoul)
println("WGS84(37.5665, 126.9780) → UTM($easting, $northing)")

val zone = UtmZone(longitudeZone = 52, hemisphere = 'S')
val restored = utmToWgs84(easting, northing, zone)
println("UTM → WGS84: $restored")
```

**EPSG code transformation**

```kotlin
import io.bluetape4k.science.projection.transform

// EPSG:4326 (WGS84) → EPSG:5179 (Korea 2000 Central Belt)
val (transformedX, transformedY) = transform(
    x = 126.9780,
    y = 37.5665,
    sourceEpsg = 4326,
    targetEpsg = 5179
)
println("EPSG:4326 → EPSG:5179: ($transformedX, $transformedY)")
```

### Shapefile Reading

**Synchronous**

```kotlin
import io.bluetape4k.science.shapefile.loadShape
import java.io.File

val shapeFile = File("/data/provinces.shp")
val shape = loadShape(shapeFile, charset = Charsets.UTF_8)

println("Type: ${shape.shapeType}, Records: ${shape.recordCount}")

shape.records.forEach { record ->
    println("Geometry: ${record.geometry.geometryType}")
    println("Attributes: ${record.attributes}")
}
```

**Asynchronous (Coroutines)**

```kotlin
import io.bluetape4k.science.shapefile.loadShapeAsync
import java.io.File

suspend fun processLargeShapefile() {
    val shapeFile = File("/data/large_dataset.shp")

    // Processes on Dispatchers.IO
    val shape = loadShapeAsync(shapeFile)

    shape.records.forEach { record ->
        // process geometry
    }
}
```

### JTS Geometry Operations

```kotlin
import io.bluetape4k.science.geometry.GeometryOperations
import org.locationtech.jts.io.WKTReader

val wkt = WKTReader()

val poly1 = wkt.read("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))")
val poly2 = wkt.read("POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))")

// Intersection
val intersection = GeometryOperations.intersection(poly1, poly2)

// Union
val union = GeometryOperations.union(poly1, poly2)

// Buffer (100m radius)
val buffered = GeometryOperations.buffer(poly1, 100.0)

// Simplify (Douglas-Peucker, tolerance=1.0)
val simplified = GeometryOperations.simplify(poly1, 1.0)

// Distance
val distance = GeometryOperations.distance(poly1, poly2)
println("Distance: $distance m")
```

### PostGIS Database Pipeline

```kotlin
import io.bluetape4k.science.exposed.service.ShapefileImportService
import io.bluetape4k.science.exposed.repository.SpatialFeatureRepository
import io.bluetape4k.science.exposed.repository.SpatialLayerRepository
import org.jetbrains.exposed.sql.Database
import java.io.File

val database = Database.connect(
    url = "jdbc:postgresql://localhost:5432/gis_db",
    driver = "org.postgresql.Driver",
    user = "postgres",
    password = "password"
)

val layerRepo = SpatialLayerRepository()
val featureRepo = SpatialFeatureRepository()
val service = ShapefileImportService(layerRepo, featureRepo)

val shapeFile = File("/data/harbors.shp")
val importedCount = service.importShapefile(
    file = shapeFile,
    layerName = "harbors-2024"
)
println("Imported: $importedCount records")
```

## Tests (Testcontainers + PostGIS)

Integration tests can be run with Testcontainers-backed PostgreSQL / PostGIS environments. The Korean README includes the full setup and sample test scenarios.

## Performance Optimization

- Cache CRS instances through `CrsRegistry`
- Use `loadShapeAsync()` for large files
- Process imports in batches through the PostGIS pipeline
- Use PostGIS GIST/BRIN spatial indexes for range queries

## Phase 4: NetCDF Support (Planned)

Planned support includes NetCDF metadata cataloging and grid-value persistence through the same
`exposed` package pipeline.

## Related Modules

- `data/exposed-postgresql`
- `data/exposed-jdbc`
- `testing/testcontainers`

## Installation and Dependencies

`bluetape4k-science` declares optional, feature-specific dependencies through
`compileOnly`. Add only the libraries you actually need at runtime.

### Basic Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
}
```

### Feature-specific Dependencies

**Coordinate transformation (Proj4J)**

```kotlin
implementation(Libs.proj4j)
implementation(Libs.proj4j_epsg)
```

**Shapefile reading (GeoTools — LGPL)**

```kotlin
// repositories block
maven(url = "https://repo.osgeo.org/repository/release/") { name = "OSGeo Release" }

// dependencies
implementation(Libs.geotools_shapefile)
implementation(Libs.geotools_referencing)
implementation(Libs.geotools_epsg_hsql)
```

**Spatial geometry (JTS)**

```kotlin
implementation(Libs.jts_core)
```

**PostGIS database**

```kotlin
implementation("io.github.bluetape4k:bluetape4k-exposed-postgresql:${bluetape4kVersion}")
implementation(Libs.postgis_jdbc)
```
