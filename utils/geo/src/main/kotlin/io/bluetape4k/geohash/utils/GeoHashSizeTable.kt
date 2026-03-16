package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.geohash.utils.GeoHashSizeTable.dLat
import io.bluetape4k.geohash.utils.GeoHashSizeTable.dLon
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlin.math.pow

object GeoHashSizeTable: KLogging() {

    private const val NUM_BITS = 64

    val dLat = DoubleArray(NUM_BITS) { 180.0 / 2.0.pow(it / 2) }
    val dLon = DoubleArray(NUM_BITS) { 360.0 / 2.0.pow((it + 1) / 2) }

    /**
     * 경계 상자를 모두 덮기 위해 필요한 GeoHash 비트 수를 계산합니다.
     *
     * ## 동작/계약
     * - 위도/경도 분해능 테이블([dLat], [dLon])을 사용해 63비트에서 역순 탐색합니다.
     * - 반환값이 클수록 더 정밀한 해시를 의미합니다.
     *
     * ```kotlin
     * val bits = GeoHashSizeTable.numberOfBitsForOverlappingGeoHash(bbox)
     * // bits in 0..63
     * ```
     */
    fun numberOfBitsForOverlappingGeoHash(bbox: BoundingBox): Int {
        var bits = 63
        val height = bbox.getLatitudeSize()
        val width = bbox.getLongitudeSize()

        log.trace { "height=$height, width=$width, bbox=$bbox" }

        while ((dLat[bits] < height || dLon[bits] < width) && bits > 0) {
            bits--
        }
        return bits
    }
}
