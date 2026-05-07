package io.bluetape4k.geohash

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeNear
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GeoHashExtensionsTest: AbstractGeoHashTest() {

    companion object: KLogging() {
        private const val DELTA = 1.0e-4
    }

    @Test
    fun `geoHashWithCharacters with point`() {
        val point = WGS84Point(37.5665, 126.9780)
        val hash = geoHashWithCharacters(point, 5)
        hash.toBase32().length shouldBeEqualTo 5
    }

    @Test
    fun `geoHashWithCharacters with lat lon`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        hash.toBase32().length shouldBeEqualTo 5
        hash.significantBits() shouldBeEqualTo 25
    }

    @Test
    fun `geoHashWithCharacters precision 1 to 12`() {
        for (chars in 1..12) {
            val hash = geoHashWithCharacters(37.5665, 126.9780, chars)
            hash.toBase32().length shouldBeEqualTo chars
        }
    }

    @Test
    fun `geoHashWithBits with point`() {
        val point = WGS84Point(37.5665, 126.9780)
        val hash = geoHashWithBits(point, 25)
        hash.significantBits() shouldBeEqualTo 25
    }

    @Test
    fun `geoHashWithBits with lat lon`() {
        val hash = geoHashWithBits(37.5665, 126.9780, 25)
        hash.significantBits() shouldBeEqualTo 25
    }

    @Test
    fun `geoHashOfString round trip`() {
        val original = geoHashWithCharacters(37.5665, 126.9780, 5)
        val base32 = original.toBase32()
        val restored = geoHashOfString(base32)
        restored.toBase32() shouldBeEqualTo base32
    }

    @Test
    fun `geoHashOfString known value`() {
        val hash = geoHashOfString("ezs42")
        hash.toBase32() shouldBeEqualTo "ezs42"
    }

    @Test
    fun `geoHashOfString invalid character throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashOfString("invalid!")
        }
    }

    @Test
    fun `geoHashOfString blank throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashOfString("")
        }
    }

    @Test
    fun `geoHashOfLongValue round trip`() {
        val original = geoHashWithCharacters(37.5665, 126.9780, 5)
        val restored = geoHashOfLongValue(original.longValue, original.significantBits())
        restored.toBase32() shouldBeEqualTo original.toBase32()
    }

    @Test
    fun `geoHashOfOrd round trip`() {
        val original = geoHashWithCharacters(37.5665, 126.9780, 5)
        val restored = geoHashOfOrd(original.ord(), original.significantBits)
        restored.ord() shouldBeEqualTo original.ord()
    }

    @Test
    fun `geoHashOfBinaryString round trip`() {
        val original = geoHashWithBits(37.5665, 126.9780, 20)
        val binaryStr = original.toBinaryString()
        val restored = geoHashOfBinaryString(binaryStr)
        restored.significantBits() shouldBeEqualTo original.significantBits()
    }

    @Test
    fun `geoHashOfBinaryString invalid char throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashOfBinaryString("01012")
        }
    }

    @Test
    fun `setBoundingBox sets correct values`() {
        val hash = geoHashWithBits(0.0, 0.0, 10)
        val latRange = doubleArrayOf(-10.0, 10.0)
        val lonRange = doubleArrayOf(-20.0, 20.0)
        hash.setBoundingBox(latRange, lonRange)
        hash.boundingBox.southLatitude shouldBeEqualTo -10.0
        hash.boundingBox.northLatitude shouldBeEqualTo 10.0
        hash.boundingBox.westLongitude shouldBeEqualTo -20.0
        hash.boundingBox.eastLongitude shouldBeEqualTo 20.0
    }

    @Test
    fun `stepsBetween returns correct count`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        val next5 = hash.next(5)
        hash.stepsBetween(next5) shouldBeEqualTo 5L
    }

    @Test
    fun `stepsBetween same hash returns 0`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        hash.stepsBetween(hash) shouldBeEqualTo 0L
    }

    @Test
    fun `stepsBetween different bit precision throws`() {
        val hash5 = geoHashWithCharacters(37.5665, 126.9780, 5)
        val hash4 = geoHashWithCharacters(37.5665, 126.9780, 4)
        assertFailsWith<IllegalArgumentException> {
            hash5.stepsBetween(hash4)
        }
    }

    @Test
    fun `getAdjacent returns 8 neighbors`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        val adjacent = hash.getAdjacent()
        adjacent.shouldHaveSize(8)
        adjacent.forEach { it.shouldNotBeNull() }
    }

    @Test
    fun `getAdjacent neighbors are distinct`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        val adjacent = hash.getAdjacent()
        val distinctCount = adjacent.map { it.toBase32() }.toSet().size
        distinctCount shouldBeEqualTo 8
    }

    @Test
    fun `next and prev are inverse operations`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 5)
        val next = hash.next()
        val backToPrev = next.prev()
        backToPrev.toBase32() shouldBeEqualTo hash.toBase32()
    }

    @Test
    fun `within checks parent containment`() {
        val parent = geoHashWithCharacters(37.5665, 126.9780, 4)
        val child = geoHashWithCharacters(37.5665, 126.9780, 5)
        child.within(parent).shouldBeTrue()
        parent.within(child).shouldBeFalse()
    }

    @Test
    fun `geoHashWithBits invalid lat throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(95.0, 0.0, 25)
        }
    }

    @Test
    fun `geoHashWithBits invalid lon throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(0.0, 200.0, 25)
        }
    }

    @Test
    fun `geoHashWithBits invalid bits throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(0.0, 0.0, 100)
        }
    }

    @Test
    fun `geoHashWithCharacters invalid numberOfChars throws`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithCharacters(0.0, 0.0, 20)
        }
    }
}
