package io.bluetape4k.geohash.tests

import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.WGS84Point
import io.bluetape4k.geohash.geoHashWithBits
import io.bluetape4k.geohash.geoHashWithCharacters
import io.bluetape4k.geohash.wgs84PointOf
import io.bluetape4k.logging.KLogging
import kotlin.random.Random

object RandomGeoHashes: KLogging() {

    fun fullRange(): Sequence<GeoHash> = sequence {
        var lat = -90.0
        while (lat <= 90.0) {
            var lon = -180.0
            while (lon <= 180.0) {
                for (precisionChars in 6..12) {
                    yield(geoHashWithCharacters(lat, lon, precisionChars))
                }
                lon += Random.nextDouble() + 1.54
            }
            lat += Random.nextDouble() + 1.45
        }
    }

    /**
     * Create a completely random [GeoHash] with a random number of bits.
     *
     * Precision will be between [5,64] bits.
     */
    fun create(): GeoHash {
        return geoHashWithBits(randomLatitude(), randomLongitude(), randomPrecision())
    }

    /**
     * Create a completely random geohash with
     * a precision that is a multiple of 5 and in [5,60] bits.
     */
    fun createWith5BitsPrecision(): GeoHash {
        return geoHashWithCharacters(randomLatitude(), randomLongitude(), randomCharacterPrecision())
    }

    /**
     * a completely random geohash with the given number of bits precision.
     *
     * @param precision number of bits precision. require positive number. (0, 64)
     * @return
     */
    fun createWithPrecision(precision: Int): GeoHash {
        return geoHashWithBits(randomLatitude(), randomLongitude(), precision)
    }

    fun createPoint(): WGS84Point {
        return wgs84PointOf(randomLatitude(), randomLongitude())
    }

    private fun randomLatitude(): Double = (Random.nextDouble() - 0.5) * 180.0

    private fun randomLongitude(): Double = (Random.nextDouble() - 0.5) * 360.0

    private fun randomPrecision(): Int = Random.nextInt(60) + 5

    private fun randomCharacterPrecision(): Int = Random.nextInt(12) + 1
}
