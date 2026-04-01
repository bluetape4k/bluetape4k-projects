package io.bluetape4k.science.projection

import io.bluetape4k.logging.KLogging
import io.bluetape4k.science.coords.GeoLocation
import io.bluetape4k.science.coords.UtmZone
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ProjectionsTest {

    companion object: KLogging() {
        const val EPSILON = 1e-4
        val SEOUL = GeoLocation(37.5665, 126.9780)
        val NEW_YORK = GeoLocation(40.7128, -74.0060)
    }

    @Test
    fun `서울 WGS84를 UTM으로 변환한다`() {
        val (easting, northing) = wgs84ToUtm(SEOUL)
        // 서울 UTM Zone 52S 기준 easting은 약 313,000~320,000m, northing은 약 4,160,000m 근방
        assert(easting > 300_000.0 && easting < 400_000.0) { "easting 범위 오류: $easting" }
        assert(northing > 4_000_000.0 && northing < 4_300_000.0) { "northing 범위 오류: $northing" }
    }

    @Test
    fun `서울 UTM을 WGS84로 역변환한다 - 왕복 정확도`() {
        val zone = UtmZone(52, 'S')
        val (easting, northing) = wgs84ToUtm(SEOUL)
        val restored = utmToWgs84(easting, northing, zone)

        assert(abs(restored.latitude - SEOUL.latitude) < EPSILON) {
            "위도 오차: ${abs(restored.latitude - SEOUL.latitude)}"
        }
        assert(abs(restored.longitude - SEOUL.longitude) < EPSILON) {
            "경도 오차: ${abs(restored.longitude - SEOUL.longitude)}"
        }
    }

    @Test
    fun `뉴욕 WGS84를 UTM으로 변환하고 역변환한다`() {
        val zone = UtmZone(18, 'T')
        val (easting, northing) = wgs84ToUtm(NEW_YORK)
        val restored = utmToWgs84(easting, northing, zone)

        assert(abs(restored.latitude - NEW_YORK.latitude) < EPSILON) {
            "위도 오차: ${abs(restored.latitude - NEW_YORK.latitude)}"
        }
        assert(abs(restored.longitude - NEW_YORK.longitude) < EPSILON) {
            "경도 오차: ${abs(restored.longitude - NEW_YORK.longitude)}"
        }
    }

    @Test
    fun `transform으로 WGS84에서 UTM Zone 52N으로 변환한다`() {
        // EPSG:32652 = UTM Zone 52N (WGS84)
        val (x, y) = transform("EPSG:4326", "EPSG:32652", SEOUL.longitude, SEOUL.latitude)
        assert(x > 300_000.0 && x < 400_000.0) { "x(easting) 범위 오류: $x" }
        assert(y > 4_000_000.0 && y < 4_300_000.0) { "y(northing) 범위 오류: $y" }
    }

    @Test
    fun `CrsRegistry 캐시가 동일 EPSG 코드에 대해 같은 객체를 반환한다`() {
        val crs1 = CrsRegistry.getCrs("EPSG:4326")
        val crs2 = CrsRegistry.getCrs("EPSG:4326")
        assert(crs1 === crs2) { "캐시에서 같은 인스턴스를 반환해야 합니다" }
    }
}
