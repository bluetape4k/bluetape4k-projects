package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.math.abs

class GeometryColumnTypeTest: AbstractMySqlGisTest() {

    companion object: KLogging()

    /**
     * 테이블 선언은 dialect 체크가 있으므로 트랜잭션 내에서만 초기화되도록
     * 클래스(인스턴스)로 선언한다. withGeoTables 내에서 인스턴스를 생성하면
     * 이미 트랜잭션이 활성화된 상태에서 컬럼이 등록된다.
     */
    class GeoPoints: LongIdTable("geo_points") {
        val name = varchar("name", 255)
        val location = geoPoint("location")
    }

    class GeoPolygons: LongIdTable("geo_polygons") {
        val name = varchar("name", 255)
        val area = geoPolygon("area")
    }

    class GeoLines: LongIdTable("geo_lines") {
        val name = varchar("name", 255)
        val path = geoLineString("path")
    }

    class GeoGeometries: LongIdTable("geo_geometries") {
        val name = varchar("name", 255)
        val geom = geoGeometry("geom")
    }

    private fun Double.shouldBeNear(expected: Double, delta: Double = 0.0001) {
        assert(abs(this - expected) <= delta) {
            "Expected $this to be near $expected (delta=$delta)"
        }
    }

    @Test
    fun `Point 저장 및 조회`() {
        val lng = 126.9780
        val lat = 37.5665
        val table = transaction(db) { GeoPoints() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울"
                it[location] = wgs84Point(lng, lat)
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.location]
            result.shouldNotBeNull()
            result.coordinate.x.shouldBeNear(lng)
            result.coordinate.y.shouldBeNear(lat)
        }
    }

    @Test
    fun `Polygon 저장 및 조회`() {
        val polygon = wgs84Rectangle(
            minLng = 126.0, minLat = 37.0,
            maxLng = 127.0, maxLat = 38.0,
        )
        val table = transaction(db) { GeoPolygons() }

        withGeoTables(table) {
            table.insert {
                it[name] = "테스트 영역"
                it[area] = polygon
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.area]
            result.shouldNotBeNull()
        }
    }

    @Test
    fun `LineString 저장 및 조회`() {
        val line = wgs84LineString(
            126.9780 to 37.5665,
            129.0756 to 35.1796,
        )
        val table = transaction(db) { GeoLines() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울-부산"
                it[path] = line
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.path]
            result.shouldNotBeNull()
            result.numPoints shouldBeEqualTo 2
        }
    }

    @Test
    fun `axis-order 검증 - lng lat 순서 보존`() {
        val lng = 126.9780
        val lat = 37.5665
        val table = transaction(db) { GeoPoints() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울"
                it[location] = wgs84Point(lng, lat)
            }

            val row = table.selectAll().single()
            val point = row[table.location]
            point.shouldNotBeNull()
            // coordinate.x = longitude, coordinate.y = latitude
            point.coordinate.x.shouldBeNear(lng)
            point.coordinate.y.shouldBeNear(lat)
        }
    }

    @Test
    fun `여러 Point 저장 후 전체 조회`() {
        val cities = listOf(
            "서울" to wgs84Point(126.9780, 37.5665),
            "부산" to wgs84Point(129.0756, 35.1796),
            "인천" to wgs84Point(126.7052, 37.4563),
            "대구" to wgs84Point(128.6014, 35.8714),
            "광주" to wgs84Point(126.8514, 35.1595),
        )
        val table = transaction(db) { GeoPoints() }

        withGeoTables(table) {
            cities.forEach { (cityName, point) ->
                table.insert {
                    it[name] = cityName
                    it[location] = point
                }
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 5
        }
    }

    @Test
    fun `Geometry 범용 컬럼에 Point 저장`() {
        val lng = 126.9780
        val lat = 37.5665
        val table = transaction(db) { GeoGeometries() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울 포인트"
                it[geom] = wgs84Point(lng, lat)
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.geom]
            result.shouldNotBeNull()
        }
    }

    @Test
    fun `GeometryColumnType sqlType 검증`() {
        transaction(db) {
            val sqlType = pointColumnType().sqlType()
            sqlType shouldBeEqualTo "POINT SRID 4326"
        }
    }
}
