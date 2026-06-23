# Issue #854 Shapefile CRS Import Review

## Scope

- `utils/science/src/main/kotlin/io/bluetape4k/science/exposed/service/ShapefileImportService.kt`
- `utils/science/src/test/kotlin/io/bluetape4k/science/exposed/repository/SpatialFeatureRepositoryTest.kt`
- `utils/science/README.md`
- `utils/science/README.ko.md`

## Verdict

Local 7-tier equivalent review: APPROVE.

P0/P1 findings: 0.

## Review Notes

| Lens | Finding | Severity | Resolution |
|---|---|---:|---|
| Correctness | Projected shapefiles now read `.prj`, transform to EPSG:4326, recompute bbox, and write EWKT with SRID 4326. | P0/P1 | Covered by projected EPSG:3857 PostGIS test. |
| Stability | Missing `.prj` keeps the previous WGS84 assumption instead of introducing a new runtime rejection path. | P2 | Documented in both READMEs. |
| Security | No new external input execution, credentials, SQL string interpolation, or file deletion paths. | P3 | SQL in test uses prepared statements. |
| Performance | One transform is created per import, then reused for all records. Per-record geometry transform is required for CRS correctness. | P3 | Acceptable for bugfix scope. |
| API/user | Public import contract now states CRS behavior and SRID storage. | P2 | README and README.ko updated. |
| Test evidence | RED failed on `ST_SRID=0`; GREEN passed targeted test, class test, and full `:bluetape4k-science:test`. | P0/P1 | Evidence recorded in PR DoD. |

## Verification

- RED: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest.Shapefile import transforms projected CRS to WGS84" --no-build-cache` failed with `Expected <0> to equal to <4326>`.
- GREEN targeted: same command passed with 1 passing test.
- Regression: `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest" --no-build-cache` passed with 7 tests.
- Module: `./gradlew :bluetape4k-science:test --no-build-cache` passed with 211 tests.
- Whitespace: `git diff --check` passed.
