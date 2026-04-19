package io.bluetape4k.geohash

import io.bluetape4k.geohash.tests.RandomGeoHashes
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class GeoHashTestConvert {

    companion object: KLogging() {
        private const val DELTA = 1e-2
    }

    @Test
    fun `convert with BinaryString`() {
        RandomGeoHashes.fullRange().forEach { geohash ->
            val binaryString = geohash.toBinaryString()
            val readBack = geoHashOfBinaryString(binaryString)

            // toBinaryString이 significantBits와 동일한 길이를 반환하므로
            // 정밀도 손실 없이 완전한 라운드트립이 보장됩니다.
            readBack.significantBits() shouldBeEqualTo geohash.significantBits()

            // toBase32는 significantBits가 5의 배수일 때만 변환 가능
            if (geohash.significantBits() % 5 == 0) {
                readBack.toBase32() shouldBeEqualTo geohash.toBase32()
            }

            // BoundingBox 중심점 비교 (Double 값은 epsilon 적용)
            readBack.boundingBoxCenter.latitude.shouldBeNear(
                geohash.boundingBoxCenter.latitude,
                DELTA,
            )
            readBack.boundingBoxCenter.longitude.shouldBeNear(
                geohash.boundingBoxCenter.longitude,
                DELTA,
            )
        }
    }
}
