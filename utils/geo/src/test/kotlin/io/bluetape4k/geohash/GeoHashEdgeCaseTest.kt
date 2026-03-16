package io.bluetape4k.geohash

import io.bluetape4k.geohash.tests.RandomGeoHashes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GeoHashEdgeCaseTest: AbstractGeoHashTest() {
    companion object: KLogging()

    @Test
    fun `경계값 위도 경도로 GeoHash 생성`() {
        // 최대/최소 위도/경도
        val maxLat = geoHashWithCharacters(90.0, 0.0, 12)
        val minLat = geoHashWithCharacters(-90.0, 0.0, 12)
        val maxLon = geoHashWithCharacters(0.0, 180.0, 12)
        val minLon = geoHashWithCharacters(0.0, -180.0, 12)

        maxLat shouldBeEqualTo geoHashOfString(maxLat.toBase32())
        minLat shouldBeEqualTo geoHashOfString(minLat.toBase32())
        maxLon shouldBeEqualTo geoHashOfString(maxLon.toBase32())
        minLon shouldBeEqualTo geoHashOfString(minLon.toBase32())
    }

    @Test
    fun `짧은 Base32 문자열`() {
        val hash1 = geoHashOfString("d")
        val hash2 = geoHashOfString("dr")
        val hash3 = geoHashOfString("drt")

        hash1.getCharacterPrecision() shouldBeEqualTo 1
        hash2.getCharacterPrecision() shouldBeEqualTo 2
        hash3.getCharacterPrecision() shouldBeEqualTo 3
    }

    @Test
    fun `next와 prev 순환 검증`() {
        val hash = geoHashOfString("9q8y")

        val next = hash.next()
        val prev = next.prev()

        prev shouldBeEqualTo hash
    }

    @Test
    fun `8방위 이웃 검증`() {
        val hash = geoHashOfString("9q8y")
        val adjacent = hash.getAdjacent()

        adjacent.size shouldBeEqualTo 8

        // 북쪽, 북동, 동, 남동, 남, 남서, 서, 북서
        val north = adjacent[0]
        val northEast = adjacent[1]
        val east = adjacent[2]
        val southEast = adjacent[3]
        val south = adjacent[4]
        val southWest = adjacent[5]
        val west = adjacent[6]
        val northWest = adjacent[7]

        // 북쪽은 남쪽의 북쪽 이웃과 같아야 함
        north.getSouthernNeighbor() shouldBeEqualTo hash
        south.getNorthernNeighbor() shouldBeEqualTo hash
        east.getWesternNeighbor() shouldBeEqualTo hash
        west.getEasternNeighbor() shouldBeEqualTo hash
    }

    @Test
    fun `이웃 GeoHash 간의 관계`() {
        val hash = geoHashOfString("9q8y")

        val north = hash.getNorthernNeighbor()
        val south = hash.getSouthernNeighbor()
        val east = hash.getEasternNeighbor()
        val west = hash.getWesternNeighbor()

        // 북쪽의 남쪽은 원래 해시
        north.getSouthernNeighbor() shouldBeEqualTo hash

        // 남쪽의 북쪽은 원래 해시
        south.getNorthernNeighbor() shouldBeEqualTo hash

        // 동쪽의 서쪽은 원래 해시
        east.getWesternNeighbor() shouldBeEqualTo hash

        // 서쪽의 동쪽은 원래 해시
        west.getEasternNeighbor() shouldBeEqualTo hash
    }

    @Test
    fun `stepsBetween 동일 precision`() {
        val hash1 = geoHashOfString("9q8y")
        val hash2 = geoHashOfString("9q8z")

        val steps = hash1.stepsBetween(hash2)
        steps shouldBeEqualTo 1L
    }

    @Test
    fun `stepsBetween 다른 precision은 예외`() {
        val hash1 = geoHashOfString("9q8y")
        val hash2 = geoHashOfString("9q8yt")

        assertThrows<IllegalArgumentException> {
            hash1.stepsBetween(hash2)
        }
    }

    @Test
    fun `ord 값 검증`() {
        val hash1 = geoHashOfString("9q8y")
        val hash2 = geoHashOfString("9q8z")

        hash1.ord() shouldBeEqualTo (hash2.ord() - 1)
    }

    @Test
    fun `within 검증`() {
        val parent = geoHashOfString("9q8")
        val child = geoHashOfString("9q8y")

        child.within(parent).shouldBeTrue()
        parent.within(child).shouldBeFalse()
    }

    @Test
    fun `contains point 검증`() {
        val hash = geoHashWithCharacters(37.5665, 126.9780, 12)
        val center = hash.boundingBoxCenter

        hash.contains(center).shouldBeTrue()
    }

    @Test
    fun `toBinaryString 검증`() {
        val hash = geoHashOfString("9q8y")
        val binary = hash.toBinaryString()

        binary.forEach { char ->
            (char == '0' || char == '1').shouldBeTrue()
        }
    }

    @Test
    fun `compareTo 검증`() {
        val hash1 = geoHashOfString("9q8y")
        val hash2 = geoHashOfString("9q8z")

        (hash1 < hash2).shouldBeTrue()
        (hash1 >= hash2).shouldBeFalse()

        hash1 shouldBeEqualTo hash1
    }

    @Test
    fun `equals와 hashCode 일관성`() {
        val hash1 = geoHashOfString("9q8y")
        val hash2 = geoHashOfString("9q8y")

        hash1 shouldBeEqualTo hash2
        hash1.hashCode() shouldBeEqualTo hash2.hashCode()
    }

    @Test
    fun `최대 precision GeoHash`() {
        val maxPrecision = GeoHash.MAX_CHARACTER_PRECISION
        val hash = geoHashWithCharacters(37.5665, 126.9780, maxPrecision)

        hash.getCharacterPrecision() shouldBeEqualTo maxPrecision
    }

    @Test
    fun `bit precision 검증`() {
        val hash = geoHashWithBits(37.5665, 126.9780, 30)

        hash.significantBits() shouldBeEqualTo 30
    }

    @Test
    fun `random GeoHash 생성`() {
        val hashes = RandomGeoHashes.fullRange().take(100).toList()

        hashes.size shouldBeEqualTo 100
        hashes.forEach { hash ->
            hash.significantBits() > 0
        }
    }

    @Test
    fun `boundingBox 확장`() {
        val hash = geoHashOfString("9q8y")
        val bbox = hash.boundingBox

        // 중심점이 bounding box 내에 있어야 함
        val center = bbox.getCenter()
        bbox.contains(center).shouldBeTrue()
    }
}
