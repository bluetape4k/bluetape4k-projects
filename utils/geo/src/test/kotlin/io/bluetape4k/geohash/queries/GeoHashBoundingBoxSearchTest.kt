package io.bluetape4k.geohash.queries

import io.bluetape4k.geohash.geoHashOfBinaryString
import io.bluetape4k.geohash.geoHashOfString
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInRange
import org.amshove.kluent.shouldBeTrue
import kotlin.test.Test

class GeoHashBoundingBoxSearchTest {

    companion object: KLogging()

    @Test
    fun testSeveralBoundingBoxes() {
        checkSearchYieldsCorrectNumberOfHashes(40.2090980098, 40.21982983232432, -22.523432424324, -22.494234232442)
        checkSearchYieldsCorrectNumberOfHashes(40.09872762, 41.23452234, 30.0113312322, 31.23432)

        checkSearchYieldsCorrectHashes(47.300200, 47.447907, 8.471276, 8.760941, "u0qj")
        checkSearchYieldsCorrectHashes(47.157502, 47.329727, 8.562244, 8.859215, "u0qj", "u0qm", "u0qh", "u0qk")

        // Testing bounding box over 180-Meridian
        checkSearchYieldsCorrectNumberOfHashes(40.2090980098, 40.21982983232432, 170.523432424324, -170.494234232442)
        checkSearchYieldsCorrectNumberOfHashes(40.2090980098, 40.21982983232432, 170.523432424324, 160.494234232442)

        checkSearchYieldsCorrectHashes(
            40.2090980098,
            40.21982983232432,
            170.523432424324,
            -170.494234232442,
            "xz", "8p"
        )
        checkSearchYieldsCorrectBinaryHashes(
            47.157502,
            47.329727,
            179.062244,
            -179.859215,
            "1111101010101111", "010100000000010100000", "010100000000010100010"
        )

        // Check duplicate handling
        checkSearchYieldsCorrectBinaryHashes(47.157502, 47.329727, 179.062244, 160.0, "")
        checkSearchYieldsCorrectBinaryHashes(47.157502, 47.329727, 179.062244, -1.0, "01", "1111101010101111")
    }

    private fun checkSearchYieldsCorrectNumberOfHashes(
        southLat: Double,
        northLat: Double,
        westLon: Double,
        eastLon: Double,
    ) {
        val search: GeoHashQuery = geoHashBoundingBoxQueryOf(southLat, northLat, westLon, eastLon)
        assertRightNumberOfSearchHashes(search)
    }

    private fun checkSearchYieldsCorrectHashes(
        southLat: Double,
        northLat: Double,
        westLon: Double,
        eastLon: Double,
        vararg hashes: String,
    ) {
        val search: GeoHashQuery = geoHashBoundingBoxQueryOf(southLat, northLat, westLon, eastLon)

        val searchHashes = search.getSearchHashes()
        searchHashes.forEach { log.debug { "search hash: $it" } }
        searchHashes.size shouldBeEqualTo hashes.size

        for (hash in hashes) {
            val expectedHash = geoHashOfString(hash)
            log.debug { "expected hash: $expectedHash" }
            searchHashes.contains(expectedHash).shouldBeTrue()
        }
    }

    private fun checkSearchYieldsCorrectBinaryHashes(
        southLat: Double,
        northLat: Double,
        westLon: Double,
        eastLon: Double,
        vararg hashes: String,
    ) {
        val search = geoHashBoundingBoxQueryOf(southLat, northLat, westLon, eastLon)

        val searchHashes = search.getSearchHashes()
        searchHashes.forEach { log.debug { "search hash: $it" } }
        searchHashes.size shouldBeEqualTo hashes.size

        for (hash in hashes) {
            val expectedHash = geoHashOfBinaryString(hash)
            log.debug { "expected hash: $expectedHash" }
            searchHashes.contains(expectedHash).shouldBeTrue()
        }
    }

    private fun assertRightNumberOfSearchHashes(search: GeoHashQuery) {
        val size: Int = search.getSearchHashes().size
        size shouldBeInRange 1..8
    }
}
