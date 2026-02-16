package io.bluetape4k.geohash.utils

import io.bluetape4k.collections.asIterable
import io.bluetape4k.collections.toList
import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArraySet

class BoundingBoxGeoHashIteratorTest {

    companion object: KLogging()

    @Test
    fun `test iterator with bit precision-10`() {
        val box = BoundingBox(37.7, 37.84, -122.52, -122.35)
        val iter = BoundingBoxGeoHashIterator(twoGeoHashWithBits(box, 10))
        verifyIterator(iter)
    }

    @Test
    fun `test iterator with bit precision-35`() {
        val box = BoundingBox(37.7, 37.84, -122.52, -122.35)
        val iter = BoundingBoxGeoHashIterator(twoGeoHashWithBits(box, 35))
        verifyIterator(iter)
    }

    @Test
    fun `test iterator with character precision-2`() {
        val box = BoundingBox(28.5, 67.15, -33.2, 44.5)
        val iter = BoundingBoxGeoHashIterator(twoGeoHashWithCharacters(box, 2))
        val hashes = verifyIterator(iter)
        hashes shouldHaveSize 49
    }

    @Test
    fun `large item iterator`() {
        val box = BoundingBox(72.28907, 88.62655, -50.976562, 170.50781)
        val twoGeoHashBoundingBox = twoGeoHashWithCharacters(box, 2)
        val iterator = BoundingBoxGeoHashIterator(twoGeoHashBoundingBox)

        val hashes = CopyOnWriteArraySet<GeoHash>()
        iterator.forEach { hash ->
            log.debug { "hash=$hash" }
            hashes.contains(hash).shouldBeFalse()
            hashes.add(hash)
        }
        log.debug { "hashes size=${hashes.size}" }
        hashes shouldHaveSize 84
    }

    @Test
    fun `all cells`() {
        val box = BoundingBox(-90.0, 90.0, -180.0, 180.0)
        val twoGeoHashBoundingBox = twoGeoHashWithCharacters(box, 2)
        val iterator = BoundingBoxGeoHashIterator(twoGeoHashBoundingBox)

        val hashes = iterator.asIterable().toSet()
        hashes shouldHaveSize 1024
    }

    @Test
    fun `top right corner`() {
        val box = BoundingBox(84.4, 84.9, 169.3, 179.6) // all in ZZ cell
        val twoGeoHashBoundingBox = twoGeoHashWithCharacters(box, 2)
        val iterator = BoundingBoxGeoHashIterator(twoGeoHashBoundingBox)

        val hashes = CopyOnWriteArraySet<GeoHash>()
        iterator.forEach { hash ->
            log.debug { "hash=$hash" }
            hashes.contains(hash).shouldBeFalse()
            hashes.add(hash)
        }
        hashes shouldHaveSize 1
    }

    private fun verifyIterator(iter: BoundingBoxGeoHashIterator): List<GeoHash> {
        val newBox = iter.boundingBox.boundingBox
        val hashes = iter.toList()

        var prev: GeoHash? = null
        hashes.forEach { gh ->
            newBox.contains(gh.originatingPoint).shouldBeTrue()
            prev?.let { pv -> pv shouldBeLessThan gh }
            prev = gh
        }

        return hashes
    }
}
