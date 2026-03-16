package io.bluetape4k.geohash

import io.bluetape4k.geohash.utils.VincentyGeodesy
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.amshove.kluent.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class WGS84PointTest {

    companion object: KLogging() {
        const val DELTA = 1.0e-5
    }

    private lateinit var a: WGS84Point
    private lateinit var b: WGS84Point
    private lateinit var c: WGS84Point
    private lateinit var d: WGS84Point

    @BeforeEach
    fun setup() {
        a = wgs84PointOf(47.2342, 15.7465465)
        b = a.copy()
        c = b.copy(latitude = -47.234)
        d = wgs84PointOf(-32.9687253, 12.42334242)
    }

    @Test
    fun `test vincenty`() {
        val startPoint = wgs84PointOf(40.0, 40.0)

        val distanceInMeters = 10_000.0
        val result = startPoint.moveInDirection(120.0, distanceInMeters)
        log.debug { "result=$result" }

        result.longitude.shouldBeNear(40.10134882, DELTA)
        result.latitude.shouldBeNear(39.9549245, DELTA)

        startPoint.distanceInMeters(result).shouldBeNear(distanceInMeters, DELTA)
        result.distanceInMeters(startPoint).shouldBeNear(distanceInMeters, DELTA)

        val p1 = WGS84Point(1.0, 1.0)
        val manKilometers = 10_000_000.0
        val p2 = VincentyGeodesy.moveInDirection(p1, 270.0, manKilometers)
        log.debug { "p2=$p2, p1=$p1" }

        p1.distanceInMeters(p2).shouldBeNear(manKilometers, DELTA)
        p2.distanceInMeters(p1).shouldBeNear(manKilometers, DELTA)
    }

    @Test
    fun `test equals`() {
        a shouldBeEqualTo a
        a shouldBeEqualTo b
        b shouldBeEqualTo a
        a shouldNotBe b
    }

    @Test
    fun `range check`() {
        assertFailsWith<IllegalArgumentException> {
            wgs84PointOf(180.0, 0.0)
        }

        assertFailsWith<IllegalArgumentException> {
            wgs84PointOf(45.0, 240.0)
        }
    }
}
