package io.bluetape4k.science.exposed.schema

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.bluetape4k.science.exposed.support.geoGeometry
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

private val spatialMapper = jacksonObjectMapper()

/**
 * 공간 레이어 메타데이터를 저장하는 Exposed 테이블입니다.
 *
 * Shapefile 등 공간 데이터 소스를 레이어 단위로 관리하며,
 * 경계 사각형(bbox)과 레코드 수 등의 요약 정보를 포함합니다.
 *
 * ```kotlin
 * // 레이어 삽입
 * transaction {
 *     SpatialLayerTable.insertAndGetId {
 *         it[name] = "korea_regions"
 *         it[srid] = 4326
 *         it[geometryType] = "MultiPolygon"
 *         it[bboxMinX] = 124.0
 *         it[bboxMinY] = 33.0
 *         it[bboxMaxX] = 131.0
 *         it[bboxMaxY] = 38.9
 *         it[recordCount] = 17
 *     }
 * }
 * ```
 */
object SpatialLayerTable: LongIdTable("spatial_layers") {

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    /** 레이어 이름 (유일 인덱스) */
    val name = varchar("name", 255).uniqueIndex()

    /** 레이어 설명 */
    val description = text("description").nullable()

    /** 원본 파일 경로 */
    val sourceFile = varchar("source_file", 1024).nullable()

    /** 좌표 참조 시스템 SRID (기본값: WGS 84) */
    val srid = integer("srid").default(4326)

    /** 도형 유형 (POINT, POLYGON, LINESTRING 등) */
    val geometryType = varchar("geometry_type", 50).nullable()

    /** 경계 사각형 최소 X 좌표 */
    val bboxMinX = double("bbox_min_x").nullable()

    /** 경계 사각형 최소 Y 좌표 */
    val bboxMinY = double("bbox_min_y").nullable()

    /** 경계 사각형 최대 X 좌표 */
    val bboxMaxX = double("bbox_max_x").nullable()

    /** 경계 사각형 최대 Y 좌표 */
    val bboxMaxY = double("bbox_max_y").nullable()

    /** 레이어에 포함된 레코드 수 */
    val recordCount = integer("record_count").default(0)
}

/**
 * 공간 피처(도형 + 속성)를 저장하는 Exposed 테이블입니다.
 *
 * 각 피처는 하나의 [SpatialLayerTable] 레이어에 속하며,
 * PostGIS `GEOMETRY` 컬럼에 도형을 저장하고 속성은 JSONB로 관리합니다.
 *
 * ```kotlin
 * // 피처 조회 예시
 * transaction {
 *     SpatialFeatureTable
 *         .selectAll()
 *         .where { SpatialFeatureTable.featureType eq "Point" }
 *         .map { it[SpatialFeatureTable.name] }
 *         .forEach { println(it) } // 예: "서울시청"
 * }
 * ```
 */
object SpatialFeatureTable: LongIdTable("spatial_features") {

    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)

    /** 소속 레이어 외래키 */
    val layerId = reference("layer_id", SpatialLayerTable)

    /** 도형 유형 문자열 (POINT, POLYGON 등) */
    val featureType = varchar("feature_type", 50)

    /** PostGIS generic geometry 컬럼 */
    val geom = geoGeometry("geom")

    /** 피처 속성을 JSONB로 저장 */
    val properties = jsonb<Map<String, Any?>>("properties",
        { spatialMapper.writeValueAsString(it) },
        { spatialMapper.readValue(it, object: TypeReference<Map<String, Any?>>() {}) }
    )

    /** 피처 이름 (선택) */
    val name = varchar("name", 255).nullable()
}
