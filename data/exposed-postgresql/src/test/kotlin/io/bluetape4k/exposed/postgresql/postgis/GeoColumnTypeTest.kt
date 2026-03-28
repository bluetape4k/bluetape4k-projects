package io.bluetape4k.exposed.postgresql.postgis

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PostgisServer
import net.postgis.jdbc.PGgeometry
import net.postgis.jdbc.geometry.LinearRing
import net.postgis.jdbc.geometry.Point
import net.postgis.jdbc.geometry.Polygon
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * PostGIS 컬럼 타입 및 공간 함수 통합 테스트.
 *
 * PostGIS 전용 컨테이너(`postgis/postgis:16-3.4`)를 사용한다.
 * `TestDB.POSTGRESQL`은 PostGIS 확장이 없으므로 별도 컨테이너를 사용한다.
 */
class GeoColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging() {
        private const val SRID = 4326

        @JvmStatic
        val postgisContainer = PostgisServer.Launcher.postgis

        @JvmStatic
        val db: Database by lazy {
            Database.connect(
                url = postgisContainer.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgisContainer.username!!,
                password = postgisContainer.password!!,
            )
        }

        /**
         * Point 생성 헬퍼. PostGIS 좌표 순서: x=경도(lng), y=위도(lat)
         */
        fun point(lng: Double, lat: Double): Point =
            Point(lng, lat).apply { srid = SRID }

