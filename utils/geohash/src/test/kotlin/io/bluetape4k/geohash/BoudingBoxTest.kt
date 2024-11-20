package io.bluetape4k.geohash

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.`should be near`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BoudingBoxTest {

    companion object: KLogging() {
        private const val DELTA = 1e-12
    }

    private lateinit var a: BoundingBox
    private lateinit var b: BoundingBox
    private lateinit var c: BoundingBox
    private lateinit var d: BoundingBox
    private lateinit var e: BoundingBox

    @BeforeEach
    fun beforeEach() {
        a = boundingBoxOf(WGS84Point(21.0, 20.0), WGS84Point(30.0, 31.0))
        b = a.copy()
        c = boundingBoxOf(WGS84Point(-45.0, -170.0), WGS84Point(45.0, 170.0))
        d = boundingBoxOf(WGS84Point(-45.0, 170.0), WGS84Point(-45.0, -170.0))
        e = d.copy()
    }

    @Test
    fun `equals bounding box`() {
        a shouldBeEqualTo b
        b shouldBeEqualTo a
        a shouldNotBeEqualTo c
        c shouldNotBeEqualTo a

        d shouldBeEqualTo e
        e shouldBeEqualTo d
        c shouldNotBeEqualTo d
        c shouldNotBeEqualTo e
    }

    @Test
    fun `test contains`() {
        val bbox = boundingBoxOf(45.0, 46.0, 120.0, 121.0)
        bbox.contains(wgs84PointOf(45.5, 120.5)).shouldBeTrue()
        bbox.contains(wgs84PointOf(90.0, 90.0)).shouldBeFalse()

        // Testing bounding box that crosses the 180th meridian
        val bbox2 = boundingBoxOf(45.0, 46.0, 170.0, -170.0)
        bbox2.contains(wgs84PointOf(45.5, 175.0)).shouldBeTrue()
        bbox2.contains(wgs84PointOf(45.5, -175.0)).shouldBeTrue()
        bbox2.contains(wgs84PointOf(45.5, 165.0)).shouldBeFalse()
        bbox2.contains(wgs84PointOf(45.5, -165.0)).shouldBeFalse()
    }

    @Test
    fun `get size`() {
        val bbox = boundingBoxOf(45.0, 90.0, 0.0, 30.0)
        bbox.getLatitudeSize().`should be near`(45.0, DELTA)
        bbox.getLongitudeSize().`should be near`(30.0, DELTA)

        val bbox2 = boundingBoxOf(-45.0, 45.0, -22.5, 30.0)
        bbox2.getLatitudeSize().`should be near`(90.0, DELTA)
        bbox2.getLongitudeSize().`should be near`(52.5, DELTA)

        val bbox3 = boundingBoxOf(-46.1, -44.0, -128.0, -127.2)
        bbox3.getLatitudeSize().`should be near`(2.1, DELTA)
        bbox3.getLongitudeSize().`should be near`(0.8, DELTA)

        // Testing bounding box that crosses the 180th meridian
        val bbox4 = boundingBoxOf(45.0, 90.0, 170.0, -170.0)
        bbox4.getLatitudeSize().`should be near`(45.0, DELTA)
        bbox4.getLongitudeSize().`should be near`(20.0, DELTA)
    }

    @Test
    fun `get intersects`() {
        val bbox = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val bbox1 = boundingBoxOf(-15.0, 5.0, 40.5, 43.0)
        val bbox2 = boundingBoxOf(-15.0, 5.0, 42.0, 43.0)
        bbox.intersects(bbox1).shouldBeTrue()
        bbox.intersects(bbox2).shouldBeFalse()
    }

    @Test
    fun `get intersects with 180 meridian`() {
        val bbox = boundingBoxOf(45.0, 90.0, 170.0, -170.0)
        val bbox1 = boundingBoxOf(50.0, 55.0, 175.0, 176.0)
        val bbox2 = bbox1.copy(westLongitude = 160.0, eastLongitude = -176.0)
        val bbox3 = bbox1.copy(westLongitude = -170.0, eastLongitude = -176.0)
        val bbox4 = bbox1.copy(westLongitude = -160.0, eastLongitude = -176.0)
        val bbox5 = bbox1.copy(westLongitude = 175.0, eastLongitude = -175.0)
        val bbox6 = bbox1.copy(westLongitude = -175.0, eastLongitude = 175.0)

        bbox.intersects(bbox1).shouldBeTrue()
        bbox.intersects(bbox2).shouldBeTrue()
        bbox.intersects(bbox3).shouldBeTrue()
        bbox.intersects(bbox4).shouldBeTrue()
        bbox.intersects(bbox5).shouldBeTrue()
        bbox.intersects(bbox6).shouldBeTrue()

        val bbox7 = boundingBoxOf(-15.0, 5.0, 42.0, 43.0)
        val bbox8 = bbox7.copy(westLongitude = 175.0, eastLongitude = 176.0)
        val bbox9 = bbox7.copy(westLongitude = 175.0, eastLongitude = -176.0)
        val bbox10 = boundingBoxOf(50.0, 55.0, 160.0, 169.0)
        val bbox11 = bbox10.copy(westLongitude = -169.0, eastLongitude = 160.0)

        bbox.intersects(bbox7).shouldBeFalse()
        bbox.intersects(bbox8).shouldBeFalse()
        bbox.intersects(bbox9).shouldBeFalse()
        bbox.intersects(bbox10).shouldBeFalse()
        bbox.intersects(bbox11).shouldBeFalse()
    }

    @Test
    fun `expand to include point - east`() {
        val bbox = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point = wgs84PointOf(0.0, 45.0)
        bbox.expandToInclude(point)
        bbox.contains(point).shouldBeTrue()

        val expandedBox = boundingBoxOf(-10.0, 10.0, 40.0, point.longitude)
        bbox shouldBeEqualTo expandedBox

        val bbox2 = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point2 = wgs84PointOf(0.0, -140.0)
        bbox2.expandToInclude(point2)
        bbox2.contains(point2).shouldBeTrue()

        val expandedBox2 = boundingBoxOf(-10.0, 10.0, 40.0, point2.longitude)
        bbox2 shouldBeEqualTo expandedBox2
    }

    @Test
    fun `expand to include point - west`() {
        val bbox = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point = wgs84PointOf(0.0, 35.0)
        bbox.expandToInclude(point)
        bbox.contains(point).shouldBeTrue()

        val expandedBox = boundingBoxOf(-10.0, 10.0, point.longitude, 41.0)
        bbox shouldBeEqualTo expandedBox

        val bbox2 = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point2 = wgs84PointOf(0.0, -139.0)
        bbox2.expandToInclude(point2)
        bbox2.contains(point2).shouldBeTrue()

        val expandedBox2 = boundingBoxOf(-10.0, 10.0, point2.longitude, 41.0)
        bbox2 shouldBeEqualTo expandedBox2
    }

    @Test
    fun `expand to include point - south`() {
        val bbox = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point = wgs84PointOf(-20.0, 40.0)
        bbox.expandToInclude(point)
        bbox.contains(point).shouldBeTrue()

        val expandedBox = boundingBoxOf(point.latitude, 10.0, 40.0, 41.0)
        bbox shouldBeEqualTo expandedBox
    }


    @Test
    fun `expand to include point - north`() {
        val bbox = boundingBoxOf(-10.0, 10.0, 40.0, 41.0)
        val point = wgs84PointOf(20.0, 40.0)
        bbox.expandToInclude(point)
        bbox.contains(point).shouldBeTrue()

        val expandedBox = boundingBoxOf(-10.0, point.latitude, 40.0, 41.0)
        bbox shouldBeEqualTo expandedBox
    }
}
