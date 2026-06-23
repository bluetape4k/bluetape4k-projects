# Issue #854 Shapefile CRS Import

Issue #854 found that `ShapefileImportService` stored every shapefile as WGS84
without reading `.prj` metadata. Projected inputs such as EPSG:3857 therefore
kept meter coordinates while the layer metadata claimed SRID 4326.

## Decision

Transform shapefile records to EPSG:4326 during import when `.prj` declares a
different CRS. Store PostGIS geometry as EWKT with `SRID=4326` and recompute
layer bbox from transformed geometries.

## Lessons

- Layer metadata and geometry SRID must be proven together. A layer-level
  `srid=4326` is not enough when `PGgeometry` is created from plain WKT.
- Dynamic shapefile fixtures are better than checked-in projected fixtures for
  this case. They keep the test small and verify `.prj` generation, CRS parsing,
  coordinate transformation, SRID storage, and bbox semantics in one path.
- Missing `.prj` remains a compatibility assumption: treat the input as WGS84,
  but document that behavior so callers know the boundary.

## Verification

- `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest.Shapefile import transforms projected CRS to WGS84" --no-build-cache`
- `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest" --no-build-cache`
- `./gradlew :bluetape4k-science:test --no-build-cache`
