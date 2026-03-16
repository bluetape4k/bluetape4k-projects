package io.bluetape4k.geohash.utils

import io.bluetape4k.geohash.GeoHash

/**
 * 두 해시 코너로 정의된 영역의 GeoHash 반복자를 생성합니다.
 *
 * ## 동작/계약
 * - [TwoGeoHashBoundingBox] 내부의 [GeoHash]를 순차 탐색합니다.
 * - 새 반복자 인스턴스를 생성하며 입력 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val iterator = boundingBoxGeoHashIteratorOf(twoGeoHashBoundingBoxOf(sw, ne))
 * // iterator.hasNext() == true
 * ```
 */
fun boundingBoxGeoHashIteratorOf(boundingBox: TwoGeoHashBoundingBox): BoundingBoxGeoHashIterator =
    BoundingBoxGeoHashIterator(boundingBox)

/**
 * 남서/북동 코너 해시로 반복자를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [twoGeoHashBoundingBoxOf]를 거쳐 반복자를 만듭니다.
 * - 두 해시의 정밀도가 다르면 박스 생성 단계에서 예외가 발생합니다.
 *
 * ```kotlin
 * val iterator = boundingBoxGeoHashIteratorOf(sw, ne)
 * // iterator.hasNext() == true
 * ```
 */
fun boundingBoxGeoHashIteratorOf(
    southWest: GeoHash,
    northEast: GeoHash,
): BoundingBoxGeoHashIterator = BoundingBoxGeoHashIterator(twoGeoHashBoundingBoxOf(southWest, northEast))

/**
 * [TwoGeoHashBoundingBox] 범위 내 GeoHash를 순차 순회하는 반복자입니다.
 *
 * ## 동작/계약
 * - `next()`는 현재 값을 반환한 뒤 다음 유효 해시로 이동합니다.
 * - 남은 요소가 없을 때 `next()` 호출 시 [NoSuchElementException]이 발생합니다.
 * - 내부 포인터를 변경하는 stateful iterator입니다.
 *
 * ```kotlin
 * val iterator = boundingBoxGeoHashIteratorOf(sw, ne)
 * val first = iterator.next()
 * // first == sw
 * ```
 */
class BoundingBoxGeoHashIterator(
    val boundingBox: TwoGeoHashBoundingBox,
) : Iterator<GeoHash> {
    private var current: GeoHash? = boundingBox.southWestCorner

    override fun hasNext(): Boolean = current != null

    override fun next(): GeoHash {
        if (!hasNext()) {
            throw NoSuchElementException()
        }

        val rv: GeoHash = current ?: throw NoSuchElementException()

        if (rv == boundingBox.northEastCorner) {
            current = null
        } else {
            current = rv.next()
            while (hasNext() &&
                !boundingBox.boundingBox.contains(
                    requireNotNull(current) { "current GeoHash가 null입니다." }.originatingPoint
                )
            ) {
                current = current?.next()
            }
        }

        return rv
    }
}
