package io.bluetape4k.science.geometry

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import kotlin.math.abs

class PolygonExtensionsTest {

    companion object: KLogging()

    private val geometryFactory = GeometryFactory()

    private fun makeSquare(x0: Double, y0: Double, x1: Double, y1: Double) =
        geometryFactory.createPolygon(
            arrayOf(
                Coordinate(x0, y0), Coordinate(x0, y1),
                Coordinate(x1, y1), Coordinate(x1, y0),
                Coordinate(x0, y0),
            )
        )

    @Test
    fun `areaInSquareMeters - 10x10 정사각형 면적은 100이다`() {
        val polygon = makeSquare(0.0, 0.0, 10.0, 10.0)
        polygon.areaInSquareMeters() shouldBeEqualTo 100.0
    }

    @Test
    fun `areaInSquareMeters - 5x3 직사각형 면적은 15이다`() {
        val polygon = makeSquare(0.0, 0.0, 5.0, 3.0)
        polygon.areaInSquareMeters() shouldBeEqualTo 15.0
    }

    @Test
    fun `areaInSquareMeters - 1x1 단위 정사각형 면적은 1이다`() {
        val polygon = makeSquare(0.0, 0.0, 1.0, 1.0)
        polygon.areaInSquareMeters() shouldBeEqualTo 1.0
    }

    @Test
    fun `centroid - 4x4 정사각형의 중심은 (2,2)이다`() {
        val polygon = makeSquare(0.0, 0.0, 4.0, 4.0)
        val c = polygon.centroid()
        c.shouldNotBeNull()
        (abs(c.x - 2.0) < 1e-9).let { assert(it) { "x 중심 오차: ${c.x}" } }
        (abs(c.y - 2.0) < 1e-9).let { assert(it) { "y 중심 오차: ${c.y}" } }
    }

    @Test
    fun `centroid - 이동한 사각형의 중심이 올바르다`() {
        val polygon = makeSquare(10.0, 20.0, 14.0, 24.0)
        val c = polygon.centroid()
        (abs(c.x - 12.0) < 1e-9).let { assert(it) { "x 중심 오차: ${c.x}" } }
        (abs(c.y - 22.0) < 1e-9).let { assert(it) { "y 중심 오차: ${c.y}" } }
    }

    @Test
    fun `toBoundingBox - 한국 경계 폴리곤에서 BoundingBox를 생성한다`() {
        val polygon = geometryFactory.createPolygon(
            arrayOf(
                Coordinate(124.0, 33.0), Coordinate(124.0, 38.9),
                Coordinate(131.0, 38.9), Coordinate(131.0, 33.0),
                Coordinate(124.0, 33.0),
            )
        )
        val bbox = polygon.toBoundingBox()
        bbox.minLon shouldBeEqualTo 124.0
        bbox.maxLon shouldBeEqualTo 131.0
        bbox.minLat shouldBeEqualTo 33.0
        bbox.maxLat shouldBeEqualTo 38.9
    }

    @Test
    fun `toBoundingBox - 원점 사각형에서 BoundingBox를 생성한다`() {
        val polygon = makeSquare(0.0, 0.0, 10.0, 10.0)
        val bbox = polygon.toBoundingBox()
        bbox.minLon shouldBeEqualTo 0.0
        bbox.maxLon shouldBeEqualTo 10.0
        bbox.minLat shouldBeEqualTo 0.0
        bbox.maxLat shouldBeEqualTo 10.0
    }

    @Test
    fun `toBoundingBox - width와 height가 올바르다`() {
        val polygon = makeSquare(124.0, 33.0, 131.0, 38.9)
        val bbox = polygon.toBoundingBox()
        (abs(bbox.width - 7.0) < 1e-9).let { assert(it) { "width 오차: ${bbox.width}" } }
        (abs(bbox.height - 5.9) < 1e-9).let { assert(it) { "height 오차: ${bbox.height}" } }
    }

    @Test
    fun `centroid가 JTS Point 타입을 반환한다`() {
        val polygon = makeSquare(0.0, 0.0, 4.0, 4.0)
        val c = polygon.centroid()
        c.shouldNotBeNull()
        // JTS Point 타입임을 확인
        (c is org.locationtech.jts.geom.Point).let { assert(it) { "centroid는 Point 타입이어야 합니다" } }
    }
}
