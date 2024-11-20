package io.bluetape4k.geocode.bing

/**
 * Bing Maps API 에서 제공하는 주소 정보를 나타내는 데이터 클래스들입니다.
 *
 * 참고: [Bing Maps API](https://www.bingmapsportal.com/)
 */
object BingMapModel {

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

    data class Location(
        val resourceSets: Array<ResourceSet> = emptyArray(),
        val statusCode: Int? = null,
        val statusDescription: String? = null,
    )

    data class ResourceSet(
        val estimatedTotal: Int? = null,
        val resources: Array<Resource> = emptyArray(),
    )

    data class Resource(
        val bbox: Array<Double> = emptyArray(),
        val name: String,
        val point: Point,
        val address: Address,
        val confidence: String? = null,
        val entityType: String? = null,
        val geocodePoints: Array<Point> = emptyArray(),
        val matchCodes: Array<String>,
    )

    data class Point(
        val type: String,
        val coordinates: Array<Double> = emptyArray(),
        val calculationMethod: String? = null,
        val usageTypes: Array<String> = emptyArray(),
    )

    data class Address(
        val addressLine: String? = null,
        val adminDistrict: String? = null,
        val countryRegion: String? = null,
        val formattedAddress: String? = null,
        val locality: String? = null,
        val postalCode: String? = null,
    )
}