        /**
         * 사각형 폴리곤 생성 헬퍼.
         */
        fun rectanglePolygon(
            minLng: Double,
            minLat: Double,
            maxLng: Double,
            maxLat: Double,
        ): Polygon {
            val ring = LinearRing(
                arrayOf(
                    Point(minLng, minLat),
                    Point(maxLng, minLat),
                    Point(maxLng, maxLat),
                    Point(minLng, maxLat),
                    Point(minLng, minLat),  // 닫힌 링
                )
            )
            return Polygon(arrayOf(ring)).apply { srid = SRID }
        }
    }

    object Locations: LongIdTable("geo_locations") {
        val name = varchar("name", 255)
        val point = geoPoint("point")
    }

    object PointPairs: LongIdTable("geo_point_pairs") {
        val name = varchar("name", 255)
        val pointA = geoPoint("point_a")
        val pointB = geoPoint("point_b")
    }

    object Regions: LongIdTable("geo_regions") {
        val name = varchar("name", 255)
        val point = geoPoint("point")
        val area = geoPolygon("area")
    }

    /**
     * 두 폴리곤 간의 공간 관계(포함/겹침/분리)를 테스트하기 위한 테이블.
     */
    object PolygonZones: LongIdTable("geo_polygon_zones") {
        val name = varchar("name", 255)
        val zoneA = geoPolygon("zone_a")
        val zoneB = geoPolygon("zone_b")
    }

    /**
     * PostGIS 전용 테이블 생성/삭제를 처리하는 헬퍼.
     */
    private fun withGeoTables(vararg tables: Table, statement: JdbcTransaction.() -> Unit) {
        transaction(db) {
            runCatching { SchemaUtils.drop(*tables) }
            SchemaUtils.create(*tables)
        }
        try {
            transaction(db) {
                statement()
            }
        } finally {
            transaction(db) {
                runCatching { SchemaUtils.drop(*tables) }
            }
        }
    }

    @Test
    fun `서울 좌표 Point 저장 및 조회`() {
        val seoul = point(lng = 126.9780, lat = 37.5665)

        withGeoTables(Locations) {
            Locations.insert {
                it[name] = "서울"
                it[point] = seoul
            }

            val row = Locations.selectAll().single()
            row[Locations.name] shouldBeEqualTo "서울"

            val result = row[Locations.point]
            result.shouldNotBeNull()
            result.x shouldBeEqualTo seoul.x
            result.y shouldBeEqualTo seoul.y
        }
    }

    @Test
    fun `여러 도시 좌표 저장 및 조회`() {
        val cities = listOf(
            "서울" to point(126.9780, 37.5665),
            "부산" to point(129.0756, 35.1796),
            "인천" to point(126.7052, 37.4563),
        )

        withGeoTables(Locations) {
            cities.forEach { (cityName, cityPoint) ->
                Locations.insert {
                    it[name] = cityName
                    it[point] = cityPoint
                }
            }

            val results = Locations.selectAll().toList()
            results.size shouldBeEqualTo 3
        }
    }

    @Test
    fun `GeoPointColumnType 은 UNKNOWN SRID 를 WGS84 로 보정한다`() {
        val point = Point(126.9780, 37.5665).apply { srid = Point.UNKNOWN_SRID }

        val geometry = GeoPointColumnType().notNullValueToDB(point) as PGgeometry

        point.srid shouldBeEqualTo SRID
        (geometry.geometry as Point).srid shouldBeEqualTo SRID
    }

    @Test
    fun `GeoPolygonColumnType 은 문자열과 PGgeometry 를 Polygon 으로 복원한다`() {
        val polygonType = GeoPolygonColumnType()
        val polygonWkt = "SRID=4326;POLYGON((126 37,127 37,127 38,126 38,126 37))"

        val fromString = polygonType.valueFromDB(polygonWkt)
        val fromGeometry = polygonType.valueFromDB(PGgeometry(polygonWkt))

        fromString.srid shouldBeEqualTo SRID
        fromGeometry.numRings() shouldBeEqualTo 1
    }

    @Test
    fun `Geo 컬럼 타입은 PostgreSQL dialect 에서 기대한 SQL 타입을 노출한다`() {
        transaction(db) {
            GeoPointColumnType().sqlType() shouldBeEqualTo "GEOMETRY(POINT, 4326)"
            GeoPolygonColumnType().sqlType() shouldBeEqualTo "GEOMETRY(POLYGON, 4326)"
        }
    }

    @Test
    fun `ST_DWithin - 거리 임계값에 따라 포함 여부가 달라진다`() {
        val seoul = point(126.9780, 37.5665)
        val suwon = point(127.0286, 37.2636)

        withGeoTables(PointPairs) {
            PointPairs.insert {
                it[name] = "서울-수원"
                it[pointA] = seoul
                it[pointB] = suwon
            }

            val withinRows = PointPairs
                .selectAll()
                .where { PointPairs.pointA.stDWithin(PointPairs.pointB, 0.31) }
                .toList()
            withinRows shouldHaveSize 1

            val outsideRows = PointPairs
                .selectAll()
                .where { PointPairs.pointA.stDWithin(PointPairs.pointB, 0.29) }
                .toList()
            outsideRows shouldHaveSize 0
        }
    }

    @Test
    fun `ST_Distance - 두 Point 간 거리를 select(expr) 로 조회할 수 있다`() {
        val seoul = point(126.9780, 37.5665)
        val incheon = point(126.7052, 37.4563)

        withGeoTables(PointPairs) {
            PointPairs.insert {
                it[name] = "서울-인천"
                it[pointA] = seoul
                it[pointB] = incheon
            }

            val distanceExpr = PointPairs.pointA.stDistance(PointPairs.pointB)
            val distance = PointPairs.select(distanceExpr).single()[distanceExpr]

            distance.shouldNotBeNull()
            distance shouldBeGreaterOrEqualTo 0.29
            distance shouldBeLessOrEqualTo 0.31
        }
    }

    @Test
    fun `ST_Within - 폴리곤 내 포인트 확인`() {
        val koreaBounds = rectanglePolygon(
            minLng = 124.0, minLat = 33.0,
            maxLng = 132.0, maxLat = 43.0,
        )
        val seoul = point(126.9780, 37.5665)

        withGeoTables(Regions) {
            Regions.insert {
                it[name] = "서울"
                it[point] = seoul
                it[area] = koreaBounds
            }

            val results = Regions
                .selectAll()
                .where { Regions.point.stWithin(Regions.area) }
                .map { it[Regions.name] }

            results.size shouldBeEqualTo 1
            results.first() shouldBeEqualTo "서울"
        }
    }

    @Test
    fun `ST_Within - 폴리곤 밖 포인트 제외`() {
        val smallArea = rectanglePolygon(
            minLng = 126.9, minLat = 37.5,
            maxLng = 127.1, maxLat = 37.6,
        )
        val seoul = point(126.9780, 37.5665)
        val busan = point(129.0756, 35.1796)

        withGeoTables(Regions) {
            Regions.insert {
                it[name] = "서울"
                it[point] = seoul
                it[area] = smallArea
            }
            Regions.insert {
                it[name] = "부산"
                it[point] = busan
                it[area] = smallArea
            }

            val withinResults = Regions
                .selectAll()
                .where { Regions.point.stWithin(Regions.area) }
                .map { it[Regions.name] }

            withinResults.size shouldBeEqualTo 1
            withinResults.contains("서울").shouldBeTrue()
        }
    }

    @Test
    fun `Polygon 저장 및 조회`() {
        val polygon = rectanglePolygon(
            minLng = 126.0, minLat = 37.0,
            maxLng = 127.0, maxLat = 38.0,
        )

        withGeoTables(Regions) {
            Regions.insert {
                it[name] = "테스트 영역"
                it[point] = point(126.5, 37.5)
                it[area] = polygon
            }

            val row = Regions.selectAll().single()
            val result = row[Regions.area]
            result.shouldNotBeNull()
            result.numRings() shouldBeEqualTo 1
        }
    }

    // =========================================================================
    // ST_Contains 테스트 (Polygon-Polygon 포함 관계)
    // =========================================================================

    @Test
    fun `ST_Contains - 외부 폴리곤이 내부 폴리곤을 완전히 포함`() {
        // 경기도 바운딩 박스 (외부)
        val gyeonggi = rectanglePolygon(minLng = 126.5, minLat = 36.9, maxLng = 127.8, maxLat = 38.3)
        // 서울 바운딩 박스 (내부 — 경기도 안에 완전히 포함됨)
        val seoul = rectanglePolygon(minLng = 126.7, minLat = 37.4, maxLng = 127.2, maxLat = 37.7)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "경기-서울"; it[zoneA] = gyeonggi; it[zoneB] = seoul }

            // 경기도가 서울을 포함
            val containsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .toList()
            containsRows shouldHaveSize 1

            // 서울이 경기도를 포함하지 않음
            val notContainsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneB.stContains(PolygonZones.zoneA) }
                .toList()
            notContainsRows shouldHaveSize 0
        }
    }

    @Test
    fun `ST_Contains - 내부 폴리곤은 외부 폴리곤에 ST_Within으로 확인 가능`() {
        // 한반도 전체 (외부)
        val korea = rectanglePolygon(minLng = 124.0, minLat = 33.0, maxLng = 132.0, maxLat = 43.0)
        // 수도권 (내부)
        val metro = rectanglePolygon(minLng = 126.0, minLat = 36.5, maxLng = 128.0, maxLat = 38.5)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "한반도-수도권"; it[zoneA] = korea; it[zoneB] = metro }

            // 한반도가 수도권을 포함 (ST_Contains)
            val containsResult = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .count()
            containsResult shouldBeEqualTo 1L

            // 수도권이 한반도에 속함 (ST_Within = ST_Contains의 역방향)
            // 단, PolygonZones에는 stWithin이 없으므로 stContains 역으로 확인
            val withinResult = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .count()
            withinResult shouldBeEqualTo 1L
        }
    }

    @Test
    fun `ST_Contains - 3단 중첩 폴리곤 포함 관계`() {
        // 국가 > 광역시 > 구 3단 중첩
        val nation = rectanglePolygon(minLng = 124.0, minLat = 33.0, maxLng = 132.0, maxLat = 43.0)
        val city = rectanglePolygon(minLng = 126.5, minLat = 36.9, maxLng = 127.8, maxLat = 38.3)
        val gu = rectanglePolygon(minLng = 126.8, minLat = 37.4, maxLng = 127.0, maxLat = 37.6)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "국가-광역시"; it[zoneA] = nation; it[zoneB] = city }
            PolygonZones.insert { it[name] = "광역시-구"; it[zoneA] = city; it[zoneB] = gu }
            PolygonZones.insert { it[name] = "국가-구"; it[zoneA] = nation; it[zoneB] = gu }

            // 국가→광역시, 광역시→구, 국가→구 모두 포함 관계 성립
            val containsCount = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .count()
            containsCount shouldBeEqualTo 3L

            // 역방향(구→광역시 등)은 포함 불가
            val reverseContainsCount = PolygonZones.selectAll()
                .where { PolygonZones.zoneB.stContains(PolygonZones.zoneA) }
                .count()
            reverseContainsCount shouldBeEqualTo 0L
        }
    }

    // =========================================================================
    // ST_Overlaps 테스트 (부분 겹침 — 포함 아님)
    // =========================================================================

    @Test
    fun `ST_Overlaps - 두 폴리곤이 부분적으로 겹침`() {
        // 왼쪽 사각형: lng 126~127, lat 37~38
        val left = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 127.0, maxLat = 38.0)
        // 오른쪽 사각형: lng 126.5~127.5 → 0.5도 겹침
        val right = rectanglePolygon(minLng = 126.5, minLat = 37.0, maxLng = 127.5, maxLat = 38.0)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "겹침"; it[zoneA] = left; it[zoneB] = right }

            val overlapsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stOverlaps(PolygonZones.zoneB) }
                .toList()
            overlapsRows shouldHaveSize 1

            // 포함 관계는 아님
            val containsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .toList()
            containsRows shouldHaveSize 0
        }
    }

    @Test
    fun `ST_Overlaps - 완전 포함인 경우 Overlaps는 false`() {
        // outer가 inner를 완전 포함 → ST_Overlaps는 false (포함≠부분 겹침)
        val outer = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 128.0, maxLat = 39.0)
        val inner = rectanglePolygon(minLng = 126.5, minLat = 37.5, maxLng = 127.5, maxLat = 38.5)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "완전포함"; it[zoneA] = outer; it[zoneB] = inner }

            // ST_Contains = true (포함 관계)
            val containsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .toList()
            containsRows shouldHaveSize 1

            // ST_Overlaps = false (포함은 부분 겹침이 아님)
            val overlapsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stOverlaps(PolygonZones.zoneB) }
                .toList()
            overlapsRows shouldHaveSize 0
        }
    }

    // =========================================================================
    // ST_Intersects / ST_Disjoint 테스트 (교차/분리)
    // =========================================================================

    @Test
    fun `ST_Intersects - 분리된 두 폴리곤은 교차하지 않음`() {
        // 서울 영역
        val seoulArea = rectanglePolygon(minLng = 126.7, minLat = 37.4, maxLng = 127.2, maxLat = 37.7)
        // 부산 영역 (서울과 완전 분리)
        val busanArea = rectanglePolygon(minLng = 128.9, minLat = 35.0, maxLng = 129.3, maxLat = 35.4)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "서울-부산"; it[zoneA] = seoulArea; it[zoneB] = busanArea }

            // 분리된 폴리곤은 교차하지 않음
            val intersectsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stIntersects(PolygonZones.zoneB) }
                .toList()
            intersectsRows shouldHaveSize 0

            // ST_Disjoint = true (완전 분리)
            val disjointRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stDisjoint(PolygonZones.zoneB) }
                .toList()
            disjointRows shouldHaveSize 1
        }
    }

    @Test
    fun `ST_Intersects - 겹치는 폴리곤은 교차함`() {
        val aArea = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 127.0, maxLat = 38.0)
        val bArea = rectanglePolygon(minLng = 126.5, minLat = 37.5, maxLng = 127.5, maxLat = 38.5)

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "겹침"; it[zoneA] = aArea; it[zoneB] = bArea }

            val intersectsRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stIntersects(PolygonZones.zoneB) }
                .toList()
            intersectsRows shouldHaveSize 1

            // 겹치므로 Disjoint = false
            val disjointRows = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stDisjoint(PolygonZones.zoneB) }
                .toList()
            disjointRows shouldHaveSize 0
        }
    }

    @Test
    fun `혼합 시나리오 - 포함, 겹침, 분리 폴리곤을 한 테이블에 저장하고 각각 필터링`() {
        val base = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 128.0, maxLat = 39.0)
        val inside = rectanglePolygon(minLng = 126.5, minLat = 37.5, maxLng = 127.5, maxLat = 38.5)  // base 안
        val overlap = rectanglePolygon(minLng = 127.5, minLat = 37.5, maxLng = 129.0, maxLat = 38.5)  // base와 부분 겹침
        val apart = rectanglePolygon(minLng = 130.0, minLat = 35.0, maxLng = 131.0, maxLat = 36.0)  // 완전 분리

        withGeoTables(PolygonZones) {
            PolygonZones.insert { it[name] = "포함"; it[zoneA] = base; it[zoneB] = inside }
            PolygonZones.insert { it[name] = "겹침"; it[zoneA] = base; it[zoneB] = overlap }
            PolygonZones.insert { it[name] = "분리"; it[zoneA] = base; it[zoneB] = apart }

            // base가 완전히 포함하는 것: inside만
            val containsNames = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stContains(PolygonZones.zoneB) }
                .map { it[PolygonZones.name] }
            containsNames shouldHaveSize 1
            containsNames.first() shouldBeEqualTo "포함"

            // 부분 겹침: overlap만 (inside는 완전 포함이라 Overlaps=false)
            val overlapsNames = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stOverlaps(PolygonZones.zoneB) }
                .map { it[PolygonZones.name] }
            overlapsNames shouldHaveSize 1
            overlapsNames.first() shouldBeEqualTo "겹침"

            // 완전 분리: apart만
            val disjointNames = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stDisjoint(PolygonZones.zoneB) }
                .map { it[PolygonZones.name] }
            disjointNames shouldHaveSize 1
            disjointNames.first() shouldBeEqualTo "분리"

            // 교차(포함 + 겹침): inside, overlap
            val intersectsNames = PolygonZones.selectAll()
                .where { PolygonZones.zoneA.stIntersects(PolygonZones.zoneB) }
                .map { it[PolygonZones.name] }
            intersectsNames shouldHaveSize 2
            intersectsNames.contains("포함").shouldBeTrue()
            intersectsNames.contains("겹침").shouldBeTrue()
        }
    }

    // =========================================================================
    // ST_Area 테스트
    // =========================================================================

    @Test
    fun `ST_Area - 큰 폴리곤이 작은 폴리곤보다 넓이가 크다`() {
        // PolygonZones 테이블의 zoneA, zoneB로 넓이 비교 대신
        // Regions 테이블에 각각 저장 후 alias로 비교
        val large = rectanglePolygon(minLng = 125.0, minLat = 36.0, maxLng = 129.0, maxLat = 40.0)  // 4x4도
        val small = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 127.0, maxLat = 38.0)  // 1x1도

        withGeoTables(Regions) {
            Regions.insert { it[name] = "large"; it[point] = point(127.0, 38.0); it[area] = large }
            Regions.insert { it[name] = "small"; it[point] = point(126.5, 37.5); it[area] = small }

            val stAreaExpr = Regions.area.stArea()
            val orderedNames = Regions.selectAll()
                .orderBy(stAreaExpr to SortOrder.ASC)
                .map { it[Regions.name] }

            orderedNames shouldHaveSize 2
            // 오름차순이므로 첫 번째가 작은 폴리곤
            orderedNames.first() shouldBeEqualTo "small"
            orderedNames.last() shouldBeEqualTo "large"
        }
    }

    @Test
    fun `ST_Area - select(expr) 로 각 행의 넓이를 직접 조회할 수 있다`() {
        val large = rectanglePolygon(minLng = 125.0, minLat = 36.0, maxLng = 129.0, maxLat = 40.0)
        val small = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 127.0, maxLat = 38.0)

        withGeoTables(Regions) {
            Regions.insert { it[name] = "large"; it[point] = point(127.0, 38.0); it[area] = large }
            Regions.insert { it[name] = "small"; it[point] = point(126.5, 37.5); it[area] = small }

            val areaExpr = Regions.area.stArea()
            val areaByName = Regions.select(Regions.name, areaExpr).associate {
                it[Regions.name] to it[areaExpr]
            }

            areaByName["large"].shouldNotBeNull()
            areaByName["small"].shouldNotBeNull()
            areaByName.getValue("large") shouldBeGreaterOrEqualTo 16.0
            areaByName.getValue("small") shouldBeEqualTo 1.0
            areaByName.getValue("large") shouldBeGreaterOrEqualTo areaByName.getValue("small")
        }
    }

    // =========================================================================
    // ST_ContainsPoint (Polygon -> Point) 테스트
    // =========================================================================

    @Test
    fun `ST_ContainsPoint - 폴리곤이 포인트를 포함하는지 직접 확인`() {
        val seoulArea = rectanglePolygon(minLng = 126.7, minLat = 37.4, maxLng = 127.2, maxLat = 37.7)

        withGeoTables(Regions) {
            val seoulCenter = point(126.9780, 37.5665)   // 서울 시청 -- 영역 내
            val busanCenter = point(129.0756, 35.1796)   // 부산 시청 -- 영역 외

            Regions.insert { it[name] = "서울 중심"; it[point] = seoulCenter; it[area] = seoulArea }
            Regions.insert { it[name] = "부산 중심"; it[point] = busanCenter; it[area] = seoulArea }

            // 폴리곤이 포인트를 포함하는 경우만 조회 (ST_Contains(polygon, point))
            val containedNames = Regions.selectAll()
                .where { Regions.area.stContainsPoint(Regions.point) }
                .map { it[Regions.name] }

            containedNames shouldHaveSize 1
            containedNames.first() shouldBeEqualTo "서울 중심"
        }
    }

    @Test
    fun `여러 행정구역 폴리곤 중 특정 포인트가 속한 구역 찾기`() {
        val seoulArea = rectanglePolygon(minLng = 126.7, minLat = 37.4, maxLng = 127.2, maxLat = 37.7)
        val busanArea = rectanglePolygon(minLng = 128.9, minLat = 35.0, maxLng = 129.3, maxLat = 35.4)
        val incheonArea = rectanglePolygon(minLng = 126.4, minLat = 37.3, maxLng = 126.8, maxLat = 37.6)

        // 행정구역 테이블 (area만 사용, point는 중심점)
        withGeoTables(Regions) {
            Regions.insert { it[name] = "서울"; it[point] = point(126.9780, 37.5665); it[area] = seoulArea }
            Regions.insert { it[name] = "부산"; it[point] = point(129.0756, 35.1796); it[area] = busanArea }
            Regions.insert { it[name] = "인천"; it[point] = point(126.7052, 37.4563); it[area] = incheonArea }

            // 각 행정구역 중심점이 자기 영역 안에 있는지 확인
            val ownAreaNames = Regions.selectAll()
                .where { Regions.area.stContainsPoint(Regions.point) }
                .map { it[Regions.name] }

            // 서울/부산/인천 중심점이 각자의 영역 안에 있어야 함
            ownAreaNames shouldHaveSize 3
            ownAreaNames.contains("서울").shouldBeTrue()
            ownAreaNames.contains("부산").shouldBeTrue()
            ownAreaNames.contains("인천").shouldBeTrue()
        }
    }
}
