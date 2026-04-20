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

/**
 * MultiPoint / MultiPolygon / MultiLineString / GeometryCollection 컬럼 타입 저장/조회 테스트.
 *
 * GIS 다중 지오메트리 타입이 MySQL에 올바르게 직렬화/역직렬화되는지 검증한다.
 */
class GeoMultiTypeColumnTest : AbstractMySqlGisTest() {

    companion object : KLogging()

    class MultiPointTable : LongIdTable("geo_multi_points") {
        val name = varchar("name", 255)
        val points = geoMultiPoint("points")
    }

    class MultiPolygonTable : LongIdTable("geo_multi_polygons") {
        val name = varchar("name", 255)
        val zones = geoMultiPolygon("zones")
    }

    class MultiLineTable : LongIdTable("geo_multi_lines") {
        val name = varchar("name", 255)
        val paths = geoMultiLineString("paths")
    }

    @Test
    fun `MultiPoint 저장 및 조회`() {
        val table = transaction(db) { MultiPointTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "도시 클러스터"
                it[points] = wgs84MultiPoint(
                    wgs84Point(126.9780, 37.5665),  // 서울
                    wgs84Point(129.0756, 35.1796),  // 부산
                    wgs84Point(128.6014, 35.8714),  // 대구
                )
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.points]
            result.shouldNotBeNull()
            result.numGeometries shouldBeEqualTo 3
        }
    }

    @Test
    fun `MultiPolygon 저장 및 조회`() {
        val table = transaction(db) { MultiPolygonTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "서울 + 부산 구역"
                it[zones] = wgs84MultiPolygon(
                    wgs84Rectangle(126.8, 37.4, 127.1, 37.7),  // 서울
                    wgs84Rectangle(128.9, 35.0, 129.2, 35.3),  // 부산
                )
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.zones]
            result.shouldNotBeNull()
            result.numGeometries shouldBeEqualTo 2
        }
    }

    @Test
    fun `MultiLineString 저장 및 조회`() {
        val table = transaction(db) { MultiLineTable() }

        withGeoTables(table) {
            table.insert {
                it[name] = "고속도로 구간"
                it[paths] = wgs84MultiLineString(
                    wgs84LineString(126.978 to 37.566, 127.000 to 37.264),  // 서울-수원
                    wgs84LineString(127.000 to 37.264, 127.5 to 36.8),      // 수원-천안
                )
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 1

            val result = rows.first()[table.paths]
            result.shouldNotBeNull()
            result.numGeometries shouldBeEqualTo 2
        }
    }

    @Test
    fun `MultiPoint - 여러 행 저장 후 전체 조회`() {
        val table = transaction(db) { MultiPointTable() }

        withGeoTables(table) {
            val clusters = listOf(
                "수도권" to wgs84MultiPoint(
                    wgs84Point(126.9780, 37.5665),
                    wgs84Point(126.7052, 37.4563),
                ),
                "영남권" to wgs84MultiPoint(
                    wgs84Point(129.0756, 35.1796),
                    wgs84Point(128.6014, 35.8714),
                ),
            )
            clusters.forEach { (clusterName, mp) ->
                table.insert {
                    it[name] = clusterName
                    it[points] = mp
                }
            }

            val rows = table.selectAll().toList()
            rows shouldHaveSize 2
        }
    }
}
