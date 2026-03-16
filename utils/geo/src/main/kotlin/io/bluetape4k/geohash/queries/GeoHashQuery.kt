package io.bluetape4k.geohash.queries

import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.WGS84Point

/**
 * GeoHash 기반 공간 질의 계약입니다.
 *
 * ## 동작/계약
 * - [contains]는 해시 또는 좌표가 질의 영역에 포함되는지 판정합니다.
 * - [getSearchHashes]는 질의 최적화에 사용하는 후보 해시 목록을 반환합니다.
 * - 구현체는 불변/가변 여부가 다를 수 있으나 조회 메서드는 일반적으로 읽기 전용입니다.
 *
 * ```kotlin
 * val contains = query.contains(point)
 * val hashes = query.getSearchHashes()
 * // hashes.isNotEmpty() == true
 * ```
 */
interface GeoHashQuery {

    /**
     * 지정한 해시가 질의 영역에 포함되는지 판정합니다.
     *
     * ## 동작/계약
     * - 판정 규칙은 구현체(원형/경계상자)에 따라 달라집니다.
     *
     * ```kotlin
     * val result = query.contains(hash)
     * // result == true
     * ```
     */
    operator fun contains(hash: GeoHash): Boolean

    /**
     * 지정한 좌표가 질의 영역에 포함되는지 판정합니다.
     *
     * ## 동작/계약
     * - 필요 시 구현체가 좌표를 GeoHash로 변환해 판정합니다.
     *
     * ```kotlin
     * val result = query.contains(point)
     * // result == true
     * ```
     */
    operator fun contains(point: WGS84Point): Boolean

    /**
     * 질의에 사용할 검색 후보 GeoHash 목록을 반환합니다.
     *
     * ## 동작/계약
     * - 반환 리스트는 구현체가 계산한 최소/확장 후보 집합입니다.
     *
     * ```kotlin
     * val hashes = query.getSearchHashes()
     * // hashes.isNotEmpty() == true
     * ```
     */
    fun getSearchHashes(): List<GeoHash>

    /**
     * 질의 영역을 WKT Polygon 문자열로 반환합니다.
     *
     * ## 동작/계약
     * - 좌표 순서/포맷은 구현체 생성 시점의 경계 정보에 의존합니다.
     *
     * ```kotlin
     * val wkt = query.getWktBox()
     * // wkt.startsWith("POLYGON") == true
     * ```
     */
    fun getWktBox(): String
}
