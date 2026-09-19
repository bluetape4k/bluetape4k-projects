package io.bluetape4k.science.projection

import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBe
import io.bluetape4k.logging.KLogging
import io.bluetape4k.science.coords.GeoLocation
import io.bluetape4k.science.coords.UtmZone
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ProjectionsTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-4
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
        easting shouldBeInRange 300_000.0..400_000.0
        northing shouldBeInRange 4_000_000.0..4_300_000.0
    }

    @Test
    fun `서울 UTM을 WGS84로 역변환한다 - 왕복 정확도`() {
        val zone = UtmZone(52, 'S')
        val (easting, northing) = wgs84ToUtm(SEOUL)
        val restored = utmToWgs84(easting, northing, zone)

        abs(restored.latitude - SEOUL.latitude) shouldBeLessThan EPSILON
        abs(restored.longitude - SEOUL.longitude) shouldBeLessThan EPSILON
    }

    @Test
    fun `뉴욕 WGS84를 UTM으로 변환하고 역변환한다`() {
        val zone = UtmZone(18, 'T')
        val (easting, northing) = wgs84ToUtm(NEW_YORK)
        val restored = utmToWgs84(easting, northing, zone)

        abs(restored.latitude - NEW_YORK.latitude) shouldBeLessThan EPSILON
        abs(restored.longitude - NEW_YORK.longitude) shouldBeLessThan EPSILON
    }

    @Test
    fun `transform으로 WGS84에서 UTM Zone 52N으로 변환한다`() {
        // EPSG:32652 = UTM Zone 52N (WGS84)
        val (x, y) = transform("EPSG:4326", "EPSG:32652", SEOUL.longitude, SEOUL.latitude)

        x shouldBeInRange 300_000.0..400_000.0
        y shouldBeInRange 4_000_000.0..4_300_000.0
    }

    @Test
    fun `시드니 남반구 WGS84를 UTM으로 변환하고 왕복 정확도를 검증한다`() {
        // 시드니: Zone 56H (남반구) — +south 없으면 northing이 수백만 미터 오차 발생
        val zone = UtmZone(56, 'H')
        val (easting, northing) = wgs84ToUtm(SYDNEY)

        // 남반구 UTM northing은 10,000,000m 기준 음수 방향: 약 6,250,000m 근방
        easting shouldBeInRange 300_000.0..400_000.0
        northing shouldBeInRange 6_000_000.0..6_500_000.0

        val restored = utmToWgs84(easting, northing, zone)
        abs(restored.latitude - SYDNEY.latitude) shouldBeLessThan EPSILON
        abs(restored.longitude - SYDNEY.longitude) shouldBeLessThan EPSILON
    }

    @Test
    fun `상파울루 남반구 WGS84를 UTM으로 변환하고 왕복 정확도를 검증한다`() {
        val zone = UtmZone(23, 'K')
        val (easting, northing) = wgs84ToUtm(SAO_PAULO)
        val restored = utmToWgs84(easting, northing, zone)

        abs(restored.latitude - SAO_PAULO.latitude) shouldBeLessThan EPSILON
        abs(restored.longitude - SAO_PAULO.longitude) shouldBeLessThan EPSILON
    }

    @Test
    fun `CrsRegistry 캐시가 동일 EPSG 코드에 대해 같은 객체를 반환한다`() {
        val crs1 = CrsRegistry.getCrs("EPSG:4326")
        val crs2 = CrsRegistry.getCrs("EPSG:4326")
        crs1 shouldBe crs2
    }

    @Test
    fun `CrsRegistry getCrsFromProj4 캐시가 동일 proj4 문자열에 대해 같은 객체를 반환한다`() {
        val proj4 = "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs"
        val crs1 = CrsRegistry.getCrsFromProj4(proj4)
        val crs2 = CrsRegistry.getCrsFromProj4(proj4)
        crs1 shouldBe crs2
    }

    @Test
    fun `CrsRegistry clearCache 후 새 인스턴스를 반환한다`() {
        val epsg = "EPSG:4326"
        val before = CrsRegistry.getCrs(epsg)
        CrsRegistry.clearCache()

        val after = CrsRegistry.getCrs(epsg)
        // clearCache 후에는 새 인스턴스가 생성되어야 함
        before shouldNotBe after
    }
}
