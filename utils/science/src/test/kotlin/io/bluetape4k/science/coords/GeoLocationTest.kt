package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

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
}
