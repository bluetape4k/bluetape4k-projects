package io.bluetape4k.geohash

import io.bluetape4k.collections.exists
import io.bluetape4k.geohash.tests.RandomGeoHashes
import io.bluetape4k.geohash.utils.boundingBoxGeoHashIteratorOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.lang.reflect.Method
import kotlin.math.pow
import kotlin.test.assertFailsWith


class GeoHashTest: AbstractGeoHashTest() {

    companion object: KLogging() {
        private const val REPEATED_SIZE = 5
    }

    private lateinit var hash: GeoHash

    @BeforeEach
    fun beforeEach() {
        hash = GeoHash()
    }

    @Test
    fun `adding ones`() {
        hash.addOnBitToEnd()
        hash.bits shouldBeEqualTo 0x1L
        hash.significantBits() shouldBeEqualTo 1

        hash.addOnBitToEnd()
        hash.addOnBitToEnd()
        hash.addOnBitToEnd()

        hash.bits shouldBeEqualTo 0x0F
        hash.significantBits() shouldBeEqualTo 4
    }

    @Test
    fun `adding zeros`() {
        hash.addOnBitToEnd()
        hash.bits shouldBeEqualTo 0x1L
        hash.significantBits() shouldBeEqualTo 1

        hash.addOffBitToEnd()
        hash.addOffBitToEnd()
        hash.addOffBitToEnd()
        hash.addOffBitToEnd()

        hash.bits shouldBeEqualTo 0x10
        hash.significantBits() shouldBeEqualTo 5
    }

    @Test
    fun `encode by base32`() {
        hash.bits = 0x6ff0414000000000L
        hash.significantBits = 25

        val base32 = hash.toBase32()
        base32 shouldBeEqualTo "ezs42"
    }

    @Test
    fun `encode by base32 with invalid significantBits`() {
        hash.bits = 0x6ff0414000000000L
        hash.significantBits = 24

        assertFailsWith<IllegalStateException> {
            hash.toBase32()
        }
    }

    @Test
    fun `decode from base32`() {
        // for all lat/lon pairs check decoded point is in the same bbox as the
        // geohash formed by encoder
        RandomGeoHashes.fullRange().forEach { geohash ->
            val bbox = geohash.boundingBox

            // Decoding
            val decodedHash = geoHashOfString(geohash.toBase32())

            decodedHash shouldBeEqualTo geohash
            decodedHash.boundingBoxCenter shouldBeEqualTo geohash.boundingBoxCenter
            decodedHash.boundingBox shouldBeEqualTo bbox
            decodedHash.toBase32() shouldBeEqualTo geohash.toBase32()
        }
    }

    // FIXME: 버그 수정
    @Disabled("버그 수정 필요")
    @Test
    fun `convert with BinaryString`() {
        RandomGeoHashes.fullRange().forEach { geohash ->
            val binaryString = geohash.toBinaryString()
            val readBack = geoHashOfBinaryString(binaryString)

            readBack shouldBeEqualTo geohash
            readBack.boundingBoxCenter shouldBeEqualTo geohash.boundingBoxCenter
            readBack.boundingBox shouldBeEqualTo geohash.boundingBox
            readBack.toBase32() shouldBeEqualTo geohash.toBase32()
        }
    }

    @Test
    fun `within with other geohash`() {
        hash.bits = 0x6ff0414000000000L
        hash.significantBits = 25
        log.debug { "hash base32=${hash.toBase32()}" }
        hash.toBase32() shouldBeEqualTo "ezs42"

        val bbox = GeoHash().apply {
            bits = 0x6ff0000000000000L
            significantBits = 12
        }

        hash.within(bbox).shouldBeTrue()
    }

    @RepeatedTest(REPEATED_SIZE)
    fun `create hash with minimum precision of 0 bits`() {
        val point = RandomGeoHashes.createPoint()
        val gh = geoHashWithBits(point.latitude, point.longitude, 0)

        gh.bits shouldBeEqualTo 0x0L
        gh.longValue shouldBeEqualTo 0L
        gh.originatingPoint shouldBeEqualTo point
    }

    @Test
    fun `create hash with maximum precision of 64 bits`() {
        val gh = geoHashWithBits(10.0, 10.0, 64)

        gh.significantBits shouldBeEqualTo 64
        gh.longValue.toULong() shouldBeEqualTo 0xc07e07e07e07e07eUL
        gh.originatingPoint shouldBeEqualTo WGS84Point(10.0, 10.0)
    }

