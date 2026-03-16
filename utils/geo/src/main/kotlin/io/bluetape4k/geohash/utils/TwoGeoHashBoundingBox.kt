package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.BoundingBox
import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.geoHashOfLongValue
import io.bluetape4k.geohash.geoHashWithBits
import io.bluetape4k.geohash.geoHashWithCharacters
import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 동일 정밀도의 남서/북동 GeoHash로 반복 가능한 경계 상자를 생성합니다.
 *
 * ## 동작/계약
 * - 두 해시의 `significantBits`가 같아야 하며 다르면 [IllegalArgumentException]이 발생합니다.
 * - 입력 해시를 동일 비트수 기준으로 정규화해 새 [TwoGeoHashBoundingBox]를 반환합니다.
 *
 * ```kotlin
 * val box = twoGeoHashBoundingBoxOf(sw, ne)
 * // box.southWestCorner.significantBits() == box.northEastCorner.significantBits()
 * ```
 */
fun twoGeoHashBoundingBoxOf(southWest: GeoHash, northEast: GeoHash): TwoGeoHashBoundingBox {
    require(southWest.significantBits() == northEast.significantBits()) {
        "Does it make sense to iterate between hashes that have different precisions?"
    }
    val southWestCorner = geoHashOfLongValue(southWest.longValue, southWest.significantBits())
    val northEastCorner = geoHashOfLongValue(northEast.longValue, northEast.significantBits())
    val boundingBox = BoundingBox(
        southWestCorner.boundingBox.southLatitude,
        northEastCorner.boundingBox.northLatitude,
        southWestCorner.boundingBox.westLongitude,
        northEastCorner.boundingBox.eastLongitude
    )
    return TwoGeoHashBoundingBox(southWestCorner, northEastCorner, boundingBox)
}

/**
 * 경계 상자와 문자 정밀도로 [TwoGeoHashBoundingBox]를 생성합니다.
 *
 * ## 동작/계약
 * - 남서/북동 코너를 같은 문자 길이로 계산합니다.
 * - 결과 코너 정밀도는 `numberOfCharacter * 5` 비트(최대 제한 적용)입니다.
 *
 * ```kotlin
 * val box = twoGeoHashWithCharacters(bbox, 6)
 * // box.southWestCorner.getCharacterPrecision() == 6
 * ```
 */
fun twoGeoHashWithCharacters(bbox: BoundingBox, numberOfCharacter: Int): TwoGeoHashBoundingBox {
    val southWest = geoHashWithCharacters(bbox.southLatitude, bbox.westLongitude, numberOfCharacter)
    val northEast = geoHashWithCharacters(bbox.northLatitude, bbox.eastLongitude, numberOfCharacter)

    return twoGeoHashBoundingBoxOf(southWest, northEast)
}

/**
 * 경계 상자와 비트 정밀도로 [TwoGeoHashBoundingBox]를 생성합니다.
 *
 * ## 동작/계약
 * - 남서/북동 코너를 같은 비트 수로 계산합니다.
 * - 비트 범위 검증은 내부 `geoHashWithBits`에서 수행됩니다.
 *
 * ```kotlin
 * val box = twoGeoHashWithBits(bbox, 30)
 * // box.southWestCorner.significantBits() == 30
 * ```
 */
fun twoGeoHashWithBits(bbox: BoundingBox, numberOfBits: Int): TwoGeoHashBoundingBox {
    val southWest = geoHashWithBits(bbox.southLatitude, bbox.westLongitude, numberOfBits)
    val northEast = geoHashWithBits(bbox.northLatitude, bbox.eastLongitude, numberOfBits)

    return twoGeoHashBoundingBoxOf(southWest, northEast)
}

/**
 * 남서/북동 GeoHash 코너와 실제 경계 상자를 함께 보관하는 값 객체입니다.
 *
 * ## 동작/계약
 * - 코너 해시는 동일 정밀도여야 반복/샘플링 유틸과 함께 사용할 수 있습니다.
 * - 불변 데이터 구조이며 직렬화를 지원합니다.
 *
 * ```kotlin
 * val text = box.toBase32()
 * // text.startsWith(box.southWestCorner.toBase32())
 * ```
 */
data class TwoGeoHashBoundingBox(
    val southWestCorner: GeoHash,
    val northEastCorner: GeoHash,
    val boundingBox: BoundingBox,
): Serializable {

    companion object: KLogging()

    /**
     * 남서/북동 코너를 이어 붙인 Base32 표현을 반환합니다.
     *
     * ## 동작/계약
     * - `southWestCorner.toBase32() + northEastCorner.toBase32()` 형식을 따릅니다.
     * - 새 문자열을 반환하며 상태를 변경하지 않습니다.
     *
     * ```kotlin
     * val encoded = box.toBase32()
     * // encoded.length == box.southWestCorner.toBase32().length * 2
     * ```
     */
    fun toBase32(): String {
        return southWestCorner.toBase32() + northEastCorner.toBase32()
    }
}
