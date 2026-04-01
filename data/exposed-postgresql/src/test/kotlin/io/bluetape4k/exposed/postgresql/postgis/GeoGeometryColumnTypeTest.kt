package io.bluetape4k.exposed.postgresql.postgis

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PostgisServer
import net.postgis.jdbc.geometry.LineString
import net.postgis.jdbc.geometry.LinearRing
import net.postgis.jdbc.geometry.Point
import net.postgis.jdbc.geometry.Polygon
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
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
 * Generic Geometry 컬럼 타입([GeoGeometryColumnType]) 및 관련 ST_* 함수 통합 테스트.
 *
 * PostGIS 전용 컨테이너(`postgis/postgis:16-3.4`)를 사용한다.
 * POINT, POLYGON, LINESTRING 등 다양한 Geometry 하위 타입을 하나의 컬럼에 저장/조회한다.
 */
class GeoGeometryColumnTypeTest: AbstractExposedTest() {

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
         * LineString 생성 헬퍼.
         */
        fun lineString(vararg points: Point): LineString =
            LineString(points).apply { srid = SRID }

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

    /**
     * generic geometry 컬럼을 가진 테이블.
     */
    object Geometries: LongIdTable("geo_geometries") {
        val name = varchar("name", 255)
        val geom = geoGeometry("geom")
    }

    /**
     * 두 generic geometry 간의 공간 관계를 테스트하기 위한 테이블.
     */
    object GeometryPairs: LongIdTable("geo_geometry_pairs") {
        val name = varchar("name", 255)
        val geomA = geoGeometry("geom_a")
        val geomB = geoGeometry("geom_b")
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

    // =========================================================================
    // DDL / 기본 CRUD 테스트
    // =========================================================================

    @Test
    fun `geoGeometry 컬럼의 SQL 타입이 GEOMETRY(GEOMETRY, 4326) 이다`() {
        transaction(db) {
            GeoGeometryColumnType().sqlType() shouldBeEqualTo "GEOMETRY(GEOMETRY, 4326)"
        }
    }

    @Test
    fun `POINT 타입 insert 및 select`() {
        val seoul = point(lng = 126.9780, lat = 37.5665)

        withGeoTables(Geometries) {
            Geometries.insert {
                it[name] = "서울 (Point)"
                it[geom] = seoul
            }

            val row = Geometries.selectAll().single()
            row[Geometries.name] shouldBeEqualTo "서울 (Point)"

            val result = row[Geometries.geom]
            result.shouldNotBeNull()
            result.typeString shouldBeEqualTo "POINT"
            (result as Point).x shouldBeEqualTo seoul.x
            (result as Point).y shouldBeEqualTo seoul.y
        }
    }

    @Test
    fun `POLYGON 타입 insert 및 select`() {
        val polygon = rectanglePolygon(
            minLng = 126.0, minLat = 37.0,
            maxLng = 127.0, maxLat = 38.0,
        )

        withGeoTables(Geometries) {
            Geometries.insert {
                it[name] = "사각 영역 (Polygon)"
                it[geom] = polygon
            }

            val row = Geometries.selectAll().single()
            val result = row[Geometries.geom]
            result.shouldNotBeNull()
            result.typeString shouldBeEqualTo "POLYGON"
            (result as Polygon).numRings() shouldBeEqualTo 1
        }
    }

    @Test
    fun `LINESTRING 타입 insert 및 select`() {
        val line = lineString(
            point(126.9780, 37.5665),
            point(127.0286, 37.2636),
            point(129.0756, 35.1796),
        )

        withGeoTables(Geometries) {
            Geometries.insert {
                it[name] = "서울-수원-부산 경로 (LineString)"
                it[geom] = line
            }

            val row = Geometries.selectAll().single()
            val result = row[Geometries.geom]
            result.shouldNotBeNull()
            result.typeString shouldBeEqualTo "LINESTRING"
            (result as LineString).numPoints() shouldBeEqualTo 3
        }
    }

    @Test
    fun `다양한 Geometry 하위 타입을 하나의 컬럼에 혼합 저장`() {
        val pointGeom = point(126.9780, 37.5665)
        val polygonGeom = rectanglePolygon(126.0, 37.0, 127.0, 38.0)
        val lineGeom = lineString(point(126.0, 37.0), point(127.0, 38.0))

        withGeoTables(Geometries) {
            Geometries.insert { it[name] = "포인트"; it[geom] = pointGeom }
            Geometries.insert { it[name] = "폴리곤"; it[geom] = polygonGeom }
            Geometries.insert { it[name] = "라인"; it[geom] = lineGeom }

            val results = Geometries.selectAll().toList()
            results shouldHaveSize 3

            val typeByName = results.associate { it[Geometries.name] to it[Geometries.geom].typeString }
            typeByName["포인트"] shouldBeEqualTo "POINT"
            typeByName["폴리곤"] shouldBeEqualTo "POLYGON"
            typeByName["라인"] shouldBeEqualTo "LINESTRING"
        }
    }

    // =========================================================================
    // ST_Intersects 테스트 (generic Geometry)
    // =========================================================================

    @Test
    fun `ST_Intersects - 겹치는 두 폴리곤 geometry가 교차함`() {
        val aArea = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 127.0, maxLat = 38.0)
        val bArea = rectanglePolygon(minLng = 126.5, minLat = 37.5, maxLng = 127.5, maxLat = 38.5)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "겹침"
                it[geomA] = aArea
                it[geomB] = bArea
            }