    @RepeatedTest(REPEATED_SIZE)
    fun `create hash with maximum precision of 64 bits with random point`() {
        val point = RandomGeoHashes.createPoint()
        val gh = geoHashWithBits(point.latitude, point.longitude, 64)

        gh.significantBits shouldBeEqualTo 64
        gh.originatingPoint shouldBeEqualTo point
    }


    @Test
    fun `invalid precision of 64 bits`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(10.0, 10.0, -1)
        }
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(10.0, 10.0, 65)
        }
        assertFailsWith<IllegalArgumentException> {
            geoHashWithBits(10.0, 10.0, 70)
        }
    }

    @Test
    fun `invalid character precision`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashWithCharacters(10.0, 10.0, -1)
        }
        assertFailsWith<IllegalArgumentException> {
            geoHashWithCharacters(10.0, 10.0, GeoHash.MAX_CHARACTER_PRECISION + 1)
        }
    }

    @Test
    fun `not within with other geohash`() {
        hash.bits = 0x6ff0414000000000L
        hash.significantBits = 25
        log.debug { "hash base32=${hash.toBase32()}" }
        hash.toBase32() shouldBeEqualTo "ezs42"

        val bbox = GeoHash().apply {
            bits = 0x6fc0000000000000L
            significantBits = 12
        }

        hash.within(bbox).shouldBeFalse()
    }

    @Test
    fun `create with bit precision`() {
        val hash1 = geoHashWithBits(45.0, 120.0, 20)
        hash1.significantBits shouldBeEqualTo 20
        log.debug { "hash1=$hash1" }
        log.debug { "hash1 base32=${hash1.toBase32()}" }

        val hash2 = geoHashWithBits(45.0, 120.0, 55)
        hash2.significantBits shouldBeEqualTo 55
        log.debug { "hash2=$hash2" }
        log.debug { "hash2 base32=${hash2.toBase32()}" }

        hash2.within(hash1).shouldBeTrue()

        // this should match Dave Troys Codebase.
        // This is also his maximum accuracy (12 5-nibbles).
        val hash3 = geoHashWithBits(20.0, 31.0, 60).apply {
            log.debug { "hash3=$this" }
            log.debug { "hash3 base32=${this.toBase32()}" }
        }

        hash3.significantBits shouldBeEqualTo 60
        hash3.toBase32() shouldBeEqualTo "sew1c2vs2q5r"
    }

    @Test
    fun `get bounding boxes`() {
        val hash = geoHashWithBits(40.0, 120.0, 10)
        log.debug { "hash=${hash.toBase32()}" }
        hash.printBoundingBox()
    }

    @Test
    fun `create by character precision`() {
        assertEncodingWithCharacterPrecision(WGS84Point(20.0, 31.0), 12, "sew1c2vs2q5r")
        assertEncodingWithCharacterPrecision(WGS84Point(-20.0, 31.0), 12, "ksqn1rje83g2")
        assertEncodingWithCharacterPrecision(WGS84Point(-20.783236276, 31.9867127312312), 12, "ksq9zbs0b7vw")

        val point = WGS84Point(-76.5110040642321, 39.0247389581054)
        val fullStringValue = "hf7u8p8gn747"

        for (characters in 12 downTo 2) {
            assertEncodingWithCharacterPrecision(point, characters, fullStringValue.substring(0, characters))
        }

        assertEncodingWithCharacterPrecision(WGS84Point(39.0247389581054, -76.5110040642321), 12, "dqcw4bnrs6s7")

        val geoHashString = geoHashWithCharacters(point.latitude, point.longitude, 12).toBase32()
        geoHashString shouldBeEqualTo fullStringValue
    }

    private fun assertEncodingWithCharacterPrecision(point: WGS84Point, numOfChars: Int, expectBase32: String) {
        val hash = geoHashWithCharacters(point.latitude, point.longitude, numOfChars)
        hash.toBase32() shouldBeEqualTo expectBase32
    }

    @Test
    fun `get latitude bits`() {
        val hash = geoHashWithBits(30.0, 30.0, 16)
        val latitudeBits = hash.getRightAlignedLatitudeBits()

        latitudeBits[0] shouldBeEqualTo 0xaaL
        latitudeBits[1] shouldBeEqualTo 8L
    }

    @Test
    fun `get longitude bits`() {
        val hash = geoHashWithBits(30.0, 30.0, 16)
        val longitudeBits = hash.getRightAlignedLongitudeBits()

        longitudeBits[0] shouldBeEqualTo 0x95L
        longitudeBits[1] shouldBeEqualTo 8L
    }

    @Test
    fun `get neighbor location code`() {
        // set up corner case
        hash.bits = 0xc400000000000000UL.toLong()
        hash.significantBits = 7

        val lonBits = hash.getRightAlignedLongitudeBits()
        lonBits[0] shouldBeEqualTo 0x8L
        lonBits[1] shouldBeEqualTo 4L

        val latBits = hash.getRightAlignedLatitudeBits()
        latBits[0] shouldBeEqualTo 0x5L
        latBits[1] shouldBeEqualTo 3L

        val north = hash.getNorthernNeighbor()
        north.bits shouldBeEqualTo 0xd000000000000000UL.toLong()
        north.significantBits shouldBeEqualTo 7

        val south = hash.getSouthernNeighbor()
        south.bits shouldBeEqualTo 0xc000000000000000UL.toLong()
        south.significantBits shouldBeEqualTo 7

        val east = hash.getEasternNeighbor()
        east.bits shouldBeEqualTo 0xc600000000000000UL.toLong()

        // NOTE: this is actually a corner case!
        val west = hash.getWesternNeighbor()
        west.bits shouldBeEqualTo 0x6e00000000000000UL.toLong()

        // NOTE: and now, for the most extreme corner case in 7-bit geohash-land
        hash.bits = -0x200000000000000L
        val east2 = hash.getEasternNeighbor()
        east2.bits shouldBeEqualTo 0x5400000000000000L
    }

    @Test
    fun `equals and hashCode`() {
        val hash1 = geoHashWithBits(30.0, 30.0, 24)
        val hash2 = geoHashWithBits(30.0, 30.0, 24)
        val hash3 = geoHashWithBits(30.0, 30.0, 10)

        hash2 shouldBeEqualTo hash1
        hash3 shouldNotBeEqualTo hash1

        hash2.hashCode() shouldBeEqualTo hash1.hashCode()
        hash3.hashCode() shouldNotBeEqualTo hash1.hashCode()
    }

    @Test
    fun `adjacent hashs`() {
        val adjacent = geoHashOfString("dqcw4").getAdjacent()
        adjacent.forEach {
            log.debug { "adjacent=${it.toBase32()}" }
        }
        adjacent shouldHaveSize 8
    }

    @Test
    fun `moving in circle`() {
        // moving around hashes in a circle should be possible
        assertMovingInCircle(34.2, -45.123)

        // this should also work at the "back" of the earth
        assertMovingInCircle(45.0, 180.0)
        assertMovingInCircle(90.0, 180.0)
        assertMovingInCircle(0.0, -180.0)
    }

    private fun assertMovingInCircle(latitude: Double, longitude: Double) {
        val start = geoHashWithCharacters(latitude, longitude, 12)
        var end = start.getEasternNeighbor()
        end = end.getSouthernNeighbor()
        end = end.getWesternNeighbor()
        end = end.getNorthernNeighbor()

        end shouldBeEqualTo start
        end.boundingBox shouldBeEqualTo start.boundingBox
    }

    @Test
    fun `moving around world on hash string`() {
        val directions = arrayOf("Northern", "Eastern", "Southern", "Western")
        directions.forEach {
            checkMoveAroundStrip(it)
        }
    }

    private fun checkMoveAroundStrip(direction: String) {
        for (bits in 2 until 16) {
            val hash: GeoHash = RandomGeoHashes.createWithPrecision(bits)
            val method: Method = hash.javaClass.getDeclaredMethod("get" + direction + "Neighbor")
            var result = hash

            // moving this direction 2^bits times should yield the same hash again
            for (i in 0 until 2.0.pow(bits.toDouble()).toInt()) {
                result = method.invoke(result) as GeoHash
            }
            result shouldBeEqualTo hash
        }
    }

    @Test
    fun `known neighboring hashes`() {
        val h1 = geoHashOfString("u1pb")
        h1.getSouthernNeighbor().toBase32() shouldBeEqualTo "u0zz"
        h1.getNorthernNeighbor().toBase32() shouldBeEqualTo "u1pc"
        h1.getEasternNeighbor().toBase32() shouldBeEqualTo "u300"
        h1.getEasternNeighbor().getEasternNeighbor().toBase32() shouldBeEqualTo "u302"
        h1.getWesternNeighbor().toBase32() shouldBeEqualTo "u1p8"

        geoHashWithCharacters(41.7, 0.08, 4).toBase32() shouldBeEqualTo "sp2j"
    }

    @Test
    fun `known adjacent neighbor`() {
        var center = "dqcjqc"
        var adjacent = arrayOf("dqcjqf", "dqcjqb", "dqcjr1", "dqcjq9", "dqcjqd", "dqcjr4", "dqcjr0", "dqcjq8")
        assertAdjacentHashesAre(center, adjacent)

        center = "u1x0dfg"
        adjacent = arrayOf("u1x0dg4", "u1x0dg5", "u1x0dgh", "u1x0dfu", "u1x0dfs", "u1x0dfe", "u1x0dfd", "u1x0dff")
        assertAdjacentHashesAre(center, adjacent)

        center = "sp2j"
        adjacent = arrayOf("ezry", "sp2n", "sp2q", "sp2m", "sp2k", "sp2h", "ezru", "ezrv")
        assertAdjacentHashesAre(center, adjacent)
    }

    private fun assertAdjacentHashesAre(centerString: String, adjacentStrings: Array<String>) {
        val center = geoHashOfString(centerString)
        val adjacent = center.getAdjacent()
        adjacentStrings.forEach { check ->
            assertArrayContainsGeoHash(check, adjacent)
        }
    }

    private fun assertArrayContainsGeoHash(check: String, hashes: Array<GeoHash>) {
        hashes.map { it.toBase32() }.exists { it == check }.shouldBeTrue()
    }

    @Test
    fun `adjacent hashes have initialized point`() {
        val center = "dqcjqc"
        val geohash = geoHashOfString(center)
        val adjacentHashes = geohash.getAdjacent()

        adjacentHashes.forEach { adjacent ->
            adjacent.boundingBox.shouldNotBeNull()
            adjacent.boundingBoxCenter.shouldNotBeNull()
            adjacent.originatingPoint.shouldNotBeNull()
        }
    }

    @Test
    fun `neighbor hashes near merdian`() {
        val hash = geoHashOfString("sp2j")
        val west = hash.getWesternNeighbor()
        west.toBase32() shouldBeEqualTo "ezrv"
        val west2 = west.getWesternNeighbor()
        west2.toBase32() shouldBeEqualTo "ezrt"
    }

    @Test
    fun `from geohash string with various characters`() {
        val lat = 40.390943
        val lon = -75.9375
        val hash = geoHashWithCharacters(lat, lon, 12)

        val base32 = "dr4jb0bn2180"
        val fromRef = geoHashOfString(base32)
        fromRef shouldBeEqualTo hash
        hash.toBase32() shouldBeEqualTo base32
        fromRef.toBase32() shouldBeEqualTo base32

        val hash2 = geoHashWithCharacters(lat, lon, 10)
        hash2.toBase32() shouldBeEqualTo "dr4jb0bn21"
    }

    @Test
    fun `simple within`() {
        val hash = geoHashWithBits(70.0, -120.0, 8)
        val inside = geoHashWithBits(74.0, -130.0, 64)
        inside.within(hash).shouldBeTrue()
    }

    @Test
    fun `convert to Long and reverse`() {
        val lat = 40.390943
        val lon = -75.9375
        val hash = geoHashWithCharacters(lat, lon, 10)
        val lv = hash.longValue

        hash.next().longValue shouldBeEqualTo lv + (1 shl (64 - hash.significantBits))

        val hashFromLong = geoHashOfLongValue(lv, hash.significantBits())
        hashFromLong shouldBeEqualTo hash
        hashFromLong.toBase32() shouldBeEqualTo hash.toBase32()
    }

    @Test
    fun `next geohash is greater`() {
        val lat = 37.7
        val lon = -122.52
        val hash = geoHashWithBits(lat, lon, 10)
        val next = hash.next()
        next shouldBeGreaterThan hash
    }

    @Test
    fun `next and prev`() {
        val lat = 37.7
        val lon = -122.52

        val hash = geoHashWithBits(lat, lon, 35)

        val next = hash.next()
        next shouldBeGreaterThan hash

        val prev1 = next.prev()
        prev1 shouldBeEqualTo hash

        val prev = prev1.next(-1)
        prev shouldBeLessThan hash
    }

    @Test
    fun `character precision을 얻기 위해서는 precision bits 가 5의 배수이어야 합니다`() {
        assertFailsWith<IllegalStateException> {
            val hash = geoHashWithBits(37.7, -122.52, 32)
            // Significand bits 는 5의 배수여야 합니다.
            hash.getCharacterPrecision()
        }
    }

    @Test
    fun `precision bits가 5의 배수라면 character precision을 얻을 수 있다`() {
        val hash = geoHashWithBits(37.7, -122.52, 60)
        hash.getCharacterPrecision() shouldBeEqualTo 12
    }

    @Test
    fun `invalid geohashing string`() {
        assertFailsWith<IllegalArgumentException> {
            geoHashOfString("abba")
        }

        assertFailsWith<IllegalArgumentException> {
            geoHashOfString("  \t  ")
        }

        assertFailsWith<IllegalArgumentException> {
            geoHashOfString("    ")
        }
    }

    @Test
    fun `steps between`() {
        val bl = geoHashWithBits(37.7, -122.52, 35)
        val ur = geoHashWithBits(37.84, -122.35, 35)

        bl.stepsBetween(bl) shouldBeEqualTo 0L
        bl.stepsBetween(bl.next(4)) shouldBeEqualTo 4L

        ur.stepsBetween(ur) shouldBeEqualTo 0L
        ur.stepsBetween(ur.next(6)) shouldBeEqualTo 6L
    }

    @Test
    fun `steps between with bouding box`() {
        val bl = geoHashWithBits(37.7, -122.52, 35)
        val ur = geoHashWithBits(37.84, -122.35, 35)

        val count = boundingBoxGeoHashIteratorOf(bl, ur).asSequence().count()
        count shouldBeEqualTo 12875

        val iter = boundingBoxGeoHashIteratorOf(bl, ur)

        var allHashes = 0
        var inBbox = 1
        var latMore = 0
        var lonMore = 0
        var bothMore = 0
        var latLess = 0
        var lonLess = 0
        var bothLess = 0
        var latLessLonMore = 0
        var latMoreLonLess = 0
        var idx: GeoHash = bl
        val iterBbox: BoundingBox = iter.boundingBox.boundingBox
        while (idx < ur) {
            idx = idx.next()
            allHashes++
            if (iterBbox.contains(idx.originatingPoint)) {
                inBbox++
            }
            var latIsMore = false
            var latIsLess = false
            if (idx.originatingPoint.latitude > iterBbox.northLatitude) {
                latIsMore = true
                latMore++
            } else if (idx.originatingPoint.latitude < iterBbox.southLatitude) {
                latIsLess = true
                latLess++
            }
            if (idx.originatingPoint.longitude > iterBbox.eastLongitude) {
                lonMore++
                if (latIsMore) {
                    bothMore++
                }
                if (latIsLess) {
                    latLessLonMore++
                }
            } else if (idx.originatingPoint.longitude < iterBbox.westLongitude) {
                lonLess++
                if (latIsLess) {
                    bothLess++
                }
                if (latIsMore) {
                    latMoreLonLess++
                }
            }
        }

        val steps = bl.stepsBetween(ur)
        steps shouldBeEqualTo 48472L
        allHashes shouldBeEqualTo steps.toInt()
        inBbox shouldBeEqualTo count
        latMore shouldBeEqualTo 14938
        lonMore shouldBeEqualTo 640
        bothMore shouldBeEqualTo 0
        latLess shouldBeEqualTo 7680
        lonLess shouldBeEqualTo 24391
        bothLess shouldBeEqualTo 0
        latLessLonMore shouldBeEqualTo 240
        latMoreLonLess shouldBeEqualTo 11811

        val sum = lonLess + latLess + latMore + lonMore + inBbox - latLessLonMore - latMoreLonLess - 1
        sum shouldBeEqualTo steps.toInt()
    }

    @Test
    fun `verify compareTo with base32`() {
        var prevHash: GeoHash? = null
        repeat(10_000) {
            val hash = RandomGeoHashes.createWith5BitsPrecision()
            prevHash?.let { prev ->
                val prevBase32 = prev.toBase32()
                val hashBase32 = hash.toBase32()

                log.trace { "prev=$prevBase32, hash=$hashBase32" }

                if (prevBase32 < hashBase32) {
                    prev shouldBeLessThan hash
                } else if (prevBase32 > hashBase32) {
                    prev shouldBeGreaterThan hash
                } else {
                    prev shouldBeEqualTo hash
                }
            }
            prevHash = hash
        }
    }

    @Test
    fun `ord is positive`() {
        val lat = 40.390943
        val lon = 75.9375
        val hash = geoHashWithCharacters(lat, lon, 12)
        hash.ord() shouldBeEqualTo 0xcf6915015410500L
        hash.ord() shouldBeGreaterOrEqualTo 0L
    }

    @Test
    fun `second case where ord must be positive`() {
        val hash = geoHashWithCharacters(-36.919550434870125, 174.71024582237604, 7)
        hash.ord() shouldBeGreaterThan 0L
    }
}
