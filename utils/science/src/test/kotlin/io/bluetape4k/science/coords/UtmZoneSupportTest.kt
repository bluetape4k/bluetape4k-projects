package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UtmZoneSupportTest {

    companion object: KLogging()

    @Test
    fun `서울 좌표의 UTM Zone은 52S이다`() {
        val zone = utmZoneOf(latitude = 37.5665, longitude = 126.9780)
        zone.longitudeZone shouldBeEqualTo 52
        zone.latitudeZone shouldBeEqualTo 'S'
    }

    @Test
    fun `뉴욕 좌표의 UTM Zone은 18T이다`() {
        val zone = utmZoneOf(latitude = 40.7128, longitude = -74.0060)
        zone.longitudeZone shouldBeEqualTo 18
        zone.latitudeZone shouldBeEqualTo 'T'
    }

    @Test
    fun `GeoLocation으로부터 UTM Zone을 구한다`() {
        val seoul = GeoLocation(37.5665, 126.9780)
        val zone = utmZoneOf(seoul)
        zone.longitudeZone shouldBeEqualTo 52
        zone.latitudeZone shouldBeEqualTo 'S'
    }

    @Test
    fun `UTM Zone의 BoundingBox가 해당 위경도를 포함한다`() {
        val zone = UtmZone(52, 'S')
        val bbox = zone.boundingBox()
        // 서울(37.5665, 126.978)이 포함되어야 함
        bbox.contains(GeoLocation(37.5665, 126.9780)).shouldBeTrue()
    }

    @Test
    fun `UTM_LATITUDE_BANDS에 I와 O가 없다`() {
        UTM_LATITUDE_BANDS.containsKey('I').let { assert(!it) { "I가 포함되어 있으면 안됩니다" } }
        UTM_LATITUDE_BANDS.containsKey('O').let { assert(!it) { "O가 포함되어 있으면 안됩니다" } }
    }

    @Test
    fun `utmLatitudeBand가 위도에 맞는 구역 문자를 반환한다`() {
        utmLatitudeBand(37.5) shouldBeEqualTo 'S'   // 32~40 → S
        utmLatitudeBand(0.0) shouldBeEqualTo 'N'    // 0~8   → N
        utmLatitudeBand(-10.0) shouldBeEqualTo 'L'  // -16~-8 → L (-10은 L 구역에 속함)
        utmLatitudeBand(-4.0) shouldBeEqualTo 'M'   // -8~0  → M
    }

    @ParameterizedTest(name = "경도 {0}의 UTM Zone 번호는 {1}이다")
    @CsvSource(
        "126.978, 52",
        "-74.006, 18",
        "0.0, 31",
        "180.0, 60",
    )
    fun `경도별 UTM 경도 구역 번호 검증`(longitude: Double, expectedZone: Int) {
        val zone = ((longitude + 180) / UTM_LONGITUDE_SIZE).toInt() + 1
        zone.coerceIn(UTM_LONGITUDE_MIN, UTM_LONGITUDE_MAX) shouldBeEqualTo expectedZone
    }

    @Test
    fun `시드니 좌표의 UTM Zone은 56H이다 (남반구)`() {
        val zone = utmZoneOf(latitude = -33.8688, longitude = 151.2093)
        zone.longitudeZone shouldBeEqualTo 56
        zone.latitudeZone shouldBeEqualTo 'H'
    }

    @Test
    fun `Band X의 BoundingBox 높이는 12도이다`() {
        val zone = UtmZone(32, 'X')
        val bbox = zone.boundingBox()
        // Band X: 72°N ~ 84°N → 높이 12도
        val height = bbox.maxLat - bbox.minLat
        assert(height == 12.0) { "Band X 높이는 12도여야 합니다. 실제: $height" }
    }

    @Test
    fun `Band X의 BoundingBox가 84도 북위를 포함한다`() {
        val zone = UtmZone(32, 'X')
        val bbox = zone.boundingBox()
        assert(bbox.maxLat == 84.0) { "Band X maxLat은 84°N이어야 합니다. 실제: ${bbox.maxLat}" }
    }

    @Test
    fun `일반 Band의 BoundingBox 높이는 8도이다`() {
        val zone = UtmZone(52, 'S')
        val bbox = zone.boundingBox()
        val height = bbox.maxLat - bbox.minLat
        assert(height == 8.0) { "일반 Band 높이는 8도여야 합니다. 실제: $height" }
    }

    @Test
    fun `utmLatitudeBand는 84도 초과 위도에 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> { utmLatitudeBand(85.0) }
        assertThrows<IllegalArgumentException> { utmLatitudeBand(90.0) }
    }

    @Test
    fun `utmLatitudeBand는 -80도 미만 위도에 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> { utmLatitudeBand(-81.0) }
        assertThrows<IllegalArgumentException> { utmLatitudeBand(-90.0) }
    }

    @Test
    fun `utmLatitudeBand가 경계값 84도에서 X를 반환한다`() {
        utmLatitudeBand(84.0) shouldBeEqualTo 'X'
        utmLatitudeBand(72.0) shouldBeEqualTo 'X'
        utmLatitudeBand(71.9) shouldBeEqualTo 'W'
    }

    @Test
    fun `cellBoundingBox가 UTM Zone 내의 유효한 BoundingBox를 반환한다`() {
        val zone = UtmZone(52, 'S')
        val cellBbox = zone.cellBoundingBox(size = 1.0, row = 0, col = 0)
        val utmBbox = zone.boundingBox()
        // 첫 번째 셀은 UTM Zone의 북서쪽 모서리에 위치해야 함
        (cellBbox.maxLat <= utmBbox.maxLat).let { assert(it) }
        (cellBbox.minLon >= utmBbox.minLon).let { assert(it) }
    }
}
