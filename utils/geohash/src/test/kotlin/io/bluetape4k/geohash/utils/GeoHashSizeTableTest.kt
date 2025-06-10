package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import kotlin.random.Random

class GeoHashSizeTableTest {

    companion object: KLogging() {
        private const val DELTA = 1e-10
    }

    @Test
    fun `verify DLat`() {
        GeoHashSizeTable.dLat.size shouldBeEqualTo 64

        GeoHashSizeTable.dLat[0] shouldBeEqualTo 180.0
        GeoHashSizeTable.dLat[1] shouldBeEqualTo 180.0
        GeoHashSizeTable.dLat[2] shouldBeEqualTo 90.0
        GeoHashSizeTable.dLat[18].shouldBeNear(0.3515625, DELTA)
        GeoHashSizeTable.dLat[19].shouldBeNear(0.3515625, DELTA)
    }

    @Test
    fun `verify DLon`() {
        GeoHashSizeTable.dLon.size shouldBeEqualTo 64

        GeoHashSizeTable.dLon[0] shouldBeEqualTo 360.0
        GeoHashSizeTable.dLon[1] shouldBeEqualTo 180.0

        GeoHashSizeTable.dLon[25].shouldBeNear(0.0439453125, DELTA)
        GeoHashSizeTable.dLon[26].shouldBeNear(0.0439453125, DELTA)
    }

    private interface BoundingBoxSizeTableVerifier {
        /**
         * generate a bounding box using a certain strategy for the given numer of bits
         */
        fun generate(bits: Int): BoundingBox

        /**
         * Get expected bits
         */
        fun getExpectedBits(bits: Int): Int
    }

    class ALittleSmallVerifier: BoundingBoxSizeTableVerifier {

        override fun generate(bits: Int): BoundingBox {
            // make the bounding box a little smaller than dLat/dLon
            val dLat = GeoHashSizeTable.dLat[bits] - DELTA
            val dLon = GeoHashSizeTable.dLon[bits] - DELTA

            return BoundingBox(45.0 - dLat, 45.0, 30.0 - dLon, 30.0)
        }

        override fun getExpectedBits(bits: Int): Int = bits
    }

    class BothALittleTooLargeVerifier: BoundingBoxSizeTableVerifier {
        override fun generate(bits: Int): BoundingBox {
            val dLat = GeoHashSizeTable.dLat[bits]
            val dLon = GeoHashSizeTable.dLon[bits]
            return BoundingBox(0.0, dLat + DELTA, 0.0, dLon + DELTA)
        }

        override fun getExpectedBits(bits: Int): Int = bits - 2
    }

    class OnlyOneALittleTooLargeVerifier: BoundingBoxSizeTableVerifier {

        private var latitudeAffected: Boolean = false

        override fun generate(bits: Int): BoundingBox {
            var dLat = GeoHashSizeTable.dLat[bits]
            var dLon = GeoHashSizeTable.dLon[bits]

            latitudeAffected = Random.nextBoolean()
            if (latitudeAffected) {
                dLat += DELTA
            } else {
                dLon += DELTA
            }
            return BoundingBox(0.0, dLat, 0.0, dLon)
        }

        override fun getExpectedBits(bits: Int): Int {
            return when {
                latitudeAffected -> if (bits % 2 != 0) bits - 2 else bits - 1
                else             -> if (bits % 2 != 0) bits - 1 else bits - 2
            }
        }
    }

    private fun checkWithGenerator(verifier: BoundingBoxSizeTableVerifier) {
        for (bits in 4 until 64) {
            val bbox = verifier.generate(bits)
            val expectedBits = verifier.getExpectedBits(bits)

            val actualBits = GeoHashSizeTable.numberOfBitsForOverlappingGeoHash(bbox)

            log.debug { "actual bits=$actualBits, expected bits=$expectedBits, bbox=$bbox" }
            actualBits shouldBeEqualTo expectedBits
        }
    }

    @Test
    fun `known smaller bouding box sizes`() {
        checkWithGenerator(ALittleSmallVerifier())
    }

    @Test
    fun `known larger bouding box sizes`() {
        checkWithGenerator(BothALittleTooLargeVerifier())
    }

    @Test
    fun `known one bit larger box sizes`() {
        checkWithGenerator(OnlyOneALittleTooLargeVerifier())
    }
}
