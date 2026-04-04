package io.bluetape4k.science.exposed.model

import io.bluetape4k.logging.KLogging
import org.locationtech.jts.geom.Geometry
import java.io.Serializable

/**
 * 공간 레이어 레코드를 담는 데이터 클래스입니다.
 *
 * ```kotlin
 * val layer = SpatialLayerRecord(
 *     name = "korea_regions",
 *     description = "대한민국 행정구역 레이어",
 *     sourceFile = "/data/korea.shp",
 *     srid = 4326,
 *     geometryType = "MultiPolygon",
 *     bboxMinX = 124.0, bboxMinY = 33.0,
 *     bboxMaxX = 131.0, bboxMaxY = 38.9,
 *     recordCount = 17
 * )
 * println(layer.name)         // "korea_regions"
 * println(layer.srid)         // 4326
 * println(layer.recordCount)  // 17
 * ```
 *
 * @param id           기본키 (자동 생성)
 * @param name         레이어 이름
 * @param description  레이어 설명
 * @param sourceFile   원본 파일 경로
 * @param srid         좌표 참조 시스템 SRID
 * @param geometryType 도형 유형
 * @param bboxMinX     경계 사각형 최소 X
 * @param bboxMinY     경계 사각형 최소 Y
 * @param bboxMaxX     경계 사각형 최대 X
 * @param bboxMaxY     경계 사각형 최대 Y
 * @param recordCount  레코드 수
 */
data class SpatialLayerRecord(
    val id: Long = 0L,
    val name: String,
    val description: String? = null,
    val sourceFile: String? = null,
    val srid: Int = 4326,
    val geometryType: String? = null,
    val bboxMinX: Double? = null,
    val bboxMinY: Double? = null,
    val bboxMaxX: Double? = null,
    val bboxMaxY: Double? = null,
    val recordCount: Int = 0,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * 공간 피처 레코드를 담는 데이터 클래스입니다.
 *
 * ```kotlin
 * val gf = GeometryFactory()
 * val point = gf.createPoint(Coordinate(126.9780, 37.5665))
 * val feature = SpatialFeatureRecord(
 *     layerId = 1L,
 *     featureType = "Point",
 *     geom = point,
 *     properties = mapOf("NAME" to "서울시청", "CATEGORY" to "GOVERNMENT"),
 *     name = "서울시청"
 * )
 * println(feature.featureType)            // "Point"
 * println(feature.properties["CATEGORY"]) // "GOVERNMENT"
 * ```
 *
 * @param id          기본키 (자동 생성)
 * @param layerId     소속 레이어 ID
 * @param featureType 도형 유형 문자열
 * @param geom        JTS [Geometry] 객체 (compileOnly 의존성인 PostGIS 타입 대신 JTS 타입 사용)
 * @param properties  속성 맵
 * @param name        피처 이름
 */
data class SpatialFeatureRecord(
    val id: Long = 0L,
    val layerId: Long,
    val featureType: String,
    val geom: Geometry,
    val properties: Map<String, Any?> = emptyMap(),
    val name: String? = null,
) : Serializable {
    companion object : KLogging() {
        private const val serialVersionUID = 1L
    }
}
