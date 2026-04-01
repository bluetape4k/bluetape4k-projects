package io.bluetape4k.science.geometry

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import kotlin.math.abs

class GeometryOperationsTest {

    companion object: KLogging()

    private fun point(x: Double, y: Double): Point =
        DEFAULT_GEOMETRY_FACTORY.createPoint(Coordinate(x, y))

    @Test
    fun `두 점 사이의 거리를 계산한다`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(3.0, 4.0)
        val dist = distanceBetween(p1, p2)
        dist shouldBeEqualTo 5.0
    }

    @Test
    fun `두 점의 각도를 계산한다 - 동쪽 방향`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(1.0, 0.0)
        val angle = angleBetween(p1, p2)
        angle shouldBeEqualTo 0.0
    }

    @Test
    fun `두 점의 각도를 계산한다 - 북쪽 방향`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(0.0, 1.0)
        val angle = angleBetween(p1, p2)
        angle shouldBeEqualTo 90.0
    }

    @Test
    fun `두 선분의 교차점을 반환한다`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(2.0, 2.0)
        val p3 = point(0.0, 2.0)
        val p4 = point(2.0, 0.0)
        val intersect = getIntersectPoint(p1, p2, p3, p4)
        intersect.shouldNotBeNull()
        (abs(intersect.x - 1.0) < 1e-9).let { assert(it) { "x 오차: ${intersect.x}" } }
        (abs(intersect.y - 1.0) < 1e-9).let { assert(it) { "y 오차: ${intersect.y}" } }
    }

    @Test
    fun `평행선은 교차점이 없다`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(1.0, 0.0)
        val p3 = point(0.0, 1.0)
        val p4 = point(1.0, 1.0)
        val intersect = getIntersectPoint(p1, p2, p3, p4)
        assert(intersect == null) { "평행선은 교차점이 없어야 합니다" }
    }

    @Test
    fun `isValidLatitude - 유효한 범위`() {
        assert(37.5665.isValidLatitude())
        assert((-90.0).isValidLatitude())
        assert(90.0.isValidLatitude())
    }

    @Test
    fun `isValidLatitude - 유효하지 않은 범위`() {
        assert(!91.0.isValidLatitude())
        assert(!(-91.0).isValidLatitude())
    }

    @Test
    fun `isValidLongitude - 유효한 범위`() {
        assert(126.9780.isValidLongitude())
        assert((-180.0).isValidLongitude())
        assert(180.0.isValidLongitude())
    }

    @Test
    fun `isValidLongitude - 유효하지 않은 범위`() {
        assert(!181.0.isValidLongitude())
        assert(!(-181.0).isValidLongitude())
    }
}
