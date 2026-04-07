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

        // 남반구 테스트 좌표
        val SYDNEY = GeoLocation(-33.8688, 151.2093)    // 시드니 (밴드 H, 남반구)
        val SAO_PAULO = GeoLocation(-23.5505, -46.6333) // 상파울루 (밴드 K, 남반구)
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
    fun `시드니 남반구 WGS84를 UTM으로 변환하고 왕복 정확도를 검증한다`() {
        // 시드니: Zone 56H (남반구) — +south 없으면 northing이 수백만 미터 오차 발생
        val zone = UtmZone(56, 'H')
        val (easting, northing) = wgs84ToUtm(SYDNEY)

        // 남반구 UTM northing은 10,000,000m 기준 음수 방향: 약 6,250,000m 근방
        assert(easting > 300_000.0 && easting < 400_000.0) { "easting 범위 오류: $easting" }
        assert(northing > 6_000_000.0 && northing < 6_500_000.0) { "northing 범위 오류 (남반구): $northing" }

        val restored = utmToWgs84(easting, northing, zone)
        assert(abs(restored.latitude - SYDNEY.latitude) < EPSILON) {
            "위도 오차: ${abs(restored.latitude - SYDNEY.latitude)}"
        }
        assert(abs(restored.longitude - SYDNEY.longitude) < EPSILON) {
            "경도 오차: ${abs(restored.longitude - SYDNEY.longitude)}"
        }
    }

    @Test
    fun `상파울루 남반구 WGS84를 UTM으로 변환하고 왕복 정확도를 검증한다`() {
        val zone = UtmZone(23, 'K')
        val (easting, northing) = wgs84ToUtm(SAO_PAULO)
        val restored = utmToWgs84(easting, northing, zone)

        assert(abs(restored.latitude - SAO_PAULO.latitude) < EPSILON) {
            "위도 오차: ${abs(restored.latitude - SAO_PAULO.latitude)}"
        }
        assert(abs(restored.longitude - SAO_PAULO.longitude) < EPSILON) {
            "경도 오차: ${abs(restored.longitude - SAO_PAULO.longitude)}"
        }
    }

    @Test
    fun `CrsRegistry 캐시가 동일 EPSG 코드에 대해 같은 객체를 반환한다`() {
        val crs1 = CrsRegistry.getCrs("EPSG:4326")
        val crs2 = CrsRegistry.getCrs("EPSG:4326")
        assert(crs1 === crs2) { "캐시에서 같은 인스턴스를 반환해야 합니다" }
    }

    @Test
    fun `CrsRegistry getCrsFromProj4 캐시가 동일 proj4 문자열에 대해 같은 객체를 반환한다`() {
        val proj4 = "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs"
        val crs1 = CrsRegistry.getCrsFromProj4(proj4)
        val crs2 = CrsRegistry.getCrsFromProj4(proj4)
        assert(crs1 === crs2) { "캐시에서 같은 인스턴스를 반환해야 합니다" }
    }

    @Test
    fun `CrsRegistry clearCache 후 새 인스턴스를 반환한다`() {
        val epsg = "EPSG:4326"
        val before = CrsRegistry.getCrs(epsg)
        CrsRegistry.clearCache()
        val after = CrsRegistry.getCrs(epsg)
        // clearCache 후에는 새 인스턴스가 생성되어야 함
        assert(before !== after) { "clearCache 후 새 인스턴스를 반환해야 합니다" }
    }
}