            val intersectsRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stIntersects(GeometryPairs.geomB) }
                .toList()
            intersectsRows shouldHaveSize 1
        }
    }

    @Test
    fun `ST_Intersects - 분리된 두 폴리곤 geometry는 교차하지 않음`() {
        val seoulArea = rectanglePolygon(minLng = 126.7, minLat = 37.4, maxLng = 127.2, maxLat = 37.7)
        val busanArea = rectanglePolygon(minLng = 128.9, minLat = 35.0, maxLng = 129.3, maxLat = 35.4)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "서울-부산"
                it[geomA] = seoulArea
                it[geomB] = busanArea
            }

            val intersectsRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stIntersects(GeometryPairs.geomB) }
                .toList()
            intersectsRows shouldHaveSize 0
        }
    }

    // =========================================================================
    // ST_Distance 테스트 (generic Geometry)
    // =========================================================================

    @Test
    fun `ST_Distance - 두 Point geometry 간 거리를 조회할 수 있다`() {
        val seoul = point(126.9780, 37.5665)
        val incheon = point(126.7052, 37.4563)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "서울-인천"
                it[geomA] = seoul
                it[geomB] = incheon
            }

            val distanceExpr = GeometryPairs.geomA.stDistance(GeometryPairs.geomB)
            val distance = GeometryPairs.select(distanceExpr).single()[distanceExpr]

            distance.shouldNotBeNull()
            distance shouldBeGreaterOrEqualTo 0.29
            distance shouldBeLessOrEqualTo 0.31
        }
    }

    // =========================================================================
    // ST_Contains 테스트 (generic Geometry)
    // =========================================================================

    @Test
    fun `ST_Contains - 큰 폴리곤이 작은 폴리곤을 포함`() {
        val outer = rectanglePolygon(minLng = 124.0, minLat = 33.0, maxLng = 132.0, maxLat = 43.0)
        val inner = rectanglePolygon(minLng = 126.5, minLat = 37.0, maxLng = 127.5, maxLat = 38.0)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "외부-내부"
                it[geomA] = outer
                it[geomB] = inner
            }

            val containsRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stContains(GeometryPairs.geomB) }
                .toList()
            containsRows shouldHaveSize 1

            // 역방향은 포함하지 않음
            val reverseRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomB.stContains(GeometryPairs.geomA) }
                .toList()
            reverseRows shouldHaveSize 0
        }
    }

    @Test
    fun `ST_Contains - 폴리곤이 내부 포인트를 포함`() {
        val area = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 128.0, maxLat = 39.0)
        val insidePoint = point(127.0, 38.0)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "영역-내부점"
                it[geomA] = area
                it[geomB] = insidePoint
            }

            val containsRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stContains(GeometryPairs.geomB) }
                .toList()
            containsRows shouldHaveSize 1
        }
    }

    // =========================================================================
    // ST_Within 테스트 (generic Geometry)
    // =========================================================================

    @Test
    fun `ST_Within - 내부 geometry가 외부 geometry 안에 있음`() {
        val outer = rectanglePolygon(minLng = 124.0, minLat = 33.0, maxLng = 132.0, maxLat = 43.0)
        val inner = point(126.9780, 37.5665)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "한반도-서울점"
                it[geomA] = inner
                it[geomB] = outer
            }

            // geomA(서울점)가 geomB(한반도) 안에 있는지
            val withinRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stWithin(GeometryPairs.geomB) }
                .toList()
            withinRows shouldHaveSize 1

            // 역방향은 성립하지 않음
            val reverseRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomB.stWithin(GeometryPairs.geomA) }
                .toList()
            reverseRows shouldHaveSize 0
        }
    }

    // =========================================================================
    // ST_DWithin 테스트 (generic Geometry)
    // =========================================================================

    @Test
    fun `ST_DWithin - 거리 임계값에 따라 포함 여부가 달라진다`() {
        val seoul = point(126.9780, 37.5665)
        val suwon = point(127.0286, 37.2636)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert {
                it[name] = "서울-수원"
                it[geomA] = seoul
                it[geomB] = suwon
            }

            val withinRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stDWithin(GeometryPairs.geomB, 0.31) }
                .toList()
            withinRows shouldHaveSize 1

            val outsideRows = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stDWithin(GeometryPairs.geomB, 0.29) }
                .toList()
            outsideRows shouldHaveSize 0
        }
    }

    // =========================================================================
    // 혼합 시나리오 테스트
    // =========================================================================

    @Test
    fun `혼합 시나리오 - 포함, 겹침, 분리 geometry를 한 테이블에 저장하고 ST_Contains로 필터링`() {
        val base = rectanglePolygon(minLng = 126.0, minLat = 37.0, maxLng = 128.0, maxLat = 39.0)
        val inside = rectanglePolygon(minLng = 126.5, minLat = 37.5, maxLng = 127.5, maxLat = 38.5)
        val apart = rectanglePolygon(minLng = 130.0, minLat = 35.0, maxLng = 131.0, maxLat = 36.0)

        withGeoTables(GeometryPairs) {
            GeometryPairs.insert { it[name] = "포함"; it[geomA] = base; it[geomB] = inside }
            GeometryPairs.insert { it[name] = "분리"; it[geomA] = base; it[geomB] = apart }

            // base가 완전히 포함하는 것: inside만
            val containsNames = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stContains(GeometryPairs.geomB) }
                .map { it[GeometryPairs.name] }
            containsNames shouldHaveSize 1
            containsNames.first() shouldBeEqualTo "포함"

            // 교차: inside만 (apart는 분리)
            val intersectsNames = GeometryPairs.selectAll()
                .where { GeometryPairs.geomA.stIntersects(GeometryPairs.geomB) }
                .map { it[GeometryPairs.name] }
            intersectsNames shouldHaveSize 1
            intersectsNames.first() shouldBeEqualTo "포함"
        }
    }
}
