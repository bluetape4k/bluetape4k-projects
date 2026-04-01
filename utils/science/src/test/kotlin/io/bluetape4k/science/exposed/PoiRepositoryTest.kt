package io.bluetape4k.science.exposed

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import net.postgis.jdbc.geometry.Point
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * [PoiTable] 통합 테스트.
 *
 * PostGIS 컨테이너(`postgis/postgis:16-3.4`)를 Testcontainers로 구동하여
 * DDL 생성, POI insert/findById, geoPoint(POINT geometry) 저장·조회를 검증합니다.
 */
class PoiRepositoryTest: AbstractPostgisTest() {

    companion object: KLogging() {
        private const val SRID = 4326

        /**
         * PostGIS Point 생성 헬퍼. 좌표 순서: x=경도(lng), y=위도(lat)
         */
        fun point(lng: Double, lat: Double): Point =
            Point(lng, lat).apply { srid = SRID }
    }

    /**
     * [PoiTable] 행을 담는 간단한 DTO
     */
    data class PoiRecord(
        val id: Long,
        val name: String,
        val category: String?,
        val location: Point,
        val properties: Map<String, Any?>,
    )

    /** ResultRow를 [PoiRecord]로 변환하는 헬퍼 */
    private fun ResultRow.toPoi() = PoiRecord(
        id = this[PoiTable.id].value,
        name = this[PoiTable.name],
        category = this[PoiTable.category],
        location = this[PoiTable.location],
        properties = this[PoiTable.properties],
    )

    @Test
    fun `DDL - PoiTable 생성 확인`() {
        transaction(db) {
            val count = PoiTable.selectAll().count()
            log.debug { "PoiTable 레코드 수: $count" }
            count shouldBeEqualTo 0L
        }
    }

    @Test
    fun `POI insert 및 findById`() {
        transaction(db) {
            val seoul = point(lng = 126.9780, lat = 37.5665)

            val id = PoiTable.insertAndGetId {
                it[name] = "서울특별시청"
                it[category] = "행정기관"
                it[location] = seoul
                it[properties] = mapOf("address" to "서울시 중구 태평로1가 31", "phone" to "02-120")
            }

            id.value shouldBeGreaterThan 0L
            log.debug { "저장된 POI id: ${id.value}" }

            val found = PoiTable.selectAll()
                .where { PoiTable.id eq id }
                .single()
                .toPoi()

            found.name shouldBeEqualTo "서울특별시청"
            found.category shouldBeEqualTo "행정기관"
            found.location.shouldNotBeNull()
            found.location.x shouldBeEqualTo seoul.x
            found.location.y shouldBeEqualTo seoul.y
            found.properties["address"] shouldBeEqualTo "서울시 중구 태평로1가 31"

            log.debug { "조회된 POI: $found" }

            // cleanup
            PoiTable.deleteWhere { PoiTable.id eq id }
        }
    }

    @Test
    fun `여러 POI insert 및 전체 조회`() {
        transaction(db) {
            val pois = listOf(
                Triple("서울시청", point(126.9780, 37.5665), "행정기관"),
                Triple("부산시청", point(129.0756, 35.1796), "행정기관"),
                Triple("인천공항", point(126.4506, 37.4692), "교통"),
                Triple("김포공항", point(126.8010, 37.5584), "교통"),
            )

            val insertedIds = pois.map { (name, location, category) ->
                PoiTable.insertAndGetId {
                    it[PoiTable.name] = name
                    it[PoiTable.category] = category
                    it[PoiTable.location] = location
                    it[PoiTable.properties] = mapOf("type" to category)
                }.value
            }

            val all = PoiTable.selectAll().map { it.toPoi() }
            all.size shouldBeEqualTo pois.size

            val names = all.map { it.name }
            names.contains("서울시청") shouldBeEqualTo true
            names.contains("인천공항") shouldBeEqualTo true

            log.debug { "저장된 POI 목록: $names" }

            // cleanup
            insertedIds.forEach { id -> PoiTable.deleteWhere { PoiTable.id eq id } }
        }
    }

    @Test
    fun `geoPoint 컬럼에 POINT geometry 저장 및 좌표 정확도 검증`() {
        transaction(db) {
            // 독도 좌표 (동경 131.864, 북위 37.242)
            val dokdo = point(lng = 131.864, lat = 37.242)

            val id = PoiTable.insertAndGetId {
                it[name] = "독도"
                it[category] = "섬"
                it[location] = dokdo
                it[properties] = mapOf("country" to "대한민국", "region" to "경상북도 울릉군")
            }

            val row = PoiTable.selectAll()
                .where { PoiTable.id eq id }
                .single()

            val retrievedPoint = row[PoiTable.location]
            retrievedPoint.shouldNotBeNull()

            // PostGIS는 부동소수점 정밀도를 유지하므로 소수점 3자리까지 비교
            val lngDiff = kotlin.math.abs(retrievedPoint.x - dokdo.x)
            val latDiff = kotlin.math.abs(retrievedPoint.y - dokdo.y)

            (lngDiff < 0.001) shouldBeEqualTo true
            (latDiff < 0.001) shouldBeEqualTo true

            log.debug { "독도 좌표 검증 — 저장: (${dokdo.x}, ${dokdo.y}), 조회: (${retrievedPoint.x}, ${retrievedPoint.y})" }

            // cleanup
            PoiTable.deleteWhere { PoiTable.id eq id }
        }
    }

    @Test
    fun `카테고리 없는 POI 저장 (nullable category)`() {
        transaction(db) {
            val id = PoiTable.insertAndGetId {
                it[name] = "미분류 장소"
                it[category] = null
                it[location] = point(127.0, 37.5)
                it[properties] = emptyMap()
            }

            val found = PoiTable.selectAll()
                .where { PoiTable.id eq id }
                .single()
                .toPoi()

            found.name shouldBeEqualTo "미분류 장소"
            found.category shouldBeEqualTo null

            log.debug { "nullable category POI 저장 확인: $found" }

            // cleanup
            PoiTable.deleteWhere { PoiTable.id eq id }
        }
    }
}
