# 이슈 #854 Shapefile CRS import

issue #854는 `ShapefileImportService`가 `.prj` metadata를 읽지 않고 모든 shapefile을
WGS84로 저장한다는 점을 찾았다. EPSG:3857 같은 projected input은 meter coordinate를
그대로 유지하면서 layer metadata는 SRID 4326이라고 표시했다.

## 결정

`.prj`가 다른 CRS를 선언하면 import 중 shapefile record를 EPSG:4326으로 transform한다.
PostGIS geometry는 `SRID=4326`을 가진 EWKT로 저장하고, transformed geometry에서 layer
bbox를 다시 계산한다.

## 교훈

- layer metadata와 geometry SRID는 함께 증명해야 한다. `PGgeometry`가 plain WKT로 만들어진
  경우 layer-level `srid=4326`만으로는 충분하지 않다.
- dynamic shapefile fixture는 이 경우 checked-in projected fixture보다 낫다. test를 작게
  유지하면서 `.prj` generation, CRS parsing, coordinate transformation, SRID storage,
  bbox semantic을 한 path에서 검증한다.
- `.prj` 누락은 compatibility assumption으로 남는다. input을 WGS84로 취급하되 caller가
  boundary를 알 수 있도록 문서화한다.

## 검증

- `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest.Shapefile import transforms projected CRS to WGS84" --no-build-cache`
- `./gradlew :bluetape4k-science:test --tests "io.bluetape4k.science.exposed.repository.SpatialFeatureRepositoryTest" --no-build-cache`
- `./gradlew :bluetape4k-science:test --no-build-cache`
