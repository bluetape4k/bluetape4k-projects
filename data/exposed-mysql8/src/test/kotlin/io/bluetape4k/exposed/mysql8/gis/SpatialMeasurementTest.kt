package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class SpatialMeasurementTest: AbstractMySqlGisTest() {

    companion object: KLogging()

    // Point 테이블 (거리 측정용) — 두 포인트 컬럼을 한 행에 저장하여 stDistance 비교
    class PointMeasureTable: LongIdTable("measure_points") {
        val name = varchar("name", 255)
        val pointA = geoPoint("point_a")
        val pointB = geoPoint("point_b")
    }

    // Polygon 테이블 (넓이 측정용)
    class PolygonMeasureTable: LongIdTable("measure_polygons") {
        val name = varchar("name", 255)
        val area = geoPolygon("area")
    }

    // LineString 테이블 (길이 측정용)
    class LineMeasureTable: LongIdTable("measure_lines") {
        val name = varchar("name", 255)
        val path = geoLineString("path")
    }

    @Test
    fun `ST_Distance - 서울-수원 거리 약 30km`() {
        val table = transaction(db) { PointMeasureTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울-수원"
                it[pointA] = wgs84Point(126.9780, 37.5665)  // 서울
                it[pointB] = wgs84Point(127.0000, 37.2636)  // 수원
            }

            val distanceExpr = table.pointA.stDistance(table.pointB)
            val distance = table.select(distanceExpr).single()[distanceExpr]

            distance.shouldNotBeNull()
            distance shouldBeGreaterOrEqualTo 25_000.0
            distance shouldBeLessOrEqualTo 35_000.0
        }
    }

    @Test
    fun `ST_Distance_Sphere - 서울-부산 거리 약 325km`() {
        val table = transaction(db) { PointMeasureTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울-부산"
                it[pointA] = wgs84Point(126.9780, 37.5665)  // 서울
                it[pointB] = wgs84Point(129.0756, 35.1796)  // 부산
            }

            val distanceExpr = table.pointA.stDistanceSphere(table.pointB)
            val distance = table.select(distanceExpr).single()[distanceExpr]

            distance.shouldNotBeNull()
            distance shouldBeGreaterOrEqualTo 310_000.0
            distance shouldBeLessOrEqualTo 340_000.0
        }
    }

    @Test
    fun `ST_Area - 큰 폴리곤이 작은 폴리곤보다 넓이가 크다`() {
        val table = transaction(db) { PolygonMeasureTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "large"
                it[area] = wgs84Rectangle(125.0, 36.0, 129.0, 40.0)  // 4×4도
            }
            table.insert {
                it[name] = "small"
                it[area] = wgs84Rectangle(126.0, 37.0, 127.0, 38.0)  // 1×1도
            }

            val areaExpr = table.area.stArea()
            val rows = table.select(table.name, areaExpr)
                .orderBy(areaExpr, SortOrder.ASC)
                .toList()

            // 작은 것(small)이 먼저 정렬됨
            rows[0][table.name] shouldBeEqualTo "small"
            rows[1][table.name] shouldBeEqualTo "large"
        }
    }

    @Test
    fun `ST_Length - LineString 길이 측정`() {
        val table = transaction(db) { LineMeasureTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울-수원"
                it[path] = wgs84LineString(
                    126.978 to 37.566,
                    127.000 to 37.264,
                )
            }

            val lengthExpr = table.path.stLength()
            val length = table.select(lengthExpr).single()[lengthExpr]

            length.shouldNotBeNull()
            length shouldBeGreaterOrEqualTo 25_000.0
            length shouldBeLessOrEqualTo 35_000.0
        }
    }
}
