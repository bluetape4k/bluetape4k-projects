package io.bluetape4k.science.coords

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UtmZoneTest {

    companion object: KLogging()

    @Test
    fun `UtmZone 데이터 클래스가 올바르게 생성된다`() {
        val zone = UtmZone(52, 'S')
        zone.longitudeZone shouldBeEqualTo 52
        zone.latitudeZone shouldBeEqualTo 'S'
    }

    @Test
    fun `UtmZone toString이 올바른 형식을 반환한다`() {
        val zone = UtmZone(52, 'S')
        zone.toString() shouldBeEqualTo "52S"
    }

    @Test
    fun `UtmZone 뉴욕 Zone 표현`() {
        val zone = UtmZone(18, 'T')
        zone.toString() shouldBeEqualTo "18T"
    }

    @Test
    fun `UtmZone equality가 올바르게 동작한다`() {
        val a = UtmZone(52, 'S')
        val b = UtmZone(52, 'S')
        (a == b).shouldBeTrue()
    }

    @Test
    fun `UtmZone 다른 값은 equal하지 않다`() {
        val a = UtmZone(52, 'S')
        val b = UtmZone(18, 'T')
        (a == b).shouldBeFalse()
    }

    @Test
    fun `UtmZone copy가 올바르게 동작한다`() {
        val original = UtmZone(52, 'S')
        val copy = original.copy(longitudeZone = 18, latitudeZone = 'T')
        copy.longitudeZone shouldBeEqualTo 18
        copy.latitudeZone shouldBeEqualTo 'T'
        (original == copy).shouldBeFalse()
    }

    @Test
    fun `UtmZone compareTo - 위도 구역이 다르면 위도로 비교한다`() {
        val seoulZone = UtmZone(52, 'S')
        val newYorkZone = UtmZone(18, 'T')
        // S < T 이므로 seoulZone < newYorkZone
        (seoulZone < newYorkZone).shouldBeTrue()
    }

    @Test
    fun `UtmZone compareTo - 위도 구역이 같으면 경도로 비교한다`() {
        val a = UtmZone(18, 'T')
        val b = UtmZone(52, 'T')
        (a < b).shouldBeTrue()
    }

    @Test
    fun `UtmZone compareTo - 같은 값은 0을 반환한다`() {
        val a = UtmZone(52, 'S')
        val b = UtmZone(52, 'S')
        a.compareTo(b) shouldBeEqualTo 0
    }

    @Test
    fun `UtmZone 경도 구역이 1~60 범위를 벗어나면 예외를 발생시킨다`() {
        assertThrows<IllegalArgumentException> { UtmZone(0, 'S') }
        assertThrows<IllegalArgumentException> { UtmZone(61, 'S') }
        assertThrows<IllegalArgumentException> { UtmZone(-1, 'S') }
    }

    @Test
    fun `UtmZone 위도 구역이 잘못된 문자이면 예외를 발생시킨다`() {
        // I, O 제외 검증
        assertThrows<IllegalArgumentException> { UtmZone(52, 'I') }
        assertThrows<IllegalArgumentException> { UtmZone(52, 'O') }
        assertThrows<IllegalArgumentException> { UtmZone(52, 'Z') }
        assertThrows<IllegalArgumentException> { UtmZone(52, 'A') }
    }

    @Test
    fun `UtmZone 경도 구역 최솟값 1이 유효하다`() {
        val zone = UtmZone(1, 'C')
        zone.longitudeZone shouldBeEqualTo 1
    }

    @Test
    fun `UtmZone 경도 구역 최댓값 60이 유효하다`() {
        val zone = UtmZone(60, 'X')
        zone.longitudeZone shouldBeEqualTo 60
    }

    @Test
    fun `UtmZone Serializable - 예외 없이 직렬화된다`() {
        val zone = UtmZone(52, 'S')
        java.io.ObjectOutputStream(java.io.ByteArrayOutputStream()).use { out ->
            out.writeObject(zone)
        }
    }

    @Test
    fun `isUtmLatitudeBand - 유효한 문자 S는 true이다`() {
        'S'.isUtmLatitudeBand.shouldBeTrue()
        'N'.isUtmLatitudeBand.shouldBeTrue()
        'C'.isUtmLatitudeBand.shouldBeTrue()
        'X'.isUtmLatitudeBand.shouldBeTrue()
    }

    @Test
    fun `isUtmLatitudeBand - 제외된 문자 I와 O는 false이다`() {
        'I'.isUtmLatitudeBand.shouldBeFalse()
        'O'.isUtmLatitudeBand.shouldBeFalse()
    }

    @Test
    fun `isUtmLatitudeBand - 유효 범위 외 문자는 false이다`() {
        'Z'.isUtmLatitudeBand.shouldBeFalse()
        'A'.isUtmLatitudeBand.shouldBeFalse()
        'B'.isUtmLatitudeBand.shouldBeFalse()
    }

    @Test
    fun `toUtmLongitude - Zone 31은 경도 0도이다`() {
        31.toUtmLongitude() shouldBeEqualTo 0.0
    }

    @Test
    fun `toUtmLongitude - Zone 32는 경도 6도이다`() {
        32.toUtmLongitude() shouldBeEqualTo 6.0
    }

    @Test
    fun `toUtmLongitude - Zone 52는 경도 126도이다`() {
        52.toUtmLongitude() shouldBeEqualTo 126.0
    }
}
