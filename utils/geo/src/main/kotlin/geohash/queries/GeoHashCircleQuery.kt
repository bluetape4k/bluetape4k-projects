package io.bluetape4k.geohash.queries

import io.bluetape4k.geohash.GeoHash
import io.bluetape4k.geohash.WGS84Point
import io.bluetape4k.geohash.boundingBoxOf
import io.bluetape4k.geohash.utils.VincentyGeodesy
import io.bluetape4k.logging.KLogging
import java.io.Serializable

/**
 * 지정된 중심 위치와 반경을 기준으로 GeoHash로 표현된 지점을 검색하는 [GeoHashCircleQuery]를 생성합니다.
 *
 * @param latitude 위도
 * @param longitude 경도
 * @param radius 반경 (단위: meter)
 */
fun geoHashCircleQueryOf(latitude: Double, longitude: Double, radius: Double): GeoHashCircleQuery =
    geoHashCircleQueryOf(WGS84Point(latitude, longitude), radius)

/**
 * 지정된 중심 위치와 반경을 기준으로 GeoHash로 표현된 지점을 검색하는 [GeoHashCircleQuery]를 생성합니다.
 *
 * @param center 중심 위치
 * @param radius 반경 (단위: meter)
 * @return GeoHashCircleQuery
 */
fun geoHashCircleQueryOf(center: WGS84Point, radius: Double): GeoHashCircleQuery {
    return GeoHashCircleQuery(center, radius)
}

/**
 * GeoHash로 표현된 특정 지점을 기준으로 반경 검색을 나타냅니다.
 * GeoHash의 정확도를 높이기 위해 원을 정사각형으로 근사합니다.
 *
 * @property center 중심 위치
 * @property radius 반경 (단위: meter)
 */
class GeoHashCircleQuery(
    private val center: WGS84Point,
    private val radius: Double,
): GeoHashQuery, Serializable {

    companion object: KLogging()

    private val query: GeoHashBoundingBoxQuery by lazy {
        val northEastCorner = VincentyGeodesy.moveInDirection(
            VincentyGeodesy.moveInDirection(center, 0.0, radius),
            90.0,
            radius
        )
        val southWestCorner = VincentyGeodesy.moveInDirection(
            VincentyGeodesy.moveInDirection(center, 180.0, radius),
            270.0,
            radius
        )
        val bbox = boundingBoxOf(southWestCorner, northEastCorner)
        GeoHashBoundingBoxQuery(bbox)
    }

    /**
     * 지정된 GeoHash가 반경 내에 포함되는지 여부를 반환합니다.
     *
     * ```
     * val center = wgs84PointOf(39.86391280373075, 116.37356590048701)
     * val query = geoHashCircleQueryOf(center, 589.0)
     *
     * // the distance between center and test1 is about 430 meters
     * val test1 = WGS84Point(39.8648866576058, 116.378465869303)
     *
     * // the distance between center and test2 is about 510 meters
     * val test2 = WGS84Point(39.8664787092599, 116.378552856158)
     *
     * // the distance between center and test2 is about 600 meters
     * val test3 = WGS84Point(39.8786787092599, 116.378552856158)
     *
     * query.contains(test1).shouldBeTrue()
     * query.contains(test2).shouldBeTrue()
     * query.contains(test3).shouldBeFalse()
     * ```
     *
     * @param hash GeoHash
     */
    override operator fun contains(hash: GeoHash): Boolean {
        return query.contains(hash)
    }

    /**
     * 지정된 지점이 반경 내에 포함되는지 여부를 반환합니다.
     *
     * ```
     * // Test query over 180-Meridian
     * val center = WGS84Point(39.86391280373075, 179.98356590048701)
     * val query = geoHashCircleQueryOf(center, 3000.0)
     *
     * val test1 = WGS84Point(39.8648866576058, 180.0)
     * val test2 = WGS84Point(39.8664787092599, -180.0)
     * val test3 = WGS84Point(39.8686787092599, -179.9957861565146)
     * val test4 = WGS84Point(39.8686787092599, 179.0057861565146)
     * val test5 = WGS84Point(39.8686787092599, -179.0)
     *
     * query.contains(test1).shouldBeTrue()
     * query.contains(test2).shouldBeTrue()
     * query.contains(test3).shouldBeTrue()
     * query.contains(test4).shouldBeFalse()
     * query.contains(test5).shouldBeFalse()
     * ```
     *
     * @param point WGS84Point
     */
    override operator fun contains(point: WGS84Point): Boolean {
        return query.contains(point)
    }

    override fun getWktBox(): String {
        return query.getWktBox()
    }

    override fun getSearchHashes(): List<GeoHash> {
        return query.getSearchHashes()
    }

    override fun toString(): String {
        return "Cicle Query [center=$center, radius=${getRadiusString()}]"
    }

    private fun getRadiusString(): String {
        return if (radius > 1000) {
            (radius / 1000).toString() + "km"
        } else {
            radius.toString() + "m"
        }
    }
}
