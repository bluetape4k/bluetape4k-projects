package io.bluetape4k.geohash.utils

import io.bluetape4k.collections.eclipse.fastListOf
import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class BoundingBoxSamplerTest {

    companion object: KLogging()

    @Test
    fun `verify Sampler`() {
        val bbox = BoundingBox(37.7, 37.84, -122.52, -122.35)
        val tbbox = twoGeoHashWithBits(bbox, 35)
        val sampler = BoundingBoxSampler(tbbox, 1179)

        val bboxSampler = sampler.boundingBox.boundingBox
        var gh = sampler.next()

        val hashes = fastListOf<String>()
        var sumOfComp = 0
        var crossingZero = 0

        var prev: GeoHash? = null
        while (gh != null) {
            bboxSampler.contains(gh.originatingPoint).shouldBeTrue()
            hashes.add(gh.toBase32()).shouldBeTrue()
            sumOfComp += prev?.compareTo(gh) ?: 0
            prev = gh
            if (sumOfComp == 0) {
                crossingZero++
            }
            gh = sampler.next()
        }

        hashes shouldHaveSize 12875

        // The expected value of the sum should be zero. This checks that it is
        // at least close. Worst case is 12875 or -12875 so -40 is sufficiently
        // close.
        sumOfComp shouldBeEqualTo -40

        // Check that the sum is zero a number of times, to make sure values are
        // increasing and decreasing.
        crossingZero shouldBeEqualTo 123
    }
}
