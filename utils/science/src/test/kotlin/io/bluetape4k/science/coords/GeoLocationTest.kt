package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInRange
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class GeoLocationTest {

    companion object: KLogging() {
        val SEOUL = GeoLocation(37.5665, 126.9780)
        val NEW_YORK = GeoLocation(40.7128, -74.0060)
    }

    @Test
    fun `서울 좌표가 정상적으로 생성된다`() {
        SEOUL.latitude shouldBeEqualTo 37.5665
        SEOUL.longitude shouldBeEqualTo 126.9780
    }

    @Test
    fun `서울-뉴욕 간 거리가 약 11000km이다`() {
        val distance = SEOUL.distanceTo(NEW_YORK)
        // 실제 거리: 약 11,038 km
        distance.shouldBeInRange(10_000_000.0..12_000_000.0)
    }

    @Test
    fun `같은 위치의 거리는 0이다`() {
        SEOUL.distanceTo(SEOUL) shouldBeEqualTo 0.0
    }

    @Test
    fun `GeoLocation 비교가 정상 동작한다`() {
        val loc1 = GeoLocation(10.0, 20.0)
        val loc2 = GeoLocation(10.0, 30.0)
        (loc1 < loc2).shouldBeTrue()
    }

    @Test
    fun `GeoLocation 동등성이 정상 동작한다`() {
        val loc1 = GeoLocation(37.5665, 126.9780)
        val loc2 = GeoLocation(37.5665, 126.9780)
        loc1 shouldBeEqualTo loc2
    }

    @Test
    fun `상수 ZERO의 위경도가 0이다`() {
        GeoLocation.ZERO.latitude shouldBeEqualTo 0.0
        GeoLocation.ZERO.longitude shouldBeEqualTo 0.0
    }

    @Test
    fun `위도 범위 초과 시 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> { GeoLocation(91.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(-91.0, 0.0) }
    }

    @Test
    fun `경도 범위 초과 시 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> { GeoLocation(0.0, 181.0) }
        assertFailsWith<IllegalArgumentException> { GeoLocation(0.0, -181.0) }
    }

    @Test
    fun `경계값 위경도가 허용된다`() {
        val northPole = GeoLocation(90.0, 180.0)
        northPole.latitude shouldBeEqualTo 90.0
        northPole.longitude shouldBeEqualTo 180.0

        val southPole = GeoLocation(-90.0, -180.0)
        southPole.latitude shouldBeEqualTo -90.0
        southPole.longitude shouldBeEqualTo -180.0
    }

    @Test
    fun `distanceTo가 대칭적이다`() {
        val distance1 = SEOUL.distanceTo(NEW_YORK)
        val distance2 = NEW_YORK.distanceTo(SEOUL)
        distance1 shouldBeEqualTo distance2
    }

    @Test
    fun `가까운 두 지점의 거리가 합리적이다`() {
        // 서울시청과 강남역: 약 9~11km
        val cityHall = GeoLocation(37.5665, 126.9780)
        val gangnam = GeoLocation(37.4979, 127.0276)
        val distance = cityHall.distanceTo(gangnam)
        distance.shouldBeInRange(7_000.0..12_000.0)
    }
}
