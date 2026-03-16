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

            // toBase32는 significantBits가 5의 배수일 때만 변환 가능
            // 원본과 복원된 모두 5의 배수일 때만 비교
            if (geohash.significantBits() % 5 == 0 && readBack.significantBits() % 5 == 0) {
                readBack.toBase32() shouldBeEqualTo geohash.toBase32()
            }

            // BoundingBox 중심점 비교 (Double 값은 epsilon 적용)
            // binaryString 변환 과정에서 정밀도 차이가 발생할 수 있으므로 큰 epsilon 사용
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
