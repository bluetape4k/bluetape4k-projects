package io.bluetape4k.science.geometry

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import kotlin.math.abs

class GeometryOperationsTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-9
    }

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
        abs(intersect.x - 1.0) shouldBeLessThan EPSILON
        abs(intersect.y - 1.0) shouldBeLessThan EPSILON
    }

    @Test
    fun `평행선은 교차점이 없다`() {
        val p1 = point(0.0, 0.0)
        val p2 = point(1.0, 0.0)
        val p3 = point(0.0, 1.0)
        val p4 = point(1.0, 1.0)

        val intersect = getIntersectPoint(p1, p2, p3, p4)
        intersect.shouldBeNull()
    }

    @Test
    fun `isValidLatitude - 유효한 범위`() {
        37.5665.isValidLatitude().shouldBeTrue()
        (-90.0).isValidLatitude().shouldBeTrue()
        90.0.isValidLatitude().shouldBeTrue()
    }

    @Test
    fun `isValidLatitude - 유효하지 않은 범위`() {
        91.0.isValidLatitude().shouldBeFalse()
        (-91.0).isValidLatitude().shouldBeFalse()
    }

    @Test
    fun `isValidLongitude - 유효한 범위`() {
        126.9780.isValidLongitude().shouldBeTrue()
        (-180.0).isValidLongitude().shouldBeTrue()
        180.0.isValidLongitude().shouldBeTrue()
    }

    @Test
    fun `isValidLongitude - 유효하지 않은 범위`() {
        181.0.isValidLongitude().shouldBeFalse()
        (-181.0).isValidLongitude().shouldBeFalse()
    }
}
