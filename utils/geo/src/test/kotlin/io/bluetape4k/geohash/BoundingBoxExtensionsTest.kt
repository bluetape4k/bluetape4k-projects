package io.bluetape4k.geohash

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

@Suppress("NOTHING_TO_INLINE")

class BoundingBoxExtensionsTest {

    companion object: KLogging()

    private val DELTA = 1.0e-9

    @Test
    fun `boundingBoxOf from corners`() {
        val sw = WGS84Point(45.0, 120.0)
        val ne = WGS84Point(46.0, 121.0)
        val bbox = boundingBoxOf(sw, ne)
        bbox.southLatitude shouldBeEqualTo 45.0
        bbox.northLatitude shouldBeEqualTo 46.0
        bbox.westLongitude shouldBeEqualTo 120.0
        bbox.eastLongitude shouldBeEqualTo 121.0
    }

    @Test
    fun `boundingBoxOf from coordinates`() {
        val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        bbox.southLatitude shouldBeEqualTo 45.0
        bbox.northLatitude shouldBeEqualTo 46.0
        bbox.westLongitude shouldBeEqualTo 120.0
        bbox.eastLongitude shouldBeEqualTo 121.0
    }

    @Test
    fun `boundingBoxOf invalid south latitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            boundingBoxOf(-100.0, 46.0, 120.0, 121.0)
        }
    }

    @Test
    fun `boundingBoxOf invalid north latitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            boundingBoxOf(45.0, 100.0, 120.0, 121.0)
        }
    }

    @Test
    fun `boundingBoxOf south greater than north throws`() {
        assertFailsWith<IllegalArgumentException> {
            boundingBoxOf(46.0, 45.0, 120.0, 121.0)
        }
    }

    @Test
    fun `boundingBoxOf invalid west longitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            boundingBoxOf(45.0, 46.0, -200.0, 121.0)
        }
    }

    @Test
    fun `boundingBoxOf invalid east longitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            boundingBoxOf(45.0, 46.0, 120.0, 200.0)
        }
    }

    @Test
    fun `BoundingBox contains point`() {
        val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        val inside = WGS84Point(45.5, 120.5)
        bbox.contains(inside).shouldBeTrue()
    }

    @Test
    fun `BoundingBox does not contain point outside`() {
        val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        val outside = WGS84Point(50.0, 130.0)
        bbox.contains(outside).shouldBeFalse()
    }

    @Test
    fun `BoundingBox getCenter`() {
        val bbox = boundingBoxOf(44.0, 46.0, 119.0, 121.0)
        val center = bbox.getCenter()
        center.latitude.shouldBeNear(45.0, DELTA)
        center.longitude.shouldBeNear(120.0, DELTA)
    }

    @Test
    fun `BoundingBox intersects overlapping boxes`() {
        val bbox1 = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        val bbox2 = boundingBoxOf(45.5, 46.5, 120.5, 121.5)
        bbox1.intersects(bbox2).shouldBeTrue()
    }

    @Test
    fun `BoundingBox does not intersect non-overlapping`() {
        val bbox1 = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        val bbox2 = boundingBoxOf(50.0, 51.0, 130.0, 131.0)
        bbox1.intersects(bbox2).shouldBeFalse()
    }

    @Test
    fun `BoundingBox expandToInclude expands southLatitude`() {
        val bbox = BoundingBox(45.0, 46.0, 120.0, 121.0)
        val other = BoundingBox(44.0, 45.5, 120.0, 121.0)
        bbox.expandToInclude(other)
        bbox.southLatitude shouldBeEqualTo 44.0
    }

    @Test
    fun `BoundingBox expandToInclude expands northLatitude`() {
        val bbox = BoundingBox(45.0, 46.0, 120.0, 121.0)
        val other = BoundingBox(45.5, 47.0, 120.0, 121.0)
        bbox.expandToInclude(other)
        bbox.northLatitude shouldBeEqualTo 47.0
    }

    @Test
    fun `BoundingBox intersects180Meridian detection`() {
        // west=170, east=-170 means it crosses 180 meridian
        val bbox = BoundingBox(45.0, 46.0, 170.0, -170.0)
        bbox.isIntersection180Meridian.shouldBeTrue()
    }

    @Test
    fun `BoundingBox normal does not intersect 180 meridian`() {
        val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        bbox.isIntersection180Meridian.shouldBeFalse()
    }

    @Test
    fun `BoundingBox equals and hashCode`() {
        val bbox1 = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        val bbox2 = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        (bbox1 == bbox2).shouldBeTrue()
        (bbox1.hashCode() == bbox2.hashCode()).shouldBeTrue()
    }

    @Test
    fun `WGS84Point toPair`() {
        val point = WGS84Point(37.5665, 126.9780)
        val pair = point.toPair()
        pair.first shouldBeEqualTo 37.5665
        pair.second shouldBeEqualTo 126.9780
    }

    @Test
    fun `wgs84PointOf creates point`() {
        val point = wgs84PointOf(37.5665, 126.9780)
        point.latitude shouldBeEqualTo 37.5665
        point.longitude shouldBeEqualTo 126.9780
    }

    @Test
    fun `wgs84PointOf invalid latitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84PointOf(95.0, 0.0)
        }
    }

    @Test
    fun `wgs84PointOf invalid longitude throws`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84PointOf(0.0, 200.0)
        }
    }

    @Test
    fun `WGS84Point moveInDirection moves point`() {
        val start = wgs84PointOf(37.5665, 126.9780)
        val moved = start.moveInDirection(90.0, 1000.0)
        (moved.longitude > start.longitude).shouldBeTrue()
    }

    @Test
    fun `WGS84Point distanceInMeters between close points`() {
        val a = wgs84PointOf(37.5665, 126.9780)
        val b = wgs84PointOf(37.5665, 126.9880)
        val dist = a.distanceInMeters(b)
        (dist > 0.0).shouldBeTrue()
    }
}

