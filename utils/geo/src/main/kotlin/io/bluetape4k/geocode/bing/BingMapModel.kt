package io.bluetape4k.geocode.bing

import io.bluetape4k.geocode.bing.BingMapModel.toBingAddress
import java.io.Serializable

/**
 * Bing Maps API 에서 제공하는 주소 정보를 나타내는 데이터 클래스들입니다.
 *
 * 참고: [Bing Maps API](https://www.bingmapsportal.com/)
 *
 * ## 동작/계약
 * - 응답 DTO를 그대로 반영하는 불변 값 객체들입니다.
 * - [Location.toBingAddress]는 첫 resourceSet/resource만 사용해 주소를 생성합니다.
 *
 * ```kotlin
 * val address = location.toBingAddress()
 * // address == null || address.formattedAddress != null
 * ```
 */
object BingMapModel {

    /**
     * Bing Location 응답을 도메인 [BingAddress]로 변환합니다.
     *
     * ## 동작/계약
     * - `resourceSets.firstOrNull()?.resources.firstOrNull()` 경로만 매핑합니다.
     * - 매핑 가능한 리소스가 없으면 null을 반환합니다.
     */
    fun Location.toBingAddress(): BingAddress? {
        return resourceSets.firstOrNull()?.let { resourceSet ->
            resourceSet.resources.firstOrNull()?.let { resource ->
                BingAddress(
                    name = resource.name,
                    country = resource.address.countryRegion,
                    city = resource.address.adminDistrict,
                    detailAddress = resource.address.addressLine,
                    zipCode = resource.address.postalCode,
                    formattedAddress = resource.address.formattedAddress,
                )
            }
        }
    }

    /**
     * Bing Maps API의 최상위 응답 모델입니다.
     *
     * ```kotlin
     * val location = client.locations(37.5665, 126.9780)
     * // location.statusCode == 200
     * ```
     */
    data class Location(
        val resourceSets: Array<ResourceSet> = emptyArray(),
        val statusCode: Int? = null,
        val statusDescription: String? = null,
    ): Serializable

    /**
     * Bing Maps API의 리소스 묶음 응답 모델입니다.
     *
     * ```kotlin
     * val resourceSet = location.resourceSets.firstOrNull()
     * // resourceSet?.estimatedTotal != null
     * ```
     */
    data class ResourceSet(
        val estimatedTotal: Int? = null,
        val resources: Array<Resource> = emptyArray(),
    ): Serializable

    /**
     * Bing Maps API의 단일 위치 리소스 모델입니다.
     *
     * ```kotlin
     * val resource = resourceSet.resources.firstOrNull()
     * // resource?.name != null
     * ```
     */
    data class Resource(
        val bbox: Array<Double> = emptyArray(),
        val name: String,
        val point: Point,
        val address: Address,
        val confidence: String? = null,
        val entityType: String? = null,
        val geocodePoints: Array<Point> = emptyArray(),
        val matchCodes: Array<String>,
    ): Serializable

    /**
     * Bing Maps API의 위치 좌표 모델입니다.
     *
     * ```kotlin
     * val point = resource.point
     * // point.coordinates.size == 2
     * ```
     */
    data class Point(
        val type: String,
        val coordinates: Array<Double> = emptyArray(),
        val calculationMethod: String? = null,
        val usageTypes: Array<String> = emptyArray(),
    ): Serializable

    /**
     * Bing Maps API의 주소 컴포넌트 모델입니다.
     *
     * ```kotlin
     * val address = resource.address
     * // address.countryRegion != null
     * ```
     */
    data class Address(
        val addressLine: String? = null,
        val adminDistrict: String? = null,
        val countryRegion: String? = null,
        val formattedAddress: String? = null,
        val locality: String? = null,
        val postalCode: String? = null,
    ): Serializable
}
