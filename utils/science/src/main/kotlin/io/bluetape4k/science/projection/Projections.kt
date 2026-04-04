package io.bluetape4k.science.projection

import io.bluetape4k.science.coords.GeoLocation
import io.bluetape4k.science.coords.UtmZone
import io.bluetape4k.science.coords.utmZoneOf
import org.locationtech.proj4j.BasicCoordinateTransform
import org.locationtech.proj4j.ProjCoordinate

/**
 * GIS 좌표계 변환 유틸리티 함수 모음입니다.
 *
 * Proj4J 라이브러리를 사용하여 UTM ↔ WGS84 및 임의 EPSG 코드 간 변환을 지원합니다.
 */

/**
 * 남반구 UTM Zone인 경우 Proj4 파라미터용 " +south" 접미사를 반환합니다.
 * 남반구는 위도 구역 문자가 'N' 미만(C~M)인 경우입니다.
 */
private fun UtmZone.southernHemisphereSuffix(): String =
    if (latitudeZone < 'N') " +south" else ""

/**
 * UTM 좌표(easting, northing)를 WGS84 위경도 [GeoLocation]으로 변환합니다.
 *
 * ```kotlin
 * val zone = UtmZone(52, 'S')
 * // 서울의 UTM Zone 52S 좌표 (approx)
 * val location = utmToWgs84(easting = 316_673.0, northing = 4_161_629.0, utmZone = zone)
 * println(location.latitude)  // 약 37.5665
 * println(location.longitude) // 약 126.9780
 * ```
 *
 * @param easting  UTM Easting (미터)
 * @param northing UTM Northing (미터)
 * @param utmZone  변환할 UTM Zone
 * @return WGS84 위경도 좌표
 */
fun utmToWgs84(easting: Double, northing: Double, utmZone: UtmZone): GeoLocation {
    val south = utmZone.southernHemisphereSuffix()
    val proj4Params = "+proj=utm +zone=${utmZone.longitudeZone} +datum=WGS84 +units=m +no_defs$south"
    val utmCrs = CrsRegistry.getCrsFromProj4(proj4Params)
    val wgs84Crs = CrsRegistry.getCrs("EPSG:4326")

    val transform = BasicCoordinateTransform(utmCrs, wgs84Crs)
    val src = ProjCoordinate(easting, northing)
    val dst = ProjCoordinate()
    transform.transform(src, dst)

    return GeoLocation(latitude = dst.y, longitude = dst.x)
}

/**
 * WGS84 위경도 [GeoLocation]을 UTM 좌표(easting, northing)로 변환합니다.
 *
 * ```kotlin
 * val seoul = GeoLocation(37.5665, 126.9780)
 * val (easting, northing) = wgs84ToUtm(seoul)
 * println(easting)  // 약 316_673.0 (미터)
 * println(northing) // 약 4_161_629.0 (미터)
 * ```
 *
 * @param location WGS84 위경도 좌표
 * @return Pair(easting, northing) — UTM 좌표 (미터)
 */
fun wgs84ToUtm(location: GeoLocation): Pair<Double, Double> {
    val utmZone = utmZoneOf(location)
    val south = utmZone.southernHemisphereSuffix()
    val proj4Params = "+proj=utm +zone=${utmZone.longitudeZone} +datum=WGS84 +units=m +no_defs$south"
    val wgs84Crs = CrsRegistry.getCrs("EPSG:4326")
    val utmCrs = CrsRegistry.getCrsFromProj4(proj4Params)

    val transform = BasicCoordinateTransform(wgs84Crs, utmCrs)
    val src = ProjCoordinate(location.longitude, location.latitude)
    val dst = ProjCoordinate()
    transform.transform(src, dst)

    return Pair(dst.x, dst.y)
}

/**
 * 소스 CRS에서 대상 CRS로 좌표를 변환합니다.
 *
 * ```kotlin
 * // WGS84(EPSG:4326) → UTM Zone 52N(EPSG:32652)
 * val (x, y) = transform("EPSG:4326", "EPSG:32652", lon = 126.9780, lat = 37.5665)
 * println(x) // 약 316_673.0 (easting, 미터)
 * println(y) // 약 4_161_629.0 (northing, 미터)
 * ```
 *
 * @param sourceCrs 소스 좌표계 EPSG 코드 (예: "EPSG:4326")
 * @param targetCrs 대상 좌표계 EPSG 코드 (예: "EPSG:32652")
 * @param lon       소스 CRS 기준 첫 번째 좌표 (WGS84인 경우 경도)
 * @param lat       소스 CRS 기준 두 번째 좌표 (WGS84인 경우 위도)
 * @return Pair(x, y) — 대상 CRS 기준 좌표
 */
fun transform(sourceCrs: String, targetCrs: String, lon: Double, lat: Double): Pair<Double, Double> {
    val src = CrsRegistry.getCrs(sourceCrs)
    val dst = CrsRegistry.getCrs(targetCrs)

    val transform = BasicCoordinateTransform(src, dst)
    val srcCoord = ProjCoordinate(lon, lat)
    val dstCoord = ProjCoordinate()
    transform.transform(srcCoord, dstCoord)

    return Pair(dstCoord.x, dstCoord.y)
}
