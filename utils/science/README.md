# Module bluetape4k-science

English | [한국어](./README.ko.md)

An integrated module for GIS coordinate conversion, Shapefile processing, JTS geometry operations, and PostGIS data-loading pipelines.

It includes coordinate transforms based on Proj4J, GeoTools-backed Shapefile parsing, JTS spatial geometry operations, and database pipelines built on Exposed + PostGIS.

## Core Features

### Coordinate Primitive Types (`coords` package)

**GeoLocation** — WGS84 latitude and longitude coordinates
- latitude: -90 to 90, longitude: -180 to 180
- distance calculation with the Haversine formula
- predefined locations such as `SEOUL`, `NEW_YORK`, and `TOKYO`

**BoundingBox** — rectangular bounding area
- check whether coordinates are contained
- intersection and union calculation
- calculate center point, width, and height

**DM / DMS** — degree-minute / degree-minute-second notation
- parse formats such as `37°33'59.4"N`
- convert to and from `GeoLocation`

**UtmZone** — UTM coordinate system
- automatic zone detection from latitude/longitude through `utmZoneOf()`
- Easting / Northing conversion

**Vector** — 2D / 3D vector operations

**CoordConverters** — coordinate conversion utilities
- decimal degrees ↔ DM / DMS conversion
- coordinate normalization

### Coordinate Transformation and Projection (`projection` package)

**Projections** — transforms based on Proj4J
- `wgs84ToUtm()` — WGS84 → UTM
- `utmToWgs84()` — UTM → WGS84
- `transform()` — arbitrary coordinate transforms between EPSG codes

**CrsRegistry** — CRS registry
- supports EPSG codes and Proj4 strings
- improves performance through instance caching

### Shapefile Reading (`shapefile` package)

**ShapefileReader / loadShape()** — synchronous Shapefile reading
- automatically handles `.shp`, `.shx`, and `.dbf` files
- returns geometry plus attributes together
- supports UTF-8 and custom charsets

**loadShapeAsync()** — asynchronous reading
- coroutine-based, using `Dispatchers.IO`
- optimized for large files

**ShapeModels** — type-safe models
- `Shape`: file metadata
- `ShapeRecord`: geometry + attributes
- `ShapeHeader`: file header information
- does not expose GeoTools types in the public API

**Supported geometry types**
- Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon

### Spatial Geometry Operations (`geometry` package)

**GeometryOperations** — JTS-based operations
- intersection, union, and difference
- buffer generation with a specified distance
- distance calculation
- simplification with the Douglas-Peucker algorithm
- envelope (minimum bounding rectangle)
- containment checks

**PolygonExtensions** — polygon extensions
- area calculation
- perimeter calculation

### PostGIS Database Pipeline (`exposed` package)

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


```

**Schema** — Exposed table definitions
- `SpatialLayerTable` / `SpatialFeatureTable` — storage for spatial data
- `PoiTable` — points of interest
- `NetCdfFileTable` / `NetCdfGridValueTable` — NetCDF metadata (Phase 4)

**Models** — serializable data classes
- `SpatialLayerRecord` / `SpatialFeatureRecord` — spatial data
- `NetCdfVariableInfo`, `NetCdfDimensionInfo`, `NetCdfFileRecord` — NetCDF (Phase 4)

**Repository** — JDBC repositories
- `SpatialLayerRepository` — layer management
- `SpatialFeatureRepository` — feature CRUD and spatial search
- `NetCdfRepository` — NetCDF catalog (Phase 4)

**Service** — business logic
- `ShapefileImportService.importShapefile()` — batch import based on virtual threads
- `NetCdfCatalogService` — NetCDF file registration (Phase 4)

## Architecture

```
coords (coordinate primitive types)
  ├─ GeoLocation (latitude / longitude)
  ├─ BoundingBox (bounding rectangle)
  ├─ DM / DMS (degree-minute / degree-minute-second)
  ├─ UtmZone (UTM coordinate system)
  └─ Vector (vector)
    │
    └─→ projection (coordinate transformation)
          ├─ Projections (based on Proj4J)
          │  ├─ wgs84ToUtm()
          │  ├─ utmToWgs84()
          │  └─ transform() [EPSG]
          └─ CrsRegistry (caching)
              │
              ├─→ shapefile (Shapefile reading)
              │     ├─ loadShape() [sync]
              │     ├─ loadShapeAsync() [async]
              │     └─ ShapeModels
              │          │
              │          └─→ exposed (PostGIS pipeline)
              │                ├─ schema/ (tables)
              │                ├─ model/ (serialized data)
              │                ├─ repository/ (JDBC)
              │                └─ service/ (business logic)
              │
              └─→ geometry (JTS geometry operations)
                    ├─ GeometryOperations
                    │  ├─ intersection()
                    │  ├─ buffer()
                    │  ├─ simplify()
                    │  └─ distance()
                    └─ PolygonExtensions
                         │
                         └─→ exposed (database loading)
```

## Installation and Dependencies

`bluetape4k-science` declares optional, feature-specific dependencies through `compileOnly`. Add only the libraries you actually need at runtime.

### Basic Installation

```kotlin
dependencies {
    implementation("io.github.bluetape4k:bluetape4k-science:${bluetape4kVersion}")
}
```

## Package Structure

- `coords`: coordinate primitives and coordinate notation helpers
- `projection`: CRS registry and coordinate transforms
- `shapefile`: sync and async Shapefile loading
- `geometry`: JTS-based spatial operations
- `exposed`: PostGIS persistence pipeline

## Main API Usage Examples

- coordinate transforms between WGS84 and UTM
- reading large Shapefiles synchronously or asynchronously
- geometry operations before persistence
- importing spatial datasets into PostgreSQL / PostGIS through Exposed repositories and services

## Tests (Testcontainers + PostGIS)

Integration tests can be run with Testcontainers-backed PostgreSQL / PostGIS environments. The Korean README includes the full setup and sample test scenarios.

## Performance Optimization

- cache CRS instances through `CrsRegistry`
- use `loadShapeAsync()` for large files
- process imports in batches through the PostGIS pipeline

## Phase 4: NetCDF Support (Planned)

Planned support includes NetCDF metadata cataloging and grid-value persistence through the same `exposed` package pipeline.

## Related Modules

- `data/exposed-postgresql`
- `data/exposed-jdbc`
- `testing/testcontainers`

## API Summary

- coordinate primitives and conversions
- CRS registry and projection transforms
- sync / async Shapefile loading
- JTS geometry helpers
- PostGIS loading pipeline and repositories
