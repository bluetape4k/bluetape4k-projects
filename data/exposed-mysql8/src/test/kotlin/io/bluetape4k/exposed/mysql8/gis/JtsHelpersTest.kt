package io.bluetape4k.exposed.mysql8.gis

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

/**
 * [JtsHelpers] 팩토리 함수 단위 테스트.
 *
 * DB 없이 JTS 객체 생성 로직만 검증한다.
 */
class JtsHelpersTest {

    companion object : KLogging()

    // ─── wgs84Point ──────────────────────────────────────────────────────────

    @Test
    fun `wgs84Point - 경도 위도 좌표 순서 확인`() {
        val lng = 126.9779
        val lat = 37.5665
        val point = wgs84Point(lng, lat)

        point.shouldNotBeNull()
        point.x shouldBeEqualTo lng
        point.y shouldBeEqualTo lat
        point.srid shouldBeEqualTo SRID_WGS84
    }

    // ─── wgs84Polygon ─────────────────────────────────────────────────────────

    @Test
    fun `wgs84Polygon - 3개 이상 좌표로 생성`() {
        val polygon = wgs84Polygon(
            126.97 to 37.56,
            126.99 to 37.56,
            126.99 to 37.57,
            126.97 to 37.56,
        )

        polygon.shouldNotBeNull()
        polygon.isValid.shouldBeTrue()
        // 닫힌 링: 입력 4개 좌표 → 이미 닫혀 있으므로 추가 없음 → numPoints==4
        polygon.numPoints shouldBeEqualTo 4
    }

    @Test
    fun `wgs84Polygon - 자동 닫힘 처리`() {
        // 마지막 좌표가 첫 좌표와 다를 때 자동으로 닫아야 한다
        val polygon = wgs84Polygon(
            126.97 to 37.56,
            126.99 to 37.56,
            126.99 to 37.57,
        )

        polygon.shouldNotBeNull()
        // 3개 + auto-close = 4좌표 → numPoints == 4
        polygon.numPoints shouldBeEqualTo 4
    }

    @Test
    fun `wgs84Polygon - 좌표 2개 이하는 예외`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84Polygon(126.97 to 37.56, 126.99 to 37.56)
        }
    }

    @Test
    fun `wgs84Polygon - 빈 입력은 예외`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84Polygon()
        }
    }

    // ─── wgs84Rectangle ───────────────────────────────────────────────────────

    @Test
    fun `wgs84Rectangle - 5개 좌표 닫힌 링 생성`() {
        val rect = wgs84Rectangle(126.97, 37.56, 126.99, 37.58)

        rect.shouldNotBeNull()
        rect.isValid.shouldBeTrue()
        rect.numPoints shouldBeEqualTo 5
    }

    // ─── wgs84LineString ──────────────────────────────────────────────────────

    @Test
    fun `wgs84LineString - 정상 생성`() {
        val line = wgs84LineString(
            126.97 to 37.56,
            126.98 to 37.57,
            126.99 to 37.58,
        )

        line.shouldNotBeNull()
        line.numPoints shouldBeEqualTo 3
        line.srid shouldBeEqualTo SRID_WGS84
    }

    @Test
    fun `wgs84LineString - 좌표 1개 이하는 예외`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84LineString(126.97 to 37.56)
        }
    }

    @Test
    fun `wgs84LineString - 빈 입력은 예외`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84LineString()
        }
    }

    // ─── wgs84MultiPoint ──────────────────────────────────────────────────────

    @Test
    fun `wgs84MultiPoint - 여러 Point 포함`() {
        val mp = wgs84MultiPoint(
            wgs84Point(126.97, 37.56),
            wgs84Point(126.98, 37.57),
            wgs84Point(126.99, 37.58),
        )

        mp.shouldNotBeNull()
        mp.numGeometries shouldBeEqualTo 3
    }

    // ─── wgs84MultiPolygon ────────────────────────────────────────────────────

    @Test
    fun `wgs84MultiPolygon - 여러 Polygon 포함`() {
        val mp = wgs84MultiPolygon(
            wgs84Rectangle(126.97, 37.56, 126.98, 37.57),
            wgs84Rectangle(126.99, 37.58, 127.00, 37.59),
        )

        mp.shouldNotBeNull()
        mp.numGeometries shouldBeEqualTo 2
    }

    // ─── wgs84MultiLineString ─────────────────────────────────────────────────

    @Test
    fun `wgs84MultiLineString - 여러 LineString 포함`() {
        val mls = wgs84MultiLineString(
            wgs84LineString(126.97 to 37.56, 126.98 to 37.57),
            wgs84LineString(126.99 to 37.58, 127.00 to 37.59),
        )

        mls.shouldNotBeNull()
        mls.numGeometries shouldBeEqualTo 2
    }

    // ─── WGS84_FACTORY ────────────────────────────────────────────────────────

    @Test
    fun `WGS84_FACTORY SRID는 4326`() {
        WGS84_FACTORY.srid shouldBeEqualTo SRID_WGS84
    }
}
