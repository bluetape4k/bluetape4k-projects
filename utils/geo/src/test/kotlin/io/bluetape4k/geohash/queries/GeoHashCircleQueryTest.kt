package io.bluetape4k.geohash.queries

import io.bluetape4k.geohash.WGS84Point
import io.bluetape4k.geohash.wgs84PointOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class GeoHashCircleQueryTest {

    companion object: KLogging()

    @Test
    fun `circle query`() {
        val center = wgs84PointOf(39.86391280373075, 116.37356590048701)
        val query = geoHashCircleQueryOf(center, 589.0)

        // the distance between center and test1 is about 430 meters
        val test1 = WGS84Point(39.8648866576058, 116.378465869303)
        // the distance between center and test2 is about 510 meters
        val test2 = WGS84Point(39.8664787092599, 116.378552856158)
        // the distance between center and test2 is about 600 meters
        val test3 = WGS84Point(39.8786787092599, 116.378552856158)

        query.contains(test1).shouldBeTrue()
        query.contains(test2).shouldBeTrue()
        query.contains(test3).shouldBeFalse()
    }

    @Test
    fun `circle query with 180 meridian`() {
        // Test query over 180-Meridian
        val center = WGS84Point(39.86391280373075, 179.98356590048701)
        val query = geoHashCircleQueryOf(center, 3000.0)

        val test1 = WGS84Point(39.8648866576058, 180.0)
        val test2 = WGS84Point(39.8664787092599, -180.0)
        val test3 = WGS84Point(39.8686787092599, -179.9957861565146)
        val test4 = WGS84Point(39.8686787092599, 179.0057861565146)
        val test5 = WGS84Point(39.8686787092599, -179.0)

        query.contains(test1).shouldBeTrue()
        query.contains(test2).shouldBeTrue()
        query.contains(test3).shouldBeTrue()
        query.contains(test4).shouldBeFalse()
        query.contains(test5).shouldBeFalse()
    }
}
