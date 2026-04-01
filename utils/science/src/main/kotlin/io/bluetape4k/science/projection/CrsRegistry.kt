package io.bluetape4k.science.projection

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateReferenceSystem
import java.util.concurrent.ConcurrentHashMap

/**
 * EPSG 코드 기반 좌표 참조 시스템(CRS) 캐시 레지스트리입니다.
 *
 * [CRSFactory]를 통해 생성된 CRS를 캐시하여 반복 생성 비용을 줄입니다.
 *
 * Proj4J 타입([CoordinateReferenceSystem])을 직접 반환하는 메서드는
 * 모두 `internal` 가시성으로 제한합니다.
 * 외부에서는 EPSG 코드나 Proj4 파라미터 문자열만 사용하십시오.
 */
internal object CrsRegistry {

    private val factory = CRSFactory()
    private val cache = ConcurrentHashMap<String, CoordinateReferenceSystem>()

    /**
     * EPSG 코드로 [CoordinateReferenceSystem]을 반환합니다.
     * 이미 생성된 경우 캐시에서 반환합니다.
     *
     * @param epsgCode EPSG 코드 (예: "EPSG:4326", "EPSG:32652")
     */
    fun getCrs(epsgCode: String): CoordinateReferenceSystem =
        cache.getOrPut(epsgCode) { factory.createFromName(epsgCode) }

    /**
     * Proj4 파라미터 문자열로 [CoordinateReferenceSystem]을 반환합니다.
     *
     * @param proj4Params Proj4 파라미터 문자열 (예: "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs")
     */
    fun getCrsFromProj4(proj4Params: String): CoordinateReferenceSystem =
        cache.getOrPut(proj4Params) { factory.createFromParameters("custom", proj4Params) }

    /** 캐시를 초기화합니다. */
    fun clearCache() = cache.clear()
}
