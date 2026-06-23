# Issue 844 Review - geo README examples

## Scope

- `utils/geo/README.md`
- `utils/geo/README.ko.md`
- `utils/geo/src/test/kotlin/io/bluetape4k/geo/GeoReadmeContractTest.kt`

## Review Notes

- README examples now import the implemented public packages:
  `io.bluetape4k.geohash`, `io.bluetape4k.geocode.*`, and
  `io.bluetape4k.geoip2.*`.
- The Google example uses `GoogleAddressFinder`, matching the implemented
  reverse geocode abstraction.
- The GeoIP2 example uses `Geoip.cityDatabase.tryFindCity`, matching the current
  `DatabaseReader` extension API.
- The installation examples no longer expose `Libs.*` catalog aliases.
- English and Korean READMEs were updated with equivalent examples.

## Verification

- RED: `GeoReadmeContractTest` failed on stale `io.bluetape4k.geo.geohash`.
- GREEN: `GeoReadmeContractTest` passed after README updates.
- `./gradlew :bluetape4k-geo:compileKotlin :bluetape4k-geo:compileTestKotlin :bluetape4k-geo:test --no-build-cache`
- `rg -n "io\\.bluetape4k\\.geo\\.(geohash|geocode|geoip2)|GoogleGeocoder|GeoIp2Support|Libs\\." utils/geo/README.md utils/geo/README.ko.md`
- `git diff --check`
